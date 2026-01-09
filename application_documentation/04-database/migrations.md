# Database Migrations (Sangita Grantha)

> **Status**: Draft | **Version**: 0.2 | **Last Updated**: 2026-01-09
> **Owners**: Sangita Grantha Data/Backend Team

**Related Documents**
- [Schema](./schema.md)
- Migrations Runner Rust (Deleted)
- [Config](../08-operations/config.md)

# Database Migrations

Sangita Grantha uses a **Rust-based migration tool** (`tools/sangita-cli`) to manage database schema changes. **Flyway is NOT used** in this project.

---

## 1. Conventions

### File Naming

Migration files follow the pattern: `NN__description.sql`

- `NN`: Two-digit sequential number (01, 02, 03, ...)
- `__`: Double underscore separator
- `description`: Lowercase, hyphenated description (e.g., `baseline-schema-and-types`)

Examples:
- `01__baseline-schema-and-types.sql`
- `02__domain-tables.sql`
- `03__constraints-and-indexes.sql`
- `04__import-pipeline.sql`
- `05__sections-tags-sampradaya-temple-names.sql`
- `06__notation-tables.sql`

### Migration Structure

Each migration file should include:

```sql
-- migrate:up
SET search_path TO public;

-- Migration SQL here

-- migrate:down
-- Optional rollback SQL (commented out for safety)
-- DROP TABLE IF EXISTS ...
```

**Note**: Down migrations are typically commented out to prevent accidental data loss. Only uncomment when explicitly needed for rollback.

---

## 2. Current Migration Files

| File | Purpose | Key Entities |
|------|---------|--------------|
| [`01__baseline-schema-and-types.sql`](../../database/migrations/01__baseline-schema-and-types.sql) | Extensions, enum types, foundational tables | `roles`, `audit_log`, enums (workflow_state, language_code, script_code, raga_section, import_status, musical_form) |
| [`02__domain-tables.sql`](../../database/migrations/02__domain-tables.sql) | Primary domain tables | `users`, `composers`, `ragas`, `talas`, `deities`, `temples`, `krithis`, `krithi_ragas`, `krithi_lyric_variants` |
| [`03__constraints-and-indexes.sql`](../../database/migrations/03__constraints-and-indexes.sql) | Constraints, indexes, search optimization | Indexes for search, trigram indexes for lyrics, foreign key constraints |
| [`04__import-pipeline.sql`](../../database/migrations/04__import-pipeline.sql) | Data ingestion tables | `import_sources`, `imported_krithis` |
| [`05__sections-tags-sampradaya-temple-names.sql`](../../database/migrations/05__sections-tags-sampradaya-temple-names.sql) | Sections, tags, sampradaya, temple names | `krithi_sections`, `krithi_lyric_sections`, `tags`, `krithi_tags`, `sampradayas`, `temple_names` |
| [`06__notation-tables.sql`](../../database/migrations/06__notation-tables.sql) | Notation support for Varnams/Swarajathis | `krithi_notation_variants`, `krithi_notation_rows` |

### Enum Types

Defined in `01__baseline-schema-and-types.sql`:

- `workflow_state_enum`: `draft`, `in_review`, `published`, `archived`
- `language_code_enum`: `sa`, `ta`, `te`, `kn`, `ml`, `hi`, `en`
- `script_code_enum`: `devanagari`, `tamil`, `telugu`, `kannada`, `malayalam`, `latin`
- `raga_section_enum`: `pallavi`, `anupallavi`, `charanam`, `other`
- `import_status_enum`: `pending`, `in_review`, `mapped`, `rejected`, `discarded`
- `musical_form_enum`: `KRITHI`, `VARNAM`, `SWARAJATHI` *(added in migration 02)*

---

## 3. Migration Workflow

### Using Sangita CLI

The Rust-based CLI tool (`tools/sangita-cli`) manages migrations:

```bash
cd tools/sangita-cli

# Run pending migrations
cargo run -- db migrate

# Reset database (drop → create → migrate → seed)
cargo run -- db reset

# Check database health
cargo run -- db health
```

### Creating New Migrations

1. **Create migration file** in `database/migrations/`:
   - Use next sequential number (e.g., `07__new-feature.sql`)
   - Follow naming convention: `NN__description.sql`

2. **Write migration SQL**:
   ```sql
   -- migrate:up
   SET search_path TO public;
   
   -- Your migration SQL here
   CREATE TABLE IF NOT EXISTS new_table (...);
   
   -- migrate:down
   -- DROP TABLE IF EXISTS new_table;
   ```

3. **Test migration**:
   ```bash
   cd tools/sangita-cli
   cargo run -- db reset  # Test full reset
   cargo run -- db migrate  # Test incremental migration
   ```

4. **Update documentation**:
   - Update this file with new migration entry
   - Update `SANGITA_SCHEMA_OVERVIEW.md` if schema changes
   - Update `domain-model.md` if entities change

### Migration Best Practices

- ✅ **Always use `IF NOT EXISTS`** for CREATE statements (idempotent)
- ✅ **Use transactions** for multi-step operations
- ✅ **Add indexes** in separate migration or same migration after table creation
- ✅ **Test rollback** before deploying (uncomment down migration temporarily)
- ✅ **Document breaking changes** in migration comments
- ❌ **Never modify existing migrations** that have been applied to production
- ❌ **Never use Flyway** - use Rust migration tool only

### Migration Ordering

Migrations are applied in numerical order. Dependencies between migrations:

1. `01__baseline-schema-and-types.sql` - Must run first (enums, roles, audit_log)
2. `02__domain-tables.sql` - Depends on 01 (uses enums, references roles)
3. `03__constraints-and-indexes.sql` - Depends on 02 (adds constraints to existing tables)
4. `04__import-pipeline.sql` - Depends on 02 (references krithis, users)
5. `05__sections-tags-sampradaya-temple-names.sql` - Depends on 02 (references krithis, krithi_lyric_variants)
6. `06__notation-tables.sql` - Depends on 02, 05 (references krithis, krithi_sections, talas, users)

---

## 4. Schema Evolution

### Adding New Entities

1. Create new migration file
2. Define table with proper constraints
3. Add indexes for search/performance
4. Update domain model documentation
5. Update API contract if needed

### Modifying Existing Tables

1. Create new migration file
2. Use `ALTER TABLE` statements
3. Handle data migration if needed
4. Update indexes if column changes affect search
5. Update documentation

### Adding New Enums

1. Add enum to `01__baseline-schema-and-types.sql` (or create new migration if 01 already applied)
2. Update Kotlin enum in `modules/shared/domain`
3. Update `DbEnums.kt` in DAL
4. Update domain model documentation

---

## 5. Rollback Strategy

- **Down migrations are commented out by default** to prevent accidental data loss
- For rollback, manually uncomment and execute down migration SQL
- **Always backup database** before rollback
- Test rollback in development environment first

---

## 6. Migration Tool Details

See `tools/sangita-cli/README.md` for complete CLI documentation.

The Rust tool:
- Reads migrations from `database/migrations/`
- Applies them in order
- Tracks applied migrations (implementation-dependent)
- Supports reset, migrate, and health check commands
