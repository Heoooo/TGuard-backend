import http from "k6/http";
import { check, sleep } from "k6";
import { Counter, Trend } from "k6/metrics";
import { hmac } from "k6/crypto";

const BASE_URL = __ENV.BASE_URL || "http://localhost:8080";
const WEBHOOK_SECRET = __ENV.WEBHOOK_SECRET || "test-secret";
const IDEM_PREFIX = __ENV.IDEM_PREFIX || "k6-idem";
const TENANT_ID = __ENV.TENANT_ID || "tenant-load";
const TEST_PHONE = __ENV.TEST_PHONE || undefined;
const STATUS = __ENV.STATUS || "APPROVED";
const CARD_BRAND = __ENV.CARD_BRAND || "VISA";
const CARD_LAST4 = __ENV.CARD_LAST4 || "9191";
const MIN_AMOUNT = Number(__ENV.MIN_AMOUNT || 3500000);
const MAX_AMOUNT = Number(__ENV.MAX_AMOUNT || 5000000);
const ORDER_USER = __ENV.ORDER_USER || "operator01";
const ORDER_PREFIX = __ENV.ORDER_PREFIX || "ord";
const PAYMENT_PREFIX = __ENV.PAYMENT_PREFIX || "pay";
const EVENT_PREFIX = __ENV.EVENT_PREFIX || "evt";
const MERCHANT = __ENV.MERCHANT || "Load Test Merchant";
const CHANNEL = __ENV.CHANNEL || "WEB";
const DEVICE_INFO = __ENV.DEVICE_INFO || "k6-load-test";

export const options = {
  scenarios: {
    steady_load: {
      executor: "constant-arrival-rate",
      rate: Number(__ENV.RATE || 50),
      timeUnit: "1s",
      duration: __ENV.DURATION || "2m",
      preAllocatedVUs: Number(__ENV.VUS || 20),
      maxVUs: Number(__ENV.MAX_VUS || 100),
    },
    stress_spike: {
      executor: "ramping-arrival-rate",
      startRate: Number(__ENV.SPIKE_START_RATE || 20),
      timeUnit: "1s",
      stages: [
        {
          duration: __ENV.SPIKE_RAMP || "1m",
          target: Number(__ENV.SPIKE_TARGET_RATE || 200),
        },
        {
          duration: __ENV.SPIKE_HOLD || "30s",
          target: Number(__ENV.SPIKE_TARGET_RATE || 200),
        },
        { duration: __ENV.SPIKE_COOL || "1m", target: 0 },
      ],
      startTime: __ENV.SPIKE_START || "2m",
      preAllocatedVUs: Number(__ENV.SPIKE_VUS || 50),
      maxVUs: Number(__ENV.SPIKE_MAX_VUS || 200),
    },
  },
  thresholds: {
    http_req_duration: ["p(95)<500", "p(99)<800"],
    http_req_failed: ["rate<0.01"],
    webhook_processing: ["p(95)<400"],
    webhook_success: ["rate>0.98"],
  },
};

const webhookProcessing = new Trend("webhook_processing", true);
const webhookSuccess = new Counter("webhook_success");

export default function () {
  const payload = buildPayload();
  const signature = hmac("sha256", WEBHOOK_SECRET, payload, "hex");
  const idemKey = `${IDEM_PREFIX}-${__ITER}-${Date.now()}`;

  const res = http.post(`${BASE_URL}/api/webhooks/payments`, payload, {
    headers: {
      "Content-Type": "application/json",
      "X-Payment-Signature": signature,
      "Idempotency-Key": idemKey,
      "X-Tenant-Id": TENANT_ID,
    },
  });

  webhookProcessing.add(res.timings.duration);

  const success = check(res, {
    "status is 200": (r) => r.status === 200,
  });

  if (!success) {
    console.log(`FAIL status=${res.status} body=${res.body}`);
  }

  if (success) {
    webhookSuccess.add(1);
  }

  sleep(Number(__ENV.SLEEP || 0.1));
}

function buildPayload() {
  const now = new Date().toISOString();
  const amount = randomAmount();
  const unique = `${__VU}-${__ITER}-${Date.now().toString(36)}`;
  const orderId = `U:${ORDER_USER}:${ORDER_PREFIX}-${unique}`;
  const eventId = `${EVENT_PREFIX}-${unique}`;
  const paymentKey = `${PAYMENT_PREFIX}-${unique}`;

  const payload = {
    eventId,
    status: STATUS,
    brand: CARD_BRAND,
    last4: CARD_LAST4,
    amount,
    currency: "KRW",
    merchant: MERCHANT,
    channel: CHANNEL,
    deviceInfo: DEVICE_INFO,
    occurredAt: now,
    paymentKey,
    orderId,
  };

  if (TEST_PHONE) {
    payload.testPhoneNumber = TEST_PHONE;
  }

  return JSON.stringify(payload);
}

function randomAmount() {
  if (MAX_AMOUNT <= MIN_AMOUNT) {
    return MIN_AMOUNT;
  }
  const span = MAX_AMOUNT - MIN_AMOUNT;
  return MIN_AMOUNT + Math.floor(Math.random() * span);
}
