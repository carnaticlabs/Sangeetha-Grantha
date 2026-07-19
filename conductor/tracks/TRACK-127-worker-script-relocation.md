| Metadata | Value |
|:---|:---|
| **Status** | Completed |
| **Version** | 1.1.0 |
| **Last Updated** | 2026-07-19 |
| **Author** | Sangeetha Grantha Team |

# TRACK-127: Relocate One-Off Operational Scripts Out of the Worker Package

**Goal:** `src/` is the installable, Docker-shipped worker package, but it contains ~1,400 lines of one-shot operational tooling: `repair_comprehensive.py`, `repair_sections.py`, `repair_section_text.py`, `import_dikshitar_krithis.py`, `browser_batch_extract.py`. Move completed repairs to `archive/`, still-useful tools to `scripts/`, and fix the hazards they carry.

**Origin:** Python best-practices evaluation, 2026-07-19 (item 2).

## Definition of Done

* No one-off repair/import scripts remain in `tools/krithi-extract-enrich-worker/src/` — the package contains only the live pipeline (worker, strategies, parsers, enricher, db, cli, schema, heuristics).
* Completed Phase-4A repair scripts land under `archive/` (repo convention for retired tooling, cf. TRACK-078) with a README noting what they fixed and when.
* Still-useful batch tools (`browser_batch_extract`, `import_dikshitar_krithis` if still needed for TRACK-093) live in `tools/krithi-extract-enrich-worker/scripts/` with a shared `_common.py` (DB URL resolution, logging setup).
* The stray wrong-credential default in `repair_sections.py` (`sangita:sangita@…`) is gone — all DB access resolves through one shared helper honouring `DATABASE_URL`.
* The `sys.path.insert` hack inside `browser_batch_extract.py` is removed (proper relative imports or run-as-module documentation).
* Docker image and wheel no longer ship the relocated files (verify `pip show -f` / image contents).

## Inputs

* `@tools/krithi-extract-enrich-worker/src/` (the five scripts listed above)
* `@tools/krithi-extract-enrich-worker/scripts/` (existing triage/repair scripts — the relocation target and style reference)
* `@archive/` (repo archive convention)
* `conductor/tracks/TRACK-093-trinity-krithi-bulk-import.md` (Paused — confirms whether `import_dikshitar_krithis.py` is still live tooling)

## Out of Scope

* Rewriting the scripts' logic — this is relocation + hazard removal only.
* Lint/type cleanup beyond what the move itself requires (TRACK-126 owns the gates).

## Steps

1. Confirm with git history / TRACK-093 status which scripts are dead (Phase-4A repairs are complete per TRACK-079) vs. still operational.
2. `git mv` dead scripts to `archive/tools/krithi-worker-repairs/` with a short README.
3. `git mv` live scripts to `scripts/`; create `scripts/_common.py`; fix imports and the credential default.
4. Update any docs/workflows referencing the old `python -m src.repair_*` invocations.
5. Rebuild the Docker image; confirm the relocated files are absent and `HEALTHCHECK`/entrypoint still work.

## Deliverables

* Relocated scripts + `scripts/_common.py` + archive README.
* Updated references in docs/Makefile/workflows.

## Verification

* `uv run pytest` green (no test imports the moved modules — verify with grep).
* `docker build` succeeds; `docker run --rm <img> python -c "import src.worker"` works; image contains no `repair_*` files.
* `grep -rn "repair_comprehensive\|repair_sections\|repair_section_text\|import_dikshitar\|browser_batch" --include="*.py" --include="*.md" .` shows only archive/scripts paths.

## Outcome (2026-07-19)

Relocation only — no script logic was rewritten.

| Script | Disposition | Why |
|:---|:---|:---|
| `repair_comprehensive.py` | `archive/tools/krithi-worker-repairs/` | Phase-4A repair, completed under TRACK-079 (Completed) |
| `repair_sections.py` | `archive/tools/krithi-worker-repairs/` | as above |
| `repair_section_text.py` | `archive/tools/krithi-worker-repairs/` | as above |
| `import_dikshitar_krithis.py` | `scripts/` | TRACK-093 is **Paused**, not abandoned — still live tooling |
| `browser_batch_extract.py` | `scripts/` | live batch tool |

`src/` now holds only the live pipeline: worker, strategies, parsers, enricher,
db, cli, schema, heuristics.

Hazards removed:

* `repair_sections.py` had a hardcoded fallback DSN
  (`postgresql://sangita:sangita@…`) whose credentials match no environment. It now
  requires `DATABASE_URL`. Every other script resolves through the new
  `scripts/_common.py:database_url()`, which carries the one correct dev default
  (`postgres:postgres`) — including `section_triage.DB_URL`, which the sibling
  scripts import.
* The `sys.path.insert` in `browser_batch_extract.py` is gone. Both relocated
  scripts run from the worker root as
  `PYTHONPATH=. uv run python scripts/<name>.py`, documented in `scripts/_common.py`.

Verification run:

* `docker build` succeeded; `import src.worker` and `from src.worker import health_check`
  both OK inside the image; `ls /app/src/` confirms no `repair_*`, `import_dikshitar*`,
  or `browser_batch*` files ship. The Dockerfile needed no change — it only ever
  copied `src/`, which is precisely why moving the files out was sufficient.
* Relocated scripts smoke-tested: `browser_batch_extract.py --help` renders,
  `import_dikshitar_krithis` imports, `section_triage.DB_URL` resolves via the helper.
* `ruff check` 0, `ruff format --check` clean (51 files), `mypy` 0,
  `pytest` 210 unit + 18 integration passing.
* DoD grep shows only `archive/`, `scripts/`, and historical conductor records.

Notes:

* The archived scripts keep their package-relative imports and are **not runnable
  as-is** — rewriting untested archived code is riskier than leaving an accurate
  record. Each carries a RETIRED docstring banner and the directory has a README.
* `TRACK-105`'s references to the old `src/browser_batch_extract.py` path were left
  alone: that document is a record of a past reconciliation, not current guidance.
* The mypy `ignore_missing_imports` override for the bare sibling imports
  (`section_triage`, `section_repair`) that TRACK-126 added is still present. Those
  scripts were not in this track's move list, so restructuring their imports was
  left out of scope.
