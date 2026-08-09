package com.minyaryung.livelab.presentation.redisdemo;

import com.minyaryung.livelab.application.redisdemo.ProductService;
import com.minyaryung.livelab.infra.common.PublicDemoRateLimiter;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.server.ResponseStatusException;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RedisDemoControllerTest {

    @Test
    void rejectsRunBeforeQueryingWhenPeerBudgetIsExhausted() {
        ProductService service = mock(ProductService.class);
        PublicDemoRateLimiter limiter = mock(PublicDemoRateLimiter.class);
        when(limiter.tryRedisRun("203.0.113.10", 20)).thenReturn(false);
        RedisDemoController controller = new RedisDemoController(service, limiter);

        assertThatThrownBy(() -> controller.run(20, "전자제품", true, request()))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(ex -> ((ResponseStatusException) ex).getStatusCode().value())
                .isEqualTo(429);
        verify(service, never()).aggregateCached("전자제품");
    }

    @Test
    void rejectsEvictionBeforeClearingCacheWhenPeerBudgetIsExhausted() {
        ProductService service = mock(ProductService.class);
        PublicDemoRateLimiter limiter = mock(PublicDemoRateLimiter.class);
        when(limiter.tryRedisEvict("203.0.113.10")).thenReturn(false);
        RedisDemoController controller = new RedisDemoController(service, limiter);

        assertThatThrownBy(() -> controller.evict(request()))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(ex -> ((ResponseStatusException) ex).getStatusCode().value())
                .isEqualTo(429);
        verify(service, never()).evictAll();
    }

    private static MockHttpServletRequest request() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("203.0.113.10");
        return request;
    }
}
