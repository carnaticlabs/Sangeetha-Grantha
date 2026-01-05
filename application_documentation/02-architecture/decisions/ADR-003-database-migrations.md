# ADR-003: Database Migration Tool Choice - Rust vs Flyway

> **Status**: Accepted | **Version**: 1.0 | **Last Updated**: 2026-01-27
> **Owners**: Platform Team

**Related Documents**
- [Database Migrations](../../04-database/migrations.md)
- [Sangita CLI README](../../../tools/sangita-cli/README.md)
- [Tech Stack](../tech-stack.md)

## Context

Sangita Grantha uses PostgreSQL as the primary database and requires a migration tool to manage schema evolution. The platform needed to choose between:

1. **Flyway**: Industry-standard Java-based migration tool (fits well with Kotlin/JVM backend)
2. **Rust Migration Tool**: Custom Rust CLI (`tools/sangita-cli`) using sqlx for migrations

The migration tool needs to:
- Manage sequential SQL migration files
- Support local development workflows
- Integrate with the Rust CLI toolchain (`sangita-cli`)
- Provide database lifecycle management (init, reset, migrate, seed)
- Work seamlessly with PostgreSQL

## Decision

Choose **Rust-based migration tool** (`tools/sangita-cli`) using **sqlx** for database migrations.

**Flyway is explicitly NOT used** in this project.

The migration tool is implemented as part of the unified Sangita CLI (`tools/sangita-cli`) which also handles:
- Database lifecycle (init, reset, start, stop)
- Development environment setup
- Test workflows
- Network configuration

## Rationale

The decision was driven by several factors:

1. **Unified Tooling**: Migration tool is part of the Sangita CLI, providing a single interface for all database operations
2. **Rust Performance**: Rust provides fast, reliable database operations without JVM overhead
3. **CLI Integration**: Migrations are seamlessly integrated with database reset, seed, and health check commands
4. **Sqlx Reliability**: Sqlx provides compile-time SQL verification and excellent PostgreSQL support
5. **Cross-Platform**: Rust CLI works consistently across macOS, Linux, and Windows
6. **Simplified Workflow**: `cargo run -- db migrate` is simpler than Gradle + Flyway setup

**Flyway Rejected** because:
- Additional dependency on JVM stack (backend already uses JVM)
- Less integration with unified CLI workflow
- More complex configuration for local development
- Team preference for Rust tooling consistency

## Implementation Details

### Migration Tool Structure

The migration tool is part of `tools/sangita-cli`:

```
tools/sangita-cli/
├── src/
│   ├── commands/
│   │   └── db.rs              # Database commands (migrate, reset, init, etc.)
│   ├── database/
│   │   └── manager.rs         # DatabaseManager with migration logic
│   └── main.rs                # CLI entry point
├── Cargo.toml                 # Rust dependencies (sqlx, clap, etc.)
└── README.md                  # CLI documentation
```

### Migration File Format

Migrations live in `database/migrations/` with naming convention `NN__description.sql`:

- `01__baseline-schema-and-types.sql`
- `02__domain-tables.sql`
- `03__constraints-and-indexes.sql`
- `04__import-pipeline.sql`
- `05__sections-tags-sampradaya-temple-names.sql`
- `06__notation-tables.sql`
- `07__add-approved-import-status.sql`

Each migration file follows this structure:
```sql
-- migrate:up
SET search_path TO public;

-- Migration SQL here

-- migrate:down
-- Optional rollback SQL (commented out by default)
```

### CLI Commands

```bash
# Run migrations
cargo run -- db migrate

# Reset database (drop → create → migrate → seed)
cargo run -- db reset

# Initialize database (create → migrate → seed)
cargo run -- db init

# Check database health
cargo run -- db health

# Start/Stop PostgreSQL instance (local)
cargo run -- db start
cargo run -- db stop
```

### Current Migration Files

| File | Purpose | Key Entities |
|------|---------|--------------|
| `01__baseline-schema-and-types.sql` | Extensions, enum types, foundational tables | `roles`, `audit_log`, enums |
| `02__domain-tables.sql` | Primary domain tables | `users`, `composers`, `ragas`, `talas`, `krithis` |
| `03__constraints-and-indexes.sql` | Constraints, indexes, search optimization | Indexes, foreign keys |
| `04__import-pipeline.sql` | Data ingestion tables | `import_sources`, `imported_krithis` |
| `05__sections-tags-sampradaya-temple-names.sql` | Sections, tags, sampradaya | `krithi_sections`, `tags`, `sampradayas` |
| `06__notation-tables.sql` | Notation support | `krithi_notation_variants`, `krithi_notation_rows` |
| `07__add-approved-import-status.sql` | Import status enhancements | Import status enum updates |

### Current Implementation Status

✅ **Completed**:
- Rust migration tool implemented using sqlx
- Migration file format and conventions established
- CLI commands for migrate, reset, init, health
- Integration with database lifecycle management
- Migration tracking and application

🔄 **In Progress**:
- Migration rollback support (down migrations currently commented out)

📋 **Planned**:
- Migration validation and testing automation
- Migration dependency checking

## Consequences

### Positive

- **Unified Workflow**: Single CLI tool for all database operations
- **Fast Execution**: Rust provides fast, reliable migrations
- **Good DX**: Simple commands (`cargo run -- db migrate`)
- **Cross-Platform**: Works consistently across operating systems
- **Compile-Time Safety**: Sqlx verifies SQL at compile time
- **Integrated Tooling**: Migrations are part of the broader CLI ecosystem

### Negative

- **Rust Dependency**: Requires Rust toolchain (in addition to Java/Kotlin)
- **Custom Implementation**: Less standard than Flyway (mitigated by good documentation)
- **Learning Curve**: Team members need to understand Rust CLI structure (minimal, well-documented)

### Neutral

- **Backend Stack**: Backend remains Kotlin/JVM (migrations are separate tool)
- **Migration Files**: SQL migration files are standard (compatible with other tools if needed)

## Follow-up

- ✅ Migration tool implemented and stable
- ✅ CLI commands working (migrate, reset, init, health)
- ⏳ Add migration validation tests (planned)
- ⏳ Document rollback procedures (down migrations) (planned)

## References

- [Database Migrations Documentation](../../04-database/migrations.md)
- [Sangita CLI README](../../../tools/sangita-cli/README.md)
- [Tech Stack](../tech-stack.md)
- [Database Schema](../../04-database/schema.md)
