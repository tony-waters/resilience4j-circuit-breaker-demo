# Resilience4j Circuit Breaker Demo

Two Spring Boot applications in one Maven repo:

- `rest-service`: REST + JPA order API backed by Postgres. It calls the downstream email API through a Resilience4j circuit breaker.
- `email-service`: downstream email API that accepts order confirmation email requests.

The circuit breaker is configured with a count-based sliding window of 4 calls, a minimum of 4 calls, and a 50% failure-rate threshold. If the downstream email service is unavailable, the first failing calls still reach the downstream service. Once the threshold is crossed, the circuit moves to `OPEN`, later calls are rejected immediately with `CallNotPermittedException`, and the REST service falls back to `EMAIL_DEFERRED`. Orders are still saved.

## Build

```bash
./mvnw test
```

If the Maven wrapper is unavailable, run Maven directly:

```bash
mvn test
```

## Run

Run the full demo stack with Docker Compose:

```bash
docker compose up --build
```

Useful URLs:

- REST service: http://localhost:8081
- Email service: http://localhost:8082
- Postgres: `localhost:5432`, database `orders`, user `demo`, password `demo`
- REST health: http://localhost:8081/actuator/health
- Circuit breaker actuator endpoint: http://localhost:8081/actuator/circuitbreakers
- Circuit breaker events endpoint: http://localhost:8081/actuator/circuitbreakerevents

To run the services directly from Maven, start Postgres first:

```bash
docker compose up postgres
```

Start the email service:

```bash
mvn -pl email-service spring-boot:run
```

Start the REST service in another terminal:

```bash
mvn -pl rest-service spring-boot:run
```

## Try It Manually

Create a successful order:

```bash
curl -i -X POST http://localhost:8081/api/orders \
  -H 'Content-Type: application/json' \
  -d '{"customerEmail":"alice@example.com","description":"Demo order","amount":42.50}'
```

If the downstream service is unavailable and enough calls fail, the circuit breaker opens and the response will still be `201 Created`, with email work deferred:

```json
{
  "emailStatus": "EMAIL_DEFERRED",
  "emailFailureReason": "CallNotPermittedException"
}
```

After `wait-duration-in-open-state` passes, the circuit breaker moves to `HALF_OPEN`. Successful trial calls close it again.

## k6 Demo

Run the k6 test to send normal order traffic while the email service is healthy:

```bash
docker compose --profile test run --rm k6-circuit-breaker
```

Watch the REST service logs in another terminal:

```bash
docker compose logs -f rest-service
```

The k6 run should report `sent_email_responses` above zero and no deferred or open-circuit responses. If the downstream service is unavailable, the REST logs can include:

```text
circuit breaker error name=emailService
circuit breaker state transition name=emailService transition=State transition from CLOSED to OPEN
circuit breaker call not permitted name=emailService state=OPEN
```

## Why This Shows Circuit Breaker Behavior

A circuit breaker reacts to failure rate, not concurrent load. During the normal k6 run, `email-service` accepts requests and the circuit remains closed. If the downstream service becomes unavailable, the REST service records those failures. With the configured 4-call sliding window and 50% threshold, the circuit opens quickly. Once open, calls stop reaching `email-service` until the open-state wait duration elapses, which protects the REST workflow from repeatedly calling a failing dependency.
