package com.quantedge.backend.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;

import com.quantedge.backend.entity.Order;
import com.quantedge.backend.enums.OrderSide;
import com.quantedge.backend.enums.OrderType;
import com.quantedge.backend.service.OrderTriggerEvaluator.Trigger;
import org.junit.jupiter.api.Test;

class OrderTriggerEvaluatorTest {

    private static Order order(OrderSide side, OrderType type, BigDecimal limitPrice, BigDecimal stopPrice) {
        return Order.builder()
                .side(side)
                .type(type)
                .limitPrice(limitPrice)
                .stopPrice(stopPrice)
                .build();
    }

    @Test
    void limitBuyFillsWhenPriceAtOrBelowLimit() {
        Order order = order(OrderSide.BUY, OrderType.LIMIT, new BigDecimal("100.00"), null);

        Trigger trigger = OrderTriggerEvaluator.evaluate(order, new BigDecimal("99.00"));

        assertThat(trigger.fills()).isTrue();
        assertThat(trigger.convertsToLimit()).isFalse();
        assertThat(trigger.fillPrice()).isEqualByComparingTo("99.00");
    }

    @Test
    void limitBuyFillsAtLimitOnExactPriceTie() {
        Order order = order(OrderSide.BUY, OrderType.LIMIT, new BigDecimal("100.00"), null);

        Trigger trigger = OrderTriggerEvaluator.evaluate(order, new BigDecimal("100.00"));

        assertThat(trigger.fills()).isTrue();
        assertThat(trigger.fillPrice()).isEqualByComparingTo("100.00");
    }

    @Test
    void limitBuyDoesNotFillWhenPriceAboveLimit() {
        Order order = order(OrderSide.BUY, OrderType.LIMIT, new BigDecimal("100.00"), null);

        Trigger trigger = OrderTriggerEvaluator.evaluate(order, new BigDecimal("100.01"));

        assertThat(trigger.fills()).isFalse();
        assertThat(trigger.convertsToLimit()).isFalse();
    }

    @Test
    void limitSellFillsWhenPriceAtOrAboveLimit() {
        Order order = order(OrderSide.SELL, OrderType.LIMIT, new BigDecimal("100.00"), null);

        Trigger trigger = OrderTriggerEvaluator.evaluate(order, new BigDecimal("101.00"));

        assertThat(trigger.fills()).isTrue();
        assertThat(trigger.fillPrice()).isEqualByComparingTo("101.00");
    }

    @Test
    void limitSellDoesNotFillWhenPriceBelowLimit() {
        Order order = order(OrderSide.SELL, OrderType.LIMIT, new BigDecimal("100.00"), null);

        Trigger trigger = OrderTriggerEvaluator.evaluate(order, new BigDecimal("99.99"));

        assertThat(trigger.fills()).isFalse();
    }

    @Test
    void stopLossSellFillsAtSyncedPriceWhenPriceDropsToOrBelowStop() {
        Order order = order(OrderSide.SELL, OrderType.STOP_LOSS, null, new BigDecimal("50.00"));

        Trigger trigger = OrderTriggerEvaluator.evaluate(order, new BigDecimal("49.50"));

        assertThat(trigger.fills()).isTrue();
        assertThat(trigger.convertsToLimit()).isFalse();
        assertThat(trigger.fillPrice()).isEqualByComparingTo("49.50");
    }

    @Test
    void stopLossSellDoesNotFireWhilePriceStaysAboveStop() {
        Order order = order(OrderSide.SELL, OrderType.STOP_LOSS, null, new BigDecimal("50.00"));

        Trigger trigger = OrderTriggerEvaluator.evaluate(order, new BigDecimal("50.01"));

        assertThat(trigger.fills()).isFalse();
        assertThat(trigger.convertsToLimit()).isFalse();
    }

    @Test
    void stopLossBuyFillsAtSyncedPriceWhenPriceRisesToOrAboveStop() {
        Order order = order(OrderSide.BUY, OrderType.STOP_LOSS, null, new BigDecimal("50.00"));

        Trigger trigger = OrderTriggerEvaluator.evaluate(order, new BigDecimal("50.00"));

        assertThat(trigger.fills()).isTrue();
        assertThat(trigger.fillPrice()).isEqualByComparingTo("50.00");
    }

    @Test
    void stopLimitConvertsToRestingLimitInsteadOfFillingWhenStopCrossed() {
        Order order = order(OrderSide.SELL, OrderType.STOP_LIMIT, new BigDecimal("48.00"), new BigDecimal("50.00"));

        Trigger trigger = OrderTriggerEvaluator.evaluate(order, new BigDecimal("49.50"));

        assertThat(trigger.fills()).isFalse();
        assertThat(trigger.convertsToLimit()).isTrue();
        assertThat(trigger.fillPrice()).isNull();
    }

    @Test
    void stopLimitDoesNothingWhileStopNotCrossed() {
        Order order = order(OrderSide.SELL, OrderType.STOP_LIMIT, new BigDecimal("48.00"), new BigDecimal("50.00"));

        Trigger trigger = OrderTriggerEvaluator.evaluate(order, new BigDecimal("50.01"));

        assertThat(trigger.fills()).isFalse();
        assertThat(trigger.convertsToLimit()).isFalse();
    }

    @Test
    void marketOrderNeverTriggers() {
        Order order = order(OrderSide.BUY, OrderType.MARKET, null, null);

        Trigger trigger = OrderTriggerEvaluator.evaluate(order, new BigDecimal("123.45"));

        assertThat(trigger.fills()).isFalse();
        assertThat(trigger.convertsToLimit()).isFalse();
    }
}
