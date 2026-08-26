package uk.bit1.resilience4jcircuitbreakerdemo.email;

import java.math.BigDecimal;

public record EmailNotificationRequest(
        Long orderId,
        String customerEmail,
        String subject,
        BigDecimal amount) {
}
