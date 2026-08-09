package com.minyaryung.livelab.infra.common;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.LongSupplier;

final class InMemoryFixedWindowRateLimiter {

    private static final long CLEANUP_EVERY_OPERATIONS = 256;

    private final ConcurrentHashMap<String, Bucket> buckets = new ConcurrentHashMap<>();
    private final AtomicLong operations = new AtomicLong();
    private final long capacity;
    private final long windowMs;
    private final LongSupplier currentTimeMillis;

    InMemoryFixedWindowRateLimiter(long capacity, long windowMs, LongSupplier currentTimeMillis) {
        if (capacity < 1 || windowMs < 1) throw new IllegalArgumentException("capacity and windowMs must be positive");
        this.capacity = capacity;
        this.windowMs = windowMs;
        this.currentTimeMillis = currentTimeMillis;
    }

    boolean tryAcquire(String key, long permits) {
        if (key == null || key.isBlank() || permits < 1 || permits > capacity) return false;

        long now = currentTimeMillis.getAsLong();
        AtomicBoolean granted = new AtomicBoolean();
        buckets.compute(key, (ignored, bucket) -> {
            if (bucket == null || now - bucket.windowStart >= windowMs) {
                granted.set(true);
                return new Bucket(now, permits);
            }

            bucket.lastAccess = now;
            if (bucket.used + permits <= capacity) {
                bucket.used += permits;
                granted.set(true);
            }
            return bucket;
        });

        if (operations.incrementAndGet() % CLEANUP_EVERY_OPERATIONS == 0) removeExpiredBuckets(now);
        return granted.get();
    }

    void removeExpiredBuckets() {
        removeExpiredBuckets(currentTimeMillis.getAsLong());
    }

    int bucketCount() {
        return buckets.size();
    }

    private void removeExpiredBuckets(long now) {
        buckets.forEach((key, observed) -> {
            if (now - observed.lastAccess < windowMs) return;
            buckets.computeIfPresent(key, (ignored, current) ->
                    current == observed && now - current.lastAccess >= windowMs ? null : current);
        });
    }

    private static final class Bucket {
        private final long windowStart;
        private long used;
        private long lastAccess;

        private Bucket(long start, long used) {
            this.windowStart = start;
            this.lastAccess = start;
            this.used = used;
        }
    }
}
