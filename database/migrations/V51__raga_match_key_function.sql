-- V51: TRACK-136 / ADR-017 — one DB-owned raga name-fold.
--
-- Identity is (raga_match_key(name), mela_disambiguator); this function is only
-- the *name* half. It is the sole definition of the fold: Kotlin ingestion
-- computes the key via SELECT raga_match_key(...) (ADR-012); Python must not
-- reimplement it.
--
-- Contract = TRACK-132 §1 as shipped in normalize_for_matching(..., "raga"):
--   fold   diacritics, case, spacing, ITRANS/HK caps (via lower),
--          aspirates th/dh/gh/kh/bh/jh→base, sh/ś→s, ch→c, w→v, oo→u, ee→i
--   keep   nn (Kanadā ≠ Kannada); terminal -i (Bhairavi); initial vowel (Abhogi);
--          digraphs mapped not deleted (Ranjani family)
-- Terminal -am is NOT stripped: Shankarabharanam must keep its final m
-- (TRACK-132 Python raga branch; Mohana/Mohanam pairs were merged by V50).
-- Honorific-word stripping is *not* applied — it would empty the key for the
-- seeded raga 'Sri'.
--
-- Ref: application_documentation/02-architecture/decisions/ADR-017-raga-reference-entity-identity-resolution.md

CREATE OR REPLACE FUNCTION raga_match_key(input_text text)
RETURNS text
LANGUAGE plpgsql
IMMUTABLE
STRICT
PARALLEL SAFE
AS $$
DECLARE
    result text := input_text;
    -- Combining Diacritical Marks U+0300–U+036F, for NFD leftovers (e.g. ḻ).
    combining text := U&'\0300\0301\0302\0303\0304\0305\0306\0307\0308\0309\030A\030B\030C\030D\030E\030F\0310\0311\0312\0313\0314\0315\0316\0317\0318\0319\031A\031B\031C\031D\031E\031F\0320\0321\0322\0323\0324\0325\0326\0327\0328\0329\032A\032B\032C\032D\032E\032F\0330\0331\0332\0333\0334\0335\0336\0337\0338\0339\033A\033B\033C\033D\033E\033F\0340\0341\0342\0343\0344\0345\0346\0347\0348\0349\034A\034B\034C\034D\034E\034F\0350\0351\0352\0353\0354\0355\0356\0357\0358\0359\035A\035B\035C\035D\035E\035F\0360\0361\0362\0363\0364\0365\0366\0367\0368\0369\036A\036B\036C\036D\036E\036F';
BEGIN
    result := strip_diacritics(result);
    result := translate(normalize(result, NFD), combining, '');
    result := lower(result);
    result := regexp_replace(result, '[^a-z0-9[:space:]]', ' ', 'g');

    -- MATCHING_COLLAPSE_RULES (longest first): ksh before sh; chh before ch.
    result := replace(result, 'ksh', 'ks');
    result := replace(result, 'chh', 'c');
    result := replace(result, 'sh', 's');
    result := replace(result, 'th', 't');
    result := replace(result, 'dh', 'd');
    result := replace(result, 'bh', 'b');
    result := replace(result, 'ph', 'p');
    result := replace(result, 'gh', 'g');
    result := replace(result, 'jh', 'j');
    result := replace(result, 'ch', 'c');
    result := replace(result, 'kh', 'k');

    result := replace(result, 'w', 'v');
    result := replace(result, 'aa', 'a');
    result := replace(result, 'ee', 'i');
    result := replace(result, 'oo', 'u');
    result := replace(result, 'uu', 'u');

    -- Strip a trailing raga-descriptor word ("... ragam"/"raga"/"ragamu") so
    -- "Sri rAgaM" folds to the raga NAME, not the descriptor (TRACK-136). Only a
    -- separate trailing token (preceded by space) is removed, so a bare "ragam"
    -- is never emptied. Mirrors normalize_for_matching(..., "raga") — keep in step.
    result := regexp_replace(result, '\s+raga[mu]*$', '', 'g');

    result := replace(result, ' ', '');
    result := btrim(result);
    RETURN result;
END;
$$;

-- Frozen acceptance suite (TRACK-136 §1.1): 2 same-key homonym pairs + 4 defensive cases.
DO $$
BEGIN
    IF raga_match_key('Kalāvathi') IS DISTINCT FROM raga_match_key('Kalāvati') THEN
        RAISE EXCEPTION 'TRACK-136 P1.1: Kalāvathi / Kalāvati must share match_key, got % / %',
            raga_match_key('Kalāvathi'), raga_match_key('Kalāvati');
    END IF;
    IF raga_match_key('Shreemati') IS DISTINCT FROM raga_match_key('Srimati') THEN
        RAISE EXCEPTION 'TRACK-136 P1.1: Shreemati / Srimati must share match_key, got % / %',
            raga_match_key('Shreemati'), raga_match_key('Srimati');
    END IF;

    IF raga_match_key('Kanadā') = raga_match_key('Kannada') THEN
        RAISE EXCEPTION 'TRACK-136 P1.1: nn must not de-double: Kanadā / Kannada both %',
            raga_match_key('Kanadā');
    END IF;
    IF raga_match_key('Bhairavi') IN (raga_match_key('Bhairava'), raga_match_key('Bhairavam')) THEN
        RAISE EXCEPTION 'TRACK-136 P1.1: terminal -i must be preserved: Bhairavi → %',
            raga_match_key('Bhairavi');
    END IF;
    IF raga_match_key('Abhogi') = raga_match_key('Bhogi') THEN
        RAISE EXCEPTION 'TRACK-136 P1.1: initial vowel must be preserved: Abhogi / Bhogi both %',
            raga_match_key('Abhogi');
    END IF;
    IF (SELECT count(DISTINCT k) FROM (VALUES
            (raga_match_key('Ranjani')),
            (raga_match_key('Niranjani')),
            (raga_match_key('Shreeranjani'))
        ) AS t(k)) <> 3 THEN
        RAISE EXCEPTION 'TRACK-136 P1.1: Ranjani family must stay 3 keys (digraphs mapped, not deleted)';
    END IF;

    -- Adjudicated MERGE pairs: the fold must actually collide (sanity on the function).
    IF raga_match_key('yadukula kAmbhOji') IS DISTINCT FROM raga_match_key('Yadukula Kāmbhoji') THEN
        RAISE EXCEPTION 'TRACK-136 P1.1: ITRANS Yadukula Kāmbhoji must fold onto the Wikipedia spelling';
    END IF;
    IF raga_match_key('Pūrvi') IS DISTINCT FROM raga_match_key('Poorvi') THEN
        RAISE EXCEPTION 'TRACK-136 P1.1: Pūrvi / Poorvi must share match_key';
    END IF;
    IF raga_match_key('ghurjari') IS DISTINCT FROM raga_match_key('Gurjari') THEN
        RAISE EXCEPTION 'TRACK-136 P1.1: ghurjari / Gurjari must share match_key (gh→g)';
    END IF;

    -- Trailing raga-descriptor is stripped; a bare descriptor is not emptied.
    IF raga_match_key('Sri rAgaM') IS DISTINCT FROM raga_match_key('Sri')
       OR raga_match_key('Sri') <> 'sri' THEN
        RAISE EXCEPTION 'TRACK-136 P1.1: "Sri rAgaM" must fold to the raga name sri, got %',
            raga_match_key('Sri rAgaM');
    END IF;
    IF raga_match_key('Ragam') <> 'ragam' THEN
        RAISE EXCEPTION 'TRACK-136 P1.1: bare descriptor "Ragam" must not be emptied, got %',
            raga_match_key('Ragam');
    END IF;
END;
$$;
