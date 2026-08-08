package com.quantedge.backend.kafka.consumer;

import com.quantedge.backend.kafka.dto.PriceEventMessage;
import com.quantedge.backend.service.OrderMatcherService;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

/** Consumes {@code stock-prices} and hands each event to the matcher; pure orchestration. */
@Component
@RequiredArgsConstructor
public class OrderMatcherConsumer {

    private final OrderMatcherService orderMatcherService;

    @KafkaListener(topics = "${app.kafka.topic.stock-prices}")
    public void onPriceEvent(PriceEventMessage message, Acknowledgment ack) {
        orderMatcherService.matchSymbol(message.symbol(), message.price());
        ack.acknowledge();
    }
}
