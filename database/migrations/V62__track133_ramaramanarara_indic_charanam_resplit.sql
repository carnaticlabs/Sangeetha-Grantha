-- V62: TRACK-133 Bucket C — `ramA ramaNa rArA` Indic variant re-split (the last residual).
-- Canon is correct at 7 (P + 6 charanams); English & Tamil variants already read 7.
-- The four Indic variants (sa, te, kn, ml) under-segment: charanam 4 (raNAdhi… tvac-
-- caraNam … cEsunu) and charanam 5 (mukhAbjamunu … durmukhAsura haraNa) are GLUED into
-- one section at canonical order 5 — each ends with the pallavi-echo refrain "(ramA)" —
-- so the variants are shifted: oi5 = C4+C5, oi6 = C6 (birAna), oi7 = absent (6 sections).
-- Musicologist Round-2 adjudication: this is an under-segmentation defect, not a canon
-- error. Correct target (matching en/ta): oi5 = C4, oi6 = C5, oi7 = C6.
--
-- Fix per Indic variant: split oi5's text at the FIRST ")<newline>" (the refrain that
-- closes C4) — part A (C4) stays in oi5, part B (C5) replaces oi6, and the current oi6
-- text (C6 birAna) moves into a new oi7 row. normalized_text is blank on these rows and
-- is left blank (consistent with siblings). Self-guarding: acts only on a variant whose
-- oi5 still has the merged shape and whose oi7 row is absent; otherwise skips (no-op on a
-- corpus where the structure already differs). English/Tamil untouched. AUDIT_LOG written.
--
-- NOTE: the durable fix is a parser refrain-split (split a charanam at an internal pallavi
-- echo when more charanam text follows); until that lands, a fresh re-extract could
-- reintroduce the glue and this repair would need re-running. Tracked as a known parser gap.
--
-- Ref: application_documentation/01-requirements/domain-model.md (§6.1 forms)

DO $$
DECLARE
    v_krithi  uuid;
    s5 uuid; s6 uuid; s7 uuid;
    r         RECORD;
    t5 text; t6 text; p int; part_a text; part_b text;
    v_fixed int := 0;
    v_bad   int;
BEGIN
    SELECT id INTO v_krithi FROM krithis WHERE title = 'ramA ramaNa rArA';
    IF v_krithi IS NULL THEN
        RAISE NOTICE 'V62: "ramA ramaNa rArA" not present — skipping (no-op)';
        RETURN;
    END IF;

    SELECT id INTO s5 FROM krithi_sections WHERE krithi_id = v_krithi AND order_index = 5;
    SELECT id INTO s6 FROM krithi_sections WHERE krithi_id = v_krithi AND order_index = 6;
    SELECT id INTO s7 FROM krithi_sections WHERE krithi_id = v_krithi AND order_index = 7;
    IF s5 IS NULL OR s6 IS NULL OR s7 IS NULL THEN
        RAISE EXCEPTION 'V62: expected 7 canonical sections (orders 5,6,7 present) — structure differs, aborting';
    END IF;

    FOR r IN
        SELECT v.id AS variant_id, v.language::text AS lang
        FROM krithi_lyric_variants v
        WHERE v.krithi_id = v_krithi AND v.language::text IN ('sa','te','kn','ml')
    LOOP
        -- Guard: skip if already correct (oi7 row present) — makes the migration idempotent/no-op-safe.
        IF EXISTS (SELECT 1 FROM krithi_lyric_sections WHERE section_id = s7 AND lyric_variant_id = r.variant_id) THEN
            CONTINUE;
        END IF;

        SELECT btrim(text) INTO t5 FROM krithi_lyric_sections WHERE section_id = s5 AND lyric_variant_id = r.variant_id;
        SELECT btrim(text) INTO t6 FROM krithi_lyric_sections WHERE section_id = s6 AND lyric_variant_id = r.variant_id;
        IF t5 IS NULL OR t6 IS NULL THEN
            RAISE EXCEPTION 'V62: variant % missing oi5/oi6 rows — structure differs, aborting', r.lang;
        END IF;

        -- First ")" that closes a line = end of C4's refrain. (Inline "(x)" parens sit mid-line,
        -- never immediately before a newline, so this uniquely finds the refrain boundary.)
        p := position(')' || chr(10) in t5 || chr(10));
        IF p = 0 THEN
            RAISE EXCEPTION 'V62: variant % oi5 has no refrain-terminated line to split on — aborting', r.lang;
        END IF;

        part_a := btrim(left(t5, p));                    -- up to & incl the ")" -> C4
        part_b := btrim(substring(t5 from p + 2));       -- skip ")" and newline -> C5
        IF part_b = '' THEN
            RAISE EXCEPTION 'V62: variant % split produced empty C5 — aborting', r.lang;
        END IF;

        UPDATE krithi_lyric_sections SET text = part_a, normalized_text = '', updated_at = now()
         WHERE section_id = s5 AND lyric_variant_id = r.variant_id;                 -- oi5 = C4
        UPDATE krithi_lyric_sections SET text = part_b, normalized_text = '', updated_at = now()
         WHERE section_id = s6 AND lyric_variant_id = r.variant_id;                 -- oi6 = C5
        INSERT INTO krithi_lyric_sections (lyric_variant_id, section_id, text, normalized_text)
        VALUES (r.variant_id, s7, t6, '');                                          -- oi7 = C6 (moved)

        v_fixed := v_fixed + 1;
    END LOOP;

    -- Verify: no Indic variant of this krithi is left mismatched against canon (7).
    SELECT count(*) INTO v_bad FROM (
        SELECT v.id, count(ls.id) c
        FROM krithi_lyric_variants v
        LEFT JOIN krithi_lyric_sections ls ON ls.lyric_variant_id = v.id
        WHERE v.krithi_id = v_krithi
        GROUP BY v.id
        HAVING count(ls.id) <> 7
    ) t;
    IF v_bad <> 0 THEN
        RAISE EXCEPTION 'V62: % variant(s) still not at 7 sections after re-split', v_bad;
    END IF;

    IF v_fixed > 0 THEN
        INSERT INTO audit_log (entity_table, entity_id, action, diff, metadata)
        VALUES ('krithi_lyric_sections', v_krithi, 'RESPLIT_SECTIONS',
            jsonb_build_object('reason','TRACK-133: split glued charanams 4+5 in Indic variants at the internal pallavi-echo refrain; canon (7) unchanged',
                               'variants_fixed', v_fixed),
            jsonb_build_object('migration','V62','track','TRACK-133','title','ramA ramaNa rArA'));
    END IF;

    RAISE NOTICE 'V62: "ramA ramaNa rArA" — re-split % Indic variant(s) to 7 sections', v_fixed;
END $$;
