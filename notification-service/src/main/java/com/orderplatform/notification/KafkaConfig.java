package com.orderplatform.notification;

import org.apache.kafka.common.TopicPartition;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.util.backoff.FixedBackOff;

@Configuration
public class KafkaConfig {

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
