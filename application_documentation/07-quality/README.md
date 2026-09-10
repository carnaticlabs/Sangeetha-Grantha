| Metadata | Value |
|:---|:---|
| **Status** | Active |
| **Version** | 1.3.0 |
| **Last Updated** | 2026-09-10 |
| **Author** | Sangeetha Grantha Team |
| **Document Type** | Navigation |

# Quality and verification

---

Choose checks by the behavior changed. Compilation, isolated scenario tests, live-corpus audits, browser journeys, and native accessibility review answer different questions. Record their results separately.

## Run the matching checks

| Scope | Command | Requirements / evidence |
|:---|:---|:---|
| Backend fast unit tests | `./gradlew :modules:backend:api:unitTest` | No database; CI's fast unit lane |
| Backend complete tests | `make test` | API and DAL tests, including Testcontainers; Docker required |
| Backend integration | `make test-integration` | Tagged API/DAL scenarios against real PostgreSQL + Flyway |
| Admin components | `make test-frontend` | Vitest non-watch run |
| Admin types/build | Module `bun run typecheck` and `bun run build` | Separate checks; Vite build alone is not typechecking |
| Shared mobile | `make test-mobile` | Client/storage/presenter JVM tests |
| Android build | `make mobile-android` | Debug APK; CI also compiles instrumentation tests |
| iOS build | `make mobile-ios` | Simulator host build; not device acceptance |
| Worker | `uv run ruff check .`, `ruff format --check .`, `mypy .`, `pytest` | Worker directory; frozen dev dependencies; Docker for integration cases |
| Raga constraints | `make raga-lakshana-checks` | Migrated target database |
| Repository raga guard | `make mint-guard` | Static SQL-insert boundary check |
| Docs | `make check-docs` | Relative file-link validation |
| Agent configuration | `make agent-evals` | Deterministic repository-rule checks |

[CI](../../.github/workflows/ci.yml) is the executable gate definition. Dependency versions belong in [Current Versions](../00-meta/current-versions.md).

## Verify complete user/data journeys

- **Import:** source → extraction → review → correct composition/variants/sections → provenance/audit → intended public visibility.
- **Catalogue:** published-only visibility, V1/V2 form boundary, invalid query handling, empty results, reader/variant ownership.
- **Search:** missing profile, compatible/incompatible profile, current content hashes, lexical/vector behavior, audience visibility.
- **Curation:** reference identity collisions, unknown/ambiguous raga decisions, ordered raga junctions, accepted revisions.
- **Rasika:** submitted query, filters, entity pages, paging recovery, source switch, bookmark restart, large text, reduced motion.

Use [post-import acceptance](./qa/test-plan.md), [E2E testing](./qa/e2e-testing.md), and [mobile verification](../05-frontend/mobile/README.md).

## Read evidence correctly

A historical “PASS” is proof for the recorded revision/environment/date. It is not a live deployment assessment. A successful build does not prove a browser or native journey; a passing parser fixture does not prove every source in the corpus is correct.

Native runtime and TalkBack/VoiceOver acceptance remain open for TRACK-140. Some sourcing quality endpoints still return placeholders, so empty charts are not a zero-issue audit.

[Integration testing](./integration-tests-approach.md) describes the implemented substrate. [Reports](./reports/README.md) and [results](./results/README.md) preserve prior observations. [Implementation reports](../10-implementations/README.md) link features to dated evidence.

---

[Section index](./../README.md) · [Documentation home](./../README.md) · [Feature status](./../01-requirements/features/README.md)
