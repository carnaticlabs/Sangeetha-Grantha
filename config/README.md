# Config directory

Local and environment-specific configuration for Sangeetha Grantha. **Do not commit secrets.**

## Single source for env values

- **Externalizable env vars** and a **Postgres source** are defined in repo-root **[tools.yaml](../tools.yaml)**. Set actual values **outside the repo** (e.g. Goose/IDE env, or a gitignored file).
- Prefer **one** of:
  - **System environment variables** (e.g. from Goose Extensions, IDE run config, or shell).
  - **A single file** `config/local.env` (gitignored) with `KEY=value` lines. The backend loads it automatically after `config/development.env`; system env still overrides.

## Files in this directory

| File | Purpose |
|------|--------|
| `application.local.toml` | Backend/database settings (canonical for TOML-based config). |
| `development.env` | Dev env vars for backend/Vite (gitignored; use `local.env` or system env for values). |
| `local.env` | **Recommended** single file for local overrides (gitignored). Backend merges it after `development.env`. |
| `.env.auto-approval.example` | Template for auto-approval env vars; copy and set values outside repo. |

## See also

- [Configuration (operations)](../application_documentation/08-operations/config.md) – canonical config docs.
- [tools.yaml](../tools.yaml) – variable list and Postgres source for MCP/Goose.
