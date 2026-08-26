package uk.bit1.resilience4jcircuitbreakerdemo.rest;

import java.math.BigDecimal;
import java.time.Instant;

public record OrderResponse(
        Long id,
        String customerEmail,
        String description,
        BigDecimal amount,
        String emailStatus,
        String emailFailureReason,
        Instant createdAt) {

    static OrderResponse from(Order order) {
        return new OrderResponse(
                order.getId(),
                order.getCustomerEmail(),
                order.getDescription(),
                order.getAmount(),
                order.getEmailStatus(),
                order.getEmailFailureReason(),
                order.getCreatedAt());
    }
}
