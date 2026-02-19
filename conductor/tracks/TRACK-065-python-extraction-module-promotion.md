| Metadata | Value |
|:---|:---|
| **Status** | Active |
| **Version** | 1.2.0 |
| **Last Updated** | 2026-02-19 |
| **Author** | Sangeetha Grantha Team |

# TRACK-065: Python Extraction Module First-Class Promotion & Naming
**ID:** TRACK-065
**Status:** Active
**Owner:** Platform / Extraction Team
**Created:** 2026-02-19
**Updated:** 2026-02-19

## Goal
Promote the Python extraction worker to a first-class module in the repository:
1. Pin Python in root toolchain management (`.mise.toml`).
2. Define a stable, meaningful module name and folder path that reflects current scope (PDF + HTML + OCR + enrichment), not just PDF.
3. Plan a low-risk migration path for code and documentation references.

## Context
- The extraction worker currently lives at `tools/krithi-extract-enrich-worker/`.
- The service now handles more than PDF:
  - `tools/krithi-extract-enrich-worker/src/html_extractor.py`
  - `tools/krithi-extract-enrich-worker/src/worker.py`
  - `tools/krithi-extract-enrich-worker/src/gemini_enricher.py`
- Tooling baseline is Kotlin/Rust/Bun-first in `.mise.toml`; Python was not explicitly pinned before this track.
- Related prior work: [TRACK-064](./TRACK-064-unified-extraction-engine-migration.md).

## Current-State Analysis
### A. First-class readiness (today)
- Packaging and lock are present:
  - `tools/krithi-extract-enrich-worker/pyproject.toml`
  - `tools/krithi-extract-enrich-worker/uv.lock`
- Runtime integration already exists:
  - `compose.yaml` service `krithi-extract-enrich-worker`
  - `tools/sangita-cli/src/commands/extraction.rs`
  - `tools/sangita-cli/src/services.rs`
- Python version requirement is consistent at 3.11 baseline:
  - `requires-python = ">=3.11"` in `tools/krithi-extract-enrich-worker/pyproject.toml`
  - Ruff/Mypy target `3.11` in the same file
  - Docker base image references Python 3.11 in project docs

### B. Naming mismatch
- Folder name implies a PDF-only scope, but the worker executes a broader extraction pipeline.
- This increases cognitive mismatch for contributors and operators.

### C. Rename blast radius
- Legacy extractor path/service references found in repository scan: 192.
- Split:
  - Runtime/tooling code + config references: ~22 (compose, Rust CLI, gitignore, Python package metadata).
  - Documentation and track references: ~170.
- Conclusion: technical rename is manageable; documentation churn is the dominant effort/risk.

## Naming Options
| Option | Candidate | Pros | Cons |
|:---|:---|:---|:---|
| O1 | Keep legacy PDF-scoped naming | Zero migration cost | Scope mismatch with current extraction + enrichment behavior |
| O2 | `tools/krithi-extract-enrich-worker` | Scope-accurate and domain-specific; captures extraction + enrichment responsibility | Requires coordinated path updates |
| O3 | `modules/extraction-engine-python` | Strong "first-class module" signal | Diverges from current `modules/` language grouping and increases migration scope |

## Recommendation
Adopt **O2: `tools/krithi-extract-enrich-worker`** in a staged migration:
1. Baseline first-class tooling with Python pinning at root (completed in this track).
2. Rename directory and update runtime references in one atomic change.
3. Sweep and normalize documentation references in a follow-up doc-focused commit.

This balances semantic clarity with delivery risk.

## Implementation Plan
- [x] Add Python runtime pin to `.mise.toml` (`python = "3.11"`).
- [x] Finalize target rename (`tools/krithi-extract-enrich-worker`) with team approval.
- [x] Rename directory and update:
  - `compose.yaml` build context
  - Rust CLI extraction commands and service wrappers
  - Runtime extraction identifiers (`docker compose` service/container names, extractor version tag)
  - `.gitignore` path entries
- [x] Add a temporary compatibility note in operator docs (if scripts still reference old path).
- [x] Update documentation references in `application_documentation/` and `conductor/`.
- [x] Run startup validation:
  - stack boot via `./start-sangita.sh`
  - Docker health/log verification for postgres + extraction worker
- [ ] Run extraction regression validation:
  - extraction service build/start/stop via `sangita-cli`
  - core extraction E2E scenario (`test extraction-e2e`)

## Acceptance Criteria
- Python version is managed at repo root by mise.
- Extraction worker path/name reflects actual scope (not PDF-only).
- Extraction lifecycle commands continue to work (`build`, `start`, `status`, `stop`).
- No broken links or stale critical references in active operations docs.

## Progress Log
- **2026-02-19**: Track created. Completed analysis for first-class module promotion, quantified rename impact, and pinned Python 3.11 in `.mise.toml`.
- **2026-02-19**: Phase 2 execution started and validated:
  - Renamed the legacy extraction folder to `tools/krithi-extract-enrich-worker`.
  - Updated runtime wiring (`compose.yaml`, `start-sangita.sh`, `sangita-cli` extraction/service commands, integration helper script).
  - Started full stack via `./start-sangita.sh` and confirmed Docker processes:
    - `sangita_postgres` healthy
    - `sangita_krithi_extract_enrich_worker` healthy (`python -m src.worker`)
  - Verified container logs:
    - worker logs show startup + DB connection
    - postgres logs show database accepting connections.
- **2026-02-19**: Phase 3 documentation refresh completed:
  - Updated all markdown references to the new worker path/service/container naming.
  - Refreshed documentation metadata headers (`Version`/`Last Updated`) for touched files.
  - Normalized retrospective/header formatting in updated documents for consistency.
