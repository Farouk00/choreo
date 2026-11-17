package com.orderplatform.inventory.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Version;

@Entity
public class Product {

    @Id
    private String sku;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private int available;

    /** Optimistic locking: two concurrent reservations on the same SKU cannot both win. */
    @Version
    private Long version;

    protected Product() {
    }

    public Product(String sku, String name, int available) {
        this.sku = sku;
        this.name = name;
        this.available = available;
    }

    public boolean canReserve(int quantity) {
        return available >= quantity;
    }

    public void reserve(int quantity) {
        if (!canReserve(quantity)) {
            throw new IllegalStateException("Insufficient stock for " + sku);
        }
        available -= quantity;
    }

    public void restock(int quantity) {
        available += quantity;
    }

    public String getSku() {
        return sku;
    }

    public String getName() {
        return name;
    }

    public int getAvailable() {
        return available;
    }
}
