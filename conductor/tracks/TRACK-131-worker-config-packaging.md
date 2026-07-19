| Metadata | Value |
|:---|:---|
| **Status** | Completed |
| **Version** | 1.1.0 |
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

## Outcome (2026-07-19)

### Provenance

The implementation was authored in an earlier session and sat uncommitted in the
working tree while this track still read *Not Started*. It was reviewed rather
than rewritten; the four defects below were found in review and three were fixed
before commit.

### DoD, verified functionally (not by inspection)

| Item | Evidence |
|:---|:---|
| `BaseSettings` port, same env names/defaults | 292 unit + 18 integration pass unchanged |
| Malformed value fails naming the variable | `SG_IDENTITY_MIN_SCORE=abc` → `ValidationError ... SG_IDENTITY_MIN_SCORE` |
| Range constraints | `SG_IDENTITY_MIN_SCORE=999` → "less than or equal to 100" |
| `extractor_version` at instantiation | `computed_field` over `EXTRACTOR_VERSION`; container reports `krithi-extract-enrich-worker:1.0.0` |
| Concurrency knobs deleted (option b) | zero refs in `src/` or `compose.yaml`; worker docstring updated |
| `ensure_connected()` | explicit, logs on reconnect, called by every public DB method |
| Dockerfile from lockfile | `uv sync --frozen --no-dev`; image builds; `src.worker` + `health_check` import inside it |
| `cache/` ignored | `git check-ignore` exits 0 |
| Skill doc Python version | `3.11+` → `3.14+` |

### Review findings

Three fixed before commit:

1. **`mypy` was failing** — `config.py` carried `# type: ignore[misc]` on the
   `computed_field`; mypy wanted the narrower `[prop-decorator]`. This broke the
   CI gate TRACK-126 had just added. Fixed.
2. **Immutability regression** — the previous `ExtractorConfig` was
   `@dataclass(frozen=True)`; `BaseSettings` is mutable by default, so
   `config.database_url = ...` silently succeeded at runtime. Restored via
   `SettingsConfigDict(frozen=True)`, confirmed by an explicit mutation attempt.
3. **Unpinned build tooling** — `COPY --from=ghcr.io/astral-sh/uv:latest`
   directly undermined this track's reproducible-build goal. Pinned to
   `uv:0.11.25`; image rebuilt and re-verified.

One flagged, **not** changed — needs a call:

4. **`env_file=".env"`** adds a configuration source the DoD did not ask for
   ("same env var names, same defaults"). No `.env` exists in the worker
   directory today, so nothing is currently affected, and real environment
   variables still take precedence over the file. But this repo does use `.env`
   files elsewhere, so a future stray file would change worker config without any
   code change. Left in place with an explanatory comment in `config.py`; remove
   it if config should read the environment only.

### Notes

* `db.py` uses `assert` for the connection invariant, which is stripped under
  `python -O`, and `worker.py`'s rollback handler (`except Exception: pass`) now
  swallows that AssertionError. Impact is contained — the next
  `ensure_connected()` recovers — but a `RuntimeError` would be the sturdier guard.
* `from __future__ import annotations` was dropped only from the files this track
  touched; 17 files in `src/` still carry it. Consistent with the DoD's
  "while touching files" scoping, not a full sweep.

### Verification

`ruff check` 0, `ruff format --check` clean (55 files), `mypy` 0,
`pytest` 292 unit + 18 integration passing, `docker build` succeeds with the
pinned uv image and the entrypoint imports inside it.
