package com.quantedge.backend.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.listener.ContainerProperties;
import org.springframework.kafka.support.serializer.JsonDeserializer;

/**
 * Manual-ack listener container factory for the order matcher (Part 2). Acking only after the
 * matcher's DB transaction commits gives at-least-once delivery with exactly-once effect, backed
 * by the order row lock + status guard rather than Kafka transactions.
 *
 * <p>{@code app.kafka.listener.auto-startup} (default true) lets the fast H2 test profile disable
 * container startup entirely, since there's no live broker for it to poll during those tests -
 * Spring Boot has no built-in {@code spring.kafka.listener.auto-startup} property to do this.
 */
@Configuration
public class KafkaConsumerConfig {

    @Bean
    public ConcurrentKafkaListenerContainerFactory<Object, Object> kafkaListenerContainerFactory(
            ConsumerFactory<Object, Object> consumerFactory,
            @Value("${app.kafka.listener.auto-startup:true}") boolean autoStartup) {
        ConcurrentKafkaListenerContainerFactory<Object, Object> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(consumerFactory);
        factory.getContainerProperties().setAckMode(ContainerProperties.AckMode.MANUAL);
        factory.setAutoStartup(autoStartup);
        return factory;
    }

    /**
     * Mirrors {@link KafkaProducerConfig}'s fix on the read side: Boot's autoconfigured {@link
     * DefaultKafkaConsumerFactory} builds its {@link JsonDeserializer} with a bare {@code new
     * ObjectMapper()}, which can't deserialize the {@code java.time.Instant} fields on {@code
     * PriceEventMessage}/{@code TradeExecutedMessage} - every price event would fail deserialization
     * without this.
     */
    @Bean
    public BeanPostProcessor kafkaJsonDeserializerJavaTimeSupport() {
        return new BeanPostProcessor() {
            @Override
            public Object postProcessAfterInitialization(Object bean, String beanName) {
                if (bean instanceof DefaultKafkaConsumerFactory<?, ?> factory) {
                    applyDeserializer(factory);
                }
                return bean;
            }

            @SuppressWarnings("unchecked")
            private <K, V> void applyDeserializer(DefaultKafkaConsumerFactory<K, V> factory) {
                factory.setValueDeserializer((JsonDeserializer<V>)
                        new JsonDeserializer<>(new ObjectMapper().registerModule(new JavaTimeModule()))
                                .trustedPackages("com.quantedge.backend.kafka.dto"));
            }
        };
    }
}
