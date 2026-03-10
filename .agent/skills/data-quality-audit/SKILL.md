---
name: data-quality-audit
description: Run diagnostic SQL queries against the database and generate a data quality report covering section consistency, evidence coverage, pending imports, and lyric variant completeness. Use after bulk imports, extraction runs, or migration repairs.
---

# Data Quality Audit

Run this audit after any data-modifying operation (bulk import, PDF extraction, migration repair) to verify data integrity.

## 1. Summary Counts

```sql
SELECT
  (SELECT count(*) FROM krithis) AS total_krithis,
  (SELECT count(*) FROM krithis k WHERE EXISTS (SELECT 1 FROM source_evidence se WHERE se.krithi_id = k.id)) AS krithis_with_evidence,
  (SELECT count(*) FROM krithi_sections) AS total_sections,
  (SELECT count(*) FROM krithi_lyric_sections) AS total_lyric_sections,
  (SELECT count(*) FROM krithi_lyric_variants) AS total_lyric_variants,
  (SELECT count(*) FROM imported_krithis WHERE status = 'PENDING') AS pending_imports,
  (SELECT count(*) FROM imported_krithis WHERE status = 'APPROVED') AS approved_imports,
  (SELECT count(*) FROM imported_krithis WHERE status = 'REJECTED') AS rejected_imports;
```

## 2. Section Consistency Check

Krithis where lyric section counts differ across language variants (should return 0 rows):

```sql
SELECT k.id, k.title, klv.language, COUNT(kls.id) AS section_count
FROM krithis k
JOIN krithi_lyric_variants klv ON klv.krithi_id = k.id
LEFT JOIN krithi_lyric_sections kls ON kls.lyric_variant_id = klv.id
GROUP BY k.id, k.title, klv.language
HAVING COUNT(kls.id) != (
    SELECT COUNT(ks.id) FROM krithi_sections ks WHERE ks.krithi_id = k.id
)
ORDER BY k.title, klv.language;
```

## 3. Zero-Section Variants

Lyric variants with no sections (should return 0 rows):

```sql
SELECT k.id, k.title, klv.language, klv.script
FROM krithis k
JOIN krithi_lyric_variants klv ON klv.krithi_id = k.id
LEFT JOIN krithi_lyric_sections kls ON kls.lyric_variant_id = klv.id
WHERE kls.id IS NULL
ORDER BY k.title;
```

## 4. Evidence Coverage

Krithis without any source evidence:

```sql
SELECT k.id, k.title
FROM krithis k
WHERE NOT EXISTS (SELECT 1 FROM source_evidence se WHERE se.krithi_id = k.id)
ORDER BY k.title;
```

## 5. Variant Completeness

Krithis that don't have exactly 6 lyric variants (one per language/script):

```sql
SELECT k.id, k.title, COUNT(klv.id) AS variant_count
FROM krithis k
LEFT JOIN krithi_lyric_variants klv ON klv.krithi_id = k.id
GROUP BY k.id, k.title
HAVING COUNT(klv.id) != 6
ORDER BY variant_count, k.title;
```

## 6. Missing Pallavi Check

Krithis without a PALLAVI section (every krithi must have at minimum a Pallavi):

```sql
SELECT k.title FROM krithis k
WHERE NOT EXISTS (
    SELECT 1 FROM krithi_sections ks
    WHERE ks.krithi_id = k.id AND ks.section_type = 'PALLAVI'
)
ORDER BY k.title;
```

## 7. Extraction Queue Status

```sql
SELECT status, count(*) FROM extraction_queue GROUP BY status ORDER BY status;
```

## Interpreting Results

| Check | Expected | Action if Failed |
|:---|:---|:---|
| Section consistency | 0 rows | Re-run section repair migration or investigate parser |
| Zero-section variants | 0 rows | Re-extract affected krithis or run repair script |
| Evidence coverage | 0 rows (ideally) | Submit missing sources for extraction |
| Variant completeness | 0 rows with != 6 | Check extraction pipeline for missing languages |
| Missing Pallavi | 0 rows | Structural parser bug — investigate source HTML/PDF |

## Usage

Run via MCP postgres tool or `psql`:
```bash
psql -h localhost -U postgres -d sangita_grantha -f <query>
```

Or use the MCP postgres tool directly in Claude Code for each query above.
