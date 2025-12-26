# Sangita CLI

A unified command-line tool for the Sangita Grantha project.

## Prerequisites
- Rust (cargo)
- Java 21+
- Bun 1.3+
- Docker (for Postgres)

## Installation
Build the tool from source:
```bash
cd tools/sangita-cli
cargo build --release
```
The binary will be at `target/release/sangita-cli`.

## Usage

### Setup
Check environment and dependencies:
```bash
cargo run -- setup
```

### Database Management
Reset database (Drop → Create → Migrate → Seed):
```bash
cargo run -- db reset
```

Run migrations only:
```bash
cargo run -- db migrate
```

### Development
Start Backend and Frontend servers:
```bash
cargo run -- dev
```

Start the full stack (DB + Backend + Frontend) for manual verification:
```bash
cargo run -- dev --start-db
```
This will:
1. **Clean up existing processes** using the configured ports (database: 5432, backend: 8080, frontend: 5001)
2. Ensure Postgres is running (if `--start-db`)
3. Wait for the backend `/health` endpoint
4. Launch the frontend

Press `Ctrl+C` to gracefully stop all services.

### Network & Mobile Setup
- Show local network info: `cargo run -- net info`
- Write configs for LAN/mDNS/Pi-hole: `cargo run -- net configure --mode ip` (or `--mode mdns|pihole --target <hostname>`)
- Verify local readiness: `cargo run -- net verify`

### Smoke Test
Quick health + search check:
```bash
cargo run -- test upload --base-url http://192.168.0.42:8080
```

### Mobile Docs
Show where the mobile testing guides/checklists live:
```bash
cargo run -- mobile guide
```

### Testing

#### Steel Thread Test
Run the end-to-end smoke verification:
```bash
cargo run -- test steel-thread
```

**What it checks:**
1. Database connectivity + migrations
2. Backend health (`/health`)
3. Krithi search endpoint (`/v1/krithis/search`)
4. Admin audit log access (using `ADMIN_TOKEN`)
5. Frontend dev server startup

**Manual Verification:**
After automated checks pass, the system stays running for manual verification:
- Admin Web: `http://localhost:5001`
- API: `http://0.0.0.0:8080`
- Admin Token: `ADMIN_TOKEN`

Press `Ctrl+C` to stop servers and exit.

## Configuration

Environment variables (set in `.env` file):
- `API_HOST` - Backend host (default: `0.0.0.0`)
- `API_PORT` - Backend port (default: `8080`)
- `FRONTEND_PORT` - Frontend port (default: `5001`)
- `ADMIN_TOKEN` - Bearer token for admin endpoints (default: `dev-admin-token`)
- `TEST_DATA_FILE` - Path to test data JSON (default: `tools/sangita-cli/test_data.json`)

## Troubleshooting

### Backend fails to start
1. Check Gradle logs for compilation errors
2. Verify database is running: `cargo run -- db health`
3. Check if port 8080 is already in use: `lsof -i :8080`
4. Try running backend manually: `./gradlew :modules:backend:api:run`

### Database connection issues
1. Ensure Docker is running (or PostgreSQL is installed)
2. Check database health: `cargo run -- db health`
3. Try starting database: `cargo run -- db start`
4. Review PostgreSQL logs in the configured pg_data directory

### Frontend fails to start
1. Ensure Bun dependencies are installed: `cd modules/frontend/sangita-admin-web && bun install`
2. Check if port 5001 is available: `lsof -i :5001`
3. Try starting frontend manually: `cd modules/frontend/sangita-admin-web && bun run dev`
