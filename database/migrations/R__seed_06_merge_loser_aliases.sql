-- Repeatable: alias the TRACK-132 merge-loser spellings onto their keepers.
--
-- Runs AFTER R__seed_05 (alphabetical), so every loser row has already been
-- merged and DELETED by track132_apply_raga_merges(). Only losers whose fold
-- does NOT already reach the keeper need an alias (folding-equivalent twins like
-- 'yadukula kAmbhOji' resolve via match_key alone). Placing these in R__seed_04
-- would collide: the V40-seeded loser (e.g. 'Gauri' mela 15) still owns its
-- identity key until R__seed_05 runs.
--
-- Effect (ADR-017 acceptance criterion 1): a re-import of any of these exact
-- spellings RESOLVES to the keeper instead of queueing as unknown.
--
-- Ref: application_documentation/02-architecture/decisions/ADR-017-raga-reference-entity-identity-resolution.md

INSERT INTO raga_aliases (raga_id, alias, alias_type, tradition, source, confidence)
SELECT id, 'Bhairava', 'transliteration', NULL,
       'TRACK-136 §1.7(a): TRACK-132 merged spelling of Bhairavam', 'high'
  FROM ragas WHERE name = 'Bhairavam'
ON CONFLICT (raga_id, alias) DO NOTHING;

INSERT INTO raga_aliases (raga_id, alias, alias_type, tradition, source, confidence)
SELECT id, 'Bauli', 'transliteration', NULL,
       'TRACK-136 §1.7(a): TRACK-132 merged spelling of Bowli', 'high'
  FROM ragas WHERE name = 'Bowli'
ON CONFLICT (raga_id, alias) DO NOTHING;

INSERT INTO raga_aliases (raga_id, alias, alias_type, tradition, source, confidence)
SELECT id, 'dhAmavati', 'transliteration', NULL,
       'TRACK-136 §1.7(a): TRACK-132 merged spelling of Dharmavati', 'high'
  FROM ragas WHERE name = 'Dharmavati'
ON CONFLICT (raga_id, alias) DO NOTHING;

INSERT INTO raga_aliases (raga_id, alias, alias_type, tradition, source, confidence)
SELECT id, 'Jujāvanti', 'transliteration', NULL,
       'TRACK-136 §1.7(a): TRACK-132 merged spelling of Dwijavanthi', 'high'
  FROM ragas WHERE name = 'Dwijavanthi'
ON CONFLICT (raga_id, alias) DO NOTHING;

INSERT INTO raga_aliases (raga_id, alias, alias_type, tradition, source, confidence)
SELECT id, 'Gamakapriyā', 'transliteration', NULL,
       'TRACK-136 §1.7(a): TRACK-132 merged spelling of Gamakakriyā', 'high'
  FROM ragas WHERE name = 'Gamakakriyā'
ON CONFLICT (raga_id, alias) DO NOTHING;

INSERT INTO raga_aliases (raga_id, alias, alias_type, tradition, source, confidence)
SELECT id, 'Gamanapriyā', 'transliteration', NULL,
       'TRACK-136 §1.7(a): TRACK-132 merged spelling of Gamakakriyā', 'high'
  FROM ragas WHERE name = 'Gamakakriyā'
ON CONFLICT (raga_id, alias) DO NOTHING;

INSERT INTO raga_aliases (raga_id, alias, alias_type, tradition, source, confidence)
SELECT id, 'Gauri Manohari', 'transliteration', NULL,
       'TRACK-136 §1.7(a): TRACK-132 merged spelling of Gourimanohari', 'high'
  FROM ragas WHERE name = 'Gourimanohari'
ON CONFLICT (raga_id, alias) DO NOTHING;

INSERT INTO raga_aliases (raga_id, alias, alias_type, tradition, source, confidence)
SELECT id, 'Gaula', 'transliteration', NULL,
       'TRACK-136 §1.7(a): TRACK-132 merged spelling of Gowla', 'high'
  FROM ragas WHERE name = 'Gowla'
ON CONFLICT (raga_id, alias) DO NOTHING;

INSERT INTO raga_aliases (raga_id, alias, alias_type, tradition, source, confidence)
SELECT id, 'Gauri', 'transliteration', NULL,
       'TRACK-136 §1.7(a): TRACK-132 merged spelling of Gowri', 'high'
  FROM ragas WHERE name = 'Gowri'
ON CONFLICT (raga_id, alias) DO NOTHING;

INSERT INTO raga_aliases (raga_id, alias, alias_type, tradition, source, confidence)
SELECT id, 'Todi', 'transliteration', NULL,
       'TRACK-136 §1.7(a): TRACK-132 merged spelling of Hanumatodi', 'high'
  FROM ragas WHERE name = 'Hanumatodi'
ON CONFLICT (raga_id, alias) DO NOTHING;

INSERT INTO raga_aliases (raga_id, alias, alias_type, tradition, source, confidence)
SELECT id, 'hindOla vasantaM', 'transliteration', NULL,
       'TRACK-136 §1.7(a): TRACK-132 merged spelling of Hindolavasanta', 'high'
  FROM ragas WHERE name = 'Hindolavasanta'
ON CONFLICT (raga_id, alias) DO NOTHING;

INSERT INTO raga_aliases (raga_id, alias, alias_type, tradition, source, confidence)
SELECT id, 'jaganmOhanaM', 'transliteration', NULL,
       'TRACK-136 §1.7(a): TRACK-132 merged spelling of Jaganmohana', 'high'
  FROM ragas WHERE name = 'Jaganmohana'
ON CONFLICT (raga_id, alias) DO NOTHING;

INSERT INTO raga_aliases (raga_id, alias, alias_type, tradition, source, confidence)
SELECT id, 'Kalyana Vasanta', 'transliteration', NULL,
       'TRACK-136 §1.7(a): TRACK-132 merged spelling of Kalyāna Vasantam', 'high'
  FROM ragas WHERE name = 'Kalyāna Vasantam'
ON CONFLICT (raga_id, alias) DO NOTHING;

INSERT INTO raga_aliases (raga_id, alias, alias_type, tradition, source, confidence)
SELECT id, 'Mohana', 'transliteration', NULL,
       'TRACK-136 §1.7(a): TRACK-132 merged spelling of Mohanam', 'high'
  FROM ragas WHERE name = 'Mohanam'
ON CONFLICT (raga_id, alias) DO NOTHING;

INSERT INTO raga_aliases (raga_id, alias, alias_type, tradition, source, confidence)
SELECT id, 'nArI rItigauLa', 'transliteration', NULL,
       'TRACK-136 §1.7(a): TRACK-132 merged spelling of Nārīrītigowla', 'high'
  FROM ragas WHERE name = 'Nārīrītigowla'
ON CONFLICT (raga_id, alias) DO NOTHING;

INSERT INTO raga_aliases (raga_id, alias, alias_type, tradition, source, confidence)
SELECT id, 'Nāta', 'transliteration', NULL,
       'TRACK-136 §1.7(a): TRACK-132 merged spelling of Nāṭṭai', 'high'
  FROM ragas WHERE name = 'Nāṭṭai'
ON CONFLICT (raga_id, alias) DO NOTHING;

INSERT INTO raga_aliases (raga_id, alias, alias_type, tradition, source, confidence)
SELECT id, 'Riti Gaula', 'transliteration', NULL,
       'TRACK-136 §1.7(a): TRACK-132 merged spelling of Reethigowla', 'high'
  FROM ragas WHERE name = 'Reethigowla'
ON CONFLICT (raga_id, alias) DO NOTHING;

INSERT INTO raga_aliases (raga_id, alias, alias_type, tradition, source, confidence)
SELECT id, 'rItigauLa - Abheri', 'transliteration', NULL,
       'TRACK-136 §1.7(a): TRACK-132 merged spelling of Reethigowla', 'high'
  FROM ragas WHERE name = 'Reethigowla'
ON CONFLICT (raga_id, alias) DO NOTHING;

INSERT INTO raga_aliases (raga_id, alias, alias_type, tradition, source, confidence)
SELECT id, 'vIra vasanta', 'transliteration', NULL,
       'TRACK-136 §1.7(a): TRACK-132 merged spelling of Veeravasantham', 'high'
  FROM ragas WHERE name = 'Veeravasantham'
ON CONFLICT (raga_id, alias) DO NOTHING;

INSERT INTO raga_aliases (raga_id, alias, alias_type, tradition, source, confidence)
SELECT id, 'vijaya vasanta', 'transliteration', NULL,
       'TRACK-136 §1.7(a): TRACK-132 merged spelling of Vijayavasantham', 'high'
  FROM ragas WHERE name = 'Vijayavasantham'
ON CONFLICT (raga_id, alias) DO NOTHING;

INSERT INTO raga_aliases (raga_id, alias, alias_type, tradition, source, confidence)
SELECT id, 'Brindāvana Sāranga', 'transliteration', NULL,
       'TRACK-136 §1.7(a): TRACK-132 merged spelling of bRndAvana sAranga', 'high'
  FROM ragas WHERE name = 'bRndAvana sAranga'
ON CONFLICT (raga_id, alias) DO NOTHING;

