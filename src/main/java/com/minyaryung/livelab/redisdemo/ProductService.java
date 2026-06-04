package com.minyaryung.livelab.redisdemo;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ProductService {

    public static final String CACHE_NAME = "category-stats";

    private final ProductRepository repo;

    public ProductService(ProductRepository repo) {
        this.repo = repo;
    }

    // 캐시 미사용 — 매 호출마다 DB 풀스캔 + GROUP BY
    @Transactional(readOnly = true)
    public List<CategoryStats> aggregateNoCache(String category) {
        return repo.aggregateByCategory(category);
    }

    // 캐시 사용 — Redis 장애 시 CacheErrorHandler 가 예외를 삼키고 자동으로 DB 직접 조회로 fallback.
    // 기존 Betax SaaS 운영 패턴(32메서드 캐시 + fallback)을 그대로 재현.
    @Cacheable(cacheNames = CACHE_NAME, key = "#category")
    @Transactional(readOnly = true)
    public List<CategoryStats> aggregateCached(String category) {
        return repo.aggregateByCategory(category);
    }

    @CacheEvict(cacheNames = CACHE_NAME, allEntries = true)
    public void evictAll() {}
}
