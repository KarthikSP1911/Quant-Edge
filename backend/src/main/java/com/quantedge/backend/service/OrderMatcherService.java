package com.quantedge.backend.service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import com.quantedge.backend.entity.Company;
import com.quantedge.backend.entity.Order;
import com.quantedge.backend.entity.OrderExecution;
import com.quantedge.backend.enums.OrderStatus;
import com.quantedge.backend.enums.OrderType;
import com.quantedge.backend.exception.InsufficientBalanceException;
import com.quantedge.backend.exception.InsufficientSharesException;
import com.quantedge.backend.kafka.producer.TradeExecutedProducer;
import com.quantedge.backend.repository.CompanyRepository;
import com.quantedge.backend.repository.OrderRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Scans OPEN orders for a symbol against a synced price and fills the ones that cross, per {@link
 * OrderTriggerEvaluator}. Each order is evaluated in its own transaction under a row lock ({@link
 * OrderRepository#findWithLockById}), re-checking status == OPEN before doing anything - the
 * double-fill guard that makes redelivery of the same price event a no-op.
 */
@Service
public class OrderMatcherService {

    private static final Logger log = LoggerFactory.getLogger(OrderMatcherService.class);

    private final CompanyRepository companyRepository;
    private final OrderRepository orderRepository;
    private final TradeExecutionService tradeExecutionService;
    private final TradeExecutedProducer tradeExecutedProducer;
    private final ObjectProvider<OrderMatcherService> self;

    public OrderMatcherService(
            CompanyRepository companyRepository,
            OrderRepository orderRepository,
            TradeExecutionService tradeExecutionService,
            TradeExecutedProducer tradeExecutedProducer,
            ObjectProvider<OrderMatcherService> self) {
        this.companyRepository = companyRepository;
        this.orderRepository = orderRepository;
        this.tradeExecutionService = tradeExecutionService;
        this.tradeExecutedProducer = tradeExecutedProducer;
        // Self-injected proxy: attemptFill must be called through this so its @Transactional
        // actually applies - calling it via `this` from matchSymbol bypasses the proxy entirely.
        // ObjectProvider defers the lookup past construction, avoiding the circular
        // BeanCurrentlyInCreationException a direct/@Lazy self-injected field triggers.
        this.self = self;
    }

    public void matchSymbol(String symbol, BigDecimal syncedPrice) {
        companyRepository
                .findBySymbol(symbol)
                .ifPresentOrElse(
                        company -> matchOpenOrders(company, syncedPrice),
                        () -> log.warn("Ignoring price event for unknown symbol={}", symbol));
    }

    /**
     * Each order fills in its own transaction under a row lock on the order (see {@link
     * #attemptFill}), but that lock does not cover the user's balance or portfolio row, which a
     * fill reads, modifies and writes back. Two orders from the same user filled at once would
     * therefore lose a balance update. So orders are grouped by user: different users' groups run
     * concurrently on virtual threads, while one user's orders run one after another inside their
     * group. The Kafka consumer driving this is still single-threaded per topic, so this only
     * parallelizes work within one price tick, not across ticks.
     */
    private void matchOpenOrders(Company company, BigDecimal syncedPrice) {
        List<Order> openOrders = orderRepository.findByCompanyAndStatus(company, OrderStatus.OPEN);
        if (openOrders.isEmpty()) {
            return;
        }

        Map<UUID, List<Order>> ordersByUser = new LinkedHashMap<>();
        for (Order order : openOrders) {
            ordersByUser
                    .computeIfAbsent(order.getUser().getId(), id -> new ArrayList<>())
                    .add(order);
        }

        try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
            List<Future<?>> groups = ordersByUser.values().stream()
                    .<Future<?>>map(userOrders -> executor.submit(() -> fillSequentially(userOrders, syncedPrice)))
                    .toList();
            for (Future<?> group : groups) {
                try {
                    group.get();
                } catch (ExecutionException ex) {
                    log.error("Order matching failed for symbol={}", company.getSymbol(), ex.getCause());
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                    log.warn("Interrupted while matching orders for symbol={}", company.getSymbol());
                    break;
                }
            }
        }
    }

    /** One failing order must not stop the same user's remaining orders from being evaluated. */
    private void fillSequentially(List<Order> userOrders, BigDecimal syncedPrice) {
        for (Order order : userOrders) {
            try {
                self.getObject().attemptFill(order.getId(), syncedPrice);
            } catch (RuntimeException ex) {
                log.error("Order fill failed for order={}", order.getId(), ex);
            }
        }
    }

    @Transactional
    public void attemptFill(UUID orderId, BigDecimal syncedPrice) {
        Order order = orderRepository.findWithLockById(orderId).orElse(null);
        if (order == null || order.getStatus() != OrderStatus.OPEN) {
            return;
        }

        OrderTriggerEvaluator.Trigger trigger = OrderTriggerEvaluator.evaluate(order, syncedPrice);
        if (trigger.convertsToLimit()) {
            order.setType(OrderType.LIMIT);
            orderRepository.save(order);
            return;
        }
        if (!trigger.fills()) {
            return;
        }

        try {
            OrderExecution execution = tradeExecutionService.fillExistingOrder(order, trigger.fillPrice());
            tradeExecutedProducer.publishAfterCommit(execution);
        } catch (InsufficientBalanceException | InsufficientSharesException ex) {
            log.warn("Rejecting order {} at fill time: {}", orderId, ex.getMessage());
            order.setStatus(OrderStatus.REJECTED);
            orderRepository.save(order);
        }
    }
}
