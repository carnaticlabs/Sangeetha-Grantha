| Metadata | Value |
|:---|:---|
| **Status** | Active |
| **Version** | 1.0.0 |
| **Last Updated** | 2026-09-10 |
| **Author** | Sangeetha Grantha Team |
| **Document Type** | Current guide |

# Integration testing architecture

---

Database behavior is tested against real PostgreSQL with pgvector, initialized by the same Flyway versioned migrations and repeatable seeds used by development. The earlier custom migration-runner/Testcontainers proposal is now implemented; this guide describes the current substrate.

## Backend test layers

| Layer | Role |
|:---|:---|
| Unit | Pure logic and service behavior with controlled dependencies |
| DAL integration | SQL, constraints, mappings, identity and query behavior |
| API/service integration | Multi-repository scenarios and route authorization/serialization |
| Steel thread | Representative cross-layer application scenario |

Shared support lives in [modules/backend/test-support](../../modules/backend/test-support). [SangitaPostgres](../../modules/backend/test-support/src/main/kotlin/com/sangita/grantha/backend/testsupport/SangitaPostgres.kt) starts a JVM-lifetime `pgvector/pgvector:pg18` Testcontainer. [TestDatabase](../../modules/backend/test-support/src/main/kotlin/com/sangita/grantha/backend/testsupport/TestDatabase.kt) applies Flyway and manages fixture/reset behavior.

This replaces an implicit dependency on a manually started localhost database. An explicitly supplied external test database is an escape hatch for a disposable test target; do not point reset-capable test support at a development corpus or production database.

```bash
./gradlew :modules:backend:api:unitTest
make test-integration
make test
```

`make test` includes API and DAL tests rather than being a unit-only shortcut. Integration tests require Docker and apply the complete current migration set; a stale fixed migration count in an old report is not the test contract.

## Scenario priorities

| Concern | What the test should establish |
|:---|:---|
| Authentication | Roles originate in storage; non-admin tokens cannot reach admin mutations; refresh honors role changes |
| Import/reingest | Correct canonical identity, idempotency, sections, variants, ordered ragas, evidence and audit |
| Versioned canon | Accepted snapshots, monotonically numbered revisions, attribution, as-of reads |
| Raga identity | Alias/key collisions, mela context, queue decisions, junction behavior |
| Public catalogue | Published visibility, V1/V2 enum compatibility, allowlisted DTOs, variant ownership |
| Search | Profile compatibility, indexed-content freshness, audience filtering, no-profile behavior |

Use deterministic fixtures and controlled external-provider responses. Test a meaningful failure or user outcome rather than merely mirroring a private implementation method.

## Worker, frontend, and mobile

Worker tests include parsing, schema/contract checks, provider failure handling, resource lifecycle, and database-backed indexing/queue scenarios. Run the [worker quality commands](../../tools/krithi-extract-enrich-worker/README.md) using frozen dependencies.

Frontend component tests use Vitest. Browser journeys use Playwright against the configured Compose stack; [E2E testing](./qa/e2e-testing.md) explains stateful fixtures and source calls.

Mobile shared tests exercise the V2 client, storage, presenters, and state transitions on JVM. Android/iOS native builds and journey tests exercise platform hosts separately. TRACK-140's native runtime and manual accessibility gates remain distinct from shared test success.

## CI and reporting

[CI](../../.github/workflows/ci.yml) includes backend unit/integration, Flyway migrate/validate, raga checks, frontend types/tests/build, worker lint/types/tests, mobile shared/native builds, and docs checks. [Nightly E2E](../../.github/workflows/e2e-nightly.yml) is a separate workflow.

Record command, revision, environment, date, and result. Keep transient infrastructure failures visible; do not skip/weaken tests to turn a run green. For a real import, add the [post-import checks](./qa/test-plan.md) because isolated fixtures cannot certify the actual corpus.

Design rationale: [ADR-013](../02-architecture/decisions/ADR-013-db-migration-with-flyway.md), [TRACK-110](../../conductor/tracks/TRACK-110-testcontainers-flyway-cutover.md), [TRACK-111](../../conductor/tracks/TRACK-111-dal-suite-ci-activation.md), [TRACK-112](../../conductor/tracks/TRACK-112-money-path-scenarios.md), [TRACK-113](../../conductor/tracks/TRACK-113-worker-e2e-tests.md).

---

[Section index](./README.md) · [Documentation home](./../README.md) · [Feature status](./../01-requirements/features/README.md)
