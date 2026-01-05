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

### Commit Guardrails
Validate commit messages and manage Git hooks:
```bash
cargo run -- commit check --message "Your commit message here"
cargo run -- commit install-hooks
```

Rust command structure:
```rust
// tools/sangita-cli/src/commands/commit.rs
#[derive(Subcommand)]
enum CommitCommands {
    /// Validate commit message format and reference
    Check {
        /// Commit message to validate (or read from stdin)
        #[arg(long)]
        message: Option<String>,
    },
    /// Install Git hooks for commit validation
    InstallHooks,
    /// Remove installed Git hooks
    UninstallHooks,
}
```

Rust wiring points:
```rust
// tools/sangita-cli/src/commands/mod.rs
pub mod commit;

// tools/sangita-cli/src/main.rs
#[derive(Subcommand)]
enum Commands {
    // ... existing commands
    Commit(commit::CommitArgs),
}
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

## Rust-Based Git Guardrails Implementation

The commit guardrails are implemented as a sub-crate within the CLI tool.

### Technical Implementation

**Dependencies**
To support the git operations and validation, `tools/sangita-cli/Cargo.toml` includes:
```toml
git2 = "0.20"  # Git repository access
walkdir = "2"  # Directory traversal
clap = { version = "...", features = ["derive"] } # CLI argument parsing
regex = "1"    # Commit message parsing
```

**Command Structure**
The CLI command enum structure (`tools/sangita-cli/src/commands/commit.rs`):
```rust
#[derive(Subcommand)]
enum CommitCommands {
    /// Validate commit message format and reference
    Check {
        /// Commit message to validate (or read from stdin)
        #[arg(long)]
        message: Option<String>,
    },
    /// Install Git hooks for commit validation
    InstallHooks,
    /// Remove installed Git hooks
    UninstallHooks,
}
```

**Git Hook Script**
When `install-hooks` is run, it places a script like this in `.git/hooks/commit-msg`:
```bash
#!/bin/sh
# .git/hooks/commit-msg

# Get commit message file path
commit_msg_file="$1"

# Call Rust CLI tool to validate
# We use 'cargo run' for dev convenience, simplified for example
# in prod environments this would call the binary directly
cargo run --quiet -- commit check --message "$(cat "$commit_msg_file")" || exit 1
```

**Validation Logic**
1. **Extract Reference**: The tool parses the commit message looking for `Ref: <path>`.
2. **File Check**: It verifies that `<path>` exists and is inside `application_documentation/`.
3. **Exit Code**: Returns 0 on success, 1 on failure (which blocks the commit in the hook).

