| Metadata | Value |
|:---|:---|
| **Status** | Active |
| **Version** | 1.1.0 |
| **Last Updated** | 2026-09-30 |
| **Author** | Sangeetha Grantha Team |
| **Document Type** | Current guide |

# Embedding pipeline and index operations

---

The embedding capability turns canonical composition text and musicological metadata into profile-bound search documents and vectors stored in PostgreSQL. It powers the Curator Console's Hybrid and Semantic modes. It is a separate indexing operation from source extraction and import; new or corrected content is not automatically re-embedded.

For API behavior and ranking, read [Catalogue search](../03-api/search.md). For detailed musicological context and retrieval benchmarks, see [TRACK-145 Implementation Summary](../10-implementations/track-145-dikshitar-kshetra-musicological-metadata.md). This guide covers document construction, indexing tools, activation, freshness, and verification as implemented in the repository.

## 1. What gets embedded

The indexing runners build four current document kinds:

| Document kind | Eligibility and content | Identity & Anchor |
|:---|:---|:---|
| `COMPOSITION_OVERVIEW` | Selected primary/earliest lyric text longer than 10 trimmed characters; formatted overview includes up to 600 characters of that text with rich musicological micro-clauses | Composition-level document (`krithi_id IS NOT NULL`) |
| `SECTION_PASSAGE` | Stored lyric-section text with at least 5 trimmed characters, formatted with section and raga context | Composition + section + lyric variant (`krithi_id`, `section_id`, `variant_id` NOT NULL) |
| `CYCLE_OVERVIEW` | Macro overview synthesizing sequence order, 5-role membership (`CORE`, `DHYANA`, `MANGALAM`, `OPTIONAL`, `DISPUTED_CONJECTURE`), deities, and kshetras across an entire group cycle (e.g., Kamalamba Navavarnam, Navagraha, Nottusvara) | Tag-level macro document (`tag_id IS NOT NULL`, `krithi_id IS NULL`) |
| `KSHETRA_OVERVIEW` | Macro overview synthesizing geographical mandalam, elemental bhuta, sthala vriksha, sacred tirthas, associated compositions, and recorded negative-evidence shrines (e.g., Kanyakumari pilgrimage gap) | Temple-level macro document (`temple_id IS NOT NULL`, `krithi_id IS NULL`) |

The overview lyric selection orders variants by primary flag, then creation time. Section documents preserve each eligible stored variant's language/script context. The schema also defines commentary/manuscript document kinds, but these runners do not implement a manuscript/media ingestion pipeline.

### Context Formatter 2.0

[context_formatter.py](../../tools/krithi-extract-enrich-worker/src/embeddings/context_formatter.py) formats compositions and macro overviews with crisp, discriminative micro-clauses while enforcing a strict programmatic ceiling:
- **Maximum 60 words for the metadata header** (`len(header.split()) <= 60`). This ensures dense, high-signal retrieval without rejecting short section passages.

Micro-clauses emitted by Context Formatter 2.0:
- **Vibhakti Scheme**: `[Vibhakti: <case> (<stem>)]` (e.g., `[Vibhakti: SHASHTHI (kamalAmbikAyA)]`).
- **Sri Chakra Geometry**: `[Cycle: <cycle_name>; Avarana: <num>; Chakra: <chakra_name>; Yogini: <yogini_clan>]` for the 9 core enclosures of the Kamalamba Navavaranam.
- **Western Airs**: `[Cycle: Nottusvara Sahitya; Tune: <tune_name>]` (e.g., `Tune: God Save the King`).
- **Sthala Features**: `[Sthala: <mandalam>, <bhuta>, vriksha: <tree>, tirtha: <waterbody>]`.
- **Architectural Lakshanas**: `[Architecture: MADHYAMA_KALA, SAMASHTI_CHARANAM, CHITTASWARAM]` derived dynamically from `krithi_sections`.

Example of a formatted composition overview:

```text
[Composition: kamalAmbikAyAstava] [Composer: muthuswAmi dIkshitar] [Raga: punnAgavarALi] [Tala: rUpaka] [Deity: kamalAmbA] [Kshetra: tiruvArUr] [Vibhakti: SHASHTHI (kamalAmbikAyA)] [Cycle: Kamalamba Navavarnam; Avarana: 6; Chakra: Sarvarakshakara; Yogini: Nigarbha]
Text:
kamalAmbikAyAstava bhaktOham...
```

Example of a macro cycle overview:

```text
[Cycle: Kamalamba Navavarnam] [Composer: Muthuswami Dikshitar] [Deity: Kamalamba] [Kshetra: Tiruvarur] [Enclosures: 9 Core Avaranas + Dhyana + Mangalam]
Compositions:
- Dhyana: Kamalambike (Todi)
- Avarana 1: Kamalambam Bhajare (Kalyani) - Trailokyamohana Chakra
...
```

This is retrieval context, not generated source material. Original text and indexed text have separate fields.

## 2. Storage contract

The storage contract is governed by versioned Flyway migrations:
- [Migration V58](../../database/migrations/V58__semantic_search_pgvector.sql): Establishes pgvector substrate, `embedding_profiles`, `search_documents`, `document_embeddings` with `vector(768)` HNSW cosine index (`m=16`, `ef_construction=64`), and trigram text index.
- [Migration V66](../../database/migrations/V66__dikshitar_musicological_enrichment.sql): Extends `temples` (place kind, mandalam, bhuta, sacred vriksha/tirthas), `krithis` (vibhakti, mudras, yati, occasion), creates `krithi_cycle_memberships`, adds nullable `temple_id` and `tag_id` anchors on `search_documents`, and appends `CYCLE_OVERVIEW` and `KSHETRA_OVERVIEW` to `search_document_kind_enum`.
- [Migration V67](../../database/migrations/V67__search_document_anchor_constraints.sql): Adds `chk_search_doc_anchor` check constraint via strict `CASE` statement enforcing anchor isolation:
  - `COMPOSITION_OVERVIEW`, `SECTION_PASSAGE`, `COMMENTARY_LAKSHANA`, `MANUSCRIPT_FACSIMILE`: `krithi_id IS NOT NULL AND temple_id IS NULL AND tag_id IS NULL`.
  - `CYCLE_OVERVIEW`: `tag_id IS NOT NULL AND krithi_id IS NULL AND temple_id IS NULL AND section_id IS NULL AND variant_id IS NULL`.
  - `KSHETRA_OVERVIEW`: `temple_id IS NOT NULL AND krithi_id IS NULL AND tag_id IS NULL AND section_id IS NULL AND variant_id IS NULL`.

| Table | Key fields and constraints |
|:---|:---|
| `search_documents` | Composition/section/variant/tag/temple identity; original/indexed content; language/script; content hash; publication flag; anchor constraint `chk_search_doc_anchor` |
| `embedding_profiles` | Unique model/dimensions/task-type combination; active flag |
| `document_embeddings` | Unique document/profile pair; `vector(768)`; embedding content hash |

The code defaults to `gemini-embedding-2` and 768 output dimensions (MRL truncated). Document requests use `RETRIEVAL_DOCUMENT`; user query requests use `RETRIEVAL_QUERY`. The storage validator rejects other dimensions before embedding calls. A new width requires a schema change, not simply a new profile or CLI flag.

## 3. Tools and execution modes

| Tool | Use | Important distinction |
|:---|:---|:---|
| [embed_catalogue.py](../../tools/krithi-extract-enrich-worker/scripts/embed_catalogue.py) | Target one composition, a bounded set, or the whole catalogue | Exposes model, dimensions, database URL, Vertex options and profile activation |
| [batch_embed_catalogue.py](../../tools/krithi-extract-enrich-worker/scripts/batch_embed_catalogue.py) | Process compositions concurrently (`--concurrency <N>`), paced local batches, and write execution reports | Multi-threaded local batch execution with per-composition commit; calls external embeddings per document |
| [macro_indexer.py](../../tools/krithi-extract-enrich-worker/src/embeddings/macro_indexer.py) | Generate and vectorize `CYCLE_OVERVIEW` and `KSHETRA_OVERVIEW` macro documents | Manages atomic transactions (`conn.commit()`) per macro document; single-writer owner of gap overviews |
| [evaluate_retrieval.py](../../tools/krithi-extract-enrich-worker/scripts/evaluate_retrieval.py) | Run the repository's retrieval benchmark suite | Provider-backed evaluation verifying discriminative recall across cycles, elements, tunes, and gaps |
| [catalogue_index.py](../../tools/krithi-extract-enrich-worker/src/embeddings/catalogue_index.py) | Shared profile, dimension, audit, and document-retirement rules | Used by all indexing entry points |

Run tools from `tools/krithi-extract-enrich-worker` after `uv sync --frozen --extra dev`.

## 4. Configure the intended target

The direct runner takes `--db-url`, defaulting to `DATABASE_URL` or the local PostgreSQL connection. The batch runner uses `DATABASE_URL`, falling back to `DB_HOST`, `DB_PORT`, `DB_NAME`, `DB_USER`, and `DB_PASSWORD`.

The Python embedder resolves an explicit key, then `SG_GEMINI_API_KEY`/`GEMINI_API_KEY`, then worker settings and a local-config fallback. Prefer explicit environment configuration for predictable runs. The direct runner also exposes `--vertexai` and `--project-id` for its implemented Vertex/ADC path.

Backend query embedding is wired in [AppModule.kt](../../modules/backend/api/src/main/kotlin/com/sangita/grantha/backend/api/di/AppModule.kt) with the backend Gemini key and the client's default model/dimensions. There is currently no dedicated embedding-model environment override wired there. A CLI `--model` change does not change the running query embedder.

[Runtime configuration](../08-operations/config.md) distinguishes enrichment settings from embedding/query configuration. Normal public catalogue reads do not need an embedding provider.

## 5. Preview before indexing

The following read the selected database and preview indexing work without calling the embedding provider or mutating the database:

```bash
uv run python scripts/embed_catalogue.py --limit 5 --dry-run
uv run python scripts/batch_embed_catalogue.py --max-krithis 5 --dry-run --report-path /tmp/embedding-preview.md
```

A batch preview still writes its local report. It does not replace the latest real execution report in application documentation. Read-only profile lookup means a dry run does not create an active profile on a fresh database.

## 6. Run and maintain an index

For an authorized bounded run:

```bash
uv run python scripts/embed_catalogue.py --limit 5
uv run python scripts/batch_embed_catalogue.py --max-krithis 50 --batch-size 10 --delay 1 --report-path /tmp/embedding-run.md
```

For high-throughput multi-threaded indexing across the full catalogue:

```bash
uv run python scripts/batch_embed_catalogue.py --concurrency 8 --delay 0.05 --report-path /tmp/embedding-full-run.md
```

To index macro cycle and kshetra overviews:

```bash
uv run python -c "
import psycopg2, os
from src.embeddings.macro_indexer import index_cycle_overviews, index_kshetra_overviews
from src.embeddings.client import GeminiEmbeddingClient
conn = psycopg2.connect(os.environ.get('DATABASE_URL', 'postgresql://postgres:postgres@localhost:5432/sangita_grantha'))
client = GeminiEmbeddingClient()
print('Cycles:', index_cycle_overviews(conn, client))
print('Kshetras:', index_kshetra_overviews(conn, client))
conn.close()
"
```

Use `--krithi-id` for a targeted direct run after a known correction. Use `--all` for a deliberate direct full-catalogue run; the batch runner defaults to all candidates unless limited. Real runs call an external provider and write database records.

The runners compare formatted-content hashes to skip already-indexed work and use document/profile upserts. `--force` requests re-embedding even when the skip check would normally apply. Removed/ineligible source text leads to obsolete-document retirement. Re-run after imports, lyric edits, section changes, and changes to metadata used by the formatter.

Hashes are implementation freshness keys (currently MD5 of formatted text), not cryptographic provenance checksums. Inspect both document and embedding hashes when validating a model-profile refresh. The scripts are incremental maintenance tools, not a background scheduler or a complete guarantee of corpus coverage.

Each composition commits separately. A failure rolls back that composition's transaction, allows other compositions to proceed, and causes a nonzero run outcome. The direct runner logs failed IDs; the batch runner also persists failure details. Shared helpers audit profile creation/activation and indexing changes.

## 7. Profile activation

With no active profile, a newly created first profile becomes active. When another profile is active, a replacement model's profile is created inactive so its partial backfill does not immediately replace live retrieval.

`--activate-profile` requests activation after a successful run. The helper verifies the profile exists and has embeddings, then deactivates other profiles and activates the target in one transaction. Activation is skipped when a runner reports failures.

**Coverage remains an operator responsibility:** “has at least one embedding” is not “all intended compositions are indexed.” A successful limited run can satisfy the activation helper's minimum check while leaving most of the corpus unindexed. Review intended coverage and evaluation before activation, and align the backend query model/dimensions at the same time.

The runtime service checks profile model/dimensions against its query client. An incompatible active profile produces an availability error rather than mixing vectors. If multiple profiles are active, the repository chooses the newest and logs a warning; the schema does not enforce a single active row by itself.

## 8. Inspect freshness and coverage

These are read-only diagnostic queries:

```sql
SELECT id, model_name, dimensions, task_type, is_active
FROM embedding_profiles
ORDER BY created_at DESC;

SELECT p.id, p.model_name, p.is_active,
       count(e.id) AS vectors,
       count(DISTINCT d.krithi_id) AS compositions
FROM embedding_profiles p
LEFT JOIN document_embeddings e ON e.profile_id = p.id
LEFT JOIN search_documents d ON d.id = e.document_id
GROUP BY p.id, p.model_name, p.is_active;

-- Document breakdown across all 4 kinds and anchor integrity
SELECT d.kind, count(*) AS total_documents,
       count(d.krithi_id) AS with_krithi,
       count(d.tag_id) AS with_tag,
       count(d.temple_id) AS with_temple,
       count(e.id) AS with_embedding
FROM search_documents d
LEFT JOIN document_embeddings e ON e.document_id = d.id
GROUP BY d.kind;

SELECT count(*) AS mismatched_content_hashes
FROM document_embeddings e
JOIN search_documents d ON d.id = e.document_id
WHERE e.content_hash <> d.content_hash;
```

Compare coverage with eligible source content, not only total `krithis` count: empty/short lyrics are deliberately excluded from some documents. Scripts can index non-published canonical rows; public retrieval also checks the current composition workflow state and document visibility. Do not infer publication from an embedding row.

## 9. Evaluate and record evidence

Run deterministic worker embedding tests and backend route/repository checks for code behavior. A provider-backed benchmark is a separate, intentionally configured run:

```bash
uv run python scripts/evaluate_retrieval.py --mode all --k 5
```

The evaluator uses [retrieval_benchmarks.json](../../tools/krithi-extract-enrich-worker/evals/retrieval_benchmarks.json) containing 31 test probes covering:
- **Baseline retrieval**: lexical hits, raga associations, multilingual variants.
- **Track 145 discriminative probes** (EVAL-24 through EVAL-31):
  - *Fire vs. space lingam*: *Arunachalanatham* (Tiruvannamalai, Agni) vs. *Ananda Natana Prakasam* (Chidambaram, Akasha).
  - *Kamalamba 6th enclosure*: *Kamalambikayastava* (Punnagavarali, Shashthi case, Sarvarakshakara chakra) disambiguated from 5th (*Sri Kamalambayah*, Bhairavi, Panchami) and 7th (*Sri Kamalambikayam*, Sahana, Saptami).
  - *Patronymic discrimination*: *Divakaratanujam* (Shani / Saturn as son of the Sun) vs. *Suryamurte* (Surya).
  - *Sthala occasion lore*: *Akshayalinga Vibho* (Keevalur temple doors opening).
  - *Nottusvara Western airs*: *Santatam Pahi Mam* matched via colonial tune "God Save the King".
  - *Macro overview retrieval*: High-level retrieval for group cycles (*Kamalamba Navavarnam*) and recorded pilgrimage gaps (*Kanyakumari Bhagavati Amman Temple*).

The batch runner writes a report and mirrors real runs into [embedding-execution-report.md](../10-implementations/embedding-execution-report.md). Its token/cost fields are estimates, not provider billing. Historical reports do not prove the current database's index state.

[Search API and ranking](../03-api/search.md) · [TRACK-145 Implementation Summary](../10-implementations/track-145-dikshitar-kshetra-musicological-metadata.md) · [Quality](../07-quality/README.md)

---

[Section index](./README.md) · [Documentation home](./../README.md) · [Feature status](./../01-requirements/features/README.md)
