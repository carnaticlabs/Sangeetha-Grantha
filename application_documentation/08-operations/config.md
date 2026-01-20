# Configuration – Canonical

| Metadata | Value |
|:---|:---|
| **Status** | Draft |
| **Version** | 0.1.0 |
| **Last Updated** | 2026-01-20 |
| **Author** | System |

---


> NOTE: Canonical configuration documentation now lives here. Module-local `config/README.md` remains for local context.

---

# Configuration Directory

This folder centralises environment-specific settings for local development and deployment. Copy these files per environment (e.g., `application.staging.toml`, `.env.staging`) and adjust values as needed.

## Files

- `application.local.toml` – canonical source for backend and database settings. Referenced by the Kotlin services and Rust database tooling.
- `.env.development` – Vite dev server variables (consumed by the React admin app). Loaded via `envDir` in `vite.config.ts`.
- `.env.production` – Build-time variables for the production React bundle.

### Suggested workflow

1. Duplicate `application.local.toml` for each environment and update credentials/URLs.
2. Point backend services to a specific file with `APP_CONFIG_PATH` or `DB_CONFIG_PATH`.
3. Update the appropriate `.env.*` file so frontend builds target the correct API base URL.