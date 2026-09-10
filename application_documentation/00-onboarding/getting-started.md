| Metadata | Value |
|:---|:---|
| **Status** | Active |
| **Version** | 2.1.0 |
| **Last Updated** | 2026-09-10 |
| **Author** | Sangeetha Grantha Team |
| **Document Type** | Current guide |

# Get started locally

---

This guide starts the development stack, explains the first empty catalogue, and points you to the appropriate client. Commands run from the repository root unless a step changes directory.

## 1. Prepare the toolchain

Install Docker Desktop or Docker Engine with Compose and make sure Docker is running. Install mise, then trust and install this repository's tool configuration:

```bash
mise trust
mise install
```

[Current Versions](../00-meta/current-versions.md) lists the toolchain; [`.mise.toml`](../../.mise.toml) supplies the pins. Android Studio/SDK and Xcode are additional requirements only for native mobile work. uv is used for local Python worker development.

The mise configuration references the ignored file `config/postgres-local.env`. On a fresh checkout, create it if it does not exist. For the default local Compose database, its minimal contents are:

```dotenv
DB_HOST=localhost
DB_PORT=5432
DB_NAME=sangita_grantha
DB_USER=postgres
DB_PASSWORD=postgres
```

These credentials describe the local development container. Keep local files out of version control. Existing local values should be preserved. If mise stops before installing because this file is absent, create it first and repeat `mise install`.

Backend overrides can be placed in `config/local.env`, or passed in the process environment. The worker and Vite use different loading rules; see [configuration](../08-operations/config.md).

## 2. Start the stack

```bash
make dev
```

The command runs in the foreground and builds/starts PostgreSQL, Flyway, the Kotlin backend, the React console, and the Python extraction worker. The database is service `db`, not `postgres`. Flyway must complete successfully before the backend and worker start.

| Service | Local address / role |
|:---|:---|
| Curator Console | [http://localhost:5001](http://localhost:5001) |
| Backend | [http://localhost:8080](http://localhost:8080) |
| PostgreSQL | `localhost:5432`, database `sangita_grantha` |
| Extraction worker | Database queue consumer; no browser page |

Use another terminal to inspect readiness:

```bash
docker compose ps
curl --fail http://localhost:8080/health
curl --fail 'http://localhost:8080/v2/catalogue/krithis?page=0&pageSize=5'
```

An empty `items` array can be a successful first run. Flyway loads reference data, not the composition corpus. Use the [import workflow](../01-requirements/features/bulk-import/README.md), or `make seed-dev` for optional development sample content. Public results also depend on publication state and catalogue version.

## 3. Provision and sign in

`make bootstrap-admin` requires `ADMIN_EMAIL` and `ADMIN_PASSWORD` in the invoking environment. It creates or updates the account, hashes the password with argon2id, and assigns the admin role.

The current console login uses an **admin token and existing email** to obtain a JWT. It does not use the provisioned password as an interactive login credential. Follow the [authentication reference](../00-meta/quick-reference-auth.md), including refresh and role behavior.

## 4. Run services individually

For host-based backend/frontend development:

```bash
make db
make migrate
./gradlew :modules:backend:api:run
```

In a separate terminal:

```bash
cd modules/frontend/sangita-admin-web
bun install --frozen-lockfile
bun run dev
```

Do not run a second backend or frontend on the same ports as an existing Compose instance. `API_PROXY_TARGET` chooses Vite's backend destination; `VITE_API_BASE_URL` overrides the browser client's `/v1` base.

For local extraction tooling, follow the [worker README](../../tools/krithi-extract-enrich-worker/README.md). Normal catalogue reads do not require Gemini credentials. Optional enrichment and vector indexing have separate prerequisites.

## 5. Build Rasika

```bash
make test-mobile
make mobile-android
make mobile-ios
```

Android emulator debug traffic uses `http://10.0.2.2:8080`; the iOS simulator uses `http://127.0.0.1:8080`. Physical devices need an explicitly reachable server address. Release transport requires HTTPS. Rasika builds separately from `make dev` and reads `/v2/catalogue`.

A build is not a device journey. Use the [mobile guide](../05-frontend/mobile/README.md) for native test commands and the outstanding acceptance gate.

## 6. Stop, update, and verify

```bash
make dev-down
```

After backend/shared Kotlin or extraction-worker code changes, restart the Compose stack to serve the current implementation. Database schema updates use `make migrate`; reference seeds arrive through repeatable migrations.

| Changed area | Matching check |
|:---|:---|
| Backend | `make test` (includes database-backed tests; Docker required) |
| Backend integration | `make test-integration` |
| Admin web | `make test-frontend`; module `bun run typecheck` and `bun run build` |
| Shared mobile | `make test-mobile` |
| Worker | Ruff, mypy, pytest from the [worker README](../../tools/krithi-extract-enrich-worker/README.md) |
| Documentation | `make check-docs` |

`make db-reset` drops the local database before rebuilding schema/reference data. `make clean` removes Compose volumes. Use them only when that data loss is intended. For an older PostgreSQL volume or a restored corpus, use the [database runbook](../08-operations/runbooks/database-runbook.md).

## Where to go next

[IDE setup](./ide-setup.md) · [Troubleshooting](./troubleshooting.md) · [Architecture](../02-architecture/README.md) · [Feature map](../01-requirements/features/README.md) · [Repository rules](../../CLAUDE.md)

---

[Section index](./README.md) · [Documentation home](./../README.md) · [Feature status](./../01-requirements/features/README.md)
