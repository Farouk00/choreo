package com.orderplatform.notification;

import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.orderplatform.common.events.OrderStatusEvent;

class OrderStatusListenerTest {

    private final NotificationSender sender = mock(NotificationSender.class);
    private final OrderStatusListener listener = new OrderStatusListener(sender);

    @Test
    void notifiesCustomerOfConfirmation() {
        listener.onOrderStatus(new OrderStatusEvent(UUID.randomUUID(), "alice", "CONFIRMED", null, Instant.now()));

        verify(sender).send(eq("alice"), contains("is confirmed"));
    }

    @Test
    void includesReasonWhenRejected() {
        listener.onOrderStatus(new OrderStatusEvent(UUID.randomUUID(), "bob", "REJECTED",
                "Insufficient stock for MONITOR-27", Instant.now()));

        verify(sender).send(eq("bob"), contains("Insufficient stock for MONITOR-27"));
    }
}
