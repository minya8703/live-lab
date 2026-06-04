package com.minyaryung.livelab.chat;

import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;

@Component
public class SimpleRateLimiter {

    private static final int LIMIT_PER_HOUR = 20;
    private static final long WINDOW_MS = 60L * 60L * 1000L;

    private final ConcurrentHashMap<String, Bucket> buckets = new ConcurrentHashMap<>();

    public boolean tryAcquire(String key) {
        long now = System.currentTimeMillis();
        Bucket bucket = buckets.computeIfAbsent(key, k -> new Bucket(now));
        synchronized (bucket) {
            if (now - bucket.windowStart >= WINDOW_MS) {
                bucket.windowStart = now;
                bucket.count = 0;
            }
            if (bucket.count >= LIMIT_PER_HOUR) return false;
            bucket.count++;
            return true;
        }
    }

    private static final class Bucket {
        long windowStart;
        int count;

        Bucket(long start) {
            this.windowStart = start;
        }
    }
}
