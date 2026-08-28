package com.quantedge.backend.service;

import java.math.BigDecimal;
import java.util.List;
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
     * Each order fills under its own row lock and its own transaction (see {@link #attemptFill}),
     * so orders competing for the same price event are independent of one another - safe to
     * evaluate concurrently on virtual threads rather than one at a time. The Kafka consumer
     * driving this is still single-threaded per topic, so this only parallelizes work within one
     * price tick, not across ticks.
     */
    private void matchOpenOrders(Company company, BigDecimal syncedPrice) {
        List<Order> openOrders = orderRepository.findByCompanyAndStatus(company, OrderStatus.OPEN);
        if (openOrders.isEmpty()) {
            return;
        }

        try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
            List<Future<?>> fills = openOrders.stream()
                    .map(order -> executor.submit(() -> self.getObject().attemptFill(order.getId(), syncedPrice)))
                    .toList();
            for (Future<?> fill : fills) {
                try {
                    fill.get();
                } catch (ExecutionException ex) {
                    log.error("Order fill failed for symbol={}", company.getSymbol(), ex.getCause());
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                    log.warn("Interrupted while matching orders for symbol={}", company.getSymbol());
                    break;
                }
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
