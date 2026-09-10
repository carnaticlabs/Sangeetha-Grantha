| Metadata | Value |
|:---|:---|
| **Status** | Active |
| **Version** | 1.1.0 |
| **Last Updated** | 2026-09-10 |
| **Author** | Sangeetha Grantha Team |
| **Document Type** | Decision record |

---

# ADR-008: Dependency Injection with Koin

---

> [!NOTE]
> Decision record: preserve the original rationale and check its decision/supersession status. Current runtime guidance is in [system architecture](./../backend-system-design.md) and [Flyway migrations](./../../04-database/migrations.md).

## Context
The backend service layer grew to 20+ services with manual wiring in `App.kt`. Manual DI reduced clarity, complicated testing, and increased the risk of wiring errors as dependencies expanded.

## Decision
Adopt **Koin** for dependency injection in the Ktor backend.

## Rationale
- **Kotlin-first**: DSL aligns with Kotlin idioms and avoids heavy reflection.
- **Ktor integration**: Native support for Ktor application lifecycle and inject() helpers.
- **Testability**: Enables easy mocking and module overrides in tests.
- **Modularity**: Clear separation of DAL and application modules.

## Consequences
- Requires explicit module definitions (`dalModule`, `appModule`).
- Cyclic dependencies must be modeled via providers/lazy access.
- Adds small runtime overhead for DI resolution.

## Follow-up
- Expand module structure as new services are introduced.
- Add module-specific tests for critical dependencies.

---

[Section index](./README.md) · [Documentation home](./../../README.md) · [Feature status](./../../01-requirements/features/README.md)
