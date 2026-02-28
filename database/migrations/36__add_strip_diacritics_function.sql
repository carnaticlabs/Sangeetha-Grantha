-- 36__add_strip_diacritics_function.sql
-- Purpose: Add strip_diacritics() function for IAST → ASCII search normalisation.
-- Used to populate title_normalized and incipit_normalized columns.
-- Ref: application_documentation/04-database/schema.md

-- migrate:up
CREATE OR REPLACE FUNCTION strip_diacritics(input_text TEXT)
RETURNS TEXT
LANGUAGE plpgsql IMMUTABLE STRICT
AS $$
DECLARE
    result TEXT := input_text;
BEGIN
    -- Macron vowels
    result := replace(result, 'ā', 'a');
    result := replace(result, 'Ā', 'A');
    result := replace(result, 'ī', 'i');
    result := replace(result, 'Ī', 'I');
    result := replace(result, 'ū', 'u');
    result := replace(result, 'Ū', 'U');
    result := replace(result, 'ē', 'e');
    result := replace(result, 'Ē', 'E');
    result := replace(result, 'ō', 'o');
    result := replace(result, 'Ō', 'O');

    -- Retroflex consonants
    result := replace(result, 'ṭ', 't');
    result := replace(result, 'Ṭ', 'T');
    result := replace(result, 'ḍ', 'd');
    result := replace(result, 'Ḍ', 'D');
    result := replace(result, 'ṇ', 'n');
    result := replace(result, 'Ṇ', 'N');

    -- Palatal/velar nasals
    result := replace(result, 'ṅ', 'n');
    result := replace(result, 'Ṅ', 'N');
    result := replace(result, 'ñ', 'n');
    result := replace(result, 'Ñ', 'N');

    -- Sibilants
    result := replace(result, 'ś', 's');
    result := replace(result, 'Ś', 'S');
    result := replace(result, 'ṣ', 's');
    result := replace(result, 'Ṣ', 'S');

    -- Anusvara and visarga
    result := replace(result, 'ṃ', 'm');
    result := replace(result, 'Ṃ', 'M');
    result := replace(result, 'ḥ', 'h');
    result := replace(result, 'Ḥ', 'H');

    -- Vocalic R
    result := replace(result, 'ṛ', 'r');
    result := replace(result, 'Ṛ', 'R');
    result := replace(result, 'ṝ', 'r');
    result := replace(result, 'Ṝ', 'R');

    -- Vocalic L
    result := replace(result, 'ḷ', 'l');
    result := replace(result, 'Ḷ', 'L');

    -- Chandrabindu and other marks (strip entirely)
    result := replace(result, 'ँ', '');
    result := replace(result, 'ं', '');
    result := replace(result, 'ः', '');

    RETURN result;
END;
$$;

-- migrate:down
DROP FUNCTION IF EXISTS strip_diacritics(TEXT);
