package com.minyaryung.livelab.kafkademo;

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

// 진짜 Kafka 컨테이너로 발행·소비·재시도·DLT 라우팅 전체 흐름 검증.
// FixedBackOff 와 DeadLetterPublishingRecoverer 가 의도대로 동작하는지를 unit 이 아닌 integration 으로 잡는다.
@SpringBootTest
@Testcontainers
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class KafkaDemoIntegrationTest {

    @Container
    static final KafkaContainer kafka = new KafkaContainer(
            DockerImageName.parse("apache/kafka:3.7.0"));

    // ProductService 자동 와이어링이 Postgres 를 요구하므로 같이 띄움.
    @Container
    static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>(
            DockerImageName.parse("postgres:16-alpine"));

    @DynamicPropertySource
    static void wireContainers(DynamicPropertyRegistry registry) {
        registry.add("spring.kafka.bootstrap-servers", kafka::getBootstrapServers);
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        // 시드는 안 돌아도 됨 — Kafka 테스트가 ProductService 를 안 쓰므로
        registry.add("livelab.demo.product-count", () -> "0");
        // 테스트에서는 캐시 비활성화 — Redis 컨테이너 안 띄우려고
        registry.add("spring.cache.type", () -> "none");
        registry.add("spring.autoconfigure.exclude",
                () -> "org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration");
        // 재시도 간격은 짧게 (테스트 빠르게)
        registry.add("livelab.kafka.consumer.retry.interval-ms", () -> "100");
    }

    @Autowired OrderProducer producer;
    @Autowired KafkaMetricsService metrics;

    @BeforeEach
    void reset() {
        metrics.reset();
    }

    @Test
    void successMessagesAreConsumed() {
        // orderId 가 17의 배수가 아닌 메시지 → 성공 소비
        producer.send(new OrderEvent(1L, "shoes", 2));
        producer.send(new OrderEvent(2L, "book", 1));
        producer.send(new OrderEvent(3L, "snack", 5));

        await().atMost(Duration.ofSeconds(30)).untilAsserted(() ->
                assertThat(metrics.snapshot().success()).isEqualTo(3));
        assertThat(metrics.snapshot().dlt()).isZero();
    }

    @Test
    void failedMessagesAreRoutedToDltAfterRetries() {
        // orderId == 17 → 의도적 실패 → FixedBackOff 재시도 → DLT 라우팅
        producer.send(new OrderEvent(17L, "shoes", 1));

        await().atMost(Duration.ofSeconds(30)).untilAsserted(() ->
                assertThat(metrics.snapshot().dlt()).isEqualTo(1));
        assertThat(metrics.snapshot().success()).isZero();
    }

    @Test
    void mixedBatchSplitsBetweenSuccessAndDlt() {
        // 17, 34 = DLT 행 / 그 외 = 성공
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
