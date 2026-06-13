| Metadata | Value |
|:---|:---|
| **Status** | Ready to schedule |
| **Version** | 1.1.0 — TRACK-096 paused; versioned canon split into spike (ADR-014) + implementation |
| **Last Updated** | 2026-06-13 |
| **Author** | Prepared from decision checklist (Seshadri's responses) |
| **Decisions source** | [north-star-production-readiness-decision.md](./north-star-production-readiness-decision.md) (D1–D18, all answered) |
| **Analysis source** | [north-star-evaluation.md](./north-star-evaluation.md), [07-quality/integration-tests-approach.md](./07-quality/integration-tests-approach.md), [ADR-013](./02-architecture/decisions/ADR-013-db-migration-with-flyway.md) |
| **Epic** | [TRACK-109 Production Readiness Roadmap](../conductor/tracks/TRACK-109-production-readiness-roadmap.md) |

# North-Star Production Readiness — Implementation Plan

> This converts the 18 answered decisions into **nine new conductor tracks** plus the bookkeeping to existing tracks. Each track section below is written to be lifted directly into a `conductor/tracks/TRACK-XXX-*.md` file. Hand this back with "create the tracks" and I'll generate the files and update the registry.
>
> **v1.1 changes:** (1) TRACK-096 is paused alongside TRACK-093 (confirmed). (2) Versioned canon is split — **TRACK-116** is a design-only **architecture spike** producing **ADR-014**, and **TRACK-117** is the implementation; frontend tests move to **TRACK-118**.

## Decision → work traceability

| Decision | Outcome | Lands in |
|:---|:---|:---|
| D1 | Pause import; design versioned canon (spike→ADR-014); implement; re-import fresh (no retrofit) | TRACK-116 (spike) + TRACK-117 (impl) |
| D2 | Freeze migrations 1 day; Flyway rename first | TRACK-110 |
| D3 | argon2id + rehash-on-login | TRACK-114 |
| D4 | TRACK-110–113 under TRACK-109 epic | all |
| D5 | Revive TRACK-035; supersede TRACK-014 | TRACK-113 + bookkeeping |
| D6 | Defer N4 / N6 / public surface | scope guard (all) |
| D7 | Testcontainers everywhere | TRACK-111 |
| D8 | Blocking, PR-triggered CI | TRACK-111 |
| D9 | unit <30s, integ <3min, E2E nightly | TRACK-111/113 |
| D10 | `@Tag("integration")` + task | TRACK-110 |
| D11 | Duplicate test-support, extract at DAL suite | TRACK-110/111 |
| D12 | Three money paths confirmed | TRACK-113 |
| D13 | Frontend component tests separate | TRACK-118 |
| D14 | Flyway Docker image, version-pinned | TRACK-110 |
| D15 | 01(−admin)/03/04/05 → `R__`; admin → bootstrap; 02 → dev | TRACK-110 (+ admin from TRACK-114) |
| D16 | Drop legacy tracking tables after one `db-reset` | TRACK-110 |
| D17 | Podman deferred to Phase 3 | out of scope (noted) |
| D18 | Repo hygiene separate track, before CI | TRACK-115 |

---

## Master execution sequence

**Track number ≠ execution order** (conductor numbers by creation). Execute in this order; dependencies in parentheses.

```
WEEK 1  ── stop the bleeding, open the freeze ──────────────────────────────
  1. TRACK-114  Password hashing (argon2id)          [N1 blocker, ~1 day]
  2. TRACK-115  Repo hygiene + token rotation         [N8, ~0.5 day]
  3. ⏸ FREEZE   Checkpoint + PAUSE TRACK-093 AND TRACK-096; freeze new migrations (D2)
  4. TRACK-116  Versioned canon ARCHITECTURE SPIKE → ADR-014   [design-only; runs DURING freeze, no Flyway dep]
  5. TRACK-110  Flyway cutover + Testcontainers        [Steps 1–2; needs 114 for admin bootstrap]
                 └─ Flyway-rename sub-part is on the import critical path
WEEK 1–2 ── data model, while substrate test-wiring continues ──────────────
  6. TRACK-117  Versioned canon IMPLEMENTATION         [N5/D1; needs 116 ADR + 110 Flyway done]
                 └─ then db-reset + re-import Trinity fresh → resume TRACK-093/096
WEEK 2–3 ── verification layer ─────────────────────────────────────────────
  7. TRACK-111  DAL suite + CI activation              [Steps 3–4; needs 110, 115]
  8. TRACK-112  Money-path service & API scenarios     [Step 5; needs 110/111]
  9. TRACK-113  Worker + E2E (revive TRACK-035)        [Step 6; needs 110/111]
LATER / PARALLEL ───────────────────────────────────────────────────────────
 10. TRACK-118  Frontend component tests (Vitest)      [D13; needs 111 CI]
```

**Critical-path note (D1↔D2):** the import stays paused only until **TRACK-110's Flyway cutover** and **TRACK-117's versioned-canon migration** land — *not* the whole testing initiative. The **TRACK-116 spike is design-only and runs during the freeze** (it produces ADR-014, authors no migration), so it adds no critical-path time. The Testcontainers test-wiring, DAL suite, CI, and scenarios (111–113) run *after* the import resumes and never block it.

**Dependency graph:**
```
              116 (spike/ADR-014) ─┐
114 ─┬─▶ 110 ─┼────────────────────┴▶ 117 ─▶ (re-import, resume 093/096)
115 ─┘        ├─▶ 111 ─┬─▶ 112
              │        └─▶ 113
              └────────────▶ 118 (after 111)
```

---

## TRACK-114 — Authentication Hardening: Password Hashing (N1)

| Field | Value |
|:---|:---|
| **Status to set** | Not Started → first in queue |
| **Priority** | P0 — Blocker (north-star N1) |
| **Decisions** | D3 |
| **Depends on** | none |
| **Blocks** | TRACK-110 (admin-user bootstrap in seed tiering) |
| **Est.** | ~1 day |

**Goal:** replace the plaintext `hashPassword()` (`UserManagementService.kt:143`) with argon2id, with transparent rehash-on-next-login. Close the single most dangerous finding.

**Checklist:**
- [ ] Add an argon2id-capable hashing library to `gradle/libs.versions.toml` (Critical Rule #2 — no hardcoded versions); evaluate `password4j` (argon2id + bcrypt, simple API) vs `de.mkammerer:argon2-jvm`.
- [ ] Implement `hashPassword` / `verifyPassword` with argon2id (tuned params: memory, iterations, parallelism — document chosen cost factors).
- [ ] Rehash-on-login: on successful auth against a legacy/plaintext record, transparently re-store as argon2id.
- [ ] Detect legacy (unhashed) credentials safely during the transition (e.g., format/marker check).
- [ ] Add login throttling / basic lockout (north-star N1 also flags no rate limiting on login).
- [ ] Write the argon2id hash format the admin bootstrap (TRACK-110 / D15) will reuse — expose a small helper so the bootstrap script and login path share one implementation.
- [ ] Unit tests: hash≠plaintext, verify roundtrip, rehash path, wrong-password rejection.
- [ ] AUDIT_LOG entry on password change / rehash (Critical Rule #3).

**Acceptance:** no plaintext password at rest; existing curator can log in without a manual reset; throttling demonstrable; tests green.

**Docs to touch:** `ADR-004-authentication-strategy.md` (note argon2id), `current-versions.md` (new lib).

---

## TRACK-115 — Repository Hygiene & Secret Rotation (N8)

| Field | Value |
|:---|:---|
| **Status to set** | Not Started |
| **Priority** | P1 — do before CI (lowers noise floor) |
| **Decisions** | D18 |
| **Depends on** | none |
| **Blocks** | TRACK-111 (clean root → green first CI / secret scan) |
| **Est.** | ~0.5 day |

**Goal:** clean the repo root so the first CI run (and its secret/dependency scans) is green against a clean tree.

**Checklist:**
- [ ] Remove stray vendored trees at root: `org/jetbrains/...`, `Users/seshadri/...` (accidental IDE copies).
- [ ] Remove build/log artifacts at root: `backend.log`, `out.log`, `*_logs.txt`, `sangita_extraction_logs.txt`, `worker.log`.
- [ ] **Rotate** the `ADMIN_TOKEN` in tracked `tools/sangita-cli-archived/.env`; purge the value from the tracked file (rotate on principle even though the CLI is archived).
- [ ] Harden `.gitignore` (logs, `.env`, IDE trees, build outputs) so these can't recur.
- [ ] Consolidate the four agent-instruction files (AGENTS/CODEX/GEMINI/GOOSE.md) into pointers to CLAUDE.md (prevent silent drift) — or confirm out of scope and defer.
- [ ] `git log`-verify no other secrets are tracked (quick `gitleaks`/grep pass).

**Acceptance:** clean root; rotated token; hardened ignore; (optional) single agent-instruction source.

---

## TRACK-110 — Testcontainers Substrate + Flyway Cutover (Steps 1–2)

| Field | Value |
|:---|:---|
| **Status to set** | Not Started |
| **Priority** | P0 — foundation for the whole initiative |
| **Decisions** | D2, D7, D10, D11, D14, D15, D16 |
| **Depends on** | TRACK-114 (admin bootstrap hash) |
| **Blocks** | 111, 112, 113, 116 |
| **Est.** | ~3–4 days |

**Goal:** make integration tests self-provisioning via Testcontainers, and consolidate the two diverged migration runners onto Flyway (ADR-013). Two sub-parts; the **Flyway-cutover sub-part is on the import critical path** (the freeze can't lift until it lands).

**Sub-part A — Flyway cutover (critical path, during the D2 freeze):**
- [ ] Scripted rename `NN__desc.sql` → `VNN__desc.sql` for all 43 files; strip `-- migrate:down` sections (single content-free commit).
- [ ] Wire Flyway **Docker image** (`flyway/flyway`, version pinned in compose + `gradle/libs.versions.toml`) into `make migrate`, `make migrate-status`, `make db-reset` (D14).
- [ ] Seed tiering (D15): carve admin user out of `01_reference_data.sql`; convert `01`(−admin)/`03`/`04`/`05` → `R__seed_*.sql` repeatable migrations (idempotent `ON CONFLICT`); `02_sample_data.sql` → `make seed-dev` only.
- [ ] Admin-user bootstrap: env-driven first-run script producing an **argon2id** hash via the TRACK-114 helper.
- [ ] Baseline existing dev DBs: `flyway baseline -baselineVersion=43`; rehearse on a Testcontainers instance restored from a dev dump first.
- [ ] Drop legacy `schema_migrations` / `_sqlx_migrations` after **one verified `make db-reset`** cycle (D16).

**Sub-part B — Testcontainers test substrate (not on import critical path):**
- [ ] Add Testcontainers BOM + `postgresql` + `junit-jupiter` to `libs.versions.toml`; wire into `dal` and `api` test classpaths.
- [ ] `SangitaPostgres` singleton: `postgres:18.3-alpine` (fully-qualified image name for Podman-readiness, §3.5), migrate via **Flyway JVM API** (replaces the 157-line `MigrationRunner`).
- [ ] `TestDatabase` with `TEST_DATABASE_URL` escape hatch (default Testcontainers; external URL when set).
- [ ] Repoint `IntegrationTestBase` at the new substrate; keep truncate-after-each reset (extend exclusion to `flyway_schema_history` + reference tables).
- [ ] `@Tag("integration")` (D10) + `integrationTest` Gradle task; `make test` runs all via `check`, `make test-integration` for the tagged set.
- [ ] Test-support starts duplicated across api/dal (D11); extraction to `backend/test-support` deferred to TRACK-111.
- [ ] Delete `MigrationRunner`; archive `tools/db-migrate` → `tools/db-migrate-archived/`.
- [ ] Update `modules/backend/CLAUDE.md` (remove phantom `IntegrationTestEnv` conventions; document the real ones).

**Acceptance:** `./gradlew check` green on a machine with **no** Postgres on 5432 and only Docker present; `make db-reset` runs entirely through Flyway; reference data arrives via `R__`; legacy tables gone; freeze lifted.

**Docs to touch:** `04-database/migrations.md` (finalize Flyway sections), `getting-started.md`, `tech-stack.md`, `integration-tests-approach.md` (mark Steps 1–2 done).

---

## TRACK-116 — Versioned Canon: Architecture Spike + ADR-014 (N5)

| Field | Value |
|:---|:---|
| **Status to set** | Not Started → can start during the freeze |
| **Priority** | P1 — design gate for TRACK-117; unblocks the data-model decision |
| **Decisions** | D1 |
| **Depends on** | none (design-only; does **not** need Flyway done) |
| **Blocks** | TRACK-117 |
| **Est.** | ~2–3 days |
| **Type** | Architecture spike — produces a decision + design, **authors no migration** |

**Goal:** decide and document *how* the canonical record carries history and provenance, before any schema is written. Output is **ADR-014** plus a concrete schema design, so TRACK-117 is pure execution.

**Checklist:**
- [ ] Use `engineering:architecture` / `engineering:system-design` skills to evaluate the revisioning pattern: append-only `krithi_revisions` + current-state projection/view vs alternatives (temporal tables, audit-derived reconstruction). Recommend one.
- [ ] Design the provenance graph: `canonical_section.provenance → extraction_id → source_document_id → source_registry` as enforced FKs; define cardinality and null-handling.
- [ ] Define the revision-write contract: what creates a revision (every accepted change), how the import path populates it from row one, how the projection serves "current".
- [ ] Specify point-in-time read semantics ("what did this krithi say on date X") and the one-query provenance answer.
- [ ] Migration shape (for TRACK-117 to author in Flyway): table DDL sketch, FK additions, projection view, backfill-not-needed rationale (re-import covers it).
- [ ] Impact check: which routes/DTOs/repos change; whether the public read surface (deferred) is affected later.
- [ ] **Write ADR-014** (status Accepted), add to `adr-index.md` + decisions `README.md`; link from `integration-tests-approach.md` N5 references and TRACK-117.

**Acceptance:** ADR-014 merged with a chosen pattern, schema design, and migration shape; TRACK-117 can be executed without further design decisions.

**Pre-req coordination:** before the data-model work begins, **checkpoint-commit and pause TRACK-093 AND TRACK-096** (the D2 freeze covers both). The spike itself can proceed during the freeze in parallel with TRACK-110.

---

## TRACK-117 — Versioned Canon: Implementation + Re-Import (N5)

| Field | Value |
|:---|:---|
| **Status to set** | Not Started |
| **Priority** | P1 — time-sensitive (gates TRACK-093/096 resume) |
| **Decisions** | D1 |
| **Depends on** | TRACK-116 (ADR-014 + design), TRACK-110 (Flyway cutover — migration authored in `V`/Flyway format) |
| **Interacts with** | TRACK-093 (paused), TRACK-096 (paused) |
| **Est.** | ~1 week |
| **Scope note** | Phase-2 data-model item; *outside* the tests+CI+Flyway scope (D6) but sequenced here because the paused import makes it cheap now and expensive later. |

**Goal:** implement the ADR-014 design and re-import the Trinity corpus fresh, so history + provenance are captured from row one (no retrofit — enabled by your D1 "re-import from scratch" call).

**Checklist:**
- [ ] Author the schema as a **Flyway** migration (`V44__versioned_canon.sql` or per ADR-014): `krithi_revisions`, provenance FKs, current-state projection view.
- [ ] Wire the import/ingestion path to populate revision + provenance rows at creation (not backfilled).
- [ ] `make db-reset` → re-import Trinity krithis fresh; verify revision + provenance rows populate from the import path.
- [ ] Verify junction tables (`krithi_ragas`) and sections populate through the full stack (DB→API→UI) post-reimport.
- [ ] Add integration coverage (reuses the TRACK-110 substrate): a krithi edit creates a revision; provenance lineage resolves in one query.
- [ ] Resume / close TRACK-093 and TRACK-096 against the new schema.

**Acceptance:** every canonical krithi has a creation revision and queryable provenance lineage; "what did this krithi say on date X / which source produced this section" answerable in one query; Trinity import complete on the new model.

---

## TRACK-111 — DAL Test Suite + CI Activation (Steps 3–4)

| Field | Value |
|:---|:---|
| **Status to set** | Not Started |
| **Priority** | P0 — converts every test into a gate |
| **Decisions** | D7, D8, D9, D11 |
| **Depends on** | TRACK-110 (substrate), TRACK-115 (clean root) |
| **Blocks** | TRACK-117 |
| **Est.** | ~3–4 days |

**Goal:** stand up the missing DAL tests and turn on GitHub Actions so the suite gates merges.

**Checklist:**
- [ ] DAL suite (`dal/src/test`) — scenarios D1–D6 from the approach doc: migrations-from-scratch, table round-trips, UUID v7, junction integrity, constraint errors, AUDIT_LOG write paths.
- [ ] Extract shared test-support into `backend/test-support` now that two modules consume it (D11).
- [ ] GitHub Actions (`.github/workflows/`): `unit → integrationTest → frontend typecheck+build → worker pytest`; Testcontainers everywhere, no service-container (D7).
- [ ] Gating: **blocking, PR-triggered**, required status check on `main` from the first green run (D8).
- [ ] CI includes the migration apply-from-scratch check (also satisfies TRACK-109 Phase-0 item) and a Flyway `validate`.
- [ ] Enforce time budgets (D9): unit <30s, integration <3min per commit; fail loud if exceeded.

**Acceptance:** a red `integrationTest` blocks merge; DAL has real coverage; CI green end-to-end on a PR.

**Docs to touch:** TRACK-109 (mark W2 partially closed), `integration-tests-approach.md` (Steps 3–4 done).

---

## TRACK-112 — Money-Path Service & API Scenarios (Step 5)

| Field | Value |
|:---|:---|
| **Status to set** | Not Started |
| **Priority** | P1 |
| **Decisions** | D9 (per-PR budget shapes scenario count) |
| **Depends on** | TRACK-110, TRACK-111 |
| **Supersedes** | TRACK-014 (bulk-import QA) |
| **Est.** | 1–2 weeks, incremental |

**Goal:** cover the business-critical flows with real-DB integration tests, prioritized by risk.

**Checklist:**
- [ ] Service scenarios S1–S7: ingestion→review→canon end-to-end; auto-approval boundaries; entity resolution; duplicate/variant handling; bulk-import partial-failure transactionality; remediation rollback; concurrent curator approval.
- [ ] API scenarios A1–A5 (Ktor `testApplication` + Testcontainers): RBAC matrix across the 15 route files; public read + ETag 304; error-envelope consistency; curator workflow over HTTP; OpenAPI conformance spot-checks.
- [ ] Fixture builders (`aKrithi { … }`, `anImportedKrithi { … }`) with deterministic UUIDs/timestamps; golden payloads from the Trinity import as `src/test/resources/payloads/`.
- [ ] Mark TRACK-014 **Superseded by TRACK-112** with a pointer.

**Acceptance:** the three-zone flow and RBAC boundary are machine-verified; per-PR integration time within budget (heavy scenarios tagged for nightly if needed).

---

## TRACK-113 — Worker + E2E Layer (Step 6, revives TRACK-035)

| Field | Value |
|:---|:---|
| **Status to set** | Not Started |
| **Priority** | P1 |
| **Decisions** | D5, D12 |
| **Depends on** | TRACK-110, TRACK-111 |
| **Revives** | TRACK-035 (Playwright scaffolding) |
| **Est.** | ~1 week |

**Goal:** cross-language worker DB tests + the three E2E money paths, reusing the existing Playwright scaffolding but fitted to the Testcontainers/Flyway substrate.

**Checklist:**
- [ ] testcontainers-python for the worker: W1 job claim/complete cycle; W2 payload validates against `canonical-extraction-schema.json` **and** ingests via the Kotlin path (shared golden fixture — the cross-language seam); W3 Gemini stubbed, malformed output → job failed, no partial writes.
- [ ] Worker migrations applied via the **same Flyway engine** (CLI/container) — confirm Java+Python Testcontainers coexist (approach §5.6).
- [ ] Reactivate TRACK-035: change status Deferred → active; **re-scope its config to the new substrate** (point DB verification at the Testcontainers/Flyway DB, not a hand-started one) per your D5 note.
- [ ] E2E money paths (D12), nightly + pre-release: login→review→approve; bulk import (happy + one failure row); krithi edit with raga change reflected in detail view.
- [ ] Playwright `webServer` drives the compose stack (`make dev` / `start-sangita.sh`).

**Acceptance:** worker↔backend schema contract machine-verified; three E2E paths green nightly; TRACK-035 folded in, not duplicated.

---

## TRACK-118 — Frontend Component Tests (Vitest) — separate, after CI

| Field | Value |
|:---|:---|
| **Status to set** | Not Started (backlog) |
| **Priority** | P2 |
| **Decisions** | D13 |
| **Depends on** | TRACK-111 (CI must exist first) |
| **Est.** | ~1 week |

**Goal:** component tests on the most business-critical UI, kept off the backend critical path.

**Checklist:**
- [ ] Vitest + Testing Library setup in `sangita-admin-web`; wire into CI (frontend job).
- [ ] Tests for `CuratorReviewPage.tsx` (688 lines) and `BulkImport.tsx` (857 lines) — the curation workflow.
- [ ] Decompose the 600–850-line page components into testable units as coverage is added.

**Acceptance:** the two critical pages have meaningful component coverage; runs in CI within the frontend budget.

---

## Existing-track bookkeeping (registry updates when tracks are created)

| Track | Action | Reason |
|:---|:---|:---|
| TRACK-093 (In Progress) | **Checkpoint-commit, set Paused**, resume in TRACK-117 | D1 — pause for versioned canon + clean re-import |
| TRACK-096 (In Progress) | **Checkpoint-commit, set Paused**, resume with 093 | Confirmed — re-import needs a settled payload format |
| TRACK-035 (Deferred) | **Reactivate**, re-scope under TRACK-113 | D5 — revive scaffolding onto new substrate |
| TRACK-014 (Deferred) | **Superseded by TRACK-112** + pointer | D5 — overlaps S5/A scenarios |
| TRACK-109 (Epic) | Update W2 "Migrations" row (Flyway, not db-migrate); link 110–113 as W2/W3 children, 116/117 as N5 data-model | Keep the epic's gap table current |
| ADR-014 | **New** — authored by TRACK-116 (versioned canon) | Records the data-model decision |
| `tracks.md` registry | Add 110–118; bump version + date | New tracks |

## Explicitly out of scope (D6, D17 — record so it doesn't creep in)

- **N4** ops/backups/DR → TRACK-109 Milestone B (separate, later).
- **N6** OpenAPI generation / contract enforcement → deferred; *direction* undecided (north-star pairs it with Phase 1 — revisit then).
- **Public read surface** (N9) → strategic, later.
- **Podman** (D17) → north-star Phase 3; the suite stays runtime-agnostic so it's a config change, not a redesign.

## Next step

Reply **"create the tracks"** and I'll generate `conductor/tracks/TRACK-110…118-*.md` from these sections, update `conductor/tracks.md`, apply the existing-track status changes (pause TRACK-093 + TRACK-096, reactivate TRACK-035, supersede TRACK-014), scaffold the ADR-014 stub for TRACK-116, and run the registry-sync check (`conductor/check-registry-sync.py`). Nothing is committed until you ask.
