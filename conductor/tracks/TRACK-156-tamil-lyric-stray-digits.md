| Metadata | Value |
|:---|:---|
| **Status** | In Progress |
| **Version** | 1.2.0 |
| **Last Updated** | 2026-11-02 |
| **Author** | Sangeetha Grantha Team |

# Track: Stray digits in Tamil-script lyrics

**ID:** TRACK-156
**Status:** In Progress — Intent, Spec and Plan Accepted
**Owner:** Seshadri
**Created:** 2026-11-01
**Updated:** 2026-11-02

## Goal

Make Tamil-script lyrics in the Rasika reader readable, without losing the pronunciation information the digits carry and without altering stored lyric text that is faithful to its source.

## Context

- **Reference:** [Domain Model §6](../../application_documentation/01-requirements/domain-model.md#6-musicological-correctness-rules-lakshana) (lyric content is a correctness contract); [ADR-013](../../application_documentation/02-architecture/decisions/ADR-013-db-migration-with-flyway.md) (Flyway only). Related but separate: TRACK-155 (mobile list UX), TRACK-157 (search follow-ups).
- **Report:** the reader for *hErambAya* (Muthuswami Dikshitar, Atana, Tiruvarur Tyagarajaswami Temple, Shodasa Ganapati; source `http://guru-guha.blogspot.com/2008/11/dikshitar-kriti-herambaaya-raga-athana.html`) shows its Tamil Pallavi as `ஹேரம்பா 3 ய நமஸ்தே ஹரி ப் 3 ரஹ்மேந்த் 3 ராதி 3 -`.
- **Evidence below is read-only** from the dev database (1,226 krithis, restored from the 27 Oct backup), queried with `docker exec -i sangeetha-grantha-db-1 psql -U postgres -d sangita_grantha`. Nothing was written.

### Finding 1 — the digits are in the stored data and are deliberate, not a rendering or extraction bug

Krithi `32444251-c229-40b2-ae79-7d6dfe1fa731`, five lyric variants, all from the guru-guha post. The Latin, Telugu, Kannada and Malayalam variants of the same Pallavi carry **no** digits (`hErambAya namastE hari brahmEndrAdi -`). Only the Tamil variant does, in all three sections (Pallavi, Anupallavi, Charanam).

The source page confirms why: its Tamil text uses plain numerals directly after certain consonants, and links to a "Tamil Notation Scheme". Tamil script has one letter for each of ka/kha/ga/gha, so the Tamil edition of these blogs marks the Sanskrit sound with a digit after the base letter. Reading the stored text against the Latin variant fixes the convention:

| Written | Latin | Rule |
|:---|:---|:---|
| `ப் 3 ரஹ்மேந்த் 3 ராதி 3` | brahmEndrAdi | 3 = third of the varga (voiced: pa→ba, ta→da, ka→ga) |
| `நாதா 2 ய` | nAthAya | 2 = second (aspirate: ta→tha) |
| `பாஸா 1 ங்குஸ 1 த 4 ராய` | pASAnkuSa dharAya | 4 = fourth (voiced aspirate); `ஸ 1` = ś |
| `வீர க 3 ணாதி 3` | vIra gaNAdi | |

The source's own legend leaked into 72 Tamil sections (one per krithi) and states the scheme: `க,ச,ட,த,ப - 2-ख छ ठ थ फ; 3-ग ड द ब; 4-घ झ ढ ध भ`, `ஸ1 श`.

So the stored Tamil text is a faithful copy of a source that already uses this notation. The problem is presentation: the notation is written as space-separated ASCII digits, which a reader who does not know the scheme sees as stray "3"s.

### Finding 2 — where the digits do not enter

- **Extraction worker:** `tools/krithi-extract-enrich-worker/src` does not generate or strip these digits. `structure_parser.py` already knows about them (comment at line 328: "superscript number spaced out (as emitted by the guruguha Tamil transliteration)"), and `tests/test_indic_charanam_markers.py::test_real_tamil_charanam_marker_with_ordinal_and_superscript_body` pins that they must not break charanam-marker matching.
- **Transliteration step:** `transliterator.py` (indic-transliteration wrapper) is not what produced the Tamil variant. The Tamil text is the blog's own, stored with `transliteration_scheme` empty and `source_reference` set on all 1,125 Tamil variants.
- **Rendering:** `KrithiReaderScreen.kt` (`LyricSection`) draws `section.text` as received. No transform exists anywhere between the database and the screen, so the gap is the missing display step.

### Finding 3 — scope (all counts from the dev database)

Every Tamil variant is affected; this is the corpus's normal Tamil format, not a few bad rows.

```sql
-- Q1: digit-bearing variants and sections by script
SELECT v.script, count(*) sections,
       count(*) FILTER (WHERE s.text ~ '[0-9]') with_digit,
       count(DISTINCT v.krithi_id) FILTER (WHERE s.text ~ '[0-9]') krithis
FROM krithi_lyric_sections s JOIN krithi_lyric_variants v ON v.id = s.lyric_variant_id
GROUP BY 1 ORDER BY 1;
```

| script | sections | with digit | krithis |
|:---|---:|---:|---:|
| tamil | 4,318 | 4,213 | 1,125 (of 1,125) |
| latin | 4,760 | 1,959 | 732 |
| telugu, kannada | 4,318 each | 12 | 9 |
| devanagari | 4,219 | 12 | 9 |
| malayalam | 4,318 | 14 | 11 |

Latin digits are not this defect: they are charanam ordinals and "Charanam 1" style headings in a different slot. The 9 to 11 krithis in the other Indic scripts are ordinals in a ragamalika (for example `1. श्री रागं`), and the Tamil sections of those krithis show the same ordinals.

```sql
-- Q2: Tamil sections by composer
SELECT c.name, count(DISTINCT v.krithi_id) krithis, count(*) sections,
       count(*) FILTER (WHERE s.text ~ '[0-9]') with_digit
FROM krithi_lyric_sections s JOIN krithi_lyric_variants v ON v.id = s.lyric_variant_id
JOIN krithis k ON k.id = v.krithi_id JOIN composers c ON c.id = k.composer_id
WHERE v.script = 'tamil' GROUP BY 1 ORDER BY 2 DESC;
```

| composer | krithis | sections | with digit |
|:---|---:|---:|---:|
| Tyagaraja | 574 | 2,786 | 2,692 |
| Muthuswami Dikshitar | 481 | 1,202 | 1,198 |
| Syama Sastri | 70 | 330 | 323 |

The 105 Tamil sections without a digit are legitimately free of them (no aspirated or voiced Sanskrit consonant; for example `வீர ஹனுமதே நமோ நமோ`). Per source site: all three composers come from the guru-guha, thyagaraja-vaibhavam and syamakrishnavaibhavam family of blogs, which share the notation.

Digit frequency in Tamil text (`regexp_matches(lyrics,'([1-4])','g')`): 1 = 4,715, 2 = 4,058, **3 = 20,911**, 4 = 6,217. Digit 3 dominates because voiced stops (ga, da, ba, ja) are the commonest. This is why the report showed "3".

### How to tell a marker digit from any other digit

A **notation marker** is a digit 1 to 4, after a Tamil letter or virama, optionally separated by one space, and not followed by another digit:

```sql
-- Q3: Tamil sections containing marker digits (3,361 have one followed by space/newline/end;
--     4,213 contain any 1–4), and sections with any other digit after removing markers
WITH t AS (
  SELECT s.id, v.krithi_id,
         regexp_replace(s.text, '[஀-௿] ?[1-4](?![0-9])', '', 'g') rest
  FROM krithi_lyric_sections s JOIN krithi_lyric_variants v ON v.id = s.lyric_variant_id
  WHERE v.script = 'tamil')
SELECT count(*) FILTER (WHERE rest ~ '[0-9]') sections, count(DISTINCT krithi_id) FILTER (WHERE rest ~ '[0-9]') krithis FROM t;
```

Result: **81 sections in 78 krithis** keep a digit after markers are removed. These are the other legitimate digits and must never be touched: ragamalika ordinals at line start (`1 . கா 3 யே ...`, `1. ஸ்ரீ ராக 3 ம`), charanam ordinals (`சரனம் 1`), and numerals inside the leaked legend. The rule therefore needs the Tamil letter on its left and a 1 to 4 digit, and must skip a digit at line start followed by a period.

### Adjacent defects seen on the way (not fixed by this track)

- 72 Tamil sections (72 krithis) hold the leaked notation legend (`2-ख छ ठ थ फ ...`) instead of lyrics. Query: `WHERE v.script='tamil' AND s.text LIKE '%2-ख छ%'`.
- 145 Kannada sections in 145 krithis end with the stray heading `Kannada- Word Division` (hErambAya's Charanam shows it). Query: `WHERE v.script='kannada' AND s.text ~* 'word division'`. TRACK-102 handled the English and Devanagari forms; the Kannada form was not matched.
- The Latin Charanam of hErambAya ends with `kshEtra - tiruvArUr`, a metadata line stored as lyric text.

Each is a lyric-correctness defect of its own; they are proposed for a follow-up track rather than widened into this one.

### Musicologist review of the marker sample (2026-11-02)

The `carnatic-musicologist` agent checked 125 sections (62 krithis; Tyagaraja, Dikshitar, Syama Sastri; including hErambAya and the 14-raga viSva nAthaM bhajEhaM) against their Latin text. **Verdict: mapping confirmed, no contradiction**, display form acceptable. It found these refinements, all adopted in the implementation (Plan divergence recorded here):

1. The digit can follow a vowel sign or virama (`ஆதி 3`, `கௌ 3`, `ரு 2`, `ப் 3`), about 400 of 914 occurrences in the sample. The rule therefore validates the **(base letter, digit) pair**: `க ச ட த ப` with 2–4, `ஸ` with 1, `ரு` with 2, and (found in the full corpus by us) `ஜ` with 2 (jha). Any other pair, such as `கீதம் 1` (ம), is left alone.
2. The leaked four-line notation legend is not lyric text and must not be formatted; the formatter skips it (stripping it from data is the follow-up track).
3. Plain `ஸ` can still be ś (`ஸ்ரீ`): a source convention, no effect on the rule.

Corpus check of the final rule over all 4,318 stored Tamil sections: 31,656 marker pairs found, 0 invalid pairs. Remaining ASCII digits 1–4 outside the legend are only line-start ordinals, Geetham numbers (`கீதம் 1` to `4`) and one bracketed form `(கீ)3` that the notation does not define (left as stored).

**Spacing finding (changes R1).** A single space after a digit is sometimes mid-word (`தே 3 வ`, dEva) and sometimes a word boundary (`நிதே 4 ஜய`, nidhE jaya); stored text cannot tell which. So the formatter always removes the space **before** the digit and removes the space **after** it only when the base ends in a virama (which must continue into the next consonant). hErambAya's Pallava is shown as `ஹேரம்பா³ ய நமஸ்தே ஹரி ப்³ரஹ்மேந்த்³ராதி³ -`; the `பா³ ய` gap remains. Closing that gap needs word boundaries restored in the data (re-extraction or Latin-aligned repair) and is left to the follow-up track.

## Intent

**Status:** Accepted
**Accepted by:** Seshadri (chat instruction)
**Accepted at:** 2026-11-02

### Problem

In the Rasika reader, Tamil lyrics are full of isolated digits between and inside words ("ஹேரம்பா 3 ய"). A rasika sees noise. The digits are not noise: they are the source edition's way to write ga/dha/ba and similar sounds that Tamil script has no letter for, and they are present on every Tamil variant in the catalogue.

### Proposed outcome

1. The reader shows the Tamil notation in a form that reads as part of the word (the digit attached to its letter and set as a superscript, `க³`), so it looks like the convention Tamil readers of Sanskrit texts know.
2. No stored lyric is lost or silently rewritten: the source's information stays recoverable.
3. Ordinals, charanam numbers and anything that is not a notation marker display exactly as today.
4. A curator and the search index still see the text they see today, so search (TRACK-157) is unaffected.

### Affected users and systems

Rasika readers (iOS and Android) of the Tamil script; the V2 public lyric payload; the admin Curator UI display of Tamil text (decision below); no change to Latin, Devanagari, Telugu, Kannada or Malayalam.

### Constraints

- Lyric text is a correctness contract (Domain Model §6): never delete a digit that carries pronunciation, never touch ordinals.
- Flyway only (ADR-013): any data change is a new versioned migration or a reviewed repeatable seed, never a committed `V__` edit, never an ad-hoc SQL executor.
- Every mutation writes `AUDIT_LOG`.
- The normalisation must be reversible and testable on the real corpus.

### Open questions

1. **Layer.** Display-only transform (stored text untouched), or one-time repair of stored text to Unicode superscripts? **Decided 2026-11-02:** display-only; stored text untouched, no data repair.
2. **Which form for readers who do not know the scheme?** Superscript digits as the sources print them, or a plain-Tamil reading (for example `க³` shown as the Grantha letter `க்³`/`ग` equivalent)? Recommended: superscript digits now; a legend or Grantha rendering is a later product decision.
3. **Should the Curator UI apply the same display step?** Curators editing the Tamil text need the raw form. Recommended: raw in the editor, formatted in read views only.
4. **Leaked legend, "Word Division" heading and `kshEtra` line:** separate follow-up track, yes or no?

## Spec

**Status:** Accepted
**Accepted by:** Seshadri (chat instruction)
**Accepted at:** 2026-11-02

### Requirements

**R1.** A pure function `formatTamilNotation(text)` converts a marker (Tamil letter or virama, optional single space, digit 1 to 4, not followed by a digit) to the Tamil letter followed by the Unicode superscript digit (`¹ ² ³ ⁴`), with the space removed. `ஹேரம்பா 3 ய` becomes `ஹேரம்பா³ய`; `ப் 3 ரஹ்மேந்த் 3 ராதி 3 -` becomes `ப்³ரஹ்மேந்த்³ராதி³ -`.

**R2.** The function leaves unchanged: a digit at line start followed by a period or space-period (`1 .`, `1.`), a digit after a non-Tamil character, a number of two or more digits, and the charanam marker form `சரனம் 1`.

**R3.** The reader applies R1 to Tamil-script variants only, and only when it draws text. The stored text, API payload and search text are unchanged.

**R4.** The transform is idempotent and lossless: the inverse maps superscripts back to the original digits, and a corpus-wide test shows `inverse(format(text))` equals `text` after whitespace normalisation of the removed space.

**R5.** Font fallback: the chosen superscript glyphs must render in the iOS and Android system Tamil fonts. If a platform lacks the glyph, the fallback is the digit in a smaller, raised span, not a missing-glyph box.

### Design

The variant script is already known to the reader, so the transform lives in `modules/shared/presentation` (or `modules/shared/domain` if the admin web wants the same rule through a mirrored TypeScript implementation), applied in `LyricSection` in `KrithiReaderScreen.kt` before `Text(section.text, ...)`. The same regex is pinned by tests in Kotlin and, if the admin read view adopts it, in TypeScript, both fed from one shared fixture of corpus examples. No database, migration or worker change.

### Flagged concerns

- **Lakshana:** none of the transform touches section types, ordinals or ragamalika raga lines (R2).
- **Data-change alternative rejected for now:** rewriting 4,213 stored sections would touch audited lyric content in bulk, and the source's plain-digit form is also what the extraction worker's marker patterns and the corpus regression fixtures expect.
- **Search:** Tamil search (if any) matches on stored text; the display transform must not leak into `normalized_text`.

### Open questions carried forward

Questions 1 to 4 from Intent, with the recommendations stated there.

## Plan

**Status:** Accepted
**Accepted by:** Seshadri (chat instruction)
**Accepted at:** 2026-11-02

### Files that change

- `modules/shared/presentation/src/commonMain/kotlin/com/sangita/grantha/shared/presentation/reader/TamilNotation.kt` (new): `formatTamilNotation` and its inverse.
- `modules/shared/presentation/src/commonMain/kotlin/com/sangita/grantha/shared/presentation/reader/KrithiReaderScreen.kt`: call it in `LyricSection` for Tamil variants; the reader must pass the variant script down if `CatalogueLyricSectionDto` does not carry it.
- `modules/shared/presentation/src/commonTest/kotlin/.../TamilNotationTest.kt` (new), plus a fixture `tamil-notation-corpus.txt` of real sections taken from the dev database.
- `application_documentation/01-requirements/domain-model.md` §6: a short "Tamil notation digits" rule (what a marker is, what it is not). `conductor/tracks.md` row.
- No migration, no seed, no worker change.

### Order of work

1. **Corpus sample for review (no data change).** Extract about 60 Tamil sections (spread across the three composers; include hErambAya, a ragamalika, a Syama Sastri krithi) with their Latin counterparts, and ask the `carnatic-musicologist` agent to confirm each marker reading (`3` = ga/da/ba/ja, `2` = aspirate, `4` = voiced aspirate, `ஸ 1` = ś) and flag any section where the digit reading is ambiguous. Block step 2 on its answer.
2. Write the failing tests first: R1 and R2 table cases (including the exact hErambAya Pallavi), idempotence, and a corpus test that runs over the fixture and asserts no digit outside the R2 exceptions survives and the inverse round-trips.
3. Implement `TamilNotation.kt`, then wire it into `LyricSection`.
4. Verify glyphs on the iOS simulator and an Android build with the hErambAya reader (R5); choose the fallback if needed.
5. Docs: Domain Model §6 note; mark this track Complete.

If the Spec's recommendation is overturned and Seshadri prefers repairing stored text, the data route is a new repeatable seed (marker-checked, per TRACK-139) generated from a reviewed JSON of before and after text for the 4,213 sections, applied with system-attributed `AUDIT_LOG` rows, plus a versioned migration only if a schema change is needed. A musicologist review of the full before-and-after sample is then mandatory before the seed is merged. That variant needs its own Plan revision.

### Risks

- A digit that is genuinely a number (a verse or beat count) after a Tamil letter would be superscripted. Measured: after markers are removed, only ordinals and the legend remain (81 sections), none of which match the marker pattern; step 2's corpus test enforces this.
- Superscript glyph coverage differs by font (R5).
- Copy-paste from the reader yields the superscript form, not the digit; accepted.

### Proof

- `make test` and the new `TamilNotationTest` (paste output); `make test-integration` if any backend file changes (none planned).
- Corpus test over the 4,318 Tamil sections on the dev database: zero residual marker digits after formatting, ordinals unchanged (query Q3 above gives the expected 81 untouched sections).
- iOS simulator screenshot of the hErambAya reader before and after, in the Tamil script, plus a ragamalika krithi (ordinals intact).
- `make check-docs` after the Domain Model note.

## Implementation Plan

- [x] Musicologist review of the marker sample (Plan step 1)
- [x] Failing tests, then `TamilNotation.kt` and reader wiring
- [x] Simulator glyph check on iPhone 18 Pro ([screenshot](evidence/TRACK-156-tamil-reader-after.png)); Android not run in this environment
- [x] Domain Model §6.5 note, `make check-docs`
- [ ] Close the track after review and merge

## Progress Log

- **2026-11-01**: Track created. Read-only investigation of the dev database and the worker code established that the digits are the source edition's Tamil notation, present on all 1,125 Tamil variants (4,213 of 4,318 sections), and that no stage of the pipeline alters them. Intent, Spec and Plan drafted; awaiting Seshadri's acceptance of each.
- **2026-11-02**: Seshadri accepted Intent, Spec and Plan with the display-only fix. Track In Progress.
- **2026-11-02**: Musicologist review confirmed the mapping; rule refined (pair validation, legend skip, spacing). `TamilNotation.kt`, reader wiring, unit and corpus tests, Domain Model §6.5 done.
- **2026-11-02**: iPhone 18 Pro simulator check: superscript glyphs (¹ ² ³ ⁴) render in the Tamil reader with consistent baselines and no fallback boxes. Search on the running backend returned "could not reach the catalogue" (older backend build; not part of this change), so the reader was reached through Collections.
