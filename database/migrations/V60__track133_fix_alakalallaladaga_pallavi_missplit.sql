-- V60: TRACK-133 Bucket C (Group 2 — the third canon case, `Alakalallalaadaga`).
-- Musicologist Round-2 adjudication (TRACK-133, 2026-09-02): this Utsava-Sampradaya
-- kriti has a PALLAVI that spans two lines — "alakalallalADaga kaniyA" +
-- "rAN-muniyeTu pongenO" — followed by ANUPALLAVI "celuvu mIraganu…" and one
-- CHARANAM. True structure = P, A, C = 3.
--
-- The Indic variants (sa,ta,te,kn,ml) ALREADY encode this correctly: pallavi line
-- 2 sits inside the pallavi section, celuvu is the anupallavi, charanam last — they
-- map to canonical orders 1,2,4 (nothing at order 3). Only the ENGLISH variant is
-- mis-split: it treats pallavi line 2 as a separate ANUPALLAVI (canonical order 2)
-- and pushes celuvu to order 3 — an off-by-one that created the phantom 4th
-- canonical section.
--
-- Fix (data-correct implementation of the adjudicated 3-section structure):
--   1. en pallavi (order 1) <- append en order-2 text (fold line 2 into pallavi)
--   2. en order-2 slot      <- en order-3 text (celuvu becomes the anupallavi)
--   3. delete en's order-3 row  => canonical order-3 section becomes childless
--   4. delete the now-childless canonical order-3 section
--   5. reindex canonical order 4 -> 3 (charanam)
-- Indic variants are left untouched (already correct), per adjudication.
--
-- Result: 3 canonical sections; en and Indic both map to P,A,C=3 with identical
-- section semantics. Self-asserting; rolls back if structure differs. AUDIT_LOG written.
--
-- Ref: application_documentation/01-requirements/domain-model.md (§6.1 forms)

DO $$
DECLARE
    v_krithi   uuid;
    s1 uuid; s2 uuid; s3 uuid; s4 uuid;      -- canonical section ids by order
    en_var     uuid;
    en_s3_txt  text; en_s3_norm text;
    v_indic    int;
    v_en       int;
    v_total    int;
BEGIN
    SELECT id INTO v_krithi FROM krithis WHERE title = 'Alakalallalaadaga';
    IF v_krithi IS NULL THEN
        RAISE NOTICE 'V60: "Alakalallalaadaga" not present — skipping (no-op)';
        RETURN;
    END IF;

    SELECT id INTO s1 FROM krithi_sections WHERE krithi_id = v_krithi AND order_index = 1;
    SELECT id INTO s2 FROM krithi_sections WHERE krithi_id = v_krithi AND order_index = 2;
    SELECT id INTO s3 FROM krithi_sections WHERE krithi_id = v_krithi AND order_index = 3;
    SELECT id INTO s4 FROM krithi_sections WHERE krithi_id = v_krithi AND order_index = 4;
    IF s1 IS NULL OR s2 IS NULL OR s3 IS NULL OR s4 IS NULL THEN
        RAISE EXCEPTION 'V60: "Alakalallalaadaga" does not have the expected 4 canonical sections — aborting';
    END IF;

    SELECT id INTO en_var FROM krithi_lyric_variants WHERE krithi_id = v_krithi AND language::text = 'en';
    IF en_var IS NULL THEN
        RAISE EXCEPTION 'V60: no English variant for "Alakalallalaadaga" — structure differs from adjudication';
    END IF;

    -- Capture en's order-3 (celuvu) text before we mutate.
    SELECT btrim(text), btrim(COALESCE(normalized_text,'')) INTO en_s3_txt, en_s3_norm
    FROM krithi_lyric_sections WHERE lyric_variant_id = en_var AND section_id = s3;
    IF en_s3_txt IS NULL THEN
        RAISE EXCEPTION 'V60: English variant has no order-3 (celuvu) row — structure differs from adjudication';
    END IF;

    -- 1. Fold en pallavi line 2 (order 2) up into the pallavi (order 1).
    UPDATE krithi_lyric_sections a
       SET text = btrim(a.text) || E'\n' || btrim(b.text),
           normalized_text = btrim(COALESCE(a.normalized_text,'')) || E'\n' || btrim(COALESCE(b.normalized_text,'')),
           updated_at = now()
    FROM krithi_lyric_sections b
    WHERE a.lyric_variant_id = en_var AND a.section_id = s1
      AND b.lyric_variant_id = en_var AND b.section_id = s2;

    -- 2. en order-2 slot becomes celuvu (the true anupallavi).
    UPDATE krithi_lyric_sections
       SET text = en_s3_txt, normalized_text = en_s3_norm, updated_at = now()
     WHERE lyric_variant_id = en_var AND section_id = s2;

    -- 3. remove en's order-3 row -> canonical s3 becomes childless.
    DELETE FROM krithi_lyric_sections WHERE lyric_variant_id = en_var AND section_id = s3;

    -- Safety: s3 must now have no variant rows at all before we drop it.
    IF EXISTS (SELECT 1 FROM krithi_lyric_sections WHERE section_id = s3) THEN
        RAISE EXCEPTION 'V60: canonical order-3 section still has variant rows after en cleanup — aborting (unexpected non-en mapping)';
    END IF;

    -- 4. drop the childless canonical section.
    DELETE FROM krithi_sections WHERE id = s3;

    -- 5. reindex charanam 4 -> 3.
    UPDATE krithi_sections SET order_index = 3, updated_at = now() WHERE id = s4;

    -- Verify: 3 canonical sections; en and every Indic variant map to 3.
    SELECT count(*) INTO v_total FROM krithi_sections WHERE krithi_id = v_krithi;
    SELECT count(*) INTO v_en FROM krithi_lyric_sections WHERE lyric_variant_id = en_var;
    SELECT min(c) INTO v_indic FROM (
        SELECT count(ls.id) c
        FROM krithi_lyric_variants v
        LEFT JOIN krithi_lyric_sections ls ON ls.lyric_variant_id = v.id
        WHERE v.krithi_id = v_krithi AND v.language::text <> 'en'
        GROUP BY v.id
    ) t;
    IF v_total <> 3 OR v_en <> 3 OR v_indic <> 3 THEN
        RAISE EXCEPTION 'V60: post-fix counts wrong (canonical=%, en=%, min-indic=%) — expected 3/3/3', v_total, v_en, v_indic;
    END IF;

    INSERT INTO audit_log (entity_table, entity_id, action, diff, metadata)
    VALUES ('krithi_sections', v_krithi, 'MERGE_SECTIONS',
        jsonb_build_object(
            'reason', 'TRACK-133 Bucket C: Alakalallalaadaga pallavi spans two lines; English variant was mis-split (line 2 as a separate anupallavi). Folded en pallavi line 2 back into the pallavi, remapped celuvu as the anupallavi, dropped the phantom canonical section. True structure P,A,C=3.',
            'dropped_section', s3, 'reindexed_charanam', s4),
        jsonb_build_object('migration', 'V60', 'track', 'TRACK-133', 'title', 'Alakalallalaadaga'));

    RAISE NOTICE 'V60: "Alakalallalaadaga" — fixed en pallavi mis-split, now 3 canonical sections';
END $$;
