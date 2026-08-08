package com.quantedge.backend.service.export;

import static org.assertj.core.api.Assertions.assertThat;

import com.quantedge.backend.dto.response.RealizedPnlLine;
import com.quantedge.backend.entity.Company;
import com.quantedge.backend.entity.Order;
import com.quantedge.backend.entity.OrderExecution;
import com.quantedge.backend.enums.OrderSide;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;

class PnlCalculationServiceTest {

    private final PnlCalculationService service = new PnlCalculationService();

    private static Company company(String symbol) {
        return Company.builder().symbol(symbol).build();
    }

    private static OrderExecution execution(
            Company company, OrderSide side, int quantity, String price, Instant executedAt) {
        Order order = Order.builder().company(company).side(side).build();
        return OrderExecution.builder()
                .order(order)
                .executionPrice(new BigDecimal(price))
                .executedQuantity(quantity)
                .executedAt(executedAt)
                .build();
    }

    @Test
    void buyThenSellAtProfitRealizesGain() {
        Company aapl = company("AAPL");
        List<OrderExecution> executions = List.of(
                execution(aapl, OrderSide.BUY, 10, "100.00", Instant.parse("2026-01-01T00:00:00Z")),
                execution(aapl, OrderSide.SELL, 10, "120.00", Instant.parse("2026-01-02T00:00:00Z")));

        List<RealizedPnlLine> result = service.calculateRealizedGains(executions);

        assertThat(result).hasSize(1);
        RealizedPnlLine line = result.get(0);
        assertThat(line.symbol()).isEqualTo("AAPL");
        assertThat(line.quantity()).isEqualTo(10);
        assertThat(line.costBasis()).isEqualByComparingTo("100.00");
        assertThat(line.realizedGain()).isEqualByComparingTo("200.00");
    }

    @Test
    void buyThenSellAtLossRealizesLoss() {
        Company aapl = company("AAPL");
        List<OrderExecution> executions = List.of(
                execution(aapl, OrderSide.BUY, 10, "100.00", Instant.parse("2026-01-01T00:00:00Z")),
                execution(aapl, OrderSide.SELL, 10, "80.00", Instant.parse("2026-01-02T00:00:00Z")));

        List<RealizedPnlLine> result = service.calculateRealizedGains(executions);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).realizedGain()).isEqualByComparingTo("-200.00");
    }

    @Test
    void multipleBuysAtDifferentPricesAverageCostBeforeSell() {
        Company aapl = company("AAPL");
        List<OrderExecution> executions = List.of(
                execution(aapl, OrderSide.BUY, 10, "100.00", Instant.parse("2026-01-01T00:00:00Z")),
                execution(aapl, OrderSide.BUY, 10, "200.00", Instant.parse("2026-01-02T00:00:00Z")),
                execution(aapl, OrderSide.SELL, 20, "180.00", Instant.parse("2026-01-03T00:00:00Z")));

        List<RealizedPnlLine> result = service.calculateRealizedGains(executions);

        // average cost = (10*100 + 10*200) / 20 = 150.00
        assertThat(result).hasSize(1);
        assertThat(result.get(0).costBasis()).isEqualByComparingTo("150.00");
        assertThat(result.get(0).realizedGain()).isEqualByComparingTo("600.00");
    }

    @Test
    void partialSellLeavesRemainderAtSameAverageCost() {
        Company aapl = company("AAPL");
        List<OrderExecution> executions = List.of(
                execution(aapl, OrderSide.BUY, 10, "100.00", Instant.parse("2026-01-01T00:00:00Z")),
                execution(aapl, OrderSide.SELL, 4, "150.00", Instant.parse("2026-01-02T00:00:00Z")),
                execution(aapl, OrderSide.SELL, 6, "90.00", Instant.parse("2026-01-03T00:00:00Z")));

        List<RealizedPnlLine> result = service.calculateRealizedGains(executions);

        assertThat(result).hasSize(2);
        assertThat(result.get(0).costBasis()).isEqualByComparingTo("100.00");
        assertThat(result.get(0).realizedGain()).isEqualByComparingTo("200.00");
        assertThat(result.get(1).costBasis()).isEqualByComparingTo("100.00");
        assertThat(result.get(1).realizedGain()).isEqualByComparingTo("-60.00");
    }

    @Test
    void fullSellFollowedByNewBuyResetsAverageCost() {
        Company aapl = company("AAPL");
        List<OrderExecution> executions = List.of(
                execution(aapl, OrderSide.BUY, 10, "100.00", Instant.parse("2026-01-01T00:00:00Z")),
                execution(aapl, OrderSide.SELL, 10, "150.00", Instant.parse("2026-01-02T00:00:00Z")),
                execution(aapl, OrderSide.BUY, 5, "300.00", Instant.parse("2026-01-03T00:00:00Z")),
                execution(aapl, OrderSide.SELL, 5, "310.00", Instant.parse("2026-01-04T00:00:00Z")));

        List<RealizedPnlLine> result = service.calculateRealizedGains(executions);

        assertThat(result).hasSize(2);
        assertThat(result.get(1).costBasis()).isEqualByComparingTo("300.00");
        assertThat(result.get(1).realizedGain()).isEqualByComparingTo("50.00");
    }

    @Test
    void interleavedSymbolsTrackSeparateLots() {
        Company aapl = company("AAPL");
        Company tsla = company("TSLA");
        List<OrderExecution> executions = List.of(
                execution(aapl, OrderSide.BUY, 10, "100.00", Instant.parse("2026-01-01T00:00:00Z")),
                execution(tsla, OrderSide.BUY, 5, "200.00", Instant.parse("2026-01-01T01:00:00Z")),
                execution(aapl, OrderSide.SELL, 10, "110.00", Instant.parse("2026-01-02T00:00:00Z")),
                execution(tsla, OrderSide.SELL, 5, "180.00", Instant.parse("2026-01-02T01:00:00Z")));

        List<RealizedPnlLine> result = service.calculateRealizedGains(executions);

        assertThat(result).hasSize(2);
        assertThat(result)
                .filteredOn(line -> line.symbol().equals("AAPL"))
                .first()
                .satisfies(line -> assertThat(line.realizedGain()).isEqualByComparingTo("100.00"));
        assertThat(result)
                .filteredOn(line -> line.symbol().equals("TSLA"))
                .first()
                .satisfies(line -> assertThat(line.realizedGain()).isEqualByComparingTo("-100.00"));
    }

    @Test
    void noSellsProducesNoRealizedGains() {
        Company aapl = company("AAPL");
        List<OrderExecution> executions =
                List.of(execution(aapl, OrderSide.BUY, 10, "100.00", Instant.parse("2026-01-01T00:00:00Z")));

        assertThat(service.calculateRealizedGains(executions)).isEmpty();
    }
}
