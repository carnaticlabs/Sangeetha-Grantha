---
description: Verify that the local database schema is in sync with the migration files.
---

1. List the migration files to identify the latest version.

```bash
ls -1 database/migrations | tail -n 5
```

2. Run pending migrations using the Python db-migrate tool via Makefile.

```bash
make migrate
```

3. Query the database directly to confirm the latest migration version matches the file system.

```sql
SELECT * FROM schema_migrations ORDER BY version DESC LIMIT 1;
```

4. Full reset if needed (drop → create → migrate → seed):

```bash
make db-reset
```
