package com.minyaryung.livelab.application.kafkademo;

import com.minyaryung.livelab.domain.kafkademo.OrderEvent;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
public class OrderProducer {

    private final KafkaTemplate<String, Object> template;
    private final String ordersTopic;

    public OrderProducer(KafkaTemplate<String, Object> template,
                         @Value("${livelab.kafka.topic.orders}") String ordersTopic) {
        this.template = template;
        this.ordersTopic = ordersTopic;
    }

    public void send(OrderEvent event) {
        template.send(ordersTopic, String.valueOf(event.orderId()), event);
    }
}
