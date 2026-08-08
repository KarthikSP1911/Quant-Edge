package com.quantedge.backend.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import com.quantedge.backend.entity.Company;
import com.quantedge.backend.entity.Order;
import com.quantedge.backend.entity.User;
import com.quantedge.backend.enums.AuthProvider;
import com.quantedge.backend.enums.OrderSide;
import com.quantedge.backend.enums.OrderStatus;
import com.quantedge.backend.enums.OrderType;
import com.quantedge.backend.enums.Role;
import com.quantedge.backend.enums.TimeInForce;
import com.quantedge.backend.kafka.dto.PriceEventMessage;
import com.quantedge.backend.kafka.dto.TradeExecutedMessage;
import com.quantedge.backend.repository.CompanyRepository;
import com.quantedge.backend.repository.OrderExecutionRepository;
import com.quantedge.backend.repository.OrderRepository;
import com.quantedge.backend.repository.PortfolioRepository;
import com.quantedge.backend.repository.UserRepository;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.KafkaMessageListenerContainer;
import org.springframework.kafka.listener.MessageListener;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.kafka.ConfluentKafkaContainer;

/**
 * Proves the matcher end to end against real Postgres + Kafka: a crossing price event fills an
 * OPEN limit order exactly once (balance, portfolio, order status, executed-trades event), a
 * redelivered price event does not double-fill it, and a non-crossing price event leaves an OPEN
 * order untouched.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@Testcontainers
class OrderMatcherIntegrationIT {

    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:15-alpine");

    @Container
    static final ConfluentKafkaContainer KAFKA = new ConfluentKafkaContainer("confluentinc/cp-kafka:7.5.0");

    @DynamicPropertySource
    static void overrideProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("spring.datasource.driver-class-name", () -> "org.postgresql.Driver");
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "validate");
        registry.add("spring.flyway.enabled", () -> "true");
        registry.add("spring.kafka.bootstrap-servers", KAFKA::getBootstrapServers);
        registry.add("app.kafka.listener.auto-startup", () -> "true");
    }

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CompanyRepository companyRepository;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private OrderExecutionRepository orderExecutionRepository;

    @Autowired
    private PortfolioRepository portfolioRepository;

    @Autowired
    private KafkaTemplate<String, Object> kafkaTemplate;

    private BlockingQueue<ConsumerRecord<String, TradeExecutedMessage>> executedTrades;
    private KafkaMessageListenerContainer<String, TradeExecutedMessage> executedTradesListener;

    private User user;
    private Company company;

    @BeforeEach
    void setUp() {
        company = companyRepository.save(Company.builder()
                .symbol("AAPL")
                .name("Apple Inc.")
                .sector("Technology")
                .industry("Consumer Electronics")
                .exchange("NASDAQ")
                .build());

        user = userRepository.save(User.builder()
                .email("trader-" + UUID.randomUUID() + "@quantedge.test")
                .passwordHash("hash")
                .name("Test Trader")
                .balance(new BigDecimal("10000.00"))
                .role(Role.USER)
                .authProvider(AuthProvider.LOCAL)
                .emailVerified(true)
                .build());

        executedTrades = new LinkedBlockingQueue<>();
        executedTradesListener = startExecutedTradesListener();
    }

    @AfterEach
    void tearDown() {
        executedTradesListener.stop();
    }

    @Test
    void crossingPriceEventFillsExactlyOnceAndIsNotDoubleFilledOnRedelivery() {
        Order order = orderRepository.save(Order.builder()
                .user(user)
                .company(company)
                .side(OrderSide.BUY)
                .type(OrderType.LIMIT)
                .quantity(10)
                .status(OrderStatus.OPEN)
                .limitPrice(new BigDecimal("180.00"))
                .timeInForce(TimeInForce.GTC)
                .idempotencyKey(UUID.randomUUID())
                .build());

        PriceEventMessage crossingPrice = new PriceEventMessage("AAPL", new BigDecimal("179.50"), Instant.now());
        kafkaTemplate.send("stock-prices", "AAPL", crossingPrice);

        await().atMost(Duration.ofSeconds(20)).untilAsserted(() -> {
            Order refreshed = orderRepository.findById(order.getId()).orElseThrow();
            assertThat(refreshed.getStatus()).isEqualTo(OrderStatus.FILLED);
        });

        assertThat(orderExecutionRepository.findTop10ByOrderUserOrderByExecutedAtDesc(user))
                .hasSize(1);

        User refreshedUser = userRepository.findById(user.getId()).orElseThrow();
        assertThat(refreshedUser.getBalance()).isEqualByComparingTo("8205.00"); // 10000 - 10 * 179.50

        assertThat(portfolioRepository.findByUserAndCompany(refreshedUser, company))
                .hasValueSatisfying(portfolio -> {
                    assertThat(portfolio.getQuantity()).isEqualTo(10);
                    assertThat(portfolio.getAverageCost()).isEqualByComparingTo("179.50");
                });

        ConsumerRecord<String, TradeExecutedMessage> firstTrade = pollExecutedTrade();
        assertThat(firstTrade.value().orderId()).isEqualTo(order.getId());
        assertThat(pollExecutedTradeOrNull()).isNull(); // exactly one message, not more

        // Redeliver the identical price event - the double-fill guard must no-op this.
        kafkaTemplate.send("stock-prices", "AAPL", crossingPrice);

        // Give the redelivered message time to be processed, then assert nothing changed.
        await().pollDelay(Duration.ofSeconds(3)).atMost(Duration.ofSeconds(10)).untilAsserted(() -> {
            assertThat(orderExecutionRepository.findTop10ByOrderUserOrderByExecutedAtDesc(user))
                    .hasSize(1);
            assertThat(userRepository.findById(user.getId()).orElseThrow().getBalance())
                    .isEqualByComparingTo("8205.00");
        });
        assertThat(pollExecutedTradeOrNull()).isNull();
    }

    @Test
    void nonCrossingPriceEventDoesNotExecute() {
        Order order = orderRepository.save(Order.builder()
                .user(user)
                .company(company)
                .side(OrderSide.BUY)
                .type(OrderType.LIMIT)
                .quantity(5)
                .status(OrderStatus.OPEN)
                .limitPrice(new BigDecimal("100.00"))
                .timeInForce(TimeInForce.GTC)
                .idempotencyKey(UUID.randomUUID())
                .build());

        kafkaTemplate.send(
                "stock-prices", "AAPL", new PriceEventMessage("AAPL", new BigDecimal("150.00"), Instant.now()));

        // No trigger expected - wait out a processing window, then assert nothing changed.
        await().pollDelay(Duration.ofSeconds(3)).atMost(Duration.ofSeconds(10)).untilAsserted(() -> {
            Order refreshed = orderRepository.findById(order.getId()).orElseThrow();
            assertThat(refreshed.getStatus()).isEqualTo(OrderStatus.OPEN);
        });
        assertThat(orderExecutionRepository.findTop10ByOrderUserOrderByExecutedAtDesc(user))
                .isEmpty();
        assertThat(pollExecutedTradeOrNull()).isNull();
    }

    private ConsumerRecord<String, TradeExecutedMessage> pollExecutedTrade() {
        try {
            ConsumerRecord<String, TradeExecutedMessage> record = executedTrades.poll(20, TimeUnit.SECONDS);
            assertThat(record).isNotNull();
            return record;
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(ex);
        }
    }

    private ConsumerRecord<String, TradeExecutedMessage> pollExecutedTradeOrNull() {
        try {
            return executedTrades.poll(2, TimeUnit.SECONDS);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(ex);
        }
    }

    private KafkaMessageListenerContainer<String, TradeExecutedMessage> startExecutedTradesListener() {
        Map<String, Object> consumerProps = Map.of(
                ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG,
                KAFKA.getBootstrapServers(),
                ConsumerConfig.GROUP_ID_CONFIG,
                "test-executed-trades-" + UUID.randomUUID(),
                ConsumerConfig.AUTO_OFFSET_RESET_CONFIG,
                "earliest",
                ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG,
                StringDeserializer.class,
                ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG,
                JsonDeserializer.class,
                JsonDeserializer.TRUSTED_PACKAGES,
                "com.quantedge.backend.kafka.dto",
                JsonDeserializer.VALUE_DEFAULT_TYPE,
                TradeExecutedMessage.class.getName(),
                JsonDeserializer.USE_TYPE_INFO_HEADERS,
                false);

        DefaultKafkaConsumerFactory<String, TradeExecutedMessage> consumerFactory =
                new DefaultKafkaConsumerFactory<>(consumerProps);
        org.springframework.kafka.listener.ContainerProperties containerProperties =
                new org.springframework.kafka.listener.ContainerProperties("executed-trades");
        KafkaMessageListenerContainer<String, TradeExecutedMessage> container =
                new KafkaMessageListenerContainer<>(consumerFactory, containerProperties);
        container.setupMessageListener((MessageListener<String, TradeExecutedMessage>) executedTrades::add);
        container.start();
        return container;
    }
}
