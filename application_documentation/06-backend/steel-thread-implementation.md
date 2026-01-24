| Metadata | Value |
|:---|:---|
| **Status** | Active |
| **Version** | 1.0.0 |
| **Last Updated** | 2026-01-24 |
| **Author** | Sangeetha Grantha Team |

# Steel Thread Implementation

| Metadata | Value |
|:---|:---|
| **Status** | Current |
| **Version** | 1.0 |
| **Last Updated** | 2026-01-20 |
| **Author** | Engineering Team |
| **Related Documents** | - [Steel Thread Runbook](../08-operations/runbooks/steel-thread-runbook.md)<br>- [Steel Thread Report](../07-quality/reports/steel-thread.md)<br>- [Sangita CLI README](../../tools/sangita-cli/README.md) |

## Overview

The steel thread is an end-to-end smoke test that verifies the core functionality of Sangita Grantha. It validates database connectivity, migrations, backend health, API endpoints, and frontend startup.

## Scope & Success Criteria

The steel thread test verifies:

1. ✅ Database connectivity and migrations
2. ✅ Backend health endpoint
3. ✅ Public API endpoints (Krithi search)
4. ✅ Admin API endpoints (with authentication)
5. ✅ Frontend dev server startup

All checks must pass for the steel thread to be considered successful.

## Environment & Tooling

The steel thread is executed via the Sangita CLI tool:

```bash
cd tools/sangita-cli
cargo run -- test steel-thread
```

**Prerequisites:**
- Rust (stable)
- PostgreSQL 15 (dev pinned via Docker Compose) / 15+ (prod)
- JDK 25+
- Bun 1.3+

## Test Execution

### Automated Phases

The steel thread runs in four phases:

#### Phase 1: Database & Migrations
- Ensures PostgreSQL is running
- Applies all database migrations
- Verifies database connectivity

#### Phase 2: Backend Verification
- Starts the Ktor backend server
- Waits for `/health` endpoint to respond
- Tests public search endpoint: `GET /v1/krithis/search`
- Tests admin endpoint: `GET /v1/audit/logs` (with bearer token)
- Verifies backend process remains alive

#### Phase 3: Frontend Launch
- Starts the React admin web frontend
- Verifies frontend dev server is accessible

#### Phase 4: Manual Verification
- Services remain running for manual QA
- Provides URLs for manual testing:
  - Admin Web: `http://localhost:5001`
  - API: `http://0.0.0.0:8080`
  - Admin Token: `ADMIN_TOKEN` (from config)

### Configuration

The test uses configuration from:
- `config/application.local.toml` (database, backend settings)
- Environment variables (optional overrides):
  - `API_HOST` (default: `0.0.0.0`)
  - `API_PORT` (default: `8080`)
  - `FRONTEND_PORT` (default: `5001`)
  - `ADMIN_TOKEN` (default: `dev-admin-token`)

## Implementation Details

### Database Setup

The test uses the same database management commands as development:

```bash
# Database is managed via Sangita CLI
cargo run -- db migrate    # Apply migrations
cargo run -- db health     # Verify connectivity
```

### Backend Health Check

The backend health endpoint (`/health`) must return `200 OK` before proceeding to API tests.

### API Verification

**Public Endpoint:**
```bash
GET /v1/krithis/search
# Expected: 200 OK (even with empty results)
```

**Admin Endpoint:**
```bash
GET /v1/audit/logs
Authorization: Bearer <ADMIN_TOKEN>
# Expected: 200 OK with audit log array
```

### Frontend Verification

The frontend dev server is started and verified to be accessible on the configured port.

## Deliverables

After successful execution, the steel thread provides:

1. ✅ Automated verification of core system components
2. ✅ Running services for manual QA
3. ✅ Clear success/failure indicators
4. ✅ Troubleshooting information on failure

## Troubleshooting

### Database Connection Failures

- Verify PostgreSQL is running: `cargo run -- db health`
- Check database credentials in `config/application.local.toml`
- Ensure migrations directory exists: `database/migrations/`

### Backend Startup Failures

- Check Gradle build: `./gradlew :modules:backend:api:build`
- Verify port 8080 is available: `lsof -i :8080`
- Review backend logs for errors

### Frontend Startup Failures

- Verify Bun dependencies: `cd modules/frontend/sangita-admin-web && bun install`
- Check port 5001 is available: `lsof -i :5001`
- Review frontend logs for errors

### Authentication Failures

- Verify `ADMIN_TOKEN` matches config: `config/application.local.toml`
- Check backend logs for authentication errors
- Ensure bearer token header is correctly formatted

## Related Commands

```bash
# Full development stack (includes steel thread verification)
cargo run -- dev --start-db

# Database management
cargo run -- db reset      # Reset database (drop → create → migrate → seed)
cargo run -- db migrate    # Apply migrations only
cargo run -- db health     # Check database health

# Quick smoke test (health + search only)
cargo run -- test upload --base-url http://localhost:8080
```

## References

- [Sangita CLI README](../../tools/sangita-cli/README.md)
- [Steel Thread Runbook](../08-operations/runbooks/steel-thread-runbook.md)
- [Backend Architecture](../02-architecture/backend-system-design.md)
- [API Contract](../03-api/api-contract.md)
