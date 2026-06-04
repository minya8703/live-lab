package com.minyaryung.livelab.kafkademo;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

// 메인 orders 컨슈머만 유지. DLT 카운팅은 KafkaConfig 의 errorHandler 안 recoverer 가 직접 처리 —
// 별도 DLT 옵저버 @KafkaListener 를 두면 1 파티션 토픽을 폴링하는 idle 스레드가 추가되어 낭비.
@Component
public class OrderConsumer {

    private static final Logger log = LoggerFactory.getLogger(OrderConsumer.class);

    private final KafkaMetricsService metrics;

    public OrderConsumer(KafkaMetricsService metrics) {
        this.metrics = metrics;
    }

    // orderId % 17 == 0 메시지는 의도적 예외 → FixedBackOff 재시도 → 한계 초과 시 DLT 라우팅.
    @KafkaListener(topics = "${livelab.kafka.topic.orders}")
    public void onMessage(OrderEvent event) {
        if (event.orderId() != null && event.orderId() % 17 == 0) {
            throw new IllegalStateException("simulated processing failure orderId=" + event.orderId());
        }
        metrics.recordSuccess();
    }
}
