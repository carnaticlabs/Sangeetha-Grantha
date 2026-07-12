# Sangita Grantha bootstrap (Windows PowerShell)
# Docker Postgres + host-run backend/frontend
#
# Usage:
#   powershell -ExecutionPolicy Bypass -File .\tools\bootstrap.ps1

$ErrorActionPreference = "Stop"

function Step($msg) {
  Write-Host "`n==> $msg" -ForegroundColor Cyan
}

function Warn($msg) {
  Write-Host "`n!! $msg" -ForegroundColor Yellow
}

$Root = (Resolve-Path (Join-Path $PSScriptRoot ".." )).Path

Step "Sangita Grantha bootstrap (Docker Postgres + host-run backend/frontend)"

# 1) Toolchain via mise (recommended)
if (Get-Command mise -ErrorAction SilentlyContinue) {
  Step "Installing pinned toolchain via mise (.mise.toml)"
  mise install
  # mise activation in PowerShell varies by setup; most users will have mise activated globally.
} else {
  Warn "mise not found. Recommended: https://mise.jdx.dev/"
  Warn "Continuing with system tools (ensure Java 25+, Rust 1.92.0, Bun 1.3.x)."
}

# 2) Ensure Docker is available
Step "Checking Docker + Compose"
if (-not (Get-Command docker -ErrorAction SilentlyContinue)) {
  throw "Missing 'docker'. Install Docker Desktop."
}

$composeOk = $false
try {
  docker compose version | Out-Null
  $composeOk = $true
} catch {
  if (Get-Command docker-compose -ErrorAction SilentlyContinue) {
    $composeOk = $true
  }
}

if (-not $composeOk) {
  throw "Docker Compose not found (need 'docker compose' or 'docker-compose')."
}

# 3) Canonical config files
Step "Ensuring config/development.env exists"
New-Item -ItemType Directory -Force -Path (Join-Path $Root "config") | Out-Null

$template = Join-Path $Root "tools\bootstrap-assets\env\development.env.example"
$target = Join-Path $Root "config\development.env"

if (Test-Path $target) {
  Write-Host "config/development.env already exists (leaving as-is)"
} else {
  if (Test-Path $template) {
    Copy-Item $template $target
    Write-Host "Created config/development.env from template"
  } else {
    Warn "Template not found at $template"
    Warn "Create config/development.env manually (see docs)"
  }
}

# 4) Start Postgres 15 via Docker Compose
Step "Starting Postgres 15 via Docker Compose"
Push-Location $Root
try {
  docker compose up -d postgres
} catch {
  docker-compose up -d postgres
}
Pop-Location

# 5) Build Rust CLI
Step "Building sangita-cli"
Push-Location $Root
cargo build --manifest-path tools/sangita-cli/Cargo.toml

# 6) Migrate + seed
Step "Running DB reset (drop/create/migrate/seed) via sangita-cli"
cargo run --manifest-path tools/sangita-cli/Cargo.toml -- db reset --mode docker

# 7) Frontend deps (optional)
Step "Installing frontend dependencies (bun install)"
if (Get-Command bun -ErrorAction SilentlyContinue) {
  Push-Location (Join-Path $Root "modules\frontend\sangita-admin-web")
  bun install
  Pop-Location
} else {
  Warn "bun not found; skipping frontend install"
}

Step "Bootstrap complete"
Write-Host "
Next commands:

  # Start full dev stack (DB + backend + frontend)
  cargo run --manifest-path tools/sangita-cli/Cargo.toml -- dev --start-db

  # Or run services separately:
  ./gradlew :modules:backend:api:run
  (cd modules/frontend/sangita-admin-web && bun run dev)

Notes:
- DB dev version is pinned to Postgres 15 via compose.yaml
- Local Postgres binary mode is still available (advanced):
    cargo run --manifest-path tools/sangita-cli/Cargo.toml -- db start --mode local
"
