package com.orderplatform.order.domain;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.orderplatform.common.events.InventoryResultEvent;
import com.orderplatform.common.events.OrderCreatedEvent;
import com.orderplatform.common.events.OrderLine;
import com.orderplatform.common.events.OrderStatusEvent;

@Service
public class OrderService {

    private static final Logger log = LoggerFactory.getLogger(OrderService.class);

    private final OrderRepository orders;
    private final ApplicationEventPublisher events;

    public OrderService(OrderRepository orders, ApplicationEventPublisher events) {
        this.orders = orders;
        this.events = events;
    }

    /**
     * Saves the order as PENDING. The Kafka event is published only after the
     * transaction commits (see OrderEventPublisher), so consumers never see an
     * order that was rolled back.
     */
    @Transactional
    public Order placeOrder(String customerId, List<OrderItem> items) {
        Order order = orders.save(new Order(customerId, items));
        List<OrderLine> lines = items.stream().map(i -> new OrderLine(i.getSku(), i.getQuantity())).toList();
        events.publishEvent(new OrderCreatedEvent(order.getId(), customerId, lines, order.getCreatedAt()));
        log.info("Order {} placed by {}", order.getId(), customerId);
        return order;
    }

    /** Applies the inventory outcome. Duplicate deliveries are ignored, so the consumer is idempotent. */
    @Transactional
    public void applyInventoryResult(InventoryResultEvent result) {
        Order order = orders.findById(result.orderId()).orElseThrow(() -> new OrderNotFoundException(result.orderId()));
        if (!order.isPending()) {
            log.info("Ignoring duplicate inventory result for order {} (already {})", order.getId(), order.getStatus());
            return;
        }
        if (result.reserved()) {
            order.confirm();
        } else {
            order.reject(result.reason());
        }
        events.publishEvent(new OrderStatusEvent(order.getId(), order.getCustomerId(), order.getStatus().name(),
                order.getReason(), Instant.now()));
        log.info("Order {} is now {}", order.getId(), order.getStatus());
    }

    @Transactional(readOnly = true)
    public Order getOrder(UUID id) {
        return orders.findById(id).orElseThrow(() -> new OrderNotFoundException(id));
    }

    @Transactional(readOnly = true)
    public List<Order> getOrdersFor(String customerId) {
        return orders.findByCustomerIdOrderByCreatedAtDesc(customerId);
    }
}
