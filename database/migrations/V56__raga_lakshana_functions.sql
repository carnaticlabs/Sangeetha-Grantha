-- V56: TRACK-136 / ADR-017 Phase 3 — swara-token helpers for standing lakshana checks.
--
-- Parenthetical anya-swara notes are stripped so documented exceptions do not
-- false-positive the janya ⊂ parent check. Tokens are the Carnatic swara set
-- S / R1–3 / G1–3 / M1–2 / P / D1–3 / N1–3.
--
-- The standing queries themselves live in database/checks/raga_lakshana.sql
-- and run from `make raga-lakshana-checks` + CI (they must not RAISE during migrate).
--
-- Ref: application_documentation/02-architecture/decisions/ADR-017-raga-reference-entity-identity-resolution.md

CREATE OR REPLACE FUNCTION raga_swara_tokens(scale text)
RETURNS text[]
LANGUAGE sql
IMMUTABLE
AS $$
    SELECT COALESCE(
        (SELECT array_agg(tok ORDER BY tok)
           FROM (
               SELECT DISTINCT upper(m[1]) AS tok
                 FROM regexp_matches(
                     regexp_replace(coalesce(scale, ''), '\([^)]*\)', ' ', 'g'),
                     '(S|R[123]|G[123]|M[12]|P|D[123]|N[123])',
                     'g'
                 ) AS m
           ) d),
        ARRAY[]::text[]
    );
$$;

CREATE OR REPLACE FUNCTION raga_swara_signature(arohanam text, avarohanam text)
RETURNS text
LANGUAGE sql
IMMUTABLE
AS $$
    SELECT array_to_string(
        ARRAY(
            SELECT DISTINCT tok
              FROM unnest(
                  raga_swara_tokens(arohanam) || raga_swara_tokens(avarohanam)
              ) AS tok
             ORDER BY tok
        ),
        ' '
    );
$$;

COMMENT ON FUNCTION raga_swara_tokens(text) IS
    'TRACK-136: unique swara tokens in a scale string; parentheticals ignored.';
COMMENT ON FUNCTION raga_swara_signature(text, text) IS
    'TRACK-136: normalised swara-set of arohana+avarohana for lakshana / scale-collision.';
