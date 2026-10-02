package com.quantedge.backend.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import com.quantedge.backend.entity.Company;
import com.quantedge.backend.entity.Order;
import com.quantedge.backend.entity.User;
import com.quantedge.backend.enums.OrderStatus;
import com.quantedge.backend.kafka.producer.TradeExecutedProducer;
import com.quantedge.backend.repository.CompanyRepository;
import com.quantedge.backend.repository.OrderRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;

class OrderMatcherServiceTest {

    private final CompanyRepository companyRepository = mock(CompanyRepository.class);
    private final OrderRepository orderRepository = mock(OrderRepository.class);
    private final TradeExecutionService tradeExecutionService = mock(TradeExecutionService.class);
    private final TradeExecutedProducer producer = mock(TradeExecutedProducer.class);

    @SuppressWarnings("unchecked")
    private final ObjectProvider<OrderMatcherService> self = mock(ObjectProvider.class);

    private final OrderMatcherService service =
            new OrderMatcherService(companyRepository, orderRepository, tradeExecutionService, producer, self);
    private final OrderMatcherService proxy = mock(OrderMatcherService.class);

    private Order orderFor(UUID userId) {
        User user = mock(User.class);
        when(user.getId()).thenReturn(userId);
        Order order = mock(Order.class);
        when(order.getId()).thenReturn(UUID.randomUUID());
        when(order.getUser()).thenReturn(user);
        return order;
    }

    @Test
    void neverFillsTwoOrdersOfTheSameUserConcurrently() {
        UUID userA = UUID.randomUUID();
        UUID userB = UUID.randomUUID();
        List<Order> orders =
                List.of(orderFor(userA), orderFor(userA), orderFor(userA), orderFor(userB), orderFor(userB));
        Map<UUID, UUID> userByOrderId = new ConcurrentHashMap<>();
        orders.forEach(o -> userByOrderId.put(o.getId(), o.getUser().getId()));

        Company company = mock(Company.class);
        when(company.getSymbol()).thenReturn("AAPL");
        when(companyRepository.findBySymbol("AAPL")).thenReturn(Optional.of(company));
        when(orderRepository.findByCompanyAndStatus(company, OrderStatus.OPEN)).thenReturn(orders);
        when(self.getObject()).thenReturn(proxy);

        Map<UUID, AtomicInteger> inFlight = new ConcurrentHashMap<>();
        AtomicInteger overlaps = new AtomicInteger();
        AtomicInteger fills = new AtomicInteger();
        doAnswer(invocation -> {
                    UUID user = userByOrderId.get(invocation.<UUID>getArgument(0));
                    AtomicInteger counter = inFlight.computeIfAbsent(user, id -> new AtomicInteger());
                    if (counter.incrementAndGet() > 1) {
                        overlaps.incrementAndGet();
                    }
                    Thread.sleep(30);
                    counter.decrementAndGet();
                    fills.incrementAndGet();
                    return null;
                })
                .when(proxy)
                .attemptFill(any(UUID.class), any(BigDecimal.class));

        service.matchSymbol("AAPL", new BigDecimal("100"));

        assertThat(fills.get()).isEqualTo(5);
        assertThat(overlaps.get()).isZero();
    }

    @Test
    void oneFailingOrderDoesNotStopTheSameUsersOtherOrders() {
        UUID user = UUID.randomUUID();
        Order first = orderFor(user);
        Order second = orderFor(user);

        Company company = mock(Company.class);
        when(company.getSymbol()).thenReturn("AAPL");
        when(companyRepository.findBySymbol("AAPL")).thenReturn(Optional.of(company));
        when(orderRepository.findByCompanyAndStatus(company, OrderStatus.OPEN)).thenReturn(List.of(first, second));
        when(self.getObject()).thenReturn(proxy);

        AtomicInteger attempts = new AtomicInteger();
        doAnswer(invocation -> {
                    attempts.incrementAndGet();
                    if (invocation.<UUID>getArgument(0).equals(first.getId())) {
                        throw new IllegalStateException("boom");
                    }
                    return null;
                })
                .when(proxy)
                .attemptFill(any(UUID.class), any(BigDecimal.class));

        service.matchSymbol("AAPL", new BigDecimal("100"));

        assertThat(attempts.get()).isEqualTo(2);
    }
}
