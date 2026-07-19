| Metadata | Value |
|:---|:---|
| **Status** | Completed |
| **Version** | 1.1.0 |
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

## Outcome (2026-07-19)

Verified locally, all four gates from a clean `.mypy_cache`:

| Gate | Before | After |
|:---|:---|:---|
| `ruff check .` | 125 errors | **0** |
| `ruff format --check .` | n/a | **53 files clean** |
| `mypy .` | 226 errors | **0** |
| `pytest` (unit) | 210 passed | **210 passed** |
| `pytest tests/integration` | 18 passed | **18 passed** |

`src/` is clean under **full** strict mypy with no relaxations.

> **Correction (2026-07-19, during TRACK-129):** this section originally claimed
> there was no `type: ignore` anywhere in the tree. That was wrong.
> `src/page_segmenter.py:138` carries a pre-existing
> `# type: ignore[arg-type]` that predates this track and survived it. The
> accurate claim is that TRACK-126 *added* none. (A second one, introduced by
> TRACK-128 in `gemini_enricher.py`, was removed once spotted.)
>
> **Resolved (2026-07-19, post-track):** `page_segmenter.py:138` no longer carries
> it. `max(size_counts, key=size_counts.get)` failed `arg-type` because
> `dict.get` is `(float) -> int | None`, which is not a valid sort key; indexing
> instead (`key=lambda size: size_counts[size]`) types cleanly. Verified
> value-identical, including tie-breaking, over 200,000 generated dicts.
>
> One `type: ignore` remains in `src/` — `config.py`'s
> `@computed_field  # type: ignore[prop-decorator]`. That one is unavoidable:
> mypy reports "Decorators on top of @property are not supported" without it,
> confirmed by removing it and re-running.

How the two big error classes cleared:

* **Pydantic aliases (64 `call-arg`)** — the models already set
  `populate_by_name`, but as a bare dict, which the plugin does not read. Switching
  to `ConfigDict(populate_by_name=True)` let the plugin generate `__init__` by field
  name; the 106 camelCase call-site kwargs were then rewritten to snake_case field
  names. Runtime behaviour is unchanged (`populate_by_name` accepts both, and the
  wire format still comes from `to_json_dict(by_alias=True)`).
* **psycopg generics (63 errors)** — cleared by two annotations in `src/db.py`
  (`psycopg.Connection[dict[str, Any]]` on `_conn` and the `conn` property).

Notes and deviations:

* `zip(..., strict=False)` was used at both `B905` sites rather than `strict=True`:
  `structure_parser.py` compares deliberately ragged strings, and the
  `gemini_enricher.py` batch site is TRACK-128's territory. Making either strict
  would be a behaviour change.
* `scripts/` sibling imports (`section_triage`, `section_repair`) resolve via
  runtime `sys.path` insertion that mypy cannot follow, so they carry an
  `ignore_missing_imports` override pointing at **TRACK-127**, per this track's
  Out of Scope note.
* One dead placeholder list in `repair_comprehensive.py` (`parser.parse("").__class__`)
  was removed — it was unconditionally overwritten two statements later.
* Test method stubs in `test_worker.py` moved to `monkeypatch.setattr` (13 sites).
* Enums moved from `(str, Enum)` to `StrEnum`; verified no f-string/`str()`
  interpolation of these members exists, so serialization is unaffected.

**Follow-up (not adopted here):** ruff groups `SIM`/`C4`/`PTH`/`RUF` report 128
additional findings. 24 are `RUF002`/`RUF003` ambiguous-unicode false positives
(the corpus is Indic text), and the `SIM` rewrites are behaviour-adjacent, so
adopting them conflicts with this track's zero-behaviour-change guarantee.
23 `RUF100` unused-`noqa` removals are the cheap subset if a later track wants them.
Coverage thresholds remain unmeasured.
