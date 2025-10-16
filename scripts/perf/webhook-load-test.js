import http from 'k6/http';
import { check, sleep } from 'k6';
import { Counter, Trend } from 'k6/metrics';
import { hmac } from 'k6/crypto';

const BASE_URL = __ENV.BASE_URL ?? 'http://localhost:8080';
const WEBHOOK_SECRET = __ENV.WEBHOOK_SECRET ?? 'test-secret';
const IDEM_PREFIX = __ENV.IDEM_PREFIX ?? 'k6-idem';

export const options = {
    scenarios: {
        steady_load: {
            executor: 'constant-arrival-rate',
            rate: Number(__ENV.RATE ?? 50),
            timeUnit: '1s',
            duration: __ENV.DURATION ?? '2m',
            preAllocatedVUs: Number(__ENV.VUS ?? 20),
            maxVUs: Number(__ENV.MAX_VUS ?? 100),
        },
        stress_spike: {
            executor: 'ramping-arrival-rate',
            startRate: Number(__ENV.SPIKE_START_RATE ?? 20),
            timeUnit: '1s',
            stages: [
                { duration: __ENV.SPIKE_RAMP ?? '1m', target: Number(__ENV.SPIKE_TARGET_RATE ?? 200) },
                { duration: __ENV.SPIKE_HOLD ?? '30s', target: Number(__ENV.SPIKE_TARGET_RATE ?? 200) },
                { duration: __ENV.SPIKE_COOL ?? '1m', target: 0 },
            ],
            startTime: __ENV.SPIKE_START ?? '2m',
            preAllocatedVUs: Number(__ENV.SPIKE_VUS ?? 50),
            maxVUs: Number(__ENV.SPIKE_MAX_VUS ?? 200),
        },
    },
    thresholds: {
        http_req_duration: ['p(95)<500', 'p(99)<800'],
        http_req_failed: ['rate<0.01'],
        webhook_processing: ['p(95)<400'],
        webhook_success: ['rate>0.98'],
    },
};

const webhookProcessing = new Trend('webhook_processing', true);
const webhookSuccess = new Counter('webhook_success');

export default function () {
    const payload = buildPayload();
    const signature = hmac('sha256', WEBHOOK_SECRET, payload, 'hex');
    const idemKey = `${IDEM_PREFIX}-${__ITER}-${Date.now()}`;

    const res = http.post(
        `${BASE_URL}/api/webhooks/payments`,
        payload,
        {
            headers: {
                'Content-Type': 'application/json',
                'X-Payment-Signature': signature,
                'Idempotency-Key': idemKey,
                'X-Tenant-Id': 'tenant-load',
            },
        },
    );

    webhookProcessing.add(res.timings.duration);

    const success = check(res, {
        'status is 200': r => r.status === 200,
    });
    if (success) {
        webhookSuccess.add(1);
    }

    sleep(Number(__ENV.SLEEP ?? 0.1));
}

function buildPayload() {
    const now = new Date().toISOString();
    const amount = Math.floor(Math.random() * 2_000_000) + 10_000;
    const orderId = `U:perf:${__ITER}:${Math.random().toString(36).slice(2, 8)}`;

    return JSON.stringify({
        event_id: `evt_${__ITER}_${Date.now()}`,
        status: 'APPROVED',
        amount,
        currency: 'KRW',
        merchant: 'Load Test Merchant',
        channel: 'ONLINE',
        occurred_at: now,
        paymentKey: `pay_${__ITER}_${Date.now()}`,
        orderId,
        card: {
            brand: 'VISA',
            last4: '1234',
        },
    });
}
