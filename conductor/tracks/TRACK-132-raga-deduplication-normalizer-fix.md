| Metadata | Value |
|:---|:---|
| **Status** | Ready — adjudication complete, migration pending |
| **Version** | 1.9.0 |
| **Last Updated** | 2026-07-19 |
| **Author** | Sangeetha Grantha Team |
| **Priority** | P2 — data correctness |
| **Decision** | [ADR-016](../../application_documentation/02-architecture/decisions/ADR-016-raga-naming-authority.md) — raga naming authority (frozen 2026-07-19) |
| **Evidence** | [Raga duplicate candidates](./evidence/TRACK-132-raga-duplicate-candidates.md) (Seshadri, 2026-07-19) |
| **Depends on** | [TRACK-093](./TRACK-093-trinity-krithi-bulk-import.md) (corpus imported), [TRACK-091](./TRACK-091-comprehensive-raga-reference-data.md) (curated raga reference data) |
| **Interacts with** | [TRACK-061](./TRACK-061-transliteration-aware-normalisation.md) (normaliser this track extends) |

# TRACK-132: Raga Deduplication & Import Normaliser Fix

## Goal

Merge the duplicate raga rows the Trinity import created, and close the normalisation gap that
produced them — so the next import matches against curated reference data instead of minting a twin.

Also folds in a small curator-UX fix (§5) that shares no code but is too small to track on its own.

## Source analysis

This track originates from a fuzzy-similarity analysis run by Seshadri before the track existed:
**[evidence/TRACK-132-raga-duplicate-candidates.md](./evidence/TRACK-132-raga-duplicate-candidates.md)**
— 244 raga pairs at ≥85% normalised string similarity. That analysis is what identified the problem
and prompted this track; it is the primary evidence and should be read first.

The SQL below is a narrower, independent confirmation of the same problem, useful because it attaches
krithi-link counts and so tells you what each merge would actually move.

### Two views of the same problem, and why the gap matters

| View | Method | Groups | Raga rows | Krithi links at stake |
|:---|:---|---:|---:|---:|
| Exact normalised collision | diacritic-fold + strip non-alpha | 16 | 32 | 51 |
| Aggressive normalised collision | above + `ch→c`, `sh→s`, `th→t`, `dh→d`, `w→v`, `oo→u`, `ee→i`, de-double, drop trailing `m` | **114** | **232** | **313** |
| Seshadri's fuzzy analysis | ≥85% similarity | 244 pairs | — | — |

**The exact-match view catches only ~14% of the problem.** It sees `dEva manOhari` vs `Deva Manohari`
but misses `Madhyamāvathi [0]` vs `madhyamAvati [22]`, `Atāna [0]` vs `aThANa [22]`,
`Chakravākam [0]` vs `cakravAkaM [2]`, `Hamsadhwani [0]` vs `haMsa dhvani [4]` — because those differ
by *transliteration convention* (aspirates, `ch`/`c`, `sh`/`ś`, terminal `-am`/`-aM`), not just
diacritics. Scope the fix to the aggressive view, not the exact one.

## Background — this is a matching bug, not a data-entry accident

The import created a second raga row whenever the source spelling used **ITRANS/Harvard-Kyoto style
internal capitals**. `normalize_for_matching()` handles diacritics, vowel length, aspirates, spacing
and case *folding*, but the caps convention (`kApi`, `sAvEri`, `kAmbhOji`) evidently still fails to
reach the curated row, so resolution falls through to "create new".

The signature is clear across the 16 exact pairs: **one side holds all the krithi links and the other
holds zero.** In 12 of 16 the import-created ITRANS row captured the krithis and the curated
TRACK-091 row was left orphaned — i.e. the *authoritative* raga is the empty one.

> **This does not generalise to the full 114 groups.** 8 of them have links on *both* sides — a
> genuinely split corpus, where krithis for one raga are divided across two rows. Those are listed in
> §0 and are the ones that most need adjudication. Don't carry the "one side is always empty"
> intuition into the wider set.

```sql
-- the duplicate-pair query (diacritic-folding normalisation)
WITH r AS (
  SELECT id, name,
    regexp_replace(lower(translate(name,
      'āáàâãäīíìîïūúùûüēéèêëōóòôõöṛṝṅñṇṭḍḷḻśṣḥṃḵ',
      'aaaaaaiiiiiuuuuueeeeeooooooRrnnntdllsshmk')),'[^a-z]','','g') AS norm
  FROM ragas)
SELECT norm, COUNT(*), array_agg(name ORDER BY name)
FROM r GROUP BY norm HAVING COUNT(*) > 1;
```

### The 16 exact-collision pairs (krithi-link counts as of 2026-07-19)

These are the unambiguous subset — same name after diacritic folding alone. Treat them as the pilot
batch; the remaining ~98 groups from the aggressive view need the same treatment plus adjudication.

| Normalised | Variant A | Variant B | Links |
|:---|:---|:---|:---|
| devamanohari | Deva Manohari **(0)** | dEva manOhari **(6)** | 6 |
| geyahejjajji | Geya Hejjajji **(0)** | gEya hejjajji **(1)** | 1 |
| janaranjani | Jana Ranjani **(0)** | janaranjani **(4)** | 4 |
| kamalamanohari | Kamala Manohari **(1)** | Kamalā Manohari **(0)** | 1 |
| kapinarayani | Kapi Narayani **(1)** | Kāpi Nārāyani **(0)** | 1 |
| karnatakabehag | Karnataka Behag **(1)** | Karnātaka Behāg **(0)** | 1 |
| karnatakakapi | Karnātaka Kāpi **(0)** | karNATaka kApi **(4)** | 4 |
| malavashree | Mālavashree **(0)** | Mālāvashree **(0)** | 0 |
| navarasakannada | Navarasa kannada **(0)** | navarasa kannaDa **(2)** | 2 |
| ravikriya | Ravi Kriyā **(0)** | ravikriyA **(1)** | 1 |
| sindhuramakriya | Sindhu Rāmakriya **(0)** | sindhu rAmakriyA **(3)** | 3 |
| suddhabangala | Suddha Bangala **(3)** | Suddha Bangāla **(0)** | 3 |
| suddhadhanyasi | Suddha Dhanyāsi **(0)** | Suddha dhanyASi **(3)** | 3 |
| suddhamukhari | Suddha Mukhāri **(0)** | Suddha mukhAri **(1)** | 1 |
| suddhasaveri | Suddha Sāveri **(0)** | Suddha sAvEri **(7)** | 7 |
| yadukulakambhoji | Yadukula Kāmbhoji **(0)** | yadukula kAmbhOji **(14)** | 14 |

**51 krithi-raga links** sit on the duplicate side of these 16 pairs — rising to **313** across the
full 114-group aggressive view.

> **`malavashree` is a different bug.** Both sides have zero links and both predate the import —
> `Mālavashree` vs `Mālāvashree` are a genuine seed-data duplicate from migrations
> `39__seed_malavasri_raga.sql` / `40__seed_missing_trinity_ragas.sql`. Fix it here, but note the
> cause is duplicated seeding, not import matching.

## Naming convention & merge direction — FROZEN (2026-07-19)

> **Decided and frozen in [ADR-016: Raga Naming Authority & Canonical Reference Data](../../application_documentation/02-architecture/decisions/ADR-016-raga-naming-authority.md).**
> That ADR is now the authority for naming, merge direction, adjudication order and the match-key
> design; it also records the long-term intent that Sangeetha Grantha itself becomes an authentic
> source of raga information, with Wikipedia as the bootstrap rather than a permanent dependency.
> The summary below is retained as the working evidence that produced the decision.

### The canonical sources

| Source | Role | Coverage | Convention |
|:---|:---|:---|:---|
| [Wikipedia — Melakarta](https://en.wikipedia.org/wiki/Melakarta) | **Canonical**, melakartas | 72 of 72 | Diacritics; `th`/`sh`/`ch`; Title Case; spaced |
| [Wikipedia — List of Janya Ragas](https://en.wikipedia.org/wiki/List_of_Janya_ragas) | **Canonical**, janyas | Organised under all 72 melakartas | Same convention |
| [Raga Surabhi](https://www.ragasurabhi.com/carnatic-music/ragas.html) | **Cross-check only** | ~170 commonly-performed ragas | *Different* — no diacritics (`Thodi`, `Shankarabaranam`) |

### Why the Wikipedia pair is canonical — this is provenance, not preference

The curated reference data **already is** these two pages. Names pulled verbatim from the janya list
resolve to existing `ragas` rows exactly, including Wikipedia's own casing quirks
(`Navarasa kannada`, lowercase `k`):

| Verbatim from Wikipedia janya list | Exists in DB | Krithi links |
|:---|:---|---:|
| `Yadukula Kāmbhoji` | ✅ | **0** |
| `Deva Manohari` | ✅ | **0** |
| `Ravi Kriyā` | ✅ | **0** |
| `Geya Hejjajji` | ✅ | **0** |
| `Sindhu Rāmakriya` | ✅ | **0** |
| `Karnātaka Kāpi` | ✅ | **0** |
| `Navarasa kannada` | ✅ | **0** |
| `Kumudhakriyā` | ✅ | **0** |
| `Nāgachooḍāmani` | ✅ | **0** |
| `Hamsadhwani` | ✅ | **0** |

**Every one has zero krithis.** The Wikipedia-sourced rows are the orphaned side of the duplicate
pairs — the import minted ITRANS twins beside them and the twins captured the corpus. So the curated
names were never wrong; they were simply bypassed.

### Consequences

1. **Merge direction: keep the Wikipedia-form row, merge the ITRANS row into it.** For the ~12 of 16
   exact pairs (and most of the 114 groups) this moves links onto the currently-empty row and deletes
   the one holding the data — the opposite of a naive "keep the row with data" merge.
2. **Raga Surabhi is not adopted as the naming convention.** It conflicts with the canonical pair on
   the central question (no diacritics vs diacritics), covers only ~170 of 1,145 rows (15%), and
   omits the rare Dikshitar janyas this corpus is full of. Adopting it would mean renaming the very
   rows that are already correct, and would leave ~975 ragas with no authority. Keep it as a
   **cross-check** for commonly-performed ragas — it is useful precisely there, see §0.
3. **`Kanadā` and `Kannada` are confirmed DISTINCT** — both appear as separate entries in the janya
   list. The §0 warning stands: do not merge them.

### Residual per-pair judgement

Adjudication is still needed where the DB has drifted from the source (`Suddha Sāveri` in DB vs
`Shuddha Saveri` in the janya list), where a group has three rows
(`vIra vasanta` / `vIra vasantaM` / `Veeravasantham`), and where vowel length is a lakshana question
rather than a formatting one (`Kamala Manohari` vs `Kamalā Manohari`). Handle these in §0; the rule
above settles the direction, not every spelling.

## Scope

### 0. Triage the candidate list — do this first, it gates everything else

The 244-pair candidate list **cannot be applied mechanically**, and this is the single biggest risk
in the track. A false merge is far worse than a missed one: a missed duplicate leaves two rows that
can be merged later, while a false merge destroys the distinction between two real ragas and silently
mis-attributes krithis.

**Proof the risk is live, not theoretical:** of the 114 aggressive-collision groups, only **8** have
krithi links on *both* sides. That subset is simultaneously the highest-value (a genuinely split
corpus) and the highest-risk (most likely to be distinct ragas):

| Group | Rows (with krithi counts) | Assessment |
|:---|:---|:---|
| kanada | `Kannada [7]` / `Kanadā [4]` | ⚠️ **Distinct ragas — do not merge.** Would corrupt 11 links |
| mohana | `Mohanam [16]` / `Mohana [3]` | Likely same (terminal `-m`) — confirm |
| kamas | `khamAs [5]` / `Kamās [2]` | Likely same |
| bhairava | `Bhairavam [2]` / `Bhairava [2]` | Likely same; **note `Bhairavi` is distinct** |
| viravasanta | `vIra vasanta [2]` / `vIra vasantaM [1]` / `Veeravasantham [0]` | Likely one raga, three spellings |
| andali | `Andhali [1]` / `Andali [1]` | Likely same |
| gurjari | `ghurjari [1]` / `Gurjari [1]` | Likely same |
| hindolavasanta | `Hindolavasanta [1]` / `hindOla vasantaM [1]` | Likely same |

Further false positives visible in the candidate list: `Bhogi`/`Abhogi`, `Bālahamsa`/`Kalāhamsa`,
`Asāveri`/`Sāveri`, `Rāmapriyā`/`Sāmapriya`, `Gāyakapriyā`/`Gamakapriyā`/`Gamanapriyā`,
`Nāgachooḍāmani`/`Rāgachoodāmani`, `Shankara`/`Shankari`, `Niranjani`/`Ranjani`.

**Deliverable:** an adjudicated merge list — every candidate marked MERGE (with keeper) or DISTINCT
(with reason) — produced by routing the full list through the `carnatic-musicologist` subagent, and
reviewed for the 8 split-corpus groups above by hand. The migration in §2 is written from *that*
list, never from the raw candidates.

> ## ✅ ADJUDICATION COMPLETE (2026-07-19) — see [§0c](#0c-adjudicated-merge-list-final) for the final
> MERGE / DISTINCT / DEFER list. The `carnatic-musicologist` review is done; what follows in this
> section is the method that produced it, retained for the next batch.

**Adjudication order — check the sources before asking the musicologist.** Most groups resolve
mechanically:

1. Is the name in [Wikipedia's janya list](https://en.wikipedia.org/wiki/List_of_Janya_ragas) or
   [Melakarta](https://en.wikipedia.org/wiki/Melakarta)? → that spelling is the keeper.
2. Do *both* sides appear there as separate entries? → **DISTINCT** (this is what settles
   `Kanadā`/`Kannada`).
3. Still ambiguous, and the raga is commonly performed? → cross-check
   [Raga Surabhi](https://www.ragasurabhi.com/carnatic-music/ragas.html), which independently lists
   `Kanada` and `Kannada` separately, and confirms keepers for `Mohanam`, `Khamas`, `Atana`,
   `Hamsadwani`.
4. Only what survives 1–3 needs musicological judgement. Expect this to be a small tail — mostly rare
   janyas absent from both sources.

### 0d. Coverage — what the adjudication does and does not cover

The adjudication resolved the **highest-risk** subset, not all 114 collision groups:

| | Groups | Status |
|:---|---:|:---|
| Split-corpus (links on **both** sides) | **8** | ✅ All adjudicated — these were the dangerous ones |
| One-sided (curated orphan + import twin) | 106 | ~27 adjudicated; **~79 remain** |
| **Total** | **114** | |

The ~79 remaining groups are all **one-sided** — one row holds the corpus, its twin holds zero. That
makes them low-risk and mechanically resolvable by step 1 of the adjudication order (name appears in
the Wikipedia pair → that spelling is the keeper). They carry roughly **150 krithi links** between
them.

Plan accordingly: **Batch A** = the 17 adjudicated merges below (high value, High confidence,
human-reviewed). **Batch B** = the ~79 one-sided remainder, resolved by script against the Wikipedia
pair with a curator spot-check, *after* Batch A has proven the migration mechanics.

### 0c. Adjudicated merge list (FINAL)

Produced by the `carnatic-musicologist` review, 2026-07-19, under the ADR-016 convention. Every
verdict below is grounded in melakarta parentage and scale, not name similarity. **This table is the
input to the §2 migration; do not write it from the raw candidate list.**

#### MERGE — repoint by `order_index`, then delete the losing row

| # | Losing row | → Keeper | Links | Confidence |
|:--|:---|:---|---:|:---|
| 1 | `Todi [40]` | `Hanumatodi` | 40 | High |
| 2 | `Kalyāni [40]` | `Mechakalyāni` | 40 | High |
| 3 | `Riti Gaula [9]` | `Reethigowla` | 9 | **High** (see §0e) |
| 3b | `rItigauLa - Abheri [1]` | `Reethigowla` | 1 | **High** (see §0e) |
| 4 | `Gaula [8]` | `Gowla` | 8 | High |
| 5 | `Nāta [7]` | `Nāṭṭai` | 7 | **High — name+scale verified §0f** |
| 6 | `Gauri [5]` | `Gowri` | 5 | High |
| 7 | `khamAs [5]` | `Kamās` | 5 | High |
| 8 | `vIra vasanta [2]` + `vIra vasantaM [1]` | `Veeravasantham` | 3 | Med-High |
| 9 | `Mohana [3]` | `Mohanam` | 3 | High |
| 10 | `Bhairava [2]` | `Bhairavam` | 2 | High |
| 11 | `Gauri Manohari [2]` | `Gourimanohari` | 2 | High |
| 12 | `dhAmavati [2]` | `Dhāmavathi` | 2 | High |
| 13 | `Bauli [1]` | `Bowli` | 1 | High |
| 14 | `hindOla vasantaM [1]` | `Hindolavasanta` | 1 | High |
| 15 | `Andali [1]` | `Andhali` | 1 | High (merge); spelling not source-verified — §0f |
| 16 | `ghurjari [1]` | `Gurjari` | 1 | Med-High |
| 17 | `Brindāvana Sāranga [0]` | `bRndAvana sAranga` | 0 | High |

**#17 carries a scale correction.** The surviving row is bare, and V40's scale is wrong — it has no
nishadam, but Brindavana Saranga's identity *is* its nishada bhedam. Set
`S R2 M1 P N3 S` / `S N2 P M1 R2 S` from a published source, with the anya swara declared in the same
`(ANya SwaRa*: N3)` form `R__seed_04` already uses. Do not carry V40's value forward.

#### DEFER — explicitly excluded from this migration

*Three remaining; `rItigauLa - Abheri` was resolved by curator ruling on 2026-07-19 (§0e).*

> **Out for expert review (2026-07-19).** All three DEFER items, plus the Medium-confidence scale and
> naming questions, are written up for a trained Carnatic vocalist/violinist (A-grade AIR artist) in
> **[raga-clarifications-musician-draft.md](./evidence/raga-clarifications-musician-draft.md)**.
> That document is deliberately free of any project or technical framing — it is a pure musicological
> query sheet. Answers land back here as adjudications.

| Pair | Why |
|:---|:---|
| `Pūrvi [1]` / `Poorvi [0]` | Needs an SSP check on which Purvi Dikshitar intended for `Srī guru guhasya`. He did set some Hindustani-derived ragas faithfully, so this is not a safe assumption |
| `Gamakapriyā` / `Gamanapriyā` | **Byte-identical parent and scale, different names, both orphaned.** Either one raga double-seeded, or two ragas with one scale copied over the other. Unresolvable without a source |
| `Dhāmavathi` vs `Dharmavati` (mela 59) | `Dharmavati` *is* mela 59; `Dhāmavathi` is a janya of 59 with an identical sampurna scale — **the Todi pathology again.** Needs a decision, not a merge |

~~`rItigauLa - Abheri`~~ — **resolved 2026-07-19, see §0e.** Promoted to MERGE #3b.

### 0f. Keeper-spelling verification against the janya list (2026-07-19)

The musicologist flagged four keeper spellings as Medium confidence because it had verified the
Melakarta page verbatim but not the janya list. All four are now checked directly against
[List of Janya ragas](https://en.wikipedia.org/wiki/List_of_Janya_ragas).

| Keeper | Source says | Verdict |
|:---|:---|:---|
| `Nāṭṭai` | **`Nāṭṭai`** — verbatim, under mela 36 | ✅ **Confirmed → High.** DB matches the name *and* both scales exactly |
| `Reethigowla` | (settled by curator ruling §0e) | ✅ Confirmed → High |
| `Jujāvanti` | **`Dwijāvanthi /Jujāvanthi`** | ⚠️ **DB spelling is wrong** — needs `th`, see below |
| `Andhali` | **absent from the list** | ⚠️ **Not source-verifiable** — see below |

#### `Nāṭṭai` — fully confirmed, and it settles the Chalanāṭa question

The janya list gives `Nāṭṭai` with ārohaṇam `S R₃ G₃ M₁ P N₃ Ṡ` / avarohaṇam `Ṡ N₃ P M₁ G₃ R₃ S` —
byte-identical to the DB row. Merge #5 (`Nāta [7]` → `Nāṭṭai`) is now High confidence on the name,
the scale *and* the parent.

> **Retraction.** An earlier version of this track listed "DB `Chalanāṭṭai` vs Wikipedia `Chalanāṭa`"
> as source drift. That was wrong. The **janya list also uses `Chalanāṭṭai`** — it is the *Melakarta*
> page that says `Chalanāṭa`. **The two canonical pages disagree with each other**, and the DB
> follows the janya list. No change needed.
>
> Worth recording as a limit of ADR-016: the "pair" is not internally consistent everywhere. Where
> they conflict, prefer the page that governs the row's own category — the janya list for janyas, the
> Melakarta page for melakartas. `Chalanāṭṭai` here is a *parent reference from a janya*, so the janya
> list wins.

#### `Jujāvanti` → should be `Jujāvanthi`

The source lists it as **`Dwijāvanthi /Jujāvanthi`** — both forms, both with `th`. The DB has
`Jujāvanti`, which matches neither. The seeded **scale is correct** (`S R2 M1 G3 M1 P D2 S` /
`S N2 D2 P M1 G3 M1 R2 G2 R2 S N2 D2 N2 S` matches the source exactly), confirming the musicologist's
"identity sound" verdict — only the romanisation is off.

**Recommended: rename to `Jujāvanthi`** — minimal change from the current value, and equally sourced.
`Dwijāvanthi` is listed first and is an acceptable alternative; either is defensible. Apply in the V40
remediation phase, together with the undeclared `(ANya SwaRa*: G2)`.

#### `Andhali` — absent from both canonical pages

Not in the janya list at all. This is exactly the "small tail" ADR-016 §4 step 4 anticipated: a rare
janya no source covers.

**Keep `Andhali` as the keeper on metadata grounds** — it carries parent Harikāmbhōji (28) and a valid
vakra scale, while `Andali [1]` is a bare name with neither. But record it as **not source-verified**;
the merge is safe (they are one raga), the *spelling* is a house choice rather than a sourced one.

### 0e. `rItigauLa - Abheri` — resolved (Seshadri, 2026-07-19)

**Curator ruling:** `rItigauLa - Abheri` is not a raga. It is an **incorrect entry in the source**;
the correct raga is Reethigowla (`rItigauLa`). The row is to be removed and its krithi repointed.

Source: the kriti is Syama Sastri's **`ninnu vinā mari`**
([syamakrishnavaibhavam.blogspot.com](https://syamakrishnavaibhavam.blogspot.com/2011/06/syama-sastry-kriti-ninnu-vina-mari.html)).

Verified against the DB — the single link on that row is exactly that composition:

| Krithi | Composer | order_index |
|:---|:---|---:|
| `ninnu vinA mari` (`69f8d57f-83c4-4e55-aa3a-a993cc0fae98`) | Syama Sastri | 0 |

**The keeper needs no scale correction.** `Reethigowla [0]` already carries precisely the ārohaṇam and
avarohaṇam the curator supplied, with parent Kharaharapriyā (22):

```
ārohaṇam    S G2 R2 G2 M1 N2 D2 M1 N2 N2 S
avarohaṇam  S N2 D2 M1 G2 M1 P M1 G2 R2 S
```

That is an independent confirmation that `Reethigowla` is the correct Wikipedia-form keeper, and it
raises merge #3 (`Riti Gaula [9]`) from Medium to **High** confidence — the row it merges into is now
curator-verified rather than inferred.

**Do not confuse these with two neighbours that must survive:**

- **`nArI rItigauLa [2]`** — Narīrītigowla, a *different* raga. Not a merge target. (It pairs with the
  corrupted `Nārērētigowla`; see adjacent issues.)
- **`Abheri [2]`** — a real raga, janya of Natabhairavi. The hyphenated row's name is the *only*
  reason `Abheri` appears here; the raga itself keeps its own krithis and is untouched.

#### DISTINCT — must survive as separate rows

`Kanadā` ≠ `Kannada` · `Bhairavi` ≠ `Bhairava`/`Bhairavam` · `Bhogi` ≠ `Abhogi` ·
`Bālahamsa` ≠ `Kalāhamsa` · `Asāveri` ≠ `Sāveri` · `Rāmapriyā` ≠ `Sāmapriya` ·
`Gāyakapriyā` ≠ `Gamakapriyā`/`Gamanapriyā` · `Nāgachooḍāmani` ≠ `Rāgachoodāmani` ·
`Shankara` ≠ `Shankari` · `Niranjani` ≠ `Ranjani` ≠ `Shreeranjani` · `Latāngi` ≠ `Lavangi` ·
`Kadaram` ≠ `Kedaram` · `Kalāvathi` ≠ `Kalāvati` · `Shreemati` ≠ `Srimati`

The `Kanadā`/`Kannada` verdict is independently verifiable from the data and worth stating in full,
since it is the tightest constraint on the normaliser:

| | Parent | Ārohaṇam | Avarohaṇam |
|:--|:--|:--|:--|
| `Kanadā [4]` | Kharaharapriyā (22) | `S R2 G2 M1 D2 N2 S` | `S N2 P M1 G2 M1 R2 S` |
| `Kannada [7]` | Dhīraśankarābharaṇam (29) | `S R2 G3 M1 P M1 D2 N3 S` | `S N3 S D2 P M1 G3 M1 G3 M1 R2 S` |

Different melakarta, different gandharam (G2/G3), different nishadam (N2/N3). **11 krithi links would
be mis-attributed by a false merge here.**

### 0a. Audit of `V40__seed_missing_trinity_ragas.sql` (completed 2026-07-19)

V40 seeded 9 ragas for the Trinity import. Audit result after musicological review:
**4 musicologically wrong, 6 redundant (the raga already existed under a Wikipedia-form name),
and 7 broken parent links. Only 1 of the 9 is clean.**

| # | Seeded as | Verdict |
|:--|:---|:---|
| 1 | Kalyāni (= Mechakalyāni, mela 65) | ⚠️ **Redundant.** `Mechakalyāni` already existed. `ON CONFLICT (name_normalized)` compares `kalyani` vs `mechakalyani`, so no collision fired and a second mela-65 row was created. Now `Kalyāni [40]` / `Mechakalyāni [0]` |
| 2 | Todi (janya of Hanumatodi #8) | ⚠️ **Mis-modelled + redundant.** Carnatic Todi *is* melakarta 8 Hanumatodi, not a janya of it. The seeded scale is byte-identical to Hanumatodi's. Now `Todi [40]` / `Hanumatodi [0]` |
| 3 | Jujāvanti (janya of #28) | ⚠️ Identity sound (Dwijāvanthi, bhashanga with legitimate G2 anya swara) but the **anya swara is undeclared** — other rows annotate this (`Kamās` → `(ANya SwaRa*: N3)`), so a validator will eventually flag it as a false subset violation. Naming needs checking against the janya list. Parent link NULL |
| 4 | Gaula (janya of #15) | ⚠️ **Redundant.** `Gowla [0]` already existed with a **byte-identical scale** and a correct parent. `Gaula [8]` captured the corpus |
| 5 | Nāta (janya of #29) | ❌ **Wrong on all three counts, and redundant.** See below |
| 6 | Bauli (janya of #15) | ⚠️ **Redundant.** `Bowli [0]` already existed with a **byte-identical scale** and a correct parent. `Bauli [1]` captured the corpus |
| 7 | Pūrvi (janya of #51) | ❌ **Wrong raga.** Scale is byte-identical to melakarta 51 (self-parenting, same error as Todi) and is the *Hindustani* Purvi thāṭ (M2). The Carnatic Purvi already exists as `Poorvi [0]` — janya of mela 15 with **M1**. Merge deferred pending a source check |
| 8 | Gauri (= Gourimanohari, mela 23) | ❌ **Wrong — fixed by [V48](../../database/migrations/V48__fix_gauri_melakarta_mistag.sql)** |
| 9 | Brindāvana Sāranga (janya of #22) | ❌ **Scale is wrong, and redundant.** Seeded `S R2 G2 M1 P D2 S` / `S D2 P M1 G2 S` has *no nishadam* — but Brindavana Saranga's whole identity is its nishada bhedam (N3 up, N2 down), with G varjya. Correct: `S R2 M1 P N3 S` / `S N2 P M1 R2 S`. `bRndAvana sAranga [2]` already existed |

#### Nāta (item 5) in detail

V40 seeded it as a janya of mela 29 with `S G3 M1 P N3 S` / `S N3 D2 P M1 G3 R2 S`. Every element is
wrong, and the correct row already existed:

- **Parent:** Nāta/Nattai is a janya of melakarta **36 Chalanāṭa**, not 29. The diagnostic is R3 and
  D3 — swaras that exist in mela 36 and *cannot* exist in mela 29, which carries R2/D2. The seeded
  avarohanam's D2/R2 are Shankarabharanam swaras and are simply not Nattai.
- **Scale:** the seeded arohanam omits R entirely. Correct: `S R3 G3 M1 P N3 S` / `S N3 P M1 G3 R3 S`.
- **Redundant:** `Nāṭṭai [0]` already existed with parent `Chalanāṭṭai` and *exactly* the correct
  scale above — verified.
- Nāta and Nattai are the same raga (Sanskrit `nāṭa` / Tamil `nāṭṭai`), not a distinction.

The 7 links confirm it: `jagadAnanda kAraka` (Tyagaraja's first Pancharatna), `mahā gaṇapatiṃ manasā`,
`ninnē bhajana`, Syama Sastri's `pāhi māṃ` — all canonical **Nattai** pieces. Do not repair `Nāta`'s
scale in place; merge it into `Nāṭṭai` and delete it.

#### Systemic finding 1 — all 7 janya parent links are NULL

V40 resolves parents with `(SELECT id FROM ragas WHERE melakarta_number = N)`. **Flyway runs every
versioned (`V__`) migration before any repeatable (`R__`) migration**, and the melakarta rows come
from `R__seed_04_raga_reference.sql`. So on a fresh `make db-reset` those subselects run against an
empty `ragas` table, return NULL, and all seven janyas are seeded parentless — silently, because
`COALESCE(EXCLUDED.parent_raga_id, ragas.parent_raga_id)` treats NULL as "leave alone".

Confirmed in the data: 890 janyas have a parent (all from `R__seed_04`), 181 do not — V40's seven
among them.

> **Rule this establishes:** a versioned migration must never depend on data seeded by a repeatable.
> Either seed the dependency inline, or move the whole thing into the repeatable. Worth checking
> `V39` and any future `V__` seed for the same pattern.

#### Systemic finding 2 — V40 is itself a major source of the duplicates this track fixes

**Six of nine** seeded ragas already existed under a Wikipedia-form spelling. In every case the
*newly seeded* row captured the corpus and the pre-existing canonical row was left empty:

| Seeded by V40 | Pre-existing canonical | Links on the V40 row |
|:---|:---|---:|
| `Kalyāni` | `Mechakalyāni` | **40** |
| `Todi` | `Hanumatodi` | **40** |
| `Gaula` | `Gowla` | **8** |
| `Nāta` | `Nāṭṭai` | **7** |
| `Gauri` | `Gowri` | **5** |
| `Bauli` | `Bowli` | **1** |
| `Brindāvana Sāranga` | `bRndAvana sAranga` | 0 |
| `Pūrvi` | `Poorvi` | 1 *(deferred — see below)* |

**V40 alone accounts for ~96 of the ~313 krithi links sitting on duplicate rows** — roughly a third
of this track's total merge workload, and it predates the import entirely. The dedup here is
therefore not primarily an import-matching problem; a single seed migration written without checking
existing reference data caused the largest identifiable share of it.

That is the strongest argument for ADR-016 §6: **a seed migration must check existing reference data
before inserting.** V40 asserted nine ragas were "missing"; six of them were already there.

### 0b. Melakarta-level problems found while resolving the convention

Two issues sit in the melakarta set itself (74 rows for 72 melakartas) and should be fixed here:

| Mela | Rows | Issue | Status |
|:---|:---|:---|:---|
| 23 | `Gauri [5]` / `Gourimanohari [0]` | **Not a duplicate — a mis-tag.** Melakarta 23 is Gaurimanohari. `Gauri` is a *janya* of mela 15, wrongly carrying `melakarta_number = 23` while holding 5 krithis. | ✅ **Fixed by V48** |
| 65 | `Kalyāni [40]` / `Mechakalyāni [0]` | Genuine duplicate — `Mechakalyāni` is the formal name, `Kalyāni` the common one holding the corpus. Straight merge. | Open — belongs to the §2 merge |

**V48 (applied 2026-07-19)** cleared `Gauri`'s melakarta number, re-parented it to melakarta 15
(`Māyāmāḻavagouḻai`), and corrected its scale to `S R1 M1 P N3 S` / `S N3 D1 P M1 G3 R1 S`. The scale
is self-verifying — R1 G3 M1 P D1 N3 *is* Māyāmāḻavagouḻai — and matches the Wikipedia-sourced `Gowri`
row already in `R__seed_04`. The 5 krithi links were left untouched: they were always pointing at the
right raga, only its metadata was wrong. Melakarta rows: 74 → 73.

Two consequences for this track's merge list:

- `Gauri [5]` and `Gowri [0]` are now metadata-identical and are a confirmed **MERGE** pair.
- Melakarta 23 is still split across `Gourimanohari [0]` (has the mela number and scale) and
  `Gauri Manohari [2]` (has neither) — a **MERGE** pair that V48 deliberately did not touch, since
  merging is this track's job, not a mis-tag fix's.

### 1. Normaliser fix (Python worker) — the root cause

- Extend `normalize_for_matching()` in `tools/krithi-extract-enrich-worker/src/normalizer.py` to fold
  ITRANS/HK internal capitals so `yadukula kAmbhOji` → `yadukulakambhoji` collides with the curated row.
- Extend it further to the transliteration-convention axes the exact view misses — aspirates
  (`th`/`t`, `dh`/`d`), `ch`/`c`, `sh`/`ś`/`s`, `w`/`v`, `oo`/`ū`, `ee`/`ī`, and terminal `-am`/`-aM`.
  These account for the 16 → 114 jump in collision groups.
- **Gap found in review — the aspirate folding as specified is incomplete.** It covers only the
  dental/retroflex series (`th`, `dh`). The velar and labial aspirates are unhandled, so two
  adjudicated MERGE pairs **would not collide and could silently recur on the next import**:
  `ghurjari`/`Gurjari` needs `gh→g`, and `khamAs`/`Kamās` needs `kh→k`. Add `gh`, `kh`, `bh`, `jh`
  for symmetry.
- Add regression cases from the adjudicated MERGE list; each must resolve to the keeper raga.
- **Mandatory negative cases.** The musicologist ran the proposed folding across all 1,145 rows and
  isolated the groups that collide *while carrying different scales* — i.e. real ragas the folding
  would destroy. This is the complete set; all four must resolve apart:

  | Collision key | Pair | Axis at fault |
  |:---|:---|:---|
  | `kanada` | `Kanadā [4]` / `Kannada [7]` | **de-doubling** (`nn`→`n`) — 11 links at risk |
  | `purvi` | `Poorvi [0]` / `Pūrvi [1]` | `oo`→`u` (disappears if the DEFER on Purvi resolves as a merge) |
  | `kalavati` | `Kalāvathi [0]` / `Kalāvati [3]` | `th`→`t` — mela 31 vs 16 |
  | `srimati` | `Shreemati [0]` / `Srimati [0]` | `sh`→`s` + `ee`→`i` — mela 2 vs 8 |

  Plus three defensive assertions that do not collide today but sit one axis from a merge target:

  - `Bhairavi` (**28 krithis**) must never fold onto `Bhairava`/`Bhairavam` — terminal `-i` must not
    be folded.
  - `Abhogi` must never fold onto `Bhogi` — guards against future initial-vowel stripping.
  - `Ranjani` / `Niranjani` / `Shreeranjani` must yield three distinct keys. This one holds **only
    because `sh`→`s` *maps* rather than deletes**; the reviewer hit exactly that bug while probing.
    If the implementation ever deletes digraphs instead of mapping them, these collapse. Assert it
    explicitly.

### 2. Merge migration (Flyway)

- `VNN__merge_duplicate_ragas.sql` per [ADR-013](../../application_documentation/02-architecture/decisions/ADR-013-db-migration-with-flyway.md).
- **Repoint `krithi_ragas.raga_id` by `order_index`. Do NOT de-duplicate first.**

> ⚠️ **Correction to an earlier version of this track, which said "de-duplicate before repointing".
> That guidance was wrong and would have destroyed data.** The junction's primary key is
> `(krithi_id, raga_id, order_index)` — verified — *not* `(krithi_id, raga_id)`. The same
> `(krithi, raga)` pair legitimately recurs at different positions in a ragamalika.
>
> `viSva nAthaM bhajEhaM` (`70623fa6-f90b-44f6-a61f-9136936e8be2`) is Dikshitar's Chaturdasa
> Ragamalika: **34 rows, `order_index` 1–34, only 14 distinct ragas**, arranged as a palindrome
> (Sri · Ārabhi · Gauri · Nāta · Gaula · Mohana → mirrored → …). `Nāta` alone appears at index 4, 9
> and 31. A `SELECT DISTINCT krithi_id, raga_id` pass would collapse 34 rows to 14 and silently
> destroy the palindrome — a genuine lakshana loss under domain-model §6.2.
>
> Repointing by `order_index` is safe: a PK collision needs the same krithi linked to both the losing
> and keeper raga at the *same* order_index, which cannot occur.

- **Repoint `krithis.primary_raga_id` too — this is not optional, and it fails silently.**

> ⚠️ **Three FKs reference `ragas`, with different delete semantics — verified:**
>
> | FK | On delete | Risk |
> |:---|:---|:---|
> | `krithi_ragas.raga_id` | **RESTRICT** | Safe — errors loudly if you forget to repoint |
> | `krithis.primary_raga_id` | **SET NULL** | ☠️ **Silent data loss** |
> | `ragas.parent_raga_id` | **SET NULL** | ☠️ Silently orphans janyas from the merged parent |
>
> The 17 losing rows carry **116 `krithis.primary_raga_id` references** — `Kalyāni` 40, `Todi` 40,
> `Riti Gaula` 9, `Gaula` 5, `khamAs` 5, `Nāta` 4, and the rest. A migration that repoints only the
> junction and then deletes would **silently NULL the primary raga on 116 krithis** — no error,
> because that FK is SET NULL, not RESTRICT. Only the junction is protected.
>
> For this batch `ragas.parent_raga_id` is safe (all 17 losing rows have **0 child ragas** —
> verified), but it must be re-checked for any later batch, since a merged row that is some janya's
> parent would silently detach it from the melakarta tree.
>
> **Order per pair: repoint `krithi_ragas` (by `order_index`) → repoint `krithis.primary_raga_id` →
> re-parent any child ragas → only then delete.**

- Delete the losing rows only after repointing; write the merges to `AUDIT_LOG` per the repo rule.
- **Do not** write a general "delete ragas with no krithis" cleanup: 816 of 1,145 ragas are legitimately
  orphaned curated reference data. The migration must be driven by the explicit pair list.

### 3. Guardrail — the structural fix

Add a **deterministic diacritic-free match key** on `ragas` with a UNIQUE constraint, derived by the
same normalisation as §1. This is what actually closes the hole: it decouples *display spelling* from
*identity*, so a future import writing `yadukula kAmbhOji` collides with the existing
`Yadukula Kāmbhoji` and is forced to match rather than insert.

```
name      = 'Yadukula Kāmbhoji'   -- Wikipedia convention, display
match_key = 'yadukulakambhoji'    -- UNIQUE, never displayed
```

Two properties worth stating explicitly:

- The constraint must use the *same* normalisation as §1, or the two drift and the guardrail passes
  rows the matcher would have merged. Derive the key in one place (ideally generated//`STORED`) rather
  than computing it independently in SQL and Python.
- It must be lenient enough to catch the 114 groups but strict enough to keep `kanada` ≠ `kannada`.
  That single pair is the tightest constraint on the normalisation and makes a good canary test.

### 4. Verification

- Re-run both collision queries (exact and aggressive) → only adjudicated-DISTINCT groups remain.
- **Total `krithi_ragas` row count unchanged** — up to 313 links move, none lost or duplicated.
- **Ragamalika integrity — the single most sensitive object in this migration.**
  `viSva nAthaM bhajEhaM` (`70623fa6-f90b-44f6-a61f-9136936e8be2`) must still have exactly **34**
  `krithi_ragas` rows with contiguous `order_index` 1–34, and the sequence must remain palindromic.
  It touches five merge pairs at once (Nāta, Gaula, Mohana, Gauri, Bhairava). Assert it before and
  after:

  ```sql
  SELECT COUNT(*), MIN(order_index), MAX(order_index), COUNT(DISTINCT raga_id)
  FROM krithi_ragas WHERE krithi_id = '70623fa6-f90b-44f6-a61f-9136936e8be2';
  -- must be: 34 | 1 | 34 | 14  (13 after the Nāta/Gaula/Mohana/Gauri/Bhairava merges collapse
  --                             onto keepers — recount expected distinct value deliberately)
  ```
- Every adjudicated-DISTINCT pair still exists as two rows with their original link counts
  (specifically: `Kannada` still has 7 and `Kanadā` still has 4).
- **`krithis` with a NULL `primary_raga_id` — count must be unchanged.** This is the assertion that
  catches the SET-NULL trap in §2. Capture the baseline *before* the migration runs.
- **No raga left with a NULL `parent_raga_id` that had one before** — catches the same trap on the
  janya tree.
- `krithis_without_raga` stays 0.
- Spot-check the raga tree-grid UI for the merged ragas.

### 5. Folded-in curator UX fix — section issues deep-link into the Lyrics tab ✅ (done 2026-07-19)

`SectionIssuesTab` rendered the krithi title as inert text, so a curator seeing a mismatch had no way
to reach the krithi. `SectionIssue.krithiId` was already in the DTO and already reaching the client,
so this was frontend-only:

- **`SectionIssuesTab.tsx`** — the title is now `<Link to={/krithis/:id?tab=Lyrics}>`. It targets the
  Lyrics tab rather than the editor's default Metadata tab, since a section-count mismatch is a lyrics
  problem; landing on Metadata would cost the curator a click on every row.
- **`KrithiEditor.tsx`** — added a `?tab=` deep link. The effect is gated on `serverKrithi` (whose
  query is itself gated on reference data being ready, which the Lyrics/Tags lazy loads depend on)
  and guarded by a ref so it applies once. It routes through the existing `handleTabChange`, so the
  tab's lazy load fires exactly as it does on click — verified working on both client-side nav and
  cold page load.
- **`krithi-editor.types.ts`** — the tab list existed twice: as a literal array in the editor's JSX
  and as a string union on `KrithiEditorState.activeTab`. Both now derive from one exported
  `EDITOR_TABS` const, with an `isEditorTab` guard validating the query param. Defining it here
  rather than in the page component also keeps React Fast Refresh working (ESLint
  `react-refresh/only-export-components` flags constants exported from component files).

Covered by a test in `SectionIssuesTab.test.tsx`. Suite: 56 passing, `tsc --noEmit` clean, no new
lint warnings.

> The `?tab=` param is now a general capability — any of the eight editor tabs can be deep-linked.
> Worth reusing wherever else the app sends a curator to a krithi for a specific reason.

## Adjacent data-quality issues surfaced by the review

Not part of this track's merge, but they sit in the same rows and will otherwise survive the dedup
silently:

- **`Nārērētigowla`** (mela 20) is a transliteration corruption of **Nārīrītigowla** — a
  domain-model §6.4 terminology violation. It pairs with `nArI rItigauLa [2]`.
- **`rItigauLa - Abheri [1]`** — a raga name field holding two raga names joined by a hyphen. An
  unresolved extraction artifact rather than a raga; needs curator triage.
- ~~**DB `Chalanāṭṭai` vs Wikipedia `Chalanāṭa`**~~ — **retracted, not drift.** The janya list also
  says `Chalanāṭṭai`; the two canonical pages disagree with each other. See §0f.
- ~~**`Abheri`'s ārohaṇam looks corrupted**~~ — ✅ **fixed by
  [V49](../../database/migrations/V49__fix_abheri_arohanam_and_parent.sql)**, and it was worse than
  first flagged. See §0g.

### 0g. `Abheri` — ārohaṇam and parent corrected (V49, applied 2026-07-19)

Two defects, the second provable from the stored data alone:

1. **Corrupted ārohaṇam** — `S M1 G2 M1 P P S` had a doubled P and no nishadam.
2. **Wrong parent** — recorded as a janya of Natabhairavi (melakarta 20), which is impossible:

   ```
   Abheri avarohanam   : S N2 D2 P M1 G2 R2 S      <- D2
   Natabhairavi   (20) : S R2 G2 M1 P D1 N2 S      <- D1   ✗ cannot contain D2
   Kharaharapriyā (22) : S R2 G2 M1 P D2 N2 S      <- D2   ✓
   ```

   A janya's swaras must be a subset of its parent's (domain-model §6.4). Abheri carries D2, which
   melakarta 20 does not have. **The row contradicted itself** — no external source needed to detect
   it, though the janya list independently confirms Abheri under melakarta 22.

**Applied:** ārohaṇam → `S G2 M1 P N2 S`, parent → `Kharaharapriyā` (22). The avarohaṇam already
matched the source exactly and was left untouched, as were the 2 krithi links — they always pointed
at the right raga.

> **This is a detectable class of bug, not a one-off.** "Janya carries a swara its parent lacks" is a
> pure-SQL check over all 890 parented janyas. Worth adding as a data-quality query — it would have
> caught this, and it is the same shape as the Todi/Pūrvi/Dhāmavathi "mela modelled as its own janya"
> pathology. Recommend folding into the §4 verification suite as a standing check.
- **`Jujāvanti`'s undeclared anya swara** (see §0a item 3).

## Out of scope

- The 29 krithis with section-count mismatches → [TRACK-133](./TRACK-133-section-mismatch-remediation.md).
- `CuratorService.getStats()` computes section-issue counts by loading every section row into two
  in-memory maps and diffing in Kotlin ([CuratorService.kt:67-84](../../modules/backend/api/src/main/kotlin/com/sangita/grantha/backend/api/services/CuratorService.kt)).
  It should be a SQL aggregate. Noted here so it is not lost; belongs with TRACK-133, which touches
  the same code.
