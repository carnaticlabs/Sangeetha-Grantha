# Gemini Embedding 2 Catalogue Execution Report

**Execution Timestamp:** 2026-09-08 06:48:40 UTC  
**Model:** `gemini-embedding-2` (768-D Matryoshka Representation Learning)  
**Database:** PostgreSQL 18 with `pgvector` HNSW index  

---

## 1. Execution Telemetry

| Metric | Value |
|:---|:---|
| **Total Krithis in Catalogue** | 1226 |
| **Krithis Target in this Run** | 1 |
| **Krithis Processed Successfully** | 0 |
| **Krithis Failed** | 1 |
| **New Embeddings Generated** | **0** |
| **Embeddings Skipped (Unchanged)** | 0 |
| **Obsolete Documents Retired** | 0 |
| **Elapsed Duration** | 0m 0s |
| **Embedding Rate** | 0.00 docs/sec |
| **Estimated Tokens Consumed** | ~0 |
| **Estimated API Cost** | **~$0.0000 USD** |

---

## 2. Database State After Execution

| Table | Total Rows |
|:---|:---|
| `search_documents` (Composition Overviews) | 1226 |
| `search_documents` (Section Passages) | 25809 |
| `document_embeddings` (Active Vectors) | **27035** |
| HNSW Cosine Index Status | `idx_doc_embeddings_hnsw_cosine (110 MB)` |

---

## 3. Post-Run Sanity Check Searches

Verification of vector retrieval against live embedded krithis:

---

## 4. Failed Compositions

Persisted to `/Users/seshadri/project/sangeetha-grantha/tools/krithi-extract-enrich-worker/reports/embedding_failures_20260908_121840.json`; exit status was non-zero.

| Krithi ID | Title | Error |
|:---|:---|:---|
| `3747ba8d-2df1-4c7f-b272-20ba3c81a5d8` | Chera Raavademi | 400 INVALID_ARGUMENT. {'error': {'code': 400, 'message': 'API key not valid. Please pass a valid API key.', 'status': 'INVALID_ARGUMENT', 'details': [{'@type': 'type.googleapis.com/google.rpc.ErrorInfo', 'reason': 'API_KEY_INVALID', 'domain': 'googleapis.com', 'metadata': {'service': 'generativelanguage.googleapis.com'}}, {'@type': 'type.googleapis.com/google.rpc.LocalizedMessage', 'locale': 'en-US', 'message': 'API key not valid. Please pass a valid API key.'}]}} |

---

## 5. Verification & Recommendations

1. **Incremental Updates:** Re-running this script after lyric edits or imports re-embeds only documents whose content hash changed and retires documents whose source text is gone. It is not triggered automatically; schedule it or run it after imports.
2. **Ktor API Integration:** Endpoints `POST /v1/search/hybrid` and `POST /v1/search/semantic` are live and querying this dataset.
3. **Frontend Search:** The admin console (`/krithis`) provides instant toggling between Lexical, Hybrid (RRF), and Semantic modes.
