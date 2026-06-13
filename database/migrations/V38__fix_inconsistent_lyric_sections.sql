-- Phase 4A: Fix lyric section inconsistency across language variants
--
-- Problem: 435/473 krithis have inconsistent krithi_lyric_sections counts
-- across their 6 language variants. Root causes:
--   1. Madhyama Kala Sahitya stored as top-level krithi_sections
--   2. Dual-format (continuous + word-division) creating duplicate sections
--   3. Variant sections mapped by position index instead of type
--
-- Fix approach:
--   Step 1: Remove MKS from krithi_sections (demote to sub-section)
--   Step 2: Remove duplicate sections from dual-format parsing
--   Step 3: Remove orphaned krithi_lyric_sections
--   Step 4: Re-index order_index sequentially

BEGIN;

-- Step 1: Delete krithi_lyric_sections linked to MKS krithi_sections
DELETE FROM krithi_lyric_sections
WHERE section_id IN (
    SELECT id FROM krithi_sections WHERE section_type = 'MADHYAMA_KALA'
);

-- Step 1b: Delete MKS from krithi_sections
DELETE FROM krithi_sections WHERE section_type = 'MADHYAMA_KALA';

-- Step 2: Remove duplicate sections caused by dual-format parsing.
-- When a krithi has multiple sections of the same type at different order_index,
-- and the second one has no lyric_sections text, it's a dual-format duplicate.
-- Keep the first occurrence (lower order_index) of each type per krithi.
DELETE FROM krithi_lyric_sections
WHERE section_id IN (
    SELECT ks.id
    FROM krithi_sections ks
    WHERE EXISTS (
        SELECT 1 FROM krithi_sections ks2
        WHERE ks2.krithi_id = ks.krithi_id
          AND ks2.section_type = ks.section_type
          AND ks2.order_index < ks.order_index
    )
);

DELETE FROM krithi_sections ks
WHERE EXISTS (
    SELECT 1 FROM krithi_sections ks2
    WHERE ks2.krithi_id = ks.krithi_id
      AND ks2.section_type = ks.section_type
      AND ks2.order_index < ks.order_index
);

-- Step 3: Remove orphaned krithi_lyric_sections (section_id no longer exists)
DELETE FROM krithi_lyric_sections kls
WHERE NOT EXISTS (
    SELECT 1 FROM krithi_sections ks WHERE ks.id = kls.section_id
);

-- Step 4: Re-index order_index to be sequential with no gaps per krithi
WITH ranked AS (
    SELECT id, krithi_id,
           ROW_NUMBER() OVER (PARTITION BY krithi_id ORDER BY order_index) AS new_order
    FROM krithi_sections
)
UPDATE krithi_sections
SET order_index = ranked.new_order
FROM ranked
WHERE krithi_sections.id = ranked.id
  AND krithi_sections.order_index != ranked.new_order;

-- Step 5: Log the repair to audit_log
INSERT INTO audit_log (entity_table, entity_id, action, diff)
SELECT 'krithis', k.id, 'REPAIR_SECTIONS',
       jsonb_build_object('phase', '4A', 'fix', 'MKS demotion + dual-format merge',
                          'sections', (SELECT COUNT(*) FROM krithi_sections ks WHERE ks.krithi_id = k.id))
FROM krithis k
WHERE EXISTS (SELECT 1 FROM krithi_sections ks WHERE ks.krithi_id = k.id);

COMMIT;
