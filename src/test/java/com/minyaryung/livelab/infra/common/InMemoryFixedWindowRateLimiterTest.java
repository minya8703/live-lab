package com.minyaryung.livelab.infra.common;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThat;

class InMemoryFixedWindowRateLimiterTest {

    private final AtomicLong now = new AtomicLong();
    private final InMemoryFixedWindowRateLimiter limiter =
            new InMemoryFixedWindowRateLimiter(100, Duration.ofMinutes(1).toMillis(), now::get);

    @Test
    void accountsForWeightedPermitsWithinOneWindow() {
        assertThat(limiter.tryAcquire("peer", 60)).isTrue();
        assertThat(limiter.tryAcquire("peer", 40)).isTrue();
        assertThat(limiter.tryAcquire("peer", 1)).isFalse();

        now.addAndGet(Duration.ofMinutes(1).toMillis());

        assertThat(limiter.tryAcquire("peer", 100)).isTrue();
    }

    @Test
    void rejectsInvalidKeysAndRequestsLargerThanCapacity() {
        assertThat(limiter.tryAcquire("", 1)).isFalse();
        assertThat(limiter.tryAcquire("peer", 101)).isFalse();
        assertThat(limiter.bucketCount()).isZero();
    }
}
