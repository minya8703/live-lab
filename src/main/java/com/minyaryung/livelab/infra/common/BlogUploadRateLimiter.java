package com.minyaryung.livelab.infra.common;

import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
public class BlogUploadRateLimiter {

    private final InMemoryFixedWindowRateLimiter delegate = new InMemoryFixedWindowRateLimiter(
            20, Duration.ofHours(1).toMillis(), System::currentTimeMillis);

    public boolean tryAcquire(String peerAddress) {
        return delegate.tryAcquire(peerAddress, 1);
    }
}
