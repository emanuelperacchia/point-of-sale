// Stress test: busca el techo del backend con 100 VUs
// Correr: k6 run k6/stress-test.js
// Esperamos degradacion controlada (p95 sube), NO errores masivos.

import http from 'k6/http';
import { check, sleep } from 'k6';

const BASE = 'http://localhost:8080';
const PRODUCT_IDS = [1, 2, 3];
const PRICES = { 1: 1200.0, 2: 500.0, 3: 3500.0 };

export const options = {
  scenarios: {
    stress: {
      executor: 'ramping-vus',
      startVUs: 0,
      stages: [
        { duration: '30s', target: 100 },
        { duration: '60s', target: 100 },
        { duration: '15s', target: 0 },
      ],
    },
  },
  thresholds: {
    http_req_failed: ['rate<0.05'],                // <5% bajo estres
    http_req_duration: ['p(95)<1000'],             // degradacion controlada
  },
};

export function setup() {
  const res = http.post(
    `${BASE}/api/auth/login`,
    JSON.stringify({ email: 'admin@pos.com', password: 'admin123' }),
    { headers: { 'Content-Type': 'application/json' } },
  );
  const token = res.json('accessToken');
  if (!token) {
    throw new Error(`Setup: login fallo (${res.status})`);
  }
  return { token };
}

export default function loadTest(data) {
  const h = { Authorization: `Bearer ${data.token}` };
  const rnd = Math.random();

  if (rnd < 0.6) {
    const res = http.get(`${BASE}/api/products?page=0&size=20`, { headers: h });
    check(res, { 'products 200': (r) => r.status === 200 });
  } else if (rnd < 0.85) {
    const productId = PRODUCT_IDS[Math.floor(Math.random() * PRODUCT_IDS.length)];
    const payload = JSON.stringify({
      clientId: null,
      notes: 'stress-test',
      items: [{ productId, quantity: 1, discount: null }],
      payments: [{ paymentMethod: 'CASH', amount: PRICES[productId], reference: null }],
    });
    const res = http.post(`${BASE}/api/sales`, payload, {
      headers: { ...h, 'Content-Type': 'application/json' },
    });
    check(res, { 'sale 201': (r) => r.status === 201 });
  } else {
    const res = http.get(`${BASE}/api/branches`, { headers: h });
    check(res, { 'branches 200': (r) => r.status === 200 });
  }

  sleep(0.3);
}