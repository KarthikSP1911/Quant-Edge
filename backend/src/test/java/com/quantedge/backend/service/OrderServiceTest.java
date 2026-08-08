package com.quantedge.backend.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import com.quantedge.backend.dto.response.OrderResponse;
import com.quantedge.backend.entity.Company;
import com.quantedge.backend.entity.Order;
import com.quantedge.backend.entity.OrderExecution;
import com.quantedge.backend.entity.Portfolio;
import com.quantedge.backend.entity.User;
import com.quantedge.backend.enums.OrderSide;
import com.quantedge.backend.enums.OrderStatus;
import com.quantedge.backend.exception.CompanyNotFoundException;
import com.quantedge.backend.exception.InsufficientBalanceException;
import com.quantedge.backend.exception.InsufficientSharesException;
import com.quantedge.backend.external.dto.FinnhubQuoteResponse;
import com.quantedge.backend.repository.CompanyRepository;
import com.quantedge.backend.repository.OrderExecutionRepository;
import com.quantedge.backend.repository.OrderRepository;
import com.quantedge.backend.repository.PortfolioRepository;
import com.quantedge.backend.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private CompanyRepository companyRepository;

    @Mock
    private PortfolioRepository portfolioRepository;

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private OrderExecutionRepository orderExecutionRepository;

    @Mock
    private QuoteService quoteService;

    private OrderService orderService;

    private final Company apple = Company.builder().symbol("AAPL").build();

    @BeforeEach
    void setUp() {
        TradeExecutionService tradeExecutionService = new TradeExecutionService(
                userRepository, portfolioRepository, orderRepository, orderExecutionRepository);
        orderService = new OrderService(
                companyRepository, orderRepository, orderExecutionRepository, quoteService, tradeExecutionService);
    }

    private User userWithBalance(BigDecimal balance) {
        return User.builder().id(UUID.randomUUID()).balance(balance).build();
    }

    private void stubQuoteAndPersistence(double price) {
        when(quoteService.getQuote("AAPL")).thenReturn(new FinnhubQuoteResponse(price, price, price, price, price, 1L));
        when(orderRepository.save(any())).thenAnswer(inv -> {
            Order order = inv.getArgument(0);
            order.setId(UUID.randomUUID());
            return order;
        });
        when(orderExecutionRepository.save(any())).thenAnswer(inv -> {
            OrderExecution execution = inv.getArgument(0);
            execution.setExecutedAt(Instant.now());
            return execution;
        });
    }

    @Test
    void buyThrowsWhenSymbolUnknown() {
        when(companyRepository.findBySymbol("NOPE")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> orderService.buy(userWithBalance(BigDecimal.TEN), "NOPE", 1))
                .isInstanceOf(CompanyNotFoundException.class);
    }

    @Test
    void buyThrowsWhenBalanceInsufficient() {
        when(companyRepository.findBySymbol("AAPL")).thenReturn(Optional.of(apple));
        when(quoteService.getQuote("AAPL")).thenReturn(new FinnhubQuoteResponse(100.0, 100.0, 100.0, 100.0, 100.0, 1L));

        User user = userWithBalance(new BigDecimal("50.00"));

        assertThatThrownBy(() -> orderService.buy(user, "AAPL", 1)).isInstanceOf(InsufficientBalanceException.class);
        verifyNoInteractions(orderRepository, orderExecutionRepository);
    }

    @Test
    void buySucceedsAndCreatesNewPortfolioPosition() {
        when(companyRepository.findBySymbol("AAPL")).thenReturn(Optional.of(apple));
        stubQuoteAndPersistence(100.0);
        User user = userWithBalance(new BigDecimal("1000.00"));
        when(portfolioRepository.findByUserAndCompany(user, apple)).thenReturn(Optional.empty());

        OrderResponse response = orderService.buy(user, "AAPL", 5);

        assertThat(response.side()).isEqualTo(OrderSide.BUY);
        assertThat(response.status()).isEqualTo(OrderStatus.FILLED);
        assertThat(response.executionPrice()).isEqualByComparingTo("100.00");
        assertThat(user.getBalance()).isEqualByComparingTo("500.00");
        verify(portfolioRepository)
                .save(argThat(
                        p -> p.getQuantity() == 5 && p.getAverageCost().compareTo(new BigDecimal("100.00")) == 0));
    }

    @Test
    void sellThrowsWhenNoHolding() {
        when(companyRepository.findBySymbol("AAPL")).thenReturn(Optional.of(apple));
        when(quoteService.getQuote("AAPL")).thenReturn(new FinnhubQuoteResponse(100.0, 100.0, 100.0, 100.0, 100.0, 1L));
        User user = userWithBalance(BigDecimal.ZERO);
        when(portfolioRepository.findByUserAndCompany(user, apple)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> orderService.sell(user, "AAPL", 1)).isInstanceOf(InsufficientSharesException.class);
    }

    @Test
    void sellThrowsWhenSellingMoreThanOwned() {
        when(companyRepository.findBySymbol("AAPL")).thenReturn(Optional.of(apple));
        when(quoteService.getQuote("AAPL")).thenReturn(new FinnhubQuoteResponse(100.0, 100.0, 100.0, 100.0, 100.0, 1L));
        User user = userWithBalance(BigDecimal.ZERO);
        Portfolio portfolio = Portfolio.builder()
                .user(user)
                .company(apple)
                .quantity(3)
                .averageCost(BigDecimal.TEN)
                .build();
        when(portfolioRepository.findByUserAndCompany(user, apple)).thenReturn(Optional.of(portfolio));

        assertThatThrownBy(() -> orderService.sell(user, "AAPL", 5)).isInstanceOf(InsufficientSharesException.class);
    }

    @Test
    void sellSucceedsAndReducesHolding() {
        when(companyRepository.findBySymbol("AAPL")).thenReturn(Optional.of(apple));
        stubQuoteAndPersistence(120.0);
        User user = userWithBalance(new BigDecimal("100.00"));
        Portfolio portfolio = Portfolio.builder()
                .user(user)
                .company(apple)
                .quantity(5)
                .averageCost(BigDecimal.TEN)
                .build();
        when(portfolioRepository.findByUserAndCompany(user, apple)).thenReturn(Optional.of(portfolio));

        OrderResponse response = orderService.sell(user, "AAPL", 2);

        assertThat(response.side()).isEqualTo(OrderSide.SELL);
        assertThat(user.getBalance()).isEqualByComparingTo("340.00");
        verify(portfolioRepository).save(argThat(p -> p.getQuantity() == 3));
    }

    @Test
    void sellingAllSharesDeletesThePortfolioPosition() {
        when(companyRepository.findBySymbol("AAPL")).thenReturn(Optional.of(apple));
        stubQuoteAndPersistence(120.0);
        User user = userWithBalance(BigDecimal.ZERO);
        Portfolio portfolio = Portfolio.builder()
                .user(user)
                .company(apple)
                .quantity(2)
                .averageCost(BigDecimal.TEN)
                .build();
        when(portfolioRepository.findByUserAndCompany(user, apple)).thenReturn(Optional.of(portfolio));

        orderService.sell(user, "AAPL", 2);

        verify(portfolioRepository).delete(portfolio);
        verify(portfolioRepository, never()).save(portfolio);
    }
}
