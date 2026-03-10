| Metadata | Value |
|:---|:---|
| **Status** | Accepted |
| **Version** | 1.0.0 |
| **Last Updated** | 2026-03-10 |
| **Author** | Sangeetha Grantha Team |
| **Supersedes** | [ADR-003](./ADR-003-database-migrations.md) |

# ADR-010: Migration Tool Course Correction — Rust CLI to Python db-migrate + Makefile

## Context

ADR-003 established a custom Rust CLI (`tools/sangita-cli`) using sqlx as the database migration tool. After 10 months of production use, several pain points emerged:

1. **Toolchain burden**: The Rust CLI required maintaining a separate Rust toolchain alongside Kotlin/JVM (backend), Node/Bun (frontend), and Python (extraction). Four language ecosystems was excessive for the team size.
2. **Compilation overhead**: Rust's compile times (30-60 seconds for incremental builds) created friction for simple migration tasks that should take seconds.
3. **Feature stagnation**: The CLI's database commands worked but saw no feature evolution — rollbacks remained unimplemented, and the CLI's other capabilities (network config, health checks) were rarely used.
4. **Docker Compose already running**: The development workflow already used Docker Compose for PostgreSQL and the backend. Adding migration execution to Docker Compose was natural.
5. **Python already in the stack**: The extraction worker (`tools/krithi-extract-enrich-worker`) already required Python, so Python was not a new dependency.

The Rust CLI was archived as part of TRACK-078 (February 2026).

## Decision

Replace the Rust-based migration CLI with a **Python `db-migrate` tool** (`tools/db-migrate/`) combined with a **Makefile** as the developer workflow interface.

- **Migration execution**: Python script using `psycopg2` for direct SQL execution
- **Developer interface**: `make migrate`, `make db-reset`, `make seed`
- **Container orchestration**: Docker Compose manages PostgreSQL lifecycle
- **Rust CLI**: Archived to `tools/sangita-cli-archived/` (preserved for reference)

## Rationale

1. **Fewer moving parts**: Eliminates the Rust toolchain entirely. The project now uses three language ecosystems (Kotlin, TypeScript, Python) instead of four.
2. **Instant execution**: Python migrations run in under 1 second vs 30+ seconds for Rust compilation + execution.
3. **Makefile as universal interface**: `make migrate` is language-agnostic, self-documenting, and tab-completable. Every developer knows Make.
4. **Docker Compose alignment**: Database lifecycle (start, stop, reset) is managed by Docker Compose, which was already the standard for local development.
5. **Python simplicity**: The migration logic is ~200 lines of straightforward Python — easy to understand, debug, and extend.
6. **Migration files unchanged**: The SQL migration files in `database/migrations/` required zero changes. The `-- migrate:up` / `-- migrate:down` format works identically.

## Implementation Details

### New Tool Structure

```text
tools/db-migrate/
├── db_migrate.py          # Migration runner (psycopg2)
├── requirements.txt       # Python dependencies
└── README.md              # Usage documentation
```

### Makefile Targets

```makefile
make db             # Start PostgreSQL via Docker Compose
make db-reset       # Drop → create → migrate → seed
make migrate        # Run pending migrations only
make seed           # Execute seed data scripts
make clean          # Remove all containers and volumes
```

### Migration File Format (unchanged)

Files in `database/migrations/` follow `NN__description.sql` naming:

```sql
-- migrate:up
SET search_path TO public;
-- SQL statements here

-- migrate:down
-- Rollback SQL (optional)
```

### Current Migrations (38 total as of March 2026)

The migration count grew from 7 (at ADR-003 time) to 38, covering:
- UUID v7 adoption (Migration 37)
- Section structure remediation (Migration 38)
- Curator review workflow tables
- Temple/deity search infrastructure
- Import pipeline enhancements

## Consequences

### Positive

- **Simplified toolchain**: One fewer language ecosystem to maintain
- **Faster iteration**: Sub-second migration execution
- **Better DX**: `make migrate` is simpler than `cargo run -- db migrate`
- **Lower barrier**: Python is more accessible than Rust for the team
- **Docker-native**: Aligns with existing containerized development workflow

### Negative

- **No compile-time SQL verification**: Lost sqlx's compile-time SQL checking (mitigated by migration tests in the backend test suite)
- **Historical knowledge**: New team members may wonder about `tools/sangita-cli-archived/`

### Neutral

- **Migration files**: Identical format, zero migration file changes required
- **Backend unchanged**: Ktor/Exposed backend is unaffected by migration tooling choice

## Follow-up

- ✅ Python db-migrate tool implemented and stable
- ✅ Makefile targets working (migrate, db-reset, seed, clean)
- ✅ Rust CLI archived with README explaining deprecation
- ✅ All documentation updated to reference new tool
- ⏳ Add rollback support to Python tool (planned)

## References

- [Database Migrations Documentation](../../04-database/migrations.md)
- [db-migrate Tool README](../../../tools/db-migrate/README.md)
- [Tech Stack](../tech-stack.md)
- [ADR-003 (Superseded)](./ADR-003-database-migrations.md)
- TRACK-078: Archive Rust CLI
