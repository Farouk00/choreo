package com.orderplatform.inventory.domain;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.orderplatform.common.events.InventoryResultEvent;
import com.orderplatform.common.events.OrderCreatedEvent;
import com.orderplatform.common.events.OrderLine;

@Service
public class InventoryService {

    private static final Logger log = LoggerFactory.getLogger(InventoryService.class);

    private final ProductRepository products;
    private final ReservationRepository reservations;

    public InventoryService(ProductRepository products, ReservationRepository reservations) {
        this.products = products;
        this.reservations = reservations;
    }

    /**
     * Reserves every line of the order or none of them. Already-processed orders
     * return their recorded outcome, which makes the Kafka consumer idempotent.
     */
    @Transactional
    public InventoryResultEvent reserve(OrderCreatedEvent order) {
        Optional<Reservation> existing = reservations.findById(order.orderId());
        if (existing.isPresent()) {
            log.info("Order {} already processed, replaying result", order.orderId());
            return toEvent(existing.get());
        }

        Map<String, Integer> requested = new HashMap<>();
        for (OrderLine line : order.lines()) {
            requested.merge(line.sku(), line.quantity(), Integer::sum);
        }

        Map<String, Product> found = new HashMap<>();
        for (Map.Entry<String, Integer> entry : requested.entrySet()) {
            Optional<Product> product = products.findById(entry.getKey());
            if (product.isEmpty()) {
                return record(order, false, "Unknown SKU " + entry.getKey());
            }
            if (!product.get().canReserve(entry.getValue())) {
                return record(order, false, "Insufficient stock for " + entry.getKey());
            }
            found.put(entry.getKey(), product.get());
        }

        requested.forEach((sku, quantity) -> found.get(sku).reserve(quantity));
        return record(order, true, null);
    }

    @Transactional(readOnly = true)
    public List<Product> listProducts() {
        return products.findAll();
    }

    @Transactional(readOnly = true)
    public Optional<Product> findProduct(String sku) {
        return products.findById(sku);
    }

    @Transactional
    public Product restock(String sku, String name, int quantity) {
        Product product = products.findById(sku).orElseGet(() -> products.save(new Product(sku, name, 0)));
        product.restock(quantity);
        return product;
    }

    private InventoryResultEvent record(OrderCreatedEvent order, boolean reserved, String reason) {
        Reservation reservation = reservations.save(new Reservation(order.orderId(), reserved, reason));
        log.info("Order {} {}", order.orderId(), reserved ? "reserved" : "rejected: " + reason);
        return toEvent(reservation);
    }

    private static InventoryResultEvent toEvent(Reservation reservation) {
        return reservation.isReserved()
                ? InventoryResultEvent.reserved(reservation.getOrderId())
                : InventoryResultEvent.rejected(reservation.getOrderId(), reservation.getReason());
    }
}
