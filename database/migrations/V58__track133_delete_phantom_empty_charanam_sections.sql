-- V58: TRACK-133 Bucket C (Group 1 — STRAIGHTFORWARD, adjudicated & unambiguous).
--
-- Two krithis carry phantom-empty trailing CHARANAM rows in krithi_sections that
-- have ZERO lyric text in EVERY variant (English included) — an import artifact,
-- not a real section-count. Musicologist adjudication (TRACK-133, 2026-09-02):
--   * rAma sItA rAma        canon 10 -> true structure Pallavi + 5 Charanams = 6
--   * Rama Rama Rama Sita    canon 14 -> true structure Pallavi + 5 Charanams = 6
--
-- Delivery vehicle = Flyway versioned migration (not the curator/API path):
-- corpus-data repairs that must survive `make db-reset`/CI Testcontainers and be
-- checksum-tracked are already done this way here (see V45/V46/V47/V38). The
-- curator API is for interactive edits that a reset would discard.
--
-- SAFETY: the DELETE is self-verifying — it can only remove sections that have no
-- non-blank text in any variant, and it ASSERTS the surviving count is exactly 6,
-- rolling back loudly otherwise. krithi_lyric_sections.section_id is
-- ON DELETE CASCADE, so dangling empty lyric-section rows are removed with the
-- parent section. If a krithi is absent (fresh reset before corpus load) the block
-- is a no-op.
--
-- Ref: application_documentation/01-requirements/domain-model.md (§6.1 forms)

DO $$
DECLARE
    r            RECORD;
    v_krithi     uuid;
    v_deleted    int;
    v_remaining  int;
    v_nonempty   int;
BEGIN
    FOR r IN
        SELECT * FROM (VALUES
            ('rAma sItA rAma',     6),
            ('Rama Rama Rama Sita', 6)
        ) AS t(title, expected_sections)
    LOOP
        SELECT id INTO v_krithi FROM krithis WHERE title = r.title;

        IF v_krithi IS NULL THEN
            RAISE NOTICE 'V58: krithi "%" not present — skipping (no-op)', r.title;
            CONTINUE;
        END IF;

        -- Guard: the surviving (non-empty) sections must already equal the target.
        SELECT count(*) INTO v_nonempty
        FROM krithi_sections cs
        WHERE cs.krithi_id = v_krithi
          AND EXISTS (
              SELECT 1 FROM krithi_lyric_sections ls
              WHERE ls.section_id = cs.id
                AND COALESCE(btrim(ls.text), '') <> ''
          );
        IF v_nonempty <> r.expected_sections THEN
            RAISE EXCEPTION
              'V58: "%" has % non-empty sections, expected % — aborting (structure differs from adjudication)',
              r.title, v_nonempty, r.expected_sections;
        END IF;

        -- Remove the phantom-empty sections (cascade clears their empty lyric rows).
        WITH doomed AS (
            SELECT cs.id
            FROM krithi_sections cs
            WHERE cs.krithi_id = v_krithi
              AND NOT EXISTS (
                  SELECT 1 FROM krithi_lyric_sections ls
                  WHERE ls.section_id = cs.id
                    AND COALESCE(btrim(ls.text), '') <> ''
              )
        )
        DELETE FROM krithi_sections cs USING doomed d WHERE cs.id = d.id;
        GET DIAGNOSTICS v_deleted = ROW_COUNT;

        SELECT count(*) INTO v_remaining FROM krithi_sections WHERE krithi_id = v_krithi;
        IF v_remaining <> r.expected_sections THEN
            RAISE EXCEPTION 'V58: "%" left % sections after cleanup, expected % (deleted %)',
                r.title, v_remaining, r.expected_sections, v_deleted;
        END IF;

        INSERT INTO audit_log (entity_table, entity_id, action, diff, metadata)
        VALUES (
            'krithi_sections', v_krithi, 'DELETE',
            jsonb_build_object(
                'reason', 'TRACK-133 Bucket C: removed phantom-empty trailing charanam sections with no lyric text in any variant; true structure is Pallavi + 5 Charanams = 6',
                'deleted_sections', v_deleted,
                'remaining_sections', v_remaining
            ),
            jsonb_build_object('migration', 'V58', 'track', 'TRACK-133', 'title', r.title)
        );

        RAISE NOTICE 'V58: "%" — deleted % phantom-empty sections, % remain', r.title, v_deleted, v_remaining;
    END LOOP;
END $$;
