# Gemini Embedding 2 Catalogue Execution Report

**Execution Timestamp:** 2026-09-30 03:44:18 UTC  
**Model:** `gemini-embedding-2` (768-D Matryoshka Representation Learning)  
**Database:** PostgreSQL 18 with `pgvector` HNSW index  

---

## 1. Execution Telemetry

| Metric | Value |
|:---|:---|
| **Total Krithis in Catalogue** | 481 |
| **Krithis Target in this Run** | 481 |
| **Krithis Processed Successfully** | 481 |
| **Krithis Failed** | 0 |
| **New Embeddings Generated** | **48** |
| **Embeddings Skipped (Unchanged)** | 7551 |
| **Obsolete Documents Retired** | 0 |
| **Elapsed Duration** | 0m 39s |
| **Embedding Rate** | 1.23 docs/sec |
| **Estimated Tokens Consumed** | ~9,600 |
| **Estimated API Cost** | **~$0.0014 USD** |

---

## 2. Database State After Execution

| Table | Total Rows |
|:---|:---|
| `search_documents` (Composition Overviews) | 1226 |
| `search_documents` (Section Passages) | 25772 |
| `search_documents` (Cycle Overviews) | 0 |
| `search_documents` (Kshetra Overviews) | 0 |
| `document_embeddings` (Active Vectors) | **26998** |
| HNSW Cosine Index Status | `idx_doc_embeddings_hnsw_cosine (144 MB)` |

---

## 3. Post-Run Sanity Check Searches

Verification of vector retrieval against live embedded krithis:

### Query: *"Vatapi Ganapatim Hamsadhvani"*

| Rank | Title | Composer | Raga | Match % | Snippet |
|:---|:---|:---|:---|:---|:---|
| #1 | **vAtApi gaNa patiM** | Muthuswami Dikshitar | Hamsadhwani | `89.1%` | [Composition: vAtApi gaNa patiM] [Composer: Muthuswami Dikshitar] [Musical Form: KRITHI] [Raga: Hamsadhwani] [Tala: Adi]... |
| #2 | **vAtApi gaNa patiM** | Muthuswami Dikshitar | Hamsadhwani | `88.2%` | [Composition: vAtApi gaNa patiM] [Composer: Muthuswami Dikshitar] [Musical Form: KRITHI] [Section: PALLAVI] [Raga: Hamsa... |
| #3 | **vAtApi gaNa patiM** | Muthuswami Dikshitar | Hamsadhwani | `86.9%` | [Composition: vAtApi gaNa patiM] [Composer: Muthuswami Dikshitar] [Musical Form: KRITHI] [Section: PALLAVI] [Raga: Hamsa... |

### Query: *"Lord Shiva Anandatandavam Chidambaram"*

| Rank | Title | Composer | Raga | Match % | Snippet |
|:---|:---|:---|:---|:---|:---|
| #1 | **Ananda naTana prakASaM** | Muthuswami Dikshitar | Kedaram | `71.2%` | [Composition: Ananda naTana prakASaM] [Composer: Muthuswami Dikshitar] [Musical Form: KRITHI] [Raga: Kedaram] [Tala: Mis... |
| #2 | **Siva kAmESvaraM** | Muthuswami Dikshitar | Ārabhi | `70.7%` | [Composition: Siva kAmESvaraM] [Composer: Muthuswami Dikshitar] [Musical Form: KRITHI] [Section: CHARANAM] [Raga: Ārabhi... |
| #3 | **Siva kAmESvaraM** | Muthuswami Dikshitar | Ārabhi | `70.3%` | [Composition: Siva kAmESvaraM] [Composer: Muthuswami Dikshitar] [Musical Form: KRITHI] [Raga: Ārabhi / Arabhi] [Tala: Ad... |

### Query: *"Kritis of Muthuswami Dikshitar on Pancha bootha"*

| Rank | Title | Composer | Raga | Match % | Snippet |
|:---|:---|:---|:---|:---|:---|
| #1 | **panca bhUta kiraNAvaLIM** | Muthuswami Dikshitar | Keeranāvali | `84.9%` | [Composition: panca bhUta kiraNAvaLIM] [Composer: Muthuswami Dikshitar] [Musical Form: KRITHI] [Raga: Keeranāvali / Keer... |
| #2 | **EkAmra nAthaM bhajEhaM** | Muthuswami Dikshitar | Gamakakriyā | `84.2%` | [Composition: EkAmra nAthaM bhajEhaM] [Composer: Muthuswami Dikshitar] [Musical Form: KRITHI] [Raga: Gamakakriyā / Gamak... |
| #3 | **panca bhUta kiraNAvaLIM** | Muthuswami Dikshitar | Keeranāvali | `83.2%` | [Composition: panca bhUta kiraNAvaLIM] [Composer: Muthuswami Dikshitar] [Musical Form: KRITHI] [Section: PALLAVI] [Raga:... |

### Query: *"Kamakshi Navavaranam Dikshitar"*

| Rank | Title | Composer | Raga | Match % | Snippet |
|:---|:---|:---|:---|:---|:---|
| #1 | **kAmAkshIM kalyANIM** | Muthuswami Dikshitar | Mechakalyāni | `84.6%` | [Composition: kAmAkshIM kalyANIM] [Composer: Muthuswami Dikshitar] [Musical Form: KRITHI] [Raga: Mechakalyāni / Mechakal... |
| #2 | **kAmAkshi vara lakshmi** | Muthuswami Dikshitar | Bilahari | `84.4%` | [Composition: kAmAkshi vara lakshmi] [Composer: Muthuswami Dikshitar] [Musical Form: KRITHI] [Raga: Bilahari] [Tala: Adi... |
| #3 | **kAmAkshi kAma kOTi** | Muthuswami Dikshitar | sumadyuti | `84.2%` | [Composition: kAmAkshi kAma kOTi] [Composer: Muthuswami Dikshitar] [Musical Form: KRITHI] [Raga: sumadyuti] [Tala: Rupak... |

---

## 4. Verification & Recommendations

1. **Incremental Updates:** Re-running this script after lyric edits or imports re-embeds only documents whose content hash changed and retires documents whose source text is gone. It is not triggered automatically; schedule it or run it after imports.
2. **Ktor API Integration:** Endpoints `POST /v1/search/hybrid` and `POST /v1/search/semantic` are live and querying this dataset.
3. **Frontend Search:** The admin console (`/krithis`) provides instant toggling between Lexical, Hybrid (RRF), and Semantic modes.
