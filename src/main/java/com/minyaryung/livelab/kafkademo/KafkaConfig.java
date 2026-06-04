package com.minyaryung.livelab.kafkademo;

import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.util.backoff.FixedBackOff;

@Configuration
public class KafkaConfig {

    @Bean
    public NewTopic ordersTopic(@Value("${livelab.kafka.topic.orders}") String name) {
        return TopicBuilder.name(name).partitions(3).replicas(1).build();
    }

    @Bean
    public NewTopic ordersDltTopic(@Value("${livelab.kafka.topic.orders-dlt}") String name) {
        return TopicBuilder.name(name).partitions(1).replicas(1).build();
    }

    // 사용자 Betax 운영 패턴: FixedBackOff 재시도 → 한계 초과 시 DLT 라우팅.
    // DLT 메시지 카운트는 별도 컨슈머가 아니라 recoverer 호출 시점에 메모리 카운터로 즉시 증가.
    // (이전 구조: DLT observer @KafkaListener 가 3 스레드를 더 띄워 1 파티션을 폴링했음 — 낭비)
    //
    // 중요: DefaultErrorHandler 는 3-arg accept(record, consumer, ex) 를 호출하지 2-arg 를 호출하지 않는다.
    // 2-arg 만 override 하면 우리 카운터가 안 불린다 (DeadLetterPublishingRecoverer 의 2-arg 는
    // 내부적으로 3-arg 로 위임만 함). 그래서 3-arg 쪽을 override 해야 카운트가 일관되게 잡힌다.
    @Bean
    public DefaultErrorHandler kafkaErrorHandler(
            KafkaTemplate<String, Object> template,
            KafkaMetricsService metrics,
            @Value("${livelab.kafka.consumer.retry.interval-ms}") long intervalMs,
            @Value("${livelab.kafka.consumer.retry.max-attempts}") long maxAttempts) {
        DeadLetterPublishingRecoverer recoverer = new DeadLetterPublishingRecoverer(template) {
            @Override
            public void accept(ConsumerRecord<?, ?> record, Consumer<?, ?> consumer, Exception ex) {
                super.accept(record, consumer, ex);
                metrics.recordDlt();
            }
        };
        return new DefaultErrorHandler(recoverer, new FixedBackOff(intervalMs, maxAttempts));
    }
}
