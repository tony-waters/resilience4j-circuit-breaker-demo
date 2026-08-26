package uk.bit1.resilience4jcircuitbreakerdemo.rest;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateOrderRequest(
        @Email @NotBlank String customerEmail,
        @NotBlank String description,
        @NotNull @DecimalMin("0.01") BigDecimal amount) {
}
