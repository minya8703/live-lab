package com.minyaryung.livelab.infra.config;

import com.minyaryung.livelab.application.kafkademo.KafkaMetricsService;
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
