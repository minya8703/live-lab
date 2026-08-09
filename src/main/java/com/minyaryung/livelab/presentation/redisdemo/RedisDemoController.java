package com.minyaryung.livelab.presentation.redisdemo;

import com.minyaryung.livelab.application.redisdemo.ProductService;
import com.minyaryung.livelab.domain.redisdemo.DemoCategories;
import com.minyaryung.livelab.infra.common.PublicDemoRateLimiter;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/redis-demo")
public class RedisDemoController {

    private static final int MAX_ITERATIONS = 200;
    private final ProductService service;
    private final PublicDemoRateLimiter rateLimiter;

    public RedisDemoController(ProductService service, PublicDemoRateLimiter rateLimiter) {
        this.service = service;
        this.rateLimiter = rateLimiter;
    }

    @GetMapping("/categories")
    public List<String> categories() { return DemoCategories.ALL; }

    @GetMapping("/run")
    public Map<String, Object> run(
            @RequestParam(defaultValue = "20") int iterations,
            @RequestParam(defaultValue = "\uc804\uc790\uc81c\ud488") String category,
            @RequestParam(defaultValue = "true") boolean cache,
            HttpServletRequest request) {
        if (iterations < 1 || iterations > MAX_ITERATIONS)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "iterations\ub294 1~" + MAX_ITERATIONS + " \uc0ac\uc774\uc5ec\uc57c \ud569\ub2c8\ub2e4.");
        if (!DemoCategories.ALL.contains(category))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "\uc9c0\uc6d0\ub418\uc9c0 \uc54a\ub294 \uce74\ud14c\uace0\ub9ac\uc785\ub2c8\ub2e4. \uc0ac\uc6a9 \uac00\ub2a5: " + DemoCategories.ALL);
        if (!rateLimiter.tryRedisRun(request.getRemoteAddr(), iterations))
            throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS,
                    "Redis 데모 사용 한도를 초과했습니다. 잠시 후 다시 시도해 주세요.");
        double[] timingsMs = new double[iterations];
        double sum = 0, min = Double.MAX_VALUE, max = 0;
        for (int i = 0; i < iterations; i++) {
            long start = System.nanoTime();
            if (cache) service.aggregateCached(category); else service.aggregateNoCache(category);
            double elapsedMs = (System.nanoTime() - start) / 1_000_000.0;
            timingsMs[i] = elapsedMs; sum += elapsedMs;
            if (elapsedMs < min) min = elapsedMs;
            if (elapsedMs > max) max = elapsedMs;
        }
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("cached", cache); body.put("category", category);
        body.put("iterations", iterations); body.put("timingsMs", timingsMs);
        body.put("avgMs", sum / iterations); body.put("minMs", min); body.put("maxMs", max);
        return body;
    }

    @PostMapping("/evict")
    public Map<String, String> evict(HttpServletRequest request) {
        if (!rateLimiter.tryRedisEvict(request.getRemoteAddr()))
            throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS,
                    "캐시 초기화 사용 한도를 초과했습니다. 잠시 후 다시 시도해 주세요.");
        service.evictAll();
        return Map.of("status", "evicted");
    }
}
