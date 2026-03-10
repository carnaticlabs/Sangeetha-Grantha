| Metadata | Value |
|:---|:---|
| **Status** | Active |
| **Version** | 1.1.0 |
| **Last Updated** | 2026-03-10 |
| **Author** | Sangeetha Grantha Team |

# 🤖 AI & Vibe Coding References


---


This document provides essential references for AI coding assistants (VS Code Copilot, Codex, Cursor, Google Antigravity) working with the Sangeetha Grantha codebase.

---

## Product & Requirements

- [Sangita Grantha PRD](../01-requirements/product-requirements-document.md) - Primary product requirements
- [Domain Model](../01-requirements/domain-model.md) - Core entity relationships and data structures
- [Glossary](../01-requirements/glossary.md) - Domain terminology

---

## Architecture & Design

- [Backend Architecture](../02-architecture/backend-system-design.md) - Ktor patterns, service layer, DAL structure
- [Mutation Handlers](../06-backend/mutation-handlers.md) - Audit logging and mutation patterns
- [Security Requirements](../06-backend/security-requirements.md) - Auth, RBAC, and security patterns
- [Tech Stack](../02-architecture/tech-stack.md) - Complete technology inventory

---

## API & Integration

- OpenAPI Spec (Deleted) - Complete API contract
- [API Contract](../03-api/api-contract.md) - API design patterns and conventions
- [UI ↔ API Mapping](../03-api/ui-to-api-mapping.md) - Frontend-backend integration

---

## Database

- [Schema Overview](../04-database/schema.md) - PostgreSQL schema documentation
- [Migrations](../04-database/migrations.md) - Migration strategy (Python db-migrate, NOT Flyway)
- [Audit Log](../04-database/audit-log.md) - Audit trail requirements

---

## Frontend

- [Admin Web PRD](../01-requirements/admin-web/prd.md) - Admin console requirements
- [React Admin Web Specs](../archive/react_admin_web_specifications.md) - Frontend architecture

---

## Development Workflow

- [Sangita CLI README](../README.md) - Database management and dev commands
- [Steel-thread Implementation](../06-backend/steel-thread-implementation.md) - Core workflows

---

## Key Patterns to Follow

### Database Operations
- ✅ Use `DatabaseFactory.dbQuery { }` for all database operations
- ✅ All mutations must write to `AUDIT_LOG` table
- ❌ Never use Flyway - use `make migrate` / `make db-reset` (Python `db-migrate` in `tools/db-migrate`)

### Dependency Management
- ✅ Use `gradle/libs.versions.toml` for dependency management
- ✅ Reference version catalog in build files

### Code Organization
- ✅ Keep DTOs in `modules/shared/domain` for cross-platform use
- ✅ Use `kotlinx.serialization` for DTOs
- ✅ Use `kotlinx.datetime.Instant` (not Java date/time classes)
- ✅ Use `kotlin.uuid.Uuid` for IDs

### Backend Patterns
- ✅ Keep routes thin; delegate to services/repositories
- ✅ Return explicit DTOs, not Exposed entity objects
- ✅ Use `suspend` functions consistently
- ✅ Error handling: sealed results or nullable returns (avoid exceptions unless necessary)

### Frontend Patterns
- ✅ Use function components with explicit TypeScript types
- ✅ Avoid `any` type; prefer `ReactNode`, `PropsWithChildren`, discriminated unions
- ✅ Use Tailwind utility classes for styling
- ✅ Keep components small and composable

---

## Conductor & AI Change Tracking (For Assistants)

When proposing or implementing **any changes** to the codebase, assistants **must follow Conductor tracking rules**:

- **Always register a Track** for any new AI feature or significant enhancement:
  - Add/ensure a row exists in `conductor/tracks.md` with a unique `Track ID`, name, and status.
  - Example: `TRACK-002 | Gemini Transliteration Hardening | In Progress`.
- **Always create or update a Track detail file**:
  - File path: `conductor/tracks/TRACK-XXX-some-slug.md`.
  - Use `TRACK-001-bulk-import-krithis.md` as the canonical template (Goal, Context, Architecture Overview, Phased Plan, Progress Log, Technical Details).
- **Tie code/docs changes to Tracks**:
  - When editing AI-related code or docs (anything under `application_documentation/09-ai/` or Gemini-related services), update the corresponding TRACK file’s **Progress Log**.
  - Ensure the work described in AI docs (e.g., new service, endpoint, or workflow) cites the relevant `TRACK-XXX` where appropriate.

Conductor resources:

- `conductor/index.md`
- `conductor/tracks.md`
- `conductor/tracks/TRACK-001-bulk-import-krithis.md`

## Quick Reference Commands

```bash
# Database management
make migrate      # Run pending migrations
make db-reset     # Reset database (drop → create → migrate → seed)

# Development workflow
make dev          # Start full stack (DB + Backend + Frontend + Extraction)
make dev-down     # Stop all services

# Testing
make test         # Run backend tests
make test-frontend  # Run frontend tests
```

---

## Important Constraints

- ❌ **Never suggest Flyway** - use `make migrate` / `make db-reset`
- ✅ **Always reference version catalog** in build files
- ✅ **Keep changes minimal and surgical**
- ✅ **Follow existing patterns in the codebase**
- ✅ **Use `DatabaseFactory.dbQuery` for all DB operations**

---

For the complete documentation index, see [README.md](../README.md).