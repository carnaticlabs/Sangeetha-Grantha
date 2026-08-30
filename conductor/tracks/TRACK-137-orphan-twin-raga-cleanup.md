| Metadata | Value |
|:---|:---|
| **Status** | Done — merged to main (PR #16), verified end-to-end against the restored corpus (2026-08-30) |
| **Version** | 1.5.0 |
| **Last Updated** | 2026-08-30 |
| **Author** | Sangeetha Grantha Team |
| **Priority** | P2 — data correctness (residual duplicates) |
| **Decision** | [ADR-016](../../application_documentation/02-architecture/decisions/ADR-016-raga-naming-authority.md) (naming authority) · [ADR-017](../../application_documentation/02-architecture/decisions/ADR-017-raga-reference-entity-identity-resolution.md) (identity) |
| **Depends on** | [TRACK-136](./TRACK-136-raga-identity-alias-resolution.md) (identity keys, `resolveRaga`, `track132_merge_raga` all in place) |
| **Interacts with** | [TRACK-132](./TRACK-132-raga-deduplication-normalizer-fix.md) (same merge mechanism / adjudication discipline) |

# TRACK-137: Orphan-Twin Raga Cleanup

## Goal

Merge the residual duplicate raga rows that TRACK-132 did not cover — **orphan twins**: ITRANS/
Harvard-Kyoto-spelled rows with no stored scale and no parent (mela sentinel `0`) that sit beside a
curated Wikipedia-form twin holding the same identity. Merging each into its keeper relocates **42**
krithi links (41 from the 18 detection-query orphans + 1 from `bhauLi`). **18 of 20** orphan spellings
then singleton-resolve; **#14 `jIvantikA` and #20 `Shreemati` remain homonym-ambiguous** (D1) — that
is the correct outcome, not a miss.

This is a data-cleanup batch on top of TRACK-136, not a change to the identity architecture. TRACK-136
made duplicate-minting impossible and correctly *surfaces* these leftovers as ambiguous; this track
removes them.

## Context — how these surfaced

The TRACK-136 Phase-1–3 re-import dry-run (2026-08-30) established that, on a pristine seed, every
merge-loser spelling resolves. But the **live dev/prod DB** carries import cruft the seed does not: a
set of orphan rows whose `raga_match_key` collides with a real seeded raga. The resolver handles them
safely (ambiguous → queue, never a silent mint or mis-attribution), but each is genuinely the same
raga as its twin and should be merged.

Detection query (the definition of "orphan twin"):

```sql
SELECT o.name, o.match_key,
       (SELECT count(*) FROM krithi_ragas kr WHERE kr.raga_id = o.id) AS links
FROM ragas o
WHERE o.mela_disambiguator = 0 AND o.arohanam IS NULL
  AND EXISTS (SELECT 1 FROM ragas r2
              WHERE r2.id <> o.id AND r2.match_key = o.match_key AND r2.arohanam IS NOT NULL);
```

Live DB (2026-08-30, Flyway V56): **18 rows**, **41 links**. Plus `bhauLi` (not in this query) = 42.

## Validation review — incorporated 2026-08-30

Independent review against the live DB, `track132_merge_raga`, `resolveRaga` / `raga_identity_keys`,
V50/`R__seed_05` asserts, `RagaIdentityTrack136Test`, ADR-017 AC4, and a
[carnatic-musicologist](../../.claude/agents/carnatic-musicologist.md) pass on the 20 keepers.
Lakshana of every MERGE is sound; none should flip to DISTINCT. The blockers below are *document /
verification / rider* errors, not keeper errors.

| # | Sev | Finding (validated) | Resolution in this draft |
|:--|:--|:--|:--|
| **B1** | must | Loser #5 is stored as `bhUpALaM` (ASCII `A`/`L`), not `bhUpālaM`. `track132_lookup_raga` is exact `name =`. The diacritic spelling would **no-op** and leave 3 links (incl. two `viSva nAthaM bhajEhaM` malika slots) on the orphan. | Merge table uses `bhUpALaM`. |
| **B2** | must | `jIvantikA` / `Jeevantikā` [17] / `Jeevanthikā` [48] all fold to `jivantika`. After deleting the orphan, a bare-name re-import still hits **two** identities → queue. | Verification carves #14 out: merge still required (Dikshitar `bRhadISa kaTAkshENa` moves to [48]); re-import stays `ambiguous` (D1). |
| **B3** | must | After deleting `Shreemati` [2] with **no** alias, `resolveRaga('Shreemati')` singleton-hits **`Srimati` [8]** — silent mis-attribution. Adding the alias inserts `(srimati, 2)` on Shreemani, so the name becomes a homonym with Srimati [8] and **stays ambiguous**. It never unique-resolves. | Alias is a *safety rail*, not a resolve. Verification: #20 re-import is `ambiguous` (Shreemani vs Srimati), never unique to Srimati [8]. |
| **B4** | must | `track132_apply_raga_merges()` (V50, re-invoked by `R__seed_05`) asserts `Shreemati`/`Srimati` DISTINCT. Dropping the seed insert without replacing that function **fails `make db-reset`**. V50 is immutable. | V57 `CREATE OR REPLACE`s the apply function (or the assert call) to drop that pair. Fold-level V51/`matchKey("Shreemati")` tests stay — they test the function, not the row. |
| **B5** | must | Track has no Intent / Spec / Plan sections. Conductor human gates cannot accept what does not exist. | Intent drafted 2026-08-30 (Status: Draft). Spec/Plan stubs present; `/spec-from-track` only after Intent is Accepted. |
| **S1** | should | Footnote 4 claimed `raga_match_key('Bowli') = 'govri'` — that is **Gowri**. Bowli is `bovli`; `bhauLi`/`Bauli` is `bauli`. `R__seed_06` already aliases `Bauli` → Bowli, so after deleting the orphan, `bhauLi` **already singleton-resolves**. Extra alias is redundant (harmless). | Merge of the row still required (Tyagaraja `mElukOvayya`, 1 link). Alias optional. |
| **S2** | should | “Same count of `primary_raga_id` references repoint” is false for #5: **3 links, 1 primary**. Two links are malika slots 20–21 of `viSva nAthaM bhajEhaM` (primary stays Sri). | Verify junction count unchanged + NULL-primary baseline; do not equate links to primaries. |
| **S3** | should | Optional P4 “correct Jeevanthikā [48] to sampurna mela-48” is the **wrong target** (that scale is already Divyamaṇi). Stored varja form matches the Wikipedia janya list; asampurna #48 is `S R1 G2 M2 P D3 N3 S` / `S N3 P M2 G2 R1 S`. | P4: leave as-is, or SSP-check the asampurna table — never overwrite with sampurna Divyamaṇi. |
| **S4** | should | Leftover compound row `bhUpALaM - bhauLi` (match_key `bupalambauli`, Tyagaraja `dIna janAvana`, 1 primary) is invisible to the detection query. Bowli lists *Deena Janaavana*. | Out of scope this batch; follow-up. |
| **S5** | nit | `raga_resolution_queue.resolved_raga_id` has no `ON DELETE` (default NO ACTION). Live queue has **0** rows on these keys, so V57 will not trip today; still `UPDATE … SET NULL` or repoint in the merge if any appear. | Preflight in V57. |
| **S6** | nit | `track132_merge_raga` audit `metadata.migration` is hardcoded `'V50'`. Reusing it from V57 logs the wrong id. | Pass-through later, or accept the stale label. |

Confirmed OK: V57 is the next version after V56; keepers exist with the Unicode names in the table; `track132_merge_raga` is idempotent and already guards the ragamalika `order_index` PK; Bhoopālam currently has 0 links so #5 malika slots will not collide; identity/alias/relation FKs are `ON DELETE CASCADE`; DISTINCT `Srimati` [8] and `Jeevantikā` [17] survive; `Kalāvathi` [31] / `Kalāvati` [16] is a valid remaining homonym example for the row-level test.

## Adjudicated merge list (FINAL)

Every keeper is a curated, scale-bearing, mela-parented row. The two musicologically ambiguous cases
were adjudicated by the curator (2026-08-30) and are footnoted.

### MERGE — repoint `krithi_ragas` + `krithis.primary_raga_id`, re-parent children, then delete loser

| # | Loser (orphan) | → Keeper | Keeper mela | Links | Confidence |
|--:|:---|:---|:--:|--:|:---|
| 1 | `nIlAmbari` | `Neelāmbari` | 29 | 11 | High (folds to keeper key) |
| 2 | `ISa manOhari` | `Eeshamanohari` | 28 | 5 | High |
| 3 | `bhUshAvati` | `Bhooshāvathi` | 64 | 4 | High |
| 4 | `mALava SrI` | `Mālavashree` | 22 | 4 | High |
| 5 | `bhUpALaM` | `Bhoopālam` | 8 | 3 | High — 1 primary + 2 malika slots ⁶ |
| 6 | `pUrvi kalyANi` | `Poorvi Kalyāni` | 53 | 2 | High |
| 7 | `Jayanta Sri` | `Jayanthashrī` | 20 | 1 | High |
| 8 | `SrImaNi` | `Shreemani` | 2 | 1 | High — adjudicated ¹ |
| 9 | `Suddha sImantini` | `Shuddha Seemantini` | 8 | 1 | High |
| 10 | `Svara Bhushani` | `Swarabhooshani` | 22 | 1 | High |
| 11 | `dIpakaM` | `Deepakam` | 51 | 1 | High |
| 12 | `gIrvANi` | `Geervāni` | 43 | 1 | High |
| 13 | `gIta priyA` | `Geethapriyā` | 63 | 1 | High |
| 14 | `jIvantikA` | `Jeevanthikā` | 48 | 1 | High — adjudicated ² |
| 15 | `rAga cUDAmaNi` | `Rāgachoodāmani` | 32 | 1 | High ³ |
| 16 | `sarasvati` | `Saraswathi` | 64 | 1 | High |
| 17 | `tanukIrti` | `Tanukeerti` | 6 | 1 | High |
| 18 | `vijaya SrI` | `Vijayashree` | 47 | 1 | High |
| 19 | `bhauLi` | `Bowli` | 15 | 1 | High — alias already covered ⁴ |
| 20 | `Shreemati` | `Shreemani` | 2 | 0 | High — ghost dup, seeded ⁵ |

**42 krithi links** move (41 + `bhauLi`). **Not** 42 primaries: #5 contributes 3 junction rows but
only 1 `primary_raga_id`. Rows 1–18 are orphan import artifacts; #19–#20 are related but
structurally different (see footnotes).

### DISTINCT — must survive (do NOT merge)

- **`Srimati` (mela 8, Hanumatodi)** — a real Bhashanga vakra janya, `S G2 R1 G2 M1 P D1 P D2 N2 S
  (ANya: D2)` / `S N2 D1 P M1 P M1 G2 R1 S`. It is *not* the mela-2 ghost; §0h B3 stands. Protect it.
- **`Jeevantikā` (mela 17, Sūryakāntam, M1 janya)** — the modern M1 raga, distinct from the Mela-48
  Divyamaṇi `Jeevanthikā` that #14 merges into.
- Every DISTINCT pair from TRACK-132 §0c remains distinct.

### Footnotes (adjudication)

1. **#8 `SrImaNi` → `Shreemani`.** Curator ruling (2026-08-30): `Shreemani` is the correct Ratnāngi
   (mela 2) janya. Its scale is `S R1 G1 P D1 S` / `S N2 D1 P G1 R1 S` (audava–shadava, M varja both
   ways). Tyagaraja's `EmandunE vicitramunu` (the orphan's one krithi) belongs here.
2. **#14 `jIvantikA` → `Jeevanthikā [48]`.** Curator ruling: the name is a dual-identity collision
   between the Dikshitar/Asampurna-Mela tradition (Mela 48 Divyamaṇi, Prati-Madhyamam raganga) and the
   modern Govindacharya system (Mela 17 Sūryakāntam janya). Dikshitar's `bRhadISa kaTAkshENa` (the
   orphan's krithi) is the **Mela-48** raga → keeper `Jeevanthikā [48]`. The Mela-17 `Jeevantikā`
   stays distinct.
3. **#15 `rAga cUDAmaNi` → `Rāgachoodāmani [32]`.** Distinct from `Nāgachooḍāmani` (§0c); the orphan is
   the ITRANS twin of *Rāgachoodāmani*, confirmed by fold key.
4. **#19 `bhauLi` → `Bowli`.** Not a fold-equivalent twin: `raga_match_key('bhauLi') = 'bauli'` ≠
   `raga_match_key('Bowli') = 'bovli'` (not `govri` — that is Gowri). It is invisible to the
   detection query (own-key match). The existing `Bauli` alias on Bowli already owns identity key
   `(bauli, 15)`, so after the orphan row is deleted a re-import of `bhauLi` **already singleton-
   resolves**. A second `bhauLi` alias is optional. The merge of the row is still required
   (Tyagaraja `mElukOvayya`).
5. **#20 `Shreemati` → `Shreemani`.** Curator ruling: `Shreemati [2]` is a *ghost duplicate* of
   `Shreemani [2]` — byte-identical scale, a `na`↔`ta` copyist substitution off the "Shrimati"
   honorific. Unlike #1–#19 it is a **seeded row** (in `R__seed_04`), not an import orphan, and its
   fold key `srimati` equals **Srimati [8]**, not the keeper (`srimani`). The alias is a **safety
   rail**: without it, `resolveRaga('Shreemati')` unique-hits Srimati [8] (silent wrong raga);
   with it, `(srimati, 2)` and `(srimati, 8)` are a D1 homonym set and the name **stays
   ambiguous**. It will never unique-resolve. **This merge forces a frozen-suite *row* edit and a
   V50 apply-function replace — see §Rider edits.** This *refines* TRACK-132 B3 (`Shreemati` ≠
   `Srimati`); it does not reverse it — Srimati [8] stays.
6. **#5 links vs primary.** `sadAcalESvaraM` is the one primary. The other two links are
   `order_index` 20 and 21 of the Chaturdaśa rāgamālika `viSva nAthaM bhajEhaM` (primary remains
   Sri). Bhoopālam currently has 0 links, so the `order_index` collision guard will not fire.

## Intent
**Status:** Accepted
**Accepted by:** Sangeetha Grantha Team (Seshadri)
**Accepted at:** 2026-08-30

### Problem

TRACK-136 did its job: leftover duplicate ragas no longer mint, they queue as *ambiguous*. The live
DB still holds the leftovers themselves — 18 ITRANS/Harvard-Kyoto orphan rows (no scale, mela
sentinel `0`) sitting beside a curated Wikipedia-form twin of the same identity, plus `bhauLi`
(alias-key collision) and a seeded ghost `Shreemati [2]`. Forty-two `krithi_ragas` links sit on those
losers. Every re-import of those spellings hits the queue even though a curator has already ruled
each pair is the same raga.

This is TRACK-132 residue the identity key cannot delete by itself: the orphans key as
`(match_key, 0)` and the keepers as `(match_key, mela)`, so they coexist. The resolver is correct to
refuse a silent pick; a human merge has to move the links and drop the loser.

The merge list above is **FINAL** (curator 2026-08-30; validation review v1.1.0). Two names stay
homonyms after the merge, by D1, and that is intended: `jIvantikA` (Mela-17 vs Mela-48) and
`Shreemati` (Shreemani [2] vs Srimati [8]).

### Proposed outcome

- The 20 adjudicated losers are gone. Their 42 junction links (and the primaries that actually point
  at them — not 42) sit on the keepers. Detection query returns **0** rows.
- A re-import of #1–#13 and #15–#19 **resolves**. #14 and #20 stay **`ambiguous`** (never a unique
  hit on the wrong twin). `Srimati [8]` and `Jeevantikā [17]` still exist.
- `krithi_ragas` count stays 1259; NULL `primary_raga_id` stays 0; `viSva nAthaM bhajEhaM` still
  34/1/34/34 after #5.
- Fresh `make db-reset` works: `Shreemati` is not re-seeded, and the V50 DISTINCT assert that
  required that row is replaced in V57 (V50 itself is not edited).
- No change to `resolveRaga`, identity keys, or the queue — only the data those already handle.

### Affected users and systems

- **Curators / import operators** — fewer false `ambiguous` queue rows on the next Trinity re-import.
- **Flyway** — `V57__cleanup_orphan_twin_ragas.sql`; `R__seed_04` (drop `Shreemati`); `R__seed_06`
  (`Shreemati`→Shreemani alias as a safety rail).
- **DAL tests / docs** — row-level `RagaIdentityTrack136Test` homonym example; ADR-017 / TRACK-136
  AC4 wording. Fold-level V51 tests stay.
- **Not affected** — Kotlin resolver, Python worker, admin UI, identity schema.

### Constraints

- Flyway only (ADR-013). Never edit committed V50; `CREATE OR REPLACE` the apply function in V57.
- Reuse `track132_merge_raga`; exact `name =` for losers (`bhUpALaM`, not `bhUpālaM`).
- Every merge already writes `AUDIT_LOG`. Preflight `raga_resolution_queue.resolved_raga_id` (NO
  ACTION FK) before delete.
- Lakshana: do not merge `Srimati [8]`, `Jeevantikā [17]`, or any TRACK-132 §0c DISTINCT pair.
  #20 *refines* TRACK-132 B3; it does not reverse it.
- `#20` alias is a safety rail so `Shreemati` does not unique-hit Srimati [8]. Do not change
  `resolveRaga` to force a unique pick.
- Test row edits need `SANGITA_ALLOW_TEST_EDITS=1`. Prove with `make db-reset`, not only incremental
  migrate.
- Do not implement until **Plan Status is Accepted**.

### Open questions

1. **P4 `Jeevanthikā [48]` scale** — **Closed at Intent accept:** leave the stored Wikipedia-janya
   varja form. Never overwrite with sampurna Divyamaṇi.
2. **Optional `bhauLi` alias** — **Closed at Intent accept:** skip. Merge the row anyway.

## Spec
**Status:** Accepted
**Accepted by:** Sangeetha Grantha Team (Seshadri)
**Accepted at:** 2026-08-30

Data-cleanup only. No identity-architecture change. Lakshana of the 20 keepers was reviewed by
`carnatic-musicologist` (2026-08-30): all MERGE keepers sound; none should flip to DISTINCT; P4
must not “correct” Jeevanthikā [48] to sampurna Divyamaṇi.

### Requirements

**R1 — Merge the FINAL list via `track132_merge_raga`.** V57 calls
`track132_merge_raga(loser, keeper)` for pairs #1–#20 with **byte-exact** `ragas.name` values from
the merge table (`bhUpALaM`, not `bhUpālaM`). Function already: collision-guards
`krithi_ragas.order_index`, repoints junction then `primary_raga_id` then `parent_raga_id`, writes
`AUDIT_LOG`, deletes the loser. Idempotent no-op if either side is missing (fresh reset).

**R2 — Detection query 0 after apply (incremental DB).** The 18 orphan-twin rows are gone.
`Srimati [8]` and `Jeevantikā [17]` still exist. Out-of-scope `bhUpALaM - bhauLi` may remain.

**R3 — Re-import outcomes (bare name, no mela).**

| Spellings | Outcome |
|:---|:---|
| #1–#13, #15–#19 | `Resolved` to the keeper. #19 via existing `Bauli` alias (`bauli` → Bowli). |
| #14 `jIvantikA` | `ambiguous` (Jeevantikā [17] vs Jeevanthikā [48]). Never unique-pick [17]. |
| #20 `Shreemati` | `ambiguous` (Shreemani [2] via alias vs Srimati [8]). **Must not** unique-hit Srimati [8]. |

**R4 — Junction / primary / ragamalika invariants.** Live V56 baselines: `krithi_ragas` = 1259,
NULL `primary_raga_id` = 0, `krithis_without_raga` = 0. After merge those three counts are
unchanged. Do not require 42 primaries to move (#5 is 3 links / 1 primary). After #5,
`viSva nAthaM bhajEhaM` remains 34 rows, min index 1, max 34, 34 distinct indices (domain-model
§6.2; TRACK-132 §4). Bhoopālam currently has 0 links, so the `order_index` collision guard must
not fire.

**R5 — Seed + reset.** Drop the `Shreemati` insert from `R__seed_04` (Sri/Shree pattern). Fresh
`make db-reset` must succeed. V57 `CREATE OR REPLACE`s `track132_apply_raga_merges` so
`R__seed_05` no longer `track132_assert_distinct('Shreemati','Srimati')`. Committed V50 is not
edited.

**R6 — #20 alias safety rail.** `R__seed_06` inserts transliteration alias `Shreemati` on
`Shreemani` **after** the loser row is gone (V57 delete on live DB; seed-drop on reset). Identity
key `(srimati, 2)` then belongs to Shreemani; `(srimati, 8)` stays Srimati. Inserting the alias
while `Shreemati [2]` still exists must not happen (identity-key collision). No `bhauLi` alias
(Intent Q2 closed).

**R7 — Riders, not resolver changes.** Re-point the **row-level**
`RagaIdentityTrack136Test` homonym query to `Kalāvathi [31]` / `Kalāvati [16]`. Keep V51 and
`matchKey("Shreemati") == matchKey("Srimati")` (function-level). Update ADR-017 / TRACK-136 AC4
wording to cite `Kalāvathi`/`Kalāvati` as the surviving **row** example. `SANGITA_ALLOW_TEST_EDITS=1`.
Do not change `resolveRaga`, identity triggers, or queue semantics.

**R8 — DISTINCT.** Do not merge `Srimati [8]`, `Jeevantikā [17]`, `Nāgachooḍāmani`, or any
TRACK-132 §0c DISTINCT pair. #20 refines TRACK-132 B3; it does not reverse it.

**R9 — Proof.** `make db-reset`; `MigrationsFromScratchTest`; `make test` / `make test-integration`
/ `make test-frontend`. Re-run detection query and a `resolveRaga` dry-run of the 20 spellings
against R3.

### Design

**Where it lives**

| Piece | File / mechanism |
|:---|:---|
| Merges + apply-function replace + queue preflight | `database/migrations/V57__cleanup_orphan_twin_ragas.sql` (next after V56) |
| Drop seeded ghost | `database/migrations/R__seed_04_raga_reference.sql` — remove the `Shreemati` `INSERT` |
| Safety-rail alias | `database/migrations/R__seed_06_merge_loser_aliases.sql` — `Shreemati` on `Shreemani` |
| Row-level homonym test | `modules/backend/dal/src/test/.../RagaIdentityTrack136Test.kt` |
| AC4 wording | ADR-017 + TRACK-136 AC4 (example only) |

**V57 body (indicative)**

1. `UPDATE raga_resolution_queue SET resolved_raga_id = NULL WHERE resolved_raga_id IN (loser ids)`
   (or repoint to keeper) — FK has no `ON DELETE` (S5). Live queue is empty on these keys; still
   preflight.
2. `CREATE OR REPLACE FUNCTION track132_apply_raga_merges()` copying V50’s body minus
   `track132_assert_distinct('Shreemati','Srimati')`. Other DISTINCT asserts stay.
3. `SELECT track132_merge_raga(...)` for the 20 pairs in table order. #8 then #20 both target
   `Shreemani` — two calls, fine.
4. No DDL. No Exposed table change. Audit rows come from `track132_merge_raga` (metadata
   `migration` stays `'V50'` unless a later nit passes a label — S6, accept stale).

**Repeatable ordering (live vs reset)**

- **Incremental (live V56):** V57 runs, orphans exist, merges apply, `Shreemati` row deleted;
  checksum-changed `R__seed_04` no longer inserts it; `R__seed_06` adds the alias onto empty
  `(srimati, 2)`.
- **Fresh reset:** V57 runs before seed → merges no-op; `R__seed_04` never creates `Shreemati`;
  `R__seed_05` re-applies TRACK-132 merges with the replaced assert; `R__seed_06` adds the alias.
  Alias must not live in `R__seed_04` (TRACK-136 R__seed_06 lesson).

**Kotlin / API / UI / worker:** unchanged. Callers already go through `resolveRaga`. After V57,
fewer names enqueue; #14 and #20 still do, by D1.

### Flagged concerns

| Concern | Resolution |
|:---|:---|
| Flyway vs ad-hoc SQL | V57 + repeatables only. No live `psql` repair as the shipped fix. |
| Audit | `track132_merge_raga` already inserts `AUDIT_LOG`. No Kotlin mutation path. |
| Junction omitted | Function `UPDATE`s `krithi_ragas` before delete (`ON DELETE RESTRICT`). |
| SET NULL trap on `primary_raga_id` | Function updates FK before delete. R4 watches NULL-primary count. |
| §6.2 ragamalika collapse | #5 moves two malika *slots* onto Bhoopālam; primary stays Sri; 34-length invariant holds. |
| Lakshana / wrong keeper | Musicologist 2026-08-30: 20 keepers sound. `bRhadISa` → [48] not [17]; `EmandunE` → Shreemani; `mElukOvayya` → Bowli. |
| #20 alias vs unique resolve | Spec requires **ambiguous**, not resolve. Changing `resolveRaga` to unique-pick is out of scope (D1). |
| Identity collision on alias insert | Delete loser (or never seed it) **before** `R__seed_06`. |
| `make db-reset` vs V50 assert | R5: replace apply function in V57; never edit V50. |
| P4 sampurna overwrite | Closed: leave stored scale. |

### Open questions carried forward

None. Intent Q1 (leave Jeevanthikā [48] scale) and Q2 (skip `bhauLi` alias) closed at Intent
accept.

## Plan
**Status:** Accepted
**Accepted by:** Sangeetha Grantha Team (Seshadri)
**Accepted at:** 2026-08-30

One Flyway versioned migration, two repeatable edits, one DAL test rider, two doc wording
patches. No Kotlin resolver, Exposed, API, UI, or worker changes. Implement on
`track-137-orphan-twin-raga-cleanup` after this Plan is Accepted.

### Files that change

| File | Change |
|:---|:---|
| `database/migrations/V57__cleanup_orphan_twin_ragas.sql` | **Create.** Queue preflight; `CREATE OR REPLACE track132_apply_raga_merges()` = V50 body minus `PERFORM track132_assert_distinct('Shreemati', 'Srimati')`; twenty `SELECT track132_merge_raga(...)` with byte-exact names. Ref: ADR-016. |
| `database/migrations/R__seed_04_raga_reference.sql` | Delete the `Shreemati` `INSERT` (lines 462–464). Leave a Sri-style comment: ghost merged into `Shreemani` (TRACK-137). Do not touch `Shreemani` or `Srimati` inserts. |
| `database/migrations/R__seed_06_merge_loser_aliases.sql` | Append one `INSERT` mirroring `Bauli`→Bowli: alias `'Shreemati'`, `alias_type='transliteration'`, source `TRACK-137 #20 safety rail`, `FROM ragas WHERE name = 'Shreemani'`, `ON CONFLICT (raga_id, alias) DO NOTHING`. **No** `bhauLi` row. |
| `modules/backend/dal/src/test/kotlin/com/sangita/grantha/backend/dal/integration/RagaIdentityTrack136Test.kt` | Keep `matchKey("Shreemati") == matchKey("Srimati")`. In `homonyms remain distinct identities…`: drop `Shreemati` from the `IN` list and the `shreemati`/`srimati` row asserts; keep `Kalāvathi`/`Kalāvati` (31/16) and `Kanadā`/`Kannada`. Assert `Srimati` still exists at mela 8; assert no `ragas` row named `Shreemati`. `SANGITA_ALLOW_TEST_EDITS=1`. |
| `modules/backend/dal/src/test/kotlin/com/sangita/grantha/backend/dal/integration/RagaOrphanTwinTrack137Test.kt` | **Create** (same env var). Against the migrated seed: `resolveRaga("nIlAmbari")` → Neelāmbari; `resolveRaga("bhauLi")` → Bowli; `resolveRaga("jIvantikA")` kind `ambiguous`; `resolveRaga("Shreemati")` kind `ambiguous` (must not be Srimati); `findByName("Srimati")` and `findByName("Jeevantikā")` non-null; `findByName("Shreemati")` null. |
| `application_documentation/02-architecture/decisions/ADR-017-raga-reference-entity-identity-resolution.md` | AC / Decision wording that treats `Shreemati`/`Srimati` as two **rows**: cite `Kalāvathi`/`Kalāvati` as the surviving row-level homonym. Leave Context fold examples (`srimati` still folds) unless they claim two seeded identities. |
| `conductor/tracks/TRACK-136-raga-identity-alias-resolution.md` | AC4: `Kanadā`/`Kannada` and both `Kalāvati`s remain distinct identities; `Srimati [8]` remains; `Shreemati` is an alias of `Shreemani`, not a separate identity. |
| `conductor/tracks/TRACK-137-orphan-twin-raga-cleanup.md` | Progress log only, as work lands. |

Do **not** edit `V50__merge_duplicate_ragas.sql`, `V51__raga_match_key_function.sql`,
`RagaRepository.kt`, or `R__seed_05`.

### Order of work

1. **Branch** `track-137-orphan-twin-raga-cleanup` (do not commit until asked).
2. **V57** — copy `track132_apply_raga_merges` from V50 verbatim; delete only the
   `Shreemati`/`Srimati` DISTINCT `PERFORM`. Then the 20 merges, `#8` before `#20` (both →
   Shreemani). Exact loser strings:

   ```
   nIlAmbari→Neelāmbari
   ISa manOhari→Eeshamanohari
   bhUshAvati→Bhooshāvathi
   mALava SrI→Mālavashree
   bhUpALaM→Bhoopālam
   pUrvi kalyANi→Poorvi Kalyāni
   Jayanta Sri→Jayanthashrī
   SrImaNi→Shreemani
   Suddha sImantini→Shuddha Seemantini
   Svara Bhushani→Swarabhooshani
   dIpakaM→Deepakam
   gIrvANi→Geervāni
   gIta priyA→Geethapriyā
   jIvantikA→Jeevanthikā
   rAga cUDAmaNi→Rāgachoodāmani
   sarasvati→Saraswathi
   tanukIrti→Tanukeerti
   vijaya SrI→Vijayashree
   bhauLi→Bowli
   Shreemati→Shreemani
   ```

   Queue preflight first:

   ```sql
   UPDATE raga_resolution_queue q
      SET resolved_raga_id = NULL
    WHERE resolved_raga_id IN (
      SELECT id FROM ragas WHERE name IN (/* the 20 losers */)
    );
   ```

3. **Drop `Shreemati` insert** in `R__seed_04` (comment, not a blank hole).
4. **`R__seed_06` alias** for `Shreemati`→Shreemani. Repeatable order is already after V57 and
   `R__seed_05`.
5. **Tests + AC4 docs** with `SANGITA_ALLOW_TEST_EDITS=1`.
6. **Proof** (below) on incremental `make migrate` *and* `make db-reset`.

### Risks

| Risk | Why it is the one | Mitigation |
|:---|:---|:---|
| **Highest: `CREATE OR REPLACE track132_apply_raga_merges` drifts from V50** | `R__seed_05` re-runs this on every reset. Dropping a TRACK-132 merge or the visvanatham assert would regress Phase 0. | Diff the replaced function against V50; the only allowed delta is removing one `PERFORM`. |
| Byte-wrong loser name | `track132_lookup_raga` is `name =`. `bhUpālaM` no-ops and leaves malika slots 20–21 on the orphan. | Copy names from the merge table / this Plan list, not from memory. |
| `#20` alias before delete | Identity key `(srimati, 2)` still owned by the `Shreemati` row → trigger exception. | V57 delete first; alias only in `R__seed_06`. |
| Ragamalika collision on #5 | If Bhoopālam already occupied index 20 or 21, merge raises. | Live DB: Bhoopālam has 0 links. Function fails loud if that changes. |
| Silent mis-resolve of `Shreemati` | Alias omitted → unique hit on Srimati [8]. | R6 alias + TRACK-137 test that `resolveRaga("Shreemati")` is `ambiguous`. |
| `make db-reset` vs leftover DISTINCT assert | If V57 forgets to replace the apply function, reset dies when Hanumatodi exists and Shreemati does not. | Proof includes `make db-reset`, not only incremental migrate. |

**Rejected:** edit V50; put the alias in `R__seed_04`; add a `bhauLi` alias; fold TRACK-137 merges
into `track132_apply_raga_merges`; change `resolveRaga`; “fix” Jeevanthikā [48] to sampurna;
ad-hoc `psql` as the shipped fix.

### Proof

Baselines (live V56, 2026-08-30): `krithi_ragas` = 1259, NULL `primary_raga_id` = 0,
`krithis_without_raga` = 0.

1. Incremental: `make migrate` (V57 + changed R__). Detection query **0** rows. Counts still
   1259 / 0 / 0. `viSva nAthaM bhajEhaM` still 34/1/34/34. `Srimati`, `Jeevantikā` present;
   `Shreemati` ragas-row absent; `bhUpALaM` ragas-row absent.
2. `SANGITA_ALLOW_TEST_EDITS=1 make test` and `make test-integration` (includes
   `MigrationsFromScratchTest`, `RagaIdentityTrack136Test`, new `RagaOrphanTwinTrack137Test`).
   `make test-frontend` (no UI change; still run).
3. `make db-reset` — this is the B4 gate. Repeat the detection/count checks on the fresh DB
   (orphans never existed; `Shreemati` must not reappear; alias must exist; `resolveRaga` outcomes
   per R3 still hold via the integration test against that seed).
4. Do not weaken a failing test. Do not skip `db-reset`.

## Scope

### 1. Merge migration (Flyway)

- `VNN__cleanup_orphan_twin_ragas.sql` (next version after V56 → **V57**) per ADR-013.
- Reuse `track132_merge_raga(loser, keeper)` (defined in V50, present at V57 runtime) for rows #1–#19:
  repoint `krithi_ragas` (by `order_index`) → `krithis.primary_raga_id` → re-parent children → delete.
  It already writes `AUDIT_LOG` and guards the ragamalika `order_index` PK.
- Row #20 (`Shreemati` → `Shreemani`) uses the same call; additionally its `R__seed_04` insert must be
  dropped so a fresh reset does not recreate it (mirror the Sri/Shree pattern, TRACK-136 §Sri).
- **Ordering (the lesson from TRACK-136 R__seed_06):** on a fresh reset the orphan rows do not exist
  (they are import cruft, not seeded), so V57's merges no-op there — safe. Any alias rows (#19, #20)
  that must survive a fresh reset go in a repeatable that runs **after** the merges/seed
  (extend `R__seed_06_merge_loser_aliases.sql`), never in `R__seed_04`.

### 2. Aliases for the non-folding cases (#19 optional, #20 required-as-safety)

- **#20 `Shreemati` → `Shreemani` (required).** Add a `transliteration` alias in `R__seed_06` so
  identity key `(srimati, 2)` points at Shreemani after the seed row is gone. Expected re-import
  outcome is **`ambiguous`** vs Srimati [8], never a unique hit. Order: delete the loser **before**
  inserting the alias, or the identity-key trigger collides with the still-living Shreemati row.
- **#19 `bhauLi` → `Bowli` (optional).** The existing `Bauli` alias already covers match_key `bauli`.
  A `bhauLi` alias is redundant; include it only for spelling fidelity.
- Rows #1–#18 (with `bhUpALaM` spelled exactly) need **no alias**. #14 still will not singleton-
  resolve — `Jeevantikā` [17] remains a same-key homonym.

### 3. Rider edits forced by #20

Merging `Shreemati [2]` away removes one of the two *row-level* mela-qualified-homonym examples:

- **`RagaIdentityTrack136Test` `homonyms remain distinct identities…`** — the query that loads
  `Shreemati`/`Srimati` **rows** must be re-pointed to **`Kalāvathi [31]` / `Kalāvati [16]`**.
  Requires `SANGITA_ALLOW_TEST_EDITS=1`. Keep the *function-level* `matchKey("Shreemati") ==
  matchKey("Srimati")` assertion and the V51 frozen-suite string test — those do not need a row.
- **ADR-017 / TRACK-136 acceptance criterion 4** wording: cite `Kalāvathi`/`Kalāvati` (and
  `Kanadā`/`Kannada`) rather than `Shreemati`/`Srimati` as surviving **identities**. The fold
  `srimati` remains a homonym via the #20 alias + Srimati [8].
- **V50 `track132_apply_raga_merges` DISTINCT assert** (B4): V57 must `CREATE OR REPLACE` that
  function (or the `track132_assert_distinct('Shreemati','Srimati')` call) so `R__seed_05` does
  not fail after the seed insert is dropped. Do not edit committed V50.

### 4. Keeper scale (optional, low priority — do **not** “fix” to sampurna)

- `Jeevanthikā [48]` stores `S M2 P D3 N3 S` / `S N3 P M2 G2 S` (Wikipedia janya list). That is
  not sampurna Divyamaṇi (already its own row) and not asampurna-mela #48 (`S R1 G2 M2 P D3 N3 S` /
  `S N3 P M2 G2 R1 S`). Musicologist: leave as-is, or SSP-check the asampurna table. **Never**
  overwrite with sampurna Divyamaṇi. Does not affect the merge.

## Verification

Baselines captured 2026-08-30 on live V56: `krithi_ragas` = **1259**, NULL `primary_raga_id` = **0**,
`krithis_without_raga` = **0**.

- Re-run the orphan-twin detection query → **0 rows** (`Srimati [8]` and `Jeevantikā [17]` still
  present; `bhUpALaM - bhauLi` is out of scope and will still appear only if a broader query is used).
- Re-import dry-run of the 20 spellings:
  - **#1–#13, #15–#19** → **Resolved** (0 unknown). #19 via existing `Bauli` alias.
  - **#14 `jIvantikA`** → **ambiguous** (Jeevantikā [17] vs Jeevanthikā [48]).
  - **#20 `Shreemati`** → **ambiguous** (Shreemani [2] via alias vs Srimati [8]); must **not** unique-
    resolve to Srimati [8].
- **Total `krithi_ragas` row count unchanged** (1259) — 42 links move, none lost or duplicated.
- **`krithis` with NULL `primary_raga_id` count unchanged** (0). Do not expect 42 primaries to move
  (#5 is 3 links / 1 primary).
- `krithis_without_raga` stays 0; `viSva nAthaM bhajEhaM` still 34 / min 1 / max 34 / 34 distinct
  indices after #5 (TRACK-132 §4).
- `make test` / `make test-integration` / `make test-frontend` green (incl. the re-pointed row-level
  `RagaIdentityTrack136Test`); `MigrationsFromScratchTest` proves V57 applies from scratch.
- `make db-reset` from scratch, not only incrementally — this is what catches B4.

## Out of scope

- Any change to the TRACK-136 identity architecture, `resolveRaga`, or the queue — this track only
  supplies data the resolver then handles.
- General "delete ragas with no scale" cleanup — 816+ curated reference rows are legitimately
  scaleless/orphaned; this migration is driven by the explicit adjudicated pair list only.
- Compound leftover `bhUpALaM - bhauLi` / Tyagaraja `dIna janAvana` (S4) — follow-up, not this batch.
- Changing `resolveRaga` to unique-resolve `Shreemati` or `jIvantikA` without a mela — that would
  violate ADR-017 D1.

## Implementation Plan
- [x] P0a Intent drafted (2026-08-30).
- [x] P0b Intent Accepted (2026-08-30, Seshadri).
- [x] P0c Spec Accepted (2026-08-30, Seshadri). Plan drafted — **stop** until Plan Status is Accepted.
- [x] P0d Plan Accepted (2026-08-30, Seshadri). Implementation started on `track-137-orphan-twin-raga-cleanup`.
- [x] P1 V57 + drop `Shreemati` from `R__seed_04` (after Plan accept)
- [x] P2 `R__seed_06` `Shreemati`→Shreemani alias only
- [x] P3 Row-level identity test + TRACK-137 resolve test + AC4 wording (`SANGITA_ALLOW_TEST_EDITS=1`)
- [x] P4 Jeevanthikā scale — cancelled (Intent Q1: leave stored form)
- [x] P5 Proof: incremental migrate + `make db-reset` + test suites

## Progress Log
- **2026-08-30**: Track drafted from the TRACK-136 re-import dry-run finding. 18 orphan twins detected
  (~41 links) plus `bhauLi` (alias-collision case) and the `Shreemati [2]` ghost duplicate. Two
  ambiguous cases (`jIvantikA`, `SrImaNi`/`Shreemati`) adjudicated by the curator: `jIvantikA` →
  `Jeevanthikā [48]` (Dikshitar Asampurna-Mela raga), `SrImaNi` → `Shreemani [2]`, `Shreemati [2]` is a
  ghost dup of `Shreemani [2]`, `Srimati [8]` is a distinct Bhashanga vakra janya and is protected.
  Merge list FINAL. **Validation review (v1.1.0):** B1–B5 / S1–S6 folded in (wrong #5 spelling,
  #14/#20 remain homonym-ambiguous, V50 DISTINCT assert rider, missing Intent/Spec/Plan, Bowli key
  is `bovli` not `govri`). Lakshana of all 20 keepers confirmed sound.
- **2026-08-30**: Spec Accepted (Seshadri). Plan drafted (Draft). Highest risk: `CREATE OR REPLACE`
  of `track132_apply_raga_merges` drifting from V50. Awaiting Plan accept before implementation.
- **2026-08-30**: Plan Accepted (Seshadri). Implementation: V57 (apply-function replace + 20 merges),
  `R__seed_04` drop of `Shreemati`, `R__seed_06` safety-rail alias, identity-test rider,
  `RagaOrphanTwinTrack137Test`, ADR-017 / TRACK-136 AC4 wording.
- **2026-08-30**: Proof green. Incremental `make migrate` to V57: 20 merges (42 links), detection 0,
  counts 1259/0/0, visvanatham 34/1/34/34. `make test` + `make test-integration` + Vitest unit suite
  green (`RagaOrphanTwinTrack137Test` 4/4). `make db-reset` applies through V57; R__seed_05 no longer
  asserts Shreemati/Srimati DISTINCT; Shreemati row not re-seeded; alias present.
