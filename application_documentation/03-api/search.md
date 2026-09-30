| Metadata | Value |
|:---|:---|
| **Status** | Active |
| **Version** | 1.2.0 |
| **Last Updated** | 2026-09-30 |
| **Author** | Sangeetha Grantha Team |
| **Document Type** | Current guide |

# Catalogue search

---

Sangeetha Grantha has two complementary search experiences. Rasika combines hybrid discovery with the public catalogue's predictable query/filter/paging contract. The Curator Console also offers hybrid and semantic retrieval over indexed composition overviews and lyric passages.

## Choose the search mode

| Mode | Useful for | Behavior |
|:---|:---|:---|
| Lexical | Known title, incipit, spelling, or phrase | Text matching and conventional catalogue filters |
| Hybrid | A phrase or topic that benefits from both exact and related matches | Combines lexical and vector result ranks using reciprocal rank fusion |
| Semantic | Meaning, related concepts, thematic discovery | Retrieves from the active vector index |

The admin list defaults to Hybrid, but an empty query uses ordinary browsing. Raga and composer filters apply to the console's discovery requests. The language filter belongs to the lexical experience. A relevance score is a ranking signal, not a probability that the musicological interpretation is correct.

Rasika Explore is not a conversational assistant. On Krithis it calls `POST /v1/search/hybrid` and `POST /v1/search/semantic` as well as the catalogue. The initial mode is Hybrid, presented as the everyday search with the hint “Search titles, lyrics and meaning together.” An optional **Search options** disclosure offers **All matches** (Hybrid), **Related meanings** (Semantic), and **Titles & lyrics** (Lexical); no mode choice is required before searching. Each discovery post sends the trimmed query, null `composerId` and `ragaId`, and `limit` 30 (the server default of 20 is not the Rasika cap). Hybrid and Semantic are not paged. The header is "Top matches" and does not treat `totalMatches` as a library count. A blank query is still posted, and an empty server list is shown; Rasika does not replace it with catalogue browse. Applied raga and composer filters stay on the Lexical catalogue GET only. Their controls and chips appear only in Titles & lyrics; discovery explains that any saved filters apply only there. Ragas and Composers stay `GET /v2/catalogue/ragas` and `GET /v2/catalogue/composers`. Lexical Krithis stays `GET /v2/catalogue/krithis`. See the [catalogue contract](./api-contract.md).

## Musicological retrieval capabilities (Track 145)

With the deployment of **Track 145** (Flyway migrations `V66` & `V67`, repeatable seed `R__seed_09`, and Python worker `Context Formatter 2.0`), Sangeetha Grantha's semantic search substrate is deeply grounded in Indian Classical Music theory (*Lakshana*) and sacred temple geography (*Kshetra*).

### What makes this search discriminative

1. **Vibhakti & Enclosure Disambiguation**: In cycles like the *Kamalamba Navavaranam*, multiple hymns address the goddess in similar high-register Sanskrit. Naive vector search collapses these hymns into an undifferentiated cluster. Context Formatter 2.0 embeds grammatical cases (*aṣṭa-vibhakti*), enclosure indices (Avaranas 1–9), Sri Chakra names, and Yogini clans. A query for the 6th enclosure isolates *Kamalambikayastava* (Punnagavarali) without colliding with the 5th enclosure (*Sri Kamalambayah*, Bhairavi) or 7th enclosure (*Sri Kamalambikayam*, Sahana).
2. **Pancha Bhuta Sthala Discrimination**: Temple lingams associated with the five cosmic elements (Agni, Akasha, Vayu, Apas, Prithvi) across different regions are discriminated by elemental metadata, preventing Tiruvannamalai (Agni) from returning Chidambaram (Akasha).
3. **Patronymic Epithet Resolution**: Queries citing divine relationships (such as Shani as the son of Surya) isolate *Divakaratanujam* without false-matching *Suryamurte*.
4. **Sthala Legends & Occasions**: Specific miraculous events and temple rituals (e.g., the closed temple doors opening at Keevalur) directly retrieve the associated composition (*Akshayalinga Vibho*).
5. **Western Airs (Nottusvara Sahitya)**: Sanskritized colonial band tunes composed by Dikshitar during his time in Manali are retrievable by their original 18th/19th-century European air names (e.g., *God Save the King*, *Castalian Dew*).
6. **Macro Cycle & Kshetra Overviews**: Generates and vectorizes synthesized overview documents for entire cycles and holy sites, including documented historical gap shrines like Kanyakumari.

### Sample query phrases and expected retrieval

| Category | End-user query phrase | Expected target composition / document | Discriminative key |
|:---|:---|:---|:---|
| **Sri Vidya / Vibhakti** | `"Kamalamba sixth enclosure sarvarakshakara nigarbha yogini shashthi"` | *Kamalambikayastava* (Punnagavarali) | Case: Shashthi; Chakra: Sarvarakshakara; Yogini: Nigarbha |
| **Cosmic Elements** | `"Dikshitar Agni lingam fire element at Tiruvannamalai"` | *Arunachalanatham* (Saranga) | Bhuta: Agni; Mandalam: Nadu |
| **Space Element** | `"Dikshitar Akasha lingam ether element at Chidambaram"` | *Ananda Natana Prakasam* (Kedaram) | Bhuta: Akasha; Mandalam: Chola |
| **Patronymics** | `"Dikshitar Navagraha krithi for Shani Saturn son of the Sun"` | *Divakaratanujam* (Yadukulakambhoji) | Deva: Shani ("Son of the Sun") |
| **Sun God** | `"Dikshitar Navagraha hymn to the Sun God Surya in Saurashtram"` | *Suryamurte* (Saurashtram) | Deva: Surya |
| **Sthala Legend** | `"Dikshitar temple door opening at Keevalur in Shankarabharanam"` | *Akshayalinga Vibho* (Shankarabharanam) | Occasion: Keevalur temple door |
| **Western Air** | `"Dikshitar Nottusvara composed to the tune of God Save the King"` | *Santatam Pahi Mam* (Sankarabharanam) | Tune: God Save the King |
| **Pilgrimage Gap** | `"Muthuswami Dikshitar pilgrimage to Kanyakumari Bhagavati Amman gap temple"` | *Kanyakumari Kshetra Overview* (Macro Document) | Negative Evidence; Recorded Gap |
| **Shrine Specific** | `"Muthuswami Dikshitar Subrahmanya kriti at Kazhugumalai shrine"` | *Subrahmanyena Rakshitoham* (Suddhadhanyasi) | Kshetra: Kazhugumalai |
| **Coastal Murugan** | `"Dikshitar kriti praising Lord Subrahmanya at sea-shore Tiruchendur"` | *Sri Subrahmanyo Mam Rakshatu* (Todi) | Kshetra: Tiruchendur |

## API behavior

Both `POST /v1/search/hybrid` and `POST /v1/search/semantic` accept `query`, optional `composerId`/`ragaId`, and `limit` (default 20). Results identify the composition, matched text, document kind, and available ranking scores. `totalMatches` is the number returned.

| Index state | Hybrid | Semantic |
|:---|:---|:---|
| No active profile | Lexical-only retrieval | Empty result list |
| Compatible active profile and indexed content | Fused lexical/vector ranking | Vector ranking |
| Active profile disagrees with query model or dimensions | Availability error | Availability error |
| Empty/whitespace query | Empty API result | Empty API result |

Hybrid's lexical fallback searches `search_documents`; it does not query the ordinary catalogue tables directly. A completely fresh index can therefore yield no hybrid results even when ordinary catalogue browsing finds compositions.

Requests without an admin role are restricted to published results. Admin identity can expose editorial results. Check both audiences when validating index visibility.

Sources: [HybridSearchService](../../modules/backend/api/src/main/kotlin/com/sangita/grantha/backend/api/services/HybridSearchService.kt), [search routes](../../modules/backend/api/src/main/kotlin/com/sangita/grantha/backend/api/routes/SemanticSearchRoutes.kt), [search DTOs](../../modules/shared/domain/src/commonMain/kotlin/com/sangita/grantha/shared/domain/model/SearchDtos.kt).

## Ranking and result identity

Semantic retrieval uses cosine similarity (`1 - cosine distance`) and selects one representative matched document per composition. Hybrid retrieves up to 500 lexical and vector candidates per branch, combines document ranks with `1/(60 + vectorRank) + 1/(60 + lexicalRank)` for the available branches, then selects one representative per composition. Small overview preferences influence representative selection.

The repository clamps requested limits to 1–100. Composer filters use the composition identity; raga filters match the primary raga or membership in `krithi_ragas`. Anonymous/non-admin scope requires both a published composition and visible search document. `similarityScore`, `lexicalScore`, and `rrfScore` are different measures; a displayed percentage is not a calibrated confidence probability.

Source: [KrithiSearchRepository](../../modules/backend/dal/src/main/kotlin/com/sangita/grantha/backend/dal/repositories/KrithiSearchRepository.kt).

## Index architecture

```mermaid
flowchart LR
    C[Canonical compositions and lyric sections] --> F[Context formatter]
    F --> D[search_documents]
    F --> G[Document embedding client]
    G --> E[document_embeddings]
    P[embedding_profiles] --> E
    Q[Query] --> K[Query embedding client]
    K --> R[Vector retrieval]
    E --> R
    Q --> L[Lexical retrieval]
    R --> H[Rank fusion]
    L --> H
    H --> A[Admin result cards]
```

[Migration V58](../../database/migrations/V58__semantic_search_pgvector.sql), [Migration V66](../../database/migrations/V66__dikshitar_musicological_enrichment.sql), and [Migration V67](../../database/migrations/V67__search_document_anchor_constraints.sql) define the storage contract: profiles identify model/dimensions/task type, documents identify composition/section/variant or macro cycle/kshetra anchors, and embeddings bind a document to a profile. Vectors are stored as `vector(768)` with a cosine HNSW index. The current tooling defaults to `gemini-embedding-2` with MRL truncation to 768 dimensions; model availability and billing are external operational concerns.

Indexing is a separate operation from importing. Importing a composition does not establish that its latest text has been embedded. The tools compare content hashes, retire obsolete documents, audit writes, and support profile activation after a successful run.

The [embedding guide](../09-ai/embeddings.md) describes document eligibility, metadata context, direct versus locally batched execution, content hashes, profile activation, and read-only coverage queries. See [TRACK-145 Implementation Summary](../10-implementations/track-145-dikshitar-kshetra-musicological-metadata.md) for the musicological enrichment architecture.

## Populate or refresh the index

Run from the worker directory after configuring the intended database and embedding credentials. Inspect the plan first:

```bash
cd tools/krithi-extract-enrich-worker
uv sync --frozen --extra dev
uv run python scripts/embed_catalogue.py --limit 5 --dry-run
```

For an authorized indexing run:

```bash
uv run python scripts/embed_catalogue.py --limit 5
```

For concurrent full-catalogue batch embedding:

```bash
uv run python scripts/batch_embed_catalogue.py --concurrency 8 --delay 0.05 --report-path /tmp/embedding-run.md
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

Use the script's `--help` for `--krithi-id`, `--all`, `--force`, `--model`, and `--activate-profile`. Keep the backend query embedder and active profile aligned. A different model needs its own profile and compatible index coverage before activation. Dimension changes require a storage migration; changing a flag cannot resize the existing column.

The [batch indexing script](../../tools/krithi-extract-enrich-worker/scripts/batch_embed_catalogue.py) shares index-maintenance logic with the direct runner. See [TRACK-108 implementation](../10-implementations/track-108-semantic-search-gemini-embedding-2.md) and [TRACK-145 implementation](../10-implementations/track-145-dikshitar-kshetra-musicological-metadata.md) for rollout evidence.

## Diagnose results

1. Verify the composition exists and its workflow state permits the requesting audience to see it.
2. Verify `search_documents`, `document_embeddings`, and the active profile for the intended database across all 4 document kinds (`COMPOSITION_OVERVIEW`, `SECTION_PASSAGE`, `CYCLE_OVERVIEW`, `KSHETRA_OVERVIEW`).
3. Compare the indexed text/hash with current lyrics, especially after reingestion or section repairs.
4. If the API reports unavailability, inspect embedding credentials, provider failures, and profile compatibility.
5. Compare a known-title lexical query with the hybrid/semantic result. A thematic miss alone is not evidence of a parser failure.
6. Verify retrieval precision using `evaluate_retrieval.py` against the 31 retrieval benchmark probes.

[Operations](../08-operations/README.md) covers logs and runtime configuration; [quality checks](../07-quality/README.md) cover deterministic tests and retrieval evidence.

---

[Section index](./README.md) · [Documentation home](./../README.md) · [Feature status](./../01-requirements/features/README.md)
