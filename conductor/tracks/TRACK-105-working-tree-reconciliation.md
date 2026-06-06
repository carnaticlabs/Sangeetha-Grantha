| Metadata | Value |
|:---|:---|
| **Status** | In Progress |
| **Version** | 1.0.0 |
| **Last Updated** | 2026-06-06 |
| **Author** | Principal Data & AI Engineering review (for Seshadri) |
| **Priority** | P0 — do before any new feature work |

# TRACK-105: Working Tree Reconciliation & Safe Landing

## Goal

Bring the repository's **dirty working tree to a clean, intentional state** — every uncommitted change is either committed with a proper `Ref:` line or deliberately discarded. No completed work should be left at risk of loss, and no throwaway artifact should be allowed to drift into the history. This is the prerequisite for every other track: you cannot cleanly land the AI uplift or the convergence cleanup on top of a tree that already contains a finished-but-uncommitted feature.

## Context — what the tree actually contains (snapshot 2026-06-06)

`git status` on `main` (up to date with `origin/main`) shows ~2,334 insertions / 2,097 deletions across 10 tracked files plus a pile of untracked files. These are **not** a single change — they are at least four distinct logical groups that became entangled because work continued without committing. The danger item is Group A: a feature already marked **Completed** in the registry whose code and track files are **uncommitted/untracked** and would be lost on a careless `git checkout`/`git restore`.

### Triage table

| Group | Items | Nature | Recommended disposition |
|:---|:---|:---|:---|
| **A. Indic extraction feature (TRACK-100–104)** | `conductor/tracks/TRACK-100…104-*.md` (untracked), `tools/krithi-extract-enrich-worker/src/structure_parser.py` (+83, modified), `tests/test_structure_parser.py` (+175, modified), 6 new test fixtures under `tests/fixtures/structure_parser/`, `src/browser_batch_extract.py` (untracked), `conductor/tracks.md` rows for 100–104 | **Completed feature, never committed.** Coherent multi-pass parsing work with tests + fixtures. | **LAND.** Commit as one logical unit, `Ref: ` the TRACK-100 doc. Verify tests pass first. |
| **B. Trinity import data (TRACK-093)** | `database/for_import/*.csv` (Dikshitar, Thyagaraja, Syama Sastri, Trinity-Test-60, raga-resolution-map, unresolved-ragas-krithis) — all heavily modified | **In-flight import data** cleanups described in TRACK-093. | **LAND**, but only after confirming the cleanups match TRACK-093's documented CSV-cleanup step. Commit `Ref: ` TRACK-093. |
| **C. Docs & agent tooling** | `application_documentation/sangeetha-grantha-state-of-nation-july-2026.md`, `…-uplift-tasks.md`, `conductor/prompts/*.md`, `.agent/workflows/agentic-prompt-optimizer.md`, `AGENTS.md` (+2) | **Intentional documentation/tooling** additions. | **LAND.** Commit with doc refs. Low risk. |
| **D. ~~Review~~ → RESOLVED (discarded 2026-06-06)** | ~~`prompt-optimizer-review-iter1.html`, `engineering.plugin`~~ | Introspected: `engineering.plugin` = a Claude Code/Cowork plugin bundle (ZIP: `.claude-plugin/plugin.json`, GitHub MCP connector, generic engineering skills) — reproducible tooling, not app code. `prompt-optimizer-review-iter1.html` = a throwaway "Eval Review" benchmark output from agentic-prompt-optimizer experimentation. Neither aligns with the application's long-term/production goals. | **DONE — discarded** both; added `*.plugin` and `*-review-iter*.html` to `.gitignore` to prevent recurrence. |

> Note on TRACK-099: its files are **already committed** (`25c1f5b`, `6562218`). The only thing wrong is the registry status line — handled in **TRACK-106**, not here.

## Implementation Plan

### Phase 1 — Verify before touching (read-only)
- [ ] `git stash list` / confirm nothing already stashed.
- [ ] Run the extraction worker test suite to confirm Group A is green **before** committing: `cd tools/krithi-extract-enrich-worker && uv run pytest`.
- [ ] Diff-review `structure_parser.py` and the new fixtures against the TRACK-100–104 descriptions to confirm code matches the documented intent.
- [ ] Diff-review the CSVs (Group B) — spot-check that row deltas are the documented "Part 1-5"/header-artifact cleanups, not accidental data loss.

### Phase 2 — Land Group A (the at-risk feature) as one commit
- [ ] Stage: `structure_parser.py`, `test_structure_parser.py`, `tests/fixtures/structure_parser/*`, `src/browser_batch_extract.py`, `conductor/tracks/TRACK-100…104-*.md`, and the `conductor/tracks.md` rows for 100–104.
- [ ] Commit with the project convention (`Ref: application_documentation/...`, TRACK reference, proper formatting per the commit-policy skill).
- [ ] Confirm `git status` no longer lists these.

### Phase 3 — Land Group B (import data) and Group C (docs)
- [ ] Commit the `database/for_import/*.csv` cleanups, `Ref:` TRACK-093.
- [ ] Commit the documentation + agent-tooling additions (Group C) in a separate doc commit.

### Phase 4 — Decide Group D ✅ DONE (2026-06-06)
- [x] `engineering.plugin`: introspected (plugin bundle, not app code) → **discarded**; `*.plugin` added to `.gitignore`.
- [x] `prompt-optimizer-review-iter1.html`: introspected (throwaway eval review) → **discarded**; `*-review-iter*.html` added to `.gitignore`.
- [x] Both removed from disk; neither appears in `git status` any longer.

### Phase 5 — Push & confirm parity
- [ ] Push to `origin/main` (or open PR per branch policy) so committed work is durable off the local machine.
- [ ] Confirm `git status` clean and `origin/main` matches local.

## Acceptance Criteria
- `git status` shows a clean working tree (or only deliberately-ignored files).
- No file that was "Completed" in the registry remains untracked.
- Extraction worker tests pass on the committed state.
- Every new commit carries a valid `Ref:` line per `CLAUDE.md` commit policy.
- Local `main` and `origin/main` are in parity.

## Risks
- **Data loss on Group A** if `git restore`/`checkout` is run before committing — highest risk; Phase 2 first.
- **Silent CSV corruption** (Group B) — large diffs; mitigate with the Phase 1 spot-check, not a blind commit.
- **Mixing logical groups in one commit** — defeats traceability; keep the four groups as separate commits.

## Dependencies
- Blocks: TRACK-107 (AI uplift), TRACK-096/099 cleanup — all want a clean tree to land on.
- Related: TRACK-106 (registry re-sync runs right after this).

## Progress Log
- 2026-06-06: Track created. Working-tree snapshot captured and triaged into four groups (A: land TRACK-100–104 feature; B: land TRACK-093 data; C: land docs; D: review engineering.plugin / review HTML). TRACK-099 confirmed already committed.

Ref: application_documentation/sangeetha-grantha-uplift-tasks.md
