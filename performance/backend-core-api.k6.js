import http from 'k6/http';
import { check, sleep } from 'k6';
import { Rate, Trend } from 'k6/metrics';

const BASE_URL = (__ENV.PERF_BASE_URL || 'http://100.89.171.113:8080').replace(/\/+$/, '');
const LOGIN_ID = __ENV.PERF_LOGIN_ID || 'admin123';
const PASSWORD = __ENV.PERF_PASSWORD;
const MODE = __ENV.PERF_MODE || 'latency';

if (!PASSWORD) {
  throw new Error('PERF_PASSWORD is required');
}

const endpoints = [
  { name: 'users_me', method: 'GET', path: '/api/users/me' },
  { name: 'shop_character_items', method: 'GET', path: '/api/shop/items?type=character' },
  { name: 'world_rankings', method: 'GET', path: '/api/worlds/rankings?limit=5' },
  { name: 'story_chats', method: 'GET', path: '/api/stories/chats?limit=20' },
  { name: 'routine_today', method: 'GET', path: '/api/routines/today' },
  { name: 'battle_summary', method: 'GET', path: '/api/battles/me/summary' },
  { name: 'record_stats_weekly', method: 'GET', path: '/api/records/stats?period=WEEKLY' },
];

const durationByEndpoint = {};
const successByEndpoint = {};
for (const endpoint of endpoints) {
  durationByEndpoint[endpoint.name] = new Trend(`api_duration_${endpoint.name}`, true);
  successByEndpoint[endpoint.name] = new Rate(`api_success_${endpoint.name}`);
}

const latencyOptions = {
  scenarios: {
    latency: {
      executor: 'shared-iterations',
      vus: Number(__ENV.PERF_VUS || 1),
      iterations: Number(__ENV.PERF_ITERATIONS || 30),
      maxDuration: __ENV.PERF_MAX_DURATION || '5m',
    },
  },
  thresholds: {
    http_req_failed: ['rate<0.01'],
    http_req_duration: ['p(95)<500'],
  },
};

const loadOptions = {
  scenarios: {
    load: {
      executor: 'constant-vus',
      vus: Number(__ENV.PERF_VUS || 30),
      duration: __ENV.PERF_DURATION || '60s',
    },
  },
  thresholds: {
    http_req_failed: ['rate<0.01'],
    http_req_duration: ['p(95)<800'],
  },
};

export const options = MODE === 'load' ? loadOptions : latencyOptions;

export function setup() {
  const response = http.post(
    `${BASE_URL}/api/auth/login`,
    JSON.stringify({ loginId: LOGIN_ID, password: PASSWORD }),
    { headers: { 'Content-Type': 'application/json' }, tags: { endpoint: 'auth_login' } },
  );

  check(response, {
    'login succeeded': (res) => res.status === 200 && Boolean(res.json('data.accessToken')),
  });

  return {
    token: response.json('data.accessToken'),
  };
}

export default function (data) {
  const headers = {
    Authorization: `Bearer ${data.token}`,
  };

  for (const endpoint of endpoints) {
    const response = http.request(endpoint.method, `${BASE_URL}${endpoint.path}`, null, {
      headers,
      tags: { endpoint: endpoint.name },
    });
    const ok = check(response, {
      [`${endpoint.name} returned 2xx`]: (res) => res.status >= 200 && res.status < 300,
    });
    durationByEndpoint[endpoint.name].add(response.timings.duration);
    successByEndpoint[endpoint.name].add(ok);
  }

  sleep(Number(__ENV.PERF_SLEEP_SECONDS || 0.2));
}
