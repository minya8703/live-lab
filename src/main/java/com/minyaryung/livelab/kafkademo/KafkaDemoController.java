package com.minyaryung.livelab.kafkademo;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;
import java.util.Random;

@RestController
@RequestMapping("/api/kafka-demo")
public class KafkaDemoController {

    private static final int MAX_COUNT = 10000;
    private static final String[] ITEMS = {"shoes", "book", "laptop", "snack", "shirt", "ball"};

    private final OrderProducer producer;
    private final KafkaMetricsService metrics;

    public KafkaDemoController(OrderProducer producer, KafkaMetricsService metrics) {
        this.producer = producer;
        this.metrics = metrics;
    }

    @PostMapping("/publish")
    public KafkaMetricsService.Snapshot publish(@RequestParam(defaultValue = "1000") int count) {
        if (count < 1 || count > MAX_COUNT) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "count는 1~" + MAX_COUNT + " 사이여야 합니다.");
        }
        metrics.reset();
        metrics.recordProduced(count);

        Random rnd = new Random();
        for (int i = 0; i < count; i++) {
            long orderId = metrics.nextOrderId();
            OrderEvent event = new OrderEvent(
                    orderId,
                    ITEMS[rnd.nextInt(ITEMS.length)],
                    rnd.nextInt(10) + 1);
            producer.send(event);
        }
        return metrics.snapshot();
    }

    @GetMapping("/status")
    public KafkaMetricsService.Snapshot status() {
        return metrics.snapshot();
    }

    @PostMapping("/reset")
    public Map<String, String> reset() {
        metrics.reset();
        return Map.of("status", "reset");
    }
}
