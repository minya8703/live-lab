package com.minyaryung.livelab.infra.common;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class LoginAttemptRateLimiterTest {

    @Test
    void allowsTenAttemptsPerPeerAndRejectsTheNextOne() {
        LoginAttemptRateLimiter limiter = new LoginAttemptRateLimiter();

        for (int i = 0; i < 10; i++) {
            assertThat(limiter.tryAcquire("203.0.113.10")).isTrue();
        }

        assertThat(limiter.tryAcquire("203.0.113.10")).isFalse();
        assertThat(limiter.tryAcquire("203.0.113.11")).isTrue();
    }
}
