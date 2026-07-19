| Metadata | Value |
|:---|:---|
| **Status** | Not Started |
| **Version** | 1.0.0 |
| **Last Updated** | 2026-07-19 |
| **Author** | Sangeetha Grantha Team |

# TRACK-131: Worker Config → pydantic-settings + Reproducible Docker Build

**Goal:** Replace the hand-rolled env parsing in `src/config.py` with `pydantic-settings` (typed, validated, self-documenting — and consistent with the worker's "Pydantic at every boundary" paradigm), resolve the advertised-but-unused concurrency knobs, and make the Docker build reproducible by actually using `uv.lock`.

**Origin:** Python best-practices evaluation, 2026-07-19 (items 3 and 6).

## Definition of Done

**Config**
* `ExtractorConfig` is a `pydantic_settings.BaseSettings` subclass: same env var names, same defaults, but malformed values (e.g. `SG_IDENTITY_MIN_SCORE=abc`) fail at load with a clear validation error naming the variable; numeric fields carry range constraints.
* `extractor_version` is computed at instantiation (currently a class attribute frozen at import time — env changes after import are silently ignored).
* Decision made and implemented for `EXTRACTION_BATCH_SIZE` / `EXTRACTION_MAX_CONCURRENT`: either (a) implement concurrent task processing (`ThreadPoolExecutor` over `claim_pending_task()`, safe with `FOR UPDATE SKIP LOCKED`), or (b) delete the knobs from config, worker docstring, and compose/env docs. **Default recommendation: (b)** — delete now, implement as its own track when queue depth demands it.
* `ExtractionQueueDB.conn`'s silent-reconnect property becomes an explicit `ensure_connected()` (reconnects observable in logs).

**Packaging & hygiene**
* Dockerfile installs from the lockfile: `COPY pyproject.toml uv.lock` → `uv sync --frozen --no-dev` (multi-stage acceptable); no more `pip install -e .` against a source-less layer.
* `cache/` added to the tool's `.gitignore` (currently one `git add .` away from committing scraped source pages).
* `requires-python = ">=3.14"` reconciled with docs: `.agents/skills/python-extraction-worker/SKILL.md` says "Python 3.11+" — update the skill; drop now-redundant `from __future__ import annotations` and `typing.Optional` imports across `src/` while touching files.

## Inputs

* `@tools/krithi-extract-enrich-worker/src/config.py`
* `@tools/krithi-extract-enrich-worker/src/worker.py` (docstring advertises the unused knobs)
* `@tools/krithi-extract-enrich-worker/src/db.py`
* `@tools/krithi-extract-enrich-worker/Dockerfile`
* `@tools/krithi-extract-enrich-worker/pyproject.toml` + `uv.lock`
* `@.agents/skills/python-extraction-worker/SKILL.md`
* `compose.yaml` (extraction service env wiring)

## Out of Scope

* Implementing worker concurrency (unless option (a) is chosen — then this track re-scopes and the Dockerfile work splits out).
* Base-image bump or slimming beyond the lockfile change.
* `current-versions.md` sync beyond what the dependency addition requires.

## Steps

1. Add `pydantic-settings` to dependencies; port `ExtractorConfig` field-for-field with identical env names/defaults; add range validators.
2. Fix `extractor_version` to instantiation-time; keep the `krithi-extract-enrich-worker:` prefix format.
3. Delete (or implement) the concurrency knobs per the decision above; update worker docstring and compose env docs.
4. Replace the `conn` auto-reconnect property with explicit `ensure_connected()`; update call sites.
5. Rewrite the Dockerfile install stage around `uv sync --frozen`; verify HEALTHCHECK and entrypoint.
6. Gitignore `cache/`; sync the skill doc's Python version; strip `__future__`/`Optional` legacy imports.
7. Update `current-versions.md` (new dependency) per version-sync rule.

## Deliverables

* Rewritten `config.py`; touched `worker.py` / `db.py`; new Dockerfile; `.gitignore` + skill-doc updates.

## Verification

* `uv run pytest` fully green (unit + integration — integration tests exercise config via env monkeypatching and must pass unchanged).
* Negative test: `SG_IDENTITY_MIN_SCORE=abc uv run python -c "from src.config import load_config; load_config()"` fails with a validation error naming the variable.
* `docker build` succeeds; container starts, health check passes, and processes a queued HTML task against a dev DB.
* `git check-ignore cache/<any-file>` returns 0.
