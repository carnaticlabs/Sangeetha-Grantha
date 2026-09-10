| Metadata | Value |
|:---|:---|
| **Status** | Active |
| **Version** | 1.0.0 |
| **Last Updated** | 2026-09-10 |
| **Author** | Sangeetha Grantha Team |

# Local configuration

---

Use [Runtime configuration](../application_documentation/08-operations/config.md) for component-specific variables and loading order. This directory contains tooling definitions and local backend configuration; it is not one shared environment source for all applications.

| File | Purpose |
|:---|:---|
| [application.local.toml](./application.local.toml) | Legacy/tooling configuration context; inspect the runtime loaders before relying on a field |
| [tools.yaml](./tools.yaml) | Externalizable variable/tooling reference |
| [mcp-servers.json](./mcp-servers.json) | Database MCP configuration |
| [.env.auto-approval.example](./.env.auto-approval.example) | Committed auto-approval example |
| `development.env`, `local.env` | Ignored local backend environment/overrides |
| `postgres-local.env` | Ignored file referenced by mise |

Keep actual credentials in local ignored files or the intended process/deployment environment. Do not overwrite another developer's existing local values.

The backend merges its environment-specific file, then `local.env`, then process variables. Python reads `.env` from its working directory; Vite has its own environment rules. See [onboarding](../application_documentation/00-onboarding/getting-started.md) for a minimal fresh-checkout setup and [authentication](../application_documentation/00-meta/quick-reference-auth.md) for provisioning and token exchange.
