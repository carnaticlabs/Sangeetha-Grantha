| Metadata | Value |
|:---|:---|
| **Status** | Completed |
| **Version** | 1.0.0 |
| **Last Updated** | 2026-11-02 |
| **Author** | Sangeetha Grantha Team |

# Track: Mobile Preview (Admin Web)

**ID:** TRACK-158
**Status:** Completed (2026-11-02): implemented and verified; changes are uncommitted on branch `track-158-mobile-preview`
**Owner:** Seshadri
**Created:** 2026-11-02
**Updated:** 2026-11-02

## Goal

Give curators a read-only **Mobile Preview** in the admin web app. Right after an edit, a curator can see (A) how the krithi, its lists and its browse views will look in the Rasika mobile app, and (B) what a rasika will get when they search, in the same order, with the reason each result matched and a warning when the search index is behind the edit.

## Context

- **Mobile app:** Rasika, Compose Multiplatform, in `modules/shared/presentation` (UI and presenters) and `modules/shared/mobile-data` (V2 client). Tracks [TRACK-138](TRACK-138-rasika-mobile-app.md), [TRACK-140](TRACK-140-rasika-discovery-experience.md), [TRACK-143](TRACK-143-rasika-semantic-search.md), [TRACK-150](TRACK-150-rasika-collections-and-lakshana-reader.md) to [TRACK-153](TRACK-153-rasika-lakshana-facets.md), [TRACK-157](TRACK-157-macro-search-followups.md).
- **Admin web:** `modules/frontend/sangita-admin-web`. The editor is `src/pages/KrithiEditor.tsx` with tabs from `src/types/krithi-editor.types.ts`; the list and its Hybrid/Semantic search are `src/pages/KrithiList.tsx`; the API client is `src/api/client.ts`.
- **Hard constraint for drafting this track:** no code was changed, no paid Gemini call or embedding run was made, and no live API or database was queried. Everything below comes from reading the repository. Items that need a running stack to confirm are marked **verify** and are the first Plan phase.

## Intent

**Status:** Accepted
**Accepted by:** Seshadri (chat)
**Accepted at:** 2026-11-02

### Problem

1. **A curator cannot see the mobile result of an edit without building mobile.** The mobile reader orders sections, labels ragamalika ragas, picks the lyric script and shows cycle context in ways the admin editor does not show.
2. **A curator cannot see what a rasika would find.** The admin console's discovery search and the mobile search are different code paths with different visibility, filters and result groups (see the discrepancy list in the Spec). A curator who checks the admin search has not checked what mobile users get.
3. **An edit does not reach hybrid or semantic search by itself.** Nothing in the backend writes `search_documents`; only the Python worker does (`embed_catalogue.py`). So a saved edit can be correct in the catalogue and absent or out of date in search, and nothing tells the curator.

### Proposed outcome

A "Mobile Preview" that is read-only and built on the same data source and the same search code as mobile, with these properties:

1. **Browse and detail** (Phase A): the krithi reader, Home, Explore lists, ragas and composers, collections and kshetras, rendered in a phone frame from the mobile V2 DTOs.
2. **Search** (Phase B): every search feature the mobile app has, running through `RasikaSearchService` and the catalogue search, with a "why it matched" panel and an index-freshness banner.
3. **Unpublished and just-saved edits are visible** to the preview, through a curator-only preview scope, without changing what mobile users see.
4. **No new mutations** and nothing written to mobile analytics.

### Constraints

- Fidelity comes from calling the same services and mappers as the V2 routes; the preview does not re-query admin data and re-derive results.
- No change to the V2 routes, their DTOs or their ranking. `CatalogueSearchResponseDto` stays as is (TRACK-157 constraint).
- Read-only. No `AUDIT_LOG` requirement arises (see Spec, "Audit").
- Any path that calls Gemini must be rate-limited and cached in preview (Spec, "Cost").
- Live runs (query embeddings, Playwright against the dev stack) need Seshadri's explicit go-ahead per run, as in TRACK-157.
- Follow the `react-vite-frontend` skill (Bun, strict TS, function components, Tailwind) and the `ktor-exposed-backend` skill.

### Open questions (decisions for Seshadri; recommendations in the Spec)

1. Approach: React phone-frame (recommended) or embed the Compose web target?
2. Backend: curator-only preview endpoints that reuse the V2 services (recommended), or call the V2 routes as they are and give up previewing drafts?
3. Placement: editor tab plus a standalone route (recommended), or a side panel?
4. Should search preview show unpublished krithis? Recommended: off by default (mobile parity), one toggle to include them, always labelled.
5. Is a timestamp-based "index may be out of date" signal acceptable for the first release, with an exact hash check deferred?
6. Rate limit for preview search (recommended: 20 uncached embeddings per minute per user).

## Spec

**Status:** Accepted
**Accepted by:** Seshadri (chat)
**Accepted at:** 2026-11-02

### Evidence read (paths)

**Mobile UI and navigation** (`modules/shared/presentation/src/commonMain/kotlin/com/sangita/grantha/shared/presentation/`)
- `navigation/RasikaDestination.kt`: tabs Home, Explore, Library, Settings; destinations Browse, Collections, CollectionDetail, Kshetras, KshetraDetail, RagaDetail, ComposerDetail, KrithiReader; back stack capped at 8.
- `reader/KrithiReaderScreen.kt`, `KrithiReaderPresenter.kt`, `SectionLabels.kt`, `TamilNotation.kt`: Lyrics pane and Details pane; stored section label wins over a fallback; Varnam charanam caption; swara sections shown preformatted; ragas bound to a section only by `sectionId`; script chosen from the user's preferred script; Tamil digit markers shown as superscripts (Domain Model §6.5); facet chip row; reading completeness; cycle context (`collections/CycleContextSection.kt`).
- `home/HomeScreen.kt`, `browse/BrowseScreen.kt`, `browse/BrowsePresenter.kt`, `explore/ExploreFilters.kt`, `entities/RagaDetailScreen.kt`, `ComposerDetailScreen.kt`, `collections/CollectionScreens.kt`, `kshetra/KshetraScreens.kt`, `favourites/`, `preferences/`.
- `search/SearchPresenter.kt`, `search/SearchScreen.kt`, `components/MacroHitCard.kt`, `DiscoveryKrithiCard.kt`, `KrithiCard.kt`, `components/LoadStateContent.kt`.
- `theme/RasikaTheme.kt` (design tokens to port), `RasikaCopy.kt` (every user-facing string).

**Mobile data path** (`modules/shared/mobile-data/src/commonMain/kotlin/com/sangita/grantha/shared/mobile/`)
- `network/CatalogueApi.kt`, `KtorCatalogueApi.kt`: the exact endpoints. `repository/CatalogueRepository.kt`: `searchHybrid`/`searchSemantic` call `searchHybridWithCards`/`searchSemanticWithCards`, so mobile search uses the **V2** `POST /v2/catalogue/search/{hybrid|semantic}`.
- `usage/MobileSession.kt`: session and interaction ids sent as `X-Rasika-Session-ID` and `X-Rasika-Interaction-ID`.
- `storage/` and `repository/FavouritesRepository.kt`, `PreferencesRepository.kt`: on-device only.

**Backend**
- `modules/backend/api/.../routes/RasikaCatalogueRoutes.kt`: the `/v2/catalogue/*` reads, all with `CatalogueVisibility.ALL_PUBLISHED`, anonymous. `RasikaSearchRoutes.kt`: the two V2 search posts, anonymous and published-only.
- `services/RasikaSearchService.kt` (floors per embedding profile, cap 3 macro cards, `exactTitleMatchId`), `services/HybridSearchService.kt` (`prepareQuery`, profile compatibility check, in-process LRU of 256 query vectors), `services/CatalogueService.kt`, `services/CatalogueUsageRecorder.kt`, `plugins/CatalogueUsage.kt`, `plugins/RequestValidation.kt` (global limit 100 requests per minute per IP), `plugins/Routing.kt` (admin routes behind `requireRole(Roles.ADMIN)`).
- `modules/backend/dal/.../repositories/CatalogueRepository.kt` (`publishedComposition`, `krithiFilter`, `searchKrithis`), `KrithiSearchRepository.kt` (dense candidates, trigram lexical branch, RRF `1/(60+rank)`), `MacroSearchRepository.kt`, `models/CatalogueVisibility.kt`.
- `modules/shared/domain/.../model/catalogue/*.kt` and `SearchDtos.kt`: the DTOs mobile consumes.
- `database/migrations/V58__semantic_search_pgvector.sql`: `search_documents`, `document_embeddings`, `embedding_profiles`.

**Admin web**
- `src/api/client.ts`: `searchKrithis` → `GET /v1/admin/krithis/search`; `searchDiscoveryKrithis` → `POST /v1/search/{hybrid|semantic}` (V1).
- `src/pages/KrithiList.tsx`, `KrithiEditor.tsx`, `src/components/krithi-editor/*`, `src/App.tsx` (routes).

**Docs:** `application_documentation/03-api/search.md`, `09-ai/embeddings.md`, `01-requirements/domain-model.md` §6, TRACK-152, TRACK-157.

### Findings that drive the approach

**F1. Mobile search is the V2 route, which is anonymous and published-only.** The V2 handlers never read the admin role (`RasikaSearchRoutes.kt` comment). A curator's admin token does not change what V2 returns. So calling V2 as it is cannot preview a draft, and is exactly right for checking what published mobile users get.

**F2. Result parity is decided on the server; the client adds little.** `SearchPresenter` only (a) sends the trimmed query with null `composerId`/`ragaId` and `limit` 30 for Hybrid/Semantic, (b) drops KSHETRA cards if the build has no kshetra screen, (c) renders `exactTitleMatchId` first, then macro cards, then the other kritis in server order, and (d) pages Lexical results 30 at a time up to 3 pages. Ranking, floors, caps and eligibility are all in `RasikaSearchService` and the repositories. Reusing those gives parity by construction.

**F3. Lexical and vector search read different data.** Catalogue lexical search (`CatalogueRepository.krithiFilter`) reads the live `krithis`, `composers` and `ragas` tables, so it reflects an edit at once. Hybrid and Semantic read `search_documents` and `document_embeddings`, which only the Python worker writes (no backend file other than the repositories mentions them). An edit is therefore visible in Titles search and can be missing or stale in All matches / Related meanings until `embed_catalogue.py` runs. `embeddings.md` says so: "new or corrected content is not automatically re-embedded".

**F4. Query embedding is the only paid step and it is already cached once.** `HybridSearchService.prepareQuery` embeds the query once and `RasikaSearchService` reuses that vector for the kriti and macro retrievals. The 256-entry LRU is keyed on the trimmed query string. Lexical mode makes no Gemini call.

**F5. Mobile analytics is a log line, not a table, and it is keyed on the path.** `plugins/CatalogueUsage.kt` records route, status, elapsed time and the two session headers for paths starting `/v1/catalogue` or `/v2/catalogue`, through `CatalogueUsageRecorder` (a logger sink named `catalogue-usage`). No stored recent searches, saved searches or search-query table exists in the backend. Preview routes outside those prefixes and without the `X-Rasika-*` headers are never recorded.

**F6. The mobile app has no autocomplete, suggestions, recent searches, saved searches or sort control.** A search of `presentation` and `mobile-data` for these found none. Favourites and bookmarks exist but are on-device (`FavouritesRepository`, `LocalDocumentStore`).

**F7. No Compose web target exists.** `modules/shared/presentation/build.gradle.kts` declares android, jvm, `iosArm64` and `iosSimulatorArm64` only; `domain` and `mobile-data` were not found to declare a wasm or js target either (verify in Phase 0).

### Discrepancies between mobile and admin (to be surfaced, not hidden)

| # | Aspect | Mobile (V2) | Admin today | Effect on a curator |
|:--|:--|:--|:--|:--|
| D1 | Visibility of krithis | `workflow_state = PUBLISHED` only (`publishedComposition`) | `GET /v1/admin/krithis/search` uses `publishedOnly = false` | Admin list shows krithis mobile users cannot find |
| D2 | Search endpoint | `POST /v2/catalogue/search/*` (macro cards, `exactTitleMatchId`) | `POST /v1/search/*` (kriti items only) | Admin never shows collection and kshetra cards or exact-title-first order |
| D3 | Search visibility | Forced published-only | `publishedOnly = !hasAdminRole()` (admin sees unpublished indexed documents) | Admin hybrid results can include krithis absent for mobile |
| D4 | Filters on discovery | `composerId`/`ragaId` always null; filters apply to Lexical only | `KrithiList` can send composer and raga to hybrid | Same query, different candidate set |
| D5 | Result limit | 30 (`DISCOVERY_LIMIT`); Lexical 30 per page, at most 3 pages | Hybrid/Semantic `limit: 30` (`KrithiList.tsx`); admin lexical page size 50 by default (`AdminKrithiRoutes.kt`) | Same discovery length; lexical pages differ (30 vs 50) |
| D6 | Lexical match fields | Title, incipit, composer name/alias, raga name/alias; ordered by normalised title ascending | Admin search also has `lyric`, tala, deity, temple, language filters | Different hit sets and order |
| D7 | Mode label vs behaviour | Lexical mode is labelled "Titles & lyrics" (`RasikaCopy.MODE_LEXICAL`) but `krithiFilter` has no lyric clause | n/a | **Candidate mobile defect**: verify in Phase 0 with a lyric-only phrase |
| D8 | Filter pickers | `loadFilterOptions` loads only the first page (30) of ragas and composers | Admin pickers load full reference data | **Candidate mobile limitation**: verify in Phase 0 |
| D9 | Visibility modes | V2 uses `ALL_PUBLISHED`; V1 catalogue `LEGACY_KNOWN_FORMS` hides `UNESTABLISHED` forms | n/a | The preview must use `ALL_PUBLISHED` |
| D10 | Index freshness | Hybrid/Semantic reflect the last worker run | Same, but admin never says so | Preview adds a freshness banner |

D7 and D8 are suspected from reading code; they are not confirmed.

### Requirements

**A. Browse and detail**

1. A "Mobile Preview" renders, inside a phone frame, the mobile screens that show catalogue data: Home (discovery), Explore (Krithis, Ragas, Composers), krithi reader (Lyrics and Details panes), raga detail, composer detail, collections (list and detail), kshetras (list and detail), and Browse with lakshana facets.
2. Content comes from curator-only preview endpoints that call the same `CatalogueService` methods and mappers as `/v2/catalogue/*`, with a scope that includes unpublished krithis (Backend design).
3. The reader applies the presentation rules in `SectionLabels`, `TamilNotation` and the section-order rules for the musical form (Domain Model §6.1 to §6.5), including ragamalika raga display and Varnam captions. Where the preview ports a rule to TypeScript, a shared golden fixture test proves the port agrees with the Kotlin rule (Parity guard).
4. The preview exposes the user preferences that change what is shown: preferred script, text size, light/dark appearance. They are preview-local (browser storage) and are never sent to the server.
5. A krithi that is not published shows a "Not published: mobile users cannot see this" banner.
6. After a save in the editor the open preview refreshes without a page reload (Refresh design).

**B. Search**

7. Search uses the exact V2 pipeline. Modes: All matches (Hybrid, default), Related meanings (Semantic), Titles & lyrics (Lexical), behind the same "Search options" disclosure; categories Krithis, Ragas, Composers.
8. Hybrid/Semantic requests send what mobile sends: trimmed query, null `composerId`/`ragaId`, `limit` 30. Lexical uses `GET` with page size 30 and at most 3 pages; Ragas directory unbounded pages; Composers 3 pages.
9. Result groups render in mobile order: exact-title kriti first, then collection and kshetra cards (at most 3), then the other kritis in server order. Empty, no-results, loading and error-with-retry states match `LoadStateContent` and `RasikaCopy`.
10. **Why it matched** (kritis): document kind (`COMPOSITION_OVERVIEW` or `SECTION_PASSAGE`), matched passage, `similarityScore`, `lexicalScore`, `rrfScore`, and, where the repository can supply them, the vector rank and lexical rank. **Cards:** similarity, lexical score, the floor in force and the gap over the 4th anchor. **Near-miss cards:** candidates that did not clear the floor are listed with the number that kept them out, so a curator can see why a card is absent.
11. **Index freshness:** for the krithi being edited, an index-status call reports whether it has search documents, whether they are published, whether embeddings exist for the active profile, and whether the source was changed after the documents were written. The UI shows "Index out of date for this krithi: All matches and Related meanings may show old text. Titles & lyrics is current." Staleness is never silent.
12. **Scope toggle:** "As mobile (published only)" is the default. "Include unpublished" runs the same pipeline with `publishedOnly = false`, and every result that mobile would not return is badged.
13. Hybrid/Semantic search runs only on explicit submit (Enter or button), as on mobile; no search-as-you-type.
14. Search is read-only and writes nothing, including nothing to the usage log.

**C. Safety and cost**

15. Preview routes are admin-only (`requireRole(Roles.ADMIN)`).
16. Preview search is rate-limited per user (Cost).
17. Preview routes live outside `/v1/catalogue` and `/v2/catalogue`, and the preview client sends no `X-Rasika-*` headers.
18. The V2 routes, their DTOs and their results for non-preview callers are byte-for-byte unchanged (integration test).

### Approach decision: React phone-frame, not embedded Compose web

| | (a) React/Tailwind phone-frame (recommended) | (b) Compose Multiplatform web (wasm) |
|:--|:--|:--|
| Fits the admin stack | Yes: Bun, Vite, TanStack Query, existing auth and API client | New Gradle web target, new bundle, a second toolchain inside the admin build |
| Today's state | No new build target | No web target exists (F7); needs one on `domain`, `mobile-data` (HTTP client engine, storage `expect/actual`) and `presentation` |
| Pixel fidelity | Close, not identical; depends on porting `RasikaTheme` tokens and layouts | Exact, same composables |
| Presenter-logic fidelity | A small ported set (labels, Tamil digits, ordering, card filter) guarded by golden fixtures | Exact, the same `SearchPresenter` |
| **Search parity** | **Same as (b), because results are decided on the server (F2). Only the ~4 rules in F2 are client-side** | Marginally easier for client rules, no help for ranking |
| Reflecting drafts and index status | Needs preview endpoints either way | Needs preview endpoints either way, plus a way to swap the client base URL and scope |
| Risk | Visual drift over time | Web target maturity, bundle size and load time unverified here; platform code (storage, files) must be made web-safe |

**Recommendation: (a).** It costs the least, uses the existing admin conventions, and gets search parity from the single server pipeline. The drift risk is handled by a golden-fixture contract (Parity guard). (b) stays a documented alternative if visual drift proves costly; its feasibility (CMP web status for the versions in `current-versions.md`) was not evaluated here.

**Alternative also rejected: (c) compile only `mobile-data` and the presenters to Kotlin/JS and keep React UI.** It would share `SearchPresenter` logic exactly but needs the same new web target as (b) for no UI benefit.

### Backend design (needs `ktor-exposed-backend`)

Options for reading drafts, with trade-offs:

| Option | How | For | Against |
|:--|:--|:--|:--|
| 1. Call V2 as it is | Admin web calls `/v2/catalogue/*` | Zero backend change; mobile-identical | Cannot show drafts or just-saved edits; its calls enter mobile analytics logs unless headers are dropped (the route category is still logged) |
| **2. Curator-only preview routes that reuse the services (recommended)** | New `/v1/admin/preview/...` routes call `CatalogueService` and `RasikaSearchService` with a `PreviewScope` | Same mappers and pipeline; shows drafts; outside the analytics prefix; admin-gated | New routes to maintain; scope parameter must be threaded through services and repositories |
| 3. Preview database or snapshot | Separate store with draft data | Isolation | Not the same data source, so it breaks the fidelity goal; heavy |

**Option 2 detail.**
- `PreviewScope { PUBLISHED_ONLY, INCLUDE_UNPUBLISHED }`. Today `CatalogueVisibility` has two values and `publishedComposition` ties both to `PUBLISHED`. Rather than overloading that enum, add a separate scope parameter to the catalogue repository functions the preview needs. Default stays `PUBLISHED_ONLY`, so V2 behaviour does not change. The exact threading is decided in Plan step 3 after reading all call sites.
- Content routes (all `GET`, under `/v1/admin/preview/catalogue/`): `discovery`, `krithis`, `krithis/{id}`, `krithis/{id}/lyrics/{variantId}`, `facets`, `ragas`, `ragas/{id}`, `composers`, `composers/{id}`, `collections`, `collections/{slug}`, `temples`, `temples/{id}`. Each returns the **same DTO** as its V2 twin.
- Search routes: `POST /v1/admin/preview/search/{hybrid|semantic}`. Body: `SemanticSearchRequest` plus `scope` and `explain`. The handler calls `RasikaSearchService.search` (extended with an optional scope and an optional explain sink) and returns `PreviewSearchResponse { response: CatalogueSearchResponseDto, explain?: PreviewSearchExplain }`. The wrapped `response` is the V2 DTO unchanged.
- `PreviewSearchExplain`: active profile (model, dimensions), mode, floor values in force, per-item vector and lexical rank where the SQL already computes them (the `semantic_candidates` and `lexical_search` CTEs rank rows; exposing the ranks is a Plan spike), and near-miss macro candidates with similarity, gap and the failed condition.
- `GET /v1/admin/preview/krithis/{id}/index-status` → `PreviewIndexStatus { krithiId, hasDocuments, publishedFlagMatches, embeddedForActiveProfile, sourceChangedAfterIndex, indexedAt, sourceUpdatedAt, activeProfile }`.
  - **Staleness rule (first release):** compare the latest `updated_at` among the krithi, its sections and its lyric variants with the newest `search_documents.updated_at` for that krithi, and compare `workflow_state` with `is_published`. Whether those `updated_at` columns exist on all three tables was not confirmed (a column search of the Exposed tables found none under that name): verify in Phase 0; if absent, the Plan adds the smallest signal that exists (audit log last-edit time for the entity) and says so.
  - **Exact check (deferred):** the content hash is computed by the Python context formatter. Re-implementing it in Kotlin would fork the logic, so an exact per-krithi check belongs in the worker (`embed_catalogue.py --krithi-id <id> --check`, a read-only comparison like the macro `--check` from TRACK-157). Not in this track unless Seshadri asks.
- **Cost controls:** (i) Preview search uses `HybridSearchService.prepareQuery`, so it shares the existing 256-entry query-vector cache with mobile; repeated queries and the same query in two modes cost zero or one embedding. (ii) A named Ktor `RateLimit` on the two preview search routes, keyed on the authenticated user id, default 20 requests per minute (decision 6); this applies on top of the global 100 per minute per IP. (iii) Lexical mode and every content route make no Gemini call. (iv) No search-as-you-type; the UI debounces nothing because it only sends on submit. (v) An embedding-service failure returns the same availability error mobile would get, shown as an error state, never as an empty result.
- **Read-only and analytics:** all new routes are `GET`/`POST` reads; the `POST` bodies carry a query only. Paths are outside the usage interceptor's prefixes (F5). If a later change widens that interceptor, a test must fail (Plan step 3).
- **Audit:** no mutation, so no `AUDIT_LOG` write. This is stated here because CLAUDE.md requires audit for mutations only. Route access by an admin is not audited, consistent with the existing admin `GET` routes.
- **Index freshness for macro cards:** the TRACK-157 `embed_catalogue.py --macro-only --check` already reports stale cycle and kshetra overviews. The preview banner for a collection or kshetra links to that command; it does not compute it.

### UI placement

- **Per-krithi:** a new **"Mobile Preview"** tab in the editor (`EDITOR_TABS`), shown for saved krithis (not `/krithis/new`), opened with the existing `?tab=` deep link. It shows the reader for the krithi being edited, the not-published banner, the index-status banner, and a "Search for this krithi" shortcut that prefills the query with its title.
- **Whole-app:** a standalone route **`/mobile-preview`** with the full phone frame: tab bar (Home, Explore, Library, Settings as far as read-only data allows), search, directories, collections and kshetras. Library (favourites) and Settings show the preview's own local preferences only.
- A side panel was considered and rejected as the primary placement: the editor already uses its width for forms, and a phone frame at a useful size would squeeze them. A "pop out" of the tab into `/mobile-preview?krithi=<id>` covers the side-by-side case on wide screens.

### Refresh after a save

- The editor already reloads its own state after a save (`KrithiEditor.tsx`). The preview reads through TanStack Query with a key per preview resource (`['mobile-preview', 'krithi', id, ...]`). The editor's save-success path invalidates those keys, so an open preview refetches the reader, lyrics and any list that includes the krithi.
- The index-status query is invalidated on save and re-polled every 30 seconds only while the tab is visible, so that a worker run shows up as "index current" without a reload.
- A search result list is not re-run automatically (a re-run could cost an embedding). After a save, results show "Edits saved since this search: run again" with a button.

### Mobile search feature map

Parity status: **Full** = same server code and same client rule; **Port** = client rule re-implemented in TypeScript behind a golden fixture; **Substitute** = cannot be replicated faithfully, closest equivalent given; **None** = the feature does not exist on mobile.

| Mobile feature | Where in mobile | Preview equivalent | Parity |
|:--|:--|:--|:--|
| Free-text query, submit on commit | `SearchPresenter.submit` | Same input, submit only | Full |
| Categories Krithis / Ragas / Composers | `ExploreCategory` | Same tabs | Full |
| Mode choice: All matches / Related meanings / Titles & lyrics, "Search options" disclosure | `KrithiSearchMode`, `RasikaCopy` | Same control and copy | Port (copy strings shared by fixture) |
| Hybrid and Semantic kriti ranking (RRF, one doc per krithi, overview preference) | `RasikaSearchService`, `KrithiSearchRepository` | Same service, preview endpoint | Full |
| Collection and kshetra cards, floor per profile, cap 3, one per anchor | `RasikaSearchService`, `MacroSearchRepository` | Same service; plus near-miss and floor explain | Full |
| Exact-title-first ordering | `exactTitleMatchId`, `SearchUiState.exactTitleItem` | Same rule | Port |
| KSHETRA cards dropped when the build has no kshetra screen | `displayableMacroHits` | Preview shows all kinds the destination screens support (flag fixed to current mobile value) | Port |
| Lexical krithi search with paging (30 × 3) and title-ascending order | `CatalogueRepository.searchKrithis` | Same route | Full |
| Raga and composer filter sheet (Lexical only), applied chips | `openFilters`, `applyFilters` | Same sheet and chips | Port; D8 first-30 picker limit reproduced deliberately and flagged |
| Lakshana facets in Browse with counts | `BrowsePresenter`, `/v2/catalogue/facets` | Same route and chips | Full |
| Raga and composer directory search | `searchRagas`, `searchComposers` | Same routes | Full |
| Home discovery | `HomePresenter`, `/v2/catalogue/discovery` | Same route | Full |
| Search over cycles and kshetras by name | TRACK-152 | Same service | Full |
| Multi-script lyric search | Hybrid lexical branch uses `search_documents.indexed_content` (trigram + `ILIKE`) per variant | Same service; matched script shown in explain (from `search_documents.script_code`) | Full; whether a transliterated query matches a Devanagari or Tamil passage depends on what was indexed, which the explain panel shows |
| Stale-request cancellation, loading, empty, error with retry | `generation`, `LoadState` | Same states | Port |
| Autocomplete and suggestions | not present | not built | None |
| Recent and saved searches | not present | not built | None |
| Sort control | not present (lexical is fixed title order) | not built | None |
| Session and interaction ids for usage logging | `MobileSession` | Not sent | Substitute: omitted on purpose (R17) |
| Favourites and bookmarks | on-device storage | Not replicated; shown disabled | Substitute: on-device-only |
| Device text size, system appearance, reduce-motion | platform | Preview-local toggles (text size, light/dark); reduce-motion not replicated | Substitute |
| Back stack depth cap (8) and tab stacks | `RasikaNavigator` | Same cap in the preview navigator | Port |

### Parity guard

- **Golden fixtures:** extend the shared fixtures used by `mobile-data` (`CatalogueFixtures.kt`) to emit JSON that both a Kotlin presenter test and a Vitest test read. Cases: section labels and order per form, Varnam caption, ragamalika raga display, Tamil digit markers (and non-markers), exact-title-first ordering, KSHETRA drop rule, copy strings.
- **Server parity test (integration):** with the default scope, the preview search and content responses equal the V2 responses for a set of probes taken from `evals/retrieval_benchmarks.json` that need no live embedding (Lexical) plus recorded vectors where the test stack has them.
- **Drift check in CI:** a Vitest test fails when a TypeScript DTO type in the admin web no longer matches a field list generated from the Kotlin DTO (or, if no generator exists, a hand-maintained list reviewed with each DTO change; decided in Plan step 4).

### API and DTO impact

- **New (preview only):** `PreviewScope`, `PreviewSearchRequest`, `PreviewSearchResponse`, `PreviewSearchExplain`, `PreviewIndexStatus`; routes under `/v1/admin/preview/`.
- **Unchanged:** every V1 and V2 route and DTO. `openapi/sangita-grantha.openapi.yaml` gains the preview routes; `application_documentation/03-api/api-contract.md` and `search.md` get a pointer.
- **Database:** no migration expected. If the staleness signal needs a column that does not exist, that is raised as an open item for Seshadri, not added silently.

### Risks

| Risk | Mitigation |
|:--|:--|
| A preview that is not identical to mobile gives false confidence | Same services and DTOs; parity tests; discrepancy list is shown in the UI help; the scope default is mobile-identical |
| Scope flag leaks drafts to mobile users | Default `PUBLISHED_ONLY`; V2 routes never read the flag; integration test that a draft is 404 on V2 and visible on preview |
| Staleness heuristic is wrong (false stale or false fresh) | Labelled "may be out of date"; shows both timestamps; exact check deferred to the worker |
| Gemini cost from repeated preview searches | Shared query-vector cache, per-user limit, submit-only, no search-as-you-type |
| Visual drift between React and Compose | Tokens ported from `RasikaTheme.kt`; golden fixtures; (b) as fallback |
| `D7` or `D8` turn out to be real mobile defects | Reported to Seshadri as findings with a separate fix track; this track only reproduces behaviour |
| The scope parameter touches many repository functions | Plan step 3 starts with a call-site inventory; the change ships behind tests before any UI depends on it |
| A new route widens the admin attack surface | `requireRole(ADMIN)`, GET and query-only POST, no body persisted |

### Spec open questions

Same list as in the Intent; recommendations are in the sections above.

## Plan

**Plan Status: Accepted** (all steps done 2026-11-02; accepted 2026-11-02, Seshadri, chat: "Accept Plan"). Decisions taken as recommended in the Spec: React phone frame; preview endpoints reusing V2 services; editor tab plus `/mobile-preview`; draft search off by default with a labelled toggle; timestamp staleness heuristic first; 20 uncached embeddings per minute per user.

### Files expected to change

- **Backend:** `modules/backend/dal/.../repositories/CatalogueRepository.kt`, `KrithiSearchRepository.kt`, `MacroSearchRepository.kt` (scope parameter and, if feasible, rank columns); `modules/backend/api/.../services/CatalogueService.kt`, `RasikaSearchService.kt`, new `PreviewIndexStatusService.kt`; new `routes/PreviewRoutes.kt`; `plugins/Routing.kt`, `plugins/RequestValidation.kt` (named rate limit); `modules/shared/domain/.../model/catalogue/PreviewDtos.kt`.
- **Frontend:** new `src/pages/MobilePreview.tsx`, `src/components/mobile-preview/*`, `src/api/previewApi.ts`, `src/hooks/useMobilePreview.ts`, `src/types/mobilePreview.types.ts`; `src/App.tsx` (route), `src/types/krithi-editor.types.ts` and `src/pages/KrithiEditor.tsx` (tab), `src/components/Sidebar.tsx` (link).
- **Tests:** backend unit and integration tests; Vitest component tests; shared golden fixtures; Playwright flow in `e2e/`.
- **Docs:** `03-api/search.md`, `03-api/api-contract.md`, `openapi/sangita-grantha.openapi.yaml`, an admin-web feature page, `conductor/tracks.md`.
- **No migration planned.**

### Live runs (each needs Seshadri's explicit authorisation)

- Any request that embeds a query (Hybrid or Semantic through the real Gemini client).
- Any run against the dev database or the dev stack (`make dev`) beyond read-only catalogue GETs.
- Playwright against the dev stack.

### Order of work

Phase 0 and A are independently shippable; B depends on 0 and on A's shell.

| # | Step | Check (paste output before reporting done) |
|:--|:--|:--|
| **Phase 0: Verify what this Spec could only read** | | |
| 0.1 | Confirm which of `krithis`, `krithi_sections`, `krithi_lyric_variants` have an `updated_at` (or equivalent) and what `search_documents.updated_at` records; decide the staleness signal. Confirm `domain` and `mobile-data` have no web target. Read all call sites of `publishedComposition`. | Written findings in this file's Progress Log; no code |
| 0.2 | Reproduce D6 to D8 in the dev stack with Lexical and catalogue GETs only (no Gemini): a lyric-only phrase through `/v2/catalogue/krithis`; the first-page limit of the filter pickers; the admin lexical ordering. | Curl output and screenshots recorded; D7/D8 marked confirmed or dropped; Seshadri told, separate fix tracks proposed if confirmed |
| **Phase A: Browse and detail** | | |
| A1 | **Backend scope.** Add `PreviewScope` and thread it through the catalogue read paths; add the content routes under `/v1/admin/preview/catalogue/`. | `make test`; `make test-integration`: (i) default scope equals V2 for the same ids, (ii) a draft is 404 on V2 and 200 on preview, (iii) non-admin gets 403, (iv) no row is added to `AUDIT_LOG` |
| A2 | **Shell.** Route `/mobile-preview`, phone frame, preview navigator (stack cap 8), tokens ported from `RasikaTheme.kt`, TypeScript DTO types and API client, TanStack Query keys. | `make test-frontend`; `bun run lint`; `bun run build` |
| A3 | **Reader.** Sections, labels, ragamalika, Varnam caption, preformatted swara, script switch, Tamil digit superscripts, Details pane, cycle context, facet chips, not-published banner. | Vitest with golden fixtures; `make test-frontend`; browser: open three krithis (a Krithi, a Varnam, a Ragamalika) and compare against the mobile simulator screenshot |
| A4 | **Lists and browse.** Home, Explore (directories), raga and composer detail, collections, kshetras, Browse facets. | `make test-frontend`; browser walk through each screen |
| A5 | **Editor tab and refresh.** "Mobile Preview" tab for saved krithis; save invalidates preview queries. | Vitest for tab visibility; browser flow: edit a section label, save, see the preview change without reload |
| **Phase B: Search** | | |
| B1 | **Search backend.** Preview search routes with scope and explain; near-miss macro candidates; rank columns (spike first; if not additive, ship without ranks and say so); per-user rate limit; index-status route and service. | `make test`; `make test-integration`: default-scope parity with `RasikaSearchService`; rate limit returns 429 on the 21st request in a minute; usage interceptor records nothing for preview paths; stale/fresh/unindexed status cases. Live Hybrid/Semantic probe only after authorisation |
| B2 | **Search UI.** Modes and options disclosure, Lexical paging, filter sheet, exact-title-first, macro cards with destinations, loading, empty, error and retry states. | `make test-frontend`; browser: run five queries per mode (title, incipit, raga, a thematic phrase, a no-result phrase) and compare to the mobile simulator |
| B3 | **Explain and freshness.** Why-matched panel (kind, passage, scores, ranks, floor and near-misses), scope toggle with badges, index-status banner, "edits saved since this search" prompt. | `make test-frontend`; browser: edit a lyric, save, confirm the banner says out of date; after an authorised worker run confirm it says current |
| **Phase C: Guard, docs, close** | | |
| C1 | **Parity guard.** Golden fixtures shared by Kotlin and Vitest; DTO drift check; server parity test with the benchmark probes that need no live call. | `make test`; `make test-frontend`; `make test-integration` |
| C2 | **Docs and register.** Update `search.md`, `api-contract.md`, OpenAPI, an admin-web feature page, the track file; list D1 to D10 outcomes. | `make check-docs`; `python3 conductor/check-registry-sync.py` |
| C3 | **End-to-end.** One Playwright flow: edit → save → preview reader updates → search shows the freshness banner. | `make test-frontend`; Playwright run (authorised) |

### Completion evidence (fixed)

- Each step's check output pasted in the Progress Log.
- Every discrepancy D1 to D10 has an outcome: surfaced in the UI, confirmed or dropped, or turned into a separate track.
- Statement that no new mutation and no new `AUDIT_LOG` requirement was introduced, with the integration test that shows it.
- Statement of what was not verified (for example live Gemini probes that were not authorised).

### Out of scope

- Editing from the preview; any mutation.
- Autocomplete, recents, saved searches or sorting (the mobile app has none).
- Changing mobile behaviour, including fixes for D7 or D8.
- An exact per-krithi staleness check (deferred to the worker).
- Re-embedding from the admin web.
- The Compose web target (alternative (b)).

## Progress Log

- 2026-11-02: Track drafted (Intent, Spec, Plan: Proposed). Read-only investigation only; no code changed, no live call made. Waiting for Seshadri's decisions and Plan acceptance.
- 2026-11-02: Seshadri accepted the Plan in chat with the Spec's recommendations. Implementation has not started; next is Phase 0.
- 2026-11-02 **Phase 0.1 done (read-only, no code changed, no live call).**
  - **Update timestamps exist and are maintained.** `updated_at` is present on `krithis`, `krithi_sections`, `krithi_lyric_variants` and `krithi_lyric_sections` (`CoreTables.kt`), and the DAL sets it on every edit (`KrithiRepository.kt` lines 145, 195, 373, 387; `KrithiLyricRepository.kt` lines 151, 210, 261, 273). Lyric text lives in `krithi_lyric_sections`, so the staleness rule must include that table, not only variants. The earlier "not found" note in the Spec came from a faulty search; no audit-log fallback is needed.
  - **The timestamp heuristic has a known false-stale case.** The worker (`batch_embed_catalogue.py` lines 289 and 376) skips a document when its content hash is unchanged and then does not touch `search_documents.updated_at`. So an edit that does not change the indexed text (for example a field the formatter ignores) stays "newer than the index" even after a re-embed. It also cannot see renames of a raga or composer, which change the header text but not the krithi's `updated_at`. The banner wording stays "may be out of date" and shows both timestamps. A reliable fix needs the worker to record a check time, or the exact per-krithi `--check` already deferred. **Decision needed from Seshadri** (see wrap-up).
  - **No web target.** `modules/shared/*/build.gradle.kts` contain no wasm or js target (grep empty). F7 confirmed.
  - **`publishedComposition` call sites:** 11 in `CatalogueRepository.kt` (the only main-source file using it); tests in `CatalogueVisibilityTest`, `CatalogueReaderTest`, `CatalogueRoutesTest`. This bounds the A1 scope-threading work.
  - **D7 confirmed by code** (not yet by a live query): `krithiFilter` matches title, incipit, composer name/alias and raga name/alias only; there is no lyric clause. **D8 confirmed by code**: `loadFilterOptions` requests one page of 30 ragas and composers.
  - **0.2 not started:** the backend is not running (`localhost:8080` refused), and starting the dev stack is a live run that needs Seshadri's authorisation.
- 2026-11-02 **Phase 0.2 done (dev stack started with Seshadri's go-ahead; unauthenticated lexical and catalogue GETs only; no Gemini call, no admin login, nothing written).** Backend started via the `backend` launch config against the already-running dev database, then stopped.
  - **D7 confirmed live.** The phrase `nIvE(y)anucunu bhakti kUDina` is taken from a charanam of *Aada Modi Galadae* (not in its title, incipit, composer or ragas). `GET /v2/catalogue/krithis?query=<phrase>` returned `total: 0`, while Lexical mode is labelled "Titles & lyrics". The label promises lyric search the route does not do. Candidate fix (separate track, not this one): rename the mode, or add a lyric clause to `krithiFilter`.
  - **D8 confirmed live, scope narrower than suspected.** The dev catalogue has 1,014 ragas and 3 composers. The raga picker loads one page of 30, so 984 ragas cannot be chosen from the filter sheet; the composer picker is complete. The directory screen is not affected (ragas page without a cap). Candidate fix (separate track): search-as-you-type picker or paging.
  - **D6 admin ordering not resolved.** `GET /v1/admin/krithis/search` needs an admin token, which was not used, and the ordering is not in `KrithiRepository`. Left for A1, when the preview and admin routes are compared side by side.
  - **Phase 0 complete except D6 ordering.** Both confirmed defects are proposed as separate tracks and are not fixed here (out of scope).
- 2026-11-02 **A1 done (branch `track-158-mobile-preview`, uncommitted).**
  - **Design change from the Spec, simpler and smaller:** instead of threading a `PreviewScope` parameter through the 11 `publishedComposition` call sites, `CatalogueRepository` takes the scope once (`PreviewScope.PUBLISHED_ONLY` by default; `previewing()` returns an `INCLUDE_UNPUBLISHED` copy) and `publishedComposition` reads it. `CatalogueService.previewing()` wraps that repository. The V2 routes are mounted a second time at `/v1/admin/preview/catalogue` inside `requireRole(ADMIN)` with the same handlers (`rasikaCatalogueRoutes(service, root)`), so the DTOs and error handling cannot diverge.
  - **Scope rule:** preview shows draft, in-review and published krithis; archived stays hidden (decided here; Spec said "unpublished").
  - **Files:** `dal/models/PreviewScope.kt` (new), `dal/repositories/CatalogueRepository.kt`, `api/services/CatalogueService.kt` (repository resolved per call so existing mock-based tests still hold), `api/routes/RasikaCatalogueRoutes.kt`, `api/plugins/Routing.kt`, `api/.../routes/PreviewCatalogueRoutesTest.kt` (new, 5 tests).
  - **Checks:** `PreviewCatalogueRoutesTest` 5/5 passed: a draft is 404 on V2 and 200 on preview; archived is 404 on preview; for a published krithi preview and V2 return byte-identical bodies for `krithis/{id}`, `krithis?query`, `ragas`, `composers`, `discovery`, `collections`; anonymous gets 401 and a non-admin 403; preview reads leave `audit_log` unchanged. `make test` BUILD SUCCESSFUL (after fixing one regression my first version caused in `CatalogueServiceTest`: an eager `dal.catalogue` read; now lazy). `make test-integration` BUILD SUCCESSFUL, no failing result files.
  - **Known limits to confirm in A4:** collection and kshetra eligibility (`reviewedCollections`, `TempleEligibility`) use their own published rules, so the preview's collection and kshetra lists may not yet include unpublished members; counts that go through `publishedComposition` do follow the scope. The usage interceptor ignores the preview path by prefix; the test that pins that is part of B1.
  - **Not verified:** nothing run against the dev stack for A1; no live Gemini call.
- 2026-11-02 **A2 to A5 done (uncommitted).** Seshadri asked for all pending work to be completed in sequence without further approval prompts; recorded here as the go-ahead for the dev-stack runs below (dev admin login used the project's own e2e dev defaults via `/v1/auth/token`; no Gemini call).
  - **A2 shell:** `/mobile-preview` route and sidebar link; `PhoneFrame` (375×812), tokens ported from `RasikaTheme.kt` (`previewTokens.ts`), pure navigator port with the depth cap of 8 (`previewNavigator.ts`), TypeScript DTO types mirroring the Kotlin DTOs (`types/mobilePreview.types.ts`), preview API client and query keys (`api/previewApi.ts`, `request` exported from `client.ts`), preview-local preferences in localStorage.
  - **A3 reader:** `readerLogic.ts` ports `SectionLabels`, `TamilNotation`, variant choice and copy; `fixtures/readerRules.golden.json` pins them (Kotlin-side reader of the same file is still C1). `KrithiReaderView` shows header facts, Lyrics and Details panes, script and source chips, varnam caption, bound-raga labels, ragamalika membership, cycle context, completeness note. The not-published banner reads the workflow state from the existing admin `GET /v1/admin/krithis/{id}`; the wire field is `workflowState`, but the admin TypeScript type calls it `status` (a pre-existing mismatch, worked around in `usePreviewReader.ts`).
  - **A4 screens:** Home, Explore directories (Krithis, Ragas, Composers with lexical search for now), Browse facets, raga and composer detail, collections list and detail, kshetras list and detail; all with loading, error and empty states. Search modes, macro cards and the filter sheet are B2.
  - **A5:** "Mobile Preview" editor tab (saved krithis only); the editor's save invalidates `previewKeys.all`; the preview also refetches on mount, on window focus and on a Refresh button. The phone's top bar shows the screen's own title and subtitle, as mobile does.
  - **Checks:** `make test-frontend` 113 tests pass (new: navigator, reader rules and golden fixture, reader view, page states and refresh, editor tab); `bun run typecheck` clean; `bun run lint` 0 errors (existing warnings only, none in the new files); `bun run build` succeeds.
  - **Browser verification (dev stack, Chrome pane):** reader for *Aada Modi Galadae* in Devanagari and Tamil (superscript digits shown as in mobile); Details tab; Home, Collections, a collection detail, Kshetras, a kshetra detail (Adi Kumbeswarar Temple), Browse facets, Explore lists, raga directory (1,014 ragas) and a raga detail. The draft krithi *Urakae Kalgunaa* shows with the "Not published (draft)" banner; V2 total 1,225 vs preview 1,226 confirms preview adds exactly that one. Edit flow: changed the title in the editor, saved, and the Mobile Preview tab showed the new title; reverted the title the same way and the preview showed the original. **These two saves were real writes to the dev database (audit rows exist); the data is back to its original title.** A console error from a hot-reload hook-order change during development did not recur after a reload.
  - **Not verified:** side-by-side comparison with the iOS simulator screenshots the Plan asked for (A3 check); a Varnam and a Ragamalika sample were not found in the first 400 dev krithis, so those two paths are covered by unit tests only. Collection and kshetra lists still use published-only eligibility, as noted under A1.
- 2026-11-02 **B1 done (uncommitted).** Backend for preview search and index status.
  - **Search:** `POST /v1/admin/preview/search/{hybrid|semantic}` (`PreviewRoutes.kt`) calls `PreviewSearchService`, which calls the same `RasikaSearchService.search` the V2 routes call, with an optional scope (`PUBLISHED_ONLY` default, or `INCLUDE_UNPUBLISHED`, which only changes the kriti retrieval's `publishedOnly`) and an optional `MacroExplainCollector`. The V2 handlers pass neither, so mobile does no extra work. The response wraps the unchanged `CatalogueSearchResponseDto` with an explanation: active profile, the floor in force, and every macro candidate with similarity, gap, lexical score, whether it cleared, and which condition failed. A profile with no calibrated floor is reported (`floorCalibrated: false`).
  - **Index status:** `GET /v1/admin/preview/krithis/{id}/index-status` (`PreviewIndexRepository`, `PreviewSearchService.indexStatus`). Verdicts `NOT_INDEXED`, `MAY_BE_STALE`, `CURRENT` from timestamps (newest of the krithi, its sections, lyric variants and lyric sections vs the newest `search_documents.updated_at`), the published flag vs the workflow state, and embeddings for the active profile, each with plain-language reasons. Known limits from Phase 0 stand: false-stale after a no-op edit followed by a worker run that skipped on an unchanged hash, and blind to raga or composer renames.
  - **Rate limit:** named Ktor limit `preview-search`, 20 requests per minute per curator (key is the JWT `userId`, falling back to the host), registered in `configureRequestValidation` beside the global 100 per minute per IP. It counts requests, not only embeddings, so it is a conservative bound on Gemini calls; the shared 256-entry query-vector cache still makes a repeated query free.
  - **Not done:** per-item vector and lexical ranks. They are computed in the SQL CTEs but not returned by `KrithiSearchRepository`, and exposing them would change a query that V2 also runs; per the Plan's "ship without ranks and say so", the panel will show `similarityScore`, `lexicalScore` and `rrfScore` only. `exactTitleMatchId` and macro cards keep their published-only rules in both scopes.
  - **Tests (`PreviewSearchRoutesTest`, 5, all pass):** published-only preview equals the V2 response and the explanation marks a weak card as not cleared with a reason; an indexed draft is hidden from V2 and published-only preview and shown by include-unpublished; non-admin is 403, the 21st request in a minute is 429 and another curator is unaffected; preview calls add nothing to the usage log and no audit row while V2 calls are still recorded; index status goes NOT_INDEXED → CURRENT → MAY_BE_STALE after an edit and 404 for an unknown krithi. `make test` and `make test-integration` both BUILD SUCCESSFUL, no failing result files.
  - **Not verified:** a live Hybrid or Semantic probe through the real Gemini client (the tests use the synthetic embedder).
- 2026-11-02 **B2, B3, C1, C2, C3 done (uncommitted).**
  - **B2 search UI:** Explore is now the mobile search: Krithis, Ragas and Composers; the "Search options" disclosure with All matches, Related meanings and Titles & lyrics; submit-only; exact-title kriti first, then up to three collection and kshetra cards (open the collection or kshetra screen), then the other kritis; Titles & lyrics pages 30 at a time and stops at 90, ragas have no cap, composers stop at 90; the filter sheet (Titles & lyrics only) with its note that only the first 30 of 1,014 ragas can be picked (D8); loading, empty, and error with Retry. Search results are cached under their own key prefix (`mobile-preview-search`), so a save, a refocus or a remount never re-runs a search.
  - **B3 explain and freshness:** "Why this matched" per kriti (matched document kind, passage, meaning, text and combined scores); "Why collections and kshetras appear" (profile, floor, each candidate, shown or the condition that hid it); the **Include unpublished** toggle, off by default, which badges **Not visible on mobile** from a server-supplied `unpublishedKrithiIds` list and warns that ranking can shift slightly; the index-status banner in the editor tab (polled every 30 s while visible); "Search for this krithi"; "Edits saved since this search. Run it again". The first design badged results by comparing two searches; the browser check showed that mislabels published krithis that merely rank differently, so the server now reports which results are unpublished and the UI makes one search call.
  - **Extra mount:** `/v1/admin/preview/mobile/*` serves the unchanged published-only V2 reads inside the admin gate, so "as mobile" Titles & lyrics, ragas and composers go through the exact mobile code without entering the mobile usage log (test: byte-identical to V2, a draft is 404 there).
  - **C1 guard:** golden fixtures `readerRules.golden.json` and `searchRules.golden.json` are read by Vitest and by the Kotlin `PreviewGoldenFixturesTest` (5 tests, pass: the TypeScript port agrees with `SectionLabels`, `TamilNotation`, `CollectionCopy.vibhakti`, `SearchUiState`, paging constants and mode labels). `mobilePreviewDtoFields.ts` plus `mobilePreviewDtoDrift.test.ts` compare every mirrored TypeScript DTO with its Kotlin `data class` (21 tests) and a type-level check fails the build if an interface gains a field that is not listed. Server parity tests (preview equals V2) are in `PreviewCatalogueRoutesTest` and `PreviewSearchRoutesTest`.
  - **C2 docs:** new [Mobile Preview guide](../../application_documentation/05-frontend/admin-web/mobile-preview.md), pointers in `search.md` and `api-contract.md`, the admin-web README, and the OpenAPI document (preview search, index status, catalogue mirrors, four schemas; the YAML parses). `make check-docs` passes (the two new files were added with `git add -N`, because the link checker resolves against git-tracked files; nothing is committed). `python3 conductor/check-registry-sync.py` reports nothing for TRACK-158.
  - **C3 end to end:** `e2e/tests/mobile-preview.spec.ts` passes against the dev stack: a seeded draft shows in the editor's Mobile Preview tab with the draft banner and "Not in the search index"; a saved title change appears in the preview without a reload; Titles & lyrics hides the draft "as mobile" and finds it with Include unpublished. It ran with the installed Google Chrome through a temporary config (the Playwright browser download was not made, and the config was removed); the test data was deleted afterwards.
  - **Live Gemini use:** three query embeddings in total during verification (`kamalamba navavarna`, `urakae`, `Aada Modi Galadae`); everything else was cached or lexical.
  - **Final checks:** `make test` and `make test-integration` BUILD SUCCESSFUL; DAL unit and integration suites pass; `make test-frontend` 150 tests pass in 21 files; shared-module `jvmTest` passes; `bun run typecheck` clean; `bun run lint` 0 errors (188 warnings, the same count as before this track); `bun run build` succeeds.
  - **Outcomes for D1 to D10:**
    - D1, D2, D3, D4: surfaced; the preview runs the V2 route and says when the list differs (scope toggle, badges, help text in the guide). Nothing in admin changed.
    - D5: resolved by reading code: Hybrid/Semantic limit 30 on both sides; admin lexical page size 50 against mobile's 30 (the preview uses mobile's).
    - D6: lexical field difference confirmed from code; admin search ordering **not determined** (needs an admin token read of `KrithiService.search`, not done).
    - D7: **confirmed live**; mobile's "Titles & lyrics" does not search lyrics. Candidate fix track: rename the mode or add a lyric clause.
    - D8: **confirmed live**; the raga picker reaches only 30 of 1,014 ragas. Candidate fix track: searchable or paged picker. The preview reproduces it and says so in the filter sheet.
    - D9: handled; preview uses the `ALL_PUBLISHED` mounts.
    - D10: surfaced as the index-status banner and the search guide.
  - **Not done or not verified:** per-item vector and lexical ranks (not exposed by the repository); a side-by-side comparison against iOS simulator screenshots; a Varnam and a Ragamalika sample in the dev data (none were found in the first 400 krithis, so those paths are covered by unit tests only); collection and kshetra list eligibility for unpublished members; the exact per-krithi staleness check (deferred to the worker); the false-stale case (the title edit and its revert made the dev krithi *Aada Modi Galadae* read "may be out of date" although its text is unchanged). D7 and D8 fix tracks were not opened because the track's scope excludes changing mobile.
  - **Side effects on the dev database to know about:** the A5 check saved two title edits (changed and restored) on *Aada Modi Galadae*, which bumped its `updated_at` and added audit rows. The C3 run created and deleted one krithi.
- 2026-11-02 **Fix: the Text size setting did nothing (reported by Seshadri).** Cause: the frame set a pixel `font-size` on itself, but every screen sizes text with Tailwind `text-xs`/`text-sm`/`text-lg`/`text-xl`, which are `rem` and ignore the frame. Fix: the frame now sets `--preview-px` and carries `preview-scale`, and unlayered rules in `src/index.css` size those steps from it (same ratios as Tailwind). Check: computed size of a `text-sm` line is 12.25 px, 14 px and 16.625 px for Small, Medium and Large in the browser; new `PhoneFrame.test.tsx` pins the variable per size. The earlier verification had checked the frame's own font size only, which is why this was missed.
