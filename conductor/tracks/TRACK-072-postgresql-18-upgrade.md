| Metadata | Value |
|:---|:---|
| **Track ID** | TRACK-072 |
| **Title** | PostgreSQL 18 Upgrade (15 → 18.3) |
| **Status** | Active |
| **Created** | 2026-02-28 |
| **Author** | Sangeetha Grantha Team |

# TRACK-072: PostgreSQL 18 Upgrade

## Objective

Upgrade PostgreSQL from version 15 to 18.3 across all environments (local Docker, staging Cloud SQL, production Cloud SQL).

## Motivation

- PostgreSQL 18 was released September 2025 with significant improvements:
  - New I/O subsystem with up to 3× read performance gains
  - Virtual generated columns (compute at query time)
  - `uuidv7()` for better UUID indexing and read performance
  - OAuth 2.0 authentication support
  - Skip scan for multicolumn B-tree indexes
  - Temporal constraints (PRIMARY KEY, UNIQUE, FOREIGN KEY over ranges)
- PostgreSQL 15 reaches end of community support November 2027; upgrading early avoids future pressure
- JDBC driver 42.7.9 already supports PostgreSQL 18

## Scope

### Files Changed

| File | Change |
|------|--------|
| `compose.yaml` | Docker image `postgres:15` → `postgres:18`; volume mount path updated for PG 18 layout |
| `application_documentation/08-operations/deployment.md` | Cloud SQL `POSTGRES_15` → `POSTGRES_18` |
| `application_documentation/08-operations/runbooks/database-runbook.md` | Version table 15 → 18 |
| `application_documentation/02-architecture/tech-stack.md` | PostgreSQL 15+ → 18+ |
| `.cursorrules` | PostgreSQL 15+ → 18+ |
| `CLAUDE.md` | PostgreSQL 15+ → 18+ |
| `conductor/tracks.md` | Add TRACK-072 entry |
| `database/migrations/37__pg18_uuidv7_defaults.sql` | Switch all 27 UUID PK defaults from `gen_random_uuid()` to `uuidv7()` |

### Not Changed (No Action Needed)

- `gradle/libs.versions.toml` — JDBC driver 42.7.9 is compatible with PG 18
- `.mise.toml` — No PostgreSQL server tool managed by mise (Docker handles it)
- `tools/sangita-cli` — Uses sqlx with no hard-coded PG version constraints
- Migration files — All existing migrations use standard SQL compatible with PG 18

## Migration Notes (Local Development)

After pulling these changes, developers must recreate their local database volume:

```bash
# Stop and remove old container + volume
docker compose down -v

# Start with new PG 18 image and re-seed
mise exec -- cargo run --manifest-path tools/sangita-cli/Cargo.toml -- dev --start-db
```

## Risk Assessment

- **Low risk**: No breaking SQL changes between PG 15 and PG 18
- **JDBC driver**: 42.7.9 fully supports PG 18
- **Exposed ORM**: No PG-version-specific code in our DAL
- **Cloud SQL**: `POSTGRES_18` is available on GCP Cloud SQL

## Progress Log

| Date | Description |
|------|-------------|
| 2026-02-28 | Created track, updated all version references |
| 2026-02-28 | Added migration 37: switch 27 tables from gen_random_uuid() to uuidv7() |
| 2026-02-28 | Fixed compose.yaml volume mount for PG 18 data directory layout change (`/data` → parent dir) |
