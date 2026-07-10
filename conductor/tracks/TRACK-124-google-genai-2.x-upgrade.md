| Metadata | Value |
|:---|:---|
| **Status** | Completed |
| **Version** | 2.0.0 |
| **Last Updated** | 2026-07-10 |
| **Author** | Sangeetha Grantha Team |

# Goal
Batch 3c — Upgrade the Gemini SDK `google-genai` `1.34.0` → `2.9.0` (major) in the Python extraction/enrichment worker. Likely breaking surface in the worker's LLM calls; review before raising the floor.

# Scope (`tools/krithi-extract-enrich-worker/pyproject.toml` + `uv.lock`)
- **google-genai** floor `>=1.0.0` (resolved `1.34.0`) → `>=2.0.0` (resolved `2.9.0`).

# Affected code
- Worker LLM integration (Intelligence-in-Python layer) — structured output / batch calls added in TRACK-107.

# Implementation Plan
1. Review the google-genai 1.x → 2.x changelog for breaking changes (client init, request/response shapes, structured-output API).
2. Bump the floor in `pyproject.toml`; `uv lock`.
3. Update worker LLM call sites and Pydantic response models as needed.
4. Run worker pytest; smoke-test an extraction+enrichment pass against a sample source.
5. Sync `current-versions.md` (Python tools section).
6. Commit per commit-policy.

# Risks
- Major SDK jump (1.x → 2.x) — API changes expected; isolated to the Python worker.
- Pairs conceptually with TRACK-107 (AI Platform Lifecycle Uplift) — review that track's structured-output/batch usage.

# Completion (2026-07-10)
Per the upstream changelog, google-genai 2.0's breaking changes are **Interactions-API-only**
("GenerateContent usage is unaffected") — the worker's surface (`genai.Client(api_key=)`,
`models.generate_content` + `GenerateContentConfig(response_mime_type, response_schema)`,
`response.text`, `batches.create`) needed no code changes. Floor raised to `>=2.0.0`, resolved
`2.9.0`. Verified: all 144 worker tests green, including the TRACK-113 W3 integration tests that
drive the real 2.x SDK through respx-stubbed HTTP (client init → request → response parsing).
Live-key smoke test not run — no `SG_GEMINI_API_KEY` available in this environment; the
HTTP-stubbed round-trip is the deepest offline verification.
