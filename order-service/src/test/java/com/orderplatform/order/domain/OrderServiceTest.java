package com.orderplatform.order.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import com.orderplatform.common.events.InventoryResultEvent;
import com.orderplatform.common.events.OrderCreatedEvent;
import com.orderplatform.common.events.OrderStatusEvent;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    OrderRepository orders;

    @Mock
    ApplicationEventPublisher events;

    @InjectMocks
    OrderService service;

    @Test
    void placeOrderSavesPendingOrderAndRaisesCreatedEvent() {
        when(orders.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));

        Order order = service.placeOrder("alice", List.of(new OrderItem("KEYBOARD-01", 2)));

        assertThat(order.getStatus()).isEqualTo(OrderStatus.PENDING);
        ArgumentCaptor<OrderCreatedEvent> event = ArgumentCaptor.forClass(OrderCreatedEvent.class);
        verify(events).publishEvent(event.capture());
        assertThat(event.getValue().orderId()).isEqualTo(order.getId());
        assertThat(event.getValue().lines()).singleElement()
                .satisfies(l -> assertThat(l.sku()).isEqualTo("KEYBOARD-01"));
    }

    @Test
    void reservedInventoryConfirmsOrder() {
        Order order = pendingOrder();

        service.applyInventoryResult(InventoryResultEvent.reserved(order.getId()));

        assertThat(order.getStatus()).isEqualTo(OrderStatus.CONFIRMED);
        verify(events).publishEvent(any(OrderStatusEvent.class));
    }

    @Test
    void rejectedInventoryRejectsOrderWithReason() {
        Order order = pendingOrder();

        service.applyInventoryResult(InventoryResultEvent.rejected(order.getId(), "Insufficient stock"));

        assertThat(order.getStatus()).isEqualTo(OrderStatus.REJECTED);
        assertThat(order.getReason()).isEqualTo("Insufficient stock");
    }

    @Test
    void duplicateInventoryResultIsIgnored() {
        Order order = pendingOrder();
        order.confirm();

        service.applyInventoryResult(InventoryResultEvent.rejected(order.getId(), "late duplicate"));

        assertThat(order.getStatus()).isEqualTo(OrderStatus.CONFIRMED);
        verify(events, never()).publishEvent(any());
    }

    @Test
    void unknownOrderThrows() {
        UUID id = UUID.randomUUID();
        when(orders.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.applyInventoryResult(InventoryResultEvent.reserved(id)))
                .isInstanceOf(OrderNotFoundException.class);
    }

    private Order pendingOrder() {
        Order order = new Order("alice", List.of(new OrderItem("KEYBOARD-01", 1)));
        when(orders.findById(order.getId())).thenReturn(Optional.of(order));
        return order;
    }
}
