---
name: start-application
description: Starts the full Sangita Grantha stack (PostgreSQL, Backend API, Frontend Admin Web, Extraction Worker) or individual services using Docker Compose via the Makefile. Use when starting development environments or verifying local services.
---

# Start Application

This skill controls starting and stopping local Sangita Grantha services using the unified Docker Compose configuration and Makefile targets.

## 1. Full Stack Startup

To launch the complete application stack (PostgreSQL, Flyway migrations, Backend API, Frontend Admin Web, and Python Extraction Worker):

```bash
make dev
```

### Active Service Endpoints:
- **PostgreSQL**: `localhost:5432` (database: `sangita_grantha`)
- **Backend API**: `http://localhost:8080` (health: `http://localhost:8080/health`)
- **Frontend Admin Web**: `http://localhost:5001`
- **Extraction Worker**: Background task runner

---

## 2. Start Database Only

If developing backend or frontend services natively on the host machine:

```bash
make db
```

---

## 3. Verify Service Health

```bash
# Check backend health check
curl -sf http://localhost:8080/health && echo "Backend OK"

# Check frontend accessibility
curl -sf http://localhost:5001 > /dev/null && echo "Frontend OK"

# View running container status
docker compose ps
```

---

## 4. Teardown / Stop Services

To stop all running services and network containers:

```bash
make dev-down
```
