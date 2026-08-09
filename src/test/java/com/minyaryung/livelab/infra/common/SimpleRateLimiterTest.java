package com.minyaryung.livelab.infra.common;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThat;

class SimpleRateLimiterTest {

    private final AtomicLong now = new AtomicLong();
    private final SimpleRateLimiter limiter = new SimpleRateLimiter(now::get);

    @Test
    void allowsTwentyRequestsPerFixedWindow() {
        for (int i = 0; i < 20; i++) {
            assertThat(limiter.tryAcquire("client")).isTrue();
        }

        assertThat(limiter.tryAcquire("client")).isFalse();

        now.addAndGet(Duration.ofHours(1).toMillis());
        assertThat(limiter.tryAcquire("client")).isTrue();
    }

    @Test
    void removesInactiveBucketsAfterOneHour() {
        limiter.tryAcquire("client-a");
        limiter.tryAcquire("client-b");
        assertThat(limiter.bucketCount()).isEqualTo(2);

        now.addAndGet(Duration.ofHours(1).toMillis());
        limiter.removeExpiredBuckets();

        assertThat(limiter.bucketCount()).isZero();
    }
}
