package com.orderplatform.order.messaging;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import com.orderplatform.common.events.InventoryResultEvent;
import com.orderplatform.common.events.Topics;
import com.orderplatform.order.domain.OrderService;

@Component
public class InventoryResultListener {

    private final OrderService orderService;

    public InventoryResultListener(OrderService orderService) {
        this.orderService = orderService;
    }

    @KafkaListener(topics = Topics.INVENTORY_RESULT)
    public void onInventoryResult(InventoryResultEvent event) {
        orderService.applyInventoryResult(event);
    }
}
