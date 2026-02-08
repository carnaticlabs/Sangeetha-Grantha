| Metadata | Value |
|:---|:---|
| **Status** | Active |
| **Version** | 1.1.0 |
| **Last Updated** | 2026-02-08 |
| **Author** | Sangeetha Grantha Team |

# Sangita CLI – Docs Command

**Conductor:** [TRACK-033](../../conductor/tracks/TRACK-033-cli-docs-command.md)

## 1. Purpose

The `docs` subcommand of `tools/sangita-cli` supports documentation workflows: version sync, link validation, and alignment with commit guardrails and change-mapper.

## 2. Code Changes Summary (Retrospective)

| File | Change |
|:---|:---|
| `tools/sangita-cli/src/commands/docs.rs` | **New.** DocsArgs and run implementation (version sync, link checks). |
| `tools/sangita-cli/src/commands/mod.rs` | **Updated.** `pub mod docs`. |
| `tools/sangita-cli/src/main.rs` | **Updated.** Import docs; add `Docs(docs::DocsArgs)` variant; dispatch `docs::run(args)`. |
| `tools/sangita-cli/Cargo.toml` | **Updated.** Dependencies if needed for doc parsing/links. |
| `tools/sangita-cli/README.md` | **Updated.** Document `cargo run -- docs` usage. |

## 3. Commit Reference

Use this file as the documentation reference for the **CLI docs command** commit:

```text
Ref: application_documentation/08-operations/cli-docs-command.md
```