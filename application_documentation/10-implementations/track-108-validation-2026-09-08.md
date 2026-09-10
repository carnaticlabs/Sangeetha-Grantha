| Metadata | Value |
|:---|:---|
| **Status** | Active |
| **Version** | 1.0.0 |
| **Last Updated** | 2026-09-10 |
| **Author** | Sangeetha Grantha Team |
| **Document Type** | Evidence record |

# TRACK-108 validation and functional opportunities

---

> [!NOTE]
> Historical evidence: results, counts, commands, and observations below belong to the original work described here. The editorial update date is not a new test or corpus verification. For present behavior, use [current feature map](./../01-requirements/features/README.md).

## Remediation status (2026-09-08, same day)

| # | Finding | Status | Where |
|:---|:---|:---|:---|
| 1 | HNSW candidate pool | Fixed | `KrithiSearchRepository`: transaction-local `hnsw.iterative_scan = 'strict_order'`, `hnsw.ef_search = 100`. Live EVAL-04: 5/5 at `limit=5`, 30/30 distinct at `limit=30` (was 3 and 20). |
| 2 | Re-embedding does not detect changes | Fixed | Both scripts compare stored vs computed hash; `embed_catalogue.py` now formats with the same tags as the batch script so both paths produce identical text/hashes. Reconciliation is still run-on-demand (no queue); see the deferred item below. |
| 3 | Batch failure handling | Fixed | Per-composition commit + `rollback()` on failure in both scripts; failed ids persisted to `reports/embedding_failures_<ts>.json` and listed in the report; exit status 1 on any failure; profile activation skipped. Verified live with an invalid API key (vector count unchanged, exit 1). |
| 4 | Mixed model profiles | Fixed | Kotlin resolves one active profile per request (`activeEmbeddingProfile`), passes its id into both SQL branches, and returns 503 (`EmbeddingServiceUnavailableException`) if its model/dimensions differ from the query embedder. Python: `--dims` must be 768 (`vector(768)`); a second profile is created **inactive**; `--activate-profile` switches atomically and refuses an empty index. Regression tests in `SemanticSearchRoutesTest` and `tests/integration/test_w4_embedding_index_maintenance.py`. |
| 5 | Evaluator quality gate | **Deferred** | `unexpected_composer` negatives are honoured; canonical-ID fixtures, spelling-tolerant matching, live-route execution, thresholds/exit codes are a separate plan (see TRACK-108 follow-up). |
| 6 | Blank credentials fabricate a vector | Fixed | Typed `EmbeddingServiceUnavailableException` → 503; dimension/finiteness/non-zero validation; synthetic vector only behind `allowSyntheticFallback` in tests. Hybrid now degrades to lexical-only when no profile is active yet. |
| 7 | Cosmetic pagination | Fixed | Pagination rendered only in standard/browse mode. |
| 8 | Stale responses / hidden errors | Fixed | Request-generation guard + explicit error state in `KrithiList.tsx`. |
| 9 | Hybrid badges | Fixed | Nullable `lexicalScore`/`rrfScore`; semantic and lexical evidence labelled separately. |
| 10 | Credentials in logs | Fixed | `redact_db_url` in `update_search_headers.py`. |
| 11 | No audit entries | Fixed | `write_audit` helper used by both embed scripts (per composition: embedded/retired counts, retired ids, profile), by profile create/activate, and by `update_search_headers.py` (per document, before/after header). |
| 12 | Dry-run creates a profile | Fixed | Dry runs use read-only `find_profile`; profile creation and the `application_documentation` report mirror happen only on real runs. |
| 13 | Stale reconciliation | Fixed | Upserts now set `original_content`; after each composition the scripts compute the desired document set and retire everything else for that krithi (embeddings cascade), previewed in dry-run, recorded in audit. |

Shared helpers live in `tools/krithi-extract-enrich-worker/src/embeddings/catalogue_index.py`. Note for the next real run: the current formatter emits aliased raga headers (`[Raga: Mechakalyāni / Mechakalyani]`) while the live index was built before that change, so the hash check will legitimately re-embed the catalogue.

The implementation works against the current catalogue, but its completion claim is premature. Compilation and regression tests pass; retrieval completeness, index maintenance, failure recovery, and the quality gate need correction before treating this as a dependable release.

This was a review, not an implementation pass. Application code and catalogue data were not changed. Read-only catalogue queries, disposable integration databases, live search requests, and browser interactions supplied the evidence below. Transaction-local HNSW settings were rolled back.

## Validation evidence

| Check | Actual result |
|:---|:---|
| Backend DAL unit tests | 59 passed |
| Backend API unit tests | 216 passed |
| Backend DAL integration tests | 52 passed |
| Backend API integration tests | 87 passed, including 3 semantic-route tests |
| Worker regression and integration tests | 403 passed, including 10 embedding tests |
| Frontend Vitest | 61 passed across 10 files |
| Frontend production build | Passed |
| Frontend TypeScript | `tsc --noEmit` passed |
| Scoped Python mypy | Passed, 8 source files |
| Scoped Ruff | Failed: E402 at `scripts/update_search_headers.py:29` |
| Documentation link check | Failed: two existing track links target documents that exist locally but are untracked; checker resolves against Git-tracked paths |
| Live Flyway history | V58 successful |
| Live coverage | 27,035 documents/vectors; all 1,226 compositions covered |
| Document grains | 1,226 overviews and 25,809 passages |
| Current consistency | No document/text hash mismatch, document/vector hash mismatch, missing eligible section documents, orphaned passage documents, stale section original text, or missing primary-raga junctions found |
| Active embedding profile | One: `gemini-embedding-2`, 768 dimensions, `RETRIEVAL_DOCUMENT` |
| Structured language metadata | All 27,035 documents have NULL `language_code` |
| HNSW index size | 109 MB; contradicts track's claimed sub-60-MB footprint |
| Live requests | All 20 frozen queries succeeded on each endpoint: 40/40 HTTP successes |
| Live combined filters | Composer + Hamsadhwani raga filters returned Vatapi correctly on both endpoints |
| Browser | Hybrid and semantic search exercised; hybrid page two repeated the same 30 composition titles |

Commands and terminal result excerpts:

```text
./gradlew :modules:backend:dal:test :modules:backend:api:test \
  :modules:backend:dal:integrationTest :modules:backend:api:integrationTest
BUILD SUCCESSFUL in 1m 2s

# In the worker directory; TEST_DATABASE_URL unset to force disposable databases
uv run --offline pytest tests -q
403 passed, 94 warnings in 25.05s

# In the frontend directory
bun run build
✓ built in 1.79s
bun run test:unit
Test Files 10 passed (10)
Tests 61 passed (61)
bun x --no-install tsc --noEmit
Exit 0

# Scoped to embedding modules and five new scripts
uv run --offline mypy ...
Success: no issues found in 8 source files
uv run --offline ruff check ...
scripts/update_search_headers.py:29:1: E402 Module level import not at top of file
Found 1 error.

make check-docs
2 broken relative link(s):
  TRACK-108 -> track-108-conversational-discovery-analysis-sep-2026.md
  TRACK-108 -> track-108-semantic-search-gemini-embedding-2.md
```

The frontend Makefile target currently invokes `bun test`; the skill explicitly requires Vitest, so validation used `bun run test:unit`. This pre-existing Makefile mismatch is not attributed to TRACK-108. Ruff's finding is recorded as a check outcome rather than a separate review finding, consistent with REVIEW.md.

The two documentation targets exist on disk but have not been added to Git. They need to accompany the implementation when it is staged. No staging or commits were performed. All 18 local file links in this review were independently checked for existence, and its evidence JSON contains all 40 benchmark responses.

## Findings

Priorities: P1 should be resolved before relying on this release; P2 is a functional or policy defect to schedule. Each finding identifies its REVIEW.md pass and distinguishes reproduced behavior from code inspection.

1. **[P1][Bugs] HNSW stops before the requested candidate pool is filled.**
   [KrithiSearchRepository.kt:273](/Users/seshadri/project/sangeetha-grantha/modules/backend/dal/src/main/kotlin/com/sangita/grantha/backend/dal/repositories/KrithiSearchRepository.kt:273).
   The SQL asks for 100–500 candidates, but no query configures `hnsw.ef_search` or iterative scanning. A live EXPLAIN of the semantic query shape, using an existing stored vector, showed only 40 HNSW rows for a requested 150; deduplication returned 20 compositions for `limit=30`. The same query returned 30 with transaction-local `hnsw.iterative_scan=strict_order`. Live semantic EVAL-04 returned only 3 items for `limit=5`. Both retrieval branches need an explicitly validated candidate strategy. Account for multiple passages/scripts per composition and selective filters, and deduplicate before final ranking where appropriate. Increasing SQL LIMIT alone does not solve this. This matches [pgvector's documented HNSW filtering behavior](https://github.com/pgvector/pgvector#filtering).

2. **[P1][Bugs, Compliance] The documented re-embedding path does not detect changes.**
   [embed_catalogue.py:200](/Users/seshadri/project/sangeetha-grantha/tools/krithi-extract-enrich-worker/scripts/embed_catalogue.py:200), with the same condition at line 278.
   It computes a new hash but fetches only document/vector IDs and skips whenever both exist. A mock probe with changed lyrics produced zero embedding calls. The newer batch script does compare hashes, but neither script is wired into lyric edits or imports; the execution report's claim of automatic updates is unsupported. Consolidate these paths, compare the embedding's accepted hash as well as document text, and provide an explicit queued or scheduled reconciliation mechanism. Keep the existing catalogue searchable while replacements are generated.

3. **[P1][Bugs] Batch failure handling leaves the database transaction open or aborted.**
   [batch_embed_catalogue.py:525](/Users/seshadri/project/sangeetha-grantha/tools/krithi-extract-enrich-worker/scripts/batch_embed_catalogue.py:525).
   The exception handler continues without rollback. A SQL error therefore poisons all subsequent compositions and the final reporting queries. A provider failure after earlier writes can instead leave partial work to be committed by the next successful composition, despite the first being recorded as failed. Use a transaction/savepoint per composition and explicit rollback on failure. Persist failed IDs and return a failure exit status for incomplete runs. This is established by transaction control inspection; no failure was injected into the live catalogue.

4. **[P1][Bugs, Compliance] Retrieval can silently mix incompatible model profiles.**
   [KrithiSearchRepository.kt:261](/Users/seshadri/project/sangeetha-grantha/modules/backend/dal/src/main/kotlin/com/sangita/grantha/backend/dal/repositories/KrithiSearchRepository.kt:261) and line 364; [embed_catalogue.py:67](/Users/seshadri/project/sangeetha-grantha/tools/krithi-extract-enrich-worker/scripts/embed_catalogue.py:67).
   Both queries select every active profile, while the CLI permits another model and activates newly created profiles immediately. The Kotlin query embedder independently defaults to Embedding 2. Starting a backfill with another 768-dimensional model therefore mixes unrelated spaces and may multiply rows in document-level fusion. Current data has only one profile, so this is a model-upgrade defect rather than current contamination. Bind query generation and retrieval to one explicit profile/generation; build replacements inactive and switch atomically after evaluation. Validate supported dimensions against the fixed `vector(768)` storage.

5. **[P1][Bugs, Compliance] The evaluation cannot establish the claimed quality gate.**
   [evaluate_retrieval.py:101](/Users/seshadri/project/sangeetha-grantha/tools/krithi-extract-enrich-worker/scripts/evaluate_retrieval.py:101).
   `unexpected_composer` is ignored. EVAL-20 has no positive criteria, so an explicitly forbidden Purandara Dasa result scores `(True, 1.0)`; this was reproduced. Several other cases accept any composition by a composer. Conversely, the correct `jagadAnanda kAraka` fails the concatenated `Jagadanandakaraka` expectation, and spelling variants of Hamsadhwani and Syama Sastri also cause false negatives. The shipped runner only exercises its own semantic SQL, omits the live krithi publication predicate, and has no threshold or failing exit code for low quality. Use canonical expected IDs/sets, explicit negative assertions, native-script cases, a lexical baseline, both real API routes, and an agreed relevance/latency gate. Report hit rate distinctly from recall over a known relevant set.

6. **[P1][Bugs] Missing credentials fabricate an invalid query vector.**
   [GeminiEmbeddingClient.kt:65](/Users/seshadri/project/sangeetha-grantha/modules/backend/api/src/main/kotlin/com/sangita/grantha/backend/api/clients/GeminiEmbeddingClient.kt:65).
   Blank configuration returns 768 zeros in production code. PostgreSQL confirms cosine distance to a zero vector is NaN. Depending on execution plan and result population, this can yield invalid scores/serialization errors or meaningless ranking instead of a clear unavailable state. Move synthetic behavior into test doubles, validate dimension/finiteness/nonzero norm, and return a typed unavailable response. Hybrid search should retain a clearly identified lexical fallback on provider outages.

7. **[P2][Bugs] Semantic/hybrid pagination is cosmetic.**
   [KrithiList.tsx:68](/Users/seshadri/project/sangeetha-grantha/modules/frontend/sangita-admin-web/src/pages/KrithiList.tsx:68) and line 419.
   Both APIs request `limit: 30` without offset; the page renders every returned item but computes pagination with a page size of 25. In the browser, “Dikshitar” showed 30 rows under “Showing 1–25 of 30”; Next showed the same set of 30 titles under “Showing 26–30 of 30”, with some order/snippet changes from ties. Either expose a fixed top-results list without pagination or implement stable pagination and a truthful count contract. `totalMatches` currently means returned items, not all matches.

8. **[P2][Bugs] Delayed requests can overwrite current search state, and failures look like no matches.**
   [KrithiList.tsx:42](/Users/seshadri/project/sangeetha-grantha/modules/frontend/sangita-admin-web/src/pages/KrithiList.tsx:42) and line 91.
   Debounce cleanup cancels only the timer, not a started request. Changing query, filter, or mode while Gemini is pending allows an old response to overwrite newer results and clear loading prematurely. The catch branch empties the list without an error state, so provider/HTTP errors are shown as a valid empty search. Use keyed TanStack queries or cancellation plus request-generation guards, with separate loading, error, empty, and fallback states. This extends an existing page pattern into a slower asynchronous flow; the race was identified from code, not deterministically reproduced under controlled delays.

9. **[P2][Bugs] Relevance badges misrepresent hybrid matches.**
   [KrithiList.tsx:401](/Users/seshadri/project/sangeetha-grantha/modules/frontend/sangita-admin-web/src/pages/KrithiList.tsx:401).
   Hybrid results rank by RRF, but the UI labels cosine similarity as a percentage match. The DAL substitutes zero for a lexical-only candidate's unavailable semantic score. The browser consequently displayed a highly ranked exact composer match as “0% Match”. Preserve missing scores as nullable and label lexical/semantic evidence; avoid presenting uncalibrated cosine values as confidence percentages.

10. **[P1][Security] Header-update logs expose database credentials.**
    [update_search_headers.py:47](/Users/seshadri/project/sangeetha-grantha/tools/krithi-extract-enrich-worker/scripts/update_search_headers.py:47).
    The script logs the complete `db_url`, including a supplied username/password, even in dry-run mode. Redact credentials before logging. The older backfill already demonstrates a safer host-only log. No real credential was read or emitted during this review.

11. **[P2][Compliance] Index mutations have no audit entries.**
    [batch_embed_catalogue.py:234](/Users/seshadri/project/sangeetha-grantha/tools/krithi-extract-enrich-worker/scripts/batch_embed_catalogue.py:234), [embed_catalogue.py:208](/Users/seshadri/project/sangeetha-grantha/tools/krithi-extract-enrich-worker/scripts/embed_catalogue.py:208), and [update_search_headers.py:91](/Users/seshadri/project/sangeetha-grantha/tools/krithi-extract-enrich-worker/scripts/update_search_headers.py:91).
    These writers update profiles/documents/vectors without writing AUDIT_LOG; V58 adds no audit trigger. Record actor/run ID, affected document/profile, prior/new hash, and outcome transactionally. This follows the repository's explicit mutation-audit rule, rather than an assumed regulatory requirement.

12. **[P2][Bugs] Dry-run can create and activate a profile.**
    [batch_embed_catalogue.py:476](/Users/seshadri/project/sangeetha-grantha/tools/krithi-extract-enrich-worker/scripts/batch_embed_catalogue.py:476) and [embed_catalogue.py:341](/Users/seshadri/project/sangeetha-grantha/tools/krithi-extract-enrich-worker/scripts/embed_catalogue.py:341).
    Both entry points call a committing `get_or_create_profile` before applying dry-run behavior. On a fresh database or new model, preview mode therefore changes persistent state. Resolve profiles read-only during preview or defer creation until a real write.

13. **[P2][Bugs] Reconciliation leaves obsolete text behind.**
    [batch_embed_catalogue.py:239](/Users/seshadri/project/sangeetha-grantha/tools/krithi-extract-enrich-worker/scripts/batch_embed_catalogue.py:239) and line 269.
    Upserts refresh indexed text/hash but never `original_content`; changing lyrics therefore leaves the original-content field stale. Clearing a lyric-section text below the eligibility threshold simply skips it, leaving the previous document/vector searchable. Removing a lyric-section junction also leaves the search document alive when its separately referenced section and variant still exist. Reconcile the desired document set, retire obsolete entries, and update original/indexed text together. Current live checks found no such stale sections; these are edit/delete-path defects.

## Additional contract and coverage observations

- **Provider compatibility needs a specific decision.** Both clients send `task_type`/`taskType`. Google's current documentation says Embedding 2 requires task instructions in the input and does not support that field; it specifies paired query/document prefixes. All live API requests nevertheless succeeded in this environment. Therefore this is a documented-contract discrepancy, not a reproduced request failure. Add a real contract test, verify whether the field is ignored, and evaluate a versioned prefix change before regenerating the catalogue. See [Google's embedding guide](https://ai.google.dev/gemini-api/docs/embeddings#task-types-with-embeddings-2).
- **The route tests do not test retrieval.** They seed no search documents or embeddings; a non-null empty item list passes. Add deterministic nonzero-vector fixtures and test ranking, composition deduplication, draft access, admin access, filters including secondary ragas, stale profiles, provider failures, invalid input, and populated responses. Existing public predicates correctly check both document visibility and current krithi publication; no draft leak was found by inspection. The live catalogue is entirely published, so it cannot validate draft exclusion.
- **Missing query bounds.** The new DTO is absent from RequestValidation. Blank strings are handled and limits are clamped, but query length/token count is unbounded below the application's general body ceiling. The existing global 100-request/minute limiter is present; this review does not claim the route is completely unthrottled. Add a provider-appropriate query limit and embedding concurrency/budget controls.
- **Corpus metadata is incomplete.** Writers do not populate structured language/script columns, do not include ordered secondary ragas in context, and omit musical form. The primary backfill also omits cycle tags that the batch path includes. These limit language-preferred passages, Ragamalika retrieval, and thematic consistency.
- **Schema mirrors are partial.** `SearchTables.kt` maps the PostgreSQL document-kind enum as plain text and does not model the vector column. Current raw-SQL reads work, but these objects are not a complete writable mirror of V58. Add a proper mapping before reusing them for ORM writes.
- **Cycle seed scope needs maintenance.** Live tag membership counts are 5 Pancha Bhuta, 11 Kamalamba, 9 Navagraha, 5 Pancharatna, 10 Abhayamba, 9 Nilotpalamba, and 3 Swarajathi entries. Pancha Bhuta titles were spot-checked. This is not a full scholarly certification of all memberships. Title-substring matching is fragile, and Flyway repeatables will not re-run solely because a later import adds compositions. Store curated membership by canonical identity and reconcile new imports. The hard-coded tag IDs also assume a slug never existed under a different UUID.
- **Documentation drifts from the implementation.** The track registry says Not Started; the track says Completed while retaining the old 001 approach. The linked architecture proposal explicitly remains Draft with no accepted Spec/Plan. The implementation summary's example schema differs from V58, and the OpenAPI document omits the new endpoints. `pg18` is a mutable tag, not a patch/digest pin. Reconcile these records alongside the corrective work.

## Live retrieval measurements

These numbers reproduce the shipped evaluator's matching predicates against the actual APIs. They are diagnostic measurements, not trustworthy musical relevance scores, because of finding 5. Each endpoint ran the same 20 queries sequentially with `limit=5`; latency is end-to-end on the local development stack, not a load test or production SLO.

| Mode | Hit@1 | Hit@5 | MRR@5 | Median latency | p95 latency |
|:---|---:|---:|---:|---:|---:|
| Semantic | 45% (9/20) | 65% (13/20) | 0.518 | 705 ms | 745 ms |
| Hybrid | 55% (11/20) | 70% (14/20) | 0.602 | 1,236 ms | 1,451 ms |

Hybrid correctly promoted Vatapi for the remembered phrase in EVAL-02. Other observations expose the scorer problem: EVAL-09's correct top title is marked wrong because of spacing; EVAL-14 accepts an unrelated Dikshitar title merely because its composer matches; the negative case always accepts the first result. No model/dimension A/B comparison, concurrency load test, or reliable recall gate has yet been established by this review.

The compact per-query evidence is in [track-108-validation-benchmark-2026-09-08.json](/Users/seshadri/project/sangeetha-grantha/application_documentation/10-implementations/track-108-validation-benchmark-2026-09-08.json).

## Functional value enabled by this foundation

These are proposed increments, not approved implementation scope. Prioritize dependable retrieval and source identity before adding generated answers.

| Order | Addition | Concrete user value | Reuse and next work | Relative effort |
|:---|:---|:---|:---|:---|
| 1 | Similar compositions on a krithi page | Discover related repertoire from a known piece, without inventing a query | Reuse overview vectors; exclude the current composition; bind one profile; diversify composer/raga; describe similarity as text/metadata similarity, not melodic equivalence | Small–medium |
| 2 | Remembered-line finder with exact passage navigation | Search a fragment, see the matching Pallavi/Anupallavi/Charanam, and open it in the preferred script | Return document, section, variant and language/script identifiers; generate a passage-focused snippet; link to the section; use meaningful lexical/semantic labels | Medium |
| 3 | Trustworthy cycle and temple exploration | Browse all five Pancha Bhuta pieces or a Navavarnam cycle, with membership and ordering visible | Use curated relational tags and provenance for membership; add cycle/temple/deity filters; semantic retrieval supplies discovery and synonyms, while relational membership establishes completeness | Medium |
| 4 | Natural-language search with visible filters | Turn “Tyagaraja in Charukesi” into editable composer/raga chips; add tala, form, deity, temple and language | Resolve canonical entities and aliases, apply deterministic constraints, and show uncertain interpretations before applying them | Medium |
| 5 | Curator duplicate/variant suggestions | Surface likely duplicate imports and related lyric variants across scripts | Use vector candidates with title/raga/composer evidence; review side by side; retain a human decision and provenance rather than automatic merges | Medium |
| 6 | Search feedback and index-health view | Let curators flag a wrong result and identify missing/stale compositions before users encounter them | Capture relevance judgments, profile/version, coverage, pending/failed re-embeds and latency; feed reviewed examples into the benchmark | Small–medium |
| 7 | Saved repertoire collections | Save a useful result set for study, teaching or concert preparation | Persist stable composition IDs and curator notes; preserve a search separately when dynamic results are intended | Medium |
| Later | Source-grounded explanation and comparison | Explain why a piece matches, or compare versions with citations | Add reviewed translation/commentary documents and precise provenance; evaluate answer support and uncertainty independently from retrieval | Large |

The best immediate product increment is **similar compositions plus a useful remembered-line result**. Both reuse the existing investment directly. Cycle discovery is the next strong addition once membership is curated. Audio/humming identification and musical-performance recommendations require separate audio/domain evaluation; text embeddings do not demonstrate those capabilities.

## Suggested corrective sequence

1. Repair candidate retrieval, bind one profile, fix blank-key/error behavior, and redact credential logs.
2. Consolidate indexing; implement transactional failure recovery, audit entries, true dry-run, freshness and deletion reconciliation.
3. Correct pagination, request lifecycle, evidence snippets and score labels.
4. Establish canonical-ID relevance fixtures and populated API tests; compare lexical/semantic/hybrid modes and agree a release threshold.
5. Update track acceptance records, schema/API documentation and reproducible infrastructure pins; then add the first functional increments above.

Ref: application_documentation/10-implementations/track-108-semantic-search-gemini-embedding-2.md

---

[Section index](./README.md) · [Documentation home](./../README.md) · [Feature status](./../01-requirements/features/README.md)
