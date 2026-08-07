package com.quantedge.backend.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.quantedge.backend.dto.response.DashboardResponse;
import com.quantedge.backend.dto.response.WatchlistItemResponse;
import com.quantedge.backend.entity.Company;
import com.quantedge.backend.entity.Order;
import com.quantedge.backend.entity.OrderExecution;
import com.quantedge.backend.entity.Portfolio;
import com.quantedge.backend.entity.User;
import com.quantedge.backend.enums.OrderSide;
import com.quantedge.backend.external.dto.FinnhubQuoteResponse;
import com.quantedge.backend.mapper.CompanyMapper;
import com.quantedge.backend.repository.OrderExecutionRepository;
import com.quantedge.backend.repository.PortfolioRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DashboardServiceTest {

    @Mock
    private PortfolioRepository portfolioRepository;

    @Mock
    private OrderExecutionRepository orderExecutionRepository;

    @Mock
    private WatchlistService watchlistService;

    @Mock
    private QuoteService quoteService;

    private DashboardService dashboardService;

    private final Company apple = Company.builder().symbol("AAPL").build();
    private final User user = User.builder().balance(new BigDecimal("500.00")).build();

    @BeforeEach
    void setUp() {
        dashboardService = new DashboardService(
                portfolioRepository, orderExecutionRepository, watchlistService, quoteService, new CompanyMapper());
    }

    @Test
    void valuesPortfolioPositionsAtCurrentQuoteAndSumsAccountValue() {
        Portfolio position = Portfolio.builder()
                .user(user)
                .company(apple)
                .quantity(10)
                .averageCost(new BigDecimal("100.00"))
                .build();
        when(portfolioRepository.findByUser(user)).thenReturn(List.of(position));
        when(quoteService.getQuote("AAPL")).thenReturn(new FinnhubQuoteResponse(120.0, 125.0, 115.0, 118.0, 117.0, 1L));
        when(orderExecutionRepository.findTop10ByOrderUserOrderByExecutedAtDesc(user))
                .thenReturn(List.of());
        when(watchlistService.list(user)).thenReturn(List.of());

        DashboardResponse response = dashboardService.getDashboard(user);

        assertThat(response.portfolio().cashBalance()).isEqualByComparingTo("500.00");
        assertThat(response.portfolio().positions()).hasSize(1);
        assertThat(response.portfolio().positions().get(0).marketValue()).isEqualByComparingTo("1200.00");
        assertThat(response.portfolio().positions().get(0).gainLoss()).isEqualByComparingTo("200.00");
        assertThat(response.portfolio().positions().get(0).previousClose()).isEqualByComparingTo("117.00");
        assertThat(response.portfolio().positions().get(0).changePercent()).isEqualByComparingTo("2.5600");
        assertThat(response.portfolio().totalMarketValue()).isEqualByComparingTo("1200.00");
        assertThat(response.portfolio().totalAccountValue()).isEqualByComparingTo("1700.00");
    }

    @Test
    void getPortfolioSummaryReturnsSameShapeAsDashboardsPortfolioPanel() {
        Portfolio position = Portfolio.builder()
                .user(user)
                .company(apple)
                .quantity(2)
                .averageCost(new BigDecimal("50.00"))
                .build();
        when(portfolioRepository.findByUser(user)).thenReturn(List.of(position));
        when(quoteService.getQuote("AAPL")).thenReturn(new FinnhubQuoteResponse(60.0, 65.0, 55.0, 58.0, 55.0, 1L));

        var summary = dashboardService.getPortfolioSummary(user);

        assertThat(summary.positions()).hasSize(1);
        assertThat(summary.totalAccountValue()).isEqualByComparingTo("620.00");
    }

    @Test
    void limitsWatchlistPreviewToFiveEntries() {
        when(portfolioRepository.findByUser(user)).thenReturn(List.of());
        when(orderExecutionRepository.findTop10ByOrderUserOrderByExecutedAtDesc(user))
                .thenReturn(List.of());
        List<WatchlistItemResponse> sixItems =
                List.of(item("A"), item("B"), item("C"), item("D"), item("E"), item("F"));
        when(watchlistService.list(user)).thenReturn(sixItems);

        DashboardResponse response = dashboardService.getDashboard(user);

        assertThat(response.watchlistPreview()).hasSize(5);
    }

    @Test
    void mapsRecentOrderExecutionsToTransactions() {
        when(portfolioRepository.findByUser(user)).thenReturn(List.of());
        when(watchlistService.list(user)).thenReturn(List.of());

        Order order = Order.builder().company(apple).side(OrderSide.BUY).build();
        OrderExecution execution = OrderExecution.builder()
                .id(UUID.randomUUID())
                .order(order)
                .executionPrice(new BigDecimal("120.00"))
                .executedQuantity(10)
                .executedAt(Instant.parse("2026-08-06T00:00:00Z"))
                .build();
        when(orderExecutionRepository.findTop10ByOrderUserOrderByExecutedAtDesc(user))
                .thenReturn(List.of(execution));

        DashboardResponse response = dashboardService.getDashboard(user);

        assertThat(response.recentTransactions()).hasSize(1);
        assertThat(response.recentTransactions().get(0).symbol()).isEqualTo("AAPL");
        assertThat(response.recentTransactions().get(0).side()).isEqualTo(OrderSide.BUY);
    }

    private WatchlistItemResponse item(String symbol) {
        return new WatchlistItemResponse(
                new CompanyMapper().toResponse(Company.builder().symbol(symbol).build()), "2026-08-06T00:00:00Z");
    }
}
