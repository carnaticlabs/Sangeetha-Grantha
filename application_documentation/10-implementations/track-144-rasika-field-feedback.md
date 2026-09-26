| Metadata | Value |
|:---|:---|
| **Status** | Complete |
| **Version** | 1.0.0 |
| **Last Updated** | 2026-09-26 |
| **Author** | Sangeetha Grantha Team |
| **Document Type** | Implementation Summary |

# TRACK-144: Rasika Field Feedback & Catalogue Harmonization

## Purpose

Address the Rasika field observations from the 22 September 2026 mobile deployment check across two major streams:
1. **Shared Mobile UI Enhancements:** Improve raga typography, script selection nomenclature, Explore screen headers, reactive favourites management, catalogue network retry handling, and Melakarta/Janya raga partitioning.
2. **Catalogue Data Repairs & Tala Harmonization:** Resolve historical catalogue omissions and corruptions (Marugelara pallavi & Jayanthashrī raga association, Sri Narada Nada Rupaka tāla correction, inline URL sanitization, Bhoopalam/Bowli raga decoupling), eliminate all 343 Unknown tālas through scholarly, verified source evidence, and harmonize the active tāla taxonomy into a pure 19-tāla Carnatic schema.

---

## 1. Shared Mobile UI & Presentation Changes

The shared Compose UI in `modules/shared/presentation` and native mobile wrappers were enhanced to address field usability feedback:

- **Raga Typography:** Increased raga label prominence on `DiscoveryKrithiCard` and `KrithiReaderScreen` to improve legibility during browsing and active reading.
- **Script Label Nomenclature:** Updated the script selection chip in `KrithiReaderScreen` and `RasikaCopy` so that `LATIN` displays as "English" for rasikas, while preserving the internal ISO `LATIN` script code.
- **Explore Header Simplification:** Removed the redundant eyebrow (`EXPLORE`) from `SearchScreen`, retaining a single, clear "Explore" page heading.
- **Reactive Favourites / Library Refresh:** Refactored `FavouritesScreen` and `SearchPresenter` to immediately reflect bookmarked krithis when the heart icon is tapped, eliminating stale library views without requiring screen reloads.
- **Catalogue Network Resilience:** Enhanced error states across Explore and Reader to provide an explicit retry action when the backend service (`/v2/catalogue`) cannot be reached.
- **Melakarta & Janya Hierarchy Grouping:** Partitioned Explore raga directory into Melakartas (ordered numerically 1–72), Janyas (grouped under their parent Melakarta), and an **Other ragas** category for historical or unclassified scales (such as Sumadyuti), ensuring robust rendering without misclassifying unknown scales as janyas.
- **Debug Endpoints:** Added internal debug API endpoint configuration helpers for Android and iOS development builds.

---

## 2. Catalogue Data Repairs & Evidence Backfill

### Catalogue Integrity & Metadata Corrections (`V61`)
- **Marugelara (Jayanthashrī):** Re-associated Tyagaraja's *Marugelara* to *Jayanthashrī* (severing the erroneous link to *Jayantasena*) and restored its missing Pallavi section.
- **Sri Narada Nada (Kanada):** Corrected tāla from *Adi* to *Rupaka*, verified against authoritative notation treatises.
- **Raga Decoupling:** Decoupled *Bhoopalam* and *Bowli*, re-establishing them as distinct musical identities in the raga catalogue.
- **Sahitya Sanitization:** Stripped extraneous inline HTTP/HTTPS source URLs embedded in lyric variants and sections.
- **Sumadyuti Relationship:** Registered the historical nomenclature relation between *Sumadyuti* and *Simhendramadhyamam* citing the *Sangita Sampradaya Pradarsini* (SSP).
- **Stored Generated Column Function Security:** Permanently set `search_path = public, pg_temp` on `raga_match_key` and `strip_diacritics` to ensure safe, immutable execution during pg_restore and migration replays.

### Corpus Source Registration (`V62`)
- Formally registered authoritative import sources in `sources`:
  - `ibiblio.org/guruguha` (Muthuswami Dikshitar English compilation by P. P. Narayanaswami)
  - `karnatik.com` (Carnatic composition archive)

### Tala Taxonomy Harmonization & Complete Evidence Backfill (`V63`)
- **Tala Taxonomy Purification:**
  - **Isra Capu / Isra Chapu → Misra Capu:** Merged 29 compositions with corrupted prefix "Isra" into canonical *Misra Capu* (*Nidhi Cala Sukhama*, *Talli Ninnu*, *Agastisvaram*, etc.). Deleted spurious `Isra Capu` and `Isra Chapu` entries from `talas`.
  - **Ad → Adi:** Merged Tyagaraja's Pancharatna *kana kana rucirA* (Varāḷi) from truncated `Ad` to canonical *Adi*. Deleted `Ad` from `talas`.
  - **Ekam & Caturasra Ekam → Catusra Ekam:** Refined generic `Ekam` (Tyagaraja's *vAsu dEva vara guNa*, Bilahari; Dikshitar's *bAlAmbikayA kaTAkshitOhaM*, Ranjani) and spelling variant `Caturasra Ekam` (Dikshitar's *gaNESa kumAra*, Janjuti) into canonical *Catusra Ekam* per scholarly treatises.
  - **English Nottuswara → Catusra Ekam:** Refined Muthuswami Dikshitar's Nottuswara *Cintaya Citta* from non-canonical `English` to *Catusra Ekam*, retiring `English` from `talas`.
- **Complete 343-Composition Evidence Backfill:**
  - All 343 previously *Unknown* tālas were researched, cross-referenced, and backfilled with 100% source-backed evidence from Karnatik.com, the Shivkumar Kalyanaraman archive, and V. Govindan's Thyagaraja Vaibhavam.
  - Comprehensive evidence catalogued in `database/data/track144-tala-evidence-tyagaraja-complete.json` and supporting manifests.
  - Zero (0) compositions remain with Unknown tāla across the entire 1,226-composition catalogue.
  - Active tāla taxonomy is pure: exactly 19 canonical Carnatic tālas (+ sentinel `Unknown` at count 0).
- **Search & Vector Synchronization:**
  - Synchronized all affected full-text `search_documents` and updated their SHA-256 vector embedding hashes to ensure immediate search consistency.

---

## 3. Migration Consolidation (ADR-013)

To maintain an orderly, production-grade Flyway migration history, the iterative step migrations `V61` through `V77` were consolidated into 3 clean, canonical migrations:

| Migration File | Description |
|:---|:---|
| `database/migrations/V61__catalogue_raga_and_metadata_repairs.sql` | Metadata repairs, Marugelara pallavi restore, URL sanitization, `search_path` security fix |
| `database/migrations/V62__register_corpus_import_sources.sql` | Registration of `ibiblio.org/guruguha` and `karnatik.com` sources |
| `database/migrations/V63__canonical_tala_harmonization_and_backfill.sql` | Taxonomy harmonization (Isra Capu, Ad, Ekam, English), complete 343-tāla backfill, search document and embedding hash sync |

Obsolete scratch migrations `V64` through `V77` were safely retired.

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
| `modules/shared/presentation/.../search/SearchPresenter.kt` | Search state management with reactive favourites and unified Explore |
| `modules/shared/presentation/.../search/SearchScreen.kt` | Removed duplicate eyebrow; unified single "Explore" header |
| `modules/shared/presentation/.../RasikaApp.kt` | Top-level navigation and screen state coordination |
| `modules/shared/presentation/.../RasikaCopy.kt` | Centralized user copy strings (script labels, error and retry text) |
| `modules/shared/presentation/.../GroupRagasTest.kt` | Unit tests for Melakarta / Janya / Other raga partitioning |
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
| `database/migrations/V61__catalogue_raga_and_metadata_repairs.sql` | Flyway canonical migration: metadata & raga repairs |
| `database/migrations/V62__register_corpus_import_sources.sql` | Flyway canonical migration: corpus import sources |
| `database/migrations/V63__canonical_tala_harmonization_and_backfill.sql` | Flyway canonical migration: taxonomy harmonization & 343-tāla backfill |
| `database/data/track144-tala-evidence*.json` | Full audit evidence manifests backing all tāla assignments |
| `database/audits/audit_unknown_talas.sql` | Read-only SQL audit for unknown tāla inventory verification |
| `tools/generate_v63.py` | Canonical V63 generation and verification utility |

---

## 5. Verification & Quality Results

| Verification Check | Result | Details |
|:---|:---|:---|
| `make test-mobile` | **PASS** | Shared JVM tests (mobile-data: 25, presentation: 55) passed with 0 failures |
| `make mobile-android` | **PASS** | `:modules:mobile:androidApp:assembleDebug` completed successfully |
| `make mobile-ios` | **PASS** | xcodebuild Debug-iphonesimulator completed with `** BUILD SUCCEEDED **` |
| Worker tests | **PASS** | 424 passed, 16 skipped; Ruff and mypy passed cleanly |
| Clean Flyway Replay | **PASS** | From-scratch replay on clean database: 70 migrations applied in 0.54s |
| `make agent-evals` | **PASS** | All agent evaluation tests passed |
| Tala Completeness | **PASS** | 1,226 / 1,226 krithis have established tālas (0 Unknown) across 19 canonical tālas |
| Search Consistency | **PASS** | 0 search document or embedding hash mismatches |

---

## 6. Commit Reference

Ref: application_documentation/10-implementations/track-144-rasika-field-feedback.md
