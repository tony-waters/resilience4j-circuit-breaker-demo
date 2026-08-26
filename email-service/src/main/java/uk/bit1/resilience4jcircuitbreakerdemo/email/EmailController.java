package uk.bit1.resilience4jcircuitbreakerdemo.email;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/emails")
class EmailController {

    private static final Logger log = LoggerFactory.getLogger(EmailController.class);

    private final Duration processingDelay;
    private volatile boolean failing;

    EmailController(@Value("${demo.email-service.processing-delay:250ms}") Duration processingDelay) {
        this.processingDelay = processingDelay;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.ACCEPTED)
    Map<String, Object> send(@RequestBody EmailNotificationRequest request) throws InterruptedException {
        log.info("email-service started orderId={} delayMs={}", request.orderId(), processingDelay.toMillis());
        Thread.sleep(processingDelay.toMillis());

        if (failing) {
            log.warn("email-service failed orderId={} customerEmail={}", request.orderId(), request.customerEmail());
            throw new EmailServiceUnavailableException();
        }

        log.info("email-service completed orderId={} customerEmail={} subject={} amount={}",
                request.orderId(), request.customerEmail(), request.subject(), request.amount());

        return Map.of(
                "status", "ACCEPTED",
                "acceptedAt", Instant.now().toString(),
                "delayMs", processingDelay.toMillis());
    }

    @PostMapping("/failure-mode")
    Map<String, Object> setFailureMode(@RequestBody FailureModeRequest request) {
        failing = request.enabled();
        log.warn("email-service failure mode changed enabled={}", failing);
        return Map.of("failing", failing);
    }

    @GetMapping("/failure-mode")
    Map<String, Object> failureMode() {
        return Map.of("failing", failing);
    }

    record FailureModeRequest(boolean enabled) {
    }

    @ResponseStatus(HttpStatus.SERVICE_UNAVAILABLE)
    static class EmailServiceUnavailableException extends RuntimeException {
    }
}
