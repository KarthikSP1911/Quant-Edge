package com.quantedge.backend.service;

import com.quantedge.backend.entity.Order;
import com.quantedge.backend.enums.OrderSide;
import java.math.BigDecimal;

/**
 * Trigger rules agreed for the Phase 3 matcher: limit buy fires at price &lt;= limit, limit sell at
 * price &gt;= limit, filling at min/max(limit, synced price). Stop-loss fires when the synced price
 * crosses the stop and fills immediately at the synced price (becomes a market order). Stop-limit
 * fires at the same stop condition but does not fill - it converts to a resting LIMIT order,
 * re-evaluated on the next price tick.
 */
public final class OrderTriggerEvaluator {

    private OrderTriggerEvaluator() {}

    public record Trigger(boolean fills, boolean convertsToLimit, BigDecimal fillPrice) {
        static final Trigger NONE = new Trigger(false, false, null);

        static Trigger fillAt(BigDecimal price) {
            return new Trigger(true, false, price);
        }

        static Trigger convertToLimit() {
            return new Trigger(false, true, null);
        }
    }

    public static Trigger evaluate(Order order, BigDecimal syncedPrice) {
        return switch (order.getType()) {
            case LIMIT -> evaluateLimit(order, syncedPrice);
            case STOP_LOSS -> stopCrossed(order, syncedPrice) ? Trigger.fillAt(syncedPrice) : Trigger.NONE;
            case STOP_LIMIT -> stopCrossed(order, syncedPrice) ? Trigger.convertToLimit() : Trigger.NONE;
            case MARKET -> Trigger.NONE;
        };
    }

    private static Trigger evaluateLimit(Order order, BigDecimal syncedPrice) {
        BigDecimal limit = order.getLimitPrice();
        if (order.getSide() == OrderSide.BUY) {
            return syncedPrice.compareTo(limit) <= 0 ? Trigger.fillAt(limit.min(syncedPrice)) : Trigger.NONE;
        }
        return syncedPrice.compareTo(limit) >= 0 ? Trigger.fillAt(limit.max(syncedPrice)) : Trigger.NONE;
    }

    private static boolean stopCrossed(Order order, BigDecimal syncedPrice) {
        BigDecimal stop = order.getStopPrice();
        return order.getSide() == OrderSide.SELL ? syncedPrice.compareTo(stop) <= 0 : syncedPrice.compareTo(stop) >= 0;
    }
}
