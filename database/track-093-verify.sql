-- TRACK-093 re-import verification (run after each bulk-import batch).
--
-- Usage:
--   docker exec -i sangeetha-grantha-db-1 psql -U postgres -d sangita_grantha \
--     < database/track-093-verify.sql
--
-- What "closing 093" needs to see:
--   §1 batch reached SUCCEEDED (some FAILED tasks are normal — retry in the UI)
--   §3 every approved krithi carries a versioned-canon revision (TRACK-117)
--   §4/§5/§6 all return ZERO rows (no coverage/junction/lyric gaps)
--   §7 the N5 provenance lineage resolves in one query

\pset pager off

\echo '== 1. Batch + task health (10 most recent) =='
SELECT b.id, left(b.source_manifest, 34) AS manifest, b.status,
       b.total_tasks AS total, b.succeeded_tasks AS ok, b.failed_tasks AS failed
FROM import_batch b ORDER BY b.created_at DESC LIMIT 10;

\echo '== 2. Import review funnel (per status) =='
SELECT import_status, count(*) FROM imported_krithis GROUP BY import_status ORDER BY 1;

\echo '== 3. Versioned-canon coverage (TRACK-117 / ADR-014) =='
SELECT
  (SELECT count(*) FROM krithis)                                                AS krithis,
  (SELECT count(DISTINCT krithi_id) FROM krithi_revisions)                      AS krithis_with_revision,
  (SELECT count(*) FROM krithi_revisions)                                       AS revisions,
  (SELECT count(*) FROM krithi_revisions WHERE extraction_id IS NOT NULL)       AS attr_extraction,
  (SELECT count(*) FROM krithi_revisions WHERE created_by_user_id IS NOT NULL)  AS attr_user,
  (SELECT count(*) FROM source_documents)                                       AS source_documents;

\echo '== 4. GAP: krithis with no revision (expect 0 for import-path krithis) =='
SELECT k.id, left(k.title, 44) AS title, k.workflow_state
FROM krithis k
WHERE NOT EXISTS (SELECT 1 FROM krithi_revisions r WHERE r.krithi_id = k.id)
ORDER BY k.created_at DESC LIMIT 25;

\echo '== 5. GAP: krithis with no raga junction row (silent-failure guard) =='
SELECT k.id, left(k.title, 44) AS title,
       k.primary_raga_id IS NOT NULL AS has_primary_fk
FROM krithis k
WHERE (SELECT count(*) FROM krithi_ragas kr WHERE kr.krithi_id = k.id) = 0
ORDER BY k.created_at DESC LIMIT 25;

\echo '== 6. GAP: krithis with sections but zero lyric text (the failure that paused 093) =='
SELECT k.id, left(k.title, 40) AS title,
       (SELECT count(*) FROM krithi_sections s WHERE s.krithi_id = k.id) AS sections
FROM krithis k
WHERE (SELECT count(*) FROM krithi_sections s WHERE s.krithi_id = k.id) > 0
  AND (SELECT count(*) FROM krithi_lyric_sections ls
         JOIN krithi_lyric_variants v ON v.id = ls.lyric_variant_id
         WHERE v.krithi_id = k.id) = 0
ORDER BY k.created_at DESC LIMIT 25;

\echo '== 7. N5 provenance spot-check: full lineage of the newest revised krithi (one query) =='
SELECT sr.section_type, sr.order_index, left(sr.text, 28) AS text,
       eq.extractor_version, left(sd.source_url, 40) AS source_url, reg.name AS registry
FROM krithi_sections_asof(
       (SELECT krithi_id FROM krithi_revisions ORDER BY recorded_at DESC LIMIT 1), now()) sr
LEFT JOIN extraction_queue eq  ON eq.id = sr.extraction_id
LEFT JOIN source_documents sd  ON sd.id = sr.source_document_id
LEFT JOIN import_sources   reg ON reg.id = sd.import_source_id
ORDER BY sr.order_index;
