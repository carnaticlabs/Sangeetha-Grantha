| Metadata | Value |
|:---|:---|
| **Status** | Completed |
| **Version** | 1.0.0 |
| **Last Updated** | 2026-02-06 |
| **Author** | Sangeetha Grantha Team |

# TRACK-037: Environment Variables & Config Standardisation

## 1. Goal

Standardise how environment variables and local configuration are defined, loaded, and documented so that secrets stay out of the repo and a single source of truth (e.g. `tools.yaml`, `config/README.md`) guides developers and tooling (Goose, IDE, CLI).

## 2. Context

- **Problem:** Committed `.env.development` and scattered env usage risk leaking secrets and cause inconsistent behaviour across IDEs, Goose, and CLI.
- **Approach:** Externalisable env vars and a Postgres source are defined in repo-root `tools.yaml`; actual values are set outside the repo (system env or a single gitignored `config/local.env`). Backend loads `config/development.env` then `config/local.env`; system env overrides.
- **Docs:** Canonical config docs in `application_documentation/08-operations/config.md`; local context in `config/README.md`.

## 3. Implementation Plan

- [x] Remove committed `config/.env.development`; ensure env files are gitignored (`development.env`, `local.env`, etc.).
- [x] Define variable list and Postgres source in repo-root `tools.yaml`.
- [x] Backend: load env from `config/development.env` then `config/local.env`; system env overrides (`ApiEnvironmentLoader`, `ApiEnvironment.kt`).
- [x] Use `config/development.env` (and optional `local.env`) as single dev env source; document in `config/README.md`.
- [x] Add `config/README.md` documenting config directory, file roles, and single-source workflow.
- [x] Provide template `tools/bootstrap-assets/env/development.env.example` for bootstrap/copy.
- [x] Update `application_documentation/08-operations/config.md` with tools.yaml reference and local.env workflow.
- [x] Align DAL/database config loading with same env source (`DatabaseConfigLoader.kt`).
- [x] Update quick-reference and runbooks (e.g. steel-thread, auth) to point to config docs and tools.yaml.
- [x] Align Vite/frontend envDir pointing at `config/`.

## 4. Progress Log

- 2026-02-06: Track created. Completed: removal of committed `.env.development`; gitignore for `config/development.env`, `config/local.env`; `tools.yaml` for variable list; `ApiEnvironmentLoader` loading from `config/development.env` then `config/local.env` with system override; `config/README.md`; `development.env.example` template; ops config doc updates.
- 2026-02-07: Completed DAL alignment in `DatabaseConfigLoader.kt` using `dotenv-kotlin`. Aligned Vite configuration in `vite.config.ts`. Updated runbooks and quick-references to point to canonical configuration documentation.
