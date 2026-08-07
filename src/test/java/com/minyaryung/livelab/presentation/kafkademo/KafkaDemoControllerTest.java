package com.minyaryung.livelab.presentation.kafkademo;

import com.minyaryung.livelab.application.kafkademo.KafkaMetricsService;
import com.minyaryung.livelab.application.kafkademo.OrderProducer;
import org.junit.jupiter.api.Test;
import org.springframework.kafka.support.SendResult;

import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class KafkaDemoControllerTest {

    @Test
    void publishSeparatesAttemptedAcknowledgedAndFailedCounts() {
        OrderProducer producer = mock(OrderProducer.class);
        CompletableFuture<SendResult<String, Object>> acknowledged = CompletableFuture.completedFuture(null);
        CompletableFuture<SendResult<String, Object>> failed = new CompletableFuture<>();
        failed.completeExceptionally(new IllegalStateException("broker unavailable"));
        when(producer.send(any()))
                .thenReturn(acknowledged)
                .thenReturn(failed)
                .thenReturn(acknowledged);

        KafkaMetricsService metrics = new KafkaMetricsService();
        KafkaDemoController controller = new KafkaDemoController(producer, metrics);

        KafkaMetricsService.Snapshot snapshot = controller.publish(3);

        assertThat(snapshot.attempted()).isEqualTo(3);
        assertThat(snapshot.acknowledged()).isEqualTo(2);
        assertThat(snapshot.publishFailed()).isEqualTo(1);
    }

    @Test
    void publishCountsSynchronousSendFailure() {
        OrderProducer producer = mock(OrderProducer.class);
        when(producer.send(any())).thenThrow(new IllegalStateException("producer closed"));

        KafkaMetricsService metrics = new KafkaMetricsService();
        KafkaDemoController controller = new KafkaDemoController(producer, metrics);

        KafkaMetricsService.Snapshot snapshot = controller.publish(1);

        assertThat(snapshot.attempted()).isEqualTo(1);
        assertThat(snapshot.acknowledged()).isZero();
        assertThat(snapshot.publishFailed()).isEqualTo(1);
    }
}
