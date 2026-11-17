package com.orderplatform.order.api;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import com.orderplatform.order.domain.Order;

public record OrderResponse(UUID id, String customerId, String status, String reason, Instant createdAt,
        List<Line> lines) {

    public record Line(String sku, int quantity) {
    }

    public static OrderResponse from(Order order) {
        return new OrderResponse(order.getId(), order.getCustomerId(), order.getStatus().name(), order.getReason(),
                order.getCreatedAt(),
                order.getItems().stream().map(i -> new Line(i.getSku(), i.getQuantity())).toList());
    }
}
