package com.minyaryung.livelab.presentation.kafkademo;

import com.minyaryung.livelab.application.kafkademo.KafkaMetricsService;
import com.minyaryung.livelab.application.kafkademo.OrderProducer;
import com.minyaryung.livelab.infra.common.PublicDemoRateLimiter;
import org.junit.jupiter.api.Test;
import org.springframework.kafka.support.SendResult;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.server.ResponseStatusException;

import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
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
        KafkaDemoController controller = controller(producer, metrics);

        KafkaMetricsService.Snapshot snapshot = controller.publish(3, request());

        assertThat(snapshot.attempted()).isEqualTo(3);
        assertThat(snapshot.acknowledged()).isEqualTo(2);
        assertThat(snapshot.publishFailed()).isEqualTo(1);
    }

    @Test
    void publishCountsSynchronousSendFailure() {
        OrderProducer producer = mock(OrderProducer.class);
        when(producer.send(any())).thenThrow(new IllegalStateException("producer closed"));

        KafkaMetricsService metrics = new KafkaMetricsService();
        KafkaDemoController controller = controller(producer, metrics);

        KafkaMetricsService.Snapshot snapshot = controller.publish(1, request());

        assertThat(snapshot.attempted()).isEqualTo(1);
        assertThat(snapshot.acknowledged()).isZero();
        assertThat(snapshot.publishFailed()).isEqualTo(1);
    }

    @Test
    void rejectsAnotherPublishWhileRunIsActive() {
        OrderProducer producer = mock(OrderProducer.class);
        when(producer.send(any())).thenReturn(CompletableFuture.completedFuture(null));

        KafkaDemoController controller = controller(producer, new KafkaMetricsService());
        KafkaMetricsService.Snapshot first = controller.publish(1, request());

        assertThat(first.active()).isTrue();
        assertThatThrownBy(() -> controller.publish(1, request()))
                .isInstanceOf(org.springframework.web.server.ResponseStatusException.class)
                .extracting(ex -> ((org.springframework.web.server.ResponseStatusException) ex)
                        .getStatusCode().value())
                .isEqualTo(409);
    }

    @Test
    void rejectsPublishBeforeSendingWhenPeerBudgetIsExhausted() {
        OrderProducer producer = mock(OrderProducer.class);
        PublicDemoRateLimiter limiter = mock(PublicDemoRateLimiter.class);
        when(limiter.tryKafkaPublish("203.0.113.10", 100)).thenReturn(false);
        KafkaDemoController controller = new KafkaDemoController(
                producer, new KafkaMetricsService(), limiter);

        assertThatThrownBy(() -> controller.publish(100, request()))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(ex -> ((ResponseStatusException) ex).getStatusCode().value())
                .isEqualTo(429);
        verify(producer, never()).send(any());
    }

    private static KafkaDemoController controller(OrderProducer producer, KafkaMetricsService metrics) {
        PublicDemoRateLimiter limiter = mock(PublicDemoRateLimiter.class);
        when(limiter.tryKafkaPublish(any(), anyInt())).thenReturn(true);
        return new KafkaDemoController(producer, metrics, limiter);
    }

    private static MockHttpServletRequest request() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("203.0.113.10");
        return request;
    }
}
