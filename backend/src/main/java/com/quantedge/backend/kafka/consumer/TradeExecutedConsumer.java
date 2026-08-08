package com.quantedge.backend.kafka.consumer;

import com.quantedge.backend.kafka.dto.TradeExecutedMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

/**
 * Consumes {@code executed-trades}. Balance/portfolio/order state is already applied atomically by
 * the matcher's fill transaction ({@code TradeExecutionService}) before this event is published, so
 * this consumer does no state mutation - it's the Part 3 SSE hand-off point only, which is why it
 * needs no idempotency guard of its own.
 */
@Component
public class TradeExecutedConsumer {

    private static final Logger log = LoggerFactory.getLogger(TradeExecutedConsumer.class);

    @KafkaListener(topics = "${app.kafka.topic.executed-trades}")
    public void onTradeExecuted(TradeExecutedMessage message, Acknowledgment ack) {
        log.info(
                "Trade executed: order={} symbol={} side={} qty={} price={}",
                message.orderId(),
                message.symbol(),
                message.side(),
                message.quantity(),
                message.price());
        ack.acknowledge();
    }
}
