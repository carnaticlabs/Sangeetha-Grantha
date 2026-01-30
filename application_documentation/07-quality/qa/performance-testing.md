| Metadata | Value |
|:---|:---|
| **Status** | Active |
| **Version** | 1.0.0 |
| **Last Updated** | 2026-01-29 |
| **Author** | Sangeetha Grantha Team |

# Performance Testing Guide

This document describes the performance testing strategy, tools, and procedures for Sangita Grantha.

---

## 1. Overview

### 1.1 Performance Goals

| Metric | Target | Critical Threshold |
|--------|--------|-------------------|
| API Response Time (p50) | < 100ms | < 200ms |
| API Response Time (p95) | < 300ms | < 500ms |
| API Response Time (p99) | < 500ms | < 1000ms |
| Search Response Time | < 300ms | < 500ms |
| Error Rate | < 0.1% | < 1% |
| Throughput | > 100 req/s | > 50 req/s |
| Concurrent Users | > 500 | > 100 |

### 1.2 Test Types

| Type | Purpose | Frequency |
|------|---------|-----------|
| **Load Testing** | Normal traffic patterns | Weekly |
| **Stress Testing** | Find breaking point | Monthly |
| **Spike Testing** | Handle traffic spikes | Quarterly |
| **Soak Testing** | Long-duration stability | Quarterly |
| **Baseline Testing** | Establish benchmarks | After releases |

---

## 2. Test Environment

### 2.1 Environment Setup

**Dedicated Performance Environment:**
- Separate from staging/production
- Mirror production configuration
- Isolated network for accurate results

```bash
# Performance test environment
export PERF_API_URL="https://perf.sangitagrantha.org"
export PERF_DB_URL="postgres://user:pass@perf-db:5432/sangita_grantha"
```

### 2.2 Data Setup

```bash
# Seed performance test data
./gradlew :modules:backend:api:seedPerformanceData

# Data volumes:
# - 10,000 krithis
# - 500 composers
# - 200 ragas
# - 50 talas
# - 50,000 lyric variants
# - 100,000 audit log entries
```

---

## 3. Tools

### 3.1 k6 (Primary Tool)

**Installation:**
```bash
# macOS
brew install k6

# Linux
sudo gpg -k
sudo gpg --no-default-keyring --keyring /usr/share/keyrings/k6-archive-keyring.gpg \
  --keyserver hkp://keyserver.ubuntu.com:80 --recv-keys C5AD17C747E3415A3642D57D77C6C491D6AC1D69
echo "deb [signed-by=/usr/share/keyrings/k6-archive-keyring.gpg] https://dl.k6.io/deb stable main" \
  | sudo tee /etc/apt/sources.list.d/k6.list
sudo apt-get update && sudo apt-get install k6
```

### 3.2 Grafana k6 Cloud (Optional)

For distributed testing and advanced reporting:
```bash
# Login to k6 Cloud
k6 login cloud --token <api-token>

# Run test on cloud
k6 cloud script.js
```

---

## 4. Test Scripts

### 4.1 Basic Load Test

Create `perf/load-test.js`:

```javascript
import http from 'k6/http';
import { check, sleep } from 'k6';
import { Rate, Trend } from 'k6/metrics';

// Custom metrics
const errorRate = new Rate('errors');
const searchLatency = new Trend('search_latency');

// Test configuration
export const options = {
  stages: [
    { duration: '2m', target: 50 },   // Ramp up to 50 users
    { duration: '5m', target: 50 },   // Stay at 50 users
    { duration: '2m', target: 100 },  // Ramp up to 100 users
    { duration: '5m', target: 100 },  // Stay at 100 users
    { duration: '2m', target: 0 },    // Ramp down
  ],
  thresholds: {
    http_req_duration: ['p(95)<500'],  // 95% of requests < 500ms
    http_req_failed: ['rate<0.01'],    // Error rate < 1%
    errors: ['rate<0.01'],
  },
};

const BASE_URL = __ENV.API_URL || 'http://localhost:8080';

export default function () {
  // Health check
  const healthRes = http.get(`${BASE_URL}/health`);
  check(healthRes, {
    'health status is 200': (r) => r.status === 200,
  });

  // Search krithis
  const searchStart = Date.now();
  const searchRes = http.get(`${BASE_URL}/v1/krithis/search?q=vatapi`);
  searchLatency.add(Date.now() - searchStart);

  const searchOk = check(searchRes, {
    'search status is 200': (r) => r.status === 200,
    'search returns items': (r) => JSON.parse(r.body).items !== undefined,
  });
  errorRate.add(!searchOk);

  // Get krithi detail
  const searchData = JSON.parse(searchRes.body);
  if (searchData.items && searchData.items.length > 0) {
    const krithiId = searchData.items[0].id;
    const detailRes = http.get(`${BASE_URL}/v1/krithis/${krithiId}`);
    check(detailRes, {
      'detail status is 200': (r) => r.status === 200,
    });
  }

  // Get reference data
  http.get(`${BASE_URL}/v1/composers`);
  http.get(`${BASE_URL}/v1/ragas`);

  sleep(1);
}
```

### 4.2 Stress Test

Create `perf/stress-test.js`:

```javascript
import http from 'k6/http';
import { check, sleep } from 'k6';

export const options = {
  stages: [
    { duration: '2m', target: 100 },
    { duration: '5m', target: 100 },
    { duration: '2m', target: 200 },
    { duration: '5m', target: 200 },
    { duration: '2m', target: 300 },
    { duration: '5m', target: 300 },
    { duration: '2m', target: 400 },
    { duration: '5m', target: 400 },
    { duration: '10m', target: 0 },
  ],
  thresholds: {
    http_req_failed: ['rate<0.05'],  // Allow 5% errors under stress
  },
};

const BASE_URL = __ENV.API_URL || 'http://localhost:8080';

export default function () {
  const res = http.get(`${BASE_URL}/v1/krithis/search`);
  check(res, {
    'status is 200': (r) => r.status === 200,
  });
  sleep(0.5);
}
```

### 4.3 Admin API Test

Create `perf/admin-test.js`:

```javascript
import http from 'k6/http';
import { check, sleep } from 'k6';

export const options = {
  vus: 10,
  duration: '5m',
  thresholds: {
    http_req_duration: ['p(95)<1000'],
    http_req_failed: ['rate<0.01'],
  },
};

const BASE_URL = __ENV.API_URL || 'http://localhost:8080';
const ADMIN_TOKEN = __ENV.ADMIN_TOKEN || 'dev-admin-token';
const USER_ID = __ENV.USER_ID;

// Get JWT at setup
export function setup() {
  const loginRes = http.post(
    `${BASE_URL}/auth/token`,
    JSON.stringify({ adminToken: ADMIN_TOKEN, userId: USER_ID }),
    { headers: { 'Content-Type': 'application/json' } }
  );
  return { jwt: JSON.parse(loginRes.body).token };
}

export default function (data) {
  const headers = {
    'Authorization': `Bearer ${data.jwt}`,
    'Content-Type': 'application/json',
  };

  // Admin krithi list
  const listRes = http.get(`${BASE_URL}/v1/admin/krithis`, { headers });
  check(listRes, {
    'admin list status is 200': (r) => r.status === 200,
  });

  // Admin krithi detail
  const listData = JSON.parse(listRes.body);
  if (listData.items && listData.items.length > 0) {
    const krithiId = listData.items[0].id;
    const detailRes = http.get(`${BASE_URL}/v1/admin/krithis/${krithiId}`, { headers });
    check(detailRes, {
      'admin detail status is 200': (r) => r.status === 200,
    });
  }

  // Audit logs
  const auditRes = http.get(`${BASE_URL}/v1/audit/logs`, { headers });
  check(auditRes, {
    'audit status is 200': (r) => r.status === 200,
  });

  sleep(1);
}
```

### 4.4 Search Performance Test

Create `perf/search-test.js`:

```javascript
import http from 'k6/http';
import { check, sleep } from 'k6';
import { Trend } from 'k6/metrics';

const searchLatency = new Trend('search_latency');
const filterLatency = new Trend('filter_latency');

export const options = {
  vus: 50,
  duration: '10m',
  thresholds: {
    search_latency: ['p(95)<300'],
    filter_latency: ['p(95)<500'],
  },
};

const BASE_URL = __ENV.API_URL || 'http://localhost:8080';

const searchTerms = [
  'vatapi', 'endaro', 'thyagaraja', 'dikshitar',
  'shanmukhapriya', 'kalyani', 'mohanam', 'bhairavi',
];

const composerIds = [/* Array of composer UUIDs from test data */];
const ragaIds = [/* Array of raga UUIDs from test data */];

export default function () {
  // Random text search
  const term = searchTerms[Math.floor(Math.random() * searchTerms.length)];
  const searchStart = Date.now();
  const searchRes = http.get(`${BASE_URL}/v1/krithis/search?q=${term}`);
  searchLatency.add(Date.now() - searchStart);
  check(searchRes, { 'search ok': (r) => r.status === 200 });

  // Filtered search
  const filterStart = Date.now();
  const filterRes = http.get(`${BASE_URL}/v1/krithis/search?composerId=${composerIds[0]}&ragaId=${ragaIds[0]}`);
  filterLatency.add(Date.now() - filterStart);
  check(filterRes, { 'filter ok': (r) => r.status === 200 });

  sleep(0.5);
}
```

---

## 5. Running Tests

### 5.1 Local Execution

```bash
# Basic load test
k6 run perf/load-test.js

# With environment variables
API_URL=http://localhost:8080 k6 run perf/load-test.js

# With custom VUs and duration
k6 run --vus 100 --duration 5m perf/load-test.js

# Output to JSON for analysis
k6 run --out json=results.json perf/load-test.js

# Output to InfluxDB (for Grafana dashboards)
k6 run --out influxdb=http://localhost:8086/k6 perf/load-test.js
```

### 5.2 CI Execution

```yaml
# .github/workflows/performance.yml
name: Performance Tests

on:
  schedule:
    - cron: '0 2 * * 0'  # Weekly at 2 AM Sunday
  workflow_dispatch:

jobs:
  performance:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4

      - name: Install k6
        run: |
          sudo gpg -k
          sudo gpg --no-default-keyring --keyring /usr/share/keyrings/k6-archive-keyring.gpg \
            --keyserver hkp://keyserver.ubuntu.com:80 --recv-keys C5AD17C747E3415A3642D57D77C6C491D6AC1D69
          echo "deb [signed-by=/usr/share/keyrings/k6-archive-keyring.gpg] https://dl.k6.io/deb stable main" \
            | sudo tee /etc/apt/sources.list.d/k6.list
          sudo apt-get update && sudo apt-get install k6

      - name: Run load test
        run: |
          k6 run --out json=results.json perf/load-test.js
        env:
          API_URL: ${{ secrets.PERF_API_URL }}

      - name: Upload results
        uses: actions/upload-artifact@v4
        with:
          name: k6-results
          path: results.json

      - name: Check thresholds
        run: |
          # Parse results and fail if thresholds exceeded
          cat results.json | jq '.metrics.http_req_duration.values.p95'
```

---

## 6. Analysis & Reporting

### 6.1 Key Metrics to Monitor

| Metric | Description | Good | Warning | Critical |
|--------|-------------|------|---------|----------|
| `http_req_duration` | Response time | p95 < 300ms | p95 < 500ms | p95 > 500ms |
| `http_req_failed` | Error rate | < 0.1% | < 1% | > 1% |
| `http_reqs` | Throughput | > 100/s | > 50/s | < 50/s |
| `vus` | Concurrent users | As configured | - | - |
| `data_received` | Data transfer | - | - | - |

### 6.2 Results Analysis

```bash
# Parse k6 JSON output
cat results.json | jq '{
  duration_p50: .metrics.http_req_duration.values.med,
  duration_p95: .metrics.http_req_duration.values["p(95)"],
  duration_p99: .metrics.http_req_duration.values["p(99)"],
  error_rate: .metrics.http_req_failed.values.rate,
  throughput: .metrics.http_reqs.values.rate
}'
```

### 6.3 Grafana Dashboard

Create dashboard with panels for:
- Request rate over time
- Response time percentiles
- Error rate
- Active virtual users
- Throughput (requests/second)

---

## 7. Performance Optimization

### 7.1 Common Bottlenecks

| Symptom | Likely Cause | Solution |
|---------|--------------|----------|
| High p95, low p50 | Occasional slow queries | Add database indexes |
| Linear degradation | Resource saturation | Scale horizontally |
| Sudden spike | Connection pool exhaustion | Increase pool size |
| Memory growth | Memory leak | Profile and fix |
| CPU saturation | Inefficient code | Profile and optimize |

### 7.2 Database Optimization

```sql
-- Check slow queries
SELECT query, calls, mean_time, total_time
FROM pg_stat_statements
ORDER BY mean_time DESC
LIMIT 10;

-- Check missing indexes
SELECT schemaname, tablename, indexname, idx_scan
FROM pg_stat_user_indexes
WHERE idx_scan = 0;

-- Analyze query plans
EXPLAIN ANALYZE SELECT * FROM krithis WHERE title ILIKE '%vatapi%';
```

### 7.3 API Optimization

- Enable response compression
- Implement caching (Redis)
- Use connection pooling
- Optimize JSON serialization
- Add pagination limits

---

## 8. Baseline Management

### 8.1 Recording Baselines

After each release, record baseline metrics:

```bash
# Run baseline test
k6 run --out json=baseline-v1.2.0.json perf/load-test.js

# Store in version control
git add perf/baselines/baseline-v1.2.0.json
git commit -m "Add performance baseline for v1.2.0"
```

### 8.2 Comparing Results

```javascript
// compare-baselines.js
const baseline = require('./baselines/baseline-v1.2.0.json');
const current = require('./results.json');

const metrics = ['http_req_duration', 'http_req_failed'];
metrics.forEach(metric => {
  const baseP95 = baseline.metrics[metric].values['p(95)'];
  const currP95 = current.metrics[metric].values['p(95)'];
  const diff = ((currP95 - baseP95) / baseP95 * 100).toFixed(2);
  console.log(`${metric} p95: ${diff}% change`);
});
```

---

## 9. Schedule

| Test Type | Frequency | Trigger |
|-----------|-----------|---------|
| Baseline | After each release | Manual/CI |
| Load | Weekly | Scheduled CI |
| Stress | Monthly | Scheduled CI |
| Soak | Quarterly | Manual |

---

## 10. Related Documents

- [Test Plan](./test-plan.md)
- [E2E Testing](./e2e-testing.md)
- [Monitoring](../../08-operations/monitoring.md)
- [Troubleshooting](../../00-onboarding/troubleshooting.md)
