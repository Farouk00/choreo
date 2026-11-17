package com.orderplatform.order;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import java.time.Duration;
import java.util.List;
import java.util.Map;

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

import com.orderplatform.common.events.InventoryResultEvent;
import com.orderplatform.common.events.Topics;
import com.orderplatform.order.domain.Order;
import com.orderplatform.order.domain.OrderItem;
import com.orderplatform.order.domain.OrderRepository;
import com.orderplatform.order.domain.OrderService;
import com.orderplatform.order.domain.OrderStatus;

/** Runs the order side of the saga against an in-memory Kafka broker and H2 database. */
@SpringBootTest(properties = {
        "spring.kafka.bootstrap-servers=${spring.embedded.kafka.brokers}",
        "spring.datasource.url=jdbc:h2:mem:orders;DB_CLOSE_DELAY=-1",
        "spring.datasource.username=sa",
        "spring.datasource.password="
})
@EmbeddedKafka(partitions = 1, topics = { Topics.ORDER_CREATED, Topics.INVENTORY_RESULT, Topics.ORDER_STATUS })
class OrderFlowIntegrationTest {

    @Autowired
    OrderService orderService;

    @Autowired
    OrderRepository orders;

    @Autowired
    KafkaTemplate<String, Object> kafka;

    @Autowired
    EmbeddedKafkaBroker broker;

    Consumer<String, String> consumer;

    @BeforeEach
    void subscribe() {
        Map<String, Object> props = KafkaTestUtils.consumerProps("test-" + System.nanoTime(), "false", broker);
        props.put("auto.offset.reset", "earliest");
        consumer = new DefaultKafkaConsumerFactory<>(props, new StringDeserializer(), new StringDeserializer())
                .createConsumer();
        broker.consumeFromEmbeddedTopics(consumer, Topics.ORDER_CREATED, Topics.ORDER_STATUS);
    }

    @AfterEach
    void close() {
        consumer.close();
    }

    @Test
    void placedOrderIsPublishedThenConfirmedByInventoryResult() {
        Order order = orderService.placeOrder("alice", List.of(new OrderItem("KEYBOARD-01", 2)));

        ConsumerRecord<String, String> created = KafkaTestUtils.getSingleRecord(consumer, Topics.ORDER_CREATED,
                Duration.ofSeconds(10));
        assertThat(created.key()).isEqualTo(order.getId().toString());
        assertThat(created.value()).contains("KEYBOARD-01");

        kafka.send(Topics.INVENTORY_RESULT, order.getId().toString(), InventoryResultEvent.reserved(order.getId()));

        await().atMost(Duration.ofSeconds(10)).untilAsserted(() ->
                assertThat(orders.findById(order.getId()).orElseThrow().getStatus())
                        .isEqualTo(OrderStatus.CONFIRMED));

        ConsumerRecord<String, String> status = KafkaTestUtils.getSingleRecord(consumer, Topics.ORDER_STATUS,
                Duration.ofSeconds(10));
        assertThat(status.value()).contains("CONFIRMED").contains("alice");
    }
}
