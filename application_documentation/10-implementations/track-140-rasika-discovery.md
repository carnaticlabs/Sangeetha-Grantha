| Metadata | Value |
|:---|:---|
| **Status** | In progress |
| **Version** | 0.3.0 |
| **Last Updated** | 2026-09-09 |
| **Author** | Sangeetha Grantha Team |

# TRACK-140 implementation evidence

---

The [accepted track](../../conductor/tracks/TRACK-140-rasika-discovery-experience.md) owns requirements and node status. The [execution guide](../09-ai/track-140-agentic-build-guide.md) defines the proof loop. This report records observed results, not release certification.

## P01 — readiness baseline, attempt 1

Baseline revision: `13cd5ccfa87b92203f0bff66688a7d16ce0ec1c3`. Existing uncommitted changes were TRACK-140 planning documents. Corrected `make test-frontend` to invoke the package's Vitest `test:unit` script.

| Check | Result |
|:---|:---|
| `make test` | PASS — Gradle BUILD SUCCESSFUL |
| `make test-integration` | PASS — Gradle BUILD SUCCESSFUL, isolated Testcontainers |
| `make test-mobile` | PASS — Gradle BUILD SUCCESSFUL |
| `make mobile-android` | PASS — assembleDebug BUILD SUCCESSFUL |
| `make test-frontend` | PASS — 10 files / 61 tests |
| Worker `uv run ruff check .` / `ruff format --check .` / `mypy .` / `pytest` | PASS — 78 formatted files; typecheck 77 source files; 407 tests |
| `make mobile-ios` with explicit iPhone 17 destination | FAIL — existing `SIGNING[@]: unbound variable` under macOS Bash; repair assigned to N01 |
| Local stack/public coverage | No running Compose services; localhost:8080 unavailable. Live counts remain unknown; integration gates own live coverage after compatible startup. |
| Devices | iPhone 17 / iOS 26.5 / `C0104896-CD64-4533-A919-1E5F22036561` available, shutdown. Android AVD Rasika_API34 configured; no running adb device. |

Initial Gradle/uv/Docker sandbox access failures were resolved with reviewed elevated tool access. No test was skipped or weakened. Java 25.0.2, Bun 1.4.0, uv 0.12.6 and Python 3.14.7 were observed. Baseline logs: `build/track-140/run-01/P01/gradle.log`, `frontend.log`, `ios.log`; worker output is also recorded in the task tool results. No real corpus was changed. Native build/runtime proof and existing-row musicological assessment remain distinct obligations.

## S01 — form state and producers, attempt 1

Additive Flyway `V59` (enum value) and `V60` (column default) plus domain/DAL/worker/curator defaults to `UNESTABLISHED`. Existing rows are not backfilled. Isolated proofs passed; the user stack was not migrated.

| Check | Result |
|:---|:---|
| `make test` | PASS |
| `make test-frontend` | PASS — 11 files / 66 tests |
| Worker ruff / mypy / pytest | PASS — 408 tests |
| User-stack migrate | Not run (M2 sequencing: wait for B01 isolated proofs) |

## B01 — V1 boundary and V2 core API, attempt 1

V1 catalogue and anonymous `/v1/krithis` exclude `UNESTABLISHED`; V2 includes it, serves discovery, and keeps `Cache-Control: no-store`. Frozen V1 decoder rejects the V2 unestablished fixture.

| Check | Result |
|:---|:---|
| `make test` | PASS |
| `make test-integration` | PASS — Testcontainers, no `db-reset` |

## D01 — mobile V2 client and safe storage, attempt 1

`KtorCatalogueApi` uses `/v2/catalogue` only, including discovery. `LocalDocumentStore` serializes writes and refuses to overwrite corrupt or future-version documents.

| Check | Result |
|:---|:---|
| `make test-mobile` | PASS |

## N01 — native harness, attempt 2

`tools/mobile/verify-rasika-journeys.sh` requires an explicit Android serial or iOS UDID and refuses generic destinations. Android `RasikaJourneyTest` and iOS `RasikaAppUITests` exist. CI compiles `assembleDebugAndroidTest`; hosted runners still do not boot devices.

| Check | Result |
|:---|:---|
| Generic destination | FAIL as required (exit 2) |
| `assembleDebug` / `assembleDebugAndroidTest` | PASS |
| Device journeys | Not run — no running adb device / simulator boot in this pass |

## U01 — shell, Home and appearance, attempt 1

Four tabs (Home / Explore / Library / Settings), Home search independent of discovery, ordered `RagaSequence` on cards, System/Light/Dark radio group with persistence rollback. Explore still uses the TRACK-138 search list; category filters and the source-faithful reader remain U02/U03.

| Check | Result |
|:---|:---|
| `make test-mobile` | PASS |
| Native appearance matrix | Deferred to device journeys (N01/GA) |

## U02 — Explore and entity pages, attempt 1

Krithis / Ragas / Composers under one explicit-submit query; draft/apply/cancel/reset filters; chip removal commits immediately; paging keeps loaded items on next-page failure; raga/composer destinations use detail DTOs and exact UUID works filters. Directory captions use parent melakarta numbers (M5). Home raga/composer shortcuts open Explore categories, not Browse.

| Check | Result |
|:---|:---|
| `make test-mobile` | PASS |
| Backend compile after optional `parentMelakartaNumber` on raga summaries | PASS |

## U03 — source-faithful reader, attempt 1

Variant swap keeps the previous reading labelled until the new lyrics succeed; Trinity hero removed; per-section raga labels require `sectionId`; UNESTABLISHED has no form badge; stored section labels win over enum fallbacks.

| Check | Result |
|:---|:---|
| `make test-mobile` | PASS |
| Device reader journeys | Not run — N01/GA |
