-- V59: TRACK-133 Bucket C (Group 2 — canon over-count corrections, the two
-- UNAMBIGUOUS cases). A spurious section produced by a variant mis-split is
-- MERGED into its correct neighbour (preserving the sahitya text) and removed —
-- never a blind delete. Musicologist adjudication (TRACK-133, 2026-09-02):
--
--   * Raanidi Raadu     P,A,C,A -> P,A,C : the trailing ANUPALLAVI sits AFTER the
--                       CHARANAM (structurally impossible) — it is a mislabelled
--                       charanam tail. Merge section order 4 UP into order 3.
--   * ramA ramaNa rArA  8 -> 7           : charanam falsely split in en+ta at the
--                       lyric word "tvac-caraNam". Merge section order 6 UP into
--                       order 5 (sa/te/kn/ml already read 7).
--
-- Alakalallalaadaga (the third Group-2 case) is DELIBERATELY EXCLUDED here — its
-- merge direction is a lakshana judgement still under musicologist confirmation.
-- It will land in a later migration once the keep/drop boundary is confirmed.
--
-- Delivery vehicle = Flyway versioned migration (survives db-reset / CI, checksum-
-- tracked), consistent with V45/V46/V47/V58. Each merge writes AUDIT_LOG.
-- SAFETY: sections are resolved by (title, canonical order_index); each block
-- asserts the post-merge section count and rolls back loudly on mismatch. Absent
-- krithi = no-op (fresh reset before corpus load).
--
-- Ref: application_documentation/01-requirements/domain-model.md (§6.1 forms)

DO $$
DECLARE
    r           RECORD;
    v_krithi    uuid;
    v_keep      uuid;
    v_drop      uuid;
    v_drop_ord  int;
    v_remaining int;
BEGIN
    FOR r IN
        SELECT * FROM (VALUES
            ('Raanidi Raadu',    3, 4, 3),   -- keep order 3, drop order 4 -> 3 sections
            ('ramA ramaNa rArA', 5, 6, 7)    -- keep order 5, drop order 6 -> 7 sections
        ) AS t(title, keep_ord, drop_ord, expected_sections)
    LOOP
        SELECT id INTO v_krithi FROM krithis WHERE title = r.title;
        IF v_krithi IS NULL THEN
            RAISE NOTICE 'V59: krithi "%" not present — skipping (no-op)', r.title;
            CONTINUE;
        END IF;

        SELECT id INTO v_keep FROM krithi_sections WHERE krithi_id = v_krithi AND order_index = r.keep_ord;
        SELECT id, order_index INTO v_drop, v_drop_ord FROM krithi_sections WHERE krithi_id = v_krithi AND order_index = r.drop_ord;
        IF v_keep IS NULL OR v_drop IS NULL THEN
            RAISE EXCEPTION 'V59: "%" missing keep(%)/drop(%) section — structure differs from adjudication', r.title, r.keep_ord, r.drop_ord;
        END IF;

        -- 2a. variants having BOTH sections: append dropped text onto the kept one
        UPDATE krithi_lyric_sections a
           SET text            = btrim(a.text) || E'\n' || btrim(b.text),
               normalized_text = btrim(COALESCE(a.normalized_text,'')) || E'\n' || btrim(COALESCE(b.normalized_text,'')),
               updated_at      = now()
        FROM krithi_lyric_sections b
        WHERE a.section_id = v_keep AND b.section_id = v_drop
          AND a.lyric_variant_id = b.lyric_variant_id;

        -- 2b. variants having ONLY the dropped section: repoint to the kept one
        UPDATE krithi_lyric_sections b SET section_id = v_keep, updated_at = now()
        WHERE b.section_id = v_drop
          AND NOT EXISTS (SELECT 1 FROM krithi_lyric_sections a
                          WHERE a.section_id = v_keep AND a.lyric_variant_id = b.lyric_variant_id);

        -- 2c. drop the spurious section (cascade clears any now-merged leftovers)
        DELETE FROM krithi_sections WHERE id = v_drop;

        -- 2d. close the order_index gap so numbering stays contiguous
        UPDATE krithi_sections SET order_index = order_index - 1, updated_at = now()
        WHERE krithi_id = v_krithi AND order_index > v_drop_ord;

        SELECT count(*) INTO v_remaining FROM krithi_sections WHERE krithi_id = v_krithi;
        IF v_remaining <> r.expected_sections THEN
            RAISE EXCEPTION 'V59: "%" left % sections after merge, expected %', r.title, v_remaining, r.expected_sections;
        END IF;

        INSERT INTO audit_log (entity_table, entity_id, action, diff, metadata)
        VALUES ('krithi_sections', v_krithi, 'MERGE_SECTIONS',
            jsonb_build_object(
                'reason', 'TRACK-133 Bucket C: merged spurious mis-split section into its correct neighbour; canon over-counted by one',
                'kept_section', v_keep, 'dropped_section', v_drop, 'remaining_sections', v_remaining),
            jsonb_build_object('migration', 'V59', 'track', 'TRACK-133', 'title', r.title));

        RAISE NOTICE 'V59: "%" — merged order % into %, % sections remain', r.title, r.drop_ord, r.keep_ord, v_remaining;
    END LOOP;
END $$;
