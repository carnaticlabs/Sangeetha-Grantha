| Metadata | Value |
|:---|:---|
| **Status** | Completed |
| **Version** | 1.5.0 |
| **Last Updated** | 2026-09-30 |
| **Author** | Sangeetha Grantha Team |

# Track: Dikshitar Kshetra & Musicological Metadata Enrichment
**ID:** TRACK-145  
**Status:** Completed  
**Owner:** Seshadri  
**Created:** 2026-09-26  
**Updated:** 2026-09-30  

---

## Goal

Implement Flyway schema evolution (`V66` and `V67`), seed migration maintenance (`R__seed_07` amendment, `R__seed_09` canonical cycle seeding with historical tag purge), and Python worker embedding enrichment (`Context Formatter 2.0`) to capture musicological, architectural, and spatial metadata for the corpus of **Muttusvāmi Dīkṣitar** (~485 kritis across attested cycles, immediate anchor shrines, and documented gaps like Kanyakumari), eliminating sibling vector collision and establishing verified retrieval benchmarks.

---

## Context

- **Authoritative Musicological Source:** Subbarāma Dīkṣitar, *Saṅgīta Sampradāya Pradarśinī* (SSP, 1904), Devanāgarī Edition (ed. Dr. P.P. Narayanaswami, guruguha.org), pp. 25–32 (*Group Compositions of Muttusvāmi Dīkṣitar* & *Vāggēyakāra Caritram*).
- **Primary Feature Spec:** [Dikshitar Kshetra & Musicological Metadata Enrichment](../../application_documentation/01-requirements/features/dikshitar-kshetra-musicological-enrichment.md).
- **Design Review Canvas:** `/Users/seshadri/.cursor/projects/Users-seshadri-project-sangeetha-grantha/canvases/track-145-review.canvas.tsx`.
- **Kshetra Readout Canvas:** `/Users/seshadri/.cursor/projects/Users-seshadri-project-sangeetha-grantha/canvases/dikshitar-kshetra-embedding-readout.canvas.tsx`.
- **Existing Vector Substrate:** [TRACK-108](../tracks/TRACK-108-semantic-search.md), [Migration V58](../../database/migrations/V58__semantic_search_pgvector.sql), [Migration V65](../../database/migrations/V65__track144_embedding_cohort_audit_and_script_recovery.sql), and [context_formatter.py](../../tools/krithi-extract-enrich-worker/src/embeddings/context_formatter.py).

---

## Intent
**Status:** Accepted  
**Accepted by:** Seshadri  
**Accepted at:** 2026-09-26  

### Problem

Muttusvāmi Dīkṣitar’s compositions are distinguished by intricate grammatical schemes (*aṣṭa-vibhakti*), esoteric Sri Vidya geometry (the 9 enclosures of the Sri Chakra), strict melodic and talic constraints (*Gauḷānta* ragas, *Śūḷādi Sapta Tāḷas*), and field epigraphy recording sacred tanks, trees, and river banks across his pilgrimage routes.

In our current indexing architecture:
1. **Sibling Collision:** Compositions belonging to the same cycle (e.g. all 11 Kamalamba Navavaranams) receive identical flat tags (`[Thematic Group / Tags: Kamalamba Navavarnam]`) and the same generic town label (`[Kshetra: Tiruvarur]`). Because the Sanskrit sahitya across these hymns shares high-register devotional vocabulary (*Jagadamba*, *Sivakanta*, *Parasakti*), vector embeddings in Gemini Embedding 2 collapse into an undifferentiated cluster, unable to separate the Dhyana, the 7th Avarana, or the Mangalam.
2. **Spatial Conflation:** The sprawling Tiruvarur complex contains the central Tyagaraja sanctum, the Kamalamba shrine, the Nilotpalamba shrine, the Navagraha mandapa, and the five local Tiruvarur lingams. Flat tagging collapses all of these into one spatial coordinate.
3. **Epithet and Patronymic Collisions:** *Divākaratanujaṁ* ("Son of the Sun") addresses Shani (Saturn), but naive vector retrieval collides directly with *Sūryamūrtē* (Surya).
4. **V58 Search Document Anchor Constraint:** In `V58__semantic_search_pgvector.sql`, `search_documents.krithi_id` is `NOT NULL`. Consequently, cycle-level or temple-level overview documents (including negative evidence / recorded gaps like Kanyakumari) cannot be stored natively.
5. **PostgreSQL Enum DDL Limitation:** PostgreSQL does not permit referencing newly added enum labels (`CYCLE_OVERVIEW`, `KSHETRA_OVERVIEW`) within the same migration transaction that added them. Attempting to add values to `search_document_kind_enum` and create `chk_search_doc_anchor` in a single migration causes an immediate DDL abort.
6. **Heuristic Seed Pollution & Persistence in `krithi_tags`:** `R__seed_07` contains heuristic `ILIKE` pattern inserts into `krithi_tags` (e.g. `%kamalamba%`). Merely deleting the `INSERT` statements from `R__seed_07` leaves existing heuristic rows in `krithi_tags` on databases that already executed that migration. A subsequent `make migrate` would leave false tags intact beside reviewed memberships unless an explicit purge runs.
7. **Single Writer Conflict on Gap Documents:** If a database seed inserts a placeholder document into `search_documents` while the Python worker's macro generator also attempts to write it, the unique constraint `uq_search_doc_identity` will cause an insert conflict.

### Proposed Outcome

1. **Schema Evolution (Flyway `V66` and `V67`):**
   - **`V66__dikshitar_musicological_enrichment.sql`:**
     - Add parent-child shrine hierarchy (`parent_temple_id`, `place_kind` with default `'LOCALITY'`, enum values `'LOCALITY', 'COMPLEX', 'SANNIDHI', 'MANDAPAM'`) and sthala features (`mandalam`, `bhuta`, `sthala_vriksha`, `sthala_tirtha`, `nadi_tirtha`, `deity_posture`) to `temples`. Keevalur is `CHOLA` (Kaveri delta); Tiruvannamalai is `NADU` (Pennai basin); `KONGU` stays in enum unassigned in this seed.
     - Add composition lakshanas (`vibhakti_case`, `vibhakti_stem`, `raga_mudra_kind`, `raga_mudra_phrase`, `yati_pattern`, `is_manipravala`, `occasion_note`) to `krithis`. Derive structural sections (`MADHYAMA_KALA`, `SAMASHTI_CHARANAM`, `CHITTASWARAM`) from existing `krithi_sections`.
     - Create `krithi_cycle_memberships` table storing sequence order, 5-role membership (`CORE`, `DHYANA`, `MANGALAM`, `OPTIONAL`, `DISPUTED_CONJECTURE`), and polymorphic axis attributes:
       - **Kamalamba Navavarana:** The 9 Chakra names belong strictly on the 9 `CORE` rows (Avaranas 1..9). `DHYANA` (*Kamalambike* in Todi) and `MANGALAM` (*Sri Kamalambike* in Sri Raga) have `axis_value = NULL` (sitting outside the 9 enclosures).
       - **Tiruvarur Pancha Linga:** The 5 local lingam names (`Achalesvara`, `Hatakesvara`, etc.).
       - **Nottusvara Sahitya:** The underlying European air / tune name (e.g. `'God Save the King'`, `'Castalian Dew'`, etc.).
       - **Gaulanta & Pure Vibhakti Sets:** `axis_value = NULL` (whole raga and `vibhakti_case` already discriminate).
       - Cosmic element stays on `temples.bhuta`, graha stays on `krithis.deity_id`, and esoteric attributes live in `discriminative_attributes` JSONB.
     - Relax `search_documents.krithi_id` to nullable, add `temple_id` and `tag_id` anchors, update `uq_search_doc_identity`, and append enum values `'CYCLE_OVERVIEW'` and `'KSHETRA_OVERVIEW'` to `search_document_kind_enum` without referencing them in constraints.
   - **`V67__search_document_anchor_constraints.sql`:**
     - Add `chk_search_doc_anchor` using a strict `CASE` statement referencing committed enum labels (`'CYCLE_OVERVIEW'`, `'KSHETRA_OVERVIEW'`) so macro documents cannot cross-pollinate with composition, section, or variant anchors.
     - Note: `KrithiSearchRepository.kt` live search API join (`JOIN krithis k ON d.krithi_id = k.id`) remains untouched in this track; macro documents are evaluated directly in `evaluate_retrieval.py` against `search_documents`.
2. **Context Formatter 2.0:**
   - Update `context_formatter.py` to emit crisp, discriminative micro-clauses for individual kritis under a single programmatic check: **maximum 60 words for the metadata header** (`len(header.split()) <= 60`).
   - Format Nottusvara tune names from `axis_value` into the header: `[Cycle: Nottusvara Sahitya; Tune: <axis_value>]`.
   - Implement macro overview indexer to generate and embed `CYCLE_OVERVIEW` and `KSHETRA_OVERVIEW` documents (including the recorded gap temple at Kanyakumari).
3. **Seeding & Backfill (`R__seed_07` Amendment & `R__seed_09` Creation):**
   - **Amend `R__seed_07_canonical_cycle_tags.sql`:** Strip out the heuristic title-pattern inserts into `krithi_tags` for Dikshitar cycles, preventing unreviewed pattern overwrites on rerun.
   - **`R__seed_09_dikshitar_cycle_memberships.sql`:**
     - Execute an explicit purge of historical heuristic links for Dikshitar cycles:
       `DELETE FROM krithi_tags WHERE tag_id IN ('...0001', '...0002', '...0003', '...0005', '...0006') AND source = 'canonical_cycle';`
       ensuring existing databases running `make migrate` are cleanly sanitized.
     - Insert missing cycle tags into `tags` with `category = 'STOTRA_STYLE'`: `guruguha-vibhakti`, `tyagaraja-vibhakti`, `tiruvarur-panchalinga`, `shodasa-ganapati`, and `nottusvara-sahitya`.
     - Seed authoritative cycle memberships for the attested Dikshitar cycles and immediate anchor shrines using curated, reviewed kriti mappings.
     - **Single-Writer Rule for Kanyakumari Gap:** Seed **Kanyakumari (Bhagavati Amman Temple)** (`place_kind = 'COMPLEX'`, `mandalam = 'PANDYA'`) in `temples` only. The worker macro indexer exclusively owns generating and upserting the `KSHETRA_OVERVIEW` row in `search_documents`.
     - Re-embed the Dikshitar cohort using `batch_embed_catalogue.py`.
4. **Retrieval Verification:**
   - Verify precision against multi-hit, negative evidence (including Kanyakumari gap embedded by the macro indexer), the Kamalamba 6th enclosure (*Kamalambikayastava*, Punnagavarali; Chakra `Sarvarakshakara`, Yogini `Nigarbha`, Case `Shashthi`) disambiguated from both the 5th enclosure (*Sri Kamalambayah*, Bhairavi; Case `Panchami`) and the 7th enclosure (*Sri Kamalambikayam*, Sahana; Chakra `Sarvarogahara`, Yogini `Rahasya`, Case `Saptami`), tune matching (*Santatam Pahi Mam* via `axis_value = 'God Save the King'`), and patronymic probes in `evaluate_retrieval.py`. The 5th enclosure is sung *Sri Kamalambayah param nahi re* and is often filed as *Sri Kamalambikayah*; the guardrail keys off Bhairavi and `Panchami`, not the title stem alone.

### Constraints

- Follow ADR-013: All database migrations through Flyway only (`make migrate` / `make db-reset`). Never modify historical migration files (`V01`–`V65`).
- Preserve Exposed entities vs `@Serializable` DTO boundary; wrap all DAL mutations in `DatabaseFactory.dbQuery { }` and write to `AUDIT_LOG`.
- Context Formatter Token Budget: Single programmatic check of maximum 60 words for the metadata header (`len(header.split()) <= 60`). Short section passages must never be rejected because of header ratio.
- Musicological scholarly integrity: Maintain `DISPUTED_CONJECTURE` for Rahu/Ketu per SSP; do not manufacture closed grids for Shodasa Ganapati (open set of ~27) or Nottusvara (39 tunes); Rama Vibhakti is out of scope until sourced with attested citations.
- Implementation boundary: This track owns schema, reference seeds, worker formatter, cohort re-embedding, and retrieval evaluation. Downstream UI search exposure on Curator Web and Rasika Mobile will consume these endpoints in subsequent tracks.

---

## Spec
**Status:** Accepted  
**Accepted by:** Seshadri  
**Accepted at:** 2026-09-26  

### Functional Requirements

1. **Migration `V66__dikshitar_musicological_enrichment.sql`:**
   - Create typed enums: `mandalam_enum` (`'CHOLA', 'PANDYA', 'TONDAI', 'NADU', 'CHERA', 'KONGU', 'UTTARA'`), `place_kind_enum` (`'LOCALITY', 'COMPLEX', 'SANNIDHI', 'MANDAPAM'`), `cycle_member_role_enum` (`'CORE', 'DHYANA', 'MANGALAM', 'OPTIONAL', 'DISPUTED_CONJECTURE'`), `vibhakti_enum` (8 cases + `SARVA_VIBHAKTI`), `raga_mudra_enum`, `yati_pattern_enum`, `deity_posture_enum`, `bhuta_enum`.
   - Extend `temples` table with `parent_temple_id`, `place_kind` (DEFAULT `'LOCALITY'`), `mandalam`, `bhuta`, `sthala_vriksha`, `sthala_tirtha`, `nadi_tirtha`, and `deity_posture`. Keevalur is mapped to `CHOLA`, Tiruvannamalai to `NADU`; `KONGU` remains unassigned in this seed.
   - Extend `krithis` table with `vibhakti_case`, `vibhakti_stem`, `raga_mudra_kind`, `raga_mudra_phrase`, `yati_pattern`, `is_manipravala`, and `occasion_note`.
   - Create table `krithi_cycle_memberships` (id, krithi_id, tag_id, sequence_order, role, axis_value, discriminative_attributes JSONB).
   - Relax `search_documents.krithi_id` nullability, add `temple_id` and `tag_id`, and update `uq_search_doc_identity`.
   - Append `'CYCLE_OVERVIEW'` and `'KSHETRA_OVERVIEW'` to `search_document_kind_enum` via `ALTER TYPE ... ADD VALUE IF NOT EXISTS`. Do NOT reference these values in any constraints within `V66`.
2. **Migration `V67__search_document_anchor_constraints.sql`:**
   - Add strict anchor check constraint `chk_search_doc_anchor` via `CASE` statement:
     - `COMPOSITION_OVERVIEW`, `SECTION_PASSAGE`, `COMMENTARY_LAKSHANA`, `MANUSCRIPT_FACSIMILE`: `krithi_id IS NOT NULL AND temple_id IS NULL AND tag_id IS NULL`
     - `CYCLE_OVERVIEW`: `tag_id IS NOT NULL AND krithi_id IS NULL AND temple_id IS NULL AND section_id IS NULL AND variant_id IS NULL`
     - `KSHETRA_OVERVIEW`: `temple_id IS NOT NULL AND krithi_id IS NULL AND tag_id IS NULL AND section_id IS NULL AND variant_id IS NULL`
     - `ELSE FALSE`
3. **Repeatable Reference Seed Maintenance (`R__seed_07` Amendment & `R__seed_09` Implementation):**
   - Amend `R__seed_07_canonical_cycle_tags.sql`: Remove heuristic `INSERT INTO krithi_tags ... WHERE lower(k.title) LIKE ...` for Dikshitar cycles. Retain non-Dikshitar links (Tyagaraja Pancharatnam, Syama Sastri) and tag catalog rows.
   - In `R__seed_09_dikshitar_cycle_memberships.sql`:
     - Delete stale heuristic links:
       `DELETE FROM krithi_tags WHERE tag_id IN ('01920000-0000-7000-8000-000000000001', '01920000-0000-7000-8000-000000000002', '01920000-0000-7000-8000-000000000003', '01920000-0000-7000-8000-000000000005', '01920000-0000-7000-8000-000000000006') AND source = 'canonical_cycle';`
     - Insert 5 missing tags with `category = 'STOTRA_STYLE'`: `guruguha-vibhakti`, `tyagaraja-vibhakti`, `tiruvarur-panchalinga`, `shodasa-ganapati`, and `nottusvara-sahitya`.
     - Seed cycle memberships using reviewed kriti mappings (curated UUIDs/catalog keys) for:
       1. Kamalamba Navavarana: 11 members (`DHYANA` at 0, `axis_value = NULL`; 9 `CORE` Avaranas with `axis_value` = Chakra name and JSONB Yoginis; `MANGALAM` at 10, `axis_value = NULL`). Enclosure 6 is *Kamalambikayastava* (Punnagavarali, `SHASHTHI`, chakra `Sarvarakshakara`, yogini `Nigarbha`). Enclosure 5 is the Bhairavi kriti in `PANCHAMI`, sung *Sri Kamalambayah* and often filed *Sri Kamalambikayah*. Enclosure 7 is *Sri Kamalambikayam* (Sahana, `SAPTAMI`, chakra `Sarvarogahara`, yogini `Rahasya`). Geometric enclosure shapes are not stored.
       2. Guruguha Vibhakti: 8 `CORE` members at Tiruttani (`axis_value = NULL`)
       3. Tyagaraja Vibhakti: 8 `CORE` members at Tiruvarur (`axis_value = NULL`)
       4. Nilotpalamba Gaulanta Vibhakti: 8 `CORE` members with raga suffix "-gaula" (`axis_value = NULL`; discriminated by `krithis.vibhakti_case`)
       5. Abhayamba Vibhakti: 8 `CORE` members + 2 `OPTIONAL` (*Sadashraye* and *Sri Abhayamba* manipravala mangalam, `axis_value = NULL`)
       6. Navagraha: 7 `CORE` members (Surya..Shani) + 2 `DISPUTED_CONJECTURE` members (Rahu, Ketu)
       7. Pancha Bhuta Sthala: 5 `CORE` elemental lingams across 5 regional towns (`axis_value = NULL`)
       8. Tiruvarur Pancha Linga: 5 `CORE` local sannidhis within Tiruvarur complex (`axis_value` = local lingam name)
       9. Shodasa Ganapati: Attested temple compositions as an open set (~27)
       10. Nottusvara Sahitya: Attested European-air tunes (`axis_value` = tune name, e.g. `'God Save the King'`)
     - Seed **Kanyakumari (Bhagavati Amman Temple)** (`place_kind = 'COMPLEX'`, `mandalam = 'PANDYA'`) in `temples`. Do NOT seed `search_documents` in SQL to preserve single-writer ownership.
     - Note: Rama Vibhakti, 72 Raganga Ragas, partial cycles, and the full ~75-kshetra gazetteer remain out of scope for this seed.
4. **Worker Context Formatter Upgrade:**
   - Update `fetch_krithi_candidates()` in `embed_catalogue.py` and `batch_embed_catalogue.py` to join `krithi_cycle_memberships` and the new temple/krithi columns, deriving section architecture from `krithi_sections`.
   - Implement `format_composition_overview()` 2.0 with discriminative micro-clauses under a strict programmatic check: `len(header.split()) <= 60`. Include `[Cycle: Nottusvara Sahitya; Tune: <axis_value>]` for Nottusvara compositions.
   - Implement macro overview indexer to generate and embed `CYCLE_OVERVIEW` and `KSHETRA_OVERVIEW` search documents anchored to tags or temples (including the Kanyakumari gap temple).
5. **Retrieval Verification Suite:**
   - Add discriminative test probes to `evaluate_retrieval.py` testing:
     - Fire vs space lingam (*Arunachalanatham* vs *Ananda Natana Prakasam*)
     - Kamalamba 6th enclosure (*Kamalambikayastava*, Punnagavarali; Chakra: `Sarvarakshakara`, Yogini: `Nigarbha`, Case: `Shashthi`) disambiguated from both 5th enclosure (*Sri Kamalambayah*, Bhairavi; Case: `Panchami`) and 7th enclosure (*Sri Kamalambikayam*, Sahana; Chakra: `Sarvarogahara`, Yogini: `Rahasya`, Case: `Saptami`). The 5th is sung *Sri Kamalambayah param nahi re* and is often filed as *Sri Kamalambikayah*; match that sibling by Bhairavi and `Panchami`, not by the title stem alone.
     - Shani vs Surya patronymic (*Divakaratanujam* vs *Suryamurte*)
     - Keevalur door opening (*Akshayalinga Vibho*)
     - Nottusvara tune matching (*Santatam Pahi Mam* matched via `axis_value = 'God Save the King'`)
     - Kanyakumari recorded gap overview evaluated directly against `search_documents`.

---

## Plan
**Status:** Accepted  
**Accepted by:** Seshadri  
**Accepted at:** 2026-09-26  

- [x] **Task 1: Database Migrations (`V66`, `V67`) & Reference Seeds (`R__seed_07` amend, `R__seed_09` author)**
  - [x] Author `database/migrations/V66__dikshitar_musicological_enrichment.sql` implementing `temples`, `krithis`, `krithi_cycle_memberships`, `search_documents` anchor relaxation, and enum value additions.
  - [x] Author `database/migrations/V67__search_document_anchor_constraints.sql` implementing strict `CASE` `chk_search_doc_anchor`.
  - [x] Amend `database/migrations/R__seed_07_canonical_cycle_tags.sql` to strip out heuristic Dikshitar cycle linking.
  - [x] Author `database/migrations/R__seed_09_dikshitar_cycle_memberships.sql` purging historical heuristic links, inserting 5 tags with `category = 'STOTRA_STYLE'`, seeding reviewed cycle memberships (with Kamalamba Dhyana/Mangalam `axis_value = NULL`, Nottusvara `axis_value` = tune name), and seeding anchor temples and the Kanyakumari gap temple (temple row only).
  - [x] Run `make migrate` and verify schema integrity on both fresh and existing databases.
- [x] **Task 2: DAL & DTO Layer Alignment (`modules/backend/dal`, `modules/shared/domain`)**
  - [x] Update `KrithiDto` and `TempleDto` with new musicological fields.
  - [x] Update `KrithisTable` and `TemplesTable` in Exposed DAL.
  - [x] Add `KrithiCycleMembershipsTable` and query functions in `KrithiRepository.kt`.
- [x] **Task 3: Worker Context Formatter 2.0 & Macro Document Generator**
  - [x] Update `tools/krithi-extract-enrich-worker/src/embeddings/context_formatter.py` to emit micro-clauses with `len(header.split()) <= 60`, deriving section structure from `krithi_sections` and emitting tune names for Nottusvaras.
  - [x] Update candidate extraction queries in `embed_catalogue.py` and `batch_embed_catalogue.py`.
  - [x] Implement macro overview indexing service to generate, upsert, and vectorize `CYCLE_OVERVIEW` and `KSHETRA_OVERVIEW` search documents (including the Kanyakumari gap overview).
- [x] **Task 4: Cohort Re-embedding & Retrieval Evaluation**
  - [x] Run `batch_embed_catalogue.py` on the Dikshitar cohort (481/481 embedded, 0 failures, 7,145 embeddings generated).
  - [x] Execute the macro document generator to embed cycle overviews (10) and kshetra overviews (94, including Kanyakumari gap overview).
  - [x] Execute `evaluate_retrieval.py` against the discriminative test battery, testing both composition and macro search documents (verified 100% recall on Track 145 battery).
- [x] **Task 5: Documentation & Track Reconciliation**
  - [x] Update `conductor/tracks.md` and feature catalog.
  - [x] Verify clean test runs across DAL and Worker suites.

## Progress Log
- **2026-09-26**: Implementation started. V66/V67 applied on the populated dev database; R__seed_09 seeded 116 reviewed memberships and 21 anchor temples, including the Kanyakumari gap row with no search document. Exposed tables and DTOs aligned. Context formatter, macro indexer, and retrieval evaluation remain.
- **2026-09-30**: Implementation complete. Extended R__seed_09 with 481 Dikshitar kritis (100% temple_id, vibhakti, occasion_note coverage). Context Formatter 2.0 with 60-word budget and macro indexer completed. Thread-local client isolation and retry resilience added to GeminiEmbedder. Re-embedded Dikshitar cohort (481/481 succeeded, 7,145 embeddings created). Macro indexer generated 10 CYCLE_OVERVIEWs and 94 KSHETRA_OVERVIEWs (including Kanyakumari gap). Retrieval benchmarks evaluated in Hybrid (MRR 0.780, Recall@5 87.1%) and Semantic modes (MRR 0.678, Recall@5 77.4%), confirming 100% discriminative separation on Kamalamba 6th vs 5th vs 7th, Shani vs Surya patronymics, Keevalur temple door, Nottusvara British anthem air, and Kanyakumari gap overview. Track completed.
