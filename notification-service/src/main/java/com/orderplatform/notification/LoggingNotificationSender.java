package com.orderplatform.notification;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/** Stand-in for a real email/SMS provider: writes the notification to the log. */
@Component
public class LoggingNotificationSender implements NotificationSender {

    private static final Logger log = LoggerFactory.getLogger(LoggingNotificationSender.class);

    @Override
    public void send(String customerId, String message) {
        log.info("NOTIFY {} -> {}", customerId, message);
    }
}
