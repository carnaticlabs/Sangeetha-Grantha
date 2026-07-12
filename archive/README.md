# Archive

Archived tools, scripts, and historical documents preserved for reference. Nothing here is active or should be invoked.

## Contents

### tools/

| Directory | What it was | Superseded by |
|-----------|------------|---------------|
| `db-migrate/` | Python database migration tool (ADR-010 era) | Flyway Community ([ADR-013](../application_documentation/02-architecture/decisions/ADR-013-db-migration-with-flyway.md)) |
| `sangita-cli/` | Rust CLI for dev workflows (migrations, dev server, commits) | `make` targets + Flyway ([TRACK-078](../conductor/tracks/TRACK-078-rust-cli-archive.md)) |
| `bootstrap/` | Bootstrap scripts (bash + PowerShell) for initial dev setup | `make dev` + [Getting Started](../application_documentation/00-onboarding/getting-started.md) |
| `one-off-scripts/` | One-time data-fix and testing scripts (`fix_db_garbled.py`, `fix_headers.py`, `test_integration_pipeline.sh`) | Results are in migrations/seeds; CI + Playwright E2E replaced ad-hoc testing |
| `raga-reference-extractor/` | Python script to extract raga reference data from external sources | Output is in `R__seed_04_raga_reference.sql`; [TRACK-091](../conductor/tracks/TRACK-091-comprehensive-raga-reference-data.md) |

### Other

| File | What it was |
|------|------------|
| `handover-track-121.md` | Working handover document for TRACK-121 (Frontend Major Toolchain Upgrade, completed) |
