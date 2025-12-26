# Sangita Grantha Documentation Index

This documentation tree is the **spec-driven source of truth** for
Sangita Grantha.

The **primary PRD** lives at the repo root:
- `Sangita Grantha – Product Requirements Document.md`

Historical / inherited drafts from earlier projects, and any future
archival docs for Sangita Grantha, live under `./archive/`.

---

- Requirements
  - PRDs
    - [Admin Web PRD](./requirements/prd/admin-web-prd.md)
    - [Mobile App PRD](./requirements/prd/mobile-app-prd.md)
  - [Domain Model](./requirements/domain-model.md)
  - [Glossary](./requirements/glossary.md)
- API
  - [API Contract](./api/api-contract.md)
  - [UI ↔ API Mapping](./api/ui-to-api-mapping.md)
  - [Auth & OTP](./api/auth-otp.md)
  - [Integration Spec](./api/integration-spec.md)
- Database
  - [Schema](./database/schema.md) *(align with Sangita Grantha migrations in `database/migrations`)*
  - [Migrations](./database/migrations.md)
  - [Audit Log](./database/audit-log.md)
  - [Schema vs ERD Differences](./database/archive/differences.md)
  - [Schema Validation](./database/schema-validation.md)
  - [Migrations Runner (Rust)](./database/migrations-runner-rust.md) *(will describe `sangita-cli` once implemented)*
- Backend
  - [Architecture](./backend/architecture.md)
  - [Mutation Handlers](./backend/mutation-handlers.md)
  - [Steel-thread Implementation](./backend/steel-thread-implementation.md)
  - [Security Requirements](./backend/security-requirements.md)
- Frontend
  - Admin Web
    - [PRD](./requirements/prd/admin-web-prd.md)
    - [UI Specs](./frontend/admin-web/ui-specs.md)
    - [Data Mapping](./frontend/admin-web/data-mapping.md)
  - Mobile
    - [Android Setup](./frontend/mobile/android-setup.md)
    - [iOS Setup](./frontend/mobile/ios-setup.md)
    - [Mobile UI (KMM)](./frontend/mobile/mobile-ui.md)
- Ops
  - [Config](./ops/config.md)
  - Runbooks
    - [Steel-thread Runbook](./ops/runbooks/steel-thread-runbook.md)
- QA
  - [Verification Report](./qa/verification-report.md)
  - [Test Plan](./qa/test-plan.md)
- Diagrams
  - [Flows](./diagrams/flows.md)
  - [ERD](./diagrams/erd.md)
- Decisions
  - [ADR Index](./decisions/adr-index.md)

Guidance:
- For **Sangita Grantha-specific changes** (schema, API, UI, workflows),
  update the corresponding spec files here **and** the root PRD.
- When you create a new Krithi-related feature, ensure there is:
  - A requirements note (under `requirements/`),
  - An API spec (under `api/`),
  - A DB note if schema changes (under `database/`), and
  - UI specs if it affects admin/mobile (under `frontend/`).
