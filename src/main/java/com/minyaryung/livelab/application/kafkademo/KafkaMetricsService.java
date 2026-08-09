package com.minyaryung.livelab.application.kafkademo;

import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

@Service
public class KafkaMetricsService {

    private static final long RUN_TIMEOUT_MS = Duration.ofMinutes(15).toMillis();

    private final AtomicLong orderIdSeq = new AtomicLong();
    private RunState current;

    public long nextOrderId() {
        return orderIdSeq.incrementAndGet();
    }

    public synchronized String beginRun(int attempted) {
        long now = System.currentTimeMillis();
        if (current != null && current.isActive(now)) {
            throw new RunAlreadyActiveException(current.runId);
        }

        String runId = UUID.randomUUID().toString();
        current = new RunState(runId, attempted, now);
        return runId;
    }

    public synchronized void recordAcknowledged(String runId) {
        if (matches(runId)) current.acknowledged++;
    }

    public synchronized void recordPublishFailed(String runId) {
        if (matches(runId)) current.publishFailed++;
    }

    public synchronized void recordSuccess(String runId) {
        if (matches(runId)) {
            current.success++;
            current.lastEventMs = System.currentTimeMillis();
        }
    }

    public synchronized void recordDlt(String runId) {
        if (matches(runId)) {
            current.dlt++;
            current.lastEventMs = System.currentTimeMillis();
        }
    }

    public synchronized Snapshot snapshot(String runId) {
        if (!matches(runId)) throw new RunNotFoundException(runId);
        return current.snapshot(System.currentTimeMillis());
    }

    public synchronized void reset(String runId) {
        if (!matches(runId)) throw new RunNotFoundException(runId);
        if (current.isActive(System.currentTimeMillis())) {
            throw new RunAlreadyActiveException(runId);
        }
        current = null;
    }

    private boolean matches(String runId) {
        return runId != null && current != null && runId.equals(current.runId);
    }

    private static final class RunState {
        private final String runId;
        private final long attempted;
        private final long startedMs;
        private long acknowledged;
        private long publishFailed;
        private long success;
        private long dlt;
        private long lastEventMs;

        private RunState(String runId, long attempted, long startedMs) {
            this.runId = runId;
            this.attempted = attempted;
            this.startedMs = startedMs;
        }

        private boolean isComplete() {
            return success + dlt + publishFailed >= attempted;
        }

        private boolean isActive(long now) {
            return !isComplete() && now - startedMs < RUN_TIMEOUT_MS;
        }

        private Snapshot snapshot(long now) {
            long endMs = lastEventMs > 0 ? lastEventMs : now;
            long elapsedMs = Math.max(0, endMs - startedMs);
            double throughput = elapsedMs > 0 ? ((success + dlt) * 1000.0) / elapsedMs : 0.0;
            return new Snapshot(runId, attempted, acknowledged, publishFailed, success, dlt,
                    elapsedMs, throughput, isActive(now), isComplete());
        }
    }

    public record Snapshot(
            String runId,
            long attempted,
            long acknowledged,
            long publishFailed,
            long success,
            long dlt,
            long elapsedMs,
            double throughputPerSec,
            boolean active,
            boolean completed) {}

    public static final class RunAlreadyActiveException extends RuntimeException {
        public RunAlreadyActiveException(String runId) {
            super("Kafka demo run is already active: " + runId);
        }
    }

    public static final class RunNotFoundException extends RuntimeException {
        public RunNotFoundException(String runId) {
            super("Kafka demo run not found: " + runId);
        }
    }
}
