package com.minyaryung.livelab.application.redisdemo;

import com.minyaryung.livelab.domain.redisdemo.CategoryStats;
import com.minyaryung.livelab.domain.redisdemo.ProductRepository;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ProductService {

    public static final String CACHE_NAME = "category-stats";
    private final ProductRepository repo;

    public ProductService(ProductRepository repo) { this.repo = repo; }

    @Transactional(readOnly = true)
    public List<CategoryStats> aggregateNoCache(String category) {
        return repo.aggregateByCategory(category);
    }

    @Cacheable(cacheNames = CACHE_NAME, key = "#category")
    @Transactional(readOnly = true)
    public List<CategoryStats> aggregateCached(String category) {
        return repo.aggregateByCategory(category);
    }

    @CacheEvict(cacheNames = CACHE_NAME, allEntries = true)
    public void evictAll() {}
}
