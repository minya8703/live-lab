package com.minyaryung.livelab.redisdemo;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/redis-demo")
public class RedisDemoController {

    private static final int MAX_ITERATIONS = 200;

    private final ProductService service;

    public RedisDemoController(ProductService service) {
        this.service = service;
    }

    @GetMapping("/categories")
    public List<String> categories() {
        return DemoCategories.ALL;
    }

    @GetMapping("/run")
    public Map<String, Object> run(
            @RequestParam(defaultValue = "20") int iterations,
            @RequestParam(defaultValue = "전자제품") String category,
            @RequestParam(defaultValue = "true") boolean cache) {

        if (iterations < 1 || iterations > MAX_ITERATIONS) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "iterations는 1~" + MAX_ITERATIONS + " 사이여야 합니다.");
        }
        if (!DemoCategories.ALL.contains(category)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "지원되지 않는 카테고리입니다. 사용 가능: " + DemoCategories.ALL);
        }

        double[] timingsMs = new double[iterations];
        double sum = 0, min = Double.MAX_VALUE, max = 0;

        for (int i = 0; i < iterations; i++) {
            long start = System.nanoTime();
            if (cache) {
                service.aggregateCached(category);
            } else {
                service.aggregateNoCache(category);
            }
            double elapsedMs = (System.nanoTime() - start) / 1_000_000.0;
            timingsMs[i] = elapsedMs;
            sum += elapsedMs;
            if (elapsedMs < min) min = elapsedMs;
            if (elapsedMs > max) max = elapsedMs;
        }

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("cached", cache);
        body.put("category", category);
        body.put("iterations", iterations);
        body.put("timingsMs", timingsMs);
        body.put("avgMs", sum / iterations);
        body.put("minMs", min);
        body.put("maxMs", max);
        return body;
    }

    @PostMapping("/evict")
    public Map<String, String> evict() {
        service.evictAll();
        return Map.of("status", "evicted");
    }
}
