---
name: library-version-audit
description: >
  Check Sangita Grantha for newer stable library releases across the Gradle
  version catalog (gradle/libs.versions.toml), the admin web package.json and
  bun.lock, and the extraction worker's Python dependencies (pyproject.toml
  and uv.lock). Use when the user asks to check, audit, or refresh library
  versions, outdated dependencies, dependency updates, npm, Maven, PyPI, or
  the Gradle catalog. Every run writes the Library version audit canvas.
  Report only. Do not upgrade unless the user asks to apply updates.
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
python3 tools/check-library-versions.py --json
```

`make check-library-versions` prints the same audit as Markdown. Use the JSON for the canvas so versions are not copied out of the Markdown by hand. One run is enough. Do not probe the registries again to fill a gap.

Other flags:

- `--include-current` lists pins that already match upstream in the Markdown report
- `--ecosystem gradle|npm|pypi` checks one manifest
- `--self-test` checks parsers and version rules without network

Exit 0 means the report finished. Updates are not a failure. Exit 2 means every probe failed; say the registries were unreachable and do not invent versions. Do not write the canvas from memory in that case.

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

## Classify each update

Read the Version History table in `application_documentation/00-meta/current-versions.md` before classifying. The manifests and lockfiles are the version source. The doc explains deliberate deferrals; it is not the upstream.

- **Hold** when Version History, or a Note on the row, says that library is deferred, held back, frozen, or staying on the current pin. A major whose Note says the toolchain rejects it is a Hold, not an upgrade.
- **Apply** when the bump is a patch and the library is not a Hold. Catalog patches are `gradle/libs.versions.toml` edits. In-range npm patches are `bun update`. Python patches whose floor still allows the release are a `uv lock` refresh.
- **Review** when the bump is minor or major and the library is not a Hold. Also Review, even for a patch, when Notes say the pin must move with a Compose, CI, or worker image.

Unchecked rows stay Unchecked. Do not invent a latest version for them.

## Canvas

Every execution writes the canvas. The chat message is the short summary; the canvas is the report.

Read the canvas skill, then overwrite:

`~/.cursor/projects/<workspace>/canvases/library-version-audit.canvas.tsx`

For this repo that file is `canvases/library-version-audit.canvas.tsx` under the Cursor project directory. Keep a single canvas. Import only from `cursor/canvas`. Embed the JSON rows inline. No `fetch`. Colors come from the canvas theme tokens.

The canvas matches this layout, using only rows from the JSON:

1. Title and one summary sentence: how many pins are behind, how many are held, and the apply / review split.
2. Four stats: behind latest stable (Apply + Review), patch bumps to apply, minor bumps to review, already on latest stable.
3. A usage bar of current, behind, and held. Held is not part of behind. The total is every checked pin.
4. A bar chart, **Pins by layer**. Y-axis is pin count. X-axis is Java + Kotlin, Frontend, Python. Series: On latest stable, Behind latest stable, Held or deferred. Caption the source manifests and the check date.
5. Two callouts. One names the Apply set and how each stack is updated. One names the Review set, including any pin that must move with an image or another library.
6. **Updates available**, with All / Java / Frontend / Python filters. Columns: Layer, Library, Pinned, Latest stable, Action, Notes. Sort Apply before Review inside each stack.
7. **Intentionally not taking**, when any row is Hold.
8. A collapsed **Already on latest stable** list.

In the chat reply, lead with the counts and the apply / review / hold recommendation, then link the canvas by its absolute path. If some rows are unchecked, name those registries or coordinates. Do not fill in a latest version yourself.

## Applying upgrades

Only when the user asks. Keep the audit and the upgrade as separate steps.

1. Edit the source the report names (`gradle/libs.versions.toml`, `package.json`, or `pyproject.toml`). Refresh `bun.lock` or `uv.lock` when the row says the lockfile is the pin.
2. Sync `application_documentation/00-meta/current-versions.md`, then any doc that repeats those versions (CLAUDE.md version rule).
3. Leave committed Flyway `V__` migrations alone. A library bump is not a schema change.
