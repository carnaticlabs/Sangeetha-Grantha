| Metadata | Value |
|:---|:---|
| **Status** | Accepted |
| **Version** | 1.0.0 |
| **Last Updated** | 2026-07-19 |
| **Author** | Sangeetha Grantha Team |
| **Deciders** | Sangeetha Grantha Team (Seshadri) |
| **Implemented by** | [TRACK-132](../../../conductor/tracks/TRACK-132-raga-deduplication-normalizer-fix.md) |

# ADR-016: Raga Naming Authority & Canonical Reference Data

## Context

The `ragas` table holds 1,145 rows and had drifted into **two coexisting naming conventions**, which
silently produced duplicate ragas:

| Convention | Example | Origin |
|:---|:---|:---|
| Diacritic | `Yadukula Kāmbhoji` | Curated reference data (`R__seed_04_raga_reference.sql`) |
| ITRANS / Harvard-Kyoto | `yadukula kAmbhOji` | Bulk import from guru-guha.blogspot.com |

Because identity resolution normalises for diacritics but not for the ITRANS capitals convention, the
import could not see the curated row and created a twin beside it. Measured impact: **114 normalised
collision groups across 232 rows, with 313 krithi-raga links sitting on a duplicate.** In most pairs
the *curated* row was left with zero krithis while the import twin captured the corpus.

Three candidate authorities were considered:

| Source | Coverage | Convention |
|:---|:---|:---|
| [Wikipedia — Melakarta](https://en.wikipedia.org/wiki/Melakarta) | 72 of 72 melakartas | Diacritics; `th`/`sh`/`ch`; Title Case; spaced |
| [Wikipedia — List of Janya ragas](https://en.wikipedia.org/wiki/List_of_Janya_ragas) | Janyas organised under all 72 melakartas | Same |
| [Raga Surabhi](https://www.ragasurabhi.com/carnatic-music/ragas.html) | ~170 commonly-performed ragas | **Different** — no diacritics (`Thodi`, `Shankarabaranam`) |

A decisive piece of evidence settled it. Names taken verbatim from the Wikipedia janya list resolve
to existing `ragas` rows *exactly* — including that page's own casing quirks (`Navarasa kannada`,
lowercase `k`) — and **every one of them has zero krithis**:

`Yadukula Kāmbhoji` · `Deva Manohari` · `Ravi Kriyā` · `Geya Hejjajji` · `Sindhu Rāmakriya` ·
`Karnātaka Kāpi` · `Navarasa kannada` · `Kumudhakriyā` · `Nāgachooḍāmani` · `Hamsadhwani`

`R__seed_04_raga_reference.sql`'s own header confirms it: *"Janya ragas from Wikipedia 'List of Janya
ragas'"*. The curated names were never wrong — the import simply bypassed them.

## Decision

### 1. The canonical authority is the Wikipedia pair

**Wikipedia "Melakarta" + "List of Janya ragas", treated as a single pair.** They share one
convention and, between them, cover the whole space by construction: 72 melakartas plus janyas
organised beneath each one.

**Convention:** diacritics as the source renders them; `th`/`sh`/`ch` for aspirates, sibilants and
affricates; Title Case; multi-word names spaced.

### 2. Raga Surabhi is a cross-check, not the naming authority

It is genuinely useful for commonly-performed ragas — it independently distinguishes `Kanada` from
`Kannada`, and confirms keepers for `Mohanam`, `Khamas`, `Atana`, `Hamsadwani`. Use it at step 3 of
the adjudication order below.

It is **not** adopted as the convention because it conflicts with the canonical pair on the central
question (no diacritics vs diacritics), covers ~170 of 1,145 rows (**15%**), and omits the rare
Dikshitar janyas this corpus is full of. Adopting it would mean renaming the rows that are already
correct and leaving ~975 ragas with no authority at all.

### 3. Merge direction

**Keep the Wikipedia-form row; merge the other into it.** For most groups this moves krithi links
*onto* the currently-empty row and deletes the one holding the data — the opposite of a naive
"keep the row with data" merge.

### 4. Adjudication order

1. Name appears in the Melakarta or Janya list → that spelling is the keeper.
2. *Both* sides appear there as separate entries → **DISTINCT** (this is what settles
   `Kanadā`/`Kannada`).
3. Still ambiguous and the raga is commonly performed → cross-check Raga Surabhi.
4. Only what survives 1–3 goes to musicological judgement (`carnatic-musicologist`).

### 5. Identity is a match key, not the display name

Store the display name in the canonical convention **and** a deterministic diacritic-free
`match_key` with a UNIQUE constraint:

```
name      = 'Yadukula Kāmbhoji'   -- canonical convention, displayed
match_key = 'yadukulakambhoji'    -- UNIQUE, never displayed
```

This is what actually closes the hole. It decouples *display spelling* from *identity*, so a future
import writing `yadukula kAmbhOji` collides with the existing row and is forced to match rather than
insert. Derive the key in one place (a generated/`STORED` column) rather than computing it
independently in SQL and Python, or the two will drift.

### 6. A seeded raga must never be introduced without checking existing reference data

`V40__seed_missing_trinity_ragas.sql` seeded 9 ragas for the Trinity import; **3 already existed
under a different spelling** (`Kalyāni`/`Mechakalyāni`, `Todi`/`Hanumatodi`, `Gauri`/`Gowri`), and in
each case the new row captured the corpus while the canonical one was left empty — **85 krithi links,
about a quarter of the total duplicate workload, caused by a seed migration rather than the import.**

Corollary, learned the same way: **a versioned (`V__`) migration must never depend on data seeded by
a repeatable (`R__`)**. Flyway runs all versioned migrations first, so V40's
`(SELECT id FROM ragas WHERE melakarta_number = N)` ran against an empty table and left all seven of
its janyas silently parentless.

## Long-term direction — the application becomes the authority

The Wikipedia pair is the **bootstrap**, not the destination. The intent is for Sangeetha Grantha
itself to become an authentic, citable source of raga information. This ADR is the starting point of
that path, not a permanent dependency.

What makes that credible is already partly built:

- **Versioned canon + provenance** ([ADR-014](./ADR-014-versioned-canon.md)) — `krithi_revisions` and
  per-section attribution mean a scholarly claim can be traced to the source document, extraction run
  or curator decision that produced it. The same discipline should extend to `ragas`.
- **Musicological review** — the `carnatic-musicologist` workflow already adjudicates lakshana
  questions (raga scales, janya-parent validity, ragamalika sequences) that no scraped list resolves.
- **A named authority per row** — once a raga's scale or parentage has been curated and reviewed
  against published sources (SSP, Prof. S. R. Janakiraman, and similar), the row is no longer
  "Wikipedia's"; it is ours, with a recorded basis.

The practical consequence for now: **treat Wikipedia as the default, not as truth.** Where our own
curation is better-grounded, it wins — record why, and cite the published source. Divergence from
the bootstrap is expected and desirable; drifting *accidentally* is what this ADR prevents.

Concretely, the schema should eventually carry, per raga, a naming/attribution source and the
published reference behind its scale — so "what is this raga, and on whose authority?" is answerable
from the data rather than from a migration comment. That is out of scope here; it is the direction
this decision is pointed at.

## Consequences

**Positive**
- One authority, one convention, full coverage of the corpus by construction.
- The merge direction becomes evidence-based rather than a matter of taste.
- The match key makes convention drift structurally unable to create duplicates again.
- No renaming of the ~527 rows that are already correct.

**Negative / accepted**
- Diacritics are harder to type than plain ASCII. Mitigated by the match key: search and import
  matching are diacritic-insensitive, so the burden is on display only, never on lookup.
- Wikipedia is editable and can itself be inconsistent (`Navarasa kannada`'s lowercase `k` is an
  artefact we have inherited). Accepted deliberately — it is the bootstrap, and the long-term
  direction above is the answer.
- Rare janyas absent from both Wikipedia pages still need musicological adjudication. Expected to be
  a small tail.

## References

- [TRACK-132](../../../conductor/tracks/TRACK-132-raga-deduplication-normalizer-fix.md) — implementation
- [Raga duplicate candidates](../../../conductor/tracks/evidence/TRACK-132-raga-duplicate-candidates.md) — the 244-pair source analysis
- [ADR-013](./ADR-013-db-migration-with-flyway.md) — Flyway-only migration policy
- [ADR-014](./ADR-014-versioned-canon.md) — versioned canon & provenance
- [Domain Model §6](../../01-requirements/domain-model.md#6-musicological-correctness-rules-lakshana) — musicological correctness rules
