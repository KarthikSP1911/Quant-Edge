package com.quantedge.backend.config;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.apache.kafka.clients.admin.AdminClient;
import org.springframework.boot.health.contributor.Health;
import org.springframework.boot.health.contributor.HealthIndicator;
import org.springframework.kafka.core.KafkaAdmin;
import org.springframework.stereotype.Component;

/**
 * Reports Kafka as up only if the cluster actually responds - Boot has no autoconfigured Kafka
 * health check, so this hits {@code AdminClient.describeCluster()} directly with a short timeout
 * rather than trusting an idle producer/consumer factory to mean the broker is reachable.
 *
 * <p>Built from the autoconfigured {@link KafkaAdmin}'s configuration map rather than just
 * {@code spring.kafka.bootstrap-servers}, so the SASL_SSL/truststore settings Boot wires up for
 * Aiven (or PLAINTEXT for local Docker Kafka) are reused as-is instead of duplicated here.
 */
@Component
public class KafkaHealthIndicator implements HealthIndicator {

    private static final long TIMEOUT_SECONDS = 3;

    private final KafkaAdmin kafkaAdmin;

    public KafkaHealthIndicator(KafkaAdmin kafkaAdmin) {
        this.kafkaAdmin = kafkaAdmin;
    }

    @Override
    public Health health() {
        try (AdminClient adminClient = AdminClient.create(kafkaAdmin.getConfigurationProperties())) {
            String clusterId = adminClient.describeCluster().clusterId().get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
            return Health.up().withDetail("clusterId", clusterId).build();
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            return Health.down(ex).build();
        } catch (ExecutionException | TimeoutException ex) {
            return Health.down(ex).build();
        }
    }
}
