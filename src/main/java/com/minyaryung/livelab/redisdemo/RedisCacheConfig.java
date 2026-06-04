package com.minyaryung.livelab.redisdemo;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cache.Cache;
import org.springframework.cache.annotation.CachingConfigurer;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.interceptor.CacheErrorHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext.SerializationPair;

import java.time.Duration;
import java.util.Set;

// spring.cache.type=none 일 때(예: Kafka 통합 테스트) 로드되지 않게 — RedisConnectionFactory 의존을 회피.
// 미설정·redis 일 때는 그대로 동작.
@Configuration
@EnableCaching
@ConditionalOnProperty(name = "spring.cache.type", havingValue = "redis", matchIfMissing = true)
public class RedisCacheConfig implements CachingConfigurer {

    @Bean
    public RedisCacheManager cacheManager(RedisConnectionFactory factory) {
        RedisCacheConfiguration config = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofSeconds(60))
                .disableCachingNullValues()
                .serializeValuesWith(SerializationPair.fromSerializer(new GenericJackson2JsonRedisSerializer()));
        return RedisCacheManager.builder(factory)
                .cacheDefaults(config)
                // 시작 시점에 캐시를 명시적으로 선언 — Micrometer가 메트릭 바인딩 가능하게.
                // lazy 생성에 맡기면 Grafana에 "No data" 로 뜸.
                .initialCacheNames(Set.of(ProductService.CACHE_NAME))
                .enableStatistics()
                .build();
    }

    // Redis 장애 시 예외를 삼켜서 @Cacheable 호출이 캐시 미스로 떨어지게 만든다.
    // 결과적으로 메서드 실제 실행 = DB 직접 조회로 자동 fallback.
    // 기존 Betax SaaS 운영 코드와 동일한 패턴.
    @Override
    public CacheErrorHandler errorHandler() {
        return new RedisFallbackCacheErrorHandler();
    }

    private static final class RedisFallbackCacheErrorHandler implements CacheErrorHandler {

        private static final Logger log = LoggerFactory.getLogger(RedisFallbackCacheErrorHandler.class);

        @Override
        public void handleCacheGetError(RuntimeException ex, Cache cache, Object key) {
            log.warn("cache GET 실패 → DB fallback — cache={} key={} cause={}: {}",
                    cache.getName(), key, ex.getClass().getSimpleName(), ex.getMessage());
        }

        @Override
        public void handleCachePutError(RuntimeException ex, Cache cache, Object key, Object value) {
            log.warn("cache PUT 실패 (응답에 영향 없음) — cache={} key={} cause={}: {}",
                    cache.getName(), key, ex.getClass().getSimpleName(), ex.getMessage());
        }

        @Override
        public void handleCacheEvictError(RuntimeException ex, Cache cache, Object key) {
            log.warn("cache EVICT 실패 — cache={} key={} cause={}: {}",
                    cache.getName(), key, ex.getClass().getSimpleName(), ex.getMessage());
        }

        @Override
        public void handleCacheClearError(RuntimeException ex, Cache cache) {
            log.warn("cache CLEAR 실패 — cache={} cause={}: {}",
                    cache.getName(), ex.getClass().getSimpleName(), ex.getMessage());
        }
    }
}
