// Load test del POS - stack Docker (localhost:8080)
// Escenarios:
//   smoke: 3 VUs x 5 iter con login real por VU (valida flujo completo)
//   load:  rampa 0->30 VUs, 60s sostenido, token compartido (setup)
//
// Mix realista de cajero: 60% listar productos, 25% procesar venta,
// 15% sucursales. La venta es transaccional (descuenta stock).
//
// Correr: k6 run k6/load-test.js
// Usar la DB con productos de prueba (ids 1..3) y stock disponible.

import http from 'k6/http';
import { check, sleep } from 'k6';

const BASE = 'http://localhost:8080';
const PRODUCT_IDS = [1, 2, 3];
const PRICES = { 1: 1200.0, 2: 500.0, 3: 3500.0 };

export const options = {
  scenarios: {
    smoke: {
      executor: 'per-vu-iterations',
      vus: 3,
      iterations: 5,
      exec: 'smokeTest',
    },
    load: {
      executor: 'ramping-vus',
      startVUs: 0,
      stages: [
        { duration: '20s', target: 30 }, // rampa de calentamiento
        { duration: '60s', target: 30 }, // carga sostenida
        { duration: '10s', target: 0 },  // enfriamiento
      ],
      exec: 'loadTest',
      startTime: '40s', // despues del smoke
    },
  },
  thresholds: {
    http_req_failed: ['rate<0.01'],                      // <1% de errores
    'http_req_duration{scenario:smoke}': ['p(95)<500'],  // lecturas rapidas
    'http_req_duration{scenario:load}': ['p(95)<800'],   // con escrituras
    'http_req_duration{name:POST /api/sales}': ['p(95)<1500'], // venta transaccional
  },
};

// Un solo login al inicio (evita el rate limiter de /api/auth/login con VUs concurrentes)
export function setup() {
  const res = http.post(
    `${BASE}/api/auth/login`,
    JSON.stringify({ email: 'admin@pos.com', password: 'admin123' }),
    { headers: { 'Content-Type': 'application/json' } },
  );
  const token = res.json('accessToken');
  if (!token) {
    throw new Error(`Setup: login fallo (${res.status}): ${res.body}`);
  }
  return { token };
}

function authHeaders(token) {
  return { Authorization: `Bearer ${token}` };
}

export function smokeTest() {
  // Login real por VU: valida el flujo completo de autenticacion
  const login = http.post(
    `${BASE}/api/auth/login`,
    JSON.stringify({ email: 'admin@pos.com', password: 'admin123' }),
    { headers: { 'Content-Type': 'application/json' } },
  );
  check(login, { 'login 200': (r) => r.status === 200 });
  const token = login.json('accessToken');
  if (!token) {
    sleep(1);
    return;
  }
  const h = authHeaders(token);

  const products = http.get(`${BASE}/api/products?page=0&size=10`, { headers: h });
  check(products, { 'products 200': (r) => r.status === 200 });

  const branches = http.get(`${BASE}/api/branches`, { headers: h });
  check(branches, { 'branches 200': (r) => r.status === 200 });

  sleep(0.5);
}

export function loadTest(data) {
  const h = authHeaders(data.token);
  const rnd = Math.random();

  if (rnd < 0.6) {
    // 60%: listar productos (lectura pesada)
    const res = http.get(`${BASE}/api/products?page=0&size=20`, { headers: h });
    check(res, { 'products 200': (r) => r.status === 200 });
  } else if (rnd < 0.85) {
    // 25%: procesar venta (escritura transaccional)
    const productId = PRODUCT_IDS[Math.floor(Math.random() * PRODUCT_IDS.length)];
    const price = PRICES[productId];
    const payload = JSON.stringify({
      clientId: null,
      notes: 'load-test',
      items: [{ productId, quantity: 1, discount: null }],
      payments: [{ paymentMethod: 'CASH', amount: price, reference: null }],
    });
    const res = http.post(`${BASE}/api/sales`, payload, {
      headers: { ...h, 'Content-Type': 'application/json' },
    });
    check(res, {
      'sale 201': (r) => r.status === 201,
      'sale ok': (r) => r.status === 201 || r.status === 400,
    });
  } else {
    // 15%: sucursales (lectura liviana)
    const res = http.get(`${BASE}/api/branches`, { headers: h });
    check(res, { 'branches 200': (r) => r.status === 200 });
  }

  sleep(0.3);
}
