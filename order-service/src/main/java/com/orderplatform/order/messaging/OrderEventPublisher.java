package com.orderplatform.order.messaging;

import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;

import com.orderplatform.common.events.OrderCreatedEvent;
import com.orderplatform.common.events.OrderStatusEvent;
import com.orderplatform.common.events.Topics;

/** Forwards domain events to Kafka once the database transaction has committed. */
@Component
public class OrderEventPublisher {

    private final KafkaTemplate<String, Object> kafka;

    public OrderEventPublisher(KafkaTemplate<String, Object> kafka) {
        this.kafka = kafka;
    }

    @TransactionalEventListener
    public void on(OrderCreatedEvent event) {
        kafka.send(Topics.ORDER_CREATED, event.orderId().toString(), event);
    }

    @TransactionalEventListener
    public void on(OrderStatusEvent event) {
        kafka.send(Topics.ORDER_STATUS, event.orderId().toString(), event);
    }
}
