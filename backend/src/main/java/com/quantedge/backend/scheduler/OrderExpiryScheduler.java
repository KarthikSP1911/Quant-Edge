package com.quantedge.backend.scheduler;

import com.quantedge.backend.entity.Order;
import com.quantedge.backend.enums.OrderStatus;
import com.quantedge.backend.repository.OrderRepository;
import java.time.Instant;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Sweeps OPEN DAY orders past their {@code expires_at} to EXPIRED. GTC orders have a null
 * expires_at, which never matches the "before now" comparison, so they're naturally excluded.
 */
@Component
@RequiredArgsConstructor
public class OrderExpiryScheduler {

    private static final Logger log = LoggerFactory.getLogger(OrderExpiryScheduler.class);

    private final OrderRepository orderRepository;

    @Scheduled(fixedRateString = "${order-expiry.fixed-rate-ms:60000}")
    @Transactional
    public void expireDayOrders() {
        List<Order> expired = orderRepository.findByStatusAndExpiresAtBefore(OrderStatus.OPEN, Instant.now());
        if (expired.isEmpty()) {
            return;
        }
        expired.forEach(order -> order.setStatus(OrderStatus.EXPIRED));
        orderRepository.saveAll(expired);
        log.info("Expired {} DAY orders past their expiry time", expired.size());
    }
}
