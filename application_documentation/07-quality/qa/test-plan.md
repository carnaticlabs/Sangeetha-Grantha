| Metadata | Value |
|:---|:---|
| **Status** | Active |
| **Version** | 1.1.0 |
| **Last Updated** | 2026-02-08 |
| **Author** | Sangeetha Grantha Team |

# Test Plan

This document defines the testing strategy, environments, and scenarios for Sangita Grantha.

---

## 1. Scope

### 1.1 In Scope

| Component | Test Types | Coverage Target |
|-----------|------------|-----------------|
| **Backend API** | Unit, Integration, E2E | 80% |
| **Data Access Layer (DAL)** | Unit, Integration | 85% |
| **Admin Web Frontend** | Unit, Component, E2E | 70% |
| **Database Migrations** | Integration | 100% |
| **Import Pipeline** | Integration, E2E | 80% |

### 1.2 Out of Scope (v1)

- Mobile app testing (future phase)
- Performance/load testing (planned for v1.1)
- Security penetration testing (planned external audit)
- Accessibility testing (planned for v1.1)

### 1.3 Test Objectives

1. **Functional correctness**: All API endpoints behave per specification
2. **Data integrity**: Database constraints and business rules enforced
3. **Authentication**: JWT auth properly secures admin endpoints
4. **Import pipeline**: Bulk import handles edge cases gracefully
5. **UI functionality**: Admin web supports all CRUD operations

---

## 2. Test Environments

### 2.1 Local Development

| Component | Configuration |
|-----------|---------------|
| **Database** | PostgreSQL 15 via Docker Compose |
| **Backend** | Ktor on port 8080 |
| **Frontend** | Vite dev server on port 5001 |
| **CLI** | Rust-based `sangita-cli` |

**Setup Commands:**
```bash
# Start full stack
mise exec -- cargo run --manifest-path tools/sangita-cli/Cargo.toml -- dev --start-db

# Reset database
mise exec -- cargo run --manifest-path tools/sangita-cli/Cargo.toml -- db reset

# Run backend tests
./gradlew :modules:backend:api:test

# Run DAL tests
./gradlew :modules:backend:dal:test
```

### 2.2 CI Environment (GitHub Actions)

| Stage | Purpose | Trigger |
|-------|---------|---------|
| **Build** | Compile all modules | Every PR |
| **Unit Tests** | Run unit tests | Every PR |
| **Integration Tests** | Run with test DB | Every PR |
| **Steel Thread** | E2E smoke test | Merge to main |

### 2.3 Staging Environment

| Component | Configuration |
|-----------|---------------|
| **Database** | Cloud SQL (PostgreSQL 15) |
| **Backend** | Cloud Run |
| **Frontend** | Cloud Storage + CDN |
| **URL** | `staging.sangitagrantha.org` |

---

## 3. Test Matrix

### 3.1 Backend API Test Matrix

| Endpoint Group | Unit | Integration | E2E | Notes |
|----------------|------|-------------|-----|-------|
| `/health` | - | ✅ | ✅ | Health check |
| `/v1/krithis` (public) | ✅ | ✅ | ✅ | Search, detail |
| `/v1/admin/krithis` | ✅ | ✅ | ✅ | CRUD operations |
| `/v1/admin/variants` | ✅ | ✅ | ✅ | Lyric variants |
| `/v1/composers` | ✅ | ✅ | ✅ | Reference data |
| `/v1/ragas` | ✅ | ✅ | ✅ | Reference data |
| `/v1/talas` | ✅ | ✅ | ✅ | Reference data |
| `/v1/admin/imports` | ✅ | ✅ | ✅ | Import pipeline |
| `/auth/token` | ✅ | ✅ | ✅ | Authentication |
| `/v1/audit/logs` | ✅ | ✅ | - | Audit trail |

### 3.2 Frontend Test Matrix

| Page/Component | Unit | Component | E2E |
|----------------|------|-----------|-----|
| Login | ✅ | ✅ | ✅ |
| Dashboard | - | ✅ | ✅ |
| Krithi List | ✅ | ✅ | ✅ |
| Krithi Editor | ✅ | ✅ | ✅ |
| Composer CRUD | ✅ | ✅ | ✅ |
| Raga CRUD | ✅ | ✅ | ✅ |
| Import Upload | ✅ | ✅ | ✅ |
| Import Review | ✅ | ✅ | ✅ |

### 3.3 Database Test Matrix

| Migration | Forward | Rollback | Idempotent |
|-----------|---------|----------|------------|
| 01 - Baseline | ✅ | Manual | ✅ |
| 02 - Domain Tables | ✅ | Manual | ✅ |
| 03 - Constraints | ✅ | Manual | ✅ |
| 04 - Import Pipeline | ✅ | Manual | ✅ |
| 05 - Sections/Tags | ✅ | Manual | ✅ |
| 06 - Notation | ✅ | Manual | ✅ |

---

## 4. Core Scenarios

### 4.1 Authentication Scenarios

| ID | Scenario | Expected Result |
|----|----------|-----------------|
| AUTH-01 | Valid admin token login | JWT returned, 200 OK |
| AUTH-02 | Invalid token login | 401 Unauthorized |
| AUTH-03 | Expired JWT on request | 401 Unauthorized |
| AUTH-04 | Missing Authorization header | 401 Unauthorized |
| AUTH-05 | Valid JWT, insufficient role | 403 Forbidden |

### 4.2 Krithi CRUD Scenarios

| ID | Scenario | Expected Result |
|----|----------|-----------------|
| KRITHI-01 | Create krithi with required fields | 201 Created |
| KRITHI-02 | Create krithi missing composer | 400 Bad Request |
| KRITHI-03 | Get published krithi (public) | 200 OK with data |
| KRITHI-04 | Get draft krithi (public) | 404 Not Found |
| KRITHI-05 | Get draft krithi (admin) | 200 OK with data |
| KRITHI-06 | Update krithi title | 200 OK, audit logged |
| KRITHI-07 | Delete krithi with variants | Cascade delete, audit logged |
| KRITHI-08 | Search by title substring | Matching results |
| KRITHI-09 | Search by raga filter | Filtered results |
| KRITHI-10 | Search by composer + raga | Combined filter |

### 4.3 Lyric Variant Scenarios

| ID | Scenario | Expected Result |
|----|----------|-----------------|
| VAR-01 | Add Tamil variant | 201 Created |
| VAR-02 | Add duplicate language/script | 409 Conflict |
| VAR-03 | Add variant with sections | Sections linked correctly |
| VAR-04 | Update section text | 200 OK, normalized text updated |
| VAR-05 | Delete variant | Cascade to sections |

### 4.4 Import Pipeline Scenarios

| ID | Scenario | Expected Result |
|----|----------|-----------------|
| IMP-01 | Upload valid CSV | All rows imported as pending |
| IMP-02 | Upload CSV with invalid columns | 400 with error details |
| IMP-03 | Upload CSV with empty rows | Rows skipped, warning returned |
| IMP-04 | Map import to existing krithi | Status = mapped |
| IMP-05 | Map import to new krithi | Krithi created, status = mapped |
| IMP-06 | Reject import with notes | Status = rejected, notes saved |
| IMP-07 | Entity resolution suggestions | Fuzzy matches returned |
| IMP-08 | Bulk approve imports | All selected updated |

### 4.5 Reference Data Scenarios

| ID | Scenario | Expected Result |
|----|----------|-----------------|
| REF-01 | Create composer | 201 Created |
| REF-02 | Create composer duplicate name | 409 Conflict |
| REF-03 | Update composer | 200 OK, audit logged |
| REF-04 | Delete composer with krithis | 409 Conflict (FK violation) |
| REF-05 | Create raga with parent | Parent relationship established |
| REF-06 | List ragas with melakarta filter | Filtered results |

---

## 5. Regression Suite

### 5.1 Smoke Tests (Run on Every PR)

| Test | Command | Expected |
|------|---------|----------|
| Backend health | `curl /health` | 200 OK |
| Public search | `curl /v1/krithis/search` | 200 OK |
| Admin auth | Login flow | JWT returned |
| DB connectivity | `cargo run -- db health` | Connected |

### 5.2 Full Regression (Run on Merge to Main)

1. **Steel Thread Test**
```bash
   cargo run -- test steel-thread
```

2. **Backend Unit Tests**
```bash
   ./gradlew :modules:backend:api:test
   ./gradlew :modules:backend:dal:test
```

3. **Frontend Tests**
```bash
   cd modules/frontend/sangita-admin-web
   bun run test
```

### 5.3 Nightly Regression

- Full database reset and seed
- All E2E scenarios
- Performance baseline (response times)
- Import pipeline with large dataset

---

## 6. Acceptance Criteria

### 6.1 Feature Acceptance

A feature is considered complete when:

- [ ] All unit tests pass
- [ ] All integration tests pass
- [ ] Code review approved
- [ ] Documentation updated
- [ ] Audit logging implemented (for mutations)
- [ ] Steel thread passes

### 6.2 Release Acceptance

A release is ready when:

- [ ] All regression tests pass
- [ ] No critical or high-severity bugs
- [ ] Performance within targets (p95 < 500ms)
- [ ] Security review completed
- [ ] Documentation complete
- [ ] Staging deployment verified

### 6.3 Quality Gates

| Gate | Threshold | Action if Failed |
|------|-----------|------------------|
| Unit test coverage | > 70% | Block merge |
| Build success | 100% | Block merge |
| Steel thread | Pass | Block deploy |
| Critical bugs | 0 | Block release |
| High bugs | < 3 | Release with plan |

---

## 7. Test Data Management

### 7.1 Seed Data

Located in `database/seed_data/`:

| File | Contents | Purpose |
|------|----------|---------|
| `01_reference_data.sql` | Composers, ragas, talas, admin user | Core reference data |
| `02_sample_krithis.sql` | Sample krithis with variants | Development/testing |

### 7.2 Test Fixtures

**Backend fixtures** (`modules/backend/api/src/test/resources/`):
- `test-krithi.json` - Sample krithi payload
- `test-import.csv` - Sample import file
- `test-user.json` - Test user data

**Frontend fixtures** (`modules/frontend/sangita-admin-web/src/__fixtures__/`):
- Mock API responses
- Component test data

### 7.3 Data Reset

```bash
# Full reset (drop → create → migrate → seed)
cargo run -- db reset

# Seed only (preserves existing data)
./gradlew :modules:backend:api:seedDatabase
```

### 7.4 Test Isolation

- Each test class uses transactions rolled back after test
- Integration tests use dedicated test database
- E2E tests reset state in `beforeEach`

---

## 8. Reporting

### 8.1 Test Reports

| Report | Format | Location |
|--------|--------|----------|
| JUnit results | XML | `build/test-results/` |
| Coverage (Kotlin) | HTML | `build/reports/jacoco/` |
| Frontend tests | JSON | `coverage/` |
| Steel thread | Console | STDOUT |

### 8.2 CI Integration

- JUnit reports uploaded as GitHub Actions artifacts
- Coverage reports posted to PR comments
- Steel thread status reported in PR checks

### 8.3 Metrics Tracked

| Metric | Target | Current |
|--------|--------|---------|
| Backend unit test coverage | 80% | TBD |
| DAL test coverage | 85% | TBD |
| Frontend test coverage | 70% | TBD |
| Steel thread pass rate | 100% | 100% |
| Avg. test execution time | < 5 min | TBD |

---

## 9. Related Documents

- [Steel Thread Implementation](../../06-backend/steel-thread-implementation.md)
- [Steel Thread Runbook](../../08-operations/runbooks/steel-thread-runbook.md)
- [API Contract](../../03-api/api-contract.md)
- [E2E Testing Guide](./e2e-testing.md) *(planned)*