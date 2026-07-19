---
name: extraction-debugger
description: Debug extraction pipeline failures. Check extraction queue status, read worker logs, compare Python extractor output with Kotlin processor results, and diagnose matching failures. Use when extractions fail, produce unexpected results, or matching rates are low.
---

# Extraction Debugger

Use this skill to diagnose issues in the PDF extraction → Kotlin processing pipeline.

## 1. Check Extraction Queue

```sql
-- Overall status
SELECT status, count(*) FROM extraction_queue GROUP BY status ORDER BY status;

-- Recent failures
SELECT id, source_name, source_url, status, error_message, updated_at
FROM extraction_queue
WHERE status IN ('FAILED', 'ERROR')
ORDER BY updated_at DESC
LIMIT 10;

-- Stuck jobs (submitted but not processing)
SELECT id, source_name, status, created_at
FROM extraction_queue
WHERE status = 'SUBMITTED'
  AND created_at < NOW() - INTERVAL '30 minutes'
ORDER BY created_at;
```

## 2. Check Worker Logs

```bash
# Docker extraction worker logs
docker compose logs extraction --tail=100

# Filter for errors only
docker compose logs extraction --tail=200 | grep -i "error\|exception\|traceback"

# Check if worker is running
docker compose ps extraction
```

## 3. Diagnose Matching Failures

```sql
-- Unmatched extractions (routed to imported_krithis as PENDING)
SELECT ik.id, ik.raw_title, ik.status, ik.import_source, ik.created_at
FROM imported_krithis ik
WHERE ik.status = 'PENDING'
  AND ik.import_source = 'PDF Extraction (Unmatched)'
ORDER BY ik.created_at DESC
LIMIT 20;

-- Match rate for a specific source
SELECT
  eq.source_name,
  COUNT(*) AS total_extractions,
  COUNT(se.id) AS matched,
  COUNT(*) - COUNT(se.id) AS unmatched
FROM extraction_queue eq
LEFT JOIN source_evidence se ON se.source_url = eq.source_url
WHERE eq.source_name ILIKE '%mdeng%'
GROUP BY eq.source_name;
```

## 4. Compare Python vs Kotlin Output

### Check what Python extracted
```sql
-- Raw extraction results (stored as JSONB)
SELECT id, source_name,
  result->>'title' AS extracted_title,
  result->>'raga' AS extracted_raga,
  result->>'tala' AS extracted_tala,
  jsonb_array_length(result->'sections') AS section_count
FROM extraction_queue
WHERE status = 'COMPLETED'
ORDER BY updated_at DESC
LIMIT 10;
```

### Check what Kotlin persisted
```sql
-- For a specific krithi, compare sections across variants
SELECT k.title, klv.language, klv.script,
       ks.section_type, ks.order_index, ks.label,
       LEFT(kls.text, 80) AS text_preview
FROM krithis k
JOIN krithi_lyric_variants klv ON klv.krithi_id = k.id
JOIN krithi_sections ks ON ks.krithi_id = k.id
LEFT JOIN krithi_lyric_sections kls ON kls.lyric_variant_id = klv.id AND kls.section_id = ks.id
WHERE k.title ILIKE '%SEARCH_TERM%'
ORDER BY klv.language, ks.order_index;
```

## 5. Common Issues

| Symptom | Likely Cause | Fix |
|:---|:---|:---|
| Extraction stuck in SUBMITTED | Worker not running or crashed | `docker compose restart extraction` |
| All extractions FAILED | Worker can't reach DB | Check `DATABASE_URL` in compose.yaml |
| 0 matches from PDF | Title normalization mismatch | Check `NameNormalizationService` rules |
| Sections inconsistent across variants | Parser script-specific bug | Check `structure_parser.py` per-script logic |
| "UnsupportedProtocol" error | Using file:// URI without volume mount | Use `/app/pdfs/` path with mounted volume |
| "current transaction is aborted" | Missing rollback after DB error | Check worker.py for rollback after exceptions |

## 6. Re-run Extraction

To re-extract after fixing a bug:

```bash
# Reset database and re-import
make db-reset
make seed

# Then re-submit extraction via API or direct SQL
```

Or selectively reset specific extractions:

```sql
-- Reset failed extractions to re-process
UPDATE extraction_queue SET status = 'SUBMITTED', error_message = NULL
WHERE status = 'FAILED' AND source_name = 'mdeng.pdf';
```

## 7. Volume Mount Verification

Ensure the extraction worker sees your latest code:

```bash
# Check volume mounts
docker compose config | grep -A5 "extraction"

# Verify src is mounted (should show local files)
docker compose exec extraction ls -la /app/src/

# Check PDF volume mount
docker compose exec extraction ls -la /app/pdfs/
```
