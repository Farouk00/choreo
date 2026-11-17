package com.orderplatform.common.events;

/** Kafka topic names shared by every service. */
public final class Topics {

    /** Published by order-service when a customer places an order. */
    public static final String ORDER_CREATED = "orders.created";

    /** Published by inventory-service once stock has been reserved or rejected. */
    public static final String INVENTORY_RESULT = "inventory.result";

    /** Published by order-service whenever an order reaches a final status. */
    public static final String ORDER_STATUS = "orders.status";

    private Topics() {
    }
}
