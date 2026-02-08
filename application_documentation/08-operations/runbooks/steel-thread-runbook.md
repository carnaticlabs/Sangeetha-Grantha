| Metadata | Value |
|:---|:---|
| **Status** | Active |
| **Version** | 1.1.0 |
| **Last Updated** | 2026-02-08 |
| **Author** | Sangeetha Grantha Team |

# Steel Thread Runbook

This runbook provides step-by-step instructions for running the Sangita Grantha steel thread test, which validates the end-to-end functionality of the system.

---

## Prerequisites

### Required Software

| Software | Version | Verification |
|----------|---------|--------------|
| **Java** | Temurin 25+ | `java --version` |
| **Rust** | 1.92.0+ | `rustc --version` |
| **Bun** | 1.3.0+ | `bun --version` |
| **Docker** | Latest | `docker --version` |
| **Docker Compose** | Latest | `docker compose version` |
| **mise** | Latest | `mise --version` |

### Installation (if needed)

```bash
# Install mise (tool version manager)
curl https://mise.run | sh

# Install all tools via mise
mise install

# Verify installation
mise exec -- java --version
mise exec -- rustc --version
mise exec -- bun --version
```

---

### Configuration

| Service | Host | Port | Notes |
|---------|------|------|-------|
| PostgreSQL | localhost | 5432 | Via Docker Compose |
| Backend API | 0.0.0.0 | 8080 | Ktor server |
| Admin Web | localhost | 5001 | Vite dev server |

See [Configuration Documentation](../config.md) for single-source env var details.

### Environment Setup

1. Copy system-wide defaults from `tools/bootstrap-assets/env/development.env.example` to `config/development.env` (if not present).
2. Create `config/local.env` (gitignored) for local overrides (secrests, DB password).
3. The backend and frontend will automatically load these files.

### Environment Variables

```bash
# Optional overrides (defaults shown)
export API_HOST=0.0.0.0
export API_PORT=8080
export FRONTEND_PORT=5001
export ADMIN_TOKEN=dev-admin-token
export DATABASE_URL=postgres://sangita:sangita@localhost:5432/sangita_grantha
```

---

## Database – Migrations & Seeds

### Start PostgreSQL

```bash
# Start database container
docker compose up -d postgres

# Verify running
docker compose ps
# Expected: postgres container "Up"
```

### Apply Migrations

```bash
# Navigate to CLI tool
cd tools/sangita-cli

# Apply all migrations
cargo run -- db migrate

# Expected output:
# ✓ Migration 01__baseline-schema-and-types.sql applied
# ✓ Migration 02__domain-tables.sql applied
# ✓ Migration 03__constraints-and-indexes.sql applied
# ✓ Migration 04__import-pipeline.sql applied
# ✓ Migration 05__sections-tags-sampradaya-temple-names.sql applied
# ✓ Migration 06__notation-tables.sql applied
```

### Seed Reference Data

```bash
# Seed is included in db reset, or run manually:
./gradlew :modules:backend:api:seedDatabase
```

### Full Database Reset

```bash
# Drop, create, migrate, and seed
cargo run -- db reset

# This performs:
# 1. DROP DATABASE IF EXISTS sangita_grantha
# 2. CREATE DATABASE sangita_grantha
# 3. Apply all migrations
# 4. Seed reference data
```

### Verify Database Setup

```bash
# Check database health
cargo run -- db health

# Expected: "Database connection successful"

# Connect directly (optional)
psql -h localhost -U sangita -d sangita_grantha

# Verify tables exist
\dt
# Expected: composers, ragas, talas, krithis, etc.

# Verify admin user exists
SELECT id, email FROM users WHERE email = 'admin@sangitagrantha.org';
```

---

## Backend – Build & Tests

### Build Backend

```bash
# From repo root
./gradlew :modules:backend:api:build

# Expected: BUILD SUCCESSFUL
```

### Run Backend Tests

```bash
# Unit tests
./gradlew :modules:backend:api:test

# DAL tests
./gradlew :modules:backend:dal:test

# Expected: All tests pass
```

### Start Backend Server

```bash
# Development mode
./gradlew :modules:backend:api:runDev

# Or production mode
./gradlew :modules:backend:api:run

# Expected: Server starts on port 8080
# Log: "Application started on http://0.0.0.0:8080"
```

### Verify Backend Health

```bash
# Health check
curl -s http://localhost:8080/health

# Expected: 200 OK with JSON response
# {"status":"ok","database":"connected"}
```

---

## Frontend (Admin Web) – Build & Run

### Install Dependencies

```bash
cd modules/frontend/sangita-admin-web
bun install
```

### Start Development Server

```bash
bun run dev

# Expected: Server starts on port 5001
# Log: "Local: http://localhost:5001/"
```

### Build for Production

```bash
bun run build

# Expected: BUILD SUCCESSFUL
# Output in: dist/
```

### Verify Frontend

```bash
# Check if accessible
curl -s -o /dev/null -w "%{http_code}" http://localhost:5001

# Expected: 200
```

---

## API Verification (cURL)

### Public Endpoints (No Auth Required)

```bash
# Health check
curl -s http://localhost:8080/health | jq

# Search krithis (public)
curl -s "http://localhost:8080/v1/krithis/search" | jq

# Get composers
curl -s "http://localhost:8080/v1/composers" | jq

# Get ragas
curl -s "http://localhost:8080/v1/ragas" | jq

# Get talas
curl -s "http://localhost:8080/v1/talas" | jq
```

### Admin Authentication

```bash
# Get admin user ID
ADMIN_USER_ID=$(psql -h localhost -U sangita -d sangita_grantha -t -c \
  "SELECT id FROM users WHERE email = 'admin@sangitagrantha.org'" | tr -d ' ')

echo "Admin User ID: $ADMIN_USER_ID"

# Login to get JWT
JWT=$(curl -s -X POST http://localhost:8080/auth/token \
  -H "Content-Type: application/json" \
  -d "{\"adminToken\": \"dev-admin-token\", \"userId\": \"$ADMIN_USER_ID\"}" \
  | jq -r '.token')

echo "JWT: $JWT"
```

### Admin Endpoints (Auth Required)

```bash
# Get audit logs
curl -s http://localhost:8080/v1/audit/logs \
  -H "Authorization: Bearer $JWT" | jq

# List all krithis (admin)
curl -s http://localhost:8080/v1/admin/krithis \
  -H "Authorization: Bearer $JWT" | jq

# Get dashboard stats
curl -s http://localhost:8080/v1/admin/stats \
  -H "Authorization: Bearer $JWT" | jq
```

### Create Test Krithi

```bash
# Create a composer first (if needed)
COMPOSER_ID=$(curl -s -X POST http://localhost:8080/v1/admin/composers \
  -H "Authorization: Bearer $JWT" \
  -H "Content-Type: application/json" \
  -d '{"name": "Test Composer", "birthYear": 1700, "deathYear": 1800}' \
  | jq -r '.id')

# Create a krithi
curl -s -X POST http://localhost:8080/v1/admin/krithis \
  -H "Authorization: Bearer $JWT" \
  -H "Content-Type: application/json" \
  -d "{
    \"title\": \"Test Krithi\",
    \"composerId\": \"$COMPOSER_ID\",
    \"musicalForm\": \"KRITHI\",
    \"primaryLanguage\": \"ta\"
  }" | jq
```

### Check Audit Log

```bash
# Verify action was logged
curl -s http://localhost:8080/v1/audit/logs \
  -H "Authorization: Bearer $JWT" | jq '.[0]'

# Expected: Recent CREATE action logged
```

---

## Running the Steel Thread Test

### Automated Test

```bash
# From repo root
cd tools/sangita-cli

# Run full steel thread
cargo run -- test steel-thread

# Expected phases:
# ✓ Phase 1: Database & Migrations
# ✓ Phase 2: Backend Verification
# ✓ Phase 3: Frontend Launch
# ✓ Phase 4: Manual Verification Ready
```

### Quick Development Stack

```bash
# Start everything (DB + Backend + Frontend)
mise exec -- cargo run --manifest-path tools/sangita-cli/Cargo.toml -- dev --start-db
```

---

## Troubleshooting

### Database Issues

**Problem: Cannot connect to database**
```bash
# Check if PostgreSQL is running
docker compose ps

# Restart if needed
docker compose restart postgres

# Check logs
docker compose logs postgres
```

**Problem: Migrations fail**
```bash
# Check database exists
psql -h localhost -U sangita -l

# Reset database completely
cargo run -- db reset
```

**Problem: Permission denied**
```bash
# Check database user
psql -h localhost -U postgres -c "\\du"

# Grant permissions
psql -h localhost -U postgres -c "GRANT ALL PRIVILEGES ON DATABASE sangita_grantha TO sangita"
```

### Backend Issues

**Problem: Port 8080 already in use**
```bash
# Find process using port
lsof -i :8080

# Kill process
kill -9 <PID>
```

**Problem: Build fails**
```bash
# Clean build
./gradlew clean
./gradlew :modules:backend:api:build --refresh-dependencies
```

**Problem: Tests fail with database error**
```bash
# Ensure test database is set up
# Check config/application-test.conf exists
# Verify TEST_DATABASE_URL environment variable
```

### Frontend Issues

**Problem: Port 5001 already in use**
```bash
# Find process using port
lsof -i :5001

# Kill process
kill -9 <PID>
```

**Problem: Module not found**
```bash
# Clean install
rm -rf node_modules bun.lockb
bun install
```

**Problem: API calls fail**
```bash
# Check backend is running
curl http://localhost:8080/health

# Check CORS configuration
# Backend should allow localhost:5001
```

### Authentication Issues

**Problem: 401 Unauthorized**
```bash
# Verify admin user exists
psql -h localhost -U sangita -d sangita_grantha -c \
  "SELECT id, email FROM users WHERE email = 'admin@sangitagrantha.org'"

# Verify admin token matches configuration
grep ADMIN_TOKEN config/local.env || grep ADMIN_TOKEN config/development.env
```

**Problem: JWT expired**
```bash
# Get new token
# Re-run authentication steps above
```

---

## Common Commands

### Check Services Status

```bash
# Database
docker compose ps

# Backend (if running in background)
curl -s http://localhost:8080/health

# Frontend
curl -s -o /dev/null -w "%{http_code}" http://localhost:5001
```

### View Logs

```bash
# Database logs
docker compose logs -f postgres

# Backend logs (if using Gradle)
# Logs appear in console

# Frontend logs
# Logs appear in console
```

### Reset Everything

```bash
# Stop all services
docker compose down

# Reset database
docker compose up -d postgres
sleep 5
cargo run -- db reset

# Rebuild backend
./gradlew clean build

# Reinstall frontend deps
cd modules/frontend/sangita-admin-web
rm -rf node_modules
bun install
```

---

## References

- [Steel Thread Implementation](../../06-backend/steel-thread-implementation.md)
- [Database Migrations](../../04-database/migrations.md)
- [API Contract](../../03-api/api-contract.md)
- [Getting Started](../../00-onboarding/getting-started.md)
- [Sangita CLI README](../../../tools/sangita-cli/README.md)