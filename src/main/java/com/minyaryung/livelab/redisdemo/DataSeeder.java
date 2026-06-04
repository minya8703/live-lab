package com.minyaryung.livelab.redisdemo;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

@Component
public class DataSeeder {

    private static final Logger log = LoggerFactory.getLogger(DataSeeder.class);
    private static final int BATCH_SIZE = 1000;
    private static final String[] SUB_CATEGORIES = {"A", "B", "C", "D", "E", "F", "G", "H", "I", "J"};

    private final ProductRepository repo;
    private final int targetCount;
    private final List<String> categories = DemoCategories.ALL;

    public DataSeeder(
            ProductRepository repo,
            @Value("${livelab.demo.product-count}") int targetCount) {
        this.repo = repo;
        this.targetCount = targetCount;
    }

    @EventListener(ApplicationReadyEvent.class)
    @Transactional
    public void seedIfEmpty() {
        long existing = repo.count();
        if (existing >= targetCount) {
            log.info("product seed 생략 — 이미 {}건 존재 (target {})", existing, targetCount);
            return;
        }
        int toInsert = targetCount - (int) existing;
        log.info("product seed 시작 — {}건 삽입 (배치 크기 {})", toInsert, BATCH_SIZE);
        long startMs = System.currentTimeMillis();

        Random rnd = new Random(42);
        List<Product> batch = new ArrayList<>(BATCH_SIZE);
        for (int i = 0; i < toInsert; i++) {
            Product p = new Product();
            p.setName("Product-" + (existing + i));
            p.setCategory(categories.get(rnd.nextInt(categories.size())));
            p.setSubCategory(SUB_CATEGORIES[rnd.nextInt(SUB_CATEGORIES.length)]);
            p.setPrice(BigDecimal.valueOf(1000L + rnd.nextInt(99_000)));
            p.setStock(rnd.nextInt(1000));
            batch.add(p);
            if (batch.size() >= BATCH_SIZE) {
                repo.saveAll(batch);
                batch.clear();
            }
        }
        if (!batch.isEmpty()) repo.saveAll(batch);

        long elapsed = System.currentTimeMillis() - startMs;
        log.info("product seed 완료 — {}건 / {}ms", toInsert, elapsed);
    }
}
