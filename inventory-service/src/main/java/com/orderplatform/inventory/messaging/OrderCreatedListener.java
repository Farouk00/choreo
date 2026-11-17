package com.orderplatform.inventory.messaging;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import com.orderplatform.common.events.InventoryResultEvent;
import com.orderplatform.common.events.OrderCreatedEvent;
import com.orderplatform.common.events.Topics;
import com.orderplatform.inventory.domain.InventoryService;

@Component
public class OrderCreatedListener {

    private final InventoryService inventoryService;
    private final KafkaTemplate<String, Object> kafka;

    public OrderCreatedListener(InventoryService inventoryService, KafkaTemplate<String, Object> kafka) {
        this.inventoryService = inventoryService;
        this.kafka = kafka;
    }

    /**
     * The reservation commits first, then the result is sent. If the send fails the
     * record is redelivered and the stored outcome is replayed, so stock is never reserved twice.
     */
    @KafkaListener(topics = Topics.ORDER_CREATED)
    public void onOrderCreated(OrderCreatedEvent event) throws Exception {
        InventoryResultEvent result = inventoryService.reserve(event);
        kafka.send(Topics.INVENTORY_RESULT, result.orderId().toString(), result).get();
    }
}
