---
description: Start the full stack application (Database, Backend, Frontend) and redirect logs.
---

1. Start the application using the convenience script. This truncates the log files and
   redirects output so `sangita_logs.txt`, `sangita_extraction_logs.txt`, and
   `exposed_queries.log` are always fresh for the current run.

```bash
./start-sangita.sh            # full stack (DB + Backend + Frontend + Extraction)
./start-sangita.sh --no-extraction   # skip the PDF extraction Docker container
```

Alternatively, run the CLI directly (output goes to terminal only, no log files):

```bash
mise exec -- cargo run --manifest-path tools/sangita-cli/Cargo.toml -- dev --start-db
```

2. Wait for the services to initialize. The frontend typically runs on port 5001 and backend on 8080.

3. Log files created:
   - `sangita_logs.txt` — main CLI + backend + frontend output (terminal + file via tee)
   - `sangita_extraction_logs.txt` — PDF extraction Docker container logs
   - `exposed_queries.log` — Exposed SQL queries (written directly by backend Logback)
