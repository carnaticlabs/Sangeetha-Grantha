---
name: library-version-audit
description: >
  Check Sangita Grantha for newer stable library releases across the Gradle
  version catalog (gradle/libs.versions.toml), the admin web package.json and
  bun.lock, and the extraction worker's Python dependencies (pyproject.toml
  and uv.lock). Use when the user asks to check, audit, or refresh library
  versions, outdated dependencies, dependency updates, npm, Maven, PyPI, or
  the Gradle catalog. Report only. Do not upgrade unless the user asks to
  apply updates.
---

# Library version audit

Report newer stable releases for direct dependencies. Do not edit manifests, lockfiles, or `application_documentation/00-meta/current-versions.md` unless the user asks to apply an upgrade.

The checker exists so the audit is the same every time: one probe per Gradle catalog pin, lockfile-resolved npm and Python versions, and pre-releases left out. Guessing from memory, or treating `current-versions.md` as the upstream, will be wrong.

## Scope

- Gradle: `gradle/libs.versions.toml`. Each `[versions]` key is one pin. The checker probes a single representative module, or a plugin when the key is plugin-only.
- npm: `modules/frontend/sangita-admin-web/package.json`, with resolved versions from `bun.lock` (dependencies and devDependencies).
- Python: `tools/krithi-extract-enrich-worker/pyproject.toml`, with resolved versions from `uv.lock` (main dependencies and the `dev` extra).

Out of scope unless the user asks: `archive/`, transitive lockfile entries, `.mise.toml`, the Gradle wrapper, and container image tags.

## Run

From the repo root. The command needs network access to Maven Central, Google Maven, JetBrains Compose, the Gradle Plugin Portal, npm, and PyPI.

```bash
make check-library-versions
```

The same check is `python3 tools/check-library-versions.py`. Useful flags:

- `--include-current` lists pins that already match upstream
- `--json` prints machine-readable rows
- `--ecosystem gradle|npm|pypi` checks one manifest
- `--self-test` checks parsers and version rules without network

Exit 0 means the report finished. Updates are not a failure. Exit 2 means every probe failed; say the registries were unreachable and do not invent versions.

## How to read a row

| Column | Meaning |
|---|---|
| Library | Catalog key, npm name, or Python project name |
| Declared | Catalog pin, npm range, or Python floor |
| Resolved | Catalog pin, `bun.lock` version, or `uv.lock` version |
| Bump | major / minor / patch against Resolved |
| Update via | What would have to change later. Not an instruction to change it now |
| Notes | Manifest comments, how many catalog entries share the pin, and the probed coordinate |

A Gradle update is always a pin edit in `gradle/libs.versions.toml`. Sharing a key (`ktor`, `compose`, `kotlin`) moves every module that references it.

An npm row marked `bun update (in range)` is inside the existing `^` or `~` range. `package.json (out of range)` is a range edit. An exact pin such as `7.0.1` with no `^` is deliberate.

A Python row marked `uv lock (floor still allows it)` does not require a `pyproject.toml` edit. The floor is a minimum; the lockfile is the pin. `pyproject.toml (above the floor)` means the latest release no longer satisfies the declared specifier.

## Presenting the report

Lead with the summary counts, then major bumps, then minor and patch. Quote Notes that record a holdback (frozen artifacts, paired releases, deferred majors).

Then read the Version History table in `application_documentation/00-meta/current-versions.md`. When a sentence names a library that appears in the update table, include that holdback in the summary. The manifests and lockfiles are the version source. The doc explains deliberate deferrals; it is not the upstream.

If some rows are unchecked, list those registries or coordinates. Do not fill in a latest version yourself.

## Applying upgrades

Only when the user asks. Keep the audit and the upgrade as separate steps.

1. Edit the source the report names (`gradle/libs.versions.toml`, `package.json`, or `pyproject.toml`). Refresh `bun.lock` or `uv.lock` when the row says the lockfile is the pin.
2. Sync `application_documentation/00-meta/current-versions.md`, then any doc that repeats those versions (CLAUDE.md version rule).
3. Leave committed Flyway `V__` migrations alone. A library bump is not a schema change.
