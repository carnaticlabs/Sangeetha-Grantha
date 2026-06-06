| Metadata | Value |
|:---|:---|
| **Status** | Active |
| **Version** | 1.0.0 |
| **Last Updated** | 2026-06-06 |
| **Author** | Principal Data & AI Engineering review (for Seshadri) |
| **Companion doc** | `sangeetha-grantha-state-of-nation-july-2026.md` |
| **Scope** | Re-prioritised task list factoring open Conductor tracks against the uplift findings. **Gemma 4 deliberately excluded** per your call. |

# Sangeetha Grantha — Uplift Task List

> This takes the findings from the State-of-Nation review (F1–F8) and collides them with what's actually open in Conductor. The priority order in the companion doc assumed a clean slate; it isn't one. Once you factor in the in-flight Trinity import and the payload-convergence cleanup, the ordering tightens — because the AI breakage sits **directly underneath** the content work you've already started. The headline: **you cannot safely resume the Trinity import until the AI layer is fixed**, so that moves to the front regardless of how "infrastructural" it felt in the abstract.

---

## 1. Why the priority changed once tracks were factored in

The State-of-Nation doc ranked the AI-availability fixes (F1/F3) first on correctness grounds, but treated them as standalone platform hygiene. They are not standalone. The open tracks reveal the coupling:

- **TRACK-093 (Trinity Krithi Bulk Import, In Progress)** routes every krithi through the Python extraction worker, which calls Gemini via `gemini_enricher.py`. That file imports the **deprecated** `google.generativeai` SDK and the worker config defaults the model to **`gemini-2.0-flash` — a model that reached retirement on 1 June 2026**. So the single largest piece of in-flight *content* work is sitting on top of a dead/dying AI dependency. Finishing TRACK-093 before fixing F1/F3 risks importing 1,245 krithis through a degraded or failing enrichment path.
- **TRACK-096 (Payload Format Convergence, In Progress)** and **TRACK-099 (Compiler Warning Cleanup, registry: Not Started)** are two halves of the same cleanup, and they align cleanly with **F4 (structured output)**: both are about making `parsed_payload` a single, schema-true `CanonicalExtractionDto`. Do them together and the structured-output adoption reinforces the convergence rather than competing with it.
- **F5 (Batch Mode)** has a concrete, immediate home: the Trinity backfill itself. 1,245 krithis processed once, not latency-sensitive — the textbook batch job. Adopting Batch *for this import* is a real 50% saving, not a hypothetical.

So the re-hash is less "new work" and more "sequence the work you already have so the foundation is sound before you pour content on it."

---

## 2. Open tracks snapshot (the only ones that matter here)

| Track | Title | Status | Relevance to uplift |
|:---|:---|:---|:---|
| TRACK-093 | Trinity Krithi Bulk Import (1,245 krithis) | **In Progress** | **Blocked by AI fixes** (F1/F3); prime candidate for Batch Mode (F5) |
| TRACK-096 | Payload Format Convergence (deprecate `ScrapedKrithiMetadata`) | **In Progress** | Phase 4 cleanup pending; aligns with F4 |
| TRACK-099 | Backend Compiler Warning Cleanup (zero-warning target) | **Not Started** (registry) / file says "staged" — *discrepancy, see §6* | Coupled to TRACK-096 Phase 4 |
| TRACK-065 | Python Extraction Module Promotion & Rename | **Deferred** | Bundle the rename with the SDK migration (F3) — same module, one disturbance |
| TRACK-035 | Frontend E2E Testing (Playwright) | **Deferred** | Playwright pinned at 1.40.0 (old); revive alongside frontend patch bumps |
| TRACK-014 | Bulk Import Testing & QA | **Deferred** | Regression safety net for the re-enabled enrichment path |
| TRACK-042 | MCP Database Tooling Optimization | **Deferred** | Low priority; unrelated to uplift |
| TRACK-002 | Doc Header Standardizations | **Deferred** | Housekeeping; do opportunistically |

---

## 3. The task list (priority-ordered)

Tasks are tagged with the finding (`F#`) and track they serve. Effort is engineer-days for one person. "Blocked by" is hard sequencing.

### P0 — Unblock the pipeline (availability & correctness; do before any more content import)

**U1 — Migrate the extraction worker off the deprecated `google.generativeai` SDK → `google-genai`**
`F3` · serves TRACK-093, TRACK-065 · **~1 day** · blocks: U2, U3, TRACK-093 resume

- Concrete targets:
  - `tools/krithi-extract-enrich-worker/pyproject.toml:27` — replace `google-generativeai>=0.8.0` with `google-genai`.
  - `tools/krithi-extract-enrich-worker/src/gemini_enricher.py:59-62` — replace the `import google.generativeai as genai` / `genai.configure(api_key=...)` / `genai.GenerativeModel(model)` pattern with the client-first form: `from google import genai` → `client = genai.Client(api_key=...)` → `client.models.generate_content(model=..., contents=..., config=...)`.
  - `tests/test_schema.py:190,200` and `tests/test_worker.py:210` — the `provider="google-generativeai"` string literals; decide whether to keep as a provenance label or rename to `google-genai`.
- Why now: the package was deprecated 30 Nov 2025 with its Gemini-API migration deadline already passed; it does not receive the Batch/structured-output surface you need for U5/U3.
- Acceptance: worker builds and runs against the new SDK; enrichment smoke test passes against a live key; `response_schema` path (already present at `gemini_enricher.py:111`) still returns valid JSON.

**U2 — Repoint the model string off retired `gemini-2.0-flash` → `gemini-3.5-flash`, behind config**
`F1`,`F2` · serves TRACK-093 · **~1 day incl. eval** · blocked by: U1

- Concrete targets:
  - `tools/krithi-extract-enrich-worker/src/config.py:33-34` — change the `SG_GEMINI_MODEL` default from `"gemini-2.0-flash"` to `"gemini-3.5-flash"`. Keep it env-overridable (it already is — good).
  - Audit for any other hardcoded `gemini-1.5-*` / `gemini-2.0-*` strings used for the "musicological validation" path (F2) and collapse onto the same constant.
- Why 3.5 Flash, not 2.5: 2.5 Flash itself retires ~16 Oct 2026 — repointing to it buys ~4 months before a second migration. 3.5 Flash has no announced shutdown.
- **Gate:** before flipping production, re-run the Indic transliteration + raga-validation golden set (the La/Lla/Zha, Grantha-vs-Tamil cases). Newer ≠ automatically better on your niche.
- Acceptance: golden-set pass rate ≥ current baseline; no remaining references to retired model strings (`grep` clean outside `.venv`).

**U3 — Formalise structured output with a Pydantic `response_schema`**
`F4` · serves TRACK-096 · **~1 day** (reduced — partial adoption already exists) · blocked by: U1

- Concrete targets: `gemini_enricher.py` already passes a `response_schema` (line ~111) via the old SDK. Port it to the new SDK's `config=types.GenerateContentConfig(response_schema=...)` and bind it to a **Pydantic model that mirrors `CanonicalExtractionDto`** (you already run Pydantic 2.12). 
- Why now: this is nearly free given the existing partial adoption, and it directly serves TRACK-096 — a schema-constrained payload is a payload that *cannot* drift back into the `ScrapedKrithiMetadata` shape. It also stabilises output across the U2 model swap.
- Acceptance: malformed-JSON rate into `imported_krithis.parsed_payload` ≈ 0 on a re-run of the test corpus; schema validation enforced at decode time, not by post-hoc parsing.

### P1 — Finish in-flight work, now de-risked

**U4 — Resume and complete TRACK-093 (Trinity import, 1,245 krithis)**
serves TRACK-093 · **~2–3 days** · blocked by: U1, U2, U3

- Pick up the documented "Remaining Steps" (restart backend, delete stale Syama Sastri/Thyagaraja batches, re-upload CSVs through the Python path, verify clean lyrics, run `make test`).
- Do **not** start this before U1–U3 land; the whole point of the re-hash is to avoid importing 1,245 records through a broken enrichment path.
- Acceptance: all three composer batches imported; lyrics clean (no Word Division / pronunciation-guide leakage); unresolved ragas exported for follow-up; resolution success rates logged.

**U5 — Run the Trinity backfill through Batch Mode**
`F5` · serves TRACK-093 · **~1 day** (fold into U4) · blocked by: U1

- Route the import-pipeline enrichment calls (not the interactive "Generate Variants" path) through the Gemini **Batch API** — 50% cost on the same model. 1,245 krithis is exactly the asynchronous, parallel, non-interactive shape Batch is for.
- Acceptance: backfill completes via batch jobs within the 24h window; per-krithi enrichment cost halved vs synchronous; interactive paths untouched.

**U6 — Complete TRACK-096 Phase 4 + TRACK-099 together (DTO convergence + zero-warning build)**
`F4` · serves TRACK-096, TRACK-099 · **~2 days** · best done after U3

- These are coupled: TRACK-099's 50/53 warnings come from the deprecated `ScrapedKrithiMetadata`/`ScrapedSectionDto`/`IWebScraper` types that TRACK-096 Phase 4 removes. Do them as one unit:
  - Remove dead Kotlin scraper (`WebScrapingService.kt`, `DeterministicWebScraper.kt`, `IWebScraper` DI binding) — *after* confirming no active route callers.
  - Migrate active code (`LyricVariantPersistenceService.kt`, `StructuralVotingEngine.kt`, `StructuralVotingProcessor.kt`, `ImportService.kt`) `ScrapedSectionDto` → `CanonicalSectionDto`.
  - Fix the 3 code-quality warnings (`ImportService.kt:491,517`; `ImportRoutes.kt:186`).
  - Remove the `ScrapedKrithiMetadata` fallback from `LyricVariantPersistenceService` **only once** no legacy payloads remain in the DB (run a count first).
- Acceptance: `./gradlew … compileKotlin 2>&1 | grep "w:"` returns zero lines; `:modules:backend:api:test` green; single payload format in `parsed_payload`.

### P2 — New capability worth pulling forward

**U7 — Semantic search v1: `gemini-embedding-001` + `pgvector`**
`F6` · new track · **~1 week** · blocked by: U1 (new SDK)

- New `krithi_embedding` table (`krithi_id`, `section_id`, `vector`, `model_version`); HNSW index; embeddings beside relational data in your existing Postgres 18 — no new datastore.
- Start at **768 dims** (MRL truncation) to keep the index small; raise only if recall demands it. Backfill is an offline, batchable job (U5 pattern applies).
- New endpoint: "find similar krithis / search by meaning" over the catalogue.
- Acceptance: corpus embedded; similarity query returns musically sensible neighbours on a hand-checked sample; latency acceptable at catalogue scale.

### P3 — Housekeeping & reviving deferred tracks (any time; one PR each)

**U8 — Bundle the Python module rename (TRACK-065) into the U1 SDK migration**
serves TRACK-065 · **~0.5 day** · do *with* U1

- TRACK-065 is deferred as "cosmetic," but you're already opening `pyproject.toml`/imports for U1 — fold the rename in so the module is disturbed once, not twice. Skip if it widens the U1 blast radius.

**U9 — Backend minor bumps: Kotlin 2.4 / Ktor 3.5; evaluate Ktor OpenAPI generation**
`F7` · **~1 day**

- Roll Kotlin 2.3.0 → 2.4.0 and Ktor 3.4.0 → 3.5.0 in one housekeeping PR via `gradle/libs.versions.toml`. While there, evaluate Ktor's built-in OpenAPI generation against your hand-maintained `openapi/sangita-grantha.openapi.yaml`.
- Acceptance: build green, tests pass; `current-versions.md` + the three sync targets in `CLAUDE.md` updated.

**U10 — Frontend + Python patch roll-up; revive TRACK-035 (Playwright E2E)**
`F8` · serves TRACK-035 · **~1–2 days**

- Patch-bump React/Vite/Tailwind/TanStack; confirm no retired model strings in the `@google/genai` (frontend already on the new unified SDK). Upgrade Playwright off the stale 1.40.0 pin and un-defer TRACK-035 so the re-enabled enrichment path has UI regression coverage.

**U11 — Revive TRACK-014 (Bulk Import Testing & QA) as the regression net for U4/U6**
serves TRACK-014 · **~2 days**

- The import pipeline just changed under it (SDK swap, model swap, DTO convergence). Un-defer the testing track to lock in the new behaviour before it drifts.

**U12 — Opportunistic: TRACK-002 (doc headers), TRACK-042 (MCP DB tooling)**
- Low priority, no uplift dependency. Do when idle. Also action the data-integrity fix in §6.

---

## 4. Dependency graph

```
U1 (SDK migration) ──┬──> U2 (model repoint) ──┐
                     ├──> U3 (structured out) ──┼──> U4 (resume Trinity import) ──> U5 (batch backfill)
                     ├──> U5 (Batch Mode)        │
                     ├──> U7 (embeddings)        │
                     └──> U8 (module rename)     │
                                                 │
U3 ───────────────────────> U6 (convergence + zero-warning) [TRACK-096 P4 + TRACK-099]
                                                 │
U9 / U10 / U11 / U12 — independent, schedule around the above
```

U1 is the keystone: it unblocks the model swap, structured output, Batch, embeddings, and the rename. Do it first.

---

## 5. Suggested sequencing (two short sprints)

**Sprint 1 — "Make the foundation sound" (P0 + start P1):**
U1 → U2 → U3 (the AI keystone, ~3 days), then U6 (convergence + zero-warning, ~2 days). End state: pipeline runs on a supported SDK + live model, payloads are schema-true, build is warning-free. *Now* it's safe to import content.

**Sprint 2 — "Pour content + one new capability":**
U4 + U5 (Trinity import via Batch, ~3 days), then U7 (semantic search v1, ~1 week) if appetite allows. Slot U8/U9/U10/U11 housekeeping around them.

This ordering means the 1,245-krithi import (U4) only runs *after* the SDK, model, and schema are fixed — which is the whole reason the priority was re-hashed.

---

## 6. Data-integrity note (action required)

**TRACK-099 status is inconsistent.** The registry (`conductor/tracks.md`) lists it as **Not Started**, but the track file's own header says **Status: Completed** with a progress log entry *"2026-03-17: … Staged for commit."* "Staged but not committed" is the most likely reality — i.e. the work was done in a working tree that was never merged. Before starting U6, **verify whether that change actually landed** (`git log`/`git status` on the listed files); if it was lost, U6 absorbs it; if it merged, update the registry to Completed. Don't trust either source until you've checked the build for the 53 warnings yourself.

---

## 7. New Conductor tracks to register

Per your `09-ai/README.md` convention, register these before starting (each needs a `TRACK-XXX` row + detail file):

- **AI Platform Lifecycle Uplift** — covers U1–U3 + U5 (SDK migration, model repoint, structured output, Batch Mode). One track, since they share the `gemini_enricher.py` blast radius.
- **Semantic Search (Embeddings + pgvector)** — U7, its own track; this is a feature, not maintenance.
- Reuse existing tracks for U4 (TRACK-093), U6 (TRACK-096 + TRACK-099), U8 (TRACK-065), U10 (TRACK-035), U11 (TRACK-014).

Remember the project commit rule: every commit needs a `Ref: application_documentation/...` line, and version changes must sync `current-versions.md`, `02-architecture/tech-stack.md`, and `00-onboarding/getting-started.md`.

---

## 8. The one-line version

Fix the AI keystone (U1–U3) and clear the convergence/warning debt (U6) **before** you resume the Trinity import (U4) — because the import you've already started is quietly sitting on a retired model and a deprecated SDK. Everything else is cheap follow-on.

---

### References
- Companion: `application_documentation/sangeetha-grantha-state-of-nation-july-2026.md` (findings F1–F8)
- Code: `tools/krithi-extract-enrich-worker/src/config.py:33-34`, `src/gemini_enricher.py:59-62,111`, `pyproject.toml:27`
- Tracks: `conductor/tracks.md`; TRACK-093, TRACK-096, TRACK-099, TRACK-065, TRACK-035, TRACK-014
