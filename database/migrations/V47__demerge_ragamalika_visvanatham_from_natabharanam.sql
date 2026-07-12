-- V47: Demerge Raga Malika "viSva nAthaM bhajEhaM" variants from the Natābharanam krithi
--
-- Two distinct Dikshitar compositions share the title "viSva nAthaM bhajEhaM":
--   1. Krithi in raga Natābharanam (ab99b150) — correctly created
--   2. Raga Malika composition — incorrectly merged into (1) during import
--
-- The 6 lyric variants currently on ab99b150 all contain ragamalika content
-- (raga subsection markers like "1. SrI rAgaM", "2. Arabhi rAgaM") from
-- source "dikshitar-kriti-visvanatham-bhajeham.html". They must be removed.
--
-- Both import records mapped to ab99b150 are incorrect and need to be unmapped
-- so re-extraction can create the Raga Malika as a separate krithi.

-- 1. Delete lyric sections for the 6 ragamalika variants
DELETE FROM krithi_lyric_sections
WHERE lyric_variant_id IN (
    '12568679-e01e-4af3-a25c-e93bded006f0',
    '9b80c690-88b9-405c-be6f-b410e267ae24',
    '8d0601f0-7f9d-4018-bf81-ab6edabeb9fa',
    '703bc613-1302-4bf7-8a58-e6a12df7869e',
    '46237d02-75cb-4f42-b4ca-7bde48669064',
    'c33d674f-af93-44bd-95df-f5294551e8cf'
);

-- 2. Delete the 6 lyric variants themselves
DELETE FROM krithi_lyric_variants
WHERE id IN (
    '12568679-e01e-4af3-a25c-e93bded006f0',
    '9b80c690-88b9-405c-be6f-b410e267ae24',
    '8d0601f0-7f9d-4018-bf81-ab6edabeb9fa',
    '703bc613-1302-4bf7-8a58-e6a12df7869e',
    '46237d02-75cb-4f42-b4ca-7bde48669064',
    'c33d674f-af93-44bd-95df-f5294551e8cf'
)
AND krithi_id = 'ab99b150-7244-4518-a7d0-557fd14f2995';

-- 3. Unmap import record for "sri-visvanatham.html" (Raga Malika source)
UPDATE imported_krithis
SET mapped_krithi_id = NULL,
    import_status = 'pending'
WHERE id = 'dfdd8b35-08c5-455a-8dc7-e3983c82b911'
  AND mapped_krithi_id = 'ab99b150-7244-4518-a7d0-557fd14f2995';

-- 4. Unmap import record for "visvanatham-bhajeham.html" (also ragamalika content)
UPDATE imported_krithis
SET mapped_krithi_id = NULL,
    import_status = 'pending'
WHERE id = '3b2eafc3-64e5-4b76-9932-9f1d18acd256'
  AND mapped_krithi_id = 'ab99b150-7244-4518-a7d0-557fd14f2995';

-- 5. Reset extraction queue entries to PENDING for re-extraction
UPDATE extraction_queue
SET status = 'PENDING',
    attempts = 0,
    claimed_at = NULL,
    claimed_by = NULL,
    result_payload = NULL,
    result_count = NULL,
    extraction_method = NULL,
    extractor_version = NULL,
    confidence = NULL,
    duration_ms = NULL,
    error_detail = NULL,
    last_error_at = NULL
WHERE id IN (
    'c3c1b9e9-20af-410c-bbf9-4d948dfff16a',
    '878717c6-47b4-487f-b53a-8e3b1ab7ac21'
);

-- 6. Audit log
INSERT INTO audit_log (entity_table, entity_id, action, metadata)
VALUES
    ('krithis', 'ab99b150-7244-4518-a7d0-557fd14f2995', 'DEMERGE_VARIANTS',
     '{"reason": "Removed 6 ragamalika lyric variants incorrectly merged from visvanatham-bhajeham extraction", "migration": "V47"}'),
    ('imported_krithis', 'dfdd8b35-08c5-455a-8dc7-e3983c82b911', 'UNMAP',
     '{"reason": "Raga Malika source incorrectly mapped to Natabharanam krithi", "migration": "V47"}'),
    ('imported_krithis', '3b2eafc3-64e5-4b76-9932-9f1d18acd256', 'UNMAP',
     '{"reason": "Ragamalika content incorrectly mapped to Natabharanam krithi", "migration": "V47"}'),
    ('extraction_queue', 'c3c1b9e9-20af-410c-bbf9-4d948dfff16a', 'RESET_TO_PENDING',
     '{"reason": "Re-extract as new Raga Malika krithi", "migration": "V47"}'),
    ('extraction_queue', '878717c6-47b4-487f-b53a-8e3b1ab7ac21', 'RESET_TO_PENDING',
     '{"reason": "Re-extract as new Raga Malika krithi", "migration": "V47"}');
