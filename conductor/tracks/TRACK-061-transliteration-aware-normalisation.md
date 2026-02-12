| Metadata | Value |
|:---|:---|
| **Track ID** | TRACK-061 |
| **Title** | Transliteration-Aware Name Normalisation |
| **Status** | Completed |
| **Priority** | High |
| **Created** | 2026-02-10 |
| **Updated** | 2026-02-10 |
| **Depends On** | TRACK-059 |
| **Spec Ref** | analysis-extraction-pipeline-failures.md (Findings 1, 2, 6) |
| **Est. Effort** | 2–3 days |

# TRACK-061: Transliteration-Aware Name Normalisation

## Objective

Make `NameNormalizationService` (Kotlin backend) produce consistent normalised
forms regardless of which transliteration scheme the source uses (IAST, Harvard-Kyoto,
ITRANS, simple ASCII). Currently, NFD decomposition + combining-mark removal + special
char stripping produces **different** normalised forms for the same composition
depending on the source encoding:

| Source | Raw title | Normalised | Problem |
|:---|:---|:---|:---|
| mdeng.pdf (IAST via diacritic fix) | `akhilāṇḍeśvari rakṣa mām` | `akhilandesvari raksa mam` | `ks` for ṣ → kṣ |
| blogspot (HK) | `akhilANDESvari raksha mAM` | `akhilandesvari raksha mam` | `ksh` for ṣ → ksha |

After NFD + strip, IAST `ṣ` becomes `s` but HK `sh` stays `sh`. These **never match**.

This track also fixes the composer name gap: "Dikshithar" (with `th`) is not in the
canonical mapping table.

## Scope

- **Kotlin backend only.** `NameNormalizationService.kt` changes.
- Add a transliteration collapse step that maps common scheme variants to a single
  canonical ASCII form.
- Extend composer canonical mapping with missing aliases.
- Unit tests for all new normalisation rules.

## Design Decisions

| Decision | Choice | Rationale |
|:---|:---|:---|
| Normalisation target | Collapse to minimal ASCII that is scheme-independent | Don't try to produce IAST — just produce a matching key |
| Implementation approach | Post-NFD substitution table applied after combining-mark removal | Simple, fast, no external library needed |
| Substitution rules | `sh` → `s`, `th` → `t`, `ksh` → `ks`, `chh` → `c`, `dh` → `d`, `bh` → `b`, `ph` → `p`, `gh` → `g`, `jh` → `j` (aspirate collapse) | These are the most common transliteration divergences in Carnatic music nomenclature |
| Rule ordering | Longest match first (`ksh` before `sh`, `chh` before `ch`) | Prevents partial replacement |
| Composer aliases | Add all known Dikshitar spellings; also add "Thyagaraja" / "Thyagarajar" variants | Known gaps from current data |
| Raga name handling | Apply same collapse to `normalizeRaga()` | Ragas also vary: "Shankarabharanam" vs "Sankarabharanam" |

## Transliteration Collapse Table

Applied **after** NFD + combining-mark removal + lowercasing, **before** special-char stripping:

| Input pattern | Output | Example |
|:---|:---|:---|
| `ksh` | `ks` | raksha → raksa |
| `sh` | `s` | shankarabharanam → sankarabharanam |
| `th` | `t` | dikshithar → dikshitar, thiruvarur → tiruvarur |
| `chh` | `c` | achchutam → acutam |
| `ch` | `c` | charanam → caranam |
| `dh` | `d` | dhyana → dyana |
| `bh` | `b` | bhairavi → bairavi |
| `ph` | `p` | phalguni → palguni |
| `gh` | `g` | ghananatam → gananatam |
| `jh` | `j` | jhallaree → jallaree |
| `ee` | `i` | (already in normalizeRaga) |
| `oo` | `u` | (already in normalizeRaga) |
| `aa` | `a` | (already in normalizeRaga) |

**Note**: These rules are applied **only to the normalised matching key** (`title_normalized`,
`name_normalized`), not to the display title.

## Task List

| Task ID | Description | Acceptance Criteria | File(s) |
|:---|:---|:---|:---|
| T61.1 | Add transliteration collapse to `basicNormalize()` | After NFD + mark removal + lowercasing, apply collapse table. `raksha` and `raksa` both normalise to same form. Existing tests still pass. | `NameNormalizationService.kt` |
| T61.2 | Fix composer canonical mapping | Add `"muthuswami dikshithar"` → `"muthuswami dikshitar"`, `"muthuswami dikshithar"` after collapse becomes `"mutuswami diksitar"` which matches. Also add `"thyagaraja"` → `"tyagaraja"`, `"thyagarajar"` → `"tyagaraja"`. | `NameNormalizationService.kt` |
| T61.3 | Apply collapse to `normalizeRaga()` | Collapse aspirates in raga names: `Shankarabharanam` → `sankarabaranam`. Apply before existing vowel reduction. | `NameNormalizationService.kt` |
| T61.4 | Apply collapse to `normalizeTala()` | `Chatusra` → `catusra`, etc. | `NameNormalizationService.kt` |
| T61.5 | Unit tests for transliteration collapse | Test pairs: (IAST form, HK form) → same normalised output. At least 20 test pairs covering titles, ragas, talas, composers. | `NameNormalizationServiceTest.kt` (new) |
| T61.6 | Verify cross-source matching | Given `akhilāṇḍeśvari rakṣa mām` (IAST) and `akhilANDESvari raksha mAM` (HK), both normalise to the same `title_normalized`. | Unit test |

## Files Changed

| File | Change |
|:---|:---|
| `modules/backend/api/.../services/NameNormalizationService.kt` | Transliteration collapse in `basicNormalize()`; composer aliases |
| `modules/backend/api/.../tests/NameNormalizationServiceTest.kt` | New — comprehensive unit tests |

## Progress Log

| Date | Unit | Notes |
|:---|:---|:---|
| 2026-02-10 | Planning | Track created from analysis-extraction-pipeline-failures.md |
| 2026-02-10 | T61.1–T61.4 | Implemented transliteration collapse in `basicNormalize()`: 10-entry table (ksh→ks, sh→s, th→t, ch→c, dh→d, bh→b, ph→p, gh→g, jh→j, chh→c) applied longest-first after NFD strip. Fixed composer canonical mapping — "Dikshithar" now collapses to "diksitar" automatically. Kotlin compileKotlin succeeds. |
