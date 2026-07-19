---
name: python-extraction-worker
description: Python conventions for the krithi extraction/enrichment worker. Use when editing tools/krithi-extract-enrich-worker — PDF/HTML/OCR extraction, section parsing, transliteration, Gemini enrichment, worker tests — or managing its uv/pyproject dependencies.
---

# Python Extraction Worker (tools/krithi-extract-enrich-worker/)

Architecture split: **Intelligence in Python, Ingestion in Kotlin, Review in Curator UI** — this worker extracts and enriches; it does not own canonical writes. Versions: [current-versions.md](../../../application_documentation/00-meta/current-versions.md). Krithi section semantics: the `Sangeetha-Krithi-Analyser` skill and [Domain Model §6](../../../application_documentation/01-requirements/domain-model.md#6-musicological-correctness-rules-lakshana).

## Paradigm: classes + Pydantic, no loose dicts

- All data crossing a function boundary is a `pydantic.BaseModel` (schemas live in `src/schema.py`) — typed fields, defaults, validators. Passing raw dicts around or procedural top-level scripts is rejected; encapsulate logic in classes with narrow responsibilities.
- Gemini calls (`gemini_enricher.py`, google-genai 2.x SDK) use structured outputs mapped to Pydantic schemas, never free-text parsing of model replies.
- Failures isolate per item: one unparseable PDF, page, or krithi is caught, logged with enough context to locate the source, and recorded as a failed unit — it must never abort the batch or silently drop the items that parsed cleanly. A batch result reports what succeeded *and* what failed; a partial run that looks like a clean run is the worst outcome.

## Key modules (src/)

- `structure_parser.py` — section classification heuristics; must honour transliteration collapses (mirrored in Kotlin's `TransliterationCollapse`).
- `velthuis_decoder.py` — Velthuis font mapping for Sanskrit; `diacritic_normalizer.py` / `normalizer.py` — diacritic and script normalization; `transliterator.py` — script conversion.
- `extractor.py` / `html_extractor.py` / `ocr_fallback.py` / `page_segmenter.py` — PDF/HTML/OCR extraction pipeline; `metadata_parser.py`, `identity_candidates.py` — metadata and entity resolution; `worker.py` / `cli.py` — entry points; `db.py` — psycopg access.

## Environment & dependencies

- The worker requires **Python 3.14+** (`requires-python = ">=3.14"` in `pyproject.toml`; mypy pinned to `python_version = "3.14"`), and `.mise.toml` pins `python = "3.14"` to match. Keep those three in step — a mise pin below `requires-python` makes `uv sync` fail on a clean machine.
- A mise-managed `.venv` is created automatically at the repo root; dependencies managed with **uv**: declare in `pyproject.toml`, lock with `uv.lock`, install via `uv sync`.

## Testing & quality (run from the tool directory)

- `pytest` — unit + integration. Integration tests use **testcontainers** (real Postgres, Flyway-migrated); Gemini/HTTP is stubbed with **respx** at the httpx layer — never mock the SDK's internals.
- `ruff check . --fix` and `ruff format .` before committing; `mypy .` must pass.
