package com.minyaryung.livelab.redisdemo;

import com.redis.testcontainers.RedisContainer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.CacheManager;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

// 진짜 Postgres + Redis 컨테이너로 캐시 동작 검증.
// mock 이 아니라 실 인프라 — 운영과 동일한 직렬화·TTL·CacheErrorHandler 경로를 그대로 탐.
@SpringBootTest
@Testcontainers
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class ProductServiceIntegrationTest {

    @Container
    static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>(
            DockerImageName.parse("postgres:16-alpine"));

    @Container
    static final RedisContainer redis = new RedisContainer(
            DockerImageName.parse("redis:7-alpine"));

    @DynamicPropertySource
    static void wireContainers(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.data.redis.host", redis::getHost);
        registry.add("spring.data.redis.port", redis::getFirstMappedPort);
        // 시드 끔 — 각 테스트가 자기 데이터만 보게.
        // 1000 으로 두면 DataSeeder 의 랜덤 데이터가 쿼리 결과를 오염시킴.
        registry.add("livelab.demo.product-count", () -> "0");
    }

    @Autowired ProductService productService;
    @Autowired ProductRepository repo;
    @Autowired CacheManager cacheManager;

    @BeforeEach
    void cleanState() {
        // 각 테스트마다 깨끗한 DB + 캐시 상태로 시작
        repo.deleteAll();
        cacheManager.getCacheNames().forEach(name -> {
            var c = cacheManager.getCache(name);
            if (c != null) c.clear();
        });
    }

    @Test
    void aggregateReturnsCategoryStats() {
        seedProduct("전자제품", "A", 1000);
        seedProduct("전자제품", "A", 2000);
        seedProduct("전자제품", "B", 5000);

        List<CategoryStats> stats = productService.aggregateNoCache("전자제품");

        assertThat(stats).extracting(CategoryStats::subCategory)
                .containsExactly("A", "B");
        assertThat(stats).extracting(CategoryStats::count)
                .containsExactly(2L, 1L);
    }

    @Test
    void cacheHitReturnsSameInstance() {
        seedProduct("도서", "A", 1000);

        List<CategoryStats> first = productService.aggregateCached("도서");
        List<CategoryStats> second = productService.aggregateCached("도서");

        // 두 호출이 같은 데이터를 반환해야 (캐시 작동)
        assertThat(first).hasSameSizeAs(second);
        assertThat(first.get(0).subCategory()).isEqualTo(second.get(0).subCategory());

        // 캐시에 실제로 키가 박혀있어야 함
        var cache = cacheManager.getCache(ProductService.CACHE_NAME);
        assertThat(cache).isNotNull();
        assertThat(cache.get("도서")).isNotNull();
    }

    @Test
    void evictAllClearsCache() {
        seedProduct("의류", "A", 1000);
        productService.aggregateCached("의류");

        var cache = cacheManager.getCache(ProductService.CACHE_NAME);
        assertThat(cache.get("의류")).isNotNull();

        productService.evictAll();
        assertThat(cache.get("의류")).isNull();
    }

    private void seedProduct(String category, String subCategory, int price) {
        Product p = new Product();
        p.setName("Test-" + System.nanoTime());
        p.setCategory(category);
        p.setSubCategory(subCategory);
        p.setPrice(BigDecimal.valueOf(price));
        p.setStock(10);
        repo.save(p);
    }
}
