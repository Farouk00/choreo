package com.orderplatform.common.events;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record OrderCreatedEvent(UUID orderId, String customerId, List<OrderLine> lines, Instant createdAt) {
}
