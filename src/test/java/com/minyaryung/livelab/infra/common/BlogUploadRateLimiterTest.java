package com.minyaryung.livelab.infra.common;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class BlogUploadRateLimiterTest {

    @Test
    void allowsTwentyUploadsPerPeerAndRejectsTheNextOne() {
        BlogUploadRateLimiter limiter = new BlogUploadRateLimiter();

        for (int i = 0; i < 20; i++) {
            assertThat(limiter.tryAcquire("203.0.113.10")).isTrue();
        }

        assertThat(limiter.tryAcquire("203.0.113.10")).isFalse();
    }
}
