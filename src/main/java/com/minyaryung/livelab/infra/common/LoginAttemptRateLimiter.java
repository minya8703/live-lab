package com.minyaryung.livelab.infra.common;

import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
public class LoginAttemptRateLimiter {

    private static final long MAX_ATTEMPTS = 10;
    private final InMemoryFixedWindowRateLimiter delegate = new InMemoryFixedWindowRateLimiter(
            MAX_ATTEMPTS, Duration.ofMinutes(10).toMillis(), System::currentTimeMillis);

    public boolean tryAcquire(String peerAddress) {
        return delegate.tryAcquire(peerAddress, 1);
    }
}
