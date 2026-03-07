Reset the database completely (drop, create, migrate, seed).

Run: `make db-reset`

This will:
1. Drop the existing database
2. Create a new database
3. Run all migrations
4. Seed the database with reference data

After reset, verify the database is accessible and seeded correctly by querying a few tables.
