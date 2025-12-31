# 🤖 AI & Vibe Coding References

This document provides essential references for AI coding assistants (VS Code Copilot, Codex, Cursor, Google Antigravity) working with the Sangeetha Grantha codebase.

---

## Product & Requirements

- [Sangita Grantha PRD](./requirements/Sangita%20Grantha%20–PRD.md) - Primary product requirements
- [Domain Model](./requirements/domain-model.md) - Core entity relationships and data structures
- [Glossary](./requirements/glossary.md) - Domain terminology

---

## Architecture & Design

- [Backend Architecture](./backend/architecture.md) - Ktor patterns, service layer, DAL structure
- [Mutation Handlers](./backend/mutation-handlers.md) - Audit logging and mutation patterns
- [Security Requirements](./backend/security-requirements.md) - Auth, RBAC, and security patterns
- [Tech Stack](./tech-stack.md) - Complete technology inventory

---

## API & Integration

- [OpenAPI Spec](../openapi/sangita-grantha.openapi.yaml) - Complete API contract
- [API Contract](./api/api-contract.md) - API design patterns and conventions
- [UI ↔ API Mapping](./api/ui-to-api-mapping.md) - Frontend-backend integration

---

## Database

- [Schema Overview](./database/SANGITA_SCHEMA_OVERVIEW.md) - PostgreSQL schema documentation
- [Migrations](./database/migrations.md) - Migration strategy (Rust-based, NOT Flyway)
- [Audit Log](./database/audit-log.md) - Audit trail requirements

---

## Frontend

- [Admin Web PRD](./requirements/prd/admin-web-prd.md) - Admin console requirements
- [React Admin Web Specs](./react_admin_web_specifications.md) - Frontend architecture

---

## Development Workflow

- [Sangita CLI README](../tools/sangita-cli/README.md) - Database management and dev commands
- [Steel-thread Implementation](./backend/steel-thread-implementation.md) - Core workflows

---

## Key Patterns to Follow

### Database Operations
- ✅ Use `DatabaseFactory.dbQuery { }` for all database operations
- ✅ All mutations must write to `AUDIT_LOG` table
- ❌ Never use Flyway - use Rust migration tool in `tools/sangita-cli`

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

## Quick Reference Commands

```bash
# Database management
cd tools/sangita-cli
cargo run -- db migrate          # Run migrations only
cargo run -- db reset            # Reset database (drop → create → migrate → seed)
cargo run -- db health           # Check database health

# Development workflow
cargo run -- dev                 # Start Backend + Frontend
cargo run -- dev --start-db      # Start DB + Backend + Frontend

# Testing
cargo run -- test steel-thread   # Run end-to-end smoke tests
```

---

## Important Constraints

- ❌ **Never suggest Flyway** - use Rust migration tool
- ✅ **Always reference version catalog** in build files
- ✅ **Keep changes minimal and surgical**
- ✅ **Follow existing patterns in the codebase**
- ✅ **Use `DatabaseFactory.dbQuery` for all DB operations**

---

For the complete documentation index, see [README.md](./README.md).

