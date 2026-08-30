-- TRACK-136 §3.2 standing lakshana checks. Fail the session if a known-good
-- seed row is flagged (zero false positives). Scale-collision listing is
-- informational and does not RAISE — same-scale is often legitimate.
--
-- Run via: make raga-lakshana-checks
-- Ref: application_documentation/02-architecture/decisions/ADR-017-raga-reference-entity-identity-resolution.md

DO $$
DECLARE
    n int;
    sample text;
BEGIN
    -- janya ⊄ parent: a janya carrying a non-S/P swara its parent lacks.
    -- Documented anya swaras live in parentheses (stripped by raga_swara_tokens)
    -- or in ANya notes; Hindustani {…} rows are out of scope.
    SELECT count(*), string_agg(j.name, ', ' ORDER BY j.name)
      INTO n, sample
      FROM ragas j
      JOIN ragas p ON p.id = j.parent_raga_id
     WHERE j.parent_raga_id IS NOT NULL
       AND j.parent_raga_id <> j.id
       AND p.melakarta_number IS NOT NULL
       AND j.arohanam IS NOT NULL
       AND j.avarohanam IS NOT NULL
       AND p.arohanam IS NOT NULL
       AND p.avarohanam IS NOT NULL
       AND j.name NOT LIKE '%{%'
       AND j.arohanam !~* 'anya'
       AND j.avarohanam !~* 'anya'
       AND EXISTS (
           SELECT 1
             FROM unnest(raga_swara_tokens(j.arohanam) || raga_swara_tokens(j.avarohanam)) AS js(tok)
            WHERE js.tok NOT IN ('S', 'P')
              AND NOT (js.tok = ANY (raga_swara_tokens(p.arohanam) || raga_swara_tokens(p.avarohanam)))
       );
    IF n > 0 THEN
        RAISE EXCEPTION 'TRACK-136 janya⊄parent: % row(s): %', n, sample;
    END IF;

    -- mela-as-own-janya: self-parent whose scale is byte-identical to a melakarta.
    SELECT count(*), string_agg(j.name, ', ' ORDER BY j.name)
      INTO n, sample
      FROM ragas j
     WHERE j.parent_raga_id = j.id
       AND j.melakarta_number IS NULL
       AND EXISTS (
           SELECT 1
             FROM ragas m
            WHERE m.melakarta_number IS NOT NULL
              AND raga_swara_signature(j.arohanam, j.avarohanam)
                = raga_swara_signature(m.arohanam, m.avarohanam)
              AND raga_swara_signature(j.arohanam, j.avarohanam) <> ''
       );
    IF n > 0 THEN
        RAISE EXCEPTION 'TRACK-136 mela-as-own-janya: % row(s): %', n, sample;
    END IF;
END $$;
