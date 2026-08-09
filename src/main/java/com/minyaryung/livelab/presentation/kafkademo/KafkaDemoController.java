package com.minyaryung.livelab.presentation.kafkademo;

import com.minyaryung.livelab.application.kafkademo.KafkaMetricsService;
import com.minyaryung.livelab.application.kafkademo.OrderProducer;
import com.minyaryung.livelab.domain.kafkademo.OrderEvent;
import com.minyaryung.livelab.infra.common.PublicDemoRateLimiter;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import java.util.Map;
import java.util.Random;

@RestController
@RequestMapping("/api/kafka-demo")
public class KafkaDemoController {

    private static final int MAX_COUNT = 2000;
    private static final String[] ITEMS = {"shoes","book","laptop","snack","shirt","ball"};
    private final OrderProducer producer;
    private final KafkaMetricsService metrics;
    private final PublicDemoRateLimiter rateLimiter;

    public KafkaDemoController(OrderProducer producer, KafkaMetricsService metrics,
                               PublicDemoRateLimiter rateLimiter) {
        this.producer = producer;
        this.metrics = metrics;
        this.rateLimiter = rateLimiter;
    }

    @PostMapping("/publish")
    public KafkaMetricsService.Snapshot publish(@RequestParam(defaultValue = "1000") int count,
                                                HttpServletRequest request) {
        if (count < 1 || count > MAX_COUNT)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "count\ub294 1~" + MAX_COUNT + " \uc0ac\uc774\uc5ec\uc57c \ud569\ub2c8\ub2e4.");
        if (!rateLimiter.tryKafkaPublish(request.getRemoteAddr(), count))
            throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS,
                    "Kafka 데모 사용 한도를 초과했습니다. 잠시 후 다시 시도해 주세요.");
        final String runId;
        try {
            runId = metrics.beginRun(count);
        } catch (KafkaMetricsService.RunAlreadyActiveException ex) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "다른 Kafka 데모 실행이 진행 중입니다. 완료 후 다시 시도해 주세요.");
        }
        Random rnd = new Random();
        for (int i = 0; i < count; i++) {
            long orderId = metrics.nextOrderId();
            OrderEvent event = new OrderEvent(runId, orderId,
                    ITEMS[rnd.nextInt(ITEMS.length)], rnd.nextInt(10) + 1);
            try {
                producer.send(event).whenComplete((result, error) -> {
                    if (error == null) metrics.recordAcknowledged(runId);
                    else metrics.recordPublishFailed(runId);
                });
            } catch (RuntimeException ex) {
                metrics.recordPublishFailed(runId);
            }
        }
        return metrics.snapshot(runId);
    }

    @GetMapping("/status")
    public KafkaMetricsService.Snapshot status(@RequestParam String runId) {
        try {
            return metrics.snapshot(runId);
        } catch (KafkaMetricsService.RunNotFoundException ex) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "실행 정보를 찾을 수 없습니다.");
        }
    }

    @PostMapping("/reset")
    public Map<String, String> reset(@RequestParam String runId) {
        try {
            metrics.reset(runId);
            return Map.of("status", "reset");
        } catch (KafkaMetricsService.RunAlreadyActiveException ex) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "실행 중에는 초기화할 수 없습니다.");
        } catch (KafkaMetricsService.RunNotFoundException ex) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "실행 정보를 찾을 수 없습니다.");
        }
    }
}
