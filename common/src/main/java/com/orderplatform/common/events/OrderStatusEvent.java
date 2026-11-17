package com.orderplatform.common.events;

import java.time.Instant;
import java.util.UUID;

public record OrderStatusEvent(UUID orderId, String customerId, String status, String reason, Instant occurredAt) {
}
