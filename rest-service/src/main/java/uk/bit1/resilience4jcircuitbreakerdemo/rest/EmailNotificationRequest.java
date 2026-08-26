package uk.bit1.resilience4jcircuitbreakerdemo.rest;

import java.math.BigDecimal;

public record EmailNotificationRequest(
        Long orderId,
        String customerEmail,
        String subject,
        BigDecimal amount) {
}
