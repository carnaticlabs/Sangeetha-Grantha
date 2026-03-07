.PHONY: dev dev-down db db-reset seed migrate migrate-status test test-frontend steel-thread clean

PYTHON := mise exec -- python
DB_MIGRATE := PYTHONPATH=tools/db-migrate $(PYTHON) -m db_migrate
PSQL := PGPASSWORD=postgres psql -h localhost -U postgres -d sangita_grantha

# Start full dev stack
dev:
	docker compose --profile dev up --build

# Stop dev stack
dev-down:
	docker compose --profile dev down

# Start database only
db:
	docker compose up -d db

# Reset database (drop + create + migrate)
db-reset:
	$(DB_MIGRATE) reset

# Seed reference data
seed:
	@for f in database/seed_data/*.sql; do echo "Seeding $$f..."; $(PSQL) -f "$$f"; done

# Run pending migrations
migrate:
	$(DB_MIGRATE) migrate

# Show migration status
migrate-status:
	$(DB_MIGRATE) status

# Run backend tests
test:
	./gradlew :modules:backend:api:test

# Run frontend tests
test-frontend:
	cd modules/frontend/sangita-admin-web && bun test

# Run steel thread E2E test
steel-thread:
	./gradlew :modules:backend:api:test --tests "*SteelThread*"

# Clean everything
clean:
	docker compose down -v
