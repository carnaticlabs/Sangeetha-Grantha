| Metadata | Value |
|:---|:---|
| **Status** | Catalogue repairs applied — V64 backfill embedding rebuild still outstanding (7,396 embeddings) |
| **Last Updated** | 2026-09-26 |
| **Author** | Sangeetha Grantha Team |

# TRACK-144 Handover & Operational Guide

> [!NOTE]
> TRACK-144 catalogue repairs are applied. All 343 previously Unknown tālas are resolved, and the tāla taxonomy is 19 canonical tālas plus sentinel Unknown at 0. Applied migrations V61–V64 stay at their recorded checksums. Additive `V65` invalidates the embedding cohort V64 missed, records the Catusra Ekam audit as an observation rather than a reconstructed update, and removes Latin pallavi text that V61 can write into non-Latin variants. Search repair is not finished: the 7,396 V64 backfill embeddings are still stale, and V61’s URL-cleanup coverage remains unproven because that audit has a null entity id.

## 1. Applied Catalogue State & Architectural Decisions

- **Full Tāla Resolution:** All 343 previously Unknown tālas are resolved with 100% source-backed evidence (from Karnatik.com, Shivkumar Kalyanaraman archives, P. P. Narayanaswami's Dikshitar compilation, and V. Govindan's *Thyagaraja Vaibhavam*). 1,226 / 1,226 compositions now have an established tāla across 19 canonical Carnatic tālas (+ sentinel `Unknown` at 0).
- **Canonical Tāla Taxonomy:** Corrupted and non-canonical variants have been harmonized:
  - `Isra Capu` / `Isra Chapu` (29 compositions) → canonical `Misra Capu`.
  - `Ad` (Tyagaraja's *kana kana rucirA*) → canonical `Adi`.
  - Generic `Ekam` and spelling variant `Caturasra Ekam` → canonical `Catusra Ekam`.
  - Dikshitar Nottuswara meter `English` → canonical `Catusra Ekam`.
- **Raga and Metadata Repairs (`V61`):**
  - Tyagaraja's *Marugelaraa* was re-associated with *Jayanthashrī* and given its missing pallavi. The applied SQL selects that krithi by title only. The pallavi insert and the lyrics prefix run for every lyric variant of that title. There is no composer predicate and no `script = 'latin'` predicate. This database has one Tyagaraja row and one Latin variant, so no non-Latin text was overwritten here. A populated database with another script, or with more than one row of that title, needs the preflight below before migrate. `V65` removes the known inserted Latin pallavi from non-Latin variants and audits the rows it reads.
  - *Sri Narada Nada* (Kanada) corrected from *Adi* to *Rupaka*.
  - *Bhoopālam* and *Bowli* decoupled into distinct raga identities.
  - Extraneous parenthetical inline HTTP/HTTPS URLs stripped from sahitya sections and variants.
  - *Sumadyuti*–*Simhendramadhyamam* nomenclature equivalence registered per SSP.
  - Search path permanently locked on `strip_diacritics` and `raga_match_key` (`search_path = public, pg_temp`).
- **Additive Corrective Migration (`V64`):**
  - **Migration Immutability:** Applied migrations `V61` (checksum `9680159`) and `V63` (checksum `1714889224`) are preserved exactly as applied.
  - **Catusra Ekam audit:** `V64` inserts an `UPDATE` whose before-image hardcodes `beat_count` and `anga_structure` as null, including when the row was already populated and V64 changed nothing. That diff is not a captured before/after. `V65` leaves the historical row in place and adds an `OBSERVE` audit whose current row is read from `talas` and whose prior values are recorded as unknown.
  - **Composition Identity Safeguards:** Enforces strict identity safeguards (`title`, `composer`, `primary_raga_id`) against current database rows, failing closed if any composition identity mismatches.
  - **Latin Incipit Verification:** All 343 compositions have verified opening Latin lyrics verified against stored text in `krithi_lyric_sections` or `krithi_lyric_variants.lyrics`, including exact matches for *Videmu Seyavae* (`viDemu`), *Enaati Nomu* (charanam opening), *Mitri Bhaagyamae* (anupallavi opening), and *Dvaitamu Sukhamaa* (anupallavi opening).
  - **Embedding Stale Invalidation:** V63 copied `search_documents.content_hash` onto `document_embeddings` without regenerating vectors. `V64` marks the 343 backfill compositions only (7,396 embeddings on this database). `V65` marks the remaining identifiable cohort: the V63 taxonomy merges (Isra Capu/Chapu, Ad, Ekam/Caturasra Ekam, English), read from their captured krithi audits, plus V61's *Marugelaraa*, *Sri Narada Nada*, and *dIna janAvana* when each title matches one krithi. On this database that is 37 compositions and 1,177 embeddings, including *nidhi cAla sukhamA* (19), *kana kana rucirA* (61), and *cintaya citta* (7). Each invalidated embedding records the content hash it had and the stale marker written in its place. V61's parenthetical URL cleanup is one audit with a null entity id, so those lyric rows are not reconstructed into this cohort.
- **Search Documents Index vs Domain Auditing:**
  - `search_documents` synchronization is a derived full-text and semantic search index cache, not a core domain entity, so index syncs do not write individual `audit_log` entries. Domain mutations to `krithis`, `talas`, and `krithi_source_evidence` remain fully audited.
- **Script Availability Contract:**
  - The mobile reader shows script selection chips only when multiple lyric variants exist. For compositions with only 1 Latin variant (e.g. *Marugelaraa* and *Sri Narada Nada*), the script choice remains absent. We strictly do not invent, synthesize, or hallucinate non-Latin scripts where none exist in the corpus.
- **Unbounded Raga Browsing:**
  - `SearchPresenter.kt` un-caps raga pagination (`pageCap(ExploreCategory.Ragas) = null`), allowing rasikas to browse the entire 1,014+ raga directory rather than being cut off at 360 rows.

---

## 2. Upgrade Path for Persistent & Development Environments

Upgrades follow standard additive Flyway migration mechanics without requiring checksum overrides or manual schema history deletions:

### Preflight before the first apply of V61
On a populated database that has not yet applied V61, run the read-only preflight. It does not change the catalogue. If *Marugelaraa* has a non-Latin variant, write a snapshot first. That file is the captured pre-migration text. V61 will still insert the Latin pallavi; V65 removes that inserted text from non-Latin variants. Ambiguous titles, or a sole *Marugelaraa* by a composer other than Tyagaraja, stop the preflight because V61 selects by title alone.

```bash
uv run python tools/krithi-extract-enrich-worker/scripts/track144_v61_preflight.py
uv run python tools/krithi-extract-enrich-worker/scripts/track144_v61_preflight.py \
  --snapshot output/track-144/marugelara-non-latin.json
```

After migrate, compare the snapshot with the restored rows:

```bash
uv run python tools/krithi-extract-enrich-worker/scripts/track144_v61_preflight.py \
  --check-snapshot output/track-144/marugelara-non-latin.json
```

### Persistent Environments (Production / Staging / Test)
Run standard Flyway migrations:
```bash
make migrate
```
Flyway validates applied migrations through `V64` against their recorded checksums and applies `V65__track144_embedding_cohort_audit_and_script_recovery.sql`.

### Forced Embedding Rebuild
`V64` left 7,396 backfill embeddings stale. `V65` adds the taxonomy and V61 title-repair cohort. Rebuild the V65 cohort on its own, or rebuild every stale embedding:

```bash
# V65 cohort only (taxonomy merges and the three V61 title repairs)
uv run python tools/krithi-extract-enrich-worker/scripts/rebuild_track144_embeddings.py --dry-run --v65-cohort
uv run python tools/krithi-extract-enrich-worker/scripts/rebuild_track144_embeddings.py --v65-cohort

# Every embedding still marked stale, including the 343 backfill compositions
uv run python tools/krithi-extract-enrich-worker/scripts/rebuild_track144_embeddings.py
uv run python tools/krithi-extract-enrich-worker/scripts/embed_catalogue.py --stale-only
```

### Clean Reset (Optional for Fresh Dev Instances)
```bash
make db-reset
make migrate
```

---

## 3. Audit Compliance & Historical Limitations

- **Captured diffs:**
  - `V63` krithi tāla updates, including the taxonomy merges, store the row read before the update and the row written after it. Tāla deletes store the deleted row.
  - `V64`'s Catusra Ekam `UPDATE` does not. Its before-image is the literal null/null pair in the migration, and the insert runs even when no update occurs. `V65` adds an `OBSERVE` row for that audit. The live subject is `01a0dd8f-6d99-7f69-b7b1-f9ac30bb4728`. Prior `beat_count` and `anga_structure` stay unknown.
  - `V65` embedding invalidation stores each embedding's content hash before and after the stale marker. It does not copy the vector into the audit.
  - When `V65` repairs a non-Latin *Marugelaraa* variant, the audit before-image is the contaminated row it locked, and the after-image is the row after the known Latin prefix or pallavi section is removed.
- **Not reconstructable from V61:**
  - The Marugelaraa pallavi insert, the lyrics prefix, the raga and tāla updates, and the URL cleanup were written as reason text. The URL cleanup has a null entity id. Those historical values are not filled in after the fact.
- **Historical Reason-Only Audits:**
  - Nine historical reason-only audit entries exist in `audit_log` (ids starting with `01a0d403...` and `01a0dd3e...`), created during initial execution runs of `V61`.
  - These historical rows contain `{"reason": "..."}` without pre-update snapshots.
  - Per audit immutability principles, these entries are preserved as historical records; they cannot and should not be retroactively fabricated with synthetic before-images.

---

## 4. Verification & Health Summary

- **V65 on this database:** 37 compositions, 1,177 embedding hashes invalidated, 0 non-Latin Marugelaraa rows to repair. `idx_audit_entity_time` agrees with a sequential scan (1,878 krithi audits, including all 29 Isra Capu merges). Checksums of V61, V63, and V64 are unchanged.
- **V65 cohort rebuild:** `rebuild_track144_embeddings.py --v65-cohort` generated 1,237 embeddings with 0 failures. The cohort then had 0 stale markers and 0 embedding/document hash mismatches. *nidhi cAla sukhamA* 19/19, *kana kana rucirA* 61/61, and *cintaya citta* 7/7 are current.
- **Still stale:** the 7,396 backfill embeddings marked by V64. Rebuild them with the unfiltered command in section 2.
- **Preflight tests:** `pytest tests/test_track144_v61_preflight.py` — 6 passed. A rolled-back rehearsal inserted a Telugu *Marugelaraa* variant carrying the Latin pallavi; V65 restored `తెలుగు పాఠం` and removed that pallavi section, and left the Latin variant unchanged.
- **Mint guard:** `make mint-guard` clean.
- **Documentation links:** `make check-docs` clean.
- **Agent evaluation hooks:** `make agent-evals` clean (52 tests passed).
