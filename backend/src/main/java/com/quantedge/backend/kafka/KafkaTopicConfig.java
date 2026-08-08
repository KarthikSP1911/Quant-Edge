package com.quantedge.backend.kafka;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaTopicConfig {

    @Bean
    public NewTopic stockPricesTopic(@Value("${app.kafka.topic.stock-prices}") String name) {
        return TopicBuilder.name(name).build();
    }

    @Bean
    public NewTopic executedTradesTopic(@Value("${app.kafka.topic.executed-trades}") String name) {
        return TopicBuilder.name(name).build();
    }
}
