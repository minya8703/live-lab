package com.minyaryung.livelab.application.kafkademo;

import com.minyaryung.livelab.domain.kafkademo.OrderEvent;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class OrderConsumer {

    private final KafkaMetricsService metrics;

    public OrderConsumer(KafkaMetricsService metrics) {
        this.metrics = metrics;
    }

    @KafkaListener(topics = "${livelab.kafka.topic.orders}")
    public void onMessage(OrderEvent event) {
        if (event.orderId() != null && event.orderId() % 17 == 0) {
            throw new IllegalStateException("simulated processing failure orderId=" + event.orderId());
        }
        metrics.recordSuccess();
    }
}
