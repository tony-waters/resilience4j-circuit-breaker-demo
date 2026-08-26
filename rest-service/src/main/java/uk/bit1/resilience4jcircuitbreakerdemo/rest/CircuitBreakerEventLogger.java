package uk.bit1.resilience4jcircuitbreakerdemo.rest;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
class CircuitBreakerEventLogger {

    private static final Logger log = LoggerFactory.getLogger(CircuitBreakerEventLogger.class);

    private final CircuitBreakerRegistry circuitBreakerRegistry;

    CircuitBreakerEventLogger(CircuitBreakerRegistry circuitBreakerRegistry) {
        this.circuitBreakerRegistry = circuitBreakerRegistry;
    }

    @EventListener(ApplicationReadyEvent.class)
    void registerCircuitBreakerLogging() {
        CircuitBreaker circuitBreaker = circuitBreakerRegistry.circuitBreaker("emailService");

        circuitBreaker.getEventPublisher()
                .onSuccess(event -> log.info(
                        "circuit breaker success name={} state={} failureRate={}",
                        event.getCircuitBreakerName(),
                        circuitBreaker.getState(),
                        circuitBreaker.getMetrics().getFailureRate()))
                .onError(event -> log.warn(
                        "circuit breaker error name={} state={} failureRate={}",
                        event.getCircuitBreakerName(),
                        circuitBreaker.getState(),
                        circuitBreaker.getMetrics().getFailureRate()))
                .onStateTransition(event -> log.warn(
                        "circuit breaker state transition name={} transition={}",
                        event.getCircuitBreakerName(),
                        event.getStateTransition()))
                .onCallNotPermitted(event -> log.warn(
                        "circuit breaker call not permitted name={} state={}",
                        event.getCircuitBreakerName(),
                        circuitBreaker.getState()));
    }
}
