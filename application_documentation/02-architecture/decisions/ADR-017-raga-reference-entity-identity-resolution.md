| Metadata | Value |
|:---|:---|
| **Status** | Accepted |
| **Version** | 1.4.0 |
| **Last Updated** | 2026-08-30 |
| **Author** | Sangeetha Grantha Team |
| **Deciders** | Sangeetha Grantha Team (Seshadri) |
| **Extends** | [ADR-016](./ADR-016-raga-naming-authority.md) — raga naming authority (the bootstrap this ADR builds identity on top of) |
| **Implemented by** | [TRACK-136](../../../conductor/tracks/TRACK-136-raga-identity-alias-resolution.md) (Phases 1–3); [TRACK-132](../../../conductor/tracks/TRACK-132-raga-deduplication-normalizer-fix.md) is Phase 0 (remediation); [TRACK-137](../../../conductor/tracks/TRACK-137-orphan-twin-raga-cleanup.md) cleans residual orphan twins |

# ADR-017: Raga Reference Entity Identity & Resolution

## Context

[ADR-016](./ADR-016-raga-naming-authority.md) settled *which spelling wins* and introduced a
`match_key` (§5) so convention drift cannot mint duplicates. [TRACK-132](../../../conductor/tracks/TRACK-132-raga-deduplication-normalizer-fix.md)
is cleaning up the ~313 krithi-links that landed on duplicate raga rows before that rule existed.

That work is necessary but it is **remediation, not a fix**: it removes today's duplicates without
removing the reason duplicates get created. Every failure catalogued in TRACK-132 has the same root
cause — **the raga's name is being used as its identity** — and a merge migration does not change
that. Left as-is, the next bulk import regenerates the mess against the freshly-cleaned rows, and
someone writes TRACK-2xx.

### Name is many-to-many with identity

A raga name cannot serve as a primary key, because the mapping is many-to-many in both directions:

| Direction | Example | Consequence today |
|:---|:---|:---|
| One raga → many **spellings** | `Yadukula Kāmbhoji` = `yadukula kAmbhOji` | Import mints a twin; corpus splits across two rows |
| One raga → many **tradition-names** | `Dharmavati` (Govindacharya) = `Dhāmavathi` (Venkatamakhin); `Gamanashrama` (53) = `Gamakakriyā` | No home for the second name except "duplicate" or "lossy merge" |
| Many ragas → one **near-name** | `Kanadā` ≠ `Kannada`; `Kalāvathi` (mela 31) ≠ `Kalāvati` (mela 16) | A too-aggressive normaliser silently corrupts links |

The four bug classes TRACK-132 documents — silent twin-minting on import, seed migrations that
re-create existing ragas (V40: ~85 links), un-mergeable nomenclature pairs, and un-distinguishable
homonyms — are all the same defect seen from four sides. ADR-016 §5 named the fix in one sentence
("identity is a match key, not the display name"); this ADR specifies the architecture that makes it
real and closes the remaining three sides.

### What ADR-016 left open

- §5 defined `match_key` but not **where the many alternate names live**. A single `name` +
  `match_key` per row still has no home for tradition-names, so A2/A3-style adjudications end up as
  prose in a markdown file rather than queryable data.
- It did not change the **import's resolution behaviour**. `normalize → match → else create` is still
  the code path that mints twins; a UNIQUE `match_key` would turn that into a hard *insert failure*,
  not a graceful match.
- It did not address **structural (lakshana) duplicates** — two rows with an identical scale under
  different names (`Gamakapriyā`/`Gamanapriyā`), which no name-based key can detect.

## Decision

Adopt an **entity + alias + controlled-resolution** model for ragas, in four parts. Parts 1–3 are the
architecture; Part 4 is the phased path (Phase 0 = the existing TRACK-132 remediation, unchanged).

### 1. Identity model: a raga is an entity with many aliases

Split *identity* from *surface form*.

- **`ragas`** remains the identity row: stable UUID (never reused), **one** canonical display `name`
  in the ADR-016 convention, the structural lakshana (`parent_raga_id`, `arohanam`, `avarohanam`,
  declared anya swaras), and a generated `match_key` (§2).
- **`raga_aliases`** (new) holds every *other* known surface form of that identity — one row per
  alias, each carrying its own `match_key` and **provenance**:

  ```
  raga_aliases(
    id            uuid pk,
    raga_id       uuid  -> ragas(id) on delete cascade,
    alias         text,                 -- the surface form, as the source renders it
    match_key     text generated stored,-- same normalisation as ragas.match_key (§2)
    alias_type    enum('transliteration','nomenclature','common','historical'),
    tradition     text null,            -- e.g. 'Venkatamakhin/asampurna', 'Govindacharya/sampurna'
    source        text,                 -- citation: SSP, Wikipedia janya list, expert clarification, …
    confidence    enum('high','medium','low'),
    created_at, updated_at
  )
  ```

  `composer_aliases` already exists and validates this shape for reference entities; ragas get the
  equivalent. This is the highest-leverage change: it turns *"is this the same raga?"* from a fuzzy
  string question into a lookup, and gives the nomenclature-tradition names a **first-class,
  cited home**. The A2/A3/B1/C3 clarifications become alias rows with expert provenance, not track
  prose:

  | `raga_id` → | alias | alias_type | tradition | source |
  |:--|:--|:--|:--|:--|
  | Gamakakriyā (janya of 53) | Gamanapriya, Gamakapriya | transliteration | — | corruption, orphan-merged |
  | Dharmavati (59) | Dhāmavathi | nomenclature | Venkatamakhin | SSP; Parandhāmavatī mudra (same scale) |
  | Nārīrītigowla (20) | Nārērētigowla | transliteration | — | corruption |
  | Andhali (28) | Andali | transliteration | — | Sangraha Chudamani / SSP |

  > **Aliases are same-identity only** (see Refinements, below). `Gamanāśrama` (mela-53 sampurna) is
  > **not** an alias of the janya `Gamakakriyā` — their scales differ, so they are two identities
  > linked by a `raga_relations` (`nomenclature_equivalent`) row. A `nomenclature` alias like
  > `Dhāmavathi`→`Dharmavati` is correct precisely because the scale is identical.

### 2. One normalisation function, owned by the database

ADR-016 §5 warned that if Python's `normalize_for_matching()` and the SQL `match_key` drift, the
guardrail passes rows the matcher would have merged. Remove the possibility of drift:

- `match_key` is a **generated `STORED` column** on **both** `ragas` and `raga_aliases`, computed by a
  single SQL normalisation function (`raga_match_key(text)`). Identity is **mela-qualified** —
  `(match_key, mela_disambiguator)`, not `match_key` alone — because the fold deliberately collapses
  spelling twins that then also collapse true homonyms (`Kalāvathi` 31 / `Kalāvati` 16). The fold
  `srimati` still maps the spelling `Shreemati` onto `Srimati` [8] (and, via alias, Shreemani [2]).
  The UNIQUE spans the **union** of both tables on that qualified key: no
  two distinct ragas may share an identity, and an alias may not collide with a *different* raga's
  identity. (Mechanism — a trigger-maintained `mela_disambiguator` column + identity table — is
  detailed in [TRACK-136](../../../conductor/tracks/TRACK-136-raga-identity-alias-resolution.md) §1.4;
  it cannot be a plain `GENERATED` column since a janya's mela is a parent join.)
- Python does not reimplement normalisation. It resolves by calling the DB (or a single shared,
  tested spec), so there is exactly one definition of "same key".

The normaliser is a **constraint-satisfaction problem with a frozen test suite**, not a bag of
`replace()` calls. It must be lenient enough to collapse the 114 twin groups yet strict enough to keep
the homonyms apart. TRACK-132 §1 already isolated the exact contract — treat it as the identity spec:

| Must merge (twins) | Must stay apart (real ragas) |
|:---|:---|
| `th/t`, `dh/d`, `sh/s`, `ch/c`, `gh/g`, `kh/k`, `bh/b`, `jh/j`, `w/v`, `oo/ū`, `ee/ī`, ITRANS caps, terminal `-am/-aM`, spacing, case | `kanada` ≠ `kannada` (**nn→n forbidden**, 11 links); `kalavati` th 31≠16; `srimati` sh/ee 2≠8; terminal `-i` preserved (`Bhairavi`, **28 links**); initial vowel preserved (`Abhogi`≠`Bhogi`); digraphs **mapped not deleted** (`Ranjani`/`Niranjani`/`Shreeranjani` stay 3) |

These cases are the acceptance test for any change to `raga_match_key`. Freeze them as a regression
gate in CI.

### 3. Resolution is a controlled operation — never a silent insert

The import's `normalize → match → else create` **is** the twin-minting bug. Replace it with lookup +
review-queue, so an unknown raga is a *reviewable event*, not an auto-mutation:

```
incoming raga name
  → raga_match_key(name)                      -- a bare name has no mela; matched on match_key
  → lookup across ragas ∪ raga_aliases identity keys
      ├─ exactly one hit → resolve to raga_id (done)
      ├─ multiple hits   → AMBIGUOUS (a homonym set, e.g. the two Kalāvatis). DO NOT pick. Enqueue.
      └─ no hit          → UNKNOWN. DO NOT insert. Enqueue to the raga-resolution review queue.
```

A curator then resolves each queue item one of three ways:

1. **Attach as a new alias** to an existing raga (the common case — a spelling we hadn't seen),
2. **Confirm a genuinely new raga** (creates the `ragas` row with its lakshana), or
3. **Disambiguate** a homonym — pick which identity each occurrence meant.

This rides on the existing Curator UI and review paradigm. It structurally eliminates the failure mode
that produced the entire TRACK-132 backlog: the import can no longer create a raga at all.

**Seed migrations resolve the same way.** ADR-016 §6 established that a seed must check existing
reference data before inserting; here that becomes mechanical — reference seeds live only in the `R__`
repeatable and upsert on the **identity key** (`ON CONFLICT (match_key, mela_disambiguator)`), never on
raw `name`, so a `V__` seed like V40 (which minted 6 duplicates by matching on name) is impossible to
repeat.

### 4. Lakshana-based duplicate detection and integrity constraints

Name-collision catches only name-shaped duplicates. Add the structural axis, as **alarms feeding the
review queue** — never as auto-merges, because same-scale is ambiguous:

- **Scale-collision alarm.** Two rows with an identical normalised swara-set are surfaced as merge
  *candidates*. A human decides, because same scale can mean a true duplicate
  (`Gamakapriyā`/`Gamanapriyā`), a mela-vs-janya nomenclature pair (`Dhāmavathi`/`Dharmavati` — the
  "Todi pathology"), **or** two distinct ragas that share arohana/avarohana but differ in
  gamaka/prayoga. The system surfaces; it does not decide.
- **Standing data-quality checks** (pure SQL, run in CI and the verification suite):
  - *janya ⊄ parent* — a janya carrying a swara its parent melakarta lacks (would have caught the
    Abheri/V49 defect from the stored data alone; see TRACK-132 §0g).
  - *mela-as-own-janya* — a janya whose scale is byte-identical to a melakarta and self-parents
    (Todi, Pūrvi, Dhāmavathi pathology).

### Provenance, and ADR-016's "application becomes the authority"

ADR-016 pointed at Sangeetha Grantha becoming a citable authority rather than a Wikipedia mirror; this
model is how that becomes true rather than aspirational. Every `ragas` row and every `raga_aliases`
row carries `source` + `confidence`, so *"what is this raga, and on whose authority?"* is answerable
from the data. Expert clarifications (e.g. the 2026-08-29 Carnatic-musician query sheet that resolved
the A2/A3/B/C questions) enter as first-class provenance on the alias/identity rows, consistent with
the versioned-canon discipline of [ADR-014](./ADR-014-versioned-canon.md).

### Refinements from implementation review (2026-08-29)

Two points sharpened while planning [TRACK-136](../../../conductor/tracks/TRACK-136-raga-identity-alias-resolution.md);
they refine Decision §1–§2 without changing the direction:

- **Identity is mela-qualified, not name-only.** The normalisation contract deliberately folds
  `th→t` and `sh/ee→s/i` to collapse spelling twins — but those same folds map genuinely **distinct**
  ragas to one key (`Kalāvathi` mela 31 / `Kalāvati` mela 16 → `kalavati`). The fold `srimati` still
  maps `Shreemati` onto `Srimati` [8] (mela-2 identity is a Shreemani alias, not a separate row).
  A name-only UNIQUE would therefore be unsatisfiable. The identity key
  is `(match_key, mela_disambiguator)` — the row's own melakarta number, or its parent's for a janya.
  Twins share a mela and still collapse; homonyms differ by mela and stay distinct. At import, a bare
  name that matches **more than one** identity is **ambiguous → resolution queue**, never auto-picked.
- **Alias (same identity) vs nomenclature-equivalence (distinct identities).** A `raga_aliases` row is
  a surface form of the *same* identity — same scale, same mela. Two rows that name one mela position
  under different systems but carry **different scales** (e.g. `Gamanāśrama` mela-53 sampurna ↔
  `Gamakakriyā` asampurna raganga, whose arohana omits N3) are **distinct identities** linked by a
  `raga_relations` (`nomenclature_equivalent`) row, *not* an alias — aliasing them would both collide
  on `match_key` and assert a false lakshana identity. The same-scale Todi-pathology case
  (`Dhāmavathi`→`Dharmavati`) remains a true alias.

## Scope and phasing

| Phase | What | Where |
|:---|:---|:---|
| **0. Remediate** (unchanged) | Merge existing duplicates, repoint `krithi_ragas` by `order_index` + `krithis.primary_raga_id`, handle the SET-NULL FK traps, patch the normaliser. **Still required** independent of this ADR. | [TRACK-132](../../../conductor/tracks/TRACK-132-raga-deduplication-normalizer-fix.md) |
| **1. Structural identity** | `raga_aliases` table; `raga_match_key()` function; generated `STORED match_key` on both tables; UNIQUE across the union; backfill aliases (incl. the expert-confirmed nomenclature names). Twin-minting becomes structurally impossible from here. | New track |
| **2. Controlled resolution** | Rewire import to lookup + review-queue-on-miss; seeds upsert on `match_key`. Root cause removed. | New track |
| **3. Integrity + provenance** | Scale-collision alarm; janya-subset and mela-as-janya checks in CI; `source`/`confidence` on identity and alias rows. | New track |

Phase 1 must land in the **same migration sequence as, or immediately after, Phase 0** — if Phase 0
merges without the UNIQUE `match_key`/alias structure in place, the next import can regenerate
duplicates against the freshly-cleaned rows. That ordering is the single most important commitment in
this ADR.

Deliberately **out of scope** here (candidates for a later ADR, noted so they are not lost): applying
the same entity/alias/resolution framework to **talas** and **composers**. `composer_aliases` is a
half-step already; generalising the pattern is worthwhile but is not a raga decision.

## Consequences

**Positive**
- Silent duplicate-minting on import becomes **structurally impossible**, not merely less likely — the
  class of bug TRACK-132 exists to clean up cannot recur.
- Nomenclature-tradition names (Govindacharya/Venkatamakhin) and every transliteration variant get a
  cited, queryable home instead of forcing a merge-or-duplicate choice.
- Homonyms stay distinct by construction; the frozen normaliser test suite is the guarantee.
- Structural (scale) duplicates become detectable — the one axis name-based dedup can never see.
- Expert adjudications become durable data with provenance, advancing ADR-016's "application is the
  authority" intent.

**Negative / accepted**
- More schema and a new import code path — a review queue, an alias table, a DB-owned normalisation
  function. Justified: the alternative is recurring cleanup tracks.
- Unknown ragas on import now require a curator action instead of silently appearing. This is the
  point — it is a feature, not a cost — but it adds a human step to bulk imports of never-seen ragas.
  Mitigated by the alias table making the common case (a new spelling of a known raga) a one-click
  attach.
- Backfilling aliases for 1,145 existing rows is a real effort. Bounded by starting from the
  TRACK-132 adjudicated MERGE list and the returned expert clarifications, which already enumerate the
  known alternate spellings.

## References

- [ADR-016](./ADR-016-raga-naming-authority.md) — raga naming authority & canonical reference data (extended here)
- [ADR-014](./ADR-014-versioned-canon.md) — versioned canon & provenance graph
- [ADR-013](./ADR-013-db-migration-with-flyway.md) — Flyway-only migration policy (R__ vs V__ discipline)
- [TRACK-132](../../../conductor/tracks/TRACK-132-raga-deduplication-normalizer-fix.md) — Phase 0 remediation; §0h holds the 2026-08-29 expert clarifications
- [raga-clarifications-musician-draft.md](../../../conductor/tracks/evidence/raga-clarifications-musician-draft.md) — expert query sheet (source of the A2/A3/B/C nomenclature provenance)
- [Domain Model §6](../../01-requirements/domain-model.md#6-musicological-correctness-rules-lakshana) — musicological correctness rules (lakshana), incl. janya-subset rule
