package com.quantedge.backend.service;

import com.quantedge.backend.entity.Order;
import com.quantedge.backend.entity.OrderExecution;
import com.quantedge.backend.enums.OrderStatus;
import com.quantedge.backend.enums.OrderType;
import com.quantedge.backend.exception.InsufficientBalanceException;
import com.quantedge.backend.exception.InsufficientSharesException;
import com.quantedge.backend.kafka.producer.TradeExecutedProducer;
import com.quantedge.backend.repository.CompanyRepository;
import com.quantedge.backend.repository.OrderRepository;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Scans OPEN orders for a symbol against a synced price and fills the ones that cross, per {@link
 * OrderTriggerEvaluator}. Each order is evaluated in its own transaction under a row lock ({@link
 * OrderRepository#findWithLockById}), re-checking status == OPEN before doing anything - the
 * double-fill guard that makes redelivery of the same price event a no-op.
 */
@Service
@RequiredArgsConstructor
public class OrderMatcherService {

    private static final Logger log = LoggerFactory.getLogger(OrderMatcherService.class);

    private final CompanyRepository companyRepository;
    private final OrderRepository orderRepository;
    private final TradeExecutionService tradeExecutionService;
    private final TradeExecutedProducer tradeExecutedProducer;

    public void matchSymbol(String symbol, BigDecimal syncedPrice) {
        companyRepository
                .findBySymbol(symbol)
                .ifPresentOrElse(
                        company -> {
                            List<Order> openOrders = orderRepository.findByCompanyAndStatus(company, OrderStatus.OPEN);
                            for (Order order : openOrders) {
                                attemptFill(order.getId(), syncedPrice);
                            }
                        },
                        () -> log.warn("Ignoring price event for unknown symbol={}", symbol));
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
