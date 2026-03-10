---
description: Start the full stack application (Database, Backend, Frontend, Extraction) via Docker Compose.
---

1. Start the application using the Makefile. This runs Docker Compose with the dev profile,
   starting PostgreSQL, running migrations, then launching the backend, frontend, and extraction worker.

```bash
make dev                     # full stack (DB + Backend + Frontend + Extraction)
```

Alternatively, start only the database:

```bash
make db                      # database only on port 5432
```

2. Wait for the services to initialize:
   - Database: `localhost:5432`
   - Backend API: `localhost:8080`
   - Frontend: `localhost:5001`

3. Stop the stack when done:

```bash
make dev-down                # stop all services
```
