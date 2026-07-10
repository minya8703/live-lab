package com.minyaryung.livelab.infra.config;

import com.minyaryung.livelab.infra.security.JwtAuthInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.CacheControl;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.time.Duration;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    private final JwtAuthInterceptor jwtAuthInterceptor;

    public WebMvcConfig(JwtAuthInterceptor jwtAuthInterceptor) {
        this.jwtAuthInterceptor = jwtAuthInterceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(jwtAuthInterceptor)
                .addPathPatterns("/api/blog/**", "/api/blog");
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // HTML → 항상 최신 확인 (새 버전 쿼리스트링이 반영되도록)
        registry.addResourceHandler("/**/*.html")
                .addResourceLocations("classpath:/static/")
                .setCacheControl(CacheControl.noCache());

        // CSS/JS/이미지 → ?v=hash 가 붙으므로 장기 캐시 OK
        registry.addResourceHandler("/**/*.css", "/**/*.js", "/**/*.png", "/**/*.jpg", "/**/*.svg")
                .addResourceLocations("classpath:/static/")
                .setCacheControl(CacheControl.maxAge(Duration.ofDays(365)).mustRevalidate());
    }
}
