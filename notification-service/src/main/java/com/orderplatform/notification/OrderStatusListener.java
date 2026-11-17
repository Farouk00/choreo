package com.orderplatform.notification;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import com.orderplatform.common.events.OrderStatusEvent;
import com.orderplatform.common.events.Topics;

@Component
public class OrderStatusListener {

    private final NotificationSender sender;

    public OrderStatusListener(NotificationSender sender) {
        this.sender = sender;
    }

    @KafkaListener(topics = Topics.ORDER_STATUS)
    public void onOrderStatus(OrderStatusEvent event) {
        sender.send(event.customerId(), messageFor(event));
    }

    static String messageFor(OrderStatusEvent event) {
        return switch (event.status()) {
            case "CONFIRMED" -> "Your order " + event.orderId() + " is confirmed.";
            case "REJECTED" -> "Sorry, your order " + event.orderId() + " could not be fulfilled: " + event.reason();
            default -> "Your order " + event.orderId() + " is now " + event.status() + ".";
        };
    }
}
