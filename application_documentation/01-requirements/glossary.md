# Sangita Grantha Glossary

| Metadata | Value |
|:---|:---|
| **Status** | Draft |
| **Version** | 0.2 |
| **Last Updated** | 2026-01-20 |
| **Author** | Engineering Team |

# Glossary

**Admin Console**  
Role-based web application used by editors, reviewers, and administrators
to manage the Sangita Grantha catalog.

**Audit Log**  
Immutable table capturing who performed each mutating action, the entity
affected, and context needed for provenance and compliance.

**Composer**  
Canonical Carnatic composer (e.g. Tyagaraja, Muthuswami Dikshitar,
Syama Sastri) with normalized name, life dates, and biographical notes.

**Deity**  
Primary deity addressed in a Krithi (e.g. Shiva, Vishnu, Devi). Used for
thematic search and for linking to temples.

**Import Source**  
External origin of raw data (e.g. karnatik.com, shivkumar.org, PDF
compendia). Tracked via `import_sources` and referenced from
`imported_krithis`.

**Imported Krithi**  
Staging record representing a Krithi as scraped or extracted from an
external source. Reviewed and mapped to canonical `Krithi` records.

**Kriti / Krithi**  
Musical composition in the Carnatic tradition, with sahitya (lyrics),
associated raga, tala, composer, and often deity/temple context. In this
project, `Krithi` is the canonical entity name.

**Krithi Lyric Variant**  
A particular textual form of a Krithi in a specific language/script and
sampradaya, represented in `krithi_lyric_variants`.

**Krithi Section**  
Logical structural units of a Krithi such as pallavi, anupallavi,
charanams, chittaswarams, etc. Modeled in `krithi_sections` and linked
to per-variant text via `krithi_lyric_sections`.

**Melakarta**  
Parent raga in the traditional 72-melakarta system. Some ragas in the
catalog are melakartas; others are janya ragas with an optional
`parent_raga_id`.

**Raga**  
Melodic framework for a Krithi. Each raga entry captures name,
normalized name, optional melakarta number, optional parent raga, and
arohanam/avarohanam.

**Ragamalika**  
Composition that employs multiple ragas in sequence. Modeled by
`krithi_ragas` with ordered entries and optional section mapping.

**Sampradaya / Patantharam**  
Lineage or school-specific rendition of a Krithi. Represented via the
`sampradayas` table and referenced from `krithi_lyric_variants`.

**Script**  
Writing system used for storing lyrics (e.g. Tamil, Telugu, Devanagari,
Kannada, Malayalam, Latin transliteration). Captured using
`script_code_enum`.

**Tag**  
Controlled vocabulary element used to categorize Krithis by bhava,
festival, philosophical theme, kshetra, etc. Implemented via `tags`
and linked to Krithis through `krithi_tags`.

**Tala**  
Rhythmic cycle associated with a Krithi (e.g. Adi, Rupaka). Captured
with name, normalized name, anga structure, and beat count.

**Temple / Kshetram**  
Place of worship or sacred location associated with a Krithi.
Canonical temple metadata lives in `temples`; multilingual names and
aliases live in `temple_names`.

**Workflow State**  
Editorial lifecycle of a Krithi: `draft`, `in_review`, `published`,
`archived`. Only `published` Krithis are shown in public search.

**Musical Form**  
Classification of a composition: `KRITHI` (lyric-focused), `VARNAM`
(notation-centric pedagogical form), or `SWARAJATHI` (notation-centric
form with sahitya). Determines required sections and presence of notation.

**Varnam**  
Pedagogical composition form emphasizing detailed swara notation,
typically used for learning. Requires structured notation variants.

**Swarajathi**  
Composition form with both swara notation and sahitya, requiring
structured notation variants.

**Notation Variant**  
A specific interpretation of notation for a composition (e.g., Lalgudi
bani, SSP notation). Stored in `krithi_notation_variants` with metadata
like tala, kalai, and eduppu. Supports both SWARA and JATHI notation
types.

**Notation Row**  
Individual line of notation within a section, containing swara text,
optional sahitya, and tala markers. Stored in `krithi_notation_rows`
and organized by section and order.

**Kalai**  
Subdivision of tala beats. Used in notation variants to specify the
rhythmic subdivision (default: 1).

**Eduppu**  
Starting offset in beats for a composition or section. Used in notation
variants to indicate where the composition begins within the tala cycle.
