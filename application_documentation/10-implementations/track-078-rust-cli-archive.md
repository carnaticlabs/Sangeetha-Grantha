| Metadata | Value |
|:---|:---|
| **Status** | Active |
| **Version** | 1.0.0 |
| **Last Updated** | 2026-09-10 |
| **Author** | Sangeetha Grantha Team |
| **Document Type** | Evidence record |

# Archive Rust CLI — Replace with Python db-migrate + Makefile

---

> [!NOTE]
> Historical evidence: results, counts, commands, and observations below belong to the original work described here. The editorial update date is not a new test or corpus verification. For present behavior, use [current feature map](./../01-requirements/features/README.md).

## Purpose

Replace Rust `sangita-cli` with Python `db-migrate` for migrations and a Makefile for dev orchestration, reducing toolchain complexity.

## Implementation Details

- Moved `tools/sangita-cli/` to `tools/sangita-cli-archived/`
- Updated `CLAUDE.md` to reference `make` commands
- Updated all `.claude/commands/` (dev-start, db-reset, test-all, steel-thread, new-migration, commit) to use `make` targets
- Updated git hooks to reference archived path

## Code Changes

| File | Change |
|------|--------|
| `tools/sangita-cli-archived/` | Moved from `tools/sangita-cli/` |
| `CLAUDE.md` | Updated commands to use `make` |
| `.claude/commands/*.md` | Updated to use `make` targets |

Ref: application_documentation/10-implementations/track-078-rust-cli-archive.md

---

[Section index](./README.md) · [Documentation home](./../README.md) · [Feature status](./../01-requirements/features/README.md)
