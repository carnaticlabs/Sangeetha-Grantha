| Metadata | Value |
|:---|:---|
| **Status** | Proposed |
| **Version** | 1.3.0 — Flyway decision ratified as [ADR-013](../02-architecture/decisions/ADR-013-db-migration-with-flyway.md); 1.2.0 added migration & seed-data tooling re-evaluation (§5) and Java/Python Testcontainers coexistence (§5.6); 1.1.0 added container-runtime analysis (§3.5) |
| **Last Updated** | 2026-06-12 |
| **Author** | Integration testing analysis (for Seshadri) |
| **Companion docs** | `../north-star-evaluation.md` (N2/N3), `broken-tests-remediation.md`, `../02-architecture/backend-system-design.md` |
| **Scope** | Detailed analysis of Testcontainers and equivalent options, plus a recommended integration-test architecture and scenario suite for the whole stack |

# Integration Testing Approach — Testcontainers Analysis & Recommendation

> **The question this answers:** the north-star evaluation rates the verification gap (N2/N3) as *Critical* — 22 backend test files but 0 DAL tests, 0 frontend tests, no CI, and database-dependent tests that silently require a hand-started Postgres on `localhost:5432`. What is the right technology and architecture to build a meaningful, real-life-scenario integration test suite, and how do Testcontainers and its alternatives compare for *this* codebase?

**TL;DR — Recommendation:** adopt **Testcontainers for the JVM** with a singleton, migration-initialized `PostgreSQLContainer` as the backbone of backend and DAL integration tests; keep the truncate-between-tests machinery (it is good and transfers unchanged); add **testcontainers-python** for the extraction worker's DB-touching paths (Java and Python Testcontainers coexist freely — §5.6); and use **Playwright against the compose stack** for E2E. Reject H2/in-memory outright. Keep an env-var escape hatch so tests can still target an externally provided database (CI service containers, or a developer's already-running `make db`) when that is faster. **On migrations (§5):** consolidate the two custom, already-diverged migration runners onto **Flyway Community** — one standards-based engine driving Kotlin tests (JVM API), Make/dev (CLI/Docker), Python tests, and CI, with repeatable migrations taking over reference-data seeding; **ratified as [ADR-013](../02-architecture/decisions/ADR-013-db-migration-with-flyway.md)** (2026-06-12), which amended project Critical Rule #1.

> **Implemented — Steps 1–4 done ([TRACK-110](../../conductor/tracks/TRACK-110-testcontainers-flyway-cutover.md) + [TRACK-111](../../conductor/tracks/TRACK-111-dal-suite-ci-activation.md)).** Steps 1–2: the Flyway cutover (Sub-part A) and this Testcontainers substrate (Sub-part B). Steps 3–4 (TRACK-111): the DAL suite (D1–D6, 11 tests, in `modules/backend/dal/src/test`), the shared `:modules:backend:test-support` module (D11), the central D5 typed-error layer (`DalErrors` mapped in `DatabaseFactory.dbQuery`), and GitHub Actions CI (`.github/workflows/ci.yml`) gating all layers. As-built notes that refine the proposal below:
> - **Substrate**: `SangitaPostgres` (Testcontainers, `docker.io/library/postgres:18.3-alpine`) + `TestDatabase` migrate the container via the **Flyway JVM API** — the 156-line `MigrationRunner` is deleted. Dep: `org.testcontainers:postgresql` 1.21.4 (no BOM / `junit-jupiter` module needed; the singleton object needs no annotations). `TEST_DATABASE_URL` is the escape hatch (§3.2).
> - **Real reference seed in tests**: the test Flyway applies the **full `V__`+`R__`** set (identical to `make db-reset`), so integration tests exercise the real reference data (entity resolution against the 972-raga seed, etc.). `TestFixtures` consume the seed via `findOrCreate` (returning the seeded Tyagaraja/Kalyani/Adi rather than inserting duplicates), and the truncate-reset **preserves the reference tables** (`flyway_schema_history` + roles/composers/ragas/talas/deities/composer_aliases/import_sources) so the seed stays stable across tests. DAL mechanics tests that need throwaway entities use distinct non-seed probe names.
> - **Tagging**: `@Tag("integration")` + a tag-filtered `integrationTest` Gradle task (`make test-integration`); `make test` / `./gradlew check` run everything.

---

## 1. Where We Are Today

### 1.1 What already exists (and is worth keeping)

The backend test suite has a real integration-test substrate, built in-house:

| Asset | Location | What it does |
|:---|:---|:---|
| `IntegrationTestBase` | `modules/backend/api/src/test/.../support/IntegrationTestBase.kt` | JUnit 5 base class: creates `sangita_grantha_test` once per JVM, runs real migrations, connects Exposed, truncates all tables after each test |
| `MigrationRunner` | `.../support/MigrationRunner.kt` | Parses and applies the real `database/migrations/*.sql` files (43 today), tracked in `_sqlx_migrations` — **schema parity with production is already guaranteed** |
| `TestDatabaseFactory` | `.../support/TestDatabaseFactory.kt` | Legacy object-style equivalent, kept for older tests |
| 9 DB-dependent test classes | `KrithiServiceTest`, `ImportServiceTest`, `EntityResolutionServiceTest`, `ExtractionResultProcessorTest`, `QualityScoringServiceTest`, `LyricVariantPersistenceServiceTest`, `AutoApprovalServiceTest`*, `ImportRoutesTest`, etc. | Service- and route-level tests that exercise real SQL |

This is genuinely the hard part done: migration-true schema setup and a fast truncate-based reset strategy are exactly what a Testcontainers suite needs internally. The investment transfers, not discards.

### 1.2 What is wrong with the current substrate

1. **Hard-coded host dependency.** `jdbc:postgresql://localhost:5432` with `postgres/postgres` literals. Tests fail with connection errors unless a developer remembered `make db`. Nothing in Gradle expresses or provisions this dependency.
2. **Unpinned database version.** Tests run against *whatever* is listening on 5432. Compose pins `postgres:18.3-alpine`; the test suite pins nothing. A developer with PG16 locally gets different behavior (UUID v7 functions, FTS details) than production.
3. **Shared mutable state.** One fixed database name means: no parallel test execution, no two branches' test runs side by side, and a developer's dev database on the same instance is one typo away from being truncated.
4. **No CI story.** "Start a DB first" is precisely the kind of implicit precondition that has kept CI from existing (north-star N2). The test infrastructure must be self-provisioning before CI is cheap to add.
5. **The DAL module has zero tests** — `modules/backend/dal` has only `kotlin("test")` on its test classpath and no test sources. Exposed table definitions, repository queries, and all 43 migrations are validated only incidentally, via api-module tests that happen to touch them.
6. **Documentation drift.** `modules/backend/CLAUDE.md` describes test conventions (`IntegrationTestEnv`, `IntegrationSeedData`, an `integration/` package) that do not exist in the codebase. The conventions this document proposes should replace that section.

### 1.3 Constraints any solution must satisfy

These come straight from the schema and roadmap, and they eliminate some options immediately:

- **PostgreSQL 18 semantics are load-bearing**: UUID v7 keys across 27 tables, PG enums, triggers, `session_replication_role` (used by the truncate reset), and full-text search.
- **pgvector is on the roadmap** (TRACK-108 semantic search; north-star §3.3 prescribes pgvector + FTS in one database). The test database must be able to load the extension — which means it must be *real Postgres from a controllable image*, not an emulation.
- **Migrations are the only schema source.** Project Critical Rule #1 currently mandates the custom `db-migrate` tool over Flyway/Liquibase; §5 re-evaluates that rule in light of the Testcontainers decision. Whatever the engine, the test harness must apply `database/migrations/` verbatim or the migration-parity guarantee is lost.
- **Three layers need coverage** (Kotlin backend, Python worker, React frontend) and the north-star explicitly calls for "DAL test suite via Testcontainers" in Phase 1, item 6.
- **Toolchain**: JVM 25, Kotlin 2.3.0, Gradle 9.1, JUnit 5, Ktor 3.4 (`testApplication` available), MockK present. Docker 29.x is installed on the dev machine (Docker Desktop on macOS).

---

## 2. Options Analysis

### Option A — Testcontainers for the JVM ✅ Recommended

[Testcontainers](https://java.testcontainers.org/) starts throwaway Docker containers from test code, waits for readiness, exposes mapped ports, and tears down via a sidecar (Ryuk) even when the JVM crashes.

**How it fits this codebase**

```kotlin
// modules/backend/dal/src/test/kotlin/.../support/SangitaPostgres.kt
object SangitaPostgres {
    val container: PostgreSQLContainer<*> by lazy {
        PostgreSQLContainer(DockerImageName.parse("postgres:18.3-alpine"))
            .withDatabaseName("sangita_grantha_test")
            .withUsername("postgres")
            .withPassword("postgres")
            .withReuse(true)                       // opt-in: survives across Gradle runs locally
            .apply {
                start()
                DriverManager.getConnection(jdbcUrl, username, password).use { conn ->
                    MigrationRunner.runMigrations(conn, MigrationRunner.loadMigrations())
                }
            }
    }
}
```

`IntegrationTestBase` changes only its connection bootstrap: instead of hard-coded `localhost:5432`, it asks `SangitaPostgres.container.jdbcUrl` (or an env-var override — §3.2). The truncate-after-each-test reset, the migration parity, and all 9 existing test classes carry over **without rewriting a single test body**.

**Strengths**
- **Exact production parity, pinned.** Same image tag as `compose.yaml` (`postgres:18.3-alpine`); swap to `pgvector/pgvector:pg18` the day TRACK-108 needs the extension. No emulation gap, ever.
- **Self-provisioning.** `./gradlew test` works on a fresh clone with only Docker present. This is the property that makes CI (north-star Phase 0, item 3) a one-day task instead of a project: Testcontainers works out of the box on GitHub Actions `ubuntu-latest` runners — no service-container YAML needed.
- **Isolation knobs.** One container per JVM (singleton pattern, above) is the right default; per-class or per-test containers are available for destructive tests (e.g., migration-failure scenarios). Parallel Gradle test JVMs each get their own container and can no longer collide.
- **Reuse mode** (`~/.testcontainers.properties` → `testcontainers.reuse.enable=true`) keeps the container alive between local runs, eliminating the ~3–5 s startup for the inner dev loop. CI skips reuse and gets clean containers.
- **Ecosystem breadth for later phases**: `testcontainers-python` (worker), `@testcontainers/postgresql` for Node (if frontend integration tests ever need a real API), `MockServer`/`WireMock` containers for stubbing Gemini at the HTTP boundary, `LocalStack` for the S3 client already in the dependency tree.
- **First-party Kotlin/JUnit 5 support**: `org.testcontainers:junit-jupiter` gives `@Testcontainers`/`@Container` annotations when wanted; the plain singleton object above needs no annotations at all.

**Costs and risks**
- **Docker becomes a hard test-time dependency.** Already true for `make dev`; the dev machine runs Docker Desktop 29.x. Colima/Podman/Rancher Desktop also work (Testcontainers honors `DOCKER_HOST`). Mitigation for the rare Docker-less environment: the env-var escape hatch (§3.2).
- **First-run startup cost**: image pull once, then ~2–5 s container start + ~2–4 s for 43 migrations per JVM. With the singleton + reuse pattern this is paid once per session, not per test. Current suite pays ~0 s but only because it externalizes the cost to a human running `make db`.
- **One more dependency family** in `libs.versions.toml` (BOM + two artifacts). Maintenance burden is low; the library is the de-facto JVM standard with releases tracking Docker API changes.

**Gradle wiring** (versions in the catalog per Critical Rule #2):

```toml
# gradle/libs.versions.toml
testcontainers = "1.21.3"   # verify latest at adoption time

testcontainers-bom = { module = "org.testcontainers:testcontainers-bom", version.ref = "testcontainers" }
testcontainers-postgresql = { module = "org.testcontainers:postgresql" }
testcontainers-junit-jupiter = { module = "org.testcontainers:junit-jupiter" }
```

```kotlin
// modules/backend/dal/build.gradle.kts and api/build.gradle.kts
testImplementation(platform(libs.testcontainers.bom))
testImplementation(libs.testcontainers.postgresql)
testImplementation(libs.testcontainers.junit.jupiter)
```

### Option B — Zonky.io Embedded Postgres ⚠️ Viable fallback, not recommended

[`io.zonky.test:embedded-postgres`](https://github.com/zonkyio/embedded-postgres) downloads native Postgres binaries and runs them as a child process — real Postgres, **no Docker**.

- **Pros:** fastest cold start (~1–2 s), zero Docker dependency, real Postgres semantics.
- **Cons:** binary availability lags — PG18 + macOS arm64 + Linux x64 must all exist in the binary repackaging project, and historically new major versions take months; **no pgvector** without building custom binary bundles (a maintenance project in itself); per-platform quirks (locale, ICU) that Docker images don't have; smaller community.
- **Verdict:** the pgvector roadmap item alone disqualifies it as the primary choice. Keep in mind only if Docker-in-CI ever becomes impossible (it won't on GitHub Actions).

### Option C — H2 / in-memory database in PostgreSQL mode ❌ Rejected

The classic "fast tests" shortcut. For this schema it is not an emulation gap, it is a wall: **no UUID v7 generation, no PG enum types, no triggers as written, no `session_replication_role`, no FTS (`tsvector`), no pgvector, no JSONB operator parity** — and the 43 migration files would not even apply, so the migration-parity guarantee dies on day one. Every hour spent making H2 swallow the schema is an hour spent testing a database we don't run. Rejected without reservation; the north-star principle is "machines verify" — verifying against a different database verifies nothing.

### Option D — Status quo hardened: external DB + CI service containers ⚠️ Partial credit

Keep `localhost:5432`, document it, and in CI use GitHub Actions `services: postgres:18.3-alpine`.

- **Pros:** zero new dependencies; CI service containers are simple and fast; the existing harness already works this way.
- **Cons:** fixes nothing locally (manual `make db`, version drift, shared mutable DB, no parallelism); test configuration forks between CI (service container) and local (hand-started DB); pgvector means remembering to change the image in two places.
- **Verdict:** not as the strategy — but its *mechanism* survives as the escape hatch in the recommended design (§3.2): when `TEST_DATABASE_URL` is set, the suite uses it and skips container startup. CI can then choose either mode; local developers get Testcontainers by default.

### Option E — Gradle-orchestrated Docker Compose (`com.avast.gradle:gradle-docker-compose-plugin`) ⚠️ Wrong layer

Spin the existing `compose.yaml` `db` service up/down around the `test` task.

- **Pros:** reuses the compose file; one DB definition.
- **Cons:** all tests share one instance again (no parallelism, no isolation); lifecycle is coupled to Gradle task graph rather than test code (IDE single-test runs don't get a DB); port conflicts with the dev stack the user runs via `start-sangita.sh`; teardown on crash is unreliable compared to Ryuk.
- **Verdict:** rejected for unit/integration layers. Compose remains the right tool one layer up — the **E2E** layer, where the full stack (DB + backend + frontend) genuinely should run as it does in `make dev` (§4.4).

### Comparison summary

| Criterion | A. Testcontainers | B. Zonky embedded | C. H2 | D. External + CI services | E. Compose plugin |
|:---|:---|:---|:---|:---|:---|
| Real PG 18.3, pinned | ✅ | ⚠️ binary lag | ❌ | ⚠️ unpinned locally | ✅ |
| pgvector-ready (TRACK-108) | ✅ image swap | ❌ | ❌ | ⚠️ manual | ⚠️ manual |
| Migration parity via `MigrationRunner` | ✅ unchanged | ✅ | ❌ | ✅ | ✅ |
| Works on fresh clone, one command | ✅ | ✅ | ✅ | ❌ | ⚠️ |
| Same config local & CI | ✅ | ✅ | ✅ | ❌ forks | ⚠️ |
| Test isolation / parallel JVMs | ✅ | ✅ | ✅ | ❌ | ❌ |
| IDE single-test runs self-contained | ✅ | ✅ | ✅ | ❌ | ❌ |
| No Docker required | ❌ | ✅ | ✅ | ❌ | ❌ |
| Python worker reuse of same approach | ✅ testcontainers-python | ❌ | ❌ | ⚠️ | ⚠️ |
| Maintenance risk | Low | Medium-high | n/a | Low | Medium |

**Decision: Option A (Testcontainers), with Option D's external-URL mode retained as an escape hatch, and compose reserved for the E2E layer.**

---

## 3. Recommended Architecture

### 3.1 Test taxonomy — where integration tests sit

```
            ┌──────────── E2E (Playwright vs compose stack) ───────────┐   few, slow
            │  login → review → approve   bulk import   krithi edit    │
            ├──────────── API integration (Ktor testApplication ──────┤
            │              + Testcontainers PG)                        │   route + auth +
            │  full HTTP: status codes, RBAC, ETag, error envelopes    │   serialization
            ├──────────── Service integration (existing pattern ──────┤
            │              + Testcontainers PG)                        │   the current 9 classes,
            │  ingestion → review → canon flows, audit invariant       │   grown to cover money paths
            ├──────────── DAL integration (NEW, in dal module ─────────┤
            │              + Testcontainers PG)                        │   tables, repos, migrations,
            │  CRUD + junction tables + constraints + UUID v7          │   north-star Phase 1 item 6
            └──────────── Unit (no DB: parsers, scoring, mappers) ────┘   many, fast — unchanged
```

The worker keeps its pytest unit suite and gains a small `testcontainers-python` layer for the DB-queue paths (claim job → write payload → mark complete).

### 3.2 The shared database fixture

One support module (duplicated minimally or extracted to a `backend/test-support` source set) provides:

```kotlin
object TestDatabase {
    /** External override: CI service container, or a dev's running `make db`. */
    private val externalUrl = System.getenv("TEST_DATABASE_URL")

    val jdbcUrl: String; val username: String; val password: String
    // if externalUrl != null → parse and use it (current behavior, opt-in)
    // else → SangitaPostgres.container (Testcontainers singleton, the default)
}
```

Rules:
- **Default path is Testcontainers.** Fresh clone + Docker → green tests.
- **`TEST_DATABASE_URL` set → use it verbatim**, keep today's behavior for developers who prefer the always-on `make db` instance (faster inner loop, no container churn) and for CI configurations that prefer service containers.
- `IntegrationTestBase` keeps its exact lifecycle (migrate once per JVM via `MigrationRunner`, truncate after each test) — only the URL source changes.
- Local reuse mode documented in `getting-started.md`: one line in `~/.testcontainers.properties`.

### 3.3 Keeping unit tests fast — task separation

Tag DB-backed tests and split Gradle execution so the pure-unit loop never pays container cost:

```kotlin
// JUnit tag on IntegrationTestBase
@Tag("integration")
abstract class IntegrationTestBase { ... }
```

```kotlin
// build.gradle.kts (api and dal)
tasks.test { useJUnitPlatform { excludeTags("integration") } }
tasks.register<Test>("integrationTest") {
    useJUnitPlatform { includeTags("integration") }
    workingDir = rootProject.projectDir   // MigrationRunner resolves database/migrations
    shouldRunAfter(tasks.test)
}
tasks.named("check") { dependsOn("integrationTest") }
```

Makefile gains `make test-integration`; `make test` keeps running everything via `check`.

### 3.4 Test data strategy

- **Deterministic fixtures**: fixed UUIDs and timestamps (the convention `modules/backend/CLAUDE.md` already states) — extend `TestFixtures.kt` into builder-style factories: `aKrithi { ragas = listOf(kalyani, todi) }`, `anImportedKrithi { status = PENDING_REVIEW }`.
- **Seed reference data once per JVM** (ragas, talas, composers, languages) after migrations; truncate-reset must therefore re-seed reference tables, or — simpler — the truncate query excludes reference tables alongside `_sqlx_migrations`.
- **Junction-table assertions are mandatory** in any fixture-creating helper (project lesson: verify `krithi_ragas` rows, not just FK columns on the main entity).
- **Golden payloads**: real (anonymized) extraction JSON from the Trinity import as `src/test/resources/payloads/` — the payload-format-convergence regressions (TRACK-096) become pinned fixtures.

### 3.5 Container runtime: Docker vs Podman (the design is runtime-agnostic)

A natural follow-on question: since the application will ultimately run in production as one or more containers, should local dev and testing move from Docker to Podman? Short answer: **the test architecture must not care — and with one configuration file, it doesn't.** Podman is a legitimate, even attractive, runtime for this project's *production* posture; it is not a reason to change anything in the testing design, and switching is best treated as an operations decision (north-star Phase 3), not a quality-initiative decision.

**Does Podman give programmatic lifecycle control?** Yes, fully. Podman exposes two REST APIs over a Unix socket: its native **libpod API** and a **Docker-compatible API** that speaks the same protocol the Docker daemon does. Anything that drives containers programmatically — Testcontainers (via docker-java), `docker`/`podman` CLI in scripts, `podman-py` for the Python worker's tooling, Go bindings — works against that socket. Create/start/stop/kill/remove, port mapping, log streaming, exec, health-check waiting: the full lifecycle surface used by Testcontainers is available. The architectural difference is that Podman is **daemonless** — the socket is served by a per-user `podman system service` (on Linux) or the `podman machine` VM (on macOS), and containers are ordinary child processes rather than children of a root daemon.

**Testcontainers + Podman — supported, with three known caveats.** Testcontainers officially supports Podman; it discovers the runtime through the standard environment. On this project's macOS dev machine:

```bash
# one-time: create and start the Podman VM
podman machine init && podman machine start

# point Testcontainers (and any Docker client) at the Podman socket
export DOCKER_HOST="unix://$(podman machine inspect --format '{{.ConnectionInfo.PodmanSocket.Path}}')"
```

or persistently in `~/.testcontainers.properties`:

```properties
docker.host=unix:///Users/<you>/.local/share/containers/podman/machine/<machine>/podman.sock
```

The caveats to know before adopting:

1. **Ryuk (the cleanup sidecar) needs configuration.** Under rootful Podman set `TESTCONTAINERS_RYUK_CONTAINER_PRIVILEGED=true`; under rootless setups where that is not possible, set `TESTCONTAINERS_RYUK_DISABLED=true` — and accept that crashed test JVMs can leave containers behind (mitigated by a `podman container prune` alias or the reuse-mode singleton, which deliberately persists anyway).
2. **Short image names resolve differently.** Podman does not assume Docker Hub; `postgres:18.3-alpine` resolves only if `registries.conf` lists `docker.io` in unqualified search (the default `podman machine` config does). The robust fix is cheap: use fully qualified names — `docker.io/library/postgres:18.3-alpine` — in the single image-tag constant §7 already recommends.
3. **Compose compatibility is good but not identical.** `podman compose` delegates to a compose provider and handles this project's `compose.yaml` profiles, but the dev stack (`make dev`, `start-sangita.sh`) is a separate migration from the test suite and should be validated independently — healthchecks, `depends_on` conditions, and volume semantics are where differences historically surface.

**Where Podman genuinely wins — production, not testing.** The north-star deployment target is "one VM with compose" or a managed container service. On a self-managed VM, Podman's strengths are real: **rootless containers** (a meaningful hardening step for a system whose corpus is the asset), no root daemon as a single point of compromise/failure, **first-class systemd integration via Quadlet** (`.container` unit files — the OS supervises restarts, boot ordering, and logging natively), `podman auto-update` for image refresh, and `podman generate kube`/`kube play` if the Kubernetes path ever opens. For the *production* phase, Podman + Quadlet is arguably a better fit than Docker + compose-as-init-system.

**Recommendation.**

- **Do not couple the testing initiative to a runtime switch.** Implement §3.1–§3.4 as written; verify it passes against Docker (zero config) — that is the critical path for closing N2/N3.
- **Keep the suite runtime-agnostic by construction**: fully qualified image names, no Docker-only API assumptions (Testcontainers already guarantees this), and the `TEST_DATABASE_URL` escape hatch, which sidesteps the runtime question entirely.
- **Trial Podman as a one-day spike, separately**: `podman machine` + the `DOCKER_HOST`/Ryuk settings above, run `./gradlew integrationTest` and `make dev`, record results in this document. If green, either runtime becomes a personal choice rather than a project dependency.
- **Revisit Podman seriously in north-star Phase 3** (production reality), where rootless + Quadlet + auto-update deliver actual value. CI is unaffected either way — GitHub Actions runners ship Docker, and Testcontainers uses it there regardless of what runs locally.

---

## 4. The Real-Life Scenario Suite

What "meaningful integration tests" concretely means here, ordered by business risk. Each scenario runs against real Postgres; none mock the database.

### 4.1 DAL layer (new module suite — closes the "0 DAL tests" finding)

| # | Scenario | What it proves |
|:---|:---|:---|
| D1 | Apply all 43 migrations to an empty container | Schema applies from scratch; any new migration that breaks ordering fails CI, not `make db-reset` |
| D2 | Every Exposed `Table` object round-trips insert/select against the migrated schema | Table definitions match migrations (column types, nullability, enum mappings) — the drift class of bug |
| D3 | UUID v7 generation + monotonic ordering on all 27 keyed tables | PG18 feature the whole keyspace depends on |
| D4 | Junction-table integrity: krithi with N ragas → N `krithi_ragas` rows; delete krithi → junction cascade | The exact regression class called out in project memory |
| D5 | Constraint violations surface as typed errors (duplicate slug, FK to missing composer) | Repos fail loudly, not with raw `PSQLException` leaking to routes |
| D6 | `AUDIT_LOG` write helper: every repo mutation path produces an audit row | Critical Rule #3, enforced by machine |

### 4.2 Service layer (extend the existing 9 classes toward the money paths)

| # | Scenario | What it proves |
|:---|:---|:---|
| S1 | **Ingestion → review → canon, end to end**: insert raw imported_krithi → quality scoring → auto-approval threshold → canonical krithi + sections + junction rows + audit trail | The three-zone flow — the system's reason to exist |
| S2 | Auto-approval boundary cases: score exactly at threshold, missing raga resolution, conflicting composer | The judgment calls that currently only curators catch |
| S3 | Entity resolution: same composer in 3 transliterations resolves to one canonical entity; ambiguous match goes to review, never auto-merges | Protects canon integrity from the AI layer |
| S4 | Duplicate/variant handling: re-import of an existing krithi creates a lyric variant, not a duplicate canonical record | TRACK-053/-094 regression class |
| S5 | Bulk import of a 50-krithi batch with 5 malformed rows: 45 succeed, 5 reported with row-level errors, nothing partially written | Transactionality of the highest-volume write path |
| S6 | Remediation/bulk-operation rollback: a bulk section update is fully revertible | Until versioned canon (N5) lands, this is the safety net |
| S7 | Concurrency: two curators approve the same imported krithi simultaneously → exactly one canonical record, one clear conflict response | Real-life multi-curator behavior; trivially testable with `runTest` + real DB |

### 4.3 API layer (Ktor `testApplication` + Testcontainers — currently only `ImportRoutesTest`)

| # | Scenario | What it proves |
|:---|:---|:---|
| A1 | AuthN/Z matrix over the 15 route files: anonymous / viewer / curator / admin against a representative endpoint of each role tier → expected 401/403/200 | The RBAC story actually holds at the HTTP boundary |
| A2 | Public read surface (`PublicKrithiRoutes`): list, detail, search return migrated-schema-true DTOs; ETag round-trip returns 304 | The future consumer surface + caching layer |
| A3 | Error envelope consistency: malformed JSON, validation failure, missing entity → consistent problem shape, no stack traces in body | North-star middleware completeness |
| A4 | Curator workflow over HTTP: GET review queue → POST approve → canonical record visible via public route → audit row exists | The S1 flow as the frontend actually drives it |
| A5 | OpenAPI conformance spot-checks (response shape vs spec) — grows into the N6 drift gate later | Contract stops being decorative |

### 4.4 E2E layer (Playwright vs `make dev` compose stack — un-defers TRACK-035)

Exactly three money paths, per the north star: **login → review → approve**, **bulk import happy path + one failure row**, **krithi edit with raga change reflected in detail view**. Run nightly and pre-release, not per-commit. The compose stack is the right substrate here (Option E's home); Playwright's `webServer` config can invoke `make dev` or `start-sangita.sh`.

### 4.5 Python worker (testcontainers-python)

| # | Scenario | What it proves |
|:---|:---|:---|
| W1 | Job claim/complete cycle against real `extraction_jobs` schema (migrated by applying the same SQL files) | The Kotlin↔Python queue contract |
| W2 | Extraction payload written by worker validates against `canonical-extraction-schema.json` *and* inserts cleanly via the Kotlin ingestion path (shared fixture file) | The cross-language schema seam — where TRACK-096 bugs lived |
| W3 | Gemini stubbed at HTTP (respx/WireMock container): malformed model output → job marked failed with diagnostics, no partial writes | The AI layer kept at arm's length |

---

## 5. Migration & Seed-Data Tooling — Re-evaluation

> Prompted by the Testcontainers decision: with provisioning solved, should the project stay on custom migration code (`tools/db-migrate` + the test-side `MigrationRunner`), or move to a standards-based engine? Requirement: **one engine usable from both the Kotlin and Python codebases.** **Outcome: this analysis was ratified as [ADR-013](../02-architecture/decisions/ADR-013-db-migration-with-flyway.md) (2026-06-12), adopting Flyway Community and amending Critical Rule #1 in `CLAUDE.md`.** The section is preserved as the decision's supporting analysis.

### 5.1 Current state — the custom approach has already forked

The project has migrated tooling twice (Rust `sqlx` CLI → archived; Python `db-migrate` → current), and the fork is visible in the database itself:

| Concern | Python `tools/db-migrate` (dev/prod path) | Kotlin `MigrationRunner` (test path) |
|:---|:---|:---|
| Tracking table | `schema_migrations` | `_sqlx_migrations` (Rust-era fossil) |
| Tracking key | filename + **checksum** | version number, no checksum |
| Marker handling | dbmate-style `-- migrate:up/down` | same markers, independently re-implemented with three fallback formats |
| Validation | checksum drift detection | none |
| Invocation | CLI (`make migrate`, `make db-reset`) | JVM, in-process |

So today, **two independent implementations of migration semantics must agree forever**, and they already disagree on tracking. Only 21 of the 43 migration files carry the up/down markers at all; the rest rely on each runner's fallback behavior. Every new capability (checksums in tests, repeatable scripts, status reporting) must be built twice. This duplication — not "custom vs standard" ideology — is the engineering case for consolidation. (The north-star evaluation §2.1 called custom tooling "a defensible call"; what has changed is that Testcontainers puts a JVM in every test context anyway, and the second implementation now demonstrably drifts.)

**Seed data** is similarly informal: five SQL files in `database/seed_data/` applied via `psql` loop in the Makefile (`make seed`), mostly idempotent via `ON CONFLICT DO NOTHING`, plus a Gradle `seedDatabase` Kotlin tool. There is no versioning, no checksum, and no machine-checked distinction between *reference data* (ragas, talas — effectively part of the schema), *environment data* (the admin user), and *dev sample data*.

### 5.2 Requirements for a replacement

1. **Plain SQL migrations, kept verbatim** — the 43 files are the asset; no DSL rewrite.
2. **Callable from Kotlin tests in-process** (the Testcontainers singleton must migrate its container) **and from Python/Make/CI via CLI** — one engine, many consumers.
3. **Checksum/drift validation** (the Python tool has it; the test path must stop lacking it).
4. **A story for reference/seed data**, not just schema.
5. **PostgreSQL 18 support**, no paid tier required for the above.

### 5.3 Candidates

**Flyway (Community Edition) ✅ recommended.** The JVM-native standard. SQL files named `V<version>__<description>.sql` — the existing `01__baseline-schema-and-types.sql` convention needs only a `V` prefix (a one-shot rename script). Checksums and `flyway validate` built in. Three consumers, one engine:
- *Kotlin tests*: `Flyway.configure().dataSource(container.jdbcUrl, …).locations("filesystem:database/migrations").load().migrate()` — `MigrationRunner` (157 lines) is replaced by ~5.
- *Make/dev/CI*: official `flyway/flyway` Docker image or CLI — `make migrate` becomes a `docker run`/CLI call; no JVM knowledge needed on the Python side.
- *Python worker tests*: invoke the same CLI/container against the testcontainers-python instance (subprocess) — same files, same engine, same tracking table.

The decisive extra: **repeatable migrations (`R__*.sql`)** — rerun automatically whenever their checksum changes — are precisely the right mechanism for reference data (§5.5). Caveats: Redgate has progressively gated features into paid tiers (undo/rollback scripts, drift reports, support for older DB engines) — verify current Community terms at adoption. Undo being paid costs nothing here: `db-migrate` exposes no down command either; `make db-reset` is and remains the rollback story.

**Liquibase (OSS) ⚠️ runner-up.** Apache-2.0 core, supports formatted-SQL changelogs, rollback included in OSS. More ceremony (changelog manifest, per-changeset headers) and a heavier mental model than this 1-person project needs. Choose it only if free rollback ever becomes a hard requirement.

**dbmate ⚠️ minimal-delta option.** Single Go binary, fully language-agnostic, and — notably — the project's `-- migrate:up/down` markers *are* dbmate's format (the Rust-era files were already written for it). Costs: no JVM API (Kotlin tests would shell out or run a dbmate container — clunkier than Flyway's 5 lines), **no checksum validation** (a regression vs the Python tool), no repeatable migrations (seed problem unsolved), and its `schema_migrations` table collides by name with the Python tool's incompatible one. Lowest switching cost, lowest ceiling.

**Atlas ⚠️ forward-looking.** Schema-as-code with versioned-migration support, excellent CI linting (`atlas migrate lint` catches destructive changes pre-merge). A genuinely modern choice, but it pulls toward a declarative paradigm the project doesn't need yet, and the best features increasingly sit behind Atlas Cloud. Revisit if schema-change velocity becomes a problem.

**Alembic ❌ rejected.** Python-native but SQLAlchemy-centric; the schema's center of gravity is the Kotlin/Exposed side, and there are no SQLAlchemy models to autogenerate from. It would deepen, not resolve, the cross-language split.

**Status quo, unified ⚠️ the honest baseline.** Delete `MigrationRunner` and have Kotlin tests shell out to `uv run db_migrate migrate` with env vars pointed at the container. Fixes the fork with minimal change — but makes Python+uv a hard dependency of every JVM test run, keeps 100% custom code, and still has no answer for seed data. If Critical Rule #1 is reaffirmed, do at least this.

| Criterion | Flyway CE | Liquibase | dbmate | Atlas | Status quo unified |
|:---|:---|:---|:---|:---|:---|
| SQL files kept verbatim | ✅ rename only | ⚠️ headers added | ✅ as-is | ⚠️ restructure | ✅ |
| In-process JVM API for Testcontainers | ✅ | ✅ | ❌ shell-out | ❌ shell-out | ❌ shell-out |
| CLI/Docker for Make + Python + CI | ✅ | ✅ | ✅ | ✅ | ✅ (custom) |
| Checksum / drift validation | ✅ | ✅ | ❌ | ✅ | ⚠️ Python side only |
| Repeatable migrations (seed story) | ✅ | ✅ | ❌ | ⚠️ | ❌ |
| Rollback in free tier | ❌ (unused today) | ✅ | ✅ | ⚠️ | ❌ |
| Custom code remaining | none | none | none | none | all of it |
| Switching effort | ~2 days | ~3–4 days | ~1 day | ~1 week | ~0.5 day |

### 5.4 Recommendation and migration path

**Adopt Flyway Community as the single migration engine**, contingent on amending Critical Rule #1. Path:

1. Scripted rename `NN__desc.sql` → `VNN__desc.sql`; strip the now-unused `-- migrate:down` sections (Flyway treats the whole file as "up").
2. New databases (every test container, `make db-reset`): Flyway applies all from scratch — nothing else needed.
3. Existing dev/prod databases: `flyway baseline -baselineVersion=43`, then migrate normally; drop `schema_migrations` and `_sqlx_migrations` after a verification window.
4. Replace `MigrationRunner` usage in `IntegrationTestBase`/`SangitaPostgres` with the Flyway API; archive `tools/db-migrate` next to the Rust CLI (`tools/db-migrate-archived/`).
5. Update Makefile (`migrate`, `migrate-status`, `db-reset` → Flyway CLI/Docker), `CLAUDE.md` Critical Rule #1, `tools/db-migrate/README.md` pointer, and the onboarding doc — per the project's documentation-sync rule.

### 5.5 Seed-data management — three tiers, made explicit

| Tier | Examples | Mechanism | Versioned? |
|:---|:---|:---|:---|
| **Reference data** (part of the schema in spirit) | ragas, talas, languages, deities, composer aliases, import-source authority | Flyway **repeatable migrations** (`R__seed_ragas.sql`, …), idempotent `ON CONFLICT` upserts (already written that way) — re-applied automatically when the file changes, checksummed, identical in dev/test/CI/prod | ✅ |
| **Environment data** | admin user + credentials | *Out* of migrations entirely (it is per-environment and security-sensitive — N1); provisioning script or first-run bootstrap | ❌ deliberately |
| **Dev sample data** | `02_sample_data.sql`, demo krithis | `make seed-dev` only; never in migrations, never in CI | ❌ |
| **Test fixtures** | per-scenario entities | Kotlin builders (§3.4), never SQL dumps | n/a |

This directly upgrades the test substrate: reference data arrives with the schema in every container (the §3.4 "seed once per JVM" step disappears), and the truncate-reset exclusion list becomes "Flyway's `flyway_schema_history` + reference tables".

### 5.6 Can Testcontainers for Java and Python coexist? — Yes, by design

The two libraries are independent clients of the same Docker (or Podman, §3.5) daemon; nothing is shared between them except the image cache — which is a benefit (`postgres:18.3-alpine` pulls once, serves both).

- **Isolation:** each test process gets its own session ID, container labels, and its own Ryuk reaper instance; a Gradle `integrationTest` run and a `pytest` run can execute *simultaneously* on one machine with zero interaction — each talks to its own Postgres container on its own mapped port.
- **Configuration:** both honor `DOCKER_HOST` and the `TESTCONTAINERS_*` environment variables; the Java library additionally reads `~/.testcontainers.properties`. Prefer env vars for settings that must apply to both (one place, both runtimes — including the Podman socket if §3.5's spike is adopted).
- **CI:** both run in the same GitHub Actions job sequentially or in parallel jobs; hosted runners need no extra setup for either.
- **The coexistence is the point:** scenario W2 (§4.5) — the same migration files, applied by the same Flyway engine, to a Kotlin-owned container and a Python-owned container, with a shared golden payload crossing the seam — is exactly how the cross-language schema contract gets machine-verified.

---

## 6. Implementation Plan

Sequenced to interleave with north-star Phase 0/1; each step lands independently.

**Step 1 — Substrate swap (1–2 days).** Add Testcontainers to the version catalog and both backend modules; introduce `TestDatabase`/`SangitaPostgres` with the `TEST_DATABASE_URL` escape hatch; point `IntegrationTestBase` at it; add the `integration` tag and `integrationTest` task; update Makefile and `modules/backend/CLAUDE.md` (removing the phantom `IntegrationTestEnv` conventions). **Exit criterion: `./gradlew check` is green on a machine with no Postgres on 5432.**

**Step 2 — Migration-engine consolidation (~2 days; decision ratified as [ADR-013](../02-architecture/decisions/ADR-013-db-migration-with-flyway.md)).** Execute §5.4: rename to Flyway convention, swap `MigrationRunner` for the Flyway API in the test substrate, convert Makefile targets, baseline existing databases, restructure seed data into the §5.5 tiers. Independent of Step 1 in content but cheapest done immediately after it, while the substrate is open.

**Step 3 — DAL suite ✅ (TRACK-111).** `dal/src/test` stood up with scenarios D1–D6 (11 tests). D1 (migrations-from-scratch) also satisfies north-star Phase 0 item 3's "apply all migrations to a scratch Postgres" CI check and verifies the Flyway cutover. Shared substrate extracted to `:modules:backend:test-support` (D11); D5 constraint violations surface as typed `DalException`s via a central mapper in `DatabaseFactory.dbQuery`.

**Step 4 — CI activation ✅ (TRACK-111).** GitHub Actions (`.github/workflows/ci.yml`): `backend-unit → backend-integration → migrations → frontend typecheck+build → worker pytest`. Testcontainers needs zero special configuration on hosted runners. Every test written in Steps 1–3 is a gate. One manual step remains: mark the checks **required** in `main`'s branch protection (D8) — the workflow defines them but cannot self-require.

**Step 5 — Money-path service & API scenarios (1–2 weeks, incremental).** S1–S7, A1–A5, prioritized in that order. Build the fixture builders here.

**Step 6 — Worker + E2E (1 week, after Step 4).** testcontainers-python for W1–W3 (applying migrations via the same Flyway engine — §5.6); Playwright for the three E2E paths, nightly schedule.

**Effort summary: ~4 weeks of part-time work, front-loaded so that the highest-leverage 20% (Steps 1–4) lands in roughly a week.**

---

## 7. Risks & Mitigations

| Risk | Mitigation |
|:---|:---|
| Docker unavailable in some future CI or dev environment | `TEST_DATABASE_URL` escape hatch preserves Option D behavior; Podman is a drop-in runtime via `DOCKER_HOST` (§3.5); Zonky (Option B) remains a documented fallback |
| Runtime switch to Podman breaks Testcontainers assumptions | Suite is runtime-agnostic by construction (§3.5): fully qualified image names, Ryuk config documented, validated by a standalone spike before any switch |
| Container startup slows the inner dev loop | Singleton per JVM + Testcontainers reuse mode locally; unit tests excluded from container cost via tags |
| Truncate-reset misses sequences/reference data | Reset already uses `session_replication_role`; extend exclusion list for reference tables and assert seed presence in `IntegrationTestBase` |
| Image drift between `compose.yaml` and tests | Single constant for the image tag, referenced in docs; CI check comparing the two is a 5-line script |
| pgvector adoption (TRACK-108) breaks the test image | Planned: swap constant to `pgvector/pgvector:pg18` in the same PR that adds the extension migration |
| Suite growth makes `check` slow | Per-commit budget: unit < 30 s, integration < 3 min; E2E nightly only; revisit with parallel JVM forks (each gets its own container — the design supports it) |
| Flyway Community terms tighten further (Redgate licensing) | Migrations stay plain SQL — the engine is swappable (Liquibase, dbmate, or even a revived custom runner can apply `V*__*.sql` files); checksums/history exportable from `flyway_schema_history` |
| Flyway baseline mis-applied to an existing database | Rehearse on a Testcontainers instance restored from a dev dump first; D1 (migrations-from-scratch) guards the fresh-DB path in CI |
| Repeatable seed migration accidentally mutates curated data | `R__` files restricted to reference tables; idempotent upserts only (`ON CONFLICT`), reviewed like schema changes; canonical tables never touched by seeds |

---

## 8. Decision Record

- **Adopt:** Testcontainers JVM (`postgresql`, `junit-jupiter`, BOM-managed) as the default database provider for all backend/DAL integration tests; `testcontainers-python` for worker DB tests (the two coexist freely — §5.6); Playwright + compose for E2E.
- **Adopt (ratified by [ADR-013](../02-architecture/decisions/ADR-013-db-migration-with-flyway.md), 2026-06-12):** Flyway Community as the single migration engine for Kotlin, Python, Make, and CI — retiring both `MigrationRunner` and `tools/db-migrate`; reference data moves to repeatable migrations, per the §5.5 seed tiers. Critical Rule #1 amended accordingly.
- **Retain:** truncate-based reset, deterministic-fixture convention, all existing test bodies, and `make db-reset` as the rollback story (no down-migrations — unused today, unneeded tomorrow).
- **Reject:** H2/in-memory emulation (semantic wall), Zonky embedded (pgvector + binary lag), compose-plugin orchestration for the integration layer (isolation), status quo as strategy (keeps N2/N3 open), Alembic (wrong center of gravity), and the continued existence of two parallel migration implementations with divergent tracking tables (§5.1).
- **Runtime stance:** Docker remains the default local runtime for this initiative; the suite is kept runtime-agnostic (fully qualified image names, `DOCKER_HOST` discovery, escape hatch) so Podman is a configuration change, not a redesign. Podman's production strengths (rootless, daemonless, systemd/Quadlet) make it the preferred candidate to evaluate in north-star Phase 3.
- **Revisit when:** pgvector lands (image swap), CI matures (consider service-container mode for speed via the escape hatch), canonical revisioning (N5, [ADR-014](../02-architecture/decisions/ADR-014-versioned-canon.md) — implemented by [TRACK-117](../../conductor/tracks/TRACK-117-versioned-canon-implementation.md)) adds bitemporal scenarios to §4, or the Phase 3 deployment work makes the Podman spike (§3.5) worth running.
