| Metadata | Value |
|:---|:---|
| **Track ID** | TRACK-083 |
| **Title** | Agent Skills, Workflows & Hooks Modernization |
| **Status** | Completed |
| **Created** | 2026-03-10 |
| **Author** | Sangeetha Grantha Team |
| **Depends On** | TRACK-078 (Rust CLI Archive) |

# TRACK-083: Agent Skills, Workflows & Hooks Modernization

## Objective

Update all `.agent/` workflows and skills to reflect the current Makefile + Docker Compose + Python toolchain, add protective hooks, create new diagnostic skills, and clean up redundant files.

## Scope

### Workflows Updated (Rust CLI → make commands)
- `debug-start-application.md` — `cargo run` → `make dev`
- `start-application.md` — `./start-sangita.sh` / `cargo run` → `make dev`
- `verify-db-status.md` — `cargo run ... db migrate` → `make migrate`, `_sqlx_migrations` → `schema_migrations`
- `e2e-test-runner.md` — Updated prerequisites and troubleshooting to use `make` commands
- `bulk-import-testing.md` — Updated reset commands, added extraction test category
- `retrospective-commit-and-push.md` — Generalized changeset heuristics, updated commit format

### Skills Updated
- `commit-policy/SKILL.md` — Added TRACK-ID prefix format, merged commit-guardrails workflow content, updated example
- `ingestion-pipeline-guardian/SKILL.md` — Fixed Docker paths (`pdf-extractor` → `krithi-extract-enrich-worker`), updated CLI references

### New Skills
- `data-quality-audit/SKILL.md` — 7 diagnostic SQL queries for section consistency, evidence coverage, variant completeness
- `extraction-debugger/SKILL.md` — Extraction queue diagnostics, worker log inspection, matching failure analysis

### New Hooks (in `.claude/settings.json`)
- Protected file guard: blocks edits to `config/*.env` files
- Migration naming check: enforces `NN__description.sql` pattern

### Cleaned Up
- Removed `workflows/commit-guardrails.md` (merged into commit-policy skill)
- Removed `rules/commit.md` (empty/unused)

### Settings Updated
- Added `Bash(make:*)`, `Bash(git push:*)`, `Bash(git diff-tree:*)` to shared permissions
