| Metadata | Value |
|:---|:---|
| **Status** | Not Started |
| **Version** | 1.0.0 |
| **Last Updated** | 2026-07-19 |
| **Author** | Sangeetha Grantha Team |

# TRACK-128: Gemini Enricher Hardening — Typed Errors, SDK-Native Parsing, Batch Safety

**Goal:** Bring `src/gemini_enricher.py` in line with the google-genai 2.x SDK's intended usage and the project's structured-output mandate ("structured outputs mapped to Pydantic schemas, never free-text parsing"). The current code detects rate limits by substring matching on exception text, hand-parses fenced JSON despite requesting `response_schema`, and has silent-truncation and double-spend hazards in batch mode.

**Origin:** Python best-practices evaluation, 2026-07-19 (item 4, first block). Builds on TRACK-107 (structured output/batch) and TRACK-124 (SDK 2.x).

## Definition of Done

* Retry logic matches typed SDK exceptions (`google.genai.errors.APIError` and subclasses, checking `.code == 429`) — no `"429" in str(exc)` substring checks.
* `_parse_response` uses the SDK's `response.parsed` (already validated against `_GeminiSuggestion`) with the manual ```` ```json ```` fence-stripping retained only as an explicit, logged fallback.
* The dead `generation_config` parameter on `_GenaiClientWrapper.generate_content` is removed (or actually honoured) — no accepted-and-ignored arguments.
* `enrich_batch` zips results with `strict=True` and surfaces a count mismatch as a warning result, not silent truncation.
* Batch-failure fallback to N sequential sync calls is guarded (config flag or at minimum a loud log with item count) — no silent quota double-spend.
* `applied` is only `True` when `fields_updated` is non-empty; the `0.8` default confidence becomes a named constant.
* Arm's-length contract preserved: enrichment failure can never fail a task (W3 integration tests stay green unmodified in intent — they may need mechanical updates for the typed-error path).

## Inputs

* `@tools/krithi-extract-enrich-worker/src/gemini_enricher.py`
* `@tools/krithi-extract-enrich-worker/tests/test_gemini_enricher.py`
* `@tools/krithi-extract-enrich-worker/tests/integration/test_w3_gemini_arms_length.py` (the behavioural contract — respx at the HTTP layer, never mock SDK internals)
* google-genai 2.x error/typed-response docs

## Out of Scope

* Prompt content changes.
* New enrichment fields or model changes.
* Worker-side finalize flow (stays as-is).

## Steps

1. Add respx-based tests pinning current behaviour for: 429 retry, non-429 failure, malformed JSON, batch count mismatch (red where the bug exists).
2. Swap substring matching for typed exception handling; keep exponential backoff parameters unchanged.
3. Move to `response.parsed`; keep text-parsing fallback behind a log warning.
4. Fix `zip(..., strict=True)`, `applied` semantics, dead parameter, magic constant.
5. Guard the batch → sync fallback; document the quota implication in the module docstring.

## Deliverables

* Modified `src/gemini_enricher.py`.
* Extended `tests/test_gemini_enricher.py`; W3 integration tests updated only where the typed-error path changes observable warnings strings.

## Verification

* `uv run pytest tests/test_gemini_enricher.py tests/integration/test_w3_gemini_arms_length.py` green.
* Full `uv run pytest` green; `ruff` / `mypy` clean on the touched files.
