package uk.bit1.resilience4jcircuitbreakerdemo.rest;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
class EmailClient {

    private static final Logger log = LoggerFactory.getLogger(EmailClient.class);

    private final RestClient emailRestClient;

    EmailClient(RestClient emailRestClient) {
        this.emailRestClient = emailRestClient;
    }

    @CircuitBreaker(name = "emailService", fallbackMethod = "emailFallback")
    public EmailDeliveryResult sendOrderConfirmation(Order order) {
        log.info("calling email-service orderId={}", order.getId());
        EmailNotificationRequest request = new EmailNotificationRequest(
                order.getId(),
                order.getCustomerEmail(),
                "Order confirmation " + order.getId(),
                order.getAmount());

        emailRestClient.post()
                .uri("/emails")
                .body(request)
                .retrieve()
                .toBodilessEntity();

        return EmailDeliveryResult.sent();
    }

    EmailDeliveryResult emailFallback(Order order, Throwable cause) {
        log.warn("email delivery deferred orderId={} reason={}",
                order.getId(), cause.getClass().getSimpleName());
        return EmailDeliveryResult.deferred(cause.getClass().getSimpleName());
    }
}
