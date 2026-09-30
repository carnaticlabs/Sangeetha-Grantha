---
name: debug-start-application
description: Starts the full Sangita Grantha stack (PostgreSQL, Backend API, Frontend Admin Web, Extraction Worker) with output redirected to a log file for diagnostic analysis and checks health endpoints. Use when diagnosing startup crashes, analyzing boot logs, or running background dev servers.
---

# Debug Start Application

This skill starts the complete Sangita Grantha application stack in the background with full log redirection, allowing real-time monitoring and post-startup triage.

## 1. Start Stack with Log Redirection

Start the Docker Compose development profile and stream both stdout and stderr to `sangita_logs.txt`:

```bash
make dev > sangita_logs.txt 2>&1 &
```

## 2. Monitor Startup Stream

Follow the logs until all containers have initialized:

```bash
tail -f sangita_logs.txt
```

Press `Ctrl+C` once the services have initialized.

## 3. Verify Health Endpoints

Confirm that services are operational:

```bash
# Verify backend API (Ktor)
curl -sf http://localhost:8080/health && echo "Backend OK"

# Verify frontend web UI (Vite / Bun)
curl -sf http://localhost:5001 > /dev/null && echo "Frontend OK"

# Check Docker container statuses
docker compose ps
```

## 4. Teardown

To shut down the entire development stack cleanly:

```bash
make dev-down
```
