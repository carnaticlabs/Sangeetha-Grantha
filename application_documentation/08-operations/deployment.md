| Metadata | Value |
|:---|:---|
| **Status** | Active |
| **Version** | 1.0.0 |
| **Last Updated** | 2026-01-29 |
| **Author** | Sangeetha Grantha Team |

# Deployment Guide

This document describes deployment procedures for Sangita Grantha across all environments.

---

## 1. Overview

### 1.1 Deployment Environments

| Environment | Purpose | URL | Database |
|-------------|---------|-----|----------|
| **Local** | Development | `localhost:8080/5001` | Docker PostgreSQL |
| **Staging** | Pre-production testing | `staging.sangitagrantha.org` | Cloud SQL |
| **Production** | Live system | `sangitagrantha.org` | Cloud SQL |

### 1.2 Architecture Overview

```
┌─────────────────────────────────────────────────────────────┐
│                    Cloud Load Balancer                       │
│                    (HTTPS termination)                       │
└─────────────────────┬───────────────────────────────────────┘
                      │
         ┌────────────┴────────────┐
         │                         │
         ▼                         ▼
┌─────────────────┐      ┌─────────────────┐
│   Admin Web     │      │   Backend API   │
│  (Cloud Storage │      │  (Cloud Run)    │
│   + CDN)        │      │                 │
└─────────────────┘      └────────┬────────┘
                                  │
                                  ▼
                         ┌─────────────────┐
                         │   Cloud SQL     │
                         │  (PostgreSQL)   │
                         └─────────────────┘
```

---

## 2. Local Development

### 2.1 Prerequisites

- Docker & Docker Compose
- mise (tool version manager)
- Java 25+, Rust 1.92+, Bun 1.3+

### 2.2 Quick Start

```bash
# Clone repository
git clone git@github.com:org/sangeetha-grantha.git
cd sangeetha-grantha

# Install tools
mise install

# Start full stack
mise exec -- cargo run --manifest-path tools/sangita-cli/Cargo.toml -- dev --start-db
```

### 2.3 Manual Setup

```bash
# 1. Start database
docker compose up -d postgres

# 2. Apply migrations
cd tools/sangita-cli
cargo run -- db reset

# 3. Start backend
./gradlew :modules:backend:api:runDev

# 4. Start frontend (new terminal)
cd modules/frontend/sangita-admin-web
bun install
bun run dev
```

### 2.4 Environment Variables

Create `config/local.env` (or set env vars in your environment):

```bash
# Database
DATABASE_URL=postgres://sangita:sangita@localhost:5432/sangita_grantha

# Backend
API_HOST=0.0.0.0
API_PORT=8080
ADMIN_TOKEN=<set-via-env-or-secrets-manager>

# JWT
JWT_SECRET=<set-via-env-or-secrets-manager>
JWT_EXPIRY_HOURS=24

# Frontend
VITE_API_URL=http://localhost:8080
```

---

## 3. Staging Deployment

### 3.1 Prerequisites

- GCP project access
- `gcloud` CLI configured
- Service account with deployment permissions

### 3.2 Database Setup

```bash
# Create Cloud SQL instance (one-time)
gcloud sql instances create sangita-grantha-staging \
  --database-version=POSTGRES_15 \
  --tier=db-f1-micro \
  --region=us-central1 \
  --storage-size=10GB

# Create database
gcloud sql databases create sangita_grantha \
  --instance=sangita-grantha-staging

# Create user
gcloud sql users create sangita \
  --instance=sangita-grantha-staging \
  --password=<secure-password>
```

### 3.3 Backend Deployment

```bash
# Build container
./gradlew :modules:backend:api:jibDockerBuild

# Tag for GCR
docker tag sangita-grantha-api gcr.io/PROJECT_ID/sangita-grantha-api:staging

# Push to GCR
docker push gcr.io/PROJECT_ID/sangita-grantha-api:staging

# Deploy to Cloud Run
gcloud run deploy sangita-grantha-api-staging \
  --image=gcr.io/PROJECT_ID/sangita-grantha-api:staging \
  --region=us-central1 \
  --platform=managed \
  --allow-unauthenticated \
  --set-env-vars="DATABASE_URL=<cloud-sql-connection-string>" \
  --set-secrets="JWT_SECRET=<secret-ref>,ADMIN_TOKEN=<secret-ref>"
```

### 3.4 Frontend Deployment

```bash
# Build for production
cd modules/frontend/sangita-admin-web
VITE_API_URL=https://api-staging.sangitagrantha.org bun run build

# Deploy to Cloud Storage
gsutil -m rsync -r dist/ gs://sangita-grantha-staging-web/

# Set cache headers
gsutil -m setmeta -h "Cache-Control:public, max-age=3600" gs://sangita-grantha-staging-web/**
```

### 3.5 Run Migrations

```bash
# Connect to staging database
gcloud sql connect sangita-grantha-staging --user=sangita --database=sangita_grantha

# Or use Cloud SQL Proxy
./cloud_sql_proxy -instances=PROJECT_ID:us-central1:sangita-grantha-staging=tcp:5433

# Apply migrations
DATABASE_URL=postgres://sangita:<password>@localhost:5433/sangita_grantha \
  cargo run -- db migrate
```

---

## 4. Production Deployment

### 4.1 Pre-deployment Checklist

- [ ] All tests pass on staging
- [ ] Database migrations tested on staging
- [ ] Security review completed
- [ ] Performance testing completed
- [ ] Rollback plan documented
- [ ] Team notified of deployment window

### 4.2 Database Setup

```bash
# Production Cloud SQL (higher tier)
gcloud sql instances create sangita-grantha-prod \
  --database-version=POSTGRES_15 \
  --tier=db-custom-2-8192 \
  --region=us-central1 \
  --storage-size=100GB \
  --storage-auto-increase \
  --backup-start-time=03:00 \
  --enable-point-in-time-recovery
```

### 4.3 Backend Deployment

```bash
# Build and tag
./gradlew :modules:backend:api:jibDockerBuild
docker tag sangita-grantha-api gcr.io/PROJECT_ID/sangita-grantha-api:v1.0.0
docker push gcr.io/PROJECT_ID/sangita-grantha-api:v1.0.0

# Deploy with traffic migration
gcloud run deploy sangita-grantha-api-prod \
  --image=gcr.io/PROJECT_ID/sangita-grantha-api:v1.0.0 \
  --region=us-central1 \
  --platform=managed \
  --no-traffic \
  --set-env-vars="DATABASE_URL=<prod-connection-string>" \
  --set-secrets="JWT_SECRET=<prod-secret-ref>,ADMIN_TOKEN=<prod-secret-ref>"

# Gradual traffic migration
gcloud run services update-traffic sangita-grantha-api-prod \
  --to-revisions=sangita-grantha-api-prod-v1.0.0=10

# Monitor, then increase
gcloud run services update-traffic sangita-grantha-api-prod \
  --to-revisions=sangita-grantha-api-prod-v1.0.0=50

# Full rollout
gcloud run services update-traffic sangita-grantha-api-prod \
  --to-revisions=sangita-grantha-api-prod-v1.0.0=100
```

### 4.4 Frontend Deployment

```bash
# Build production
cd modules/frontend/sangita-admin-web
VITE_API_URL=https://api.sangitagrantha.org bun run build

# Deploy with versioned path
VERSION=$(git rev-parse --short HEAD)
gsutil -m rsync -r dist/ gs://sangita-grantha-prod-web/$VERSION/

# Update CDN to point to new version
gcloud compute url-maps update sangita-grantha-prod-lb \
  --default-backend-bucket=sangita-grantha-prod-web \
  --path-rules="/=$VERSION/index.html,/assets/*=$VERSION/assets/*"
```

---

## 5. Rollback Procedures

### 5.1 Backend Rollback

```bash
# List recent revisions
gcloud run revisions list --service=sangita-grantha-api-prod

# Rollback to previous revision
gcloud run services update-traffic sangita-grantha-api-prod \
  --to-revisions=<previous-revision>=100
```

### 5.2 Frontend Rollback

```bash
# Update URL map to previous version
gcloud compute url-maps update sangita-grantha-prod-lb \
  --path-rules="/=<previous-version>/index.html,/assets/*=<previous-version>/assets/*"
```

### 5.3 Database Rollback

> ⚠️ **Warning**: Database rollbacks can cause data loss. Always take a backup first.

```bash
# Create backup before migration
gcloud sql backups create \
  --instance=sangita-grantha-prod \
  --description="Pre-migration backup"

# If rollback needed, restore from backup
gcloud sql backups restore <backup-id> \
  --restore-instance=sangita-grantha-prod
```

---

## 6. Monitoring & Health Checks

### 6.1 Health Endpoints

| Endpoint | Purpose | Expected |
|----------|---------|----------|
| `/health` | Basic health | 200 OK |
| `/health/ready` | Readiness | 200 OK when ready |
| `/health/live` | Liveness | 200 OK if running |

### 6.2 Cloud Run Monitoring

```bash
# View logs
gcloud logging read "resource.type=cloud_run_revision AND resource.labels.service_name=sangita-grantha-api-prod" --limit=100

# View metrics
gcloud monitoring dashboards list
```

### 6.3 Alerts Configuration

Set up alerts for:
- Error rate > 1%
- Latency p95 > 500ms
- Instance count > threshold
- Database connection errors

---

## 7. CI/CD Pipeline

### 7.1 GitHub Actions Workflow

```yaml
# .github/workflows/deploy.yml
name: Deploy

on:
  push:
    branches: [main]
    tags: ['v*']

jobs:
  test:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-java@v4
        with:
          java-version: '25'
      - run: ./gradlew test

  deploy-staging:
    needs: test
    if: github.ref == 'refs/heads/main'
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - uses: google-github-actions/auth@v2
        with:
          credentials_json: ${{ secrets.GCP_SA_KEY }}
      - run: ./scripts/deploy-staging.sh

  deploy-prod:
    needs: test
    if: startsWith(github.ref, 'refs/tags/v')
    runs-on: ubuntu-latest
    environment: production
    steps:
      - uses: actions/checkout@v4
      - uses: google-github-actions/auth@v2
        with:
          credentials_json: ${{ secrets.GCP_SA_KEY_PROD }}
      - run: ./scripts/deploy-prod.sh
```

### 7.2 Deployment Scripts

Create `scripts/deploy-staging.sh` and `scripts/deploy-prod.sh` with the deployment commands from sections 3 and 4.

---

## 8. Security Considerations

### 8.1 Secrets Management

- Use Google Secret Manager for all secrets
- Never commit secrets to version control
- Rotate secrets regularly

### 8.2 Network Security

- Cloud SQL uses private IP in production
- VPC connector for Cloud Run to Cloud SQL
- HTTPS only (HTTP redirects to HTTPS)

### 8.3 Access Control

- Service accounts with minimal permissions
- IAM roles for deployment access
- Audit logging enabled

---

## 9. Related Documents

- [Configuration](./config.md)
- [Database Runbook](./runbooks/database-runbook.md)
- [Steel Thread Runbook](./runbooks/steel-thread-runbook.md)
- [Monitoring](./monitoring.md) *(planned)*
