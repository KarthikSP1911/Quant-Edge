package com.quantedge.backend.kafka.producer;

import com.quantedge.backend.entity.Order;
import com.quantedge.backend.entity.OrderExecution;
import com.quantedge.backend.kafka.dto.TradeExecutedMessage;
import java.time.Instant;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * Publishes a fill to {@code executed-trades} only after the matcher's DB transaction commits, so
 * a rolled-back fill never produces a trade event.
 */
@Component
public class TradeExecutedProducer {

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final String topic;

    public TradeExecutedProducer(
            KafkaTemplate<String, Object> kafkaTemplate, @Value("${app.kafka.topic.executed-trades}") String topic) {
        this.kafkaTemplate = kafkaTemplate;
        this.topic = topic;
    }

    public void publishAfterCommit(OrderExecution execution) {
        Order order = execution.getOrder();
        Runnable send = () -> kafkaTemplate.send(
                topic,
                order.getCompany().getSymbol(),
                new TradeExecutedMessage(
                        execution.getId(),
                        order.getId(),
                        order.getUser().getId(),
                        order.getCompany().getSymbol(),
                        order.getSide(),
                        execution.getExecutedQuantity(),
                        execution.getExecutionPrice(),
                        Instant.now()));

        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    send.run();
                }
            });
        } else {
            send.run();
        }
    }
}
