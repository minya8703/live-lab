package com.minyaryung.livelab.infra.common;

import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
public class PublicDemoRateLimiter {

    private static final long KAFKA_MESSAGE_CAPACITY = 5_000;
    private static final long REDIS_ITERATION_CAPACITY = 400;
    private static final long REDIS_EVICT_CAPACITY = 10;

    private final InMemoryFixedWindowRateLimiter kafkaMessages = new InMemoryFixedWindowRateLimiter(
            KAFKA_MESSAGE_CAPACITY, Duration.ofMinutes(10).toMillis(), System::currentTimeMillis);
    private final InMemoryFixedWindowRateLimiter redisIterations = new InMemoryFixedWindowRateLimiter(
            REDIS_ITERATION_CAPACITY, Duration.ofMinutes(1).toMillis(), System::currentTimeMillis);
    private final InMemoryFixedWindowRateLimiter redisEvictions = new InMemoryFixedWindowRateLimiter(
            REDIS_EVICT_CAPACITY, Duration.ofMinutes(1).toMillis(), System::currentTimeMillis);

    public boolean tryKafkaPublish(String peerAddress, int messageCount) {
        return kafkaMessages.tryAcquire(peerAddress, messageCount);
    }

    public boolean tryRedisRun(String peerAddress, int iterations) {
        return redisIterations.tryAcquire(peerAddress, iterations);
    }

    public boolean tryRedisEvict(String peerAddress) {
        return redisEvictions.tryAcquire(peerAddress, 1);
    }
}
