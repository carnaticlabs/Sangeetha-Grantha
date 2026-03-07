#!/bin/bash
# Start all Sangita components: Database + Backend + Frontend + Extraction Service.
#
# This is a thin wrapper around the sangita-cli `dev` command which manages the
# full process lifecycle, signal handling, and graceful shutdown natively in Rust.
#
# Logs are written to:
#   sangita_logs.txt            — main CLI + backend + frontend output
#   sangita_extraction_logs.txt — extraction worker Docker container logs
#   exposed_queries.log         — Exposed SQL queries (written by backend Logback)
#
# Usage:  ./start-sangita.sh                    (full stack)
#         ./start-sangita.sh --no-extraction     (skip extraction Docker container)
# Stop:   Ctrl+C or kill -TERM <pid>            (graceful shutdown of all services)

set -euo pipefail

PROJECT_HOME="$(cd "$(dirname "$0")" && pwd)"
CARGO_MANIFEST="${PROJECT_HOME}/tools/sangita-cli/Cargo.toml"

LOG_FILE="${PROJECT_HOME}/sangita_logs.txt"
EXTRACTION_LOG="${PROJECT_HOME}/sangita_extraction_logs.txt"
EXPOSED_LOG="${PROJECT_HOME}/exposed_queries.log"

# ── Parse arguments: strip --no-extraction, pass the rest to cargo ────────────
NO_EXTRACTION=false
ARGS=()
for arg in "$@"; do
    if [[ "$arg" == "--no-extraction" ]]; then
        NO_EXTRACTION=true
    else
        ARGS+=("$arg")
    fi
done

# ── Truncate log files so they reflect this run ─────────────────────────────
echo "===== Sangita startup: $(date '+%Y-%m-%d %H:%M:%S') =====" > "$LOG_FILE"
echo "===== Sangita startup: $(date '+%Y-%m-%d %H:%M:%S') =====" > "$EXTRACTION_LOG"
: > "$EXPOSED_LOG"  # Logback appends; clear before start

# ── PID tracking for cleanup ────────────────────────────────────────────────
EXTRACTION_LOG_PID=""
CARGO_PID=""

cleanup() {
    # Forward signal to cargo process if it's still running
    if [[ -n "$CARGO_PID" ]]; then
        kill "$CARGO_PID" 2>/dev/null || true
        wait "$CARGO_PID" 2>/dev/null || true
    fi
    if [[ -n "$EXTRACTION_LOG_PID" ]]; then
        kill "$EXTRACTION_LOG_PID" 2>/dev/null || true
        wait "$EXTRACTION_LOG_PID" 2>/dev/null || true
    fi
}
trap cleanup EXIT INT TERM

# ── Stream extraction container logs in background (unless --no-extraction) ──
if [[ "$NO_EXTRACTION" == false ]]; then
    (
        # Poll until the container is running instead of a fixed sleep
        for _ in {1..30}; do
            if docker inspect -f '{{.State.Running}}' sangita_krithi_extract_enrich_worker 2>/dev/null | grep -q true; then
                docker logs -f sangita_krithi_extract_enrich_worker >> "$EXTRACTION_LOG" 2>&1
                break
            fi
            sleep 5
        done
    ) &
    EXTRACTION_LOG_PID=$!
fi

# ── Start the CLI, teeing output to the log file ────────────────────────────
# Run cargo in background so we can capture its PID for signal forwarding.
set +e
mise exec -- cargo run --manifest-path "${CARGO_MANIFEST}" -- dev --start-db ${ARGS[@]+"${ARGS[@]}"} 2>&1 | tee -a "$LOG_FILE" &
CARGO_PID=$!
wait "$CARGO_PID"
EXIT_CODE=$?
set -e

exit "$EXIT_CODE"
