package com.orderplatform.inventory;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.test.EmbeddedKafkaBroker;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.kafka.test.utils.KafkaTestUtils;

import com.orderplatform.common.events.OrderCreatedEvent;
import com.orderplatform.common.events.OrderLine;
import com.orderplatform.common.events.Topics;
import com.orderplatform.inventory.domain.ProductRepository;

/** Publishes an OrderCreatedEvent and checks inventory-service answers on the result topic. */
@SpringBootTest(properties = {
        "spring.kafka.bootstrap-servers=${spring.embedded.kafka.brokers}",
        "spring.datasource.url=jdbc:h2:mem:inventory;DB_CLOSE_DELAY=-1",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "inventory.seed-demo-data=true"
})
@EmbeddedKafka(partitions = 1, topics = { Topics.ORDER_CREATED, Topics.INVENTORY_RESULT })
class InventoryKafkaIntegrationTest {

    @Autowired
    KafkaTemplate<String, Object> kafka;

    @Autowired
    EmbeddedKafkaBroker broker;

    @Autowired
    ProductRepository products;

    Consumer<String, String> consumer;

    @BeforeEach
    void subscribe() {
        Map<String, Object> props = KafkaTestUtils.consumerProps("test-" + System.nanoTime(), "false", broker);
        props.put("auto.offset.reset", "earliest");
        consumer = new DefaultKafkaConsumerFactory<>(props, new StringDeserializer(), new StringDeserializer())
                .createConsumer();
        broker.consumeFromEmbeddedTopics(consumer, Topics.INVENTORY_RESULT);
    }

    @AfterEach
    void close() {
        consumer.close();
    }

    @Test
    void orderCreatedEventReservesStockAndPublishesResult() {
        int before = products.findById("MOUSE-01").orElseThrow().getAvailable();
        UUID orderId = UUID.randomUUID();

        kafka.send(Topics.ORDER_CREATED, orderId.toString(),
                new OrderCreatedEvent(orderId, "alice", List.of(new OrderLine("MOUSE-01", 2)), Instant.now()));

        ConsumerRecord<String, String> result = KafkaTestUtils.getSingleRecord(consumer, Topics.INVENTORY_RESULT,
                Duration.ofSeconds(10));
        assertThat(result.key()).isEqualTo(orderId.toString());
        assertThat(result.value()).contains("\"reserved\":true");
        assertThat(products.findById("MOUSE-01").orElseThrow().getAvailable()).isEqualTo(before - 2);
    }
}
