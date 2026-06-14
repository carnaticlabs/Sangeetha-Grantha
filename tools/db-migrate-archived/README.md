> **⚠️ SUPERSEDED (2026-06-12):** Database migrations are standardized on **Flyway Community** per
> [ADR-013](../../application_documentation/02-architecture/decisions/ADR-013-db-migration-with-flyway.md).
> This tool is retained only until the cutover completes, after which it will be archived to
> `tools/db-migrate-archived/`. Use `make migrate` / `make db-reset` as before — the Makefile is the stable interface.

# db-migrate

A simple Python-based database migration runner for Sangita Grantha.
It applies numerically sorted SQL scripts from `database/migrations/` to the PostgreSQL database.

## Installation

Within the `tools/db-migrate` folder (or referencing it):

```bash
pip install -e .
```

## Usage

```bash
# Apply pending migrations
python -m db_migrate migrate

# Show what would be applied
python -m db_migrate migrate --dry-run

# Drop + create + migrate
python -m db_migrate reset

# Show migration status table
python -m db_migrate status

# Creates next numbered migration file (e.g. 38__add_foo.sql)
python -m db_migrate create "add_foo"
```

## Environment Variables

- `DB_HOST` (default: localhost)
- `DB_PORT` (default: 5432)
- `DB_NAME` (default: sangita_grantha)
- `DB_USER` (default: postgres)
- `DB_PASSWORD` (default: postgres)
- `MIGRATIONS_DIR` (default: database/migrations)
