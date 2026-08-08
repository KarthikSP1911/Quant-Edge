package com.quantedge.backend.service;

import com.quantedge.backend.entity.Company;
import com.quantedge.backend.entity.OrderExecution;
import com.quantedge.backend.enums.OrderSide;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Pure replay of a user's {@link OrderExecution} history into a point-in-time snapshot: open
 * holdings (quantity + weighted-average cost) and realized gain/loss per SELL execution. Mirrors
 * {@link TradeExecutionService}'s weighted-average-cost math exactly, so a holding's average cost
 * here always agrees with what the live portfolio would have shown at that moment - no separate
 * FIFO-lot bookkeeping is introduced.
 *
 * <p>Callers must pass executions already filtered to {@code executedAt <= asOf} and sorted
 * ascending by {@code executedAt}; this class has no notion of "now" or of a cutoff date.
 */
public final class TransactionReplayer {

    private TransactionReplayer() {}

    public record HoldingState(Company company, int quantity, BigDecimal averageCost) {}

    public record RealizedDecision(
            Company company,
            int quantity,
            BigDecimal buyPrice,
            BigDecimal sellPrice,
            Instant executedAt,
            BigDecimal realizedGainPercent) {}

    public record ReplayResult(
            List<HoldingState> openHoldings, List<RealizedDecision> realizedDecisions, BigDecimal cashDelta) {}

    public static ReplayResult replay(List<OrderExecution> executionsAscending) {
        Map<Company, HoldingState> holdings = new LinkedHashMap<>();
        List<RealizedDecision> decisions = new ArrayList<>();
        BigDecimal cashDelta = BigDecimal.ZERO;

        for (OrderExecution execution : executionsAscending) {
            Company company = execution.getOrder().getCompany();
            OrderSide side = execution.getOrder().getSide();
            int quantity = execution.getExecutedQuantity();
            BigDecimal price = execution.getExecutionPrice();
            BigDecimal notional = price.multiply(BigDecimal.valueOf(quantity));

            if (side == OrderSide.BUY) {
                cashDelta = cashDelta.subtract(notional);
                holdings.merge(
                        company,
                        new HoldingState(company, quantity, price),
                        (existing, added) -> mergeBuy(existing, quantity, price));
            } else {
                HoldingState existing = holdings.get(company);
                if (existing == null || existing.quantity() < quantity) {
                    // Defensive: a sell with no matching prior buy in the replayed window shouldn't
                    // happen for a consistent execution history, so skip rather than fabricate a cost basis.
                    continue;
                }
                cashDelta = cashDelta.add(notional);
                decisions.add(new RealizedDecision(
                        company,
                        quantity,
                        existing.averageCost(),
                        price,
                        execution.getExecutedAt(),
                        realizedGainPercent(existing.averageCost(), price)));
                applySell(holdings, existing, quantity);
            }
        }

        return new ReplayResult(List.copyOf(holdings.values()), decisions, cashDelta);
    }

    private static HoldingState mergeBuy(HoldingState existing, int addedQuantity, BigDecimal addedPrice) {
        BigDecimal existingCost = existing.averageCost().multiply(BigDecimal.valueOf(existing.quantity()));
        BigDecimal addedCost = addedPrice.multiply(BigDecimal.valueOf(addedQuantity));
        int newQuantity = existing.quantity() + addedQuantity;
        BigDecimal newAverageCost =
                existingCost.add(addedCost).divide(BigDecimal.valueOf(newQuantity), 2, RoundingMode.HALF_UP);
        return new HoldingState(existing.company(), newQuantity, newAverageCost);
    }

    private static void applySell(Map<Company, HoldingState> holdings, HoldingState existing, int quantity) {
        int remaining = existing.quantity() - quantity;
        if (remaining == 0) {
            holdings.remove(existing.company());
        } else {
            holdings.put(existing.company(), new HoldingState(existing.company(), remaining, existing.averageCost()));
        }
    }

    private static BigDecimal realizedGainPercent(BigDecimal averageCost, BigDecimal sellPrice) {
        if (averageCost.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }
        return sellPrice
                .subtract(averageCost)
                .divide(averageCost, 8, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100))
                .setScale(4, RoundingMode.HALF_UP);
    }
}
