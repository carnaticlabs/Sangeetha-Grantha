| Metadata | Value |
|:---|:---|
| **Status** | Active |
| **Version** | 1.1.0 |
| **Last Updated** | 2026-02-08 |
| **Author** | Sangeetha Grantha Team |

# Configuration – Canonical

---

> NOTE: Canonical configuration documentation now lives here. Module-local `config/README.md` remains for local context.

---

# Configuration Repository

This repository uses a single source of truth for environment variables and secrets.

## Single Source of Truth

**Externalizable env vars and a Postgres source are defined in repo-root [tools.yaml](../../tools.yaml)**.

Actual values (API keys, DB credentials, etc.) are set **outside the repo**:
1. **System environment variables** (e.g., in Goose Extensions, IDE run config, or shell).
2. **A single gitignored file** [config/local.env](../../config/local.env).

The backend (API and DAL) loads configuration in this order (last one wins):
1. `config/development.env` (default values)
2. `config/local.env` (local overrides, gitignored)
3. System environment variables

## Files in `config/`

- `development.env` – Default dev values for backend and Vite.
- `local.env` – (gitignored) Recommended single file for local overrides and secrets.
- `README.md` – Local guidance for researchers and developers.

## Suggested workflow

1. Set your secrets (like `GEMINI_API_KEY` or `DB_PASSWORD`) in a `config/local.env` file.
2. Ensure you have a standard Postgres instance running (or use `cargo run -- db reset` to start one via Docker if using the CLI).
3. The backend and frontend will automatically pick up changes from `local.env` on restart.
4. For production, set the corresponding system environment variables.