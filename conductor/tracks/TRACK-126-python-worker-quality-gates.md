| Metadata | Value |
|:---|:---|
| **Status** | Not Started |
| **Version** | 1.0.0 |
| **Last Updated** | 2026-07-19 |
| **Author** | Sangeetha Grantha Team |

# TRACK-126: Python Worker Quality Gates — Zero ruff/mypy Errors + CI Enforcement

**Goal:** Make the quality gates declared in `tools/krithi-extract-enrich-worker/pyproject.toml` real. The tool declares `mypy strict = true` and a ruff rule set, but currently carries ~226 mypy errors and ~125 ruff errors. Drive both to zero and gate them in CI so they stay there.

**Origin:** Python best-practices evaluation, 2026-07-19 (item 1 — highest priority). Most of the debt clears mechanically; the two big error classes are Pydantic-alias false positives and untyped psycopg generics.

## Definition of Done

* `uv run ruff check .`, `uv run ruff format --check .`, and `uv run mypy .` all exit 0 from `tools/krithi-extract-enrich-worker/`.
* The Pydantic mypy plugin is enabled (`[tool.mypy] plugins = ["pydantic.mypy"]`) — no alias-related `call-arg` suppressions anywhere.
* `psycopg.Connection` / cursors in `src/db.py` are parameterized for `dict_row` (`psycopg.Connection[dict[str, Any]]`) — no `type: ignore` on row access.
* Strictness is scoped honestly: `[[tool.mypy.overrides]]` for `tests.*` / `scripts.*` relax only what those layers genuinely need (e.g. `no-untyped-def`); `src/` stays fully strict.
* CI runs all three checks for this tool and fails the build on regression.
* Zero behavioural change — this track is types, imports, and formatting only.

## Inputs

* `@tools/krithi-extract-enrich-worker/pyproject.toml`
* `@tools/krithi-extract-enrich-worker/src/db.py` (63 mypy errors — psycopg generics)
* `@.agents/skills/python-extraction-worker/SKILL.md`
* Ruff statistics baseline: UP045 (36), B007 (22), E501 (19), F401 (15), I001 (10) — 70 of 125 auto-fixable.

## Out of Scope

* Moving/deleting the one-off scripts in `src/` (TRACK-127) — lint them where they sit or exclude them explicitly with a comment pointing at TRACK-127.
* Any refactoring of runtime logic (TRACK-128/129/130).
* Coverage thresholds (note as follow-up if measured).

## Steps

1. Enable `pydantic.mypy` plugin; re-baseline mypy.
2. Parameterize psycopg types in `src/db.py`; type `to_json_dict() -> dict[str, Any]` in `schema.py`.
3. `uv run ruff check --fix .` (safe fixes), then hand-fix the remainder (B007 unused loop vars, E501, E741, B905 `zip(strict=)`).
4. `uv run ruff format .` on the tree.
5. Add mypy per-module overrides for `tests.*`/`scripts.*`; replace raw method assignment in tests with `monkeypatch.setattr` where that is the cheaper fix for `method-assign`.
6. Add the three checks to CI for this tool's path.
7. Consider enabling ruff groups `SIM`, `C4`, `PTH`, `RUF` — adopt if the residual fix cost is small, otherwise record as follow-up.

## Deliverables

* Updated `pyproject.toml` (mypy plugin, overrides, ruff config).
* Type/lint/format fixes across `src/`, `tests/`, `scripts/`.
* CI workflow change gating the worker's checks.

## Verification

Run inside `tools/krithi-extract-enrich-worker/`:

* `uv run ruff check .` → 0 errors
* `uv run ruff format --check .` → clean
* `uv run mypy .` → 0 errors
* `uv run pytest` → all green (210 unit + 18 integration at time of writing)
