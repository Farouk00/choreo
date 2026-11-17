package com.orderplatform.order.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.common.TopicPartition;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.util.backoff.FixedBackOff;

import com.orderplatform.common.events.Topics;

@Configuration
public class KafkaConfig {

    @Bean
    NewTopic orderCreatedTopic() {
        return TopicBuilder.name(Topics.ORDER_CREATED).partitions(3).build();
    }

    @Bean
    NewTopic orderStatusTopic() {
        return TopicBuilder.name(Topics.ORDER_STATUS).partitions(3).build();
    }

    @Bean
    NewTopic inventoryResultTopic() {
        return TopicBuilder.name(Topics.INVENTORY_RESULT).partitions(3).build();
    }

    /**
     * Retries a failed record 3 times, then parks it on "<topic>.DLT" instead of blocking the partition.
     * Partition -1 lets Kafka choose, so the DLT does not need as many partitions as the source topic.
     */
    @Bean
    DefaultErrorHandler kafkaErrorHandler(KafkaTemplate<String, Object> template) {
        return new DefaultErrorHandler(new DeadLetterPublishingRecoverer(template,
                (record, ex) -> new TopicPartition(record.topic() + ".DLT", -1)), new FixedBackOff(1000L, 3));
    }
}
