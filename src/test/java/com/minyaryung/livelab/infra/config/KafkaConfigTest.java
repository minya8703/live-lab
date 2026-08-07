package com.minyaryung.livelab.infra.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class KafkaConfigTest {

    private final KafkaConfig config = new KafkaConfig();

    @Test
    void dltHasSamePartitionCountAsSourceTopic() {
        NewTopic orders = config.ordersTopic("livelab.orders");
        NewTopic dlt = config.ordersDltTopic("livelab.orders.DLT");

        assertThat(orders.numPartitions()).isEqualTo(3);
        assertThat(dlt.numPartitions()).isEqualTo(orders.numPartitions());
    }
}
