package com.orderplatform.notification;

/** Delivery channel for customer notifications (email, SMS, push...). */
public interface NotificationSender {

    void send(String customerId, String message);
}
