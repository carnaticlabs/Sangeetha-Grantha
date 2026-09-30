| Metadata | Value |
|:---|:---|
| **Status** | Proposed |
| **Version** | 1.4.1 |
| **Last Updated** | 2026-09-26 |
| **Author** | Sangeetha Grantha Team |
| **Document Type** | Feature Specification |

# Feature: Dikshitar Kshetra & Musicological Metadata Enrichment

---

## 1. Executive Summary

This feature specifies the data architecture, Flyway schema evolution (`V66` and `V67`), seed migration maintenance (`R__seed_07` amendment, `R__seed_09` canonical cycle seeding with historical tag purge), and vector embedding context formatting required to support deep musicological search and retrieval across the corpus of **Muttusvāmi Dīkṣitar** (~485 kritis across attested cycles, immediate anchor shrines, and documented gaps like Kanyakumari).

By addressing the foundational phenomenon of **sibling collision**—where kritis belonging to the same cycle, deity family, or pilgrimage complex share general devotional vocabulary and collide in naive dense vector spaces—this feature introduces:
1. Structured spatial and sthala hierarchy on `temples` (`parent_temple_id`, `place_kind`, `mandalam`, `bhuta`, `sthala_vriksha`, `sthala_tirtha`, `nadi_tirtha`, `deity_posture`).
2. Essential composition lakshana on `krithis` (`vibhakti_case`, `vibhakti_stem`, `raga_mudra_kind`, `raga_mudra_phrase`, `yati_pattern`, `is_manipravala`, `occasion_note`).
3. An ordered, role-aware `krithi_cycle_memberships` relation for canonical groups.
4. Schema anchor extensions on `search_documents` across two Flyway steps (`V66` for enum expansion and nullable anchors; `V67` for strict `CASE` check constraints) so macro overview documents (`CYCLE_OVERVIEW`, `KSHETRA_OVERVIEW`) can anchor directly to temples or tags without fabricating dummy krithis.
5. Context Formatter 2.0 with a strict programmatic 60-word micro-clause header budget, deriving section architecture from existing `krithi_sections` rather than duplicating state.
6. Seed remediation amending `R__seed_07` to dismantle unreviewed title-pattern matching, paired with an explicit purge of historical heuristic links and reviewed authoritative mappings in `R__seed_09`.
7. Clear single-writer ownership for recorded gaps: `R__seed_09` seeds the Kanyakumari temple row, while the Python worker macro indexer alone generates, upserts, and embeds the `KSHETRA_OVERVIEW` document.

---

## 2. Musicological Grounding: Subbarama Dikshitar's *Sangita Sampradaya Pradarshini*

As chronicled in the *Vāggēyakāra Caritram* of the *Saṅgīta Sampradāya Pradarśinī* (SSP, 1904, pp. 18–19) and Dr. P.P. Narayanaswami's musicological preface to the Devanāgarī edition (pp. 25–32), Muttusvāmi Dīkṣitar's compositions are characterized by:
1. **Parallel Bhakti & Vibhakti Architecture:** Declining proper noun stems across the eight Sanskrit cases (*aṣṭa-vibhaktis*) in cycles such as the Guruguha Vibhaktis at Tiruttani and Tyagaraja Vibhaktis at Tiruvarur. (Note: Rama Vibhakti is mentioned as a scheme in the SSP preface, but the exact incipit list is withheld; it is explicitly excluded from this initial seed until verified citations are established).
2. **Esoteric Sri Vidya Mapping:** The 11 *Kamalāmbā Navāvaraṇa Kṛtis* at Tiruvarur mapping the 9 concentric enclosures of the Sri Chakra, their presiding Yoginis, and Tantrik mudras.
3. **Rigorous Melodic & Structural Constraints:** The *Gauḷānta* cycle (all ragas ending in the suffix "-gaula"), the *Navagraha* cycle set systematically across the *Śūḷādi Sapta Tāḷams*, and the 72 *Raganga Ragas* illustrating the Venkatamakhin tradition (the 72 Raganga program is out of scope for this cycle seed).
4. **Epigraphical Field Recording of Kshetras:** Sāhityas explicitly naming the *sthala vṛkṣa* (sacred tree), *sthala tīrtha* (temple pushkarini), *nadī tīrtha* (sacred river), and *archa-mūrti* posture (*śayana*, *sthānaka*, *āsīna*, *tāṇḍava*).
5. **Architectural Signatures:** *Madhyama Kāla Sāhitya* (double-tempo passages), *Samaṣṭi Caraṇam* (bipartite compositions lacking separate anupallavis), *Yati* patterns (*gōpuccha*, *srōtōvaha*), and *Maṇipravāḷa* (multilingual lyrics in Sanskrit, Telugu, and Tamil).

---

## 3. The Core Retrieval Problem: Sibling Collision & Semantic Dilution

### 3.1 Sibling Collision
General dense embeddings (e.g., Gemini Embedding 2 / 768-D MRL) rely on shared semantic tokens. When applied to Dīkṣitar's corpus:
* **Sister-Goddess Drift:** Epithets like *Jagadamba*, *Sivakanta*, *Parasakti*, and *Bhakti-mukti-pradayini* are shared across Kamalamba (Tiruvarur), Abhayamba (Mayuram), Balambika (Vaitheeswarankoil), and Meenakshi (Madurai). Dense vectors place them in near-identical clusters.
* **Complex Spatial Conflation:** Tagging every composition in Tiruvarur as `[Kshetra: Tiruvarur]` fails to separate the central Tyagaraja sanctum from the Kamalamba shrine, the Nilotpalamba shrine, the Navagraha mandapa, and the five internal Tiruvarur lingams.
* **Case Ending Insensitivity:** Embedders treat case morphemes (`-ikāyāḥ` vs `-ikāyām`) as grammatical inflection rather than distinct ontological stages in the Sri Chakra ascent.
* **Patronymic Confusion:** *Divākaratanujaṁ* ("son of the Sun") addresses Shani (Saturn), but collides directly with *Sūryamūrtē* (Surya).

### 3.2 The Anti-Dilution Principle (Token Budget)
Copying full puranic essays or 300-word Sri Vidya treatises onto every sibling vector causes **semantic dilution**. If 11 Kamalamba kritis share 80% of their header tokens, their vectors become copies of one another, drowning out the actual sahitya.
* **Single Programmatic Budget Rule:** The metadata header is constrained to **maximum 60 words** (`len(header.split()) <= 60`). The ambiguous character count gate is dropped.
* **Separation of Concerns:** The full purana, sthala legend, and roster of members live on dedicated **Macro Documents** (`CYCLE_OVERVIEW`, `KSHETRA_OVERVIEW`).
* **Preservation of Section Passages:** The 60-word budget applies strictly to the header metadata block. Short section passages (e.g. 2-line charanams) are preserved and never dropped on account of lyric-to-header ratios.

---

## 4. Entity Schema Specifications

### 4.1 Temple Entity Extensions (`temples`)

To transform the flat temple table into a true spatial hierarchy:

| Column | Type | Constraints / Enum | Musicological Purpose | Example |
|:---|:---|:---|:---|:---|
| `parent_temple_id` | `UUID` | `FK -> temples(id) ON DELETE SET NULL` | Sannidhi inside a macro-complex | Kamalamba Sannidhi under Tiruvarur Tyagaraja Complex |
| `place_kind` | `place_kind_enum` | `'LOCALITY', 'COMPLEX', 'SANNIDHI', 'MANDAPAM'` | Spatial granularity (DEFAULT: `LOCALITY`) | Tiruvarur complex is `COMPLEX`; Mayuram town is `LOCALITY` |
| `mandalam` | `mandalam_enum` | `'CHOLA', 'PANDYA', 'TONDAI', 'NADU', 'CHERA', 'KONGU', 'UTTARA'` | Classical 7 pilgrimage provinces | `CHOLA` for Tiruvarur and Keevalur (Kaveri delta); `NADU` for Tiruvannamalai (Pennai basin); `KONGU` stays unassigned in this seed |
| `bhuta` | `bhuta_enum` | `'AKASHA', 'VAYU', 'AGNI', 'PRITHVI', 'APPU'`, Nullable | Elemental Lingam tag (Pancha Bhuta only) | `APPU` for Jambukeswaram; null for Tiruvarur local lingams |
| `sthala_vriksha` | `VARCHAR(64)` | Nullable | Sacred tree named in sahitya | `Sahakara Mango` at Ekamranatha |
| `sthala_tirtha` | `VARCHAR(64)` | Nullable | Holy temple tank (pushkarini) | `Kamalalayam` at Tiruvarur; `Sivaganga` at Kanchi |
| `nadi_tirtha` | `VARCHAR(64)` | Nullable | Sacred river bank distinct from tank | `Kaveri` at Tiruvaiyaru; `Ganga` at Kasi |
| `deity_posture` | `deity_posture_enum`| `'SAYANA', 'STHANAKA', 'ASINA', 'TANDAVA'`, Nullable | Archa-murti posture driving incipit | `SAYANA` (Ranganatha), `TANDAVA` (Nataraja) |

> **Architectural vs. Water Features:** `place_kind` strictly models architectural places (`'LOCALITY', 'COMPLEX', 'SANNIDHI', 'MANDAPAM'`). Sacred tanks (pushkarinis) are modeled via `sthala_tirtha`, avoiding redundant entity inflation. Existing records default to `'LOCALITY'`.

### 4.2 Krithi Entity Extensions (`krithis`)

To capture composition-level lakshana without duplicating shrine facts or existing section data:

| Column | Type | Constraints / Enum | Musicological Purpose | Example |
|:---|:---|:---|:---|:---|
| `vibhakti_case` | `vibhakti_enum` | `'PRATHAMA' .. 'SAMBODHANA', 'SARVA_VIBHAKTI'`, Nullable | Grammatical phase of the composition | `SAPTAMI` on *Sri Kamalambikayam*; `SARVA_VIBHAKTI` on Ghanta kriti |
| `vibhakti_stem` | `VARCHAR(64)` | Nullable | Proper noun declined across the cycle | `'Kamalamba'`, `'Guruguha'`, `'Tyagaraja'` |
| `raga_mudra_kind` | `raga_mudra_enum` | `'SUDDHA', 'SLESHA', 'NONE'`, DEFAULT `'NONE'` | Whether raga name is spoken or concealed | `SLESHA` for *samsarabhityapahe* in Arabhi |
| `raga_mudra_phrase`| `VARCHAR(128)` | Nullable | Exact sahitya phrase containing the mudra | `'kumudakriyā-rāga-nutam'`, `'saṁsārabhītyāpahē'` |
| `yati_pattern` | `yati_pattern_enum` | `'GOPUCCHA', 'SROTOVAHA', 'DAMARU', 'NONE'`, DEFAULT `'NONE'` | Poetic expansion / tapering rhythm | `GOPUCCHA` on *Tyagaraja Yoga Vaibhavam* |
| `is_manipravala` | `BOOLEAN` | `NOT NULL DEFAULT false` | Trilingual lyric (Sanskrit + Telugu + Tamil) | `true` on *Sri Abhayamba* (Mangalam) |
| `occasion_note` | `TEXT` | Nullable | Historical anecdote, miracle, or ethical stance | `'Rain miracle during drought at Ettayapuram'`, `'Vairagya: refuses human court wealth'` |

> **No Section Duplication:** Structural traits already defined in [`krithi_sections`](file:///Users/seshadri/project/sangeetha-grantha/database/migrations/V08__add-samashti-charanam-enum.sql) (`MADHYAMA_KALA`, `SAMASHTI_CHARANAM`, `CHITTASWARAM`) are **not duplicated** as columns on `krithis`. They are derived at index/formatting time from the section rows. `is_manipravala` is retained because language variant records cannot currently express sentence-level trilingual blending.

### 4.3 Cycle Membership Entity (`krithi_cycle_memberships`)

Decouples flat tags into ordered, role-aware cycle hierarchies:

```sql
CREATE TYPE cycle_member_role_enum AS ENUM (
    'CORE', 'DHYANA', 'MANGALAM', 'OPTIONAL', 'DISPUTED_CONJECTURE'
);

CREATE TABLE IF NOT EXISTS krithi_cycle_memberships (
    id UUID PRIMARY KEY DEFAULT uuidv7(),
    krithi_id UUID NOT NULL REFERENCES krithis(id) ON DELETE CASCADE,
    tag_id UUID NOT NULL REFERENCES tags(id) ON DELETE CASCADE,
    sequence_order INT NOT NULL DEFAULT 0,
    role cycle_member_role_enum NOT NULL DEFAULT 'CORE',
    axis_value VARCHAR(64),
    discriminative_attributes JSONB NOT NULL DEFAULT '{}'::jsonb,
    created_at TIMESTAMPTZ NOT NULL DEFAULT clock_timestamp(),
    CONSTRAINT uq_krithi_cycle UNIQUE (krithi_id, tag_id)
);
```

> **Strict Attribute Placement & `axis_value` Semantics:**
> - `vibhakti_case` and `vibhakti_stem` live strictly on `krithis`.
> - `axis_value` is populated **only** when the cycle possesses an internal ontological axis distinct from the composition's raga, deity, or grammatical case:
>   - **Kamalamba Navavarana:** The 9 concentric Sri Chakra enclosure names (`Trailokyamohana`, ..., `Sarvanandamaya`) on the 9 `CORE` rows (Avaranas 1..9). For `DHYANA` (*Kamalambike* in Todi) and `MANGALAM` (*Sri Kamalambike* in Sri Raga), `axis_value` is **`NULL`** (they sit outside the 9 geometric enclosures, exactly like pure vibhakti kritis).
>   - **Tiruvarur Pancha Linga:** The 5 local lingams within the complex (`Achalesvara`, `Hatakesvara`, `Valmikesvara`, `Anandesvara`, `Siddhisvara`).
>   - **Nottusvara Sahitya:** The underlying **European air / tune name** (e.g. `'God Save the King'`, `'Castalian Dew'`, `'Galop'`, etc.), giving the embedder the crucial discriminator that separates simple Sankarabharanam tunes from one another.
>   - **Nilotpalamba Gaulanta:** `axis_value` is **`NULL`**. The raga suffix "-gaula" is invariant across all eight members; the whole raga name is already recorded on `krithis.primary_raga_id`; and `krithis.vibhakti_case` serves as the sole structural discriminator across the cycle.
>   - **Pure Vibhakti Cycles (Guruguha, Tyagaraja, Abhayamba):** `axis_value` is **`NULL`**; `krithis.vibhakti_case` provides the structural axis.
> - Cosmic element stays strictly on `temples.bhuta` (not duplicated on cycle membership).
> - Graha stays strictly on `krithis.deity_id`.
> - Esoteric attributes (`yogini`, `mudra`, `avarana` index) live in `discriminative_attributes` JSONB (`{"yogini": "Rahasya", "avarana": 7}`).

#### Repeatable Seed Maintenance & Rules (`R__seed_07` Amendment & `R__seed_09` Creation):
1. **Amend `R__seed_07_canonical_cycle_tags.sql`:**
   - Currently, `R__seed_07` contains `INSERT INTO krithi_tags ... SELECT ... WHERE lower(k.title) LIKE '%kamalamb%' ...` and similar clauses for Navagraha, Abhayamba, Nilotpalamba, and Pancha Bhuta.
   - Merely deleting those `INSERT` statements in `R__seed_07` leaves existing rows in `krithi_tags` on databases that already ran `R__seed_07`.
   - `R__seed_07` must be edited to **strip out all heuristic Dikshitar `krithi_tags` inserts**, retaining only tag dictionary definitions and non-Dikshitar cycle links (Tyagaraja Pancharatnam, Syama Sastri Swarajathis).
2. **Explicit Historical Tag Purge in `R__seed_09`:**
   - To guarantee clean, idempotent migrations on existing databases during `make migrate`, `R__seed_09` begins with an explicit purge of all historical heuristic rows written by `R__seed_07`:
     ```sql
     DELETE FROM krithi_tags 
     WHERE tag_id IN (
         '01920000-0000-7000-8000-000000000001', -- pancha-bhuta-sthala
         '01920000-0000-7000-8000-000000000002', -- kamalamba-navavarnam
         '01920000-0000-7000-8000-000000000003', -- navagraha-krithis
         '01920000-0000-7000-8000-000000000005', -- abhayamba-vibhakti
         '01920000-0000-7000-8000-000000000006'  -- nilotpalamba-vibhakti
     ) AND source = 'canonical_cycle';
     ```
3. **Insert Missing Tags in `R__seed_09`:**
   - The `tags` table enforces `CHECK (category IN ('BHAVA', 'FESTIVAL', 'PHILOSOPHY', 'KSHETRA', 'STOTRA_STYLE', 'NAYIKA_BHAVA', 'OTHER'))`.
   - `R__seed_09` must insert the 5 missing cycle tags with `category = 'STOTRA_STYLE'` (matching the convention in `R__seed_07`):
     - `guruguha-vibhakti` (Guruguha Vibhakti Krithis)
     - `tyagaraja-vibhakti` (Tyagaraja Vibhakti Krithis)
     - `tiruvarur-panchalinga` (Tiruvarur Pancha Linga Krithis)
     - `shodasa-ganapati` (Shodasa Ganapati Krithis)
     - `nottusvara-sahitya` (Nottusvara Sahitya Compositions)
4. **Reviewed Mapping Policy in `R__seed_09`:**
   - Memberships in `R__seed_09` must be mapped via explicit, reviewed kriti UUIDs or exact catalog identifiers into both `krithi_tags` and `krithi_cycle_memberships`. The fragile wildcard title matching patterns from `R__seed_07` must not remain the authority.
5. **Cycle Specifications:**
   - **Kamalamba Navavarana:** 11 members: `DHYANA` (0, *Kamalambike* in Todi, `axis_value = NULL`), `CORE` (1–9, mapping 9 Chakras in `axis_value` and Yoginis in JSONB), `MANGALAM` (10, *Sri Kamalambike* in Sri Raga, `axis_value = NULL`). Enclosure 6 is *Kamalambikayastava* (Punnagavarali, `SHASHTHI`, chakra `Sarvarakshakara`, yogini `Nigarbha`). Enclosure 5 is the Bhairavi kriti in `PANCHAMI`, sung *Sri Kamalambayah param nahi re* and often filed as *Sri Kamalambikayah*. Enclosure 7 is *Sri Kamalambikayam* (Sahana, `SAPTAMI`, chakra `Sarvarogahara`, yogini `Rahasya`). Geometric enclosure shapes (antardaśāra, aṣṭakoṇa, trikoṇa, bindu) are exegetical only and are not stored.
   - **Abhayamba Vibhakti:** 8 `CORE` vibhakti members (`axis_value = NULL`), plus 2 `OPTIONAL`: *Sadashraye* (Dhyana invocation, `axis_value = NULL`) and *Sri Abhayamba* (Manipravala Mangalam, `axis_value = NULL`).
   - **Navagraha:** 7 `CORE` members (Surya through Shani set to Suladi talas); 2 `DISPUTED_CONJECTURE` members (*Smaramyaham sada Rahum* and *Mahasuram Ketumaham*).
   - **Pancha Bhuta Sthala:** 5 `CORE` elemental lingams across 5 regional towns (`axis_value = NULL`).
   - **Tiruvarur Pancha Linga:** 5 `CORE` local lingams within the Tiruvarur temple complex (`axis_value` = local lingam name).
   - **Nilotpalamba Gaulanta:** 8 `CORE` members with raga names ending in "-gaula" (`axis_value = NULL`).
   - **Tyagaraja Vibhakti & Guruguha Vibhakti:** 8 `CORE` members across 8 Sanskrit cases (`axis_value = NULL`).
   - **Open & Attested Sets:**
     - *Shodasa Ganapati:* Attested temple compositions (Vatapi, Maha, Ucchishta, Vallabha, etc.) as an open set (~27).
     - *Nottusvara Sahitya:* Attested European-air tunes (`axis_value` = tune name, e.g. `'God Save the King'`).
     - *Anecdotes & Vairagya:* Maintained via `krithis.occasion_note`, **not** an artificial ordered cycle.
6. **In-Scope Negative Evidence / Gap Temple (Single-Writer Principle):**
   - **Kanyakumari (Bhagavati Amman Temple):** Seeded in `temples` only (`place_kind = 'COMPLEX'`, `mandalam = 'PANDYA'`) with zero associated kritis.
   - **Single Writer Rule:** `R__seed_09` does **not** insert into `search_documents`. The worker macro indexer exclusively owns generating, upserting, and vectorizing the `KSHETRA_OVERVIEW` search document recording the documented gap, preventing unique constraint race conditions.
7. **Explicitly Excluded from this Seed:**
   - *Rama Vibhakti:* The SSP preface names the scheme but withholds the list; excluded until citations are sourced.
   - *72 Raganga Ragas:* Excluded from this cycle seed pass.
   - *Partial Cycles (Balambika, Meenakshi):* Excluded from this seed pass.
   - *Unbounded ~75-Kshetra Gazetteer:* Only the immediate anchor shrines of attested cycles and the Kanyakumari gap temple are seeded.

---

## 5. Storage Architecture for Macro Overview Documents & Migration DDL Split

In [Migration V58](file:///Users/seshadri/project/sangeetha-grantha/database/migrations/V58__semantic_search_pgvector.sql), `search_documents.krithi_id` is `NOT NULL`. Consequently, a cycle-level or temple-level overview (especially for temples with recorded gaps like Kanyakumari) cannot be stored natively.

### 5.1 PostgreSQL DDL Constraint & Migration Split (`V66` and `V67`)
In PostgreSQL, `ALTER TYPE ... ADD VALUE` cannot be followed by a constraint that references the new enum values in the same migration transaction. Attempting to add `'CYCLE_OVERVIEW'` and `'KSHETRA_OVERVIEW'` to `search_document_kind_enum` and immediately add `chk_search_doc_anchor` in the same migration fails with:
`ERROR: unsafe use of new value "CYCLE_OVERVIEW" of enum type search_document_kind_enum`.

Therefore, the migration is split into two distinct Flyway steps:
1. **`V66__dikshitar_musicological_enrichment.sql`:**
   - Creates all new domain types.
   - Extends `temples` and `krithis`.
   - Creates `krithi_cycle_memberships`.
   - Drops `NOT NULL` on `search_documents.krithi_id`, adds nullable `temple_id` and `tag_id`, and updates `uq_search_doc_identity`.
   - Appends `'CYCLE_OVERVIEW'` and `'KSHETRA_OVERVIEW'` to `search_document_kind_enum`.
   - **Does NOT reference the new enum values in any constraint.**
2. **`V67__search_document_anchor_constraints.sql`:**
   - Executed after `V66` has committed the new enum values.
   - Adds the strict `chk_search_doc_anchor` constraint using a `CASE` statement.

### 5.2 Search DAL Join Note:
`KrithiSearchRepository.kt` currently executes:
```sql
JOIN search_documents d ON e.document_id = d.id
JOIN krithis k ON d.krithi_id = k.id
```
Because macro documents (`CYCLE_OVERVIEW`, `KSHETRA_OVERVIEW`) have `krithi_id = NULL`, they will not be returned by that composition API query. In this track, macro documents are indexed, vectorized, and evaluated directly in `evaluate_retrieval.py` against `search_documents`. Live search repository query extensions to surface hybrid composition/macro results are left unchanged in this track and reserved for a downstream consumer track.

---

## 6. Vector Context Formatter 2.0 Specifications

The python worker's [`context_formatter.py`](file:///Users/seshadri/project/sangeetha-grantha/tools/krithi-extract-enrich-worker/src/embeddings/context_formatter.py) constructs `search_documents.indexed_content` enforcing strict token budget rules:

```text
[Composition: <title_diacritic> / <title_ascii>] [Composer: <composer>]
[Musical Form: <form>] [Structure: <derived_structure_clause>]
[Raga: <raga_diacritic> / <raga_ascii>] [Tala: <tala>]
[Kshetra: <canonical_kshetra> / <vernacular_alias>] [Sannidhi: <shrine_path>]
[Mandalam: <mandalam>] [Water / Tree: <theertham_and_vriksham>]
[Cycle: <cycle_name>; Position: <pos>; Role: <role>]
[Axis: <axis_specific_clause>] [Grammar: <vibhakti_clause>]
[Lakshana: <raga_mudra_phrase> | <yati_pattern>] [Anecdote: <occasion_note>]
Text:
<cleaned_sahitya>
```

### Context Clauses for Specific Cycles:
* **Kamalamba Navavarana:**
  * For Core Avaranas (1..9): `[Cycle: Kamalamba Navavarnam; Avarana: <seq>; Chakra: <axis_value>; Yogini: <yogini>] [Grammar: <vibhakti_case>]`
  * For Dhyana (0): `[Cycle: Kamalamba Navavarnam; Position: Dhyana Invocation; Role: DHYANA] [Grammar: Sambodhana]`
  * For Mangalam (10): `[Cycle: Kamalamba Navavarnam; Position: Mangalam Benediction; Role: MANGALAM]`
* **Nottusvara Sahitya:**
  * `[Cycle: Nottusvara Sahitya; Tune: <axis_value>] [Lakshana: Western March Air / Shankarabharanam Scale]`
* **Tiruvarur Pancha Linga:**
  * `[Cycle: Tiruvarur Pancha Linga; Sannidhi: <axis_value>]`

### Word Budget Guardrail:
* Discriminative metadata header must remain under **60 words** (`len(header.split()) <= 60`).
* Short section passages (e.g. 2-line Charanam) are preserved and never rejected based on header proportion.

---

## 7. Migration DDL Specifications

### 7.1 `V66__dikshitar_musicological_enrichment.sql`

```sql
SET search_path TO public;

-- 1. Enums
CREATE TYPE mandalam_enum AS ENUM ('CHOLA', 'PANDYA', 'TONDAI', 'NADU', 'CHERA', 'KONGU', 'UTTARA');
CREATE TYPE place_kind_enum AS ENUM ('LOCALITY', 'COMPLEX', 'SANNIDHI', 'MANDAPAM');
CREATE TYPE cycle_member_role_enum AS ENUM ('CORE', 'DHYANA', 'MANGALAM', 'OPTIONAL', 'DISPUTED_CONJECTURE');
CREATE TYPE vibhakti_enum AS ENUM ('PRATHAMA', 'DVITIYA', 'TRITIYA', 'CHATURTHI', 'PANCHAMI', 'SHASHTHI', 'SAPTAMI', 'SAMBODHANA', 'SARVA_VIBHAKTI');
CREATE TYPE raga_mudra_enum AS ENUM ('SUDDHA', 'SLESHA', 'NONE');
CREATE TYPE yati_pattern_enum AS ENUM ('GOPUCCHA', 'SROTOVAHA', 'DAMARU', 'NONE');
CREATE TYPE deity_posture_enum AS ENUM ('SAYANA', 'STHANAKA', 'ASINA', 'TANDAVA');
CREATE TYPE bhuta_enum AS ENUM ('AKASHA', 'VAYU', 'AGNI', 'PRITHVI', 'APPU');

-- 2. Temples Table Extensions
ALTER TABLE temples
    ADD COLUMN IF NOT EXISTS parent_temple_id UUID REFERENCES temples(id) ON DELETE SET NULL,
    ADD COLUMN IF NOT EXISTS place_kind place_kind_enum NOT NULL DEFAULT 'LOCALITY',
    ADD COLUMN IF NOT EXISTS mandalam mandalam_enum,
    ADD COLUMN IF NOT EXISTS bhuta bhuta_enum,
    ADD COLUMN IF NOT EXISTS sthala_vriksha VARCHAR(64),
    ADD COLUMN IF NOT EXISTS sthala_tirtha VARCHAR(64),
    ADD COLUMN IF NOT EXISTS nadi_tirtha VARCHAR(64),
    ADD COLUMN IF NOT EXISTS deity_posture deity_posture_enum;

-- 3. Krithis Table Extensions
ALTER TABLE krithis
    ADD COLUMN IF NOT EXISTS vibhakti_case vibhakti_enum,
    ADD COLUMN IF NOT EXISTS vibhakti_stem VARCHAR(64),
    ADD COLUMN IF NOT EXISTS raga_mudra_kind raga_mudra_enum NOT NULL DEFAULT 'NONE',
    ADD COLUMN IF NOT EXISTS raga_mudra_phrase VARCHAR(128),
    ADD COLUMN IF NOT EXISTS yati_pattern yati_pattern_enum NOT NULL DEFAULT 'NONE',
    ADD COLUMN IF NOT EXISTS is_manipravala BOOLEAN NOT NULL DEFAULT false,
    ADD COLUMN IF NOT EXISTS occasion_note TEXT;

-- 4. Cycle Memberships Table
CREATE TABLE IF NOT EXISTS krithi_cycle_memberships (
    id UUID PRIMARY KEY DEFAULT uuidv7(),
    krithi_id UUID NOT NULL REFERENCES krithis(id) ON DELETE CASCADE,
    tag_id UUID NOT NULL REFERENCES tags(id) ON DELETE CASCADE,
    sequence_order INT NOT NULL DEFAULT 0,
    role cycle_member_role_enum NOT NULL DEFAULT 'CORE',
    axis_value VARCHAR(64),
    discriminative_attributes JSONB NOT NULL DEFAULT '{}'::jsonb,
    created_at TIMESTAMPTZ NOT NULL DEFAULT clock_timestamp(),
    CONSTRAINT uq_krithi_cycle UNIQUE (krithi_id, tag_id)
);

CREATE INDEX IF NOT EXISTS idx_krithi_cycle_attributes ON krithi_cycle_memberships USING gin (discriminative_attributes);

-- 5. Search Documents Schema Anchor Extensions (No Constraints Yet)
ALTER TABLE search_documents 
    ALTER COLUMN krithi_id DROP NOT NULL,
    ADD COLUMN IF NOT EXISTS temple_id UUID REFERENCES temples(id) ON DELETE CASCADE,
    ADD COLUMN IF NOT EXISTS tag_id UUID REFERENCES tags(id) ON DELETE CASCADE;

ALTER TABLE search_documents 
    DROP CONSTRAINT IF EXISTS uq_search_doc_identity;

ALTER TABLE search_documents 
    ADD CONSTRAINT uq_search_doc_identity UNIQUE NULLS NOT DISTINCT (
        krithi_id, temple_id, tag_id, section_id, variant_id, document_kind, source_chunk_index
    );

-- Add enum values without referencing them in constraints within V66
ALTER TYPE search_document_kind_enum ADD VALUE IF NOT EXISTS 'CYCLE_OVERVIEW';
ALTER TYPE search_document_kind_enum ADD VALUE IF NOT EXISTS 'KSHETRA_OVERVIEW';
```

### 7.2 `V67__search_document_anchor_constraints.sql`

```sql
SET search_path TO public;

-- Strict CASE check constraint enforcing mutually exclusive document anchors
ALTER TABLE search_documents 
    ADD CONSTRAINT chk_search_doc_anchor CHECK (
        CASE 
            WHEN document_kind IN ('COMPOSITION_OVERVIEW', 'SECTION_PASSAGE', 'COMMENTARY_LAKSHANA', 'MANUSCRIPT_FACSIMILE') THEN
                krithi_id IS NOT NULL AND temple_id IS NULL AND tag_id IS NULL
            WHEN document_kind = 'CYCLE_OVERVIEW' THEN
                tag_id IS NOT NULL AND krithi_id IS NULL AND temple_id IS NULL AND section_id IS NULL AND variant_id IS NULL
            WHEN document_kind = 'KSHETRA_OVERVIEW' THEN
                temple_id IS NOT NULL AND krithi_id IS NULL AND tag_id IS NULL AND section_id IS NULL AND variant_id IS NULL
            ELSE FALSE
        END
    );
```

---

## 8. Retrieval Benchmark Evaluation Probes

The test suite in [`evaluate_retrieval.py`](file:///Users/seshadri/project/sangeetha-grantha/tools/krithi-extract-enrich-worker/scripts/evaluate_retrieval.py) enforces discriminative retrieval across the following target probes directly evaluated against `search_documents`:

| Natural Language Test Query | Top Expected Target | Negative Guardrail | Evaluation Criteria |
|:---|:---|:---|:---|
| *"Dikshitar on the fire lingam"* | *Arunachalanatham* (Tiruvannamalai) | *Ananda Natana Prakasam* or *Sadachalesvaram* | Disambiguates Agni Lingam from Space Lingam & Tiruvarur Lingams |
| *"Sixth enclosure, Kamalamba"* | *Kamalambikayastava* (Punnagavarali) | 5th enclosure in Bhairavi, `PANCHAMI` (sung *Sri Kamalambayah*; often filed as *Sri Kamalambikayah*), and the Sahana 7th enclosure | Isolated by `[Cycle: Kamalamba Navavarnam; Avarana: 6; Chakra: Sarvarakshakara; Yogini: Nigarbha] [Grammar: Shashthi]`. Match the 5th-enclosure sibling by Bhairavi and `PANCHAMI`, not by the title stem alone. |
| *"Gaula-ending ragas on Nilotpalamba"* | Top ranks dominated by the 8 Gaulanta kritis | Generic Gaula kritis like *Sri Tyagarajo* | Multi-hit precision: Set coverage |
| *"Tiruvarur local panchalinga in Nilambari"* | *Siddhisvaraya* (Nilambari) | *Jambupate* or *Chintaya Makanda* | Separates internal Tiruvarur lingam from cosmic Pancha Bhuta |
| *"Shani kriti of Dikshitar"* | *Divakaratanujam* (Yadukulakambhoji) | *Suryamurte* (Saurashtram) | Disambiguated Graha role overrides *Divakara* solar patronymic |
| *"Rahu"* | *Ramamanohari* | Misleading 100% certainty | Retrieved item retains visible `DISPUTED_CONJECTURE` role |
| *"Abhayamba invocation"* | *Sadashraye* (Chamaram) | *Abhayamba Jagadamba* (Kalyani) | Preserves retrieval of invocation lacking "Abhayamba" in title |
| *"Kanyakumari Dikshitar"* | Kanyakumari Kshetra Overview (Recorded Gap) | False positive song from Tiruchendur | Negative evidence match (anchored to seeded gap temple, indexed by worker) |
| *"Door-opening miracle at Keevalur"* | *Akshayalinga Vibho* (Sankarabharanam) | Generic Shiva kritis at Keevalur | Anecdotal sthala mahatmya link |
| *"Western marching tune on Sarasvati"* | *Santatam Pahi Mam* (God Save the King) | Classical Carnatic Sarasvati kritis | Melodic Nottusvara class & tune match (`axis_value = 'God Save the King'`) |
