| Metadata | Value |
|:---|:---|
| **Status** | Complete |
| **Version** | 1.1.0 |
| **Last Updated** | 2026-09-26 |
| **Author** | Sangeetha Grantha Team |
| **Document Type** | Implementation Summary |

# TRACK-144: Rasika Field Feedback & Catalogue Harmonization

## Purpose

Address the Rasika field observations from the 22 September 2026 mobile deployment check across two major streams:
1. **Shared Mobile UI Enhancements:** Improve raga typography, script selection nomenclature, Explore screen headers, reactive favourites management, catalogue network retry handling, Melakarta/Janya raga partitioning, and un-capped raga directory pagination.
2. **Catalogue Data Repairs & Tala Harmonization:** Resolve historical catalogue omissions and corruptions (Marugelara pallavi & Jayanthashrī raga association, Sri Narada Nada Rupaka tāla correction, inline URL sanitization, Bhoopalam/Bowli raga decoupling), eliminate all 343 Unknown tālas through scholarly, verified source evidence, and harmonize the active tāla taxonomy into a pure 19-tāla Carnatic schema. V63 stores captured row diffs for the tāla updates. V61's audits are reason text, and V64's Catusra Ekam before-image was hardcoded; V65 records that limitation instead of inventing the missing values.

---

## 1. Shared Mobile UI & Presentation Changes

The shared Compose UI in `modules/shared/presentation` and native mobile wrappers were enhanced to address field usability feedback:

- **Raga Typography:** Increased raga label prominence on `DiscoveryKrithiCard` and `KrithiReaderScreen` to improve legibility during browsing and active reading.
- **Script Label Nomenclature & Script Presence:** Updated the script selection chip in `KrithiReaderScreen` and `RasikaCopy` so that `LATIN` displays as "English" for rasikas, while preserving the internal ISO `LATIN` script code. Where only 1 Latin variant exists (such as *Marugelaraa* and *Sri Narada Nada*), the script choice remains absent; we strictly avoid inventing or synthesizing unverified non-Latin scripts.
- **Explore Header Simplification:** Removed the redundant eyebrow (`EXPLORE`) from `SearchScreen`, retaining a single, clear "Explore" page heading.
- **Reactive Favourites / Library Refresh:** Refactored `FavouritesScreen` and `SearchPresenter` to immediately reflect bookmarked krithis when the heart icon is tapped, eliminating stale library views without requiring screen reloads.
- **Catalogue Network Resilience:** Enhanced error states across Explore and Reader to provide an explicit retry action when the backend service (`/v2/catalogue`) cannot be reached.
- **Melakarta & Janya Hierarchy Grouping:** Partitioned Explore raga directory into Melakartas (ordered numerically 1–72), Janyas (grouped under their parent Melakarta), and an **Other ragas** category for historical or unclassified scales (such as Sumadyuti), ensuring robust rendering without misclassifying unknown scales as janyas.
- **Uncapped Raga Browsing:** Removed the hard 12-page (360-row) cap on raga directory exploration in `SearchPresenter.kt` (`pageCap(ExploreCategory.Ragas) = null`), allowing rasikas to paginate through the full corpus of 1,014+ ragas past Jyothishmathi.
- **Debug Endpoints:** Added internal debug API endpoint configuration helpers for Android and iOS development builds.

---

## 2. Catalogue Data Repairs & Evidence Backfill

### Catalogue Integrity & Metadata Corrections (`V61`)
- **Marugelara (Jayanthashrī):** Re-associated the stored *Marugelaraa* row with *Jayanthashrī* and restored its missing pallavi. The applied `V61` statements select by title only and write the Latin pallavi into every lyric variant. They are not limited to Tyagaraja or to `script = 'latin'`. This catalogue has one Tyagaraja Latin variant. `V65` is the recovery for a database that already received that Latin text on a non-Latin variant, and `scripts/track144_v61_preflight.py` is the check to run before V61 is applied elsewhere.
- **Sri Narada Nada (Kanada):** Corrected tāla from *Adi* to *Rupaka*, verified against authoritative notation treatises.
- **Raga Decoupling:** Decoupled *Bhoopalam* and *Bowli*, re-establishing them as distinct musical identities in the raga catalogue.
- **Sahitya Sanitization:** Stripped extraneous inline HTTP/HTTPS source URLs embedded in lyric variants and sections.
- **Sumadyuti Relationship:** Registered the historical nomenclature relation between *Sumadyuti* and *Simhendramadhyamam* citing the *Sangita Sampradaya Pradarsini* (SSP).
- **Stored Generated Column Function Security:** Permanently set `search_path = public, pg_temp` on `raga_match_key` and `strip_diacritics` to ensure safe, immutable execution during pg_restore and migration replays.
- **Auditing:** `V61` recorded reason text, including one URL-cleanup entry with a null entity id. Those rows stay as historical records. Later mutations that `V65` performs read the current row before writing.

### Corpus Source Registration (`V62`)
- Formally registered authoritative import sources in `sources`:
  - `ibiblio.org/guruguha` (Muthuswami Dikshitar English compilation by P. P. Narayanaswami)
  - `karnatik.com` (Carnatic composition archive)

### Tala Taxonomy Harmonization & Complete Evidence Backfill (`V63` & `V64`)
- **Tala Taxonomy Purification:**
  - **Isra Capu / Isra Chapu → Misra Capu:** Merged 29 compositions with corrupted prefix "Isra" into canonical *Misra Capu* (*Nidhi Cala Sukhama*, *Talli Ninnu*, *Agastisvaram*, etc.). Audited all `krithis` and `krithi_notation_variants` updates with row diffs, and safely deleted spurious `Isra Capu` and `Isra Chapu` entries from `talas`.
  - **Ad → Adi:** Merged Tyagaraja's Pancharatna *kana kana rucirA* (Varāḷi) from truncated `Ad` to canonical *Adi*, audited before/after rows, and deleted `Ad` from `talas`.
  - **Ekam & Caturasra Ekam → Catusra Ekam:** Refined generic `Ekam` (Tyagaraja's *vAsu dEva vara guNa*, Bilahari; Dikshitar's *bAlAmbikayA kaTAkshitOhaM*, Ranjani) and spelling variant `Caturasra Ekam` (Dikshitar's *gaNESa kumAra*, Janjuti) into canonical *Catusra Ekam* per scholarly treatises, with full audit logging.
  - **English Nottuswara → Catusra Ekam:** Refined Muthuswami Dikshitar's Nottuswara *Cintaya Citta* from non-canonical `English` to *Catusra Ekam*, retiring `English` from `talas`.
- **Complete 343-Composition Evidence Backfill with Identity Safeguards:**
  - All 343 previously *Unknown* tālas were researched, cross-referenced, and backfilled with 100% source-backed evidence.
  - Strict identity safeguards (`title`, `composer`, `primary_raga_id`) are enforced against current database rows, failing closed on any identity mismatch.
  - Verified Latin opening lyrics are verified for all 343 records against stored Latin lyric sections or full lyrics, including exact matches for *Videmu Seyavae* (`viDemu`), *Enaati Nomu* (charanam opening), *Mitri Bhaagyamae* (anupallavi opening), and *Dvaitamu Sukhamaa* (anupallavi opening).
  - Explicitly resolved scholarly source discrepancies:
    - *valla kAdanaka* (`09fc370e-ec19-482a-9519-41b0d903125c`): Karnatik.com lists Harikambhoji, but catalogue stores Sankarabharanam; locator cites V. Govindan's *Thyagaraja Vaibhavam* scholarly variation note confirming both traditions use Rupaka.
    - *SrI nArasiMha* (`12058429-320a-4040-b682-78c26399e0ae`): Pedagogical name variant Phalamanjari / Phalaranjani documented.
  - Zero (0) compositions remain with Unknown tāla across the entire 1,226-composition catalogue.
  - Active tāla taxonomy is pure: exactly 19 canonical Carnatic tālas (+ sentinel `Unknown` at count 0).
- **Search & Vector Refresh Contract:**
  - Updated full-text `search_documents.indexed_content` and `content_hash = md5(indexed_content)`. As derived full-text search index data, index synchronizations do not write individual `audit_log` entries.
  - V63 copied new document hashes onto existing vectors. `V64` marks only the 343 backfill compositions stale (7,396 embeddings here). `V65` marks the taxonomy-merge compositions and the three V61 title repairs as well (37 compositions, 1,177 embeddings here). *nidhi cAla sukhamA*, *kana kana rucirA*, and *cintaya citta* are in that second set. `rebuild_track144_embeddings.py --v65-cohort` rebuilds it. The unfiltered rebuild still covers the backfill embeddings `V64` already marked.

---

## 3. Migration Architecture & Upgrade Path (ADR-013)

Applied versioned migrations stay immutable. Later versions add the missing cohort, the audit observation, and the non-Latin recovery:

| Migration File | Description |
|:---|:---|
| `database/migrations/V61__catalogue_raga_and_metadata_repairs.sql` | Applied metadata repairs. Marugelara pallavi text is title-scoped only, not composer- or script-scoped. Checksum `9680159` is unchanged. |
| `database/migrations/V62__register_corpus_import_sources.sql` | Registration of `ibiblio.org/guruguha` and `karnatik.com` sources |
| `database/migrations/V63__canonical_tala_harmonization_and_backfill.sql` | Applied baseline: taxonomy harmonization, 343-tāla backfill, search document text update |
| `database/migrations/V64__track144_catalogue_audit_and_vector_refresh_repair.sql` | Applied correction for the 343-composition checks and their embedding invalidation. Its Catusra Ekam before-image is hardcoded null/null. Checksum `1348837057` is unchanged. |
| `database/migrations/V65__track144_embedding_cohort_audit_and_script_recovery.sql` | Reindexes `idx_audit_entity_time`, observes the fabricated tāla audit, recovers non-Latin Marugelaraa rows, and invalidates the taxonomy plus V61 title-repair embeddings |

### Upgrade Procedure for Persistent Environments
Run standard Flyway migrations:
```bash
make migrate
```
This preserves applied migrations `V61` through `V64` intact and applies `V65`. Before the first apply of `V61` on a populated multilingual database, run `scripts/track144_v61_preflight.py`.

### Forced Embedding Rebuild
To regenerate the 7,396 invalidated embeddings using Gemini Embedding 2:
```bash
# Dry run to preview the rebuild plan
uv run python tools/krithi-extract-enrich-worker/scripts/rebuild_track144_embeddings.py --dry-run

# Rebuild stale embeddings
uv run python tools/krithi-extract-enrich-worker/scripts/rebuild_track144_embeddings.py
```

### Audit Compliance & Historical Notes
- `V63` krithi updates store the row that was read and the row that was written. `V64`'s Catusra Ekam update does not: its before-image is hardcoded null/null. `V65` records that as an observation and does not invent the missing prior values.
- Nine historical reason-only audit entries exist in `audit_log` (ids starting with `01a0d403...` and `01a0dd3e...`), created during initial execution runs of `V61`. These records are preserved as-is per audit immutability principles.
- `search_documents` synchronization is derived full-text search index data and not audited as core entities.

---

## 4. Code Changes Summary

| Area / File | Changes Made |
|:---|:---|
| `modules/shared/presentation/.../components/DiscoveryKrithiCard.kt` | Increased raga label typography size relative to title |
| `modules/shared/presentation/.../components/RagaSequence.kt` | Adjusted typography and spacing for raga sequences |
| `modules/shared/presentation/.../explore/ExploreFilters.kt` | Implemented Melakarta (1–72), Janya, and Other ragas grouping logic |
| `modules/shared/presentation/.../favourites/FavouritesScreen.kt` | Live reactive refresh of bookmarked krithis on mutation |
| `modules/shared/presentation/.../preferences/PreferencesScreen.kt` | UI alignment and typography updates |
| `modules/shared/presentation/.../reader/KrithiReaderScreen.kt` | Script chip label mapped to "English" for LATIN; improved retry action |
| `modules/shared/presentation/.../search/SearchPresenter.kt` | Search state management with reactive favourites, unified Explore, and un-capped raga pagination |
| `modules/shared/presentation/.../search/SearchScreen.kt` | Removed duplicate eyebrow; unified single "Explore" header |
| `modules/shared/presentation/.../RasikaApp.kt` | Top-level navigation and screen state coordination |
| `modules/shared/presentation/.../RasikaCopy.kt` | Centralized user copy strings (script labels, error and retry text) |
| `modules/shared/presentation/.../GroupRagasTest.kt` | Unit tests for Melakarta / Janya / Other raga partitioning |
| `modules/shared/presentation/.../search/ExplorePagingPresenterTest.kt` | Unit tests for unbounded raga pagination beyond default MAX_PAGES |
| `modules/mobile/androidApp/.../MainActivity.kt` | Android container integration for updated shared presentation |
| `modules/mobile/androidApp/.../DebugApiEndpoints.kt` | Android debug endpoint configuration |
| `modules/mobile/androidApp/.../network_security_config.xml` | Local dev network security configuration |
| `modules/mobile/iosApp/.../Info.plist` | iOS bundle configuration |
| `modules/shared/presentation/.../iosMain/.../MainViewController.kt` | iOS Compose host controller integration |
| `modules/shared/presentation/.../iosMain/.../DebugApiEndpoints.kt` | iOS debug endpoint configuration |
| `tools/krithi-extract-enrich-worker/src/metadata_parser.py` | Enhanced regex parsing for tāla prefixes, suffixes, and trailing commas |
| `tools/krithi-extract-enrich-worker/src/tala_audit.py` | Candidate audit tool for missing tāla discovery |
| `tools/krithi-extract-enrich-worker/src/tala_repair.py` | Tala repair verification and migration builder logic |
| `tools/krithi-extract-enrich-worker/scripts/embed_catalogue.py` | Indexer verifying stored document, text hash, and vector hash |
| `tools/krithi-extract-enrich-worker/tests/...` | Regressions and integration test suites for metadata parsing & embeddings |
| `tools/krithi-extract-enrich-worker/tests/test_tala_repair.py` | Validates generated migration SQL structure directly without referencing retired files |
| `database/migrations/V61__catalogue_raga_and_metadata_repairs.sql` | Applied metadata repairs. Pallavi text is selected by title, not by composer or script. Checksum unchanged. |
| `database/migrations/V65__track144_embedding_cohort_audit_and_script_recovery.sql` | Additive repair for the embedding cohort, the fabricated tāla audit, and non-Latin pallavi recovery |
| `tools/krithi-extract-enrich-worker/scripts/track144_v61_preflight.py` | Read-only preflight and snapshot for populated multilingual installs |
| `database/migrations/V62__register_corpus_import_sources.sql` | Flyway canonical migration: corpus import sources |
| `database/migrations/V63__canonical_tala_harmonization_and_backfill.sql` | Flyway canonical migration: taxonomy harmonization, 343-tāla backfill with identity safeguards, audited notation updates |
| `database/data/track144-tala-evidence*.json` | Full audit evidence manifests backing all tāla assignments with complete pallavi and discrepancy resolution |
| `database/audits/audit_unknown_talas.sql` | Read-only SQL audit for unknown tāla inventory verification |
| `tools/generate_v63.py` | Canonical V63 generation and verification utility with strict guards |

---

## 5. Verification & Quality Results

| Verification Check | Result | Details |
|:---|:---|:---|
| `make test-mobile` | **PASS** | Shared JVM tests (mobile-data: 25, presentation: 56) passed with 0 failures |
| `make mobile-android` | **PASS** | `:modules:mobile:androidApp:assembleDebug` completed successfully |
| `make mobile-ios` | **PASS** | xcodebuild Debug-iphonesimulator completed with `** BUILD SUCCEEDED **` |
| Worker tests | **PASS** | 424 passed, 16 skipped; Ruff and mypy passed cleanly |
| Clean Flyway Replay | **PASS** | From-scratch replay on clean database: 63 migrations applied cleanly |
| `make mint-guard` | **PASS** | Clean: zero unauthorized raga inserts |
| `make agent-evals` | **PASS** | All agent evaluation tests passed |
| Tala Completeness | **PASS** | 1,226 / 1,226 krithis have established tālas (0 Unknown) across 19 canonical tālas |
| Identity & Audit Integrity | **PASS with historical limits** | The 343 backfilled records pass title/composer/raga guards. V61 reason-only audits and V64's hardcoded Catusra Ekam before-image are preserved and described by V65 observations, not rewritten. |

---

## 6. Commit Reference

Ref: application_documentation/10-implementations/track-144-rasika-field-feedback.md
