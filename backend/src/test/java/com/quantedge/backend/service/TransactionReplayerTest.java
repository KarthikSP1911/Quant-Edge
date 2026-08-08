package com.quantedge.backend.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.quantedge.backend.entity.Company;
import com.quantedge.backend.entity.Order;
import com.quantedge.backend.entity.OrderExecution;
import com.quantedge.backend.enums.OrderSide;
import com.quantedge.backend.service.TransactionReplayer.HoldingState;
import com.quantedge.backend.service.TransactionReplayer.RealizedDecision;
import com.quantedge.backend.service.TransactionReplayer.ReplayResult;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;

class TransactionReplayerTest {

    private static final Company AAPL =
            Company.builder().symbol("AAPL").name("Apple").build();
    private static final Company TSLA =
            Company.builder().symbol("TSLA").name("Tesla").build();

    private static OrderExecution execution(
            Company company, OrderSide side, int quantity, String price, String executedAt) {
        Order order = Order.builder().company(company).side(side).build();
        return OrderExecution.builder()
                .order(order)
                .executedQuantity(quantity)
                .executionPrice(new BigDecimal(price))
                .executedAt(Instant.parse(executedAt))
                .build();
    }

    @Test
    void emptyHistoryProducesNoHoldingsOrDecisions() {
        ReplayResult result = TransactionReplayer.replay(List.of());

        assertThat(result.openHoldings()).isEmpty();
        assertThat(result.realizedDecisions()).isEmpty();
        assertThat(result.cashDelta()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    void singleBuyLeavesAnOpenHoldingAtThatCost() {
        List<OrderExecution> executions = List.of(execution(AAPL, OrderSide.BUY, 10, "150.00", "2026-01-01T10:00:00Z"));

        ReplayResult result = TransactionReplayer.replay(executions);

        assertThat(result.openHoldings()).containsExactly(new HoldingState(AAPL, 10, new BigDecimal("150.00")));
        assertThat(result.realizedDecisions()).isEmpty();
        assertThat(result.cashDelta()).isEqualByComparingTo("-1500.00");
    }

    @Test
    void twoBuysAtDifferentPricesProduceAWeightedAverageCost() {
        List<OrderExecution> executions = List.of(
                execution(AAPL, OrderSide.BUY, 10, "100.00", "2026-01-01T10:00:00Z"),
                execution(AAPL, OrderSide.BUY, 10, "200.00", "2026-01-02T10:00:00Z"));

        ReplayResult result = TransactionReplayer.replay(executions);

        assertThat(result.openHoldings()).containsExactly(new HoldingState(AAPL, 20, new BigDecimal("150.00")));
    }

    @Test
    void fullyClosingAPositionRemovesItFromOpenHoldingsAndRecordsARealizedDecision() {
        List<OrderExecution> executions = List.of(
                execution(AAPL, OrderSide.BUY, 10, "150.00", "2026-01-01T10:00:00Z"),
                execution(AAPL, OrderSide.SELL, 10, "180.00", "2026-01-05T10:00:00Z"));

        ReplayResult result = TransactionReplayer.replay(executions);

        assertThat(result.openHoldings()).isEmpty();
        assertThat(result.realizedDecisions()).hasSize(1);
        RealizedDecision decision = result.realizedDecisions().get(0);
        assertThat(decision.company()).isEqualTo(AAPL);
        assertThat(decision.buyPrice()).isEqualByComparingTo("150.00");
        assertThat(decision.sellPrice()).isEqualByComparingTo("180.00");
        assertThat(decision.realizedGainPercent()).isEqualByComparingTo("20.0000");
        assertThat(result.cashDelta()).isEqualByComparingTo("300.00");
    }

    @Test
    void partialSellReducesQuantityButKeepsTheAverageCostUnchanged() {
        List<OrderExecution> executions = List.of(
                execution(AAPL, OrderSide.BUY, 10, "150.00", "2026-01-01T10:00:00Z"),
                execution(AAPL, OrderSide.SELL, 4, "180.00", "2026-01-05T10:00:00Z"));

        ReplayResult result = TransactionReplayer.replay(executions);

        assertThat(result.openHoldings()).containsExactly(new HoldingState(AAPL, 6, new BigDecimal("150.00")));
        assertThat(result.realizedDecisions()).hasSize(1);
    }

    @Test
    void aLosingSaleProducesANegativeRealizedGainPercent() {
        List<OrderExecution> executions = List.of(
                execution(TSLA, OrderSide.BUY, 5, "300.00", "2026-01-01T10:00:00Z"),
                execution(TSLA, OrderSide.SELL, 5, "250.00", "2026-01-05T10:00:00Z"));

        ReplayResult result = TransactionReplayer.replay(executions);

        assertThat(result.realizedDecisions().get(0).realizedGainPercent()).isEqualByComparingTo("-16.6667");
    }

    @Test
    void executionsForDifferentSymbolsAreTrackedIndependently() {
        List<OrderExecution> executions = List.of(
                execution(AAPL, OrderSide.BUY, 10, "150.00", "2026-01-01T10:00:00Z"),
                execution(TSLA, OrderSide.BUY, 5, "300.00", "2026-01-02T10:00:00Z"),
                execution(AAPL, OrderSide.SELL, 10, "180.00", "2026-01-05T10:00:00Z"));

        ReplayResult result = TransactionReplayer.replay(executions);

        assertThat(result.openHoldings()).containsExactly(new HoldingState(TSLA, 5, new BigDecimal("300.00")));
        assertThat(result.realizedDecisions()).hasSize(1);
        assertThat(result.realizedDecisions().get(0).company()).isEqualTo(AAPL);
    }

    @Test
    void aSellWithoutAMatchingPriorBuyIsSkippedRatherThanFabricatingACostBasis() {
        List<OrderExecution> executions =
                List.of(execution(AAPL, OrderSide.SELL, 10, "180.00", "2026-01-05T10:00:00Z"));

        ReplayResult result = TransactionReplayer.replay(executions);

        assertThat(result.openHoldings()).isEmpty();
        assertThat(result.realizedDecisions()).isEmpty();
        assertThat(result.cashDelta()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    void executionsAfterTheAsOfCutoffMustAlreadyBeExcludedByTheCaller() {
        // TransactionReplayer trusts its input is pre-filtered to executedAt <= asOf; verify that
        // replaying only the in-window prefix of a longer history gives the point-in-time state.
        List<OrderExecution> fullHistory = List.of(
                execution(AAPL, OrderSide.BUY, 10, "150.00", "2026-01-01T10:00:00Z"),
                execution(AAPL, OrderSide.SELL, 10, "180.00", "2026-01-05T10:00:00Z"),
                execution(AAPL, OrderSide.BUY, 5, "160.00", "2026-01-10T10:00:00Z"));

        ReplayResult asOfJan6 = TransactionReplayer.replay(fullHistory.subList(0, 2));

        assertThat(asOfJan6.openHoldings()).isEmpty();
        assertThat(asOfJan6.realizedDecisions()).hasSize(1);
    }
}
