| Metadata | Value |
|:---|:---|
| **Status** | Active |
| **Version** | 1.1.0 |
| **Last Updated** | 2026-02-08 |
| **Author** | Sangeetha Grantha Team |

---

# ADR-007: Exposed ORM Selection (Kotlin DSL)

## Context
Sangita Grantha requires a type-safe data access layer that integrates tightly with Kotlin, supports PostgreSQL features (UUID, enums, JSONB), and provides predictable performance for complex domain queries. The team evaluated several options including raw SQL, JPA/Hibernate, and Kotlin-native DSLs.

## Decision
Adopt **Exposed (DSL)** as the primary ORM/query builder for the backend.

## Rationale
- **Kotlin-native**: Exposed is idiomatic Kotlin, enabling static typing and IDE support without annotation-heavy boilerplate.
- **PostgreSQL fit**: Supports enums, UUIDs, JSONB, and advanced joins needed for musicological data.
- **Testability**: Works well with in-memory databases for unit tests and transactional DSL for integration tests.
- **Control**: DSL provides explicit SQL control while retaining type safety.

## Consequences
- Requires explicit mapping (DTO mappers) and careful query optimization.
- Exposed DSL updates must stay aligned with library version upgrades.
- Explicit migrations remain outside Exposed (handled by `tools/sangita-cli`).

## Follow-up
- Continue documenting query performance patterns and indexing strategy.
- Track DSL limitations that require raw SQL and document in `decisions/exposed-dsl-optimization-implementation.md`.