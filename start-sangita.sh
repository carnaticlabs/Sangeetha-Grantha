#!/bin/bash
# Start all Sangita components: Database + Backend + Frontend + Extraction Service.
#
# Delegates to `make dev` (Docker Compose --profile dev) for the full lifecycle.
#
# Logs are written to:
#   sangita_logs.txt            — compose output (backend + frontend + extraction)
#   sangita_extraction_logs.txt — extraction worker container logs (streamed separately)
#   exposed_queries.log         — Exposed SQL queries (written by backend Logback)
#
# Usage:  ./start-sangita.sh                    (full stack)
#         ./start-sangita.sh --no-extraction     (skip extraction container)
# Stop:   Ctrl+C or kill -TERM <pid>            (graceful shutdown via compose)

set -euo pipefail

PROJECT_HOME="$(cd "$(dirname "$0")" && pwd)"
cd "$PROJECT_HOME"

LOG_FILE="${PROJECT_HOME}/sangita_logs.txt"
EXTRACTION_LOG="${PROJECT_HOME}/sangita_extraction_logs.txt"
EXPOSED_LOG="${PROJECT_HOME}/exposed_queries.log"

# ── Parse arguments ─────────────────────────────────────────────────────────
NO_EXTRACTION=false
for arg in "$@"; do
    if [[ "$arg" == "--no-extraction" ]]; then
        NO_EXTRACTION=true
    fi
done

# ── Truncate log files so they reflect this run ─────────────────────────────
echo "===== Sangita startup: $(date '+%Y-%m-%d %H:%M:%S') =====" > "$LOG_FILE"
echo "===== Sangita startup: $(date '+%Y-%m-%d %H:%M:%S') =====" > "$EXTRACTION_LOG"
: > "$EXPOSED_LOG"

# ── PID tracking for cleanup ───────────────────────────────────────────────
COMPOSE_PID=""
EXTRACTION_LOG_PID=""

cleanup() {
    if [[ -n "$EXTRACTION_LOG_PID" ]]; then
        kill "$EXTRACTION_LOG_PID" 2>/dev/null || true
        wait "$EXTRACTION_LOG_PID" 2>/dev/null || true
    fi
    if [[ -n "$COMPOSE_PID" ]]; then
        kill "$COMPOSE_PID" 2>/dev/null || true
        wait "$COMPOSE_PID" 2>/dev/null || true
    fi
    make dev-down 2>/dev/null || true
}
trap cleanup EXIT INT TERM

# ── Stream extraction container logs in background ──────────────────────────
if [[ "$NO_EXTRACTION" == false ]]; then
    (
        for _ in {1..30}; do
            CONTAINER=$(docker compose ps -q extraction 2>/dev/null)
            if [[ -n "$CONTAINER" ]] && docker inspect -f '{{.State.Running}}' "$CONTAINER" 2>/dev/null | grep -q true; then
                docker compose logs -f extraction >> "$EXTRACTION_LOG" 2>&1
                break
            fi
            sleep 5
        done
    ) &
    EXTRACTION_LOG_PID=$!
fi

# ── Start the dev stack ─────────────────────────────────────────────────────
set +e
if [[ "$NO_EXTRACTION" == true ]]; then
    docker compose up --build -d db
    docker compose up --build -d --wait db
    docker compose run --rm migrate
    docker compose up --build backend frontend 2>&1 | tee -a "$LOG_FILE" &
else
    make dev 2>&1 | tee -a "$LOG_FILE" &
fi
COMPOSE_PID=$!
wait "$COMPOSE_PID"
EXIT_CODE=$?
set -e

exit "$EXIT_CODE"
