package com.quantedge.backend.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.support.serializer.JsonSerializer;

/**
 * Boot's autoconfigured {@link DefaultKafkaProducerFactory} builds its {@link JsonSerializer} with
 * a bare {@code new ObjectMapper()}, which can't serialize {@code java.time.Instant} - both
 * {@code PriceEventMessage} and {@code TradeExecutedMessage} carry one. Swapping the serializer in
 * via a {@link BeanPostProcessor} (rather than replacing the whole {@code ProducerFactory} bean)
 * keeps Boot's SASL_SSL/bundle wiring intact instead of having to reproduce it by hand.
 */
@Configuration
public class KafkaProducerConfig {

    @Bean
    public BeanPostProcessor kafkaJsonSerializerJavaTimeSupport() {
        return new BeanPostProcessor() {
            @Override
            public Object postProcessAfterInitialization(Object bean, String beanName) {
                if (bean instanceof DefaultKafkaProducerFactory<?, ?> factory) {
                    factory.setValueSerializer(
                            new JsonSerializer<>(new ObjectMapper().registerModule(new JavaTimeModule())));
                }
                return bean;
            }
        };
    }
}
