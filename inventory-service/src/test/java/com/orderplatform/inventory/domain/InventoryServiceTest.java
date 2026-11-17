package com.orderplatform.inventory.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;

import com.orderplatform.common.events.InventoryResultEvent;
import com.orderplatform.common.events.OrderCreatedEvent;
import com.orderplatform.common.events.OrderLine;

@DataJpaTest(properties = "inventory.seed-demo-data=false")
@Import(InventoryService.class)
class InventoryServiceTest {

    @Autowired
    InventoryService service;

    @Autowired
    ProductRepository products;

    @BeforeEach
    void stock() {
        products.save(new Product("KEYBOARD-01", "Keyboard", 10));
        products.save(new Product("MONITOR-27", "Monitor", 1));
    }

    @Test
    void reservesAllLinesWhenStockIsAvailable() {
        InventoryResultEvent result = service.reserve(order(line("KEYBOARD-01", 3), line("MONITOR-27", 1)));

        assertThat(result.reserved()).isTrue();
        assertThat(available("KEYBOARD-01")).isEqualTo(7);
        assertThat(available("MONITOR-27")).isZero();
    }

    @Test
    void rejectsWholeOrderWhenOneLineIsShort() {
        InventoryResultEvent result = service.reserve(order(line("KEYBOARD-01", 3), line("MONITOR-27", 2)));

        assertThat(result.reserved()).isFalse();
        assertThat(result.reason()).contains("MONITOR-27");
        assertThat(available("KEYBOARD-01")).as("no partial reservation").isEqualTo(10);
    }

    @Test
    void rejectsUnknownSku() {
        InventoryResultEvent result = service.reserve(order(line("DOES-NOT-EXIST", 1)));

        assertThat(result.reserved()).isFalse();
        assertThat(result.reason()).contains("Unknown SKU");
    }

    @Test
    void sumsDuplicateSkuLinesBeforeChecking() {
        InventoryResultEvent result = service.reserve(order(line("MONITOR-27", 1), line("MONITOR-27", 1)));

        assertThat(result.reserved()).isFalse();
        assertThat(available("MONITOR-27")).isEqualTo(1);
    }

    @Test
    void redeliveredEventDoesNotReserveTwice() {
        OrderCreatedEvent order = order(line("KEYBOARD-01", 4));

        InventoryResultEvent first = service.reserve(order);
        InventoryResultEvent second = service.reserve(order);

        assertThat(second).isEqualTo(first);
        assertThat(available("KEYBOARD-01")).isEqualTo(6);
    }

    private int available(String sku) {
        return products.findById(sku).orElseThrow().getAvailable();
    }

    private static OrderLine line(String sku, int quantity) {
        return new OrderLine(sku, quantity);
    }

    private static OrderCreatedEvent order(OrderLine... lines) {
        return new OrderCreatedEvent(UUID.randomUUID(), "alice", List.of(lines), Instant.now());
    }
}
