| Metadata | Value |
|:---|:---|
| **Status** | Active |
| **Version** | 1.0.0 |
| **Last Updated** | 2026-01-27 |
| **Author** | Sangeetha Grantha Team |

---

# ADR-008: Dependency Injection with Koin

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
