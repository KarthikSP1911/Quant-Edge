package com.quantedge.backend.service.export;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.quantedge.backend.dto.response.RealizedPnlLine;
import com.quantedge.backend.entity.Order;
import com.quantedge.backend.entity.OrderExecution;
import com.quantedge.backend.enums.OrderSide;
import org.springframework.stereotype.Service;

/**
 * Replays a user's executions in chronological order to derive realized gains/losses, using the
 * same running weighted-average-cost formula {@link
 * com.quantedge.backend.service.TradeExecutionService} uses to maintain {@code
 * Portfolio.averageCost} - so a sell's realized gain here always matches what the portfolio page
 * would have shown as cost basis at the moment of that sale. Average cost is unaffected by sells,
 * exactly as in the live portfolio (a full sell followed by a new buy starts a fresh average, per
 * {@code applyBuyToPortfolio}'s no-existing-position branch).
 */
@Service
public class PnlCalculationService {

    public List<RealizedPnlLine> calculateRealizedGains(List<OrderExecution> executionsChronological) {
        Map<String, LotState> stateBySymbol = new HashMap<>();
        List<RealizedPnlLine> realizedGains = new ArrayList<>();

        for (OrderExecution execution : executionsChronological) {
            Order order = execution.getOrder();
            String symbol = order.getCompany().getSymbol();
            LotState state = stateBySymbol.computeIfAbsent(symbol, s -> new LotState());
            int quantity = execution.getExecutedQuantity();
            BigDecimal price = execution.getExecutionPrice();

            if (order.getSide() == OrderSide.BUY) {
                state.applyBuy(quantity, price);
            } else {
                BigDecimal costBasis = state.averageCost;
                BigDecimal realizedGain = price.subtract(costBasis).multiply(BigDecimal.valueOf(quantity));
                realizedGains.add(new RealizedPnlLine(
                        symbol, execution.getExecutedAt(), quantity, price, costBasis, realizedGain));
                state.applySell(quantity);
            }
        }

        return realizedGains;
    }

    private static final class LotState {
        private int quantity = 0;
        private BigDecimal averageCost = BigDecimal.ZERO;

        void applyBuy(int addedQuantity, BigDecimal price) {
            BigDecimal existingCost = averageCost.multiply(BigDecimal.valueOf(quantity));
            BigDecimal addedCost = price.multiply(BigDecimal.valueOf(addedQuantity));
            int newQuantity = quantity + addedQuantity;
            averageCost = existingCost.add(addedCost).divide(BigDecimal.valueOf(newQuantity), 2, RoundingMode.HALF_UP);
            quantity = newQuantity;
        }

        void applySell(int soldQuantity) {
            quantity -= soldQuantity;
        }
    }
}
