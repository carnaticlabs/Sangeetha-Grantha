| Metadata | Value |
|:---|:---|
| **Status** | Active |
| **Version** | 1.0.0 |
| **Last Updated** | 2026-03-10 |
| **Author** | Sangeetha Grantha Team |

# Agent Skills, Workflows & Hooks Modernization

## Purpose

Modernize all `.agent/` workflows and skills to reflect the current Makefile + Docker Compose + Python toolchain. Add protective hooks, create diagnostic skills, and clean up redundant files.

## Implementation Details

### Workflows Updated
| File | Change |
|------|--------|
| `debug-start-application.md` | `cargo run` → `make dev`, added health check |
| `start-application.md` | `./start-sangita.sh` / `cargo run` → `make dev` / `make db` |
| `verify-db-status.md` | `cargo run ... db migrate` → `make migrate`, `_sqlx_migrations` → `schema_migrations` |
| `e2e-test-runner.md` | Updated prerequisites, troubleshooting to use make commands |
| `bulk-import-testing.md` | Updated reset, added extraction test category, removed H2 references |
| `retrospective-commit-and-push.md` | Generalized changeset heuristics, updated commit format with TRACK-ID |

### Skills Updated
| File | Change |
|------|--------|
| `commit-policy/SKILL.md` | v1.0.0 → v1.1.0: TRACK-ID format, merged commit-guardrails |
| `ingestion-pipeline-guardian/SKILL.md` | Fixed Docker paths and CLI references |

### New Skills
| File | Purpose |
|------|---------|
| `data-quality-audit/SKILL.md` | 7 diagnostic SQL queries for data integrity |
| `extraction-debugger/SKILL.md` | Extraction pipeline diagnostics and troubleshooting |

### Hooks Added
| Hook | Event | Purpose |
|------|-------|---------|
| Protected file guard | PreToolUse (Write/Edit) | Block edits to config/*.env files |
| Migration naming | PreToolUse (Write/Edit) | Enforce NN__description.sql pattern |

### Removed
| File | Reason |
|------|--------|
| `workflows/commit-guardrails.md` | Merged into commit-policy skill |
| `rules/commit.md` | Empty/unused |

Ref: application_documentation/10-implementations/track-083-agent-skills-workflows-update.md
