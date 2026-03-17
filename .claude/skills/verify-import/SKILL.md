# Verify Import Pipeline

1. Check PostgreSQL is running and accessible
2. Run the import script and capture logs
3. Query the database for record counts, duplicates, and unresolved references
4. Verify junction tables are populated (not just FK columns)
5. Spot-check 3 random records through the API endpoint
6. Report: total imported, duplicates found, unresolved references, junction table gaps
