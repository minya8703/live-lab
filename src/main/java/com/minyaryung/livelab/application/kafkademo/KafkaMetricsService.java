package com.minyaryung.livelab.application.kafkademo;

import org.springframework.stereotype.Service;
import java.util.concurrent.atomic.AtomicLong;

@Service
public class KafkaMetricsService {

    private final AtomicLong attemptedTotal = new AtomicLong();
    private final AtomicLong acknowledgedTotal = new AtomicLong();
    private final AtomicLong publishFailedTotal = new AtomicLong();
    private final AtomicLong successTotal = new AtomicLong();
    private final AtomicLong dltTotal = new AtomicLong();
    private final AtomicLong orderIdSeq = new AtomicLong();
    private volatile long runStartedMs;
    private volatile long lastEventMs;

    public long nextOrderId() { return orderIdSeq.incrementAndGet(); }
    public void beginRun(int attempted) {
        reset();
        runStartedMs = System.currentTimeMillis();
        attemptedTotal.set(attempted);
    }
    public void recordAcknowledged() { acknowledgedTotal.incrementAndGet(); }
    public void recordPublishFailed() { publishFailedTotal.incrementAndGet(); }
    public void recordSuccess() { successTotal.incrementAndGet(); lastEventMs = System.currentTimeMillis(); }
    public void recordDlt() { dltTotal.incrementAndGet(); lastEventMs = System.currentTimeMillis(); }

    public Snapshot snapshot() {
        long attempted = attemptedTotal.get(), acknowledged = acknowledgedTotal.get();
        long publishFailed = publishFailedTotal.get(), success = successTotal.get(), dlt = dltTotal.get();
        long elapsedMs = (lastEventMs > 0 && runStartedMs > 0) ? Math.max(0, lastEventMs - runStartedMs) : 0;
        double throughput = elapsedMs > 0 ? ((success + dlt) * 1000.0) / elapsedMs : 0.0;
        return new Snapshot(attempted, acknowledged, publishFailed, success, dlt, elapsedMs, throughput);
    }

    public void reset() {
        attemptedTotal.set(0); acknowledgedTotal.set(0); publishFailedTotal.set(0);
        successTotal.set(0); dltTotal.set(0);
        runStartedMs = 0; lastEventMs = 0;
    }

    public record Snapshot(
            long attempted,
            long acknowledged,
            long publishFailed,
            long success,
            long dlt,
            long elapsedMs,
            double throughputPerSec) {}
}
