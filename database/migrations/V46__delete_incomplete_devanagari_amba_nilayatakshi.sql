-- amba nIlAyatAkshi: Devanagari variant has only 2 lyric sections (missing Pallavi)
-- due to parser bug where unlabeled leading block couldn't match canonical PALLAVI.
-- Delete so re-extraction with the fixed parser recreates it with all 3 sections.

DELETE FROM krithi_lyric_sections
WHERE lyric_variant_id = '4281b4e2-a6d9-4122-9340-bc32faa0fad7';

DELETE FROM krithi_lyric_variants
WHERE id = '4281b4e2-a6d9-4122-9340-bc32faa0fad7';

INSERT INTO audit_log (entity_table, entity_id, action, diff)
VALUES (
    'krithi_lyric_variants',
    '4281b4e2-a6d9-4122-9340-bc32faa0fad7',
    'DELETE',
    '{"reason": "incomplete Devanagari variant (2/3 sections) for amba nIlAyatAkshi — will be recreated by re-extraction with fixed parser"}'
);
