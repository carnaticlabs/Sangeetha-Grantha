# Sangita CLI

| Metadata | Value |
|:---|:---|
| **Status** | Active |
| **Version** | 1.0.0 |
| **Last Updated** | 2026-01-30 |
| **Author** | Sangeetha Grantha Team |

A unified command-line tool for the Sangita Grantha project.

## Prerequisites

**Recommended**: Use [mise](https://mise.jdx.dev/) for toolchain management (see `.mise.toml` in project root).

Tools managed by mise (see [current versions](../../application_documentation/00-meta/current-versions.md)):
- Rust (see `.mise.toml`)
- Java (Temurin, see `.mise.toml`)
- Bun (see `.mise.toml`)
- Docker Compose (latest)

System requirements (not managed by mise):
- Docker Desktop (macOS/Windows) or Docker Engine (Linux)

## Installation

Build the tool from source:
```bash
cd tools/sangita-cli
cargo build --release
```
The binary will be at `target/release/sangita-cli`.

## Usage

### Running via mise (Recommended)

Since Rust and other tools are managed by mise, run sangita-cli through mise:

```bash
# Development workflow
mise exec -- cargo run --manifest-path tools/sangita-cli/Cargo.toml -- dev --start-db

# Database management - Reset existing database database 
mise exec -- cargo run --manifest-path tools/sangita-cli/Cargo.toml -- db reset

# Database management - Applying database schema and seed data changes 
mise exec -- cargo run --manifest-path tools/sangita-cli/Cargo.toml -- db migrate

# Testing
mise exec -- cargo run --manifest-path tools/sangita-cli/Cargo.toml -- test steel-thread

# Setup check
mise exec -- cargo run --manifest-path tools/sangita-cli/Cargo.toml -- setup
```

This ensures:
- ✅ Correct Rust version (per `.mise.toml`)
- ✅ Correct Java version (per `.mise.toml`)
- ✅ Correct Bun version (per `.mise.toml`)
- ✅ Correct Docker Compose version (latest)

### Without mise (Fallback)

If mise is not available, ensure tools are installed manually with correct versions:
```bash
cd tools/sangita-cli
cargo run -- dev --start-db
```

**Note**: You must ensure tool versions match `.mise.toml` requirements manually.

### Setup
Check environment and dependencies:
```bash
# Via mise (recommended)
mise exec -- cargo run --manifest-path tools/sangita-cli/Cargo.toml -- setup

# Or without mise
cd tools/sangita-cli
cargo run -- setup
```

### Commit Guardrails
Validate commit messages and manage Git hooks:
```bash
# Validate a commit message (reads from stdin if --message not provided)
cargo run -- commit check --message "Your commit message here"

# Install Git hooks (commit-msg and pre-commit)
cargo run -- commit install-hooks

# Remove installed Git hooks
cargo run -- commit uninstall-hooks

# Manually scan staged files for sensitive data
cargo run -- commit scan-sensitive
```

**Commit Message Format:**
All commits must include a reference to a documentation file in `application_documentation/`:
```
<subject line>

Ref: application_documentation/01-requirements/features/my-feature.md

<optional body>
```

**Features:**
- ✅ Validates that commit messages reference documentation files
- ✅ Enforces single reference per commit (1:1 mapping)
- ✅ Pre-commit hook scans for sensitive data (API keys, secrets)
- ✅ Fast validation (< 500ms)
- ✅ Works seamlessly with IDEs (VS Code, IntelliJ, etc.)

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

### Documentation Management
Sync version information from source files to documentation:
```bash
# Generate/update current-versions.md
cargo run -- docs sync-versions

# Check if versions are in sync (for CI - exits with error if out of sync)
cargo run -- docs sync-versions --check

# Validate documentation links (future feature)
cargo run -- docs validate-links
```

**Sources of Truth:**
- `gradle/libs.versions.toml` - Backend/Mobile dependencies
- `modules/frontend/sangita-admin-web/package.json` - Frontend dependencies
- `.mise.toml` - Development toolchain versions

**Output:** `application_documentation/00-meta/current-versions.md`

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

The commit guardrails are implemented in `tools/sangita-cli/src/commands/commit.rs`.

### Technical Implementation

**Dependencies**
The implementation uses the following dependencies (already in `Cargo.toml`):
```toml
regex = "1.11.1"  # Commit message parsing and sensitive data pattern matching
clap = { version = "4.5.53", features = ["derive"] }  # CLI argument parsing
```

**Command Structure**
The CLI command structure (`tools/sangita-cli/src/commands/commit.rs`):
```rust
#[derive(Subcommand)]
pub enum CommitCommands {
    /// Validate commit message format and reference
    Check {
        /// Commit message to validate (or read from stdin)
        #[arg(long)]
        message: Option<String>,
    },
    /// Scan staged files for sensitive data (used by pre-commit hook)
    ScanSensitive,
    /// Install Git hooks for commit validation
    InstallHooks,
    /// Remove installed Git hooks
    UninstallHooks,
}
```

**Git Hook Scripts**
When `install-hooks` is run, it creates two hooks:

1. **`.git/hooks/commit-msg`** - Validates commit message format:
```bash
#!/bin/sh
# Sangita Grantha Commit Guardrails Hook
# This hook validates commit messages to ensure they reference documentation

exec "/path/to/sangita-cli" commit check --message "$(cat "$1")"
```

2. **`.git/hooks/pre-commit`** - Scans for sensitive data:
```bash
#!/bin/sh
# Sangita Grantha Pre-commit Hook
# This hook scans staged files for sensitive data

exec "/path/to/sangita-cli" commit scan-sensitive
```

The hooks automatically detect the binary location (release/debug) or fall back to `cargo run` for development.

**Validation Logic**
1. **Extract Reference**: Parses commit message using regex pattern `(?i)ref:\s*(.+?)(?:\n|$)` to find `Ref: <path>`.
2. **Single Reference Check**: Ensures only one reference exists per commit (enforces 1:1 mapping).
3. **File Existence Check**: Verifies that the referenced path exists and is within `application_documentation/` directory.
4. **Path Normalization**: Handles relative paths, paths starting with `application_documentation/`, and normalizes to absolute paths.
5. **Exit Code**: Returns 0 on success, 1 on failure (which blocks the commit in the hook).

**Sensitive Data Scanning**
The `scan-sensitive` command:
- Gets staged files using `git diff --cached --name-only`
- Scans each file for patterns matching:
  - API keys: `api[_-]?key`, `SG_GEMINI_API_KEY`
  - Secrets: `secret`, `password`, `token`
  - AWS credentials: `AWS_ACCESS_KEY_ID`, `AWS_SECRET_ACCESS_KEY`
- Skips binary files (>1MB) and common placeholder patterns
- Provides detailed error messages with file locations and line numbers
