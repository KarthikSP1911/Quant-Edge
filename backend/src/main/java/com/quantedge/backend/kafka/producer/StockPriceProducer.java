package com.quantedge.backend.kafka.producer;

import java.math.BigDecimal;
import java.time.Instant;

import com.quantedge.backend.kafka.dto.PriceEventMessage;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

/** Publishes a price event after every successful per-symbol price sync. */
@Component
public class StockPriceProducer {

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final String topic;

    public StockPriceProducer(
            KafkaTemplate<String, Object> kafkaTemplate, @Value("${app.kafka.topic.stock-prices}") String topic) {
        this.kafkaTemplate = kafkaTemplate;
        this.topic = topic;
    }

    public void publish(String symbol, BigDecimal price) {
        kafkaTemplate.send(topic, symbol, new PriceEventMessage(symbol, price, Instant.now()));
    }
}
