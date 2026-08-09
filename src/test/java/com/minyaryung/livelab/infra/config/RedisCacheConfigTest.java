package com.minyaryung.livelab.infra.config;

import org.junit.jupiter.api.Test;
import org.springframework.cache.Cache;
import org.springframework.cache.interceptor.CacheErrorHandler;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class RedisCacheConfigTest {

    private final CacheErrorHandler handler = new RedisCacheConfig().errorHandler();
    private final Cache cache = namedCache();

    @Test
    void cacheReadFailureFallsBackWithoutFailingTheRequest() {
        assertThatCode(() -> handler.handleCacheGetError(
                new IllegalStateException("redis unavailable"), cache, "전자제품"))
                .doesNotThrowAnyException();
    }

    @Test
    void cacheInvalidationFailureIsVisibleToTheCaller() {
        IllegalStateException failure = new IllegalStateException("redis unavailable");

        assertThatThrownBy(() -> handler.handleCacheClearError(failure, cache))
                .isSameAs(failure);
    }

    private static Cache namedCache() {
        Cache cache = mock(Cache.class);
        when(cache.getName()).thenReturn("category-stats");
        return cache;
    }
}
