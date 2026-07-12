# Sangita CLI

Rust-based CLI for database management and development workflows.

## Quick Reference
```bash
cargo run -- dev --start-db      # Full stack: DB + Backend + Frontend
cargo run -- db reset            # Drop → Create → Migrate → Seed
cargo run -- db migrate          # Run pending migrations
cargo run -- test steel-thread   # End-to-end smoke test
cargo run -- docs sync-versions  # Sync version docs
```

## Key Rules
- This is the ONLY migration tool — never use Flyway or Liquibase
- Migration files: `database/migrations/V###__description.sql`
- Uses SQLx for schema migrations
- Config read from `config/application.<env>.toml`
