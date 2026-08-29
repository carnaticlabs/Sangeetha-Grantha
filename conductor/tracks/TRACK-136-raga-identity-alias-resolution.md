| Metadata | Value |
|:---|:---|
| **Status** | Ready to start — Spec & Plan Accepted; TRACK-132 Phase 0 gate cleared (PR #14 merged 2026-08-29) |
| **Version** | 1.3.0 |
| **Last Updated** | 2026-08-29 |
| **Author** | Sangeetha Grantha Team |
| **Priority** | P1 — structural fix; removes the root cause behind TRACK-132 |
| **Decision** | [ADR-017](../../application_documentation/02-architecture/decisions/ADR-017-raga-reference-entity-identity-resolution.md) — raga reference entity identity & resolution; extends [ADR-016](../../application_documentation/02-architecture/decisions/ADR-016-raga-naming-authority.md) |
| **Depends on** | [TRACK-132](./TRACK-132-raga-deduplication-normalizer-fix.md) (Phase 0 — **Batch A + Batch B** merged) · [TRACK-093](./TRACK-093-trinity-krithi-bulk-import.md) (corpus imported) |
| **Interacts with** | [TRACK-061](./TRACK-061-transliteration-aware-normalisation.md) (normaliser this track promotes to the identity function) · [ADR-012](../../application_documentation/02-architecture/decisions/ADR-012-unified-extraction-architecture.md) (Python extracts / Kotlin ingests) · [ADR-014](../../application_documentation/02-architecture/decisions/ADR-014-versioned-canon.md) (provenance) |

# TRACK-136: Raga Identity — Alias Table, Match-Key & Controlled Resolution

## Goal

Make silent duplicate raga-minting **structurally impossible** by implementing [ADR-017](../../application_documentation/02-architecture/decisions/ADR-017-raga-reference-entity-identity-resolution.md):
decouple a raga's *identity* from its *spelling*, give every alternate spelling and nomenclature-tradition
name a cited home, and turn import/seed resolution from a silent `else create` into a controlled
lookup with a human fallback.

This is the fix TRACK-132 is not. TRACK-132 (Phase 0) merges the ~313 links currently sitting on
duplicate rows; this track removes the reason those duplicates get created, so the next import cannot
regenerate them.

## Context

- **[ADR-017](../../application_documentation/02-architecture/decisions/ADR-017-raga-reference-entity-identity-resolution.md)** is **Accepted** (Seshadri, 2026-08-29) and specifies the target architecture in four parts. This track implements ADR-017 Phases 1–3.
- **Phase 0 is [TRACK-132](./TRACK-132-raga-deduplication-normalizer-fix.md)** — remediation, adjudication complete, migration pending. Its §0h holds the returned expert clarifications whose nomenclature answers become alias rows here.
- **Ordering constraint (the single most important commitment in ADR-017):** Phase 1's UNIQUE identity key must land in the **same migration sequence as, or immediately after, Phase 0 — and Phase 0 means both Batch A (17 adjudicated merges) *and* Batch B (~79 one-sided twins).** A UNIQUE constraint added while Batch B twins still exist would fail on migrate (blocker **B4**).

## Architecture review — incorporated 2026-08-29

An independent review against the accepted ADR, the frozen TRACK-132 suite, the seeded reference data,
and the live Kotlin ingestion path found the Intent sound but **five blockers** in the first Spec/Plan
draft. All are validated against code/DB and resolved below; the resolutions (D1–D5) are now part of
the Spec.

| # | Blocker (validated) | Resolution |
|:--|:--|:--|
| **B1** | th→t and sh/ee→s/i fold **distinct** ragas to one key: `Kalāvathi(31)`/`Kalāvati(16)`→`kalavati`, `Shreemati(2)`/`Srimati(8)`→`srimati`. Name-only UNIQUE contradicts acceptance criterion 4. | **D1** — identity key is **mela-qualified**, not name-only. Frozen suite updated (Poorvi/Pūrvi is now a *merge*, not a negative case). |
| **B2** | The live silent mint is **Kotlin `RagaRepository.findOrCreate`** (`findByNameNormalized → findByName → create`), called from `ImportService`, `KrithiCreationFromExtractionService`, `EntityResolutionServiceImpl`; plus admin `ReferenceDataService.createRaga` and a direct `INSERT` in `scripts/import_dikshitar_krithis.py`. The Python worker does **not** insert ragas. | **B2** — Phase 2 retargets Kotlin `findOrCreate`; §Mint-path inventory below is exhaustive and each path must route through the resolver or be forbidden. Aligns with ADR-012. |
| **B3** | Backfill aliased `Gamanashrama` onto janya `Gamakakriyā`, but the seed already stores **mela-53 `Gamanāśrama`** as its own identity with a **different scale** (`…P D2 N3 S` vs Gamakakriyā `…P D2 S`). The alias collides on `match_key` and is lakshana-wrong. | **D2** — distinct scales ⇒ distinct identities. Removed the alias row; the mela↔raganga link is a **nomenclature-equivalence relation**, not an alias. Contrast Dharmavati/Dhāmavathi (identical scale ⇒ alias is correct). |
| **B4** | TRACK-132 Batch B (~79 twins) still open; UNIQUE can't land after Batch A alone. | Gate (above + Plan) now requires **Batch A + B**. |
| **B5** | `krithi_ragas.raga_id` is `NOT NULL`; spec said "hold unresolved links" without saying where. | **D4** (refined per R3) — write **resolved** ragas' junction rows at their `order_index`; hold only the **unresolved** slots in the queue `context`; NULL `primary_raga_id` **only** if the primary itself is unknown. Ragamalika contiguity verified after the queue drains. |

### Resolved decisions (D1–D5)

- **D1 — Homonym identity → mela-qualified key.** The identity key is
  `(raga_match_key(name), mela_disambiguator)` where `mela_disambiguator` is the row's own
  `melakarta_number`, or its parent melakarta number for a janya. Spelling *twins* share a mela, so
  they still collapse; *homonyms* differ by mela (`Kalāvathi` 31 vs `Kalāvati` 16; `Shreemati` 2 vs
  `Srimati` 8), so they stay distinct. **At import, a bare name folds to `match_key` only:** a single
  identity hit resolves; a *multi-hit* (a homonym set) is **ambiguous → queue for human
  disambiguation**, never auto-picked.
- **D2 — Sampurna vs asampurna → two identities + nomenclature relation.** Rows with different scales
  are different identities even when they name the same mela position under two systems
  (`Gamanāśrama` mela-53 sampurna ↔ `Gamakakriyā` asampurna raganga). Link them with a
  `raga_relations` row (`relation='nomenclature_equivalent'`), **not** an alias. Aliases are reserved
  for identical-identity surface forms (spellings; and same-scale Todi-pathology names like
  `Dhāmavathi`→`Dharmavati`).
- **D3 — Union UNIQUE → trigger-maintained identity table.** PostgreSQL cannot put a UNIQUE on a
  `UNION`; a unique index on a matview is not transactional. Use a
  `raga_identity_keys(match_key, mela_disambiguator, raga_id)` table with `PRIMARY KEY (match_key,
  mela_disambiguator)`, maintained by triggers on `ragas` and `raga_aliases`. This is the one
  transactional mechanism and is the actual guardrail.
- **D4 — Unresolved persistence → write what resolved, hold only the gaps (refined per R3).** Verified:
  `krithis.primary_raga_id` nullable, `krithi_ragas.raga_id` NOT NULL. A krithi's **resolved** ragas
  are written to `krithi_ragas` **at their own `order_index`**; only the **unresolved** slots are held
  in `raga_resolution_queue.context` as `(krithi_id, order_index)`. `primary_raga_id` is set when the
  **primary** raga resolved, and left NULL **only** if the primary itself is unknown — never blanked
  because some *non-primary* raga is unknown. A ragamalika with one unknown raga therefore has an
  explicit, tracked hole (not a silent NULL), and TRACK-132's palindrome/contiguity invariant is
  verified **after** that krithi's queue is drained, not mid-flight.
- **D5 — Own the guardrail once.** The `match_key` UNIQUE guardrail proposed in **TRACK-132 §3** is
  **transferred to this track** (it is the same constraint). TRACK-132 §3 is descoped to point here so
  two migrations do not both add it.

### Re-review of v1.1.0 (2026-08-29) — R1–R5 + nits, all incorporated

The revised Spec was re-reviewed; B1–B5 confirmed closed. Five follow-ups, all validated and folded in:

| # | Sev | Finding (validated) | Resolution in Spec |
|:--|:--|:--|:--|
| **R1** | must | A PK/UNIQUE column is NOT NULL, so `mela_disambiguator` cannot be NULL; the **181** ragas (verified) with no resolvable mela would get no identity. | §1.4 — `mela_disambiguator int NOT NULL DEFAULT 0` **sentinel** (never NULL, so UNIQUE can't be defeated by NULL-distinctness); orphans key on `(…,0)` and collide → **forces curation**. Stated as a feature. |
| **R2** | must | A janya's mela is `parent.melakarta_number` — a cross-row join — so it **cannot** be a `GENERATED` column; and after dropping `name_normalized` UNIQUE the `R__` upsert has no `ON CONFLICT` target on `ragas`. | §1.4 — `mela_disambiguator` is a **real, trigger-maintained** column on `ragas` with `UNIQUE(match_key, mela_disambiguator)` = the seed conflict target (§1.5); `raga_identity_keys` still unions **alias** keys (Dhāmavathi→dhamavati ≠ dharmavati). |
| **R3** | must | D4's "no junction row + NULL primary" punches `order_index` holes in ragamalikas (breaks TRACK-132's palindrome invariant) and wrongly NULLs primary when only a *non-primary* raga is unknown. | D4 refined — write resolved ragas' junction rows at their `order_index`; NULL primary **only** if the primary is unknown; hold only missing slots in queue `context`; verify palindrome **after** the queue drains. |
| **R4** | should | ADR-017 Decision §§1–3 body still showed name-only UNIQUE and Gamanashrama-as-alias (stale vs the refinements). | ADR-017 §1/§2 body patched (see that ADR). |
| **R5** | should | Retagging Dhāmavathi as `common` dropped the Venkatamakhin `tradition` metadata. | §1.3/§1.7 — `alias_type='nomenclature'` + `tradition` restored for **same-identity** tradition names; `raga_relations` kept only for distinct-scale pairs. |

Nits also folded in: `Kanadā`/`Kannada` reclassified out of the same-key homonym set (they diverge at
the `nn` fold); cache only singleton hits; `raga_relations` `CHECK (from < to)`; triggers fire on
UPDATE of `parent_raga_id`/`melakarta_number`/`name`; `EntityResolutionServiceImpl` reclassified as a
*resolution* path (not a mint); Phase-1-without-Phase-2 sequencing guard added.

### Re-review of v1.2.0 (2026-08-29, r3) — "Plan is Acceptable"; nits only

Third pass: **zero remaining blockers**, architecture confirmed. Five nits, all folded in:

- **N1** (spec) — queue `UNIQUE (match_key, kind)` would keep firing after `rejected`/`attached`,
  blocking re-enqueue of a previously-rejected name → replaced with a **partial unique index
  `WHERE status = 'pending'`** (§2.2).
- **N2** (plan) — `mela_disambiguator` trigger must be **`BEFORE INSERT/UPDATE`** so UNIQUE/`ON CONFLICT`
  see the parent mela, not the `0` default (§1.4, Plan P1.4).
- **N3** (plan) — corrected the stale claim that Phase 0 re-parents most orphans (it fixes only V40's
  **7** of 181); added a **preflight `ASSERT`** for `(match_key, mela_disambiguator)` duplicates before
  the UNIQUE (§1.4, Plan P1.4).
- **N4** (hygiene) — refreshed the stale B5 row to the R3-refined D4; Plan P1.1 "3 → 4 defensive cases";
  added `ragas.mela_disambiguator` to the Exposed files list.
- **N5** (ADR) — ADR-017 §3 seed sentence corrected to `ON CONFLICT (match_key, mela_disambiguator)`.

---

## Intent
**Status:** Accepted

### Problem

Every failure class in TRACK-132 has one root cause: **the raga's `name` is its identity.** That makes
the name-to-identity mapping many-to-many and produces four recurring bugs — import twin-minting,
seed migrations re-creating existing ragas (V40: ~85 links), un-mergeable nomenclature pairs
(`Dharmavati`≡`Dhāmavathi`), and un-distinguishable homonyms (`Kanadā`≠`Kannada`, the two `Kalāvati`s).
A merge migration fixes instances, never the class.

### Desired outcome

A raga is an **entity** with a stable id, one canonical display name, structural lakshana, and **many
cited aliases**. Identity is a DB-owned, mela-qualified UNIQUE key. Import and seed **resolve** against
that identity (lookup → attach-alias / confirm-new / disambiguate), and never silently insert.
Structural (scale) duplicates and janya-integrity violations are **surfaced**, not auto-decided.
Duplicate-minting becomes impossible rather than merely unlikely.

### Non-goals

- Applying the same entity/alias/resolution model to **talas** and **composers** — worthwhile, but a
  later ADR/track. `composer_aliases` is the existing half-step.
- Changing the ADR-016 naming *authority* (Wikipedia pair) or merge *direction* — unchanged.
- Re-adjudicating TRACK-132's MERGE/DISTINCT list — this track consumes it, doesn't revisit it.

---

## Spec
**Status:** Accepted
**Accepted by:** Sangeetha Grantha Team (Seshadri)
**Accepted at:** 2026-08-29

Three phases, each independently deployable, in order. DDL below is indicative (final column types
follow PG18 + ADR-011 conventions; align Exposed tables per the postgres-flyway-db skill).

### Phase 1 — Structural identity (schema + match-key)

**1.1 `raga_match_key(text)` — one normalisation function, owned by the DB.**
A single `IMMUTABLE` SQL function is the sole definition of the *name* fold (identity also carries the
mela disambiguator, §1.4). It implements exactly the folding contract frozen in TRACK-132 §1:

| Must fold (→ same key) | Must preserve (→ distinct key) |
|:---|:---|
| diacritics; case; spacing; ITRANS/HK internal caps; aspirates `th/dh/gh/kh/bh/jh`→base; `sh/ś`→`s`; `ch`→`c`; `w`→`v`; `oo`→`u`; `ee`→`i`; terminal `-am/-aM` | **no** `nn`→`n` de-doubling (`Kanadā`≠`Kannada`, 11 links); terminal `-i` (`Bhairavi`, 28 links); initial vowel (`Abhogi`≠`Bhogi`); digraphs **mapped, never deleted** (`Ranjani`/`Niranjani`/`Shreeranjani` stay 3) |

**Frozen acceptance suite (updated per B1 + nit):** the `oo→u` axis is now guarded by the mela
disambiguator (D1), not by name-fold, and Poorvi/Pūrvi is a *merge*, not a negative case. The suite is:

- **2 same-name-key homonym pairs** — fold to one `match_key` but must stay **distinct identities** via
  the mela disambiguator (D1): `kalavati` (`Kalāvathi` 31 / `Kalāvati` 16), `srimati` (`Shreemati` 2 /
  `Srimati` 8).
- **4 defensive assertions** — `nn` **not** de-doubled, so `Kanadā` (22) ≠ `Kannada` (29) never even
  share a name-key (nit — they diverge at the fold, so they are *not* a same-key homonym); terminal
  `-i` preserved (`Bhairavi`, 28 links); initial vowel preserved (`Abhogi`≠`Bhogi`); digraphs mapped
  not deleted (Ranjani family stays 3).

Digraph handling **must map, not delete**. Python does not reimplement this — ingestion computes the
key via `SELECT raga_match_key(...)` inside the Kotlin transaction (ADR-012; see B2).

**1.2 `match_key` as a generated `STORED` column** on `ragas`, from `raga_match_key(name)`.

**1.3 `raga_aliases` table** — one row per alternate surface form, with provenance:

```sql
CREATE TABLE raga_aliases (
  id          uuid PRIMARY KEY DEFAULT uuidv7(),
  raga_id     uuid NOT NULL REFERENCES ragas(id) ON DELETE CASCADE,
  alias       text NOT NULL,                                   -- surface form as the source renders it
  match_key   text GENERATED ALWAYS AS (raga_match_key(alias)) STORED,
  alias_type  text NOT NULL CHECK (alias_type IN
                 ('transliteration','nomenclature','common','historical')),
  tradition   text,          -- e.g. 'Venkatamakhin/asampurna', 'Govindacharya/sampurna'; NULL if n/a
  source      text NOT NULL, -- citation: 'SSP', 'Wikipedia janya list', 'expert 2026-08-29', …
  confidence  text NOT NULL DEFAULT 'high' CHECK (confidence IN ('high','medium','low')),
  created_at  timestamptz NOT NULL DEFAULT now(),
  updated_at  timestamptz NOT NULL DEFAULT now()
);
```

An alias is a surface form of **the same identity** (same scale, same mela). `alias_type='nomenclature'`
+ `tradition` is retained for **same-identity** tradition names (e.g. `Dhāmavathi` is the Venkatamakhin
name for the *same* mela-59 identity — R5). It is **distinct-scale** pairs that are not aliases (§1.6).

**1.4 Mela-qualified identity + UNIQUE (D1, D3; reworked per R1/R2).** Identity is
`(match_key, mela_disambiguator)`, where `mela_disambiguator` = the row's own `melakarta_number`, else
its parent's, else **`0` (sentinel: mela unresolved)**. Two constraints, because PostgreSQL requires
the seed-upsert conflict target to live *on* `ragas` (R2), while the union with alias keys must be
enforced globally (D3):

```sql
-- (a) A real, trigger-maintained column on ragas — NOT a GENERATED column, because a janya's
--     mela is parent.melakarta_number, a cross-row join (R2). Sentinel 0, never NULL, so a
--     UNIQUE cannot be defeated by NULL-distinctness (R1).
ALTER TABLE ragas ADD COLUMN mela_disambiguator int NOT NULL DEFAULT 0;   -- trigger-maintained
ALTER TABLE ragas ADD CONSTRAINT ragas_identity_uq UNIQUE (match_key, mela_disambiguator);
--     ^ this is the R__seed_04 ON CONFLICT target (§1.5, §2.4).

-- (b) The global union table: every raga identity key PLUS every alias key whose (match_key, mela)
--     differs from its raga's own (e.g. Dhāmavathi → dhamavati ≠ dharmavati). Trigger-maintained
--     from BOTH ragas and raga_aliases; PK rejects a collision across the union.
CREATE TABLE raga_identity_keys (
  match_key          text NOT NULL,
  mela_disambiguator int  NOT NULL,       -- 0 sentinel when unresolved
  raga_id            uuid NOT NULL REFERENCES ragas(id) ON DELETE CASCADE,
  PRIMARY KEY (match_key, mela_disambiguator)
);
```

**Consequence of the `0` sentinel (state it — R1):** the **181** existing ragas with no resolvable mela
(no `melakarta_number`, no parent with one — verified 2026-08-29) all key on `(…, 0)`. Two genuinely
distinct such orphans sharing a name-key would then collide and **force curation** (assign a mela) —
this is a feature: identity is refused until lakshana is known. **Note (N3):** TRACK-132 Phase 0 only
re-parents V40's **seven** parentless janyas (systemic finding 1) — the other ~174 of the 181 remain,
so P1.4 must **preflight-assert** no `(match_key, mela_disambiguator)` duplicate (sentinel 0 included)
*before* adding the UNIQUE, or the migration fails on a leftover same-key orphan pair. The
`mela_disambiguator` trigger is **`BEFORE INSERT/UPDATE`** (N2) so the UNIQUE and `ON CONFLICT` see the
resolved parent mela, not the `0` default; it **fires on UPDATE too** (`parent_raga_id`,
`melakarta_number`, `name` changes rewrite the affected identity rows — nit).

An alias whose `(match_key, mela)` equals its **own** raga's identity contributes no new
`raga_identity_keys` row (no-op); one that differs inserts a lookup row; one equal to a **different**
raga's identity is rejected at commit.

**1.5 `name_normalized` coexistence (must-specify gap; R2).** `ragas.name_normalized` is UNIQUE today
and is the `R__seed_04` `ON CONFLICT` target. Decision: **`ragas_identity_uq (match_key,
mela_disambiguator)` becomes the identity authority and the new `ON CONFLICT` target; `name_normalized`
is retained for display/search only and its UNIQUE constraint is dropped** in the same migration (two
competing unique keys would fight).

**1.6 `raga_relations` — nomenclature equivalence (D2).** **Distinct-scale** rows that name one mela
position under two systems are linked, not aliased:

```sql
CREATE TABLE raga_relations (
  from_raga_id uuid NOT NULL REFERENCES ragas(id) ON DELETE CASCADE,
  to_raga_id   uuid NOT NULL REFERENCES ragas(id) ON DELETE CASCADE,
  relation     text NOT NULL CHECK (relation IN ('nomenclature_equivalent')),
  source       text NOT NULL,
  CHECK (from_raga_id < to_raga_id),      -- undirected: store each pair once (nit)
  PRIMARY KEY (from_raga_id, to_raga_id, relation)
);
```

**1.7 Backfill (corrected per B3/R5).** Seed `raga_aliases` from (a) the TRACK-132 adjudicated MERGE
list (each losing spelling → `transliteration` alias of the keeper) and (b) the §0h clarifications.
Only **same-identity** rows below; `Gamanashrama` is **not** here (it is a `raga_relations` row, §1.6):

| raga_id → keeper | alias | alias_type | tradition | source |
|:---|:---|:---|:---|:---|
| Dharmavati (mela 59) | Dhāmavathi | nomenclature | Venkatamakhin | SSP; Parandhāmavatī mudra (identical scale, Todi-pathology) |
| Nārīrītigowla (mela 20) | Nārērētigowla | transliteration | — | corruption (TRACK-132 §0h C3) |
| Andhali (janya of 28) | Andali | transliteration | — | Sangraha Chudamani / SSP |
| Poorvi (janya of 15) | Pūrvi | transliteration | — | TRACK-132 §0h A1 |
| Dwijavanthi (janya of 28) | Jujavanthi | transliteration | — | Wikipedia janya list |
| Gamakakriyā (janya of 53) | Gamakapriya, Gamanapriya | transliteration | — | orphan-merged (TRACK-132 §0h A2) |

`raga_relations`: `Gamanāśrama (mela 53) ↔ Gamakakriyā` as `nomenclature_equivalent` (source: expert
2026-08-29) — **not** an alias, per D2/B3.

### Phase 2 — Controlled resolution (Kotlin ingestion; per B2)

**2.1 Resolver contract — replaces `RagaRepository.findOrCreate`.** The single resolution entry point:

```
resolve_raga(name, mela?):                       -- mela optional; usually absent at import
  key = SELECT raga_match_key(name)              -- computed in the Kotlin txn (ADR-012)
  hits = identity rows with this match_key
  if hits.size == 1:            return hits[0].raga_id
  if hits.size >  1:            enqueue(AMBIGUOUS, name, key, hits); return UNRESOLVED   -- homonym (D1)
  else /* 0 */:                 enqueue(UNKNOWN,   name, key);       return UNRESOLVED
```

**No ingestion code path inserts a `ragas` row.** `findOrCreate`'s `else create` branch is deleted;
`UNRESOLVED` links are held per D4, not fabricated.

**2.2 `raga_resolution_queue`** (per must-specify gaps — dedup + lakshana capture):

```sql
CREATE TABLE raga_resolution_queue (
  id          uuid PRIMARY KEY DEFAULT uuidv7(),
  raw_name    text NOT NULL,
  match_key   text NOT NULL,
  kind        text NOT NULL CHECK (kind IN ('unknown','ambiguous')),
  context     jsonb,          -- [{krithi_id, title, order_index, source_url, extraction_run}] — enough to adjudicate + to insert the junction on resolve (D4)
  proposed_lakshana jsonb,    -- confirm-new needs parent/arohana/avarohana; collected here
  status      text NOT NULL DEFAULT 'pending'
                CHECK (status IN ('pending','attached','created','disambiguated','rejected')),
  resolved_raga_id uuid REFERENCES ragas(id),
  created_at  timestamptz NOT NULL DEFAULT now(),
  resolved_at timestamptz
);
-- N1: pending-only uniqueness — a *plain* UNIQUE(match_key, kind) would keep firing after a row is
-- rejected/attached, so re-importing a previously-rejected name could never re-enqueue. Scope it:
CREATE UNIQUE INDEX raga_resolution_queue_pending_uq
  ON raga_resolution_queue (match_key, kind) WHERE status = 'pending';
```

One unknown spelling therefore yields **one** *pending* queue row (its occurrences accumulate in
`context`), not a flood — while a later re-occurrence of a resolved/rejected key can still open a fresh
pending row.

**2.3 Curator resolution — three actions:** *attach-alias* to an existing raga (unknown → known
spelling), *confirm-new* (creates the `ragas` row from `proposed_lakshana` — parent, arohana,
avarohana required, satisfying ADR-017 §3.2), or *disambiguate* (ambiguous → pick which homonym each
`context` link meant). All write `AUDIT_LOG` (repo rule) and are role-gated (ADR-004). On resolve,
insert the held `krithi_ragas` rows and set `primary_raga_id` (D4). Rides on the existing Curator
UI/review paradigm — a new "Unresolved ragas" queue view.

**2.4 Seed discipline (ADR-016 §6, now mechanical).** Reference seeds live only in the `R__`
repeatable and **upsert on the identity key** (§1.5), never on raw `name`. A `V__` seed that mints by
name (the V40 pathology) is thereby impossible.

**2.5 `entity_resolution_cache` relationship (must-inventory gap).** The existing
`entity_resolution_cache` (V15, `EntityResolutionCacheTable`) maps raw→resolved entity as a
performance cache. It is **not** replaced: it becomes a read-through cache in front of `resolve_raga`,
keyed by `match_key`, and is **invalidated** whenever an alias is attached or a raga is created/merged
(otherwise it would re-serve a stale miss). **Cache only singleton (`hits.size == 1`) resolutions**
(nit) — caching an ambiguous `kalavati` → first row would skip the disambiguation queue (§2.1). The
queue is the system of record for *misses* and *ambiguous* keys; the cache is only for unambiguous
*hits*.

### Phase 3 — Integrity + provenance

**3.1 Scale-collision alarm.** A data-quality query surfaces rows with an identical normalised
swara-set as *merge candidates* into the resolution queue — **never auto-merged** (same-scale is
ambiguous: true duplicate vs mela/janya nomenclature vs distinct-by-gamaka). Human decides.

**3.2 Standing lakshana checks** (pure SQL, in CI + the verification suite):
- *janya ⊄ parent* — a janya carrying a swara its parent melakarta lacks (would have caught Abheri/V49
  from stored data alone; TRACK-132 §0g).
- *mela-as-own-janya* — a janya whose scale is byte-identical to a melakarta and self-parents
  (Todi/Pūrvi/Dhāmavathi pathology).

**3.3 Provenance on identity rows.** Add `source` + `confidence` to `ragas` (mirroring `raga_aliases`)
so "what is this raga, and on whose authority?" is answerable from data (ADR-016 long-term direction,
ADR-014 discipline). Expert clarifications enter as first-class provenance.

### Mint-path inventory (Phase 2 is incomplete until every path is closed — per B2)

| Path | Layer | Action |
|:---|:---|:---|
| `RagaRepository.findOrCreate` | Kotlin DAL | Replace `else create` with `resolve_raga`; callers (`ImportService`, `KrithiCreationFromExtractionService`, `EntityResolutionServiceImpl`) route through it |
| `ReferenceDataService.createRaga` (admin UI) | Kotlin API | Keep — but route through identity-key check; it is a *curated* create, allowed, must not bypass UNIQUE |
| `EntityResolutionServiceImpl` (in-memory `groupBy{…}.mapValues{ first() }`) | Kotlin API | **Resolution path, not a mint path** (nit — it does not insert). Still must change: the silent `first()` on a key collision must become an **ambiguous → queue** outcome, not a silent pick |
| `scripts/import_dikshitar_krithis.py` (direct `INSERT INTO ragas`) | Python one-off | **Forbid** — retire the script or route via the API resolver; add a CI grep guard against `INSERT INTO ragas` outside migrations |

### Acceptance criteria

1. A second import of the Trinity corpus creates **0** new `ragas` rows; every previously-seen spelling
   resolves; genuinely-new names land in `raga_resolution_queue` (`unknown`), homonyms land as
   `ambiguous`, neither in `ragas`.
2. The frozen suite (2 same-key homonym pairs distinct + 4 defensive assertions) passes against the identity key.
3. Inserting a `ragas`/`raga_aliases` row whose `(match_key, mela)` collides with a *different* raga's
   identity **fails at the DB**, not silently.
4. `Kanadā`/`Kannada`, both `Kalāvati`s, `Shreemati`/`Srimati` remain distinct identities post-migration.
5. The two lakshana checks (3.2) run green in CI and flag zero known-good rows as false positives.
6. No ingestion path inserts a `ragas` row (CI grep guard green; `findOrCreate` has no create branch).

---

## Plan
**Status:** Accepted
**Accepted by:** Sangeetha Grantha Team (Seshadri)
**Accepted at:** 2026-08-29

### Files that change (impact list — per review recommendation)

- **DB migrations:** `raga_match_key` fn, `ragas.match_key` col, `raga_aliases`, `raga_identity_keys`
  (+triggers), drop `name_normalized` UNIQUE, `raga_relations`, `raga_resolution_queue`, `ragas`
  provenance cols. (Flyway `VNN__…`, ADR-013.)
- **Reference seed:** `database/migrations/R__seed_04_raga_reference.sql` — alias/relation backfill,
  `ON CONFLICT` target → identity key.
- **DAL (Exposed):** `RagaRepository.kt` (resolver), new `RagaAliasesTable`, `RagaIdentityKeysTable`,
  `RagaRelationsTable`, `RagaResolutionQueueTable`; `Ragas` table gains **`match_key` and
  `mela_disambiguator`** (N4).
- **API/services:** `EntityResolutionServiceImpl`, `ImportService`, `KrithiCreationFromExtractionService`,
  `ReferenceDataService`; new curator queue endpoints.
- **Frontend (Curator UI):** "Unresolved ragas" queue view (list, attach-alias, confirm-new, disambiguate).
- **Worker:** delete the `INSERT INTO ragas` in `scripts/import_dikshitar_krithis.py`; CI guard.
- **Docs:** `application_documentation/04-database/schema.md`, `openapi/sangita-grantha.openapi.yaml`,
  `current-versions.md` if applicable.

### Phase 1 — schema (order matters; all Flyway per ADR-013)

1. **`VNN__raga_match_key_function.sql`** — `IMMUTABLE raga_match_key(text)` + inline `ASSERT`s for the
   4 defensive cases (§1.1).
2. **`VNN__ragas_match_key_column.sql`** — generated `STORED match_key` on `ragas` (no UNIQUE yet).
3. **Gate on TRACK-132 Phase 0 — Batch A *and* Batch B merged** (blocker B4).
4. **`VNN__raga_identity_and_aliases.sql`** — `ragas.mela_disambiguator` (NOT NULL DEFAULT 0) +
   **`BEFORE INSERT/UPDATE` trigger** (N2) → backfill existing rows → **preflight `ASSERT` no
   duplicate `(match_key, mela_disambiguator)`, sentinel 0 included** (N3) → `UNIQUE(match_key,
   mela_disambiguator)` (seed target); `raga_aliases`; `raga_relations` (`CHECK from<to`);
   `raga_identity_keys` (+triggers on both tables, incl. UPDATE, PK enforces the union); **drop
   `ragas.name_normalized` UNIQUE** (§1.5).
5. **`R__seed_04_raga_reference.sql`** — alias + relation backfill (§1.7); `ON CONFLICT` → identity key.
   *(H1 `Veeravasantham` avarohanam, H2 `Nārērētigowla` rename, H3 `kalAvati kamalAsana` retag are
   lakshana/link fixes and belong to **TRACK-132 Phase 0**, not here — moved out per review.)*
6. **Exposed DAL** — `RagaAliasesTable`, `RagaIdentityKeysTable`, `RagaRelationsTable`; `match_key` **and
   `mela_disambiguator`** on
   `Ragas`; integration tests migrate real containers and catch drift.

### Phase 2 — controlled resolution

7. **`RagaRepository.findOrCreate` → `resolve_raga()`** (Kotlin DAL, blocker B2): delete the create
   branch; single/zero/multi-hit semantics (§2.1); `raga_match_key` via `SELECT` in the txn (ADR-012).
8. **`VNN__raga_resolution_queue.sql`** — the queue table (§2.2), **partial unique index on
   `(match_key, kind) WHERE status = 'pending'`** (N1 — not a plain UNIQUE).
9. **Close every mint path** per the inventory: `EntityResolutionServiceImpl`, `ReferenceDataService`,
   retire `import_dikshitar_krithis.py` INSERT + CI grep guard.
10. **Backend + Curator UI** — "Unresolved ragas" queue: attach-alias / confirm-new (lakshana form) /
    disambiguate; audited, role-gated. `entity_resolution_cache` as read-through + invalidation (§2.5).
11. **Seed upsert-on-identity-key** in `R__seed_04`.

### Phase 3 — integrity + provenance

12. **`VNN__raga_provenance_columns.sql`** — `source` + `confidence` on `ragas`; backfill known rows.
13. **Data-quality checks** — janya⊄parent + mela-as-own-janya SQL, wired into `make` + CI +
    verification; scale-collision alarm feeds the queue.

### Verification

- `make test` / `make test-integration` / `make test-frontend` green; paste output (repo rule).
- `verify-import` skill run after the re-import dry-run proving acceptance criteria 1 & 6.
- Migrations tested via `make db-reset` from scratch, not only incrementally (postgres-flyway-db skill).
- Junction-table population confirmed DB → API → UI; ragamalika integrity preserved (TRACK-132 §4).
- `make check-docs` for the doc updates.

### Rollout & risk

- **Sequencing is the top risk** — Phase 1 step 4 must not precede TRACK-132 Batch A **and** B (B4).
  Encoded by gating the identity table on both batches.
- Phases are independently deployable; Phase 2 can trail Phase 1 without leaving the DB inconsistent
  (identity structure is inert until the resolver uses it). **But** a live import path hitting Phase 1's
  UNIQUE *before* Phase 2's `resolve_raga` ships would turn silent mints into hard insert failures (nit).
  So either land Phase 2's resolver in the same release, or have the still-present `findOrCreate` catch
  the unique violation and fall back to a lookup until Phase 2 replaces it.
- Backfill effort bounded by starting from the TRACK-132 adjudicated list + §0h answers.

---

## Implementation Plan
- [ ] P1.1 `raga_match_key()` function + inline assertions (VNN)
- [ ] P1.2 `ragas.match_key` generated column (VNN, no unique yet)
- [x] P1.3 (gate) TRACK-132 Phase 0 **Batch A + B** merged — cleared 2026-08-29 (PR #14, `9dc29f3`)
- [ ] P1.4 `raga_aliases` + `raga_relations` + `raga_identity_keys`/triggers; drop `name_normalized` UNIQUE (VNN)
- [ ] P1.5 `R__seed_04` alias + relation backfill; `ON CONFLICT` → identity key
- [ ] P1.6 Exposed tables + `Ragas.match_key` **and `Ragas.mela_disambiguator`**; integration tests
- [ ] P2.7 `resolve_raga()` — delete `findOrCreate` create branch; key via DB in Kotlin txn
- [ ] P2.8 `raga_resolution_queue` with **partial unique `(match_key, kind) WHERE status='pending'`** (VNN)
- [ ] P2.9 Close all mint paths (EntityResolution, ReferenceData, retire python INSERT + CI guard)
- [ ] P2.10 Curator "Unresolved ragas" queue UI + attach/confirm/disambiguate (audited, role-gated); cache read-through + invalidation
- [ ] P2.11 `R__seed_04` upsert-on-identity-key
- [ ] P3.12 `ragas` provenance columns (VNN) + backfill
- [ ] P3.13 janya⊄parent + mela-as-own-janya checks in CI; scale-collision alarm
- [ ] Verification suite + re-import dry-run proving acceptance criteria

## Progress Log
- **2026-08-29**: Track created from [ADR-017](../../application_documentation/02-architecture/decisions/ADR-017-raga-reference-entity-identity-resolution.md). Intent accepted; Spec & Plan drafted.
- **2026-08-29**: Architecture review incorporated. Five blockers validated against code/DB and resolved (D1–D5): mela-qualified identity key (homonyms), Phase 2 retargeted at Kotlin `findOrCreate`, Gamanashrama moved from alias to `raga_relations`, UNIQUE gated on TRACK-132 Batch A+B, unresolved-link persistence defined. Added mint-path inventory, `name_normalized` coexistence, queue dedup + lakshana capture, `entity_resolution_cache` relationship, files-that-change list. H1/H2/H3 relocated to TRACK-132 Phase 0; §3 guardrail transferred here from TRACK-132 §3 (D5). Version → 1.1.0. Still Draft — awaiting Plan accept gate.
- **2026-08-29 (r2)**: Re-review incorporated; prior 5 blockers confirmed closed. R1–R5 + nits validated and folded in: `mela_disambiguator` NOT NULL sentinel `0` (181 orphans verified) with a **real trigger-maintained column + `UNIQUE(match_key, mela_disambiguator)` on `ragas`** as the seed `ON CONFLICT` target (a `GENERATED` column is impossible — janya mela is a parent join), `raga_identity_keys` unions alias keys; D4 refined for ragamalika holes (write resolved slots, NULL primary only if primary unknown, verify palindrome post-drain); `nomenclature`+`tradition` restored on same-identity aliases; `raga_relations CHECK(from<to)`; triggers on UPDATE; cache singleton-only; `EntityResolutionServiceImpl` reclassified (resolution, not mint); ADR-017 §§1–2 body patched (R4). Version → 1.2.0. Still Draft — awaiting Plan accept gate.
- **2026-08-29 (r3)**: Third review pass — **zero blockers, "Plan is Acceptable"**. Five nits folded in (N1 partial-unique pending-only queue index; N2 BEFORE trigger; N3 preflight ASSERT + corrected 7-of-181 Phase-0 claim; N4 doc hygiene — stale B5 row, P1.1 case count, Exposed files list; N5 ADR seed conflict-target). Version → 1.3.0. Ready for the Plan accept gate.
- **2026-08-29**: Spec and Plan **Accepted** (Seshadri). Track status → Ready; implementation remains gated on TRACK-132 Phase 0 Batch A+B.
- **2026-08-29**: TRACK-132 Phase 0 (Batch A+B) **merged to main** (PR #14, `9dc29f3`, CI green). P1.3 gate cleared → status **Ready to start**; implementation may begin at P1.1 (`raga_match_key()` function).
