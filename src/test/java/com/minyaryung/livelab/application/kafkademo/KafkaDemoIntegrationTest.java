package com.minyaryung.livelab.application.kafkademo;

import com.minyaryung.livelab.domain.kafkademo.OrderEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.kafka.KafkaContainer;
import org.testcontainers.utility.DockerImageName;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

@SpringBootTest
@Testcontainers(disabledWithoutDocker = true)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class KafkaDemoIntegrationTest {

    @Container
    static final KafkaContainer kafka = new KafkaContainer(DockerImageName.parse("apache/kafka:3.7.0"));

    @Container
    static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>(DockerImageName.parse("postgres:16-alpine"));

    @DynamicPropertySource
    static void wireContainers(DynamicPropertyRegistry registry) {
        registry.add("spring.kafka.bootstrap-servers", kafka::getBootstrapServers);
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("livelab.demo.product-count", () -> "0");
        registry.add("spring.cache.type", () -> "none");
        registry.add("spring.autoconfigure.exclude",
                () -> "org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration");
        registry.add("livelab.kafka.consumer.retry.interval-ms", () -> "100");
    }

    @Autowired OrderProducer producer;
    @Autowired KafkaMetricsService metrics;

    @BeforeEach
    void reset() { metrics.reset(); }

    @Test
    void successMessagesAreConsumed() {
        producer.send(new OrderEvent(1L, "shoes", 2));
        producer.send(new OrderEvent(2L, "book", 1));
        producer.send(new OrderEvent(3L, "snack", 5));
        await().atMost(Duration.ofSeconds(30)).untilAsserted(() ->
                assertThat(metrics.snapshot().success()).isEqualTo(3));
        assertThat(metrics.snapshot().dlt()).isZero();
    }

    @Test
    void failedMessagesAreRoutedToDltAfterRetries() {
        producer.send(new OrderEvent(17L, "shoes", 1));
        await().atMost(Duration.ofSeconds(30)).untilAsserted(() ->
                assertThat(metrics.snapshot().dlt()).isEqualTo(1));
        assertThat(metrics.snapshot().success()).isZero();
    }

    @Test
    void mixedBatchSplitsBetweenSuccessAndDlt() {
        producer.send(new OrderEvent(17L, "x", 1));
        producer.send(new OrderEvent(18L, "x", 1));
        producer.send(new OrderEvent(34L, "x", 1));
        producer.send(new OrderEvent(35L, "x", 1));
        await().atMost(Duration.ofSeconds(60)).untilAsserted(() -> {
            var snap = metrics.snapshot();
            assertThat(snap.success()).isEqualTo(2);
            assertThat(snap.dlt()).isEqualTo(2);
        });
    }
}
