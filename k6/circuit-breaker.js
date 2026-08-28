import http from 'k6/http';
import { check, sleep } from 'k6';
import { Counter, Rate } from 'k6/metrics';

const BASE_URL = __ENV.BASE_URL || 'http://localhost:8081';

export const sentEmails = new Counter('sent_email_responses');
export const deferredEmails = new Counter('deferred_email_responses');
export const openCircuitResponses = new Counter('open_circuit_responses');
export const unexpectedResponses = new Rate('unexpected_responses');

export const options = {
  scenarios: {
    normal_email_delivery: {
      executor: 'constant-arrival-rate',
      rate: 8,
      timeUnit: '1s',
      duration: '10s',
      preAllocatedVUs: 8,
      maxVUs: 16,
    },
  },
  thresholds: {
    sent_email_responses: ['count>0'],
    deferred_email_responses: ['count==0'],
    open_circuit_responses: ['count==0'],
    unexpected_responses: ['rate<0.01'],
  },
};

export default function () {
  const id = `${__VU}-${__ITER}-${Date.now()}`;
  const payload = JSON.stringify({
    customerEmail: `circuit-breaker-${id}@example.com`,
    description: `Circuit breaker demo order ${id}`,
    amount: '42.50',
  });

  const response = http.post(`${BASE_URL}/api/orders`, payload, {
    headers: { 'Content-Type': 'application/json' },
    tags: { endpoint: 'POST /api/orders' },
  });

  let body = {};
  if (response.headers['Content-Type'] && response.headers['Content-Type'].includes('application/json')) {
    body = response.json();
  }

  const isSent = response.status === 201 && body.emailStatus === 'SENT';
  const isDeferred = response.status === 201 && body.emailStatus === 'EMAIL_DEFERRED';
  const reason = body.emailFailureReason || '';
  const isOpenCircuit = isDeferred && reason.includes('CallNotPermittedException');
  const expected = check(response, {
    'order created with sent email': () => isSent,
  });

  if (isSent) {
    sentEmails.add(1);
  }

  if (isDeferred) {
    deferredEmails.add(1);
  }

  if (isOpenCircuit) {
    openCircuitResponses.add(1);
  }

  if (!expected) {
    console.warn(`Unexpected response status=${response.status} emailStatus=${body.emailStatus || 'n/a'}`);
  }

  unexpectedResponses.add(!expected);
  sleep(0.1);
}
