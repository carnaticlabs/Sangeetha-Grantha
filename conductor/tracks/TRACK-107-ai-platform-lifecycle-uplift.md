| Metadata | Value |
|:---|:---|
| **Status** | Not Started |
| **Version** | 1.0.0 |
| **Last Updated** | 2026-06-06 |
| **Author** | Principal Data & AI Engineering review (for Seshadri) |
| **Priority** | P0 (functional) — blocks safe resumption of TRACK-093 |

# TRACK-107: AI Platform Lifecycle Uplift

## Goal

Move the AI layer off **retired and deprecated** dependencies and onto a supported, cost-efficient, schema-true footing — so the extraction/enrichment pipeline keeps working and is ready to scale toward production. Covers four coupled changes (uplift findings F1–F5): SDK migration, model repoint, structured output, and Batch Mode. They share one blast radius (`gemini_enricher.py`), so they are one track.

## Context — why this is urgent

The enrichment path is built on two dependencies that are end-of-life:

1. **Deprecated SDK.** `tools/krithi-extract-enrich-worker/src/gemini_enricher.py:59-62` imports `google.generativeai` and uses the `genai.configure()` + `GenerativeModel()` pattern. That package was deprecated 30 Nov 2025; its Gemini-API migration deadline (31 Aug 2025) has passed. `pyproject.toml:27` pins `google-generativeai>=0.8.0`.
2. **Retired model.** `src/config.py:33-34` defaults `SG_GEMINI_MODEL` to `"gemini-2.0-flash"`, which reached retirement on **1 June 2026**.

Enrichment is currently gated off by default (`SG_ENABLE_GEMINI_ENRICHMENT` defaults False), which is the only reason this hasn't already broken production. The moment it's enabled — and TRACK-093's import quality benefits from it — it calls a dead endpoint via an unsupported SDK. Fix this **before** resuming the 1,245-krithi Trinity import.

A useful detail: `gemini_enricher.py:111` **already passes a `response_schema`**, so structured output is partially adopted — the migration preserves and formalises it rather than introducing it cold.

## Architecture / Approach

Target the **unified `google-genai` SDK** (client-first), repoint to **`gemini-3.5-flash`** (GA, no announced retirement — skips the 2.5 Flash → Oct-2026 re-migration), bind the existing `response_schema` to a **Pydantic model mirroring `CanonicalExtractionDto`**, and route bulk/backfill enrichment through the **Batch API** (50% cost) while keeping interactive calls synchronous.

```
config.py (model = gemini-3.5-flash, env-overridable)
        │
gemini_enricher.py
   client = genai.Client(api_key=...)             # was genai.configure + GenerativeModel
   client.models.generate_content(
       model=cfg.model,
       contents=prompt,
       config=GenerateContentConfig(
           response_schema=CanonicalExtractionModel  # Pydantic, schema-constrained decode
       ))
        │
   ┌────┴─────────────────────────────┐
   │ interactive  → synchronous        │  ("Generate Variants" UI)
   │ backfill     → Batch API (−50%)   │  (TRACK-093 import, re-extraction)
   └───────────────────────────────────┘
```

## Implementation Plan

### Phase 1 — SDK migration (F3)
- [ ] `pyproject.toml:27`: `google-generativeai>=0.8.0` → `google-genai` (latest); refresh `uv.lock`.
- [ ] `gemini_enricher.py:59-62`: replace implicit-config pattern with `from google import genai` → `genai.Client(api_key=...)` → `client.models.generate_content(...)`.
- [ ] Update provenance label strings in `tests/test_schema.py:190,200` and `tests/test_worker.py:210` (`provider="google-generativeai"`) — keep as historical label or rename; decide and document.
- [ ] Smoke test enrichment against a live key.

### Phase 2 — Model repoint (F1, F2)
- [ ] `config.py:34`: default `SG_GEMINI_MODEL` → `"gemini-3.5-flash"` (keep env override).
- [ ] `grep` the tree for any other `gemini-2.0-*` / `gemini-1.5-*` strings (validation path) and collapse onto the same config constant.
- [ ] **Gate:** run the Indic transliteration + raga-validation golden set; require pass rate ≥ current baseline before promoting.

### Phase 3 — Structured output hardening (F4)
- [ ] Define a Pydantic model mirroring `CanonicalExtractionDto`; pass as `response_schema` via the new SDK's `GenerateContentConfig`.
- [ ] Remove any post-hoc JSON-coercion/parsing now made redundant.
- [ ] Confirm near-zero malformed-payload rate into `imported_krithis.parsed_payload` (reinforces TRACK-096).

### Phase 4 — Batch Mode for bulk jobs (F5)
- [ ] Route import-pipeline/backfill enrichment through the Batch API; keep "Generate Variants" synchronous.
- [ ] Wire batch job submit/poll/collect into the worker; handle the 24h window and partial-failure retries.
- [ ] Measure: per-krithi enrichment cost halved vs synchronous.

### Phase 5 — Validate & document
- [ ] Full extraction E2E on a representative sample (incl. Trinity rows).
- [ ] Update `09-ai/gemini-selection-rationale.md` and `current-versions.md` (hand version sync to TRACK-106).

## Acceptance Criteria
- No references to `google-generativeai` or retired model strings remain (outside `.venv`).
- Enrichment runs on `google-genai` + `gemini-3.5-flash`; golden-set pass rate ≥ baseline.
- `response_schema` enforced via Pydantic; malformed-payload rate ≈ 0.
- Bulk enrichment runs via Batch at ~50% cost; interactive paths unaffected.
- Extraction E2E green.

## Risks
- **Transliteration regression** on edge scripts after model swap → mitigated by the Phase 2 golden-set gate (newer ≠ better on the niche).
- **SDK auth/client regressions** → migrate on a branch; the worker isolates the Gemini client.
- **Batch latency leaking into interactive UX** → strict separation of batch vs sync call sites.

## Dependencies
- Blocked by: TRACK-105 (clean tree).
- Blocks: TRACK-093 resume (do not import 1,245 krithis through a broken path), TRACK-108 (embeddings need the new SDK).
- Related: TRACK-096 (structured output reinforces canonical-DTO convergence), TRACK-065 (bundle the worker rename into Phase 1 if it doesn't widen blast radius).

## Progress Log
- 2026-06-06: Track created. Confirmed in code: deprecated SDK at `gemini_enricher.py:59-62`, retired model default at `config.py:34`, existing `response_schema` at `gemini_enricher.py:111`. Target = google-genai + gemini-3.5-flash + Pydantic schema + Batch.

Ref: application_documentation/sangeetha-grantha-state-of-nation-july-2026.md
