| Metadata | Value |
|:---|:---|
| **Status** | Active |
| **Version** | 1.1.0 |
| **Last Updated** | 2026-02-08 |
| **Author** | Sangeetha Grantha Team |

# Monitoring & Observability

This document describes the monitoring strategy, health checks, metrics, and alerting for Sangita Grantha.

---

## 1. Overview

### 1.1 Monitoring Stack

| Component | Tool | Purpose |
|-----------|------|---------|
| **Metrics** | Cloud Monitoring (GCP) | Performance metrics |
| **Logs** | Cloud Logging (GCP) | Centralized logging |
| **Traces** | Cloud Trace (GCP) | Distributed tracing |
| **Alerts** | Cloud Alerting | Incident notification |
| **Uptime** | Cloud Monitoring | Availability checks |

### 1.2 Monitoring Philosophy

1. **Observable by Default**: All components expose health and metrics
2. **Alert on Symptoms**: Alert on user-facing impact, not every error
3. **Actionable Alerts**: Every alert should have a clear response
4. **Defense in Depth**: Multiple layers of monitoring

---

## 2. Health Checks

### 2.1 Backend Health Endpoints

| Endpoint | Purpose | Expected Response |
|----------|---------|-------------------|
| `/health` | Basic health | `{"status":"ok"}` |
| `/health/ready` | Readiness probe | 200 when ready to serve |
| `/health/live` | Liveness probe | 200 when process alive |

**Implementation:**

```kotlin
// Basic health check
get("/health") {
    call.respond(mapOf("status" to "ok", "database" to "connected"))
}

// Readiness (checks dependencies)
get("/health/ready") {
    val dbHealthy = checkDatabaseConnection()
    if (dbHealthy) {
        call.respond(HttpStatusCode.OK, mapOf("status" to "ready"))
    } else {
        call.respond(HttpStatusCode.ServiceUnavailable, mapOf("status" to "not_ready"))
    }
}

// Liveness (process alive)
get("/health/live") {
    call.respond(HttpStatusCode.OK, mapOf("status" to "alive"))
}
```

### 2.2 Database Health

```sql
-- Connection test
SELECT 1;

-- Table accessibility
SELECT COUNT(*) FROM krithis LIMIT 1;

-- Connection pool status
SELECT count(*) FROM pg_stat_activity WHERE datname = 'sangita_grantha';
```

### 2.3 Frontend Health

```bash
# Check if serving
curl -s -o /dev/null -w "%{http_code}" https://admin.sangitagrantha.org/

# Check static assets
curl -s -o /dev/null -w "%{http_code}" https://admin.sangitagrantha.org/assets/index.js
```

---

## 3. Key Metrics

### 3.1 Application Metrics

| Metric | Type | Description | Alert Threshold |
|--------|------|-------------|-----------------|
| `http_requests_total` | Counter | Total HTTP requests | N/A |
| `http_request_duration_seconds` | Histogram | Request latency | p95 > 500ms |
| `http_requests_error_total` | Counter | 4xx/5xx responses | Error rate > 1% |
| `active_connections` | Gauge | Current connections | > 80% max |
| `database_query_duration` | Histogram | DB query latency | p95 > 200ms |

### 3.2 Business Metrics

| Metric | Description | Purpose |
|--------|-------------|---------|
| `krithis_total` | Total published krithis | Content growth |
| `searches_total` | Search queries | Usage tracking |
| `imports_pending` | Pending imports | Queue health |
| `imports_processed_total` | Processed imports | Throughput |
| `active_users` | Logged-in admins | Usage patterns |

### 3.3 Infrastructure Metrics

| Metric | Warning | Critical |
|--------|---------|----------|
| CPU Usage | > 70% | > 90% |
| Memory Usage | > 80% | > 95% |
| Disk Usage | > 75% | > 90% |
| Network I/O | > 80% capacity | > 95% capacity |
| Container restarts | > 1/hour | > 5/hour |

---

## 4. Logging Strategy

### 4.1 Log Levels

| Level | Usage | Example |
|-------|-------|---------|
| `ERROR` | Failures requiring attention | Database connection failed |
| `WARN` | Potential issues | Slow query detected |
| `INFO` | Normal operations | Request completed |
| `DEBUG` | Development details | Query parameters |

### 4.2 Structured Logging Format

```json
{
  "timestamp": "2026-01-29T10:30:00.000Z",
  "level": "INFO",
  "logger": "KrithiService",
  "message": "Krithi created",
  "context": {
    "krithiId": "uuid",
    "userId": "uuid",
    "action": "CREATE",
    "duration_ms": 45
  },
  "trace_id": "abc123",
  "span_id": "def456"
}
```

### 4.3 Log Retention

| Environment | Retention | Storage |
|-------------|-----------|---------|
| Development | 7 days | Local |
| Staging | 30 days | Cloud Logging |
| Production | 90 days | Cloud Logging |
| Audit logs | 7 years | Cloud Storage (archived) |

### 4.4 Sensitive Data

**Never log:**
- Passwords or tokens
- Full credit card numbers
- Personal identifiable information (PII)
- JWT contents

**Mask or redact:**
- Email addresses (show first 3 chars)
- User IDs (use hashed/truncated)

---

## 5. Alerting Strategy

### 5.1 Alert Severity Levels

| Severity | Response Time | Examples |
|----------|---------------|----------|
| **P1 Critical** | 15 minutes | Site down, data loss risk |
| **P2 High** | 1 hour | Major feature broken |
| **P3 Medium** | 4 hours | Performance degraded |
| **P4 Low** | Next business day | Minor issues |

### 5.2 Alert Configuration

**Critical Alerts (P1):**

```yaml
# Site availability
- alert: SiteDown
  expr: probe_success{job="uptime"} == 0
  for: 2m
  labels:
    severity: critical
  annotations:
    summary: "Site is down"
    runbook: "docs/runbooks/site-down.md"

# Error rate spike
- alert: HighErrorRate
  expr: rate(http_requests_error_total[5m]) / rate(http_requests_total[5m]) > 0.05
  for: 5m
  labels:
    severity: critical
```

**High Alerts (P2):**

```yaml
# Slow responses
- alert: HighLatency
  expr: histogram_quantile(0.95, http_request_duration_seconds) > 1
  for: 10m
  labels:
    severity: high

# Database connection issues
- alert: DatabaseConnectionFailures
  expr: database_connection_errors_total > 0
  for: 5m
  labels:
    severity: high
```

**Medium Alerts (P3):**

```yaml
# High memory usage
- alert: HighMemoryUsage
  expr: container_memory_usage_bytes / container_memory_limit_bytes > 0.85
  for: 15m
  labels:
    severity: medium

# Import queue growing
- alert: ImportQueueBacklog
  expr: imports_pending > 100
  for: 30m
  labels:
    severity: medium
```

### 5.3 Notification Channels

| Severity | Channels |
|----------|----------|
| P1 Critical | PagerDuty, Slack #incidents, Email |
| P2 High | Slack #incidents, Email |
| P3 Medium | Slack #alerts |
| P4 Low | Slack #alerts (business hours) |

---

## 6. Dashboards

### 6.1 Overview Dashboard

**Panels:**
1. Request rate (requests/second)
2. Error rate (percentage)
3. Latency (p50, p95, p99)
4. Active users
5. Database connections
6. Container health

### 6.2 API Dashboard

**Panels:**
1. Requests by endpoint
2. Latency by endpoint
3. Error rate by endpoint
4. Top slow endpoints
5. Request size distribution
6. Response codes distribution

### 6.3 Database Dashboard

**Panels:**
1. Query latency
2. Connections (active/idle)
3. Transactions per second
4. Cache hit ratio
5. Disk I/O
6. Table sizes

### 6.4 Import Pipeline Dashboard

**Panels:**
1. Imports pending
2. Processing rate
3. Success/failure rate
4. Processing time
5. Queue age (oldest pending)
6. By source breakdown

---

## 7. Uptime Monitoring

### 7.1 Synthetic Checks

| Check | Frequency | Timeout | Locations |
|-------|-----------|---------|-----------|
| Homepage | 1 min | 10s | 3 regions |
| API Health | 1 min | 5s | 3 regions |
| Search API | 5 min | 10s | 2 regions |
| Login Flow | 5 min | 30s | 1 region |

### 7.2 Uptime SLO

| Service | Target | Measurement |
|---------|--------|-------------|
| API | 99.9% | Monthly rolling |
| Admin Web | 99.5% | Monthly rolling |
| Database | 99.95% | Monthly rolling |

### 7.3 Incident Response

When an alert fires:

1. **Acknowledge** the alert within SLA
2. **Assess** impact and severity
3. **Communicate** status to stakeholders
4. **Mitigate** immediate impact
5. **Resolve** root cause
6. **Document** in post-mortem

---

## 8. Local Development Monitoring

### 8.1 Development Health Checks

```bash
# Quick health check script
#!/bin/bash
echo "=== Sangita Grantha Health Check ==="

# Database
echo -n "Database: "
docker compose ps postgres --format "{{.Status}}" 2>/dev/null || echo "NOT RUNNING"

# Backend
echo -n "Backend: "
curl -s http://localhost:8080/health | jq -r '.status' 2>/dev/null || echo "NOT RUNNING"

# Frontend
echo -n "Frontend: "
curl -s -o /dev/null -w "%{http_code}" http://localhost:5001 2>/dev/null || echo "NOT RUNNING"
```

### 8.2 Development Logging

```bash
# View all logs
docker compose logs -f

# View specific service
docker compose logs -f postgres

# Backend logs (when running via Gradle)
# Logs appear in terminal

# Frontend logs
# Logs appear in terminal running bun dev
```

---

## 9. Runbook Links

| Alert | Runbook |
|-------|---------|
| Site Down | [steel-thread-runbook.md](./runbooks/steel-thread-runbook.md) |
| Database Issues | [database-runbook.md](./runbooks/database-runbook.md) |
| High Error Rate | [incident-response.md](./runbooks/incident-response.md) |
| Deployment Issues | [deployment.md](./deployment.md) |

---

## 10. Related Documents

- [Deployment Guide](./deployment.md)
- [Database Runbook](./runbooks/database-runbook.md)
- [Steel Thread Runbook](./runbooks/steel-thread-runbook.md)
- [Troubleshooting Guide](../00-onboarding/troubleshooting.md)