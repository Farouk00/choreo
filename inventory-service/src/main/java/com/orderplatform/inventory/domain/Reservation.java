package com.orderplatform.inventory.domain;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;

/** One row per processed order, so a redelivered event returns the original outcome instead of reserving twice. */
@Entity
public class Reservation {

    @Id
    private UUID orderId;

    @Column(nullable = false)
    private boolean reserved;

    private String reason;

    @Column(nullable = false)
    private Instant processedAt;

    protected Reservation() {
    }

    public Reservation(UUID orderId, boolean reserved, String reason) {
        this.orderId = orderId;
        this.reserved = reserved;
        this.reason = reason;
        this.processedAt = Instant.now();
    }

    public UUID getOrderId() {
        return orderId;
    }

    public boolean isReserved() {
        return reserved;
    }

    public String getReason() {
        return reason;
    }
}
