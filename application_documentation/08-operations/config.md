| Metadata | Value |
|:---|:---|
| **Status** | Active |
| **Version** | 1.3.0 |
| **Last Updated** | 2026-09-10 |
| **Author** | Sangeetha Grantha Team |
| **Document Type** | Current guide |

# Runtime configuration

---

Configuration is component-specific. The backend, extraction worker, browser build, and mobile hosts do not all load the same environment file. Use this guide to choose where a value belongs; use the source loaders for the full accepted variable set.

## Loading rules

| Component | Files / environment | Precedence and practical effect |
|:---|:---|:---|
| Backend API | Environment-specific file under `config/`, then `config/local.env`, then process environment | Process environment wins; missing files are tolerated |
| DAL configuration | `config/development.env`, `config/local.env`, process environment | Backend-style DB variables; see loader |
| Python worker | `.env` in the worker's working directory, process environment | pydantic-settings; environment wins |
| Admin web | Vite environment and explicit process variables at dev/build time | Client uses `VITE_API_BASE_URL`; proxy uses `API_PROXY_TARGET` |
| Rasika | `MobileApiConfig` supplied by the native host | Emulator/simulator debug defaults; release HTTPS |
| Compose | Service `environment` blocks and explicitly supplied overrides | Host variables are not automatically forwarded to every service |
| mise | `config/postgres-local.env` | Local tool/session environment; separate from backend `local.env` loading |

The backend API selects `development.env`, `.env.test`, or `.env.production` according to its environment. Do not publish credentials in committed examples. Local environment files are intentionally absent from Git.

Sources: [API loader](../../modules/backend/api/src/main/kotlin/com/sangita/grantha/backend/api/config/ApiEnvironment.kt), [DAL loader](../../modules/backend/dal/src/main/kotlin/com/sangita/grantha/backend/dal/support/DatabaseConfigLoader.kt), [worker settings](../../tools/krithi-extract-enrich-worker/src/config.py), [Compose](../../compose.yaml).

## Database and backend

| Variable | Purpose |
|:---|:---|
| `DB_HOST`, `DB_PORT`, `DB_NAME`, `DB_USER`, `DB_PASSWORD` | Backend/DAL database connection |
| `DB_SCHEMA` | Optional database schema |
| `API_HOST`, `API_PORT` | API listener |
| `ADMIN_TOKEN` | Credential used for JWT token exchange |
| `JWT_SECRET`, `JWT_ISSUER`, `JWT_AUDIENCE`, `JWT_REALM` | JWT signing and verification |
| `TOKEN_TTL_SECONDS` | Access-token lifetime; loader default 86400 |
| `CORS_ALLOWED_ORIGINS` | Accepted browser origins |
| `STORAGE_UPLOAD_DIR`, `STORAGE_PUBLIC_URL` | Upload storage and public URL settings |
| `ADMIN_EMAIL`, `ADMIN_PASSWORD` | Account provisioning command inputs |

The stock Compose services use database host `db`. Host processes use `localhost`. `DATABASE_URL` is the worker connection variable; it is not a replacement for the backend's `DB_*` variables. Likewise, setting a host `DATABASE_URL` does not redirect the Makefile's hardcoded Compose migration service to a remote database.

See [authentication](../00-meta/quick-reference-auth.md) and [migrations](../04-database/migrations.md).

## AI and extraction

| Variable | Consumer / purpose |
|:---|:---|
| `SG_GEMINI_API_KEY` | Backend AI and worker credentials; backend also accepts `GEMINI_API_KEY` |
| `SG_GEMINI_MODEL_URL` | Backend generation endpoint |
| `SG_GEMINI_FALLBACK_MODEL_URL`, `SG_GEMINI_USE_SCHEMA_MODE` | Backend generation options |
| `SG_GEMINI_QPS_LIMIT`, `SG_GEMINI_MAX_CONCURRENT`, `SG_GEMINI_MAX_RETRIES` | Backend request limits |
| `SG_GEMINI_MIN_INTERVAL_MS`, `SG_GEMINI_MAX_RETRY_WINDOW_MS`, `SG_GEMINI_REQUEST_TIMEOUT_MS` | Backend timing/retry settings |
| `SG_GEMINI_MODEL` | Worker enrichment model |
| `SG_ENABLE_GEMINI_ENRICHMENT` | Worker enrichment opt-in; false by default |
| `DATABASE_URL` | Worker PostgreSQL connection |
| `EXTRACTION_POLL_INTERVAL_S`, `EXTRACTION_CACHE_DIR`, `EXTRACTOR_VERSION` | Worker polling, artifacts, evidence version |
| `SG_ENABLE_IDENTITY_DISCOVERY`, `SG_IDENTITY_MIN_SCORE`, `SG_IDENTITY_MAX_COUNT`, `SG_IDENTITY_CACHE_TTL_SECONDS` | Worker identity candidate discovery |

Generation, enrichment, and embedding are separate operations. An embedding profile must agree with the query embedder; changing a generation model variable is not an embedding-index migration. See [search operations](../03-api/search.md).

The stock extraction container receives its database URL, but no Gemini credential/enrichment flag in the committed service definition. Supply intended variables explicitly in an environment-specific deployment override. A backend local environment file is not implicitly available inside the worker.

## Browser and mobile

The [admin client](../../modules/frontend/sangita-admin-web/src/api/client.ts) defaults to `/v1`. Set `VITE_API_BASE_URL` when the browser needs another API base. The Vite `/v1` proxy targets `http://localhost:8080` unless `API_PROXY_TARGET` is set; Compose uses `http://backend:8080`.

There are two existing configuration mismatches to account for:

- Compose sets `VITE_API_URL`, but the client reads `VITE_API_BASE_URL`. The default `/v1` proxy is what makes the normal development path work.
- [vite.config.ts](../../modules/frontend/sangita-admin-web/vite.config.ts) uses `../../config`, which resolves beneath `modules`, rather than repository-root `config/`. It does not automatically load the backend's `config/local.env`.

The Vite config also defines legacy `process.env.*` AI-key values. Treat all frontend-injected values as browser-visible; keep service credentials in server/worker configuration.

[MobileApiConfig](../../modules/shared/mobile-data/src/commonMain/kotlin/com/sangita/grantha/shared/mobile/config/MobileApiConfig.kt) defines Android emulator and iOS simulator debug URLs, timeouts, and HTTPS requirements. Base URLs must be nonblank and have no trailing slash.

## Change a setting

Identify the component and execution context, set only the variables it consumes, restart/rebuild that component as required, then verify behavior through the relevant API or UI. Inspect [logs](./monitoring.md) without dumping local credentials. [config/README.md](../../config/README.md) provides a short local entry point; [tools.yaml](../../config/tools.yaml) is a tooling reference and is not the complete runtime contract.

---

[Section index](./README.md) · [Documentation home](./../README.md) · [Feature status](./../01-requirements/features/README.md)
