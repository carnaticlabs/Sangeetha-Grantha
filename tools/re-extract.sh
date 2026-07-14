#!/usr/bin/env bash
# re-extract.sh — Detect, fix, and reset extraction for krithis with OTHER sections.
#
# This script automates the manual cycle discovered during the Tyagaraja bulk import
# (2026-07-14). See: application_documentation/11-retrospective/thyagaraja-krithi-import-analysis.md
#
# What it does:
#   1. DETECT — Find imported_krithis with OTHER sections matching a source URL pattern
#   2. RESET  — Clear parsed_payload, reset extraction_queue to PENDING
#   3. WAIT   — Poll until Python worker completes extraction (DONE)
#   4. PATCH  — Fix stale importId references in extraction_queue.request_payload
#   5. WAIT   — Poll until Kotlin worker completes ingestion (INGESTED)
#   6. VERIFY — Confirm zero OTHER sections remain
#
# Usage:
#   ./tools/re-extract.sh                              # all sources with OTHER sections
#   ./tools/re-extract.sh thyagaraja-vaibhavam         # only matching source URLs
#   ./tools/re-extract.sh --detect-only                # report only, no changes
#   ./tools/re-extract.sh --dry-run thyagaraja          # show SQL, don't execute
#
# Prerequisites:
#   - PostgreSQL client (psql) installed
#   - Database accessible on localhost:5432
#   - Extraction worker container running (sangeetha-grantha-extraction-1)
#   - Backend container running (Kotlin ExtractionWorker polling)
#
# Environment variables (all optional, with defaults):
#   DB_HOST       (default: localhost)
#   DB_PORT       (default: 5432)
#   DB_NAME       (default: sangita_grantha)
#   DB_USER       (default: postgres)
#   PGPASSWORD    (default: postgres)
#   POLL_INTERVAL (default: 5, seconds between status checks)
#   POLL_TIMEOUT  (default: 300, max seconds to wait per phase)

set -euo pipefail

# --- Configuration -----------------------------------------------------------

DB_HOST="${DB_HOST:-localhost}"
DB_PORT="${DB_PORT:-5432}"
DB_NAME="${DB_NAME:-sangita_grantha}"
DB_USER="${DB_USER:-postgres}"
export PGPASSWORD="${PGPASSWORD:-postgres}"
POLL_INTERVAL="${POLL_INTERVAL:-5}"
POLL_TIMEOUT="${POLL_TIMEOUT:-300}"

SOURCE_PATTERN="${1:-}"
DETECT_ONLY=false
DRY_RUN=false

# Parse flags
for arg in "$@"; do
    case "$arg" in
        --detect-only) DETECT_ONLY=true; shift ;;
        --dry-run)     DRY_RUN=true; shift ;;
    esac
done
# Re-read positional after flag removal
SOURCE_PATTERN="${1:-}"

PSQL="psql -h $DB_HOST -p $DB_PORT -U $DB_USER -d $DB_NAME -tA"
PSQL_PRETTY="psql -h $DB_HOST -p $DB_PORT -U $DB_USER -d $DB_NAME"

# --- Helpers ------------------------------------------------------------------

log()  { echo "[$(date +%H:%M:%S)] $*"; }
warn() { echo "[$(date +%H:%M:%S)] WARNING: $*" >&2; }
die()  { echo "[$(date +%H:%M:%S)] ERROR: $*" >&2; exit 1; }

run_sql() {
    if $DRY_RUN; then
        echo "  [DRY RUN] $1"
        echo "0"
    else
        $PSQL -c "$1" 2>/dev/null | tr -d '[:space:]'
    fi
}

run_sql_pretty() {
    $PSQL_PRETTY -c "$1" 2>/dev/null
}

build_source_filter() {
    if [[ -n "$SOURCE_PATTERN" ]]; then
        echo "AND ik.source_key LIKE '%${SOURCE_PATTERN}%'"
    else
        echo ""
    fi
}

# --- Phase 1: DETECT ----------------------------------------------------------

detect() {
    local filter
    filter=$(build_source_filter)

    log "=== Phase 1: DETECT ==="
    log "Source filter: ${SOURCE_PATTERN:-<all sources>}"

    # Count krithis with OTHER sections
    local other_count
    other_count=$(run_sql "
        SELECT COUNT(DISTINCT ik.id)
        FROM imported_krithis ik,
          LATERAL jsonb_array_elements(ik.parsed_payload->'sections') AS sec
        WHERE sec->>'type' = 'OTHER'
          AND ik.import_status IN ('in_review', 'pending')
          $filter
    ")

    log "Found $other_count krithis with OTHER sections"

    if [[ "$other_count" == "0" ]]; then
        log "Nothing to fix."
        exit 0
    fi

    # Show section distribution
    log ""
    log "Current section distribution:"
    run_sql_pretty "
        SELECT sec->>'type' AS section_type, COUNT(*) AS count
        FROM imported_krithis ik,
          LATERAL jsonb_array_elements(ik.parsed_payload->'sections') AS sec
        WHERE ik.import_status IN ('in_review', 'pending')
          $filter
        GROUP BY sec->>'type'
        ORDER BY count DESC
    "

    # Show sample affected krithis
    log ""
    log "Sample affected krithis:"
    run_sql_pretty "
        SELECT ik.raw_title, ik.import_status,
          array_agg(sec->>'type' ORDER BY (sec->>'order')::int) AS sections
        FROM imported_krithis ik,
          LATERAL jsonb_array_elements(ik.parsed_payload->'sections') AS sec
        WHERE ik.id IN (
            SELECT DISTINCT ik2.id
            FROM imported_krithis ik2,
              LATERAL jsonb_array_elements(ik2.parsed_payload->'sections') AS s
            WHERE s->>'type' = 'OTHER'
              AND ik2.import_status IN ('in_review', 'pending')
        )
        $filter
        GROUP BY ik.raw_title, ik.import_status
        LIMIT 10
    "

    echo "$other_count"
}

# --- Phase 2: RESET -----------------------------------------------------------

reset_for_reextraction() {
    local filter
    filter=$(build_source_filter)

    log ""
    log "=== Phase 2: RESET ==="

    # Step 2a: Clear parsed_payload on affected imports
    local cleared
    cleared=$(run_sql "
        UPDATE imported_krithis
        SET parsed_payload = NULL, import_status = 'pending'
        WHERE id IN (
            SELECT DISTINCT ik.id
            FROM imported_krithis ik,
              LATERAL jsonb_array_elements(ik.parsed_payload->'sections') AS sec
            WHERE sec->>'type' = 'OTHER'
              AND ik.import_status IN ('in_review', 'pending')
              $filter
        )
    " | grep -oE '[0-9]+' | head -1)
    log "Cleared parsed_payload on ${cleared:-0} import rows"

    # Step 2b: Reset extraction queue entries to PENDING
    local reset
    reset=$(run_sql "
        UPDATE extraction_queue eq
        SET status = 'PENDING', attempts = 0, updated_at = NOW()
        FROM imported_krithis ik
        WHERE eq.source_url = ik.source_key
          AND ik.import_status = 'pending'
          AND ik.parsed_payload IS NULL
          AND eq.status IN ('INGESTED', 'DONE', 'FAILED')
          $filter
    " | grep -oE '[0-9]+' | head -1)
    log "Reset ${reset:-0} extraction queue entries to PENDING"
}

# --- Phase 3: WAIT for Python extraction --------------------------------------

wait_for_extraction() {
    local filter
    filter=$(build_source_filter)

    log ""
    log "=== Phase 3: WAIT for Python extraction ==="

    local elapsed=0
    while true; do
        local pending
        pending=$($PSQL -c "
            SELECT COUNT(*)
            FROM extraction_queue eq
            JOIN imported_krithis ik ON eq.source_url = ik.source_key
            WHERE ik.parsed_payload IS NULL
              AND eq.status = 'PENDING'
              $(build_source_filter)
        " 2>/dev/null | tr -d '[:space:]')

        if [[ "$pending" == "0" ]]; then
            log "All extractions complete"
            break
        fi

        if [[ $elapsed -ge $POLL_TIMEOUT ]]; then
            die "Timed out after ${POLL_TIMEOUT}s waiting for extraction ($pending still pending)"
        fi

        log "  $pending extractions pending... (${elapsed}s elapsed)"
        sleep "$POLL_INTERVAL"
        elapsed=$((elapsed + POLL_INTERVAL))
    done
}

# --- Phase 4: PATCH importIds -------------------------------------------------

patch_import_ids() {
    local filter
    filter=$(build_source_filter)

    log ""
    log "=== Phase 4: PATCH importIds ==="

    local patched
    patched=$(run_sql "
        UPDATE extraction_queue eq
        SET
          request_payload = jsonb_set(eq.request_payload, '{importId}', to_jsonb(ik.id::text)),
          status = 'DONE',
          updated_at = NOW()
        FROM imported_krithis ik
        WHERE eq.source_url = ik.source_key
          AND ik.parsed_payload IS NULL
          AND ik.import_status = 'pending'
          AND eq.status IN ('DONE', 'INGESTED')
          $filter
    " | grep -oE '[0-9]+' | head -1)
    log "Patched importId on ${patched:-0} extraction queue entries"
}

# --- Phase 5: WAIT for Kotlin ingestion ---------------------------------------

wait_for_ingestion() {
    local filter
    filter=$(build_source_filter)

    log ""
    log "=== Phase 5: WAIT for Kotlin ingestion ==="

    local elapsed=0
    while true; do
        local unprocessed
        unprocessed=$($PSQL -c "
            SELECT COUNT(*)
            FROM imported_krithis ik
            WHERE ik.parsed_payload IS NULL
              AND ik.import_status = 'pending'
              $(build_source_filter)
        " 2>/dev/null | tr -d '[:space:]')

        if [[ "$unprocessed" == "0" ]]; then
            log "All imports enriched"
            break
        fi

        # Check if there are DONE entries needing importId patches (race condition)
        local needs_patch
        needs_patch=$($PSQL -c "
            SELECT COUNT(*)
            FROM extraction_queue eq
            JOIN imported_krithis ik ON eq.source_url = ik.source_key
            WHERE ik.parsed_payload IS NULL
              AND ik.import_status = 'pending'
              AND eq.status = 'INGESTED'
              AND eq.request_payload->>'importId' != ik.id::text
              $(build_source_filter)
        " 2>/dev/null | tr -d '[:space:]')

        if [[ "$needs_patch" != "0" ]]; then
            log "  Found $needs_patch entries with stale importIds — re-patching..."
            patch_import_ids
        fi

        if [[ $elapsed -ge $POLL_TIMEOUT ]]; then
            die "Timed out after ${POLL_TIMEOUT}s waiting for ingestion ($unprocessed still pending)"
        fi

        log "  $unprocessed imports awaiting enrichment... (${elapsed}s elapsed)"
        sleep "$POLL_INTERVAL"
        elapsed=$((elapsed + POLL_INTERVAL))
    done
}

# --- Phase 6: VERIFY ----------------------------------------------------------

verify() {
    local filter
    filter=$(build_source_filter)

    log ""
    log "=== Phase 6: VERIFY ==="

    local remaining
    remaining=$(run_sql "
        SELECT COUNT(DISTINCT ik.id)
        FROM imported_krithis ik,
          LATERAL jsonb_array_elements(ik.parsed_payload->'sections') AS sec
        WHERE sec->>'type' = 'OTHER'
          AND ik.import_status IN ('in_review', 'pending')
          $filter
    ")

    if [[ "$remaining" == "0" ]]; then
        log "SUCCESS: Zero OTHER sections remaining"
    else
        warn "$remaining krithis still have OTHER sections"
        warn "This likely means the parser needs additional patterns."
        warn "Check the affected krithis:"
        run_sql_pretty "
            SELECT ik.raw_title,
              LEFT((
                SELECT s->>'text'
                FROM jsonb_array_elements(ik.parsed_payload->'lyricVariants') lv,
                  LATERAL jsonb_array_elements(lv->'sections') s
                WHERE lv->>'script' = 'latin'
                LIMIT 1
              ), 200) AS text_preview
            FROM imported_krithis ik
            WHERE ik.id IN (
                SELECT DISTINCT ik2.id
                FROM imported_krithis ik2,
                  LATERAL jsonb_array_elements(ik2.parsed_payload->'sections') AS sec
                WHERE sec->>'type' = 'OTHER'
                  AND ik2.import_status IN ('in_review', 'pending')
            )
            $filter
            LIMIT 10
        "
    fi

    # Final section distribution
    log ""
    log "Final section distribution:"
    run_sql_pretty "
        SELECT sec->>'type' AS section_type, COUNT(*) AS count
        FROM imported_krithis ik,
          LATERAL jsonb_array_elements(ik.parsed_payload->'sections') AS sec
        WHERE ik.import_status IN ('in_review', 'pending', 'approved', 'mapped')
          $filter
        GROUP BY sec->>'type'
        ORDER BY count DESC
    "
}

# --- Main ---------------------------------------------------------------------

main() {
    log "re-extract.sh — Detect, fix, and reset OTHER sections"
    log "======================================================="
    log ""

    # Verify DB connectivity
    $PSQL -c "SELECT 1" >/dev/null 2>&1 || die "Cannot connect to database at $DB_HOST:$DB_PORT"

    # Phase 1: Detect
    local count
    count=$(detect)

    if $DETECT_ONLY; then
        log ""
        log "Detect-only mode — no changes made."
        exit 0
    fi

    if $DRY_RUN; then
        log ""
        log "Dry-run mode — showing SQL but not executing."
    fi

    # Confirm before proceeding
    if [[ -t 0 ]] && ! $DRY_RUN; then
        echo ""
        read -rp "Proceed with re-extraction of $count krithis? [y/N] " confirm
        if [[ "$confirm" != [yY] ]]; then
            log "Aborted."
            exit 0
        fi
    fi

    # Phases 2-6
    reset_for_reextraction
    wait_for_extraction
    patch_import_ids
    wait_for_ingestion
    verify

    log ""
    log "Done."
}

main
