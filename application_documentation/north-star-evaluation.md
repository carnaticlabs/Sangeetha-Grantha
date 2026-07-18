| Metadata | Value |
|:---|:---|
| **Status** | Active |
| **Version** | 1.0.0 |
| **Last Updated** | 2026-06-11 |
| **Author** | Critical architecture review (for Seshadri) |
| **Companion docs** | `sangeetha-grantha-state-of-nation-july-2026.md`, `sangeetha-grantha-uplift-tasks.md`, `02-architecture/backend-system-design.md` |
| **Scope** | Critical evaluation of the current codebase + a north-star reference architecture for a system of this class, with a gap analysis and sequenced path |

# Sangeetha Grantha — North-Star Evaluation

> **The question this answers:** if you were building a *digital scholarly corpus with AI-assisted ingestion and human curation* today, from scratch, with no legacy — what would it look like, and how far is Sangeetha Grantha from that? The companion State-of-Nation doc covers dependency/AI-ecosystem drift; this doc deliberately goes one level up to **architecture, engineering practice, and product shape**. Findings are evidence-based (file paths cited) and severity-rated. The verdict is not "rewrite" — the bones are good — it is "harden the trust boundary, close the verification gap, and converge the data model toward a provenance-first canonical record."

---

## 1. Executive Summary

Sangeetha Grantha is, in class terms, a **curated knowledge base**: a system of record for Carnatic krithis, fed by an AI extraction pipeline, gated by human review, served through an admin console, with an aspirational consumer mobile surface. The right comparison points are not generic CRUD apps but systems like Wikidata, MusicBrainz, and digital-humanities corpora (Perseus, SARIT for Sanskrit texts) — systems whose core asset is **trustworthy, attributable, versioned data**, not code.

Measured against that bar:

**What is genuinely strong**

- The **domain model is the best part of the system**: 27 tables, UUID v7 keys, junction tables for multi-valued musicology (krithi_ragas), section-structure normalization at 100% consistency, and a deliberate separation of *imported/extracted* payloads from *canonical* records with a human review queue between them. That ingestion → review → canon flow is exactly the right shape for this class of system, and many professional teams never get it right.
- The **"Intelligence in Python, Ingestion in Kotlin, Review in Curator UI" split** is a sound architectural decision: the volatile, model-churning AI layer is quarantined in a replaceable worker; the typed backend owns durability and audit.
- **Documentation culture is far above typical** — 12 numbered doc sections, ADR-style decisions, a conductor track system with 86+ tracked work items, and honest self-review (the State-of-Nation doc critiques its own AI layer harder than most external audits would).
- Operational hygiene where it exists is real: migrations are ordered and tooled (43 migrations via `db-migrate`), Prometheus metrics and ETag caching are wired in ([Metrics.kt](../modules/backend/api/src/main/kotlin/com/sangita/grantha/backend/api/plugins/Metrics.kt), [Etag.kt](../modules/backend/api/src/main/kotlin/com/sangita/grantha/backend/api/support/Etag.kt)), and every mutation writes to AUDIT_LOG.

**Where it falls materially short of north star**

| # | Finding | Severity | Area |
|---|---------|----------|------|
| N1 | Passwords are stored **unhashed** — `hashPassword()` returns the input verbatim (`UserManagementService.kt:143`) | **Blocker** | Security |
| N2 | **No CI/CD whatsoever** — no `.github/`, no pipeline; "verify across all three layers before committing" is a human convention, not a gate | **Critical** | Engineering practice |
| N3 | **Verification gap**: 22 backend test files vs 0 DAL tests, 0 frontend tests (113 TS/TSX files untested), E2E track deferred (TRACK-035) | **Critical** | Quality |
| N4 | **No production deployment story**: compose `prod` profile still uses `postgres/postgres` literals; no managed-infra target, no backup/restore drill, no TLS story | **Critical** | Operations |
| N5 | The **canonical record has no version history** — edits overwrite; AUDIT_LOG records *that* a change happened, not a re-materializable *what*. For a scholarly corpus, this is a data-model gap, not a nicety | **Major** | Data architecture |
| N6 | **API surface drift**: OpenAPI spec is 661 lines against ~30 route files and 35+ services — the contract is decorative, not enforced | **Major** | API |
| N7 | **No API rate limiting / abuse controls** (rate limiting exists only client-side toward Gemini), no request-ID propagation, no tracing | **Major** | Security / Observability |
| N8 | **Repo hygiene**: stray vendored `org/jetbrains/...` and `Users/` directories at root, `backend.log`/`out.log`/`*_logs.txt` at root, an `ADMIN_TOKEN` in tracked `tools/sangita-cli-archived/.env`, four parallel agent-config files (AGENTS/CODEX/GEMINI/GOOSE.md) drifting | **Major** | Hygiene |
| N9 | **KMP mobile is aspirational** — 18 Kotlin files across both shared modules; the "multiplatform" in the project name is currently a build configuration, not a product | **Observation** | Product |
| N10 | The AI layer issues (retired models, deprecated SDK, prompt-coerced JSON) — covered fully in the State-of-Nation doc as F1–F6; inherited here by reference | **Critical** | AI |

**The one-paragraph verdict:** the system has a *publisher's* data model wrapped in a *prototype's* trust boundary. The north star for this application class is **"provenance-first corpus platform"** — every canonical fact traceable to a source, every change replayable, every pipeline output schema-constrained and eval-gated, all behind a hardened, boring, automated delivery system. The gap is closable incrementally; nothing requires a rewrite, and the single most dangerous item (N1) is a one-day fix.

---

## 2. Critical Evaluation of the Current Architecture

### 2.1 What the architecture gets right (and should be preserved)

1. **Three-zone data flow.** `imported_krithis` (raw + parsed payloads) → curator review → canonical krithi tables is a staging/curation/canon pipeline — the medallion pattern applied to scholarly data. The auto-approval service with quality scoring ([QualityScoringService.kt](../modules/backend/api/src/main/kotlin/com/sangita/grantha/backend/api/services/QualityScoringService.kt), [AutoApprovalService.kt](../modules/backend/api/src/main/kotlin/com/sangita/grantha/backend/api/services/AutoApprovalService.kt)) layered on top is the correct way to scale human review: score, auto-approve the unambiguous, queue the rest.
2. **Replaceable AI periphery.** Model churn (three Gemini generations in 15 months) hit the Python worker and config strings, not the domain model. That containment is the architecture working as designed.
3. **Monorepo with a single version catalog.** `gradle/libs.versions.toml` + Makefile + compose gives one-command onboarding (`make dev`). The decomposition done in TRACK-073–076 (DTOs/mappers/repos/services split) shows the backend is being actively gardened, not just grown.
4. **Audit-first mutation rule.** "All mutations write to AUDIT_LOG" as a stated invariant is rare discipline. (Its *limitation* is N5 — see below — but the instinct is right.)
5. **Custom migration tooling over Flyway** was a defensible call: full control over PostgreSQL 18 features (UUID v7), no JVM coupling for a DB-ops concern, and it's exercised in tests via `MigrationRunner.kt`.

### 2.2 Where it falls short — the detailed case

**N1 — The authentication layer is hollow.** [UserManagementService.kt:143](../modules/backend/api/src/main/kotlin/com/sangita/grantha/backend/api/services/UserManagementService.kt) stores the password as-is with a `// NOT SECURE` TODO. JWT issuance and role claims sit on top of credentials that are plaintext at rest. Everything else in the security story (JWT verification in [Security.kt](../modules/backend/api/src/main/kotlin/com/sangita/grantha/backend/api/plugins/Security.kt), role-based route guards) is undermined by this single function. There is also no login rate limiting, no lockout, no token revocation path. For a single-curator dev system this has been survivable; it must be fixed before any deployment that isn't localhost.

**N2/N3 — The verification gap is the biggest *systemic* risk.** The project's own CLAUDE.md instructs "verify changes compile/build across all three layers before committing" — which is an admission that no machine does this. Concretely:
- Backend: 22 test files concentrated in services and scraping parsers — genuinely good coverage of the extraction logic (the riskiest code). But the **DAL has zero tests**, meaning the Exposed table definitions, repository queries, and the 43-migration schema are validated only by integration tests that happen to touch them.
- Frontend: **zero tests** across 113 TS/TSX files, including an 857-line `BulkImport.tsx` and a 688-line `CuratorReviewPage.tsx` that encode the curation workflow — the most business-critical UI in the system.
- No CI means none of the existing tests gate anything. A broken `main` is discovered by the next human who runs `make test`.

The pattern this produces is visible in the conductor history itself: TRACK-079 remediated 92% section inconsistency, TRACK-093 needed import data cleanup, TRACK-096 is converging divergent payload formats. These are all *data regressions that tests and schema enforcement would have prevented upstream*. The cost of the missing verification layer is already being paid — it's just being paid in remediation tracks instead of CI minutes.

**N4 — There is no path to production.** The compose `prod` profile exists but ships `POSTGRES_PASSWORD: postgres`, no TLS termination, no secrets management, no backup automation, and the ops docs (`08-operations/deployment.md`, `monitoring.md`) describe intent more than runbook. The Google-cloud scaling docs in `02-architecture/` are forward-looking design, not provisioned reality. For a system of record, **the backup/restore drill is the most important missing operational artifact** — the corpus is the asset, and right now it lives in one Docker volume on one machine.

**N5 — Audit ≠ history.** AUDIT_LOG answers "who changed what, when" for compliance, but the canonical krithi tables hold only current state. A scholarly corpus needs *bitemporal* answers: "what did the canonical text of this krithi say on 2026-01-15, and which source/extraction/curator decision produced each section?" Today that's reconstructable only by forensic reading of audit rows, if at all. This also blocks future features the docs already gesture at: variant comparison over time, curator-decision analytics, public changelog per krithi (the MusicBrainz "edit history" page every serious reference site has).

**N6 — The API contract is unenforced.** 661 lines of OpenAPI cannot describe ~30 route files. Nothing generates the spec from the routes, nothing generates clients/types from the spec, and the frontend hand-maintains its own `types.ts` (plus a parallel `types/` directory — duplication already visible). The shared-domain KMP module was *supposed* to be the single source of DTO truth, but the frontend, being TypeScript, can't consume it — so the project currently has **three** parallel type systems (Kotlin DTOs, OpenAPI YAML, TS types) with no mechanism keeping them aligned.

**N7 — Observability is half-built.** Prometheus metrics: yes. Structured request logging: partially ([RequestLogging.kt](../modules/backend/api/src/main/kotlin/com/sangita/grantha/backend/api/plugins/RequestLogging.kt)). But: no trace/correlation IDs flowing curator-click → API → worker → Gemini call, which is exactly the path that fails in interesting ways; no error aggregation; no alerting; pipeline observability is log files (`sangita_extraction_logs.txt` sitting at repo root tells the story). For an async-pipeline system, **per-job lineage** (which source URL → which extraction → which import → which canonical record) is the observability that matters, and it's currently spread across DB rows and logs.

**N8 — Hygiene debt that erodes trust in the repo.** Stray `org/jetbrains/kotlin/...` Kotlin-Gradle source files and a `Users/seshadri/...` tree vendored at repo root (almost certainly an accidental IDE copy), build/log artifacts at root, an `ADMIN_TOKEN` value in the *tracked* `tools/sangita-cli-archived/.env`, and four AI-agent instruction files (AGENTS.md, CODEX.md, GEMINI.md, GOOSE.md) that will silently drift from CLAUDE.md. None of this breaks the build; all of it raises the noise floor for every future contributor, human or agent. The token should be rotated on principle even though the CLI is archived.

**N9 — The product surface is narrower than the architecture pretends.** The Gradle settings include shared KMP modules "for iOS & Android," but with 18 files of shared code and no app modules, the mobile story is a placeholder. Meanwhile the *actual* product today is admin-only: there is no public read surface at all — no consumer web, no public API, no export. For a compendium, the absence of any *reader* is the largest product-level gap; the corpus currently has curators but no audience.

### 2.3 A note on what is *correctly* absent

To be fair to the architecture, several fashionable things are rightly missing, and the north star below deliberately keeps them out: microservices (a modular monolith is correct at this scale and team size), Kafka/event buses (Postgres queues are fine), Kubernetes (one VM or one managed container service is enough for years), agent frameworks in the AI layer (bounded extraction with human review is the right posture for a system of record), and a separate vector database (pgvector covers it). Restraint is part of why this codebase is salvageable in-place.

---

## 3. The North Star — Reference Architecture for This Class of Application

The application class: **a provenance-first scholarly corpus platform** — AI-assisted ingestion, human curation, canonical publication. Here is what "best of breed, buildable by 1–3 people, 2026 toolchain" looks like.

### 3.1 Guiding principles

1. **The corpus is the product; the code is overhead.** Every architectural choice optimizes for durability, attributability, and reversibility of *data*. Losing code is recoverable; losing curation decisions is not.
2. **Provenance is a first-class column, not a log.** Every canonical fact carries its lineage (source → extraction → review decision) queryably.
3. **Machines verify; humans curate.** CI gates correctness so 100% of scarce human attention goes to musicological judgment, not regression hunting.
4. **One source of truth per concern, generated outward.** Schema → types → API contract → clients, derived, never hand-duplicated.
5. **The AI layer is a vendor, kept at arm's length.** Schema-constrained I/O, eval-gated model swaps, batch-priced bulk work, and a provider-agnostic seam.

### 3.2 The target architecture

```text
                       ┌─────────────────────────────────────────────┐
                       │                 PUBLIC SURFACE              │
                       │  Read-only API (cached, rate-limited)       │
                       │  Consumer web (SSR, SEO, search)            │
                       │  Bulk exports (JSON-LD / TEI dumps)         │
                       └──────────────────┬──────────────────────────┘
                                          │ reads canon only
┌───────────────┐   ┌─────────────────────▼─────────────────────────┐
│  INGESTION    │   │              CORE (modular monolith)          │
│  Python worker│──▶│  Staging zone   → Review/curation → Canon     │
│  (extraction, │   │  (immutable     (queue, scoring,  (versioned, │
│  enrichment,  │   │   payloads,      auto-approve)     provenance- │
│  embeddings)  │   │   schema-true)                     linked)     │
│  Batch-mode   │   │  AUDIT + EVENT LOG (append-only, replayable)  │
│  LLM calls    │   └─────────────────────┬─────────────────────────┘
└───────┬───────┘                         │
        │  evals gate every model change  │
┌───────▼───────┐   ┌─────────────────────▼─────────────────────────┐
│  EVAL HARNESS │   │  PostgreSQL 18: relational + pgvector + FTS   │
│  golden sets, │   │  (one database, one backup story, PITR)       │
│  regression   │   └───────────────────────────────────────────────┘
│  on transliteration, sections, ragas                              │
└───────────────────────────────────────────────────────────────────┘
        All of it behind: CI/CD (build+test+migrate-check on every PR),
        IaC-provisioned single deploy target, secrets manager, tracing.
```

### 3.3 North-star characteristics, by layer

**Data architecture — the heart of the difference**

- **Immutable staging.** Extraction payloads are write-once, schema-validated at the door (JSON Schema / Pydantic ↔ kotlinx-serialization, one schema artifact generating both — the repo already has `shared/domain/model/import/canonical-extraction-schema.json` pointing this direction).
- **Versioned canon.** Canonical entities are append-versioned (a `krithi_revisions` pattern: every accepted change creates a revision row; current view is a projection). This gives diff-able history, point-in-time reads, and safe bulk-operation rollback — the feature that turns "we ran a bad remediation" from a crisis into a `revert`.
- **Provenance graph.** `canonical_section.provenance → extraction_id → source_document_id → source_registry` as enforced foreign keys. The question "why does this line read *paramAtmA* and not *paramAthma*?" has a one-query answer.
- **Search as a data feature, not a service.** Postgres FTS for keyword, pgvector (HNSW, 768-dim, model-versioned embedding rows) for semantic, one `SearchService` fusing both. No external search infra.

**AI/ingestion layer**

- Unified SDK, current-generation model behind a config seam, **structured output everywhere** (no prompt-coerced JSON), **Batch API for everything without a human waiting**, and a **golden-set eval harness that gates every model/prompt change** — transliteration edge cases (La/Lla/Zha, Grantha-vs-Tamil), section detection, raga validation. The State-of-Nation doc already specifies this (F1–F6); the north star simply adds: *the eval harness runs in CI*, so a model bump is a PR like any other.
- **Cost and quality telemetry per pipeline run**: tokens, latency, schema-violation rate, auto-approval rate, curator-override rate. The curator-override rate is the single best quality signal an AI-curation system has; it should be a dashboard number, not an anecdote.

**Backend**

- Stays a Kotlin/Ktor modular monolith — but with the module boundary enforced: `dal` has its own test suite (Testcontainers against real Postgres), services are constructor-injected and unit-tested, routes are thin and **the OpenAPI spec is generated from code** (or code from spec — either direction, but mechanically). TypeScript client types are generated from that spec; the frontend never hand-writes a DTO again.
- AuthN/Z done boringly and completely: argon2id/bcrypt hashing, login throttling, short-lived access + refresh tokens, token revocation, audit on auth events. RBAC stays claims-based as today.
- Platform middleware completed: request IDs, OpenTelemetry traces spanning API → worker → LLM, server-side rate limiting, consistent problem-details error envelope.

**Frontend**

- Same React/Vite/Tailwind/TanStack stack — the choice is fine. The north-star deltas are: generated API types; component tests (Vitest + Testing Library) on the curation workflow; Playwright E2E on the three money paths (login → review → approve; bulk import; krithi edit); and decomposition of the 600–850-line page components into testable units.
- **A public, read-only consumer surface** — SSR for SEO (a corpus nobody can Google might as well not exist), search-first UX, per-krithi permalink with revision history. This is the largest *product* leap and what makes everything else worth it.

**Delivery & operations**

- CI on every PR: Gradle build+test, frontend typecheck+test+build, Python worker tests, migration apply-from-scratch check, OpenAPI-drift check, secret scan. CD to a single managed target (Cloud Run + Cloud SQL, or one VM with compose — either is fine; *provisioned by IaC and documented as a runbook* is the bar).
- **Nightly logical backups + PITR, with a quarterly restore drill.** For this system, this one bullet outranks every other ops item.
- Secrets in a manager (or at minimum SOPS-encrypted in repo), never literals in compose; tracked `.env` files purged and tokens rotated.

**Repository & collaboration**

- Root contains only what builds: stray vendored trees, logs, and OS junk gone; `.gitignore` hardened. One canonical agent-instruction file (CLAUDE.md) with the others reduced to pointers, so guidance can't fork. Conductor tracks stay — they're a strength — but get a CI-checked link between track ID and commit (the convention already exists; enforce it).

### 3.4 What the north star deliberately excludes

Microservices, Kubernetes, event buses, GraphQL federation, multi-region, a separate vector DB, agentic AI pipelines, and real-time collaboration. At <10⁶ records, <10 curators, and a read-mostly public surface, every one of these is negative-value complexity. The north star is **a very well-run monolith with a very trustworthy database**.

---

## 4. Gap Analysis — Current vs North Star

| Dimension | Current state | North star | Gap size |
|:---|:---|:---|:---|
| Domain/data model | Strong: staged ingestion, junction tables, audit log | + versioned canon, enforced provenance graph | **Medium** — additive migrations, no redesign |
| Security | JWT + RBAC shell over plaintext passwords; no server rate limiting; tracked token | Boring-complete auth; secrets manager; throttling | **Large but cheap** — days, not weeks |
| Quality/verification | Good service tests; 0 DAL, 0 frontend, 0 E2E; no CI | CI-gated full-stack tests + eval harness | **Large** — the biggest sustained investment |
| AI layer | Deprecated SDK, retired model strings, prompt-coerced JSON (per F1–F6) | Unified SDK, structured output, batch, evals-in-CI | **Medium** — already fully specified in uplift tasks U1–U5 |
| API contract | Hand-written OpenAPI, drifted; 3 parallel type systems | Generated spec + generated TS client | **Medium** |
| Observability | Metrics + request logs | + request IDs, traces, pipeline lineage, curator-override dashboard | **Medium** |
| Operations | `make dev` excellent; prod profile cosmetic; no backups | IaC'd single target, PITR + restore drills | **Large** — and the highest-stakes gap |
| Product surface | Admin console only; KMP mobile aspirational | + public read API/web with search & permalinks | **Large** — the strategic leap |
| Repo hygiene | Stray trees, logs, multi-agent-file drift | Clean root, one instruction source | **Small** — an afternoon |

## 5. Sequenced Path (what order, and why)

This deliberately interleaves with the open uplift tasks (U1–U5) rather than competing with them.

**Phase 0 — Stop the bleeding (days).**
1. Replace `hashPassword` with bcrypt/argon2id + a migration to rehash on next login; add login throttling. (N1)
2. Rotate and purge the archived `ADMIN_TOKEN`; remove stray `Users/`, `org/`, root logs; harden `.gitignore`. (N8)
3. Minimal CI: build backend, run existing tests, typecheck+build frontend, run worker tests, apply all 43 migrations to a scratch Postgres. Even with today's coverage, this converts every existing test into a gate. (N2)

**Phase 1 — Foundation under in-flight work (1–2 weeks, alongside U1–U3).**
4. Execute uplift U1–U3 (SDK migration, model repoint, structured output) — already specified; add the golden-set eval harness *into CI* while doing U2's eval anyway.
5. OpenAPI: pick generation direction, wire TS type generation, delete hand-written duplicates. (N6)
6. DAL test suite via Testcontainers; component tests for `CuratorReviewPage` and `BulkImport`. (N3)

**Phase 2 — The data-model uplift (2–3 weeks).**
7. Canonical revisioning (`krithi_revisions` + projection views) and enforced provenance FKs. Do this *before* the Trinity import completes at full scale — retrofitting history onto 1,245 imported krithis is much harder than recording it from day one. (N5)
8. Pipeline lineage + curator-override-rate dashboard. (N7)

**Phase 3 — Production reality (1–2 weeks).**
9. One IaC-provisioned deploy target, secrets manager, TLS, nightly backups + PITR, and one rehearsed restore. (N4)

**Phase 4 — The audience (ongoing).**
10. Public read-only API + SSR consumer web with FTS/semantic search (pgvector per F6), per-krithi permalinks with revision history. This is where the project stops being an admin tool over a database and becomes the compendium it is named for. (N9)

---

## 6. Closing Assessment

The honest one-liner: **Sangeetha Grantha has a north-star-quality data model and documentation culture attached to prototype-grade security, verification, and operations.** That is the *good* failure mode — the inverse (hardened pipelines around a muddled domain model) is far more expensive to fix. Every gap identified here is closable in-place, in sequence, without a rewrite, and the two highest-leverage moves are also the cheapest: hash the passwords this week, and stand up CI so the tests you already wrote start protecting you. After that, versioned canon + provenance is the investment that most differentiates this system in its class — it is the difference between a database of krithis and a *scholarly record* of them.
