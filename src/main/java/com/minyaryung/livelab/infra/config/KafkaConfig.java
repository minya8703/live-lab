package com.minyaryung.livelab.infra.config;

import com.minyaryung.livelab.application.kafkademo.KafkaMetricsService;
import com.minyaryung.livelab.domain.kafkademo.OrderEvent;
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

    static final int ORDER_PARTITIONS = 3;

    @Bean
    public NewTopic ordersTopic(@Value("${livelab.kafka.topic.orders}") String name) {
        return TopicBuilder.name(name).partitions(ORDER_PARTITIONS).replicas(1).build();
    }

    @Bean
    public NewTopic ordersDltTopic(@Value("${livelab.kafka.topic.orders-dlt}") String name) {
        // DeadLetterPublishingRecoverer는 기본적으로 원본 partition을 유지한다.
        // DLT partition 수가 더 적으면 원본 partition 1, 2의 실패 레코드를 발행할 수 없다.
        return TopicBuilder.name(name).partitions(ORDER_PARTITIONS).replicas(1).build();
    }

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
                if (record.value() instanceof OrderEvent event) {
                    metrics.recordDlt(event.runId());
                }
            }
        };
        return new DefaultErrorHandler(recoverer, new FixedBackOff(intervalMs, maxAttempts));
    }
}
