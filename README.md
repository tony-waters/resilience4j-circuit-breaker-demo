# Resilience4j Circuit Breaker Demo

Two Spring Boot applications in one Maven repo:

- `rest-service`: REST + JPA order API backed by Postgres. It calls the downstream email API through a Resilience4j circuit breaker.
- `email-service`: downstream email API with a toggleable failure mode so the circuit breaker can record failures and open.

The circuit breaker is configured with a count-based sliding window of 4 calls, a minimum of 4 calls, and a 50% failure-rate threshold. When `email-service` is put into failure mode, the first failing calls still reach the downstream service. Once the threshold is crossed, the circuit moves to `OPEN`, later calls are rejected immediately with `CallNotPermittedException`, and the REST service falls back to `EMAIL_DEFERRED`. Orders are still saved.

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

Turn on downstream failures:

```bash
curl -i -X POST http://localhost:8082/emails/failure-mode \
  -H 'Content-Type: application/json' \
  -d '{"enabled":true}'
```

Create several more orders. After 4 measured calls, the circuit breaker should open and the response will still be `201 Created`, with email work deferred:

```json
{
  "emailStatus": "EMAIL_DEFERRED",
  "emailFailureReason": "CallNotPermittedException"
}
```

Turn downstream failures back off:

```bash
curl -i -X POST http://localhost:8082/emails/failure-mode \
  -H 'Content-Type: application/json' \
  -d '{"enabled":false}'
```

After `wait-duration-in-open-state` passes, the circuit breaker moves to `HALF_OPEN`. Successful trial calls close it again.

## k6 Demo

Run the k6 test to enable email failure mode, send enough orders to open the circuit, and disable failure mode at the end:

```bash
docker compose --profile test run --rm k6-circuit-breaker
```

Watch the REST service logs in another terminal:

```bash
docker compose logs -f rest-service
```

The k6 run should report `deferred_email_responses` and `open_circuit_responses` above zero. The REST logs should include:

```text
circuit breaker error name=emailService
circuit breaker state transition name=emailService transition=State transition from CLOSED to OPEN
circuit breaker call not permitted name=emailService state=OPEN
```

## Why This Shows Circuit Breaker Behavior

A circuit breaker reacts to failure rate, not concurrent load. This demo deliberately makes `email-service` return `503 Service Unavailable`, so the REST service records downstream failures. With the configured 4-call sliding window and 50% threshold, the circuit opens quickly. Once open, calls stop reaching `email-service` until the open-state wait duration elapses, which protects the REST workflow from repeatedly calling a failing dependency.
