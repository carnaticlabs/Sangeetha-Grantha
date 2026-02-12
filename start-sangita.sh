#!/bin/bash
# Start all Sangita components: Database + Backend + Frontend + Extraction Service.
#
# This is a thin wrapper around the sangita-cli `dev` command which manages the
# full process lifecycle, signal handling, and graceful shutdown natively in Rust.
#
# Logs are written to:
#   sangita_logs.txt            — main CLI + backend + frontend output
#   sangita_extraction_logs.txt — PDF extraction Docker container logs
#   exposed_queries.log         — Exposed SQL queries (written by backend Logback)
#
# Usage:  ./start-sangita.sh                    (full stack)
#         ./start-sangita.sh --no-extraction     (skip extraction Docker container)
# Stop:   Ctrl+C or kill -TERM <pid>            (graceful shutdown of all services)

set -euo pipefail

PROJECT_HOME="${HOME}/project/sangeetha-grantha"
CARGO_MANIFEST="${PROJECT_HOME}/tools/sangita-cli/Cargo.toml"

LOG_FILE="${PROJECT_HOME}/sangita_logs.txt"
EXTRACTION_LOG="${PROJECT_HOME}/sangita_extraction_logs.txt"
EXPOSED_LOG="${PROJECT_HOME}/exposed_queries.log"

# ── Truncate log files so they reflect this run ─────────────────────────────
echo "===== Sangita startup: $(date '+%Y-%m-%d %H:%M:%S') =====" > "$LOG_FILE"
echo "===== Sangita startup: $(date '+%Y-%m-%d %H:%M:%S') =====" > "$EXTRACTION_LOG"
: > "$EXPOSED_LOG"  # Logback appends; clear before start

# ── PID tracking for cleanup ────────────────────────────────────────────────
EXTRACTION_LOG_PID=""

cleanup() {
    if [[ -n "$EXTRACTION_LOG_PID" ]]; then
        kill "$EXTRACTION_LOG_PID" 2>/dev/null || true
        wait "$EXTRACTION_LOG_PID" 2>/dev/null || true
    fi
}
trap cleanup EXIT

# ── Stream extraction container logs in background (unless --no-extraction) ──
if [[ ! " $* " =~ " --no-extraction " ]]; then
    (
        # Wait for the CLI to start the Docker container (~45s for backend + build)
        sleep 45
        # Retry a few times in case the container isn't ready yet
        for _ in {1..5}; do
            if docker logs -f sangita_pdf_extractor >> "$EXTRACTION_LOG" 2>&1; then
                break
            fi
            sleep 10
        done
    ) &
    EXTRACTION_LOG_PID=$!
fi

# ── Start the CLI, teeing output to the log file ────────────────────────────
# `tee -a` appends to the log file while still showing output in the terminal.
# Allow non-zero exit (SIGINT → 130) so the script shuts down cleanly.
set +e
mise exec -- cargo run --manifest-path "${CARGO_MANIFEST}" -- dev --start-db "$@" 2>&1 | tee -a "$LOG_FILE"
EXIT_CODE=${PIPESTATUS[0]}
set -e

exit "${EXIT_CODE:-0}"
