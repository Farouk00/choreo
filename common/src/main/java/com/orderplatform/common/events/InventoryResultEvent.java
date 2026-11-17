package com.orderplatform.common.events;

import java.util.UUID;

public record InventoryResultEvent(UUID orderId, boolean reserved, String reason) {

    public static InventoryResultEvent reserved(UUID orderId) {
        return new InventoryResultEvent(orderId, true, null);
    }

    public static InventoryResultEvent rejected(UUID orderId, String reason) {
        return new InventoryResultEvent(orderId, false, reason);
    }
}
