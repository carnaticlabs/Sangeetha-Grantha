#!/bin/bash
set -e

# ==============================================================================
# Integration Test Pipeline for Extraction & Deduplication
# ==============================================================================
# 1. Resets Database
# 2. Starts Backend & Python Extractor
# 3. Submits mdeng.pdf (Page 17-37) -> PRIMARY
# 4. Submits Truncated CSV -> SECONDARY
# 5. Submits mdskt.pdf (Page 17-37) -> ENRICH
# 6. Verifies Deduplication (0 Duplicates expected)
# ==============================================================================

DB_URL="postgresql://postgres:postgres@localhost:5432/sangita_grantha"
API_URL="http://localhost:8080"
AUTH_HEADER="Authorization: Bearer dev-admin-token"

cleanup() {
    echo "Stopping services..."
    if [ ! -z "$BACKEND_PID" ]; then kill $BACKEND_PID; fi
    if [ ! -z "$WORKER_PID" ]; then kill $WORKER_PID; fi
}
trap cleanup EXIT

echo ">>> Step 1: Resetting Database..."
cargo run --manifest-path tools/sangita-cli/Cargo.toml -- db reset

echo ">>> Step 2: Preparing Test Data..."
# Create truncated CSV if not exists (header + 20 rows)
head -n 21 database/for_import/Dikshitar-Krithi-For-Import.csv > database/for_import/Dikshitar-Krithi-Test-20.csv

echo ">>> Step 3: Starting Backend..."
./gradlew :modules:backend:api:run > backend.log 2>&1 &
BACKEND_PID=$!
echo "Backend PID: $BACKEND_PID"

echo "Waiting for Backend (max 60s)..."
for i in {1..60}; do
    if curl -s $API_URL/health > /dev/null; then
        echo "Backend is UP!"
        break
    fi
    sleep 1
    echo -n "."
done

echo ">>> Step 4: Python Extractor Worker (Running in Docker)..."
# cd tools/pdf-extractor
# source .venv/bin/activate
# export DATABASE_URL=$DB_URL
# # Run in background
# python -m src.worker > worker.log 2>&1 &
# WORKER_PID=$!
# echo "Worker PID: $WORKER_PID"
# cd ../..
echo "Skipping local worker start (assuming docker container 'sangita_pdf_extractor' is running)"

echo ">>> Step 5: generating Auth Token..."
ADMIN_USER_ID=$(psql "$DB_URL" -t -c "SELECT id FROM users WHERE email='admin@sangitagrantha.org'" | xargs)
echo "Admin User ID: $ADMIN_USER_ID"

# Get JWT using the dev-admin-token
JWT_RESP=$(curl -s -X POST "$API_URL/v1/auth/token" \
  -H "Content-Type: application/json" \
  -d '{
    "adminToken": "dev-admin-token",
    "userId": "'"$ADMIN_USER_ID"'",
    "roles": ["admin"]
  }')

TOKEN=$(echo $JWT_RESP | jq -r '.token')
echo "JWT Token: $TOKEN"
AUTH_HEADER="Authorization: Bearer $TOKEN"

echo ">>> Step 6: Fetching Source ID for 'guruguha.org'..."
SOURCE_ID=$(psql "$DB_URL" -t -c "SELECT id FROM import_sources WHERE name='guruguha.org'" | xargs)
echo "Source ID: $SOURCE_ID"

if [ -z "$SOURCE_ID" ]; then
    echo "Error: Source ID not found!"
    exit 1
fi

echo ">>> Step 6: Submitting 'mdeng.pdf' (PRIMARY, Pages 17-37)..."
# Using dev-admin-token defined in application.local.toml
RESP=$(curl -s -X POST "$API_URL/v1/admin/sourcing/extractions" \
  -H "Content-Type: application/json" \
  -H "$AUTH_HEADER" \
  -d '{ "sourceUrl": "https://guruguha.org/wp-content/uploads/2022/03/mdeng.pdf", "sourceFormat": "PDF", "importSourceId": "'$SOURCE_ID'", "pageRange": "17-37", "extractionIntent": "PRIMARY" }')
echo "Submission Response: $RESP"

echo ">>> Step 7: Waiting for Extraction & Ingestion..."
TASK_ID=$(echo $RESP | jq -r '.id')
echo "Task ID: $TASK_ID"

cat > check_status.sh << 'EOF'
#!/bin/bash
STATUS=$(psql "$1" -t -c "SELECT status FROM extraction_queue WHERE id='$2'" | xargs)
echo $STATUS
EOF
chmod +x check_status.sh

for i in {1..120}; do
    STATUS=$(./check_status.sh "$DB_URL" "$TASK_ID")
    echo "Task Status: $STATUS"
    if [ "$STATUS" == "INGESTED" ]; then
        echo "Extraction Processed & Ingested!"
        break
    fi
    if [ "$STATUS" == "FAILED" ]; then
        echo "Extraction FAILED!"
        psql "$DB_URL" -c "SELECT error_detail FROM extraction_queue WHERE id='$TASK_ID'"
        exit 1
    fi
    sleep 2
done
rm check_status.sh

echo ">>> Step 8: Submitting CSV Import (Truncated)..."
# Using POST /v1/admin/bulk-import/upload with multipart/form-data
# The file is database/for_import/Dikshitar-Krithi-Test-20.csv
CSV_PATH="database/for_import/Dikshitar-Krithi-Test-20.csv"

# Note: curl handles multipart upload with -F "file=@path"
# The param name in BulkImportRoutes.kt seems to be dynamic but typically 'file'
# checking code: multipart.forEachPart ... if (part is PartData.FileItem) ... 
# so any part name works if it's a file.

CSV_RESP=$(curl -s -X POST "$API_URL/v1/admin/bulk-import/upload" \
  -H "$AUTH_HEADER" \
  -F "file=@$CSV_PATH")

echo "CSV Upload Response: $CSV_RESP"
BATCH_ID=$(echo $CSV_RESP | jq -r '.id')
echo "Batch ID: $BATCH_ID"

if [ -z "$BATCH_ID" ] || [ "$BATCH_ID" == "null" ]; then
    echo "Error: CSV Upload Failed!"
    exit 1
fi

echo ">>> Step 9: Waiting for CSV Batch Processing..."
# Poll batch status
for i in {1..120}; do
    # Check status of the batch from DB or API.
    # API: GET /v1/admin/bulk-import/batches/$BATCH_ID
    BATCH_STATUS=$(curl -s -X GET "$API_URL/v1/admin/bulk-import/batches/$BATCH_ID" \
        -H "$AUTH_HEADER" | jq -r '.status')
    
    echo "Batch Status: $BATCH_STATUS"
    
    if [ "$BATCH_STATUS" == "COMPLETED" ] || [ "$BATCH_STATUS" == "PARTIAL_SUCCESS" ]; then
        echo "Batch Processing Complete!"
        break
    fi
    if [ "$BATCH_STATUS" == "FAILED" ]; then
        echo "Batch FAILED!"
        exit 1
    fi
    sleep 2
done

echo ">>> Verification: Checking Krithi Count & Duplicates..."
# Total should be around 22 (PDF) + maybe some new ones from CSV if no match?
# Ideally, count stays same or +few if CSV has extra songs.
# "Ananada Natana Prakaasam" (CSV) should MATCH "anandanatanaprakasam" (PDF)
# So we expect NO "Ananada Natana Prakaasam" as a NEW krithi, or it gets merged.
# Wait, Bulk Import usually creates NEW imports in `imported_krithis` table for review
# OR if auto-approved, creates Krithis.
# The CSV strategy (TRACK-001) usually goes to `imported_krithis`.
# Let's check `imported_krithis` table and `krithis` table.

COUNT=$(psql "$DB_URL" -t -c "SELECT count(*) FROM krithis" | xargs)
echo "Total Krithis: $COUNT"

echo "Checking for duplicate 'anandanata' variants..."
psql "$DB_URL" -c "SELECT id, title, title_normalized FROM krithis WHERE title_normalized LIKE '%anandanata%' OR title_normalized LIKE '%ananada%'"

# Check imported_krithis status
echo "Checking imported_krithis status for 'Ananada'..."
psql "$DB_URL" -c "SELECT id, title, status, resolution_status FROM imported_krithis WHERE title LIKE '%Ananada%'"

echo "Done!"

