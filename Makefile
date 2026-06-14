.PHONY: dev dev-down db db-reset seed seed-dev migrate migrate-status bootstrap-admin test test-integration test-frontend steel-thread clean

COMPOSE := docker compose
# Flyway runs as the compose `migrate` service (flyway/flyway image) on the db network.
FLYWAY  := $(COMPOSE) run --rm migrate
PSQL_DB := $(COMPOSE) exec -T db psql -U postgres -v ON_ERROR_STOP=1

# Start full dev stack
dev:
	$(COMPOSE) --profile dev up --build

# Stop dev stack
dev-down:
	$(COMPOSE) --profile dev down

# Start database only
db:
	$(COMPOSE) up -d db

# Reset database: drop -> create -> Flyway migrate (applies V__ schema + R__ reference data)
db-reset:
	$(COMPOSE) up -d --wait db
	$(PSQL_DB) -d postgres \
	  -c "SELECT pg_terminate_backend(pid) FROM pg_stat_activity WHERE datname='sangita_grantha' AND pid<>pg_backend_pid();" \
	  -c "DROP DATABASE IF EXISTS sangita_grantha;" \
	  -c "CREATE DATABASE sangita_grantha;"
	$(FLYWAY)

# Run pending migrations (Flyway: V__ versioned + R__ repeatable reference data)
migrate:
	$(FLYWAY)

# Show migration + repeatable status
migrate-status:
	$(FLYWAY) info

# Dev-only sample content (never a migration, never CI). Reference data arrives via `migrate`.
seed-dev:
	$(PSQL_DB) -d sangita_grantha < database/seed_data/02_sample_data.sql

# Back-compat alias: reference data is now applied by `make migrate`; this loads dev sample only.
seed: seed-dev

# Provision/update the admin user with an argon2id hash (TRACK-114 helper).
# Requires ADMIN_EMAIL and ADMIN_PASSWORD in the environment; connects to DB_HOST (default localhost).
bootstrap-admin:
	./gradlew :modules:backend:api:bootstrapAdmin

# Run backend tests (unit + integration; integration self-provisions Postgres via Testcontainers)
test:
	./gradlew :modules:backend:dal:test :modules:backend:api:test

# Run only the @Tag("integration") tests (DAL D1–D6 suite + api integration)
test-integration:
	./gradlew :modules:backend:dal:integrationTest :modules:backend:api:integrationTest

# Run frontend tests
test-frontend:
	cd modules/frontend/sangita-admin-web && bun test

# Run steel thread E2E test
steel-thread:
	./gradlew :modules:backend:api:test --tests "*SteelThread*"

# Clean everything
clean:
	$(COMPOSE) down -v
