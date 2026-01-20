# Cross-Platform Development Environment Standardisation

> **Status**: Implemented | **Version**: 1.0 | **Last Updated**: 2026-01-14  
> **Target**: Consistent, reproducible development environments across macOS, Linux, and Windows  
> **Philosophy**: "One command to rule them all" — eliminate environment setup friction

## 1. Executive Summary

This document outlines the **requirement for Cross-Platform Development Environment Standardisation** in the Sangeetha Grantha project. As a multi-platform monorepo with diverse technology stacks (Kotlin Multiplatform, React, Rust, PostgreSQL), ensuring consistent development environments across different operating systems is critical for team productivity, CI/CD reliability, and onboarding efficiency.

**Core Requirement**: All developers, regardless of their operating system (macOS, Linux, or Windows), must be able to set up a fully functional development environment with a single command, using identical toolchain versions and configuration.

---

## 2. Problem Statement

### 2.1 The Multi-Platform Challenge

Sangeetha Grantha is a complex monorepo requiring multiple toolchains:

| Component | Technology | Version Requirements |
|-----------|-----------|---------------------|
| **Backend** | Kotlin + Ktor | Java 25+ (JVM toolchain) |
| **Mobile** | Kotlin Multiplatform | Java 25+ (Android), Xcode (iOS) |
| **Frontend** | React + TypeScript | Bun 1.3.0 (package manager) |
| **CLI Tools** | Rust | Rust 1.92.0 (compiler) |
| **Database** | PostgreSQL | PostgreSQL 15+ |
| **Build System** | Gradle | Gradle wrapper (included) |

### 2.2 Current Pain Points

#### 2.2.1 OS-Specific Toolchain Installation

**macOS Developers:**
- Use Homebrew to install Java, Rust, Bun
- Different Homebrew versions lead to different tool versions
- System Java conflicts with project requirements
- Manual PATH configuration required

**Linux Developers:**
- Use system package managers (apt/yum/pacman)
- System packages often provide outdated versions
- Requires manual repository configuration for newer versions
- Different distributions have different package names

**Windows Developers:**
- Manual download and installation of each tool
- No unified package manager (unless using Chocolatey/Winget)
- PATH configuration is error-prone
- Different installation paths across systems

**Impact**: Developers spend 2-4 hours on initial setup, with frequent version mismatches causing "works on my machine" issues.

#### 2.2.2 Configuration File Inconsistencies

**Problem**: Environment configuration scattered across multiple files with inconsistent naming:

- Backend expects: `config/application.local.toml`
- Frontend expects: `.env` or `config/.env.development`
- Database connection strings vary by developer
- API endpoints don't match between services
- Sensitive keys accidentally committed to version control

**Impact**: 
- New developers create incorrect config files
- Services fail to communicate (wrong API URLs)
- Database connection errors due to mismatched credentials
- Security risks from committed secrets

#### 2.2.3 Database Setup Variations

**Problem**: PostgreSQL setup differs across platforms:

- macOS: Homebrew PostgreSQL vs Docker Compose
- Linux: System PostgreSQL vs Docker Compose
- Windows: Manual PostgreSQL installation vs Docker Desktop

**Impact**:
- Different database versions (12, 13, 14, 15)
- Port conflicts (5432 already in use)
- Migration failures due to version incompatibilities
- Inconsistent data between developers

#### 2.2.4 Onboarding Friction

**Problem**: New team members face a steep learning curve:

1. **Discovery Phase** (30-60 minutes)
   - Finding which tools are needed
   - Determining correct versions
   - Locating setup documentation

2. **Installation Phase** (1-2 hours)
   - Installing Java, Rust, Bun, PostgreSQL
   - Resolving version conflicts
   - Fixing PATH issues

3. **Configuration Phase** (30-60 minutes)
   - Creating environment files
   - Setting up database
   - Configuring API endpoints

4. **Verification Phase** (30-60 minutes)
   - Running migrations
   - Starting services
   - Debugging connection issues

**Total Time**: 2.5-4.5 hours per developer

**Impact**:
- Delayed project contributions
- Frustration and reduced morale
- Inconsistent environments leading to bugs
- Support burden on experienced developers

### 2.3 Business Impact

#### 2.3.1 Developer Productivity Loss

- **Setup Time**: 2-4 hours per developer × N developers = significant time loss
- **Debugging Time**: 1-2 hours per week per developer troubleshooting environment issues
- **Context Switching**: Developers lose focus when dealing with environment problems

#### 2.3.2 CI/CD Reliability Issues

- **Local vs CI Mismatches**: Tests pass locally but fail in CI due to version differences
- **Flaky Tests**: Environment-specific bugs that are hard to reproduce
- **Deployment Risk**: Production environments may differ from local setups

#### 2.3.3 Team Velocity Impact

- **Slower Onboarding**: New team members take longer to become productive
- **Knowledge Silos**: Only certain developers know how to fix environment issues
- **Reduced Collaboration**: Developers avoid switching between projects due to setup overhead

---

## 3. Requirements

### 3.1 Functional Requirements

#### FR-1: Single-Command Setup
**Requirement**: A single command must set up the complete development environment.

**Acceptance Criteria**:
- Works on macOS, Linux, and Windows
- Installs all required toolchains (Java, Rust, Bun)
- Creates canonical configuration files
- Starts required services (PostgreSQL)
- Runs database migrations
- Verifies environment correctness

**Priority**: **P0 (Critical)**

#### FR-2: Version Pinning
**Requirement**: All toolchain versions must be explicitly pinned and enforced.

**Acceptance Criteria**:
- Version specifications in version-controlled configuration file
- Automatic installation of correct versions
- Verification step confirms versions match requirements
- Clear error messages if versions don't match

**Priority**: **P0 (Critical)**

#### FR-3: Cross-Platform Compatibility
**Requirement**: Setup process must work identically on macOS, Linux, and Windows.

**Acceptance Criteria**:
- Same command works on all platforms
- Same configuration file structure
- Same database setup (Docker Compose)
- Same verification steps

**Priority**: **P0 (Critical)**

#### FR-4: Idempotent Operations
**Requirement**: Setup script must be safe to run multiple times.

**Acceptance Criteria**:
- Running setup twice doesn't break environment
- Existing configurations are preserved
- Services are started only if not already running
- Dependencies are installed only if missing

**Priority**: **P1 (High)**

#### FR-5: Configuration Management
**Requirement**: Environment configuration must be standardized and template-based.

**Acceptance Criteria**:
- Template files in version control
- Generated files are gitignored
- Sensitive data never committed
- Clear documentation of required vs optional variables

**Priority**: **P1 (High)**

### 3.2 Non-Functional Requirements

#### NFR-1: Setup Time
**Requirement**: Complete environment setup must complete in under 15 minutes.

**Target**: 5-10 minutes for typical setup
**Measurement**: Time from running setup command to verified working environment

**Priority**: **P1 (High)**

#### NFR-2: Error Handling
**Requirement**: Setup script must provide clear, actionable error messages.

**Acceptance Criteria**:
- Errors identify the specific problem
- Errors suggest solutions
- Errors don't leave environment in broken state
- Graceful degradation when optional tools are missing

**Priority**: **P1 (High)**

#### NFR-3: Documentation
**Requirement**: Setup process must be fully documented.

**Acceptance Criteria**:
- README includes setup instructions
- Troubleshooting guide available
- Architecture documentation explains design decisions
- Examples provided for common scenarios

**Priority**: **P2 (Medium)**

#### NFR-4: Maintainability
**Requirement**: Setup configuration must be easy to update.

**Acceptance Criteria**:
- Single file for toolchain versions
- Template-based configuration
- Clear separation of concerns
- Version changes require minimal updates

**Priority**: **P2 (Medium)**

### 3.3 Technical Constraints

#### TC-1: Toolchain Version Manager
**Constraint**: Must use a cross-platform toolchain version manager.

**Options Considered**:
- **mise** (formerly rtx): ✅ Cross-platform, fast, simple
- **asdf**: ⚠️ Slower, more complex
- **nvm/rbenv/etc.**: ❌ Language-specific, not suitable for multi-language project

**Decision**: Use **mise** for unified toolchain management.

#### TC-2: Database Setup
**Constraint**: Must use Docker Compose for database to ensure consistency.

**Rationale**:
- Same PostgreSQL version across all platforms
- No system-level installation conflicts
- Easy to reset and recreate
- Matches production containerization strategy

#### TC-3: Configuration Format
**Constraint**: Must support both TOML (backend) and `.env` (frontend) formats.

**Solution**: Generate both formats from canonical templates.

---

## 4. Solution Overview

### 4.1 Architecture

The standardisation solution consists of three core components:

1. **Toolchain Version Manager (mise)**
   - Single `.mise.toml` file pins all tool versions
   - Cross-platform installation and activation
   - Automatic PATH management

2. **Canonical Configuration Templates**
   - Template files in `tools/bootstrap-assets/env/`
   - Generated files in `config/` (gitignored)
   - Single source of truth for environment variables

3. **Bootstrap Scripts**
   - `tools/bootstrap` (Unix/Linux/macOS)
   - `tools/bootstrap.ps1` (Windows)
   - Automated setup workflow

### 4.1.1 mise (Toolchain Version Manager)

**What it is**: A cross-platform toolchain version manager (successor to rtx/asdf) that automatically installs and manages tool versions.

**Why mise**:
- ✅ Works on macOS, Linux, and Windows (PowerShell)
- ✅ Fast (written in Rust)
- ✅ Simple configuration (single `.mise.toml` file)
- ✅ Automatic activation in shells
- ✅ No language-specific package managers needed

**Configuration**: `.mise.toml` at project root

```toml
[tools]
java = "temurin-25"      # Matches Gradle toolchain requirement
rust = "1.92.0"          # Matches tools/sangita-cli/rust-toolchain.toml
bun = "1.3.0"            # Frontend package manager
```

**Usage**:
```bash
# Install mise (one-time, per developer)
curl https://mise.run | sh  # macOS/Linux
# Windows: Use winget or download from https://mise.jdx.dev/

# In project directory
mise install              # Installs all tools from .mise.toml
mise activate            # Activates tools in current shell
```

### 4.1.2 Canonical Local Config Files

**Problem**: Environment variables scattered across multiple files, inconsistent naming, risk of committing secrets.

**Solution**: Single source of truth for configuration templates.

**Structure**:
```
tools/bootstrap-assets/
└── env/
    └── env.development.example    # Template (committed to git)
config/
└── .env.development               # Actual config (gitignored)
```

**Template File**: `tools/bootstrap-assets/env/env.development.example`
- Contains all required environment variables
- Includes safe defaults for development
- Documents optional vs required variables
- No sensitive data (API keys commented out)

**Generated File**: `config/.env.development`
- Created automatically by bootstrap script
- Gitignored (never committed)
- Developer can customize without affecting others
- Used by both backend (Ktor) and frontend (Vite)

**Key Variables**:
```bash
# Frontend
VITE_API_BASE_URL=http://localhost:8080

# Backend
API_HOST=0.0.0.0
API_PORT=8080
ADMIN_TOKEN=dev-admin-token

# Database (Docker Compose defaults)
DB_HOST=localhost
DB_PORT=5432
DB_NAME=sangita_grantha
DB_USER=postgres
DB_PASSWORD=postgres

# Optional: Gemini AI
# SG_GEMINI_API_KEY=your-api-key-here
```

### 4.1.3 Bootstrap Scripts

**Purpose**: Automated, one-command setup that works identically across all platforms.

**Unix/Linux/macOS**: `tools/bootstrap` (bash)
- Detects and uses mise if available
- Falls back to system tools with warnings
- Creates canonical config files from templates
- Starts Docker Compose PostgreSQL
- Builds Rust CLI tool
- Runs database migrations and seeds
- Installs frontend dependencies

**Windows**: `tools/bootstrap.ps1` (PowerShell)
- Same functionality as bash script
- PowerShell-native path handling
- Compatible with Windows Docker Desktop

**Workflow**:
```bash
# Unix/Linux/macOS
./tools/bootstrap

# Windows
powershell -ExecutionPolicy Bypass -File .\tools\bootstrap.ps1
```

**What it does** (in order):
1. ✅ Installs toolchain via mise (Java 25, Rust 1.92.0, Bun 1.3.0)
2. ✅ Verifies Docker + Docker Compose availability
3. ✅ Creates `config/.env.development` from template (if missing)
4. ✅ Starts PostgreSQL 15 via Docker Compose
5. ✅ Builds `sangita-cli` Rust tool
6. ✅ Runs database reset (drop → create → migrate → seed)
7. ✅ Installs frontend dependencies (bun install)

**Post-Bootstrap**:
```bash
# Start full dev stack
cargo run --manifest-path tools/sangita-cli/Cargo.toml -- dev --start-db

# Or run services separately
./gradlew :modules:backend:api:run
cd modules/frontend/sangita-admin-web && bun run dev
```

### 4.2 Workflow

```
┌─────────────────────────────────────────────────────────────┐
│ Developer runs: ./tools/bootstrap (or bootstrap.ps1)        │
└───────────────────────┬─────────────────────────────────────┘
                        │
                        ▼
┌─────────────────────────────────────────────────────────────┐
│ 1. Check for mise (toolchain manager)                       │
│    ├─ If present: Install tools from .mise.toml             │
│    └─ If missing: Warn and use system tools                 │
└───────────────────────┬─────────────────────────────────────┘
                        │
                        ▼
┌─────────────────────────────────────────────────────────────┐
│ 2. Verify Docker + Docker Compose                          │
│    └─ Exit with error if missing                            │
└───────────────────────┬─────────────────────────────────────┘
                        │
                        ▼
┌─────────────────────────────────────────────────────────────┐
│ 3. Create config files from templates                       │
│    ├─ config/.env.development (if missing)                  │
│    └─ Preserve existing files (idempotent)                  │
└───────────────────────┬─────────────────────────────────────┘
                        │
                        ▼
┌─────────────────────────────────────────────────────────────┐
│ 4. Start PostgreSQL via Docker Compose                      │
│    └─ Wait for health check                                 │
└───────────────────────┬─────────────────────────────────────┘
                        │
                        ▼
┌─────────────────────────────────────────────────────────────┐
│ 5. Build Rust CLI tool (sangita-cli)                        │
└───────────────────────┬─────────────────────────────────────┘
                        │
                        ▼
┌─────────────────────────────────────────────────────────────┐
│ 6. Run database migrations and seed data                    │
│    └─ cargo run -- db reset                                 │
└───────────────────────┬─────────────────────────────────────┘
                        │
                        ▼
┌─────────────────────────────────────────────────────────────┐
│ 7. Install frontend dependencies                            │
│    └─ bun install                                           │
└───────────────────────┬─────────────────────────────────────┘
                        │
                        ▼
┌─────────────────────────────────────────────────────────────┐
│ ✅ Environment ready!                                        │
│    Next: cargo run -- dev --start-db                        │
└─────────────────────────────────────────────────────────────┘
```

### 4.3 Key Design Decisions

#### Decision 1: Use mise Instead of Language-Specific Managers
**Rationale**: 
- Single tool manages all languages (Java, Rust, Bun)
- Faster than asdf (written in Rust)
- Better Windows support than asdf
- Simpler configuration (single TOML file)

#### Decision 2: Docker Compose for Database
**Rationale**:
- Ensures identical PostgreSQL version across platforms
- No system-level installation conflicts
- Easy to reset and recreate
- Aligns with containerization strategy

#### Decision 3: Template-Based Configuration
**Rationale**:
- Prevents accidental secret commits
- Ensures consistency across developers
- Easy to update (change template, regenerate)
- Supports multiple environments (dev/staging/prod)

#### Decision 4: Idempotent Bootstrap Scripts
**Rationale**:
- Safe to run multiple times
- Preserves developer customizations
- Reduces support burden
- Enables automated verification

#### Decision 5: CLI Tools Trust mise
**Rationale**:
- sangita-cli complements mise rather than duplicating its functionality
- mise is the single source of truth for tool versions
- CLI focuses on workflow orchestration, not toolchain management
- Reduces code duplication and maintenance burden

**Implementation**:
- sangita-cli's `setup` command trusts mise for toolchain (Rust, Java, Bun, Docker Compose)
- Only checks tools NOT managed by mise (Docker service)
- Detects mise environment and provides helpful guidance
- Developers run CLI via `mise exec cargo run -- ...` to ensure correct tool versions

**Key Principles**:
- ✅ Trust mise for toolchain management
- ✅ Focus CLI on database, workflow, and testing (core purpose)
- ✅ Don't duplicate version checking or tool discovery
- ✅ Provide helpful error messages suggesting mise when tools are missing
- ✅ Backward compatible (works without mise, with warnings)

### 4.4 Implementation Details

#### 4.4.1 Bootstrap Script Logic

**Phase 1: Toolchain Setup**
```bash
if mise is available:
    mise install          # Install Java, Rust, Bun
    mise activate         # Add to PATH
else:
    warn: "Use system tools (verify versions manually)"
```

**Phase 2: Docker Verification**
```bash
check docker command exists
check docker compose or docker-compose exists
exit if missing
```

**Phase 3: Config File Creation**
```bash
if config/.env.development exists:
    skip (preserve user customizations)
else:
    copy tools/bootstrap-assets/env/env.development.example
         → config/.env.development
```

**Phase 4: Database Setup**
```bash
docker compose up -d postgres    # Start PostgreSQL 15
wait for health check
```

**Phase 5: Build & Migrate**
```bash
cargo build --manifest-path tools/sangita-cli/Cargo.toml
cargo run --manifest-path tools/sangita-cli/Cargo.toml -- db reset --mode docker
```

**Phase 6: Frontend Dependencies**
```bash
if bun is available:
    cd modules/frontend/sangita-admin-web
    bun install
else:
    warn: "Skip frontend install"
```

#### 4.4.2 Error Handling

**Bootstrap Script Behavior**:
- ✅ Uses `set -euo pipefail` (bash) / `$ErrorActionPreference = "Stop"` (PowerShell)
- ✅ Exits on first error with clear message
- ✅ Provides actionable error messages (e.g., "Install Docker Desktop")
- ✅ Preserves existing config files (doesn't overwrite)

**Graceful Degradation**:
- If mise is missing: Continue with system tools (with warnings)
- If bun is missing: Skip frontend install (optional step)
- If config exists: Preserve user customizations

#### 4.4.3 Cross-OS Compatibility

**Path Handling**:
- **Unix/Linux/macOS**: Uses forward slashes (`/`)
- **Windows**: PowerShell script uses `Join-Path` for cross-platform paths

**Shell Activation**:
- **Unix/Linux/macOS**: `eval "$(mise activate bash)"` or `eval "$(mise activate zsh)"`
- **Windows**: mise activation handled by PowerShell profile (if configured)

**Docker Compose**:
- **Modern**: `docker compose` (Docker Compose V2)
- **Legacy**: `docker-compose` (fallback for older installations)

#### 4.4.4 Version Pinning Strategy

| Tool | Version | Source | Rationale |
| :--- | :--- | :--- | :--- |
| **Java** | `temurin-25` | `.mise.toml` | Matches `build.gradle.kts` JVM toolchain (Java 25) |
| **Rust** | `1.92.0` | `.mise.toml` + `tools/sangita-cli/rust-toolchain.toml` | CLI toolchain requirement |
| **Bun** | `1.3.0` | `.mise.toml` | Frontend package manager (faster than npm) |
| **PostgreSQL** | `15` | `compose.yaml` | Database version (via Docker) |

**Version Sync**: `.mise.toml` must stay in sync with:
- `build.gradle.kts` (Java version)
- `tools/sangita-cli/rust-toolchain.toml` (Rust version)

#### 4.4.5 File Structure

```
sangeetha-grantha/
├── .mise.toml                                    # Toolchain versions
├── tools/
│   ├── bootstrap                                 # Unix/Linux/macOS script
│   ├── bootstrap.ps1                             # Windows PowerShell script
│   └── bootstrap-assets/
│       └── env/
│           └── env.development.example           # Config template
├── config/
│   └── .env.development                          # Generated (gitignored)
└── compose.yaml                                  # Docker Compose (Postgres 15)
```

---

## 5. Success Criteria

### 5.1 Quantitative Metrics

| Metric | Target | Measurement |
|--------|--------|-------------|
| **Setup Time** | < 15 minutes | Time from command to verified environment |
| **Onboarding Time** | < 30 minutes | Total time for new developer to first commit |
| **Environment Consistency** | 100% | All developers use same tool versions |
| **Setup Success Rate** | > 95% | Percentage of successful first-time setups |

### 5.2 Qualitative Outcomes

- ✅ **Developer Satisfaction**: Reduced frustration with environment setup
- ✅ **Team Velocity**: Faster onboarding of new team members
- ✅ **CI/CD Reliability**: Fewer environment-related test failures
- ✅ **Documentation Quality**: Single source of truth for setup instructions
- ✅ **Maintenance Burden**: Reduced support requests for environment issues

---

## 6. Dependencies & Prerequisites

### 6.1 External Dependencies

- **Docker Desktop** (macOS/Windows) or **Docker Engine** (Linux)
- **Git** (for version control)
- **mise** (optional but recommended) — installs automatically if present

### 6.2 Internal Dependencies

- **Sangita CLI** (`tools/sangita-cli`): Rust tool for database management
- **Docker Compose** (`compose.yaml`): PostgreSQL 15 container definition
- **Gradle Wrapper**: Included in repository
- **Bootstrap Assets**: Template files in `tools/bootstrap-assets/`

---

## 7. Risks & Mitigations

### 7.1 Risk: mise Not Available on All Platforms

**Mitigation**: 
- Bootstrap script gracefully degrades to system tools
- Clear warnings guide developers to install mise
- Documentation includes manual setup instructions

### 7.2 Risk: Docker Not Available

**Mitigation**:
- Bootstrap script checks for Docker before proceeding
- Clear error message with installation instructions
- Alternative: Manual PostgreSQL setup documented (not recommended)

### 7.3 Risk: Version Conflicts with System Tools

**Mitigation**:
- mise isolates tool versions in project directory
- PATH management ensures project tools take precedence
- Clear documentation of version requirements

### 7.4 Risk: Configuration Template Drift

**Mitigation**:
- Template files in version control
- Bootstrap script validates template existence
- Regular review of template vs generated files

---

## 8. Future Enhancements

### 8.1 Potential Improvements

1. **IDE Integration**
   - VS Code/Cursor workspace settings auto-activate mise
   - IntelliJ IDEA project SDK detection from mise

2. **Health Checks**
   - Automated verification of environment correctness
   - Pre-commit hook to verify tool versions match `.mise.toml`

3. **Advanced Config Management**
   - Support for multiple environments (dev/staging/prod)
   - Config validation (ensure required variables are set)
   - Secret management integration (e.g., 1Password, AWS Secrets Manager)

4. **Documentation Automation**
   - Auto-generate setup instructions from `.mise.toml`
   - Bootstrap script output includes next steps
   - Interactive troubleshooting guide

5. **CI/CD Integration**
   - Use same mise configuration in GitHub Actions
   - Ensure CI environment matches local setup
   - Automated environment verification in CI pipeline

---

## 9. Usage Guide

### 9.1 First-Time Setup

**Step 1: Clone Repository**
```bash
git clone <repository-url>
cd sangeetha-grantha
```

**Step 2: Install mise (Recommended)**
```bash
# macOS/Linux
curl https://mise.run | sh

# Windows (PowerShell as Administrator)
winget install jdx.mise
# Or download from https://mise.jdx.dev/
```

**Step 3: Run Bootstrap**
```bash
# Unix/Linux/macOS
./tools/bootstrap

# Windows
powershell -ExecutionPolicy Bypass -File .\tools\bootstrap.ps1
```

**Step 4: Verify Setup**
```bash
# Check toolchain versions
java -version    # Should show Java 25
rustc --version  # Should show rustc 1.92.0
bun --version    # Should show 1.3.0

# Check database
docker ps        # Should show sangita_postgres container running
cargo run --manifest-path tools/sangita-cli/Cargo.toml -- db health
```

### 9.2 Daily Development Workflow

**Start Development Stack**:
```bash
# Option 1: Via mise (recommended - ensures correct tool versions)
mise exec cargo run --manifest-path tools/sangita-cli/Cargo.toml -- dev --start-db

# Option 2: Direct (requires tools to be installed manually)
cargo run --manifest-path tools/sangita-cli/Cargo.toml -- dev --start-db

# Option 3: Manual (for debugging)
docker compose up -d postgres
./gradlew :modules:backend:api:run
cd modules/frontend/sangita-admin-web && bun run dev
```

**Reset Database**:
```bash
# Via mise (recommended)
mise exec cargo run --manifest-path tools/sangita-cli/Cargo.toml -- db reset --mode docker

# Or direct
cargo run --manifest-path tools/sangita-cli/Cargo.toml -- db reset --mode docker
```

**Update Toolchain**:
```bash
# Update .mise.toml with new versions
mise install    # Installs updated versions
```

### 9.3 Troubleshooting

**Issue: "mise not found"**
- **Solution**: Install mise or use system tools (verify versions manually)

**Issue: "Docker not running"**
- **Solution**: Start Docker Desktop (macOS/Windows) or Docker daemon (Linux)

**Issue: "Port 5432 already in use"**
- **Solution**: Stop existing PostgreSQL instance or change port in `compose.yaml`

**Issue: "Config file not created"**
- **Solution**: Manually copy `tools/bootstrap-assets/env/env.development.example` to `config/.env.development`

**Issue: "Rust build fails"**
- **Solution**: Run via mise to ensure correct Rust version: `mise exec cargo run -- ...`
- **Alternative**: Ensure Rust 1.92.0 is installed manually (`rustup install 1.92.0`)

**Issue: "Tool version mismatch"**
- **Solution**: Always run sangita-cli via mise: `mise exec cargo run -- ...`
- **Note**: sangita-cli trusts mise for toolchain management and doesn't validate versions

### 9.4 Testing & Verification

#### 9.4.1 Test Matrix

| Platform | OS Version | Status | Notes |
| :--- | :--- | :--- | :--- |
| macOS | 14+ (Sonoma) | ✅ Tested | Works with mise and Docker Desktop |
| Linux | Ubuntu 22.04+ | ✅ Tested | Works with mise and Docker Engine |
| Windows | Windows 11 | ✅ Tested | Works with mise and Docker Desktop |

#### 9.4.2 Verification Checklist

After running bootstrap, verify:
- [ ] Java 25 is available (`java -version`)
- [ ] Rust 1.92.0 is available (`rustc --version`)
- [ ] Bun 1.3.0 is available (`bun --version`)
- [ ] PostgreSQL container is running (`docker ps`)
- [ ] `config/.env.development` exists and has correct values
- [ ] Database migrations completed (`cargo run -- db health`)
- [ ] Frontend dependencies installed (`ls modules/frontend/sangita-admin-web/node_modules`)

---

## 10. Related Documentation

- **[Project README](../../../README.md)**: Getting started guide and project overview
- **[Sangita CLI README](../../../tools/sangita-cli/README.md)**: Database management and development workflow
- **[Architecture Documentation](../../02-architecture/)**: System architecture and design decisions
- **mise Documentation**: https://mise.jdx.dev/
- **Docker Compose**: https://docs.docker.com/compose/

---

## 11. Approval & Status

**Status**: ✅ **Implemented**

**Implementation Date**: 2026-01-14

**Components Delivered**:
- ✅ `.mise.toml` with toolchain version pinning
- ✅ `tools/bootstrap` (Unix/Linux/macOS)
- ✅ `tools/bootstrap.ps1` (Windows)
- ✅ `tools/bootstrap-assets/env/env.development.example` (config template)
- ✅ Documentation and usage guides

**Testing Status**: ✅ Tested on macOS, Linux, and Windows

**Maintainer**: Development Team

---

**Last Updated**: 2026-01-14  
**Document Version**: 1.0  
**Next Review**: 2026-04-14 (quarterly review)