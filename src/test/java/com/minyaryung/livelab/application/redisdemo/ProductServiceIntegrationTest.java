package com.minyaryung.livelab.application.redisdemo;

import com.minyaryung.livelab.domain.redisdemo.CategoryStats;
import com.minyaryung.livelab.domain.redisdemo.Product;
import com.minyaryung.livelab.domain.redisdemo.ProductRepository;
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

@SpringBootTest
@Testcontainers(disabledWithoutDocker = true)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class ProductServiceIntegrationTest {

    @Container
    static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>(DockerImageName.parse("postgres:16-alpine"));

    @Container
    static final RedisContainer redis = new RedisContainer(DockerImageName.parse("redis:7-alpine"));

    @DynamicPropertySource
    static void wireContainers(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.data.redis.host", redis::getHost);
        registry.add("spring.data.redis.port", redis::getFirstMappedPort);
        registry.add("livelab.demo.product-count", () -> "0");
    }

    @Autowired ProductService productService;
    @Autowired ProductRepository repo;
    @Autowired CacheManager cacheManager;

    @BeforeEach
    void cleanState() {
        repo.deleteAll();
        cacheManager.getCacheNames().forEach(name -> {
            var c = cacheManager.getCache(name);
            if (c != null) c.clear();
        });
    }

    @Test
    void aggregateReturnsCategoryStats() {
        seedProduct("\uc804\uc790\uc81c\ud488", "A", 1000);
        seedProduct("\uc804\uc790\uc81c\ud488", "A", 2000);
        seedProduct("\uc804\uc790\uc81c\ud488", "B", 5000);
        List<CategoryStats> stats = productService.aggregateNoCache("\uc804\uc790\uc81c\ud488");
        assertThat(stats).extracting(CategoryStats::subCategory).containsExactly("A", "B");
        assertThat(stats).extracting(CategoryStats::count).containsExactly(2L, 1L);
    }

    @Test
    void cacheHitReturnsSameInstance() {
        seedProduct("\ub3c4\uc11c", "A", 1000);
        List<CategoryStats> first = productService.aggregateCached("\ub3c4\uc11c");
        List<CategoryStats> second = productService.aggregateCached("\ub3c4\uc11c");
        assertThat(first).hasSameSizeAs(second);
        assertThat(first.get(0).subCategory()).isEqualTo(second.get(0).subCategory());
        var cache = cacheManager.getCache(ProductService.CACHE_NAME);
        assertThat(cache).isNotNull();
        assertThat(cache.get("\ub3c4\uc11c")).isNotNull();
    }

    @Test
    void evictAllClearsCache() {
        seedProduct("\uc758\ub958", "A", 1000);
        productService.aggregateCached("\uc758\ub958");
        var cache = cacheManager.getCache(ProductService.CACHE_NAME);
        assertThat(cache.get("\uc758\ub958")).isNotNull();
        productService.evictAll();
        assertThat(cache.get("\uc758\ub958")).isNull();
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
