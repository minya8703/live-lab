package com.minyaryung.livelab.application.kafkademo;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class KafkaMetricsServiceTest {

    private final KafkaMetricsService metrics = new KafkaMetricsService();

    @Test
    void ignoresLateCallbacksFromPreviousRun() {
        String firstRun = metrics.beginRun(1);
        metrics.recordSuccess(firstRun);

        String secondRun = metrics.beginRun(1);
        metrics.recordAcknowledged(firstRun);
        metrics.recordDlt(firstRun);

        KafkaMetricsService.Snapshot snapshot = metrics.snapshot(secondRun);
        assertThat(snapshot.acknowledged()).isZero();
        assertThat(snapshot.dlt()).isZero();
        assertThat(snapshot.active()).isTrue();
    }

    @Test
    void rejectsResetWhileRunIsActive() {
        String runId = metrics.beginRun(1);

        assertThatThrownBy(() -> metrics.reset(runId))
                .isInstanceOf(KafkaMetricsService.RunAlreadyActiveException.class);
    }

    @Test
    void resetsOnlyTheCompletedMatchingRun() {
        String runId = metrics.beginRun(1);
        metrics.recordPublishFailed(runId);

        assertThat(metrics.snapshot(runId).active()).isFalse();
        assertThat(metrics.snapshot(runId).completed()).isTrue();
        metrics.reset(runId);

        assertThatThrownBy(() -> metrics.snapshot(runId))
                .isInstanceOf(KafkaMetricsService.RunNotFoundException.class);
    }
}
