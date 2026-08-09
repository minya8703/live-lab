package com.minyaryung.livelab.infra.common;

import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.function.LongSupplier;

@Component
public class SimpleRateLimiter {

    private final InMemoryFixedWindowRateLimiter delegate;

    public SimpleRateLimiter() {
        this(System::currentTimeMillis);
    }

    SimpleRateLimiter(LongSupplier currentTimeMillis) {
        this.delegate = new InMemoryFixedWindowRateLimiter(
                20, Duration.ofHours(1).toMillis(), currentTimeMillis);
    }

    public boolean tryAcquire(String key) {
        return delegate.tryAcquire(key, 1);
    }

    void removeExpiredBuckets() {
        delegate.removeExpiredBuckets();
    }

    int bucketCount() {
        return delegate.bucketCount();
    }
}
