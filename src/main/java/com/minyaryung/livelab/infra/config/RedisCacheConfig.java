package com.minyaryung.livelab.infra.config;

import com.minyaryung.livelab.application.redisdemo.ProductService;
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
                .initialCacheNames(Set.of(ProductService.CACHE_NAME))
                .enableStatistics()
                .build();
    }

    @Override
    public CacheErrorHandler errorHandler() { return new RedisFallbackCacheErrorHandler(); }

    private static final class RedisFallbackCacheErrorHandler implements CacheErrorHandler {
        private static final Logger log = LoggerFactory.getLogger(RedisFallbackCacheErrorHandler.class);
        @Override public void handleCacheGetError(RuntimeException ex, Cache cache, Object key) {
            log.warn("cache GET fail — cache={} key={} cause={}: {}", cache.getName(), key, ex.getClass().getSimpleName(), ex.getMessage());
        }
        @Override public void handleCachePutError(RuntimeException ex, Cache cache, Object key, Object value) {
            log.warn("cache PUT fail — cache={} key={}", cache.getName(), key);
        }
        @Override public void handleCacheEvictError(RuntimeException ex, Cache cache, Object key) {
            log.warn("cache EVICT fail — cache={} key={}", cache.getName(), key);
            throw ex;
        }
        @Override public void handleCacheClearError(RuntimeException ex, Cache cache) {
            log.warn("cache CLEAR fail — cache={}", cache.getName());
            throw ex;
        }
    }
}
