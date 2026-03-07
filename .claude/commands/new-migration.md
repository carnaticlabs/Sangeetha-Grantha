Create a new database migration. The user will describe what the migration should do: $ARGUMENTS

1. List existing migrations in `database/migrations/` to determine the next version number
2. Create the SQL migration file: `database/migrations/V<next>__<description>.sql`
3. Write the SQL (both up migration in the file)
4. Update the Exposed table definitions in `modules/backend/dal/` to match
5. Update `application_documentation/04-database/schema.md` if schema changed
6. Remind to run `make migrate` to apply the migration
