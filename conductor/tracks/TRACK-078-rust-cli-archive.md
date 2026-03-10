| Metadata | Value |
|:---|:---|
| **Track ID** | TRACK-078 |
| **Title** | Archive Rust CLI — Replace with Python db-migrate + Makefile |
| **Status** | Completed |
| **Created** | 2026-03-08 |
| **Author** | Sangeetha Grantha Team |
| **Depends On** | TRACK-064 (Unified Extraction Engine) |

# TRACK-078: Archive Rust CLI — Replace with Python db-migrate + Makefile

## Objective

Replace the Rust-based `sangita-cli` with Python `db-migrate` for migrations and a Makefile for dev orchestration. Archive the Rust tool to reduce build complexity and toolchain requirements.

## Problem Statement

The Rust CLI (`tools/sangita-cli/`) provided database migration, dev orchestration, and extraction commands. With the move to Docker Compose orchestration and the Python extraction worker, the Rust tool became redundant. Maintaining a Rust toolchain added unnecessary complexity for contributors.

## Scope

1. Move `tools/sangita-cli/` to `tools/sangita-cli-archived/`
2. Update `CLAUDE.md` to reference `make` commands instead of `cargo run` commands
3. Update all `.claude/commands/` to use `make` targets
4. Update git hooks to reference archived path

## Verification

- `make dev` starts full stack successfully
- `make db-reset` runs migrations and seeds correctly
- All `.claude/commands/` reference make targets

## Outcome

Rust toolchain removed from active prerequisites. All dev workflows use Makefile + Docker Compose.
