---
description: Verify that the local database schema is in sync with the migration files.
---

1. List the migration files to identify the latest version.

```bash
ls -1 database/migrations | tail -n 5
```

2. Run the `sangita-cli` migration command to ensure all pending migrations are applied.

```bash
cargo run --manifest-path tools/sangita-cli/Cargo.toml -- db migrate
```

3. Query the database directly to confirm the latest migration version matches the file system.

```sql
SELECT * FROM _sqlx_migrations ORDER BY version DESC LIMIT 1;
```
