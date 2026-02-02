| Metadata | Value |
|:---|:---|
| **Status** | Completed |
| **Version** | 1.0.1 |
| **Last Updated** | 2026-02-02 |
| **Author** | Sangeetha Grantha Team |

# TRACK-033: Sangita CLI – Docs Command

## 1. Goal

Add a `docs` subcommand to `tools/sangita-cli` for documentation management: version sync, link validation, and related workflows (e.g. commit guardrails, change-mapper).

## 2. Context

- **CLI:** Rust-based `sangita-cli` in `tools/sangita-cli/`; already has `db`, `dev`, `test`, `commit`, etc.
- **Use case:** Run doc validation or version checks from the CLI without switching to a separate tool.

## 3. Implementation Plan

- [x] Add `docs` subcommand module (`commands/docs.rs`).
- [x] Register in `main.rs` and `commands/mod.rs`.
- [x] Update `Cargo.toml` if new dependencies.
- [x] Update `tools/sangita-cli/README.md` with docs command usage.

## 4. Progress Log

- 2026-01-30: Track created. Implemented `docs` subcommand (version sync, link validation); wired in main and mod; README updated.
- 2026-02-02: Track completed. Command verified in production workflow.
