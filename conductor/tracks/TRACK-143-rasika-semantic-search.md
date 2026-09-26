| Metadata | Value |
|:---|:---|
| **Status** | Completed |
| **Version** | 1.1.0 |
| **Last Updated** | 2026-09-26 |
| **Author** | Sangeetha Grantha Team |

# Track: Rasika Semantic Search
**ID:** TRACK-143
**Status:** Completed
**Owner:** Seshadri
**Created:** 2026-09-22
**Updated:** 2026-09-22

## Goal

Port the Curator Console's existing Hybrid and Semantic composition search to the Rasika mobile app, as a client of the search API that already serves the console.

## Context

- **Reference:** [Catalogue search](../../application_documentation/03-api/search.md) — the two search experiences and the ranking contract.
- **Reference:** [Embedding pipeline](../../application_documentation/09-ai/embeddings.md) — what is indexed; indexing is a separate operation from this port.
- **Prior work:** [TRACK-108](./TRACK-108-semantic-search.md) shipped embeddings, `POST /v1/search/hybrid`, `POST /v1/search/semantic`, and the Curator Kritis modes. [TRACK-138](./TRACK-138-rasika-mobile-app.md) and [TRACK-140](./TRACK-140-rasika-discovery-experience.md) built Rasika Explore as lexical catalogue search and left AI discovery for later.
- **Reference UI (do not redesign):** `modules/frontend/sangita-admin-web/src/pages/KrithiList.tsx`.

## Intent
**Status:** Accepted
**Accepted by:** Seshadri
**Accepted at:** 2026-09-22

### Problem

I want the semantic search that already exists on the Curator Console brought to the Rasika mobile app. The console Kritis page (`KrithiList.tsx`) lets a curator switch Lexical / Hybrid / Semantic. Hybrid is the default. A query such as "Dikshithar Kamalamba Navavarna Krithis" returns ranked compositions with title and melody (composer, raga, tala), a matched-content snippet, a kind badge (Overview or Passage), and a relevance treatment: Hybrid Match when both sides hit, otherwise a Semantic % or Lexical % badge, plus the breakdown RRF / Sem % / Lex %. Semantic mode shows a single "% Match". An empty query falls back to ordinary lexical browse. Hybrid and Semantic are not paged; the console asks for 30 results and captions them as "Top discovery results" ranked by Reciprocal Rank Fusion or dense similarity.

Rasika does not have that. Explore (`SearchScreen` / `SearchPresenter` in `modules/shared/presentation`) is an explicit Search submit over Krithis, Ragas, and Composers. Krithi results come from `GET /v2/catalogue/krithis` through `CatalogueApi.searchKrithis`: paged, published catalogue rows (title, incipit, composer, raga sequence, tala) rendered on `KrithiCard`. There is no mode toggle, no passage from the embedding index, and no relevance score. `application_documentation/03-api/search.md` already says Rasika does not call the vector-search routes. TRACK-140 kept AI discovery out of the first delivery.

### Proposed outcome

On Rasika Explore, a rasika should be able to search compositions the way the console already searches them in Hybrid and Semantic: by mixed exact-and-related wording, and by meaning, then open the composition in the existing reader.

This is a port of the existing feature, not a new search backend. The API is already there and the shared DTOs are already in `commonMain`:

- `POST /v1/search/hybrid` and `POST /v1/search/semantic` (`SemanticSearchRoutes.kt` → `HybridSearchService` → `KrithiSearchRepository`). Request: `query`, optional `composerId` / `ragaId`, `limit` (default 20, clamped 1–100). Response: `SemanticSearchResponse` with `matchedContent`, `documentKind`, `similarityScore`, and on hybrid `lexicalScore` / `rrfScore`.
- Those types live in `modules/shared/domain/.../SearchDtos.kt`. Rasika's mobile client does not call them yet.
- Admin auth on these routes is optional. With no admin role, results are published compositions and visible search documents only (`publishedOnly = !hasAdminRole()`). An admin token can see editorial rows. Rasika should stay on the published side.

In scope: a Rasika client of those two endpoints, on the existing Explore krithi search, in the shared Compose UI and mobile-data layer. Curator Lexical stays what it is — `GET /v1/admin/krithis/search`, admin-only, all workflow states — and Rasika must not call it. Rasika's current catalogue search remains the lexical/browse path for known titles, incipits, and lines, and for Ragas and Composers. Raga and composer UUID filters already exist on the discovery request; whether Explore forwards the filters it already has is a Spec decision.

Out of scope unless a later accepted Spec says otherwise: redesigning the Curator Console; a new embedding model, index, or Flyway migration; re-embedding the corpus; generative or chat answers. Indexing stays the TRACK-108 tools. Hybrid with no active profile already degrades to lexical search over `search_documents`, which is not the same as catalogue browse; Semantic with no profile returns an empty list. The port consumes that behavior; it does not replace it.

### Affected users and systems

- Rasikas searching on Android and iOS Explore.
- Shared KMP: `modules/shared/presentation` (Explore), `modules/shared/mobile-data` (`CatalogueApi` / `KtorCatalogueApi`), `modules/shared/domain` (existing `SemanticSearch*` DTOs). Android and iOS shells host that shared UI; search does not live in the app shells.
- Backend search API as the server of record. Curator Console is the reference implementation only.
- Docs that already describe the split: `application_documentation/03-api/search.md` and `application_documentation/09-ai/embeddings.md`. `openapi/sangita-grantha.openapi.yaml` does not list `POST /v1/search/hybrid` or `POST /v1/search/semantic`.

### Constraints

- KMP: UI and presenter stay in `commonMain`; hoist state; unidirectional updates via `copy()`; immutable DTO lists into composables. `expect`/`actual` only where the platform truly differs. Domain stays `@Serializable` DTOs. Never put Exposed entities on the client. The search DTOs are already shared; do not fork a mobile-only copy of the same payload.
- Rasika remains a published-only anonymous catalogue client. Do not ship admin credentials or editorial workflow fields. Catalogue calls keep their session and interaction headers; discovery posts do not automatically inherit an admin token.
- Search is read-only. A query does not write `AUDIT_LOG`. Mutations, if any later Spec adds them, still audit. Schema changes, if any, are new Flyway versions only — do not edit `V58__semantic_search_pgvector.sql`.
- A similarity score is a rank, not a musicological verdict. Do not generate lyrics, translations, scales, or completeness claims from a match. Lakshana rules are not a new contract for this port.
- Versions come from `gradle/libs.versions.toml`. Do not commit until asked. Do not use a `cursor/` branch prefix. Do not implement until Plan Status is Accepted.

### Open questions

Closed by Seshadri on 2026-09-22. Do not reopen these in the Plan.

- **Auth.** Rasika calls with no admin token. `authenticate("admin-auth", optional = true)` plus `publishedOnly = !hasAdminRole()` already limits a missing or non-admin caller to published compositions and visible search documents. This port does not add a server auth test or a new auth mechanism. The client test asserts the outgoing post has no `Authorization` header.
- **Modes.** Ship Hybrid and Semantic next to today's catalogue Lexical search. **Default to Hybrid.**
- **Score chrome.** A short snippet and a quieter rank. No Hybrid Match badge, no RRF label, no Sem %, no Lex %, no Overview/Passage kind badge.
- **Offline.** Unsupported. Do not cache or invent Hybrid or Semantic results offline.
- **Snippet text.** Yes. The card displays indexed `matchedContent` as stored, including the plain-ASCII raga form.
- **Paging.** Cap at 30. No "more results" / next page for Hybrid and Semantic. `limit` is 30. Do not pretend `totalMatches` is a catalogue total.
- **Filters.** Leave the existing raga and composer filters as they are. Hybrid/Semantic is only for Krithis (not the Raga or Composer search tabs). Do not redesign those filters. They keep their current catalogue-lexical behavior. Do not add filter wiring that sends them on the discovery posts. Hybrid does not become a search over ragas or composers as entities.
- **OpenAPI.** Include `POST /v1/search/hybrid` and `POST /v1/search/semantic` in `openapi/sangita-grantha.openapi.yaml` as part of this port.

## Spec
**Status:** Accepted
**Accepted by:** Seshadri
**Accepted at:** 2026-09-22

### Requirements

Rasika Explore gains Hybrid and Semantic composition search as a client of the existing discovery API. Lexical catalogue search stays. Curator Console search stays.

1. **Modes, Krithis only.** On the Krithis category, Explore offers three modes: Lexical, Hybrid, and Semantic. The initial mode is Hybrid. Ragas and Composers have no mode control and keep calling `GET /v2/catalogue/ragas` and `GET /v2/catalogue/composers`.
2. **Lexical path unchanged.** Lexical still calls `CatalogueRepository.searchKrithis` → `GET /v2/catalogue/krithis` with the committed query, the applied raga and composer ids, and the current page size and page. Paging stays `PAGE_SIZE` × `MAX_PAGES` (3), with Load more. The result header stays "N in this library" and "Title order". The card stays `KrithiCard` with the catalogue incipit as the snippet. Rasika must not call `GET /v1/admin/krithis/search`.
3. **Hybrid and Semantic calls.** Hybrid calls `POST /v1/search/hybrid`. Semantic calls `POST /v1/search/semantic`. Both send `SemanticSearchRequest` and decode `SemanticSearchResponse` from `modules/shared/domain/.../SearchDtos.kt`. Do not fork those DTOs. The body is `query` = the trimmed committed query, `composerId` = null, `ragaId` = null, `limit` = 30. Send 30 explicitly. The DTO default of 20 is not the Rasika cap.
4. **Anonymous and published-only.** The post uses the same anonymous HTTP client as the catalogue. It sends the existing session and interaction headers (analytics metadata, not credentials). It does not send `Authorization`. The server already sets `publishedOnly = !hasAdminRole()`.
5. **No new filter wiring.** The filter sheet, chips, draft/applied facets, and the raga and composer pickers stay as they are and remain available on Krithis. Applied `ragaId` and `composerId` are sent only on the Lexical catalogue GET. Hybrid and Semantic ignore them. Do not add a language filter. Do not search ragas or composers as entities through the discovery API.
6. **Cap 30, no next page.** Hybrid and Semantic request `limit` 30 once. `hasMore` is false. There is no Load more control and no offset. `totalMatches` is the number of rows in `items` (the server sets it to `items.size`). Do not display it as a library total and do not reuse `RasikaCopy.resultCount` ("N in this library") for these modes. The result header is the fixed caption **Top matches**, with no count and without "Title order".
7. **Server empty-query and no-profile behavior stays.** Do not replace it with catalogue browse.
   - A blank or whitespace query on Hybrid or Semantic is still posted. `HybridSearchService.execute` trims it and returns `SemanticSearchResponse` with `totalMatches` 0 and `items` empty. Show the existing empty state. Do not call `GET /v2/catalogue/krithis` instead.
   - No active embedding profile: Hybrid returns lexical retrieval over `search_documents` (which can be empty even when the catalogue has rows). Semantic returns an empty list. Show what the server returned. Do not fall back to the catalogue.
   - An incompatible profile or embedder failure is HTTP 503 with `ErrorResponse` `{ "message": string }` from `StatusPages`. Show a retryable error using that message when the body has `message`. Do not treat 503 as an empty result list. The catalogue error envelope `{ code, message }` is not what this route returns.
8. **Discovery card.** One card per `SemanticSearchResultItem`. Tap calls the existing `onOpenKrithi(krithiId)` reader. The heart uses the existing favourite callback with `krithiId` and `title`. Fields shown:
   - Title: `title`.
   - Raga line: `ragaName` when non-null. This is the primary raga name from the search join (`LEFT JOIN ragas ON primary_raga_id`), not the catalogue raga sequence and not a ragamalika badge. Do not call the reader API to enrich the card.
   - Subline: `composerName`, then `talaName` when non-null, in the same muted composer · tala pattern as `KrithiCard`.
   - Snippet: `matchedContent` as stored, including the plain-ASCII raga form. Do not transliterate, strip, or rewrite it. Visual cap is the same as the catalogue snippet: `bodySmall`, `inkMuted`, `maxLines` 2, ellipsis.
   - Quiet rank, specified below. No other score chrome.
9. **Quiet rank.** A score is a rank, not a musicological claim. Do not show `documentKind` (`COMPOSITION_OVERVIEW` or `SECTION_PASSAGE`). Do not show a Hybrid Match badge, an "RRF" label, "Sem %", "Lex %", or "% Match".
   - **Hybrid** uses `rrfScore` only. Render it unlabeled, exactly four digits after the decimal, in muted `labelSmall` / `inkMuted` (example: `0.0325`). Do not convert it to a percent. RRF is `1/(60 + rank)` per branch, so a top hit is on the order of 0.016–0.033, not a 0–1 probability. Do not display `similarityScore` or `lexicalScore` on a Hybrid card, including when one of them is 0 (that is how the console chooses Hybrid Match vs Semantic % vs Lexical %). If `rrfScore` is null, show no rank numeral.
   - **Semantic** uses `similarityScore` only (`1 - cosine distance`, non-null). Render the nearest integer percent of `similarityScore * 100`, half up, with a `%` suffix and no other word (example: `0.874` → `87%`), in the same muted style. `lexicalScore` and `rrfScore` are null on this route and are not shown.
10. **Offline.** Do not cache, prefetch, or synthesize Hybrid or Semantic rows when the network is unavailable. Surface the existing unavailable/error path.
11. **Read-only.** No `AUDIT_LOG` write. No Flyway migration. Do not edit `database/migrations/V58__semantic_search_pgvector.sql`. Do not re-embed the corpus or change the embedding client.
12. **Out of scope.** Curator `KrithiList.tsx` and its badges. Admin lexical search. Generative or chat answers. New embedding index. Redesign of the filter sheet. Raga and Composer tabs.

### Design

**Where it lives**

- Presentation, `commonMain`: `SearchScreen` / `SearchPresenter` / `SearchUiState` in `modules/shared/presentation/.../search/`. Hoist the mode on `SearchUiState`. Mutations go through `copy()`. Pass an immutable `List<SemanticSearchResultItem>` into the composable. No `expect`/`actual`. No Exposed types.
- A discovery card composable next to `KrithiCard`, taking `SemanticSearchResultItem` (plus favourite callbacks). Do not map a discovery item into `CatalogueKrithiSummaryDto`. That DTO carries the raga sequence, language, musical form, incipit, and `isRagamalika`, which this response does not have.
- Mobile-data, `commonMain`: add `searchHybrid` and `searchSemantic` on `CatalogueApi`, `KtorCatalogueApi`, `CatalogueRepository`, and `FixtureCatalogueApi`. One client. POST JSON. Decode `SemanticSearchResponse`. Non-2xx on these posts reads `message` from `{ "message": string }`. 503 becomes a retryable `CatalogueFailure.Unavailable`.
- Domain: reuse `SemanticSearchRequest`, `SemanticSearchResultItem`, and `SemanticSearchResponse`. No new DTO.
- Backend routes and `HybridSearchService` / `KrithiSearchRepository`: no behavior change.
- OpenAPI: add the two posts to `openapi/sangita-grantha.openapi.yaml`. Add a `Search` tag. Override the file's global `bearerAuth` on both operations with anonymous access and an optional bearer (`security: [{}, { bearerAuth: [] }]`), and say in the description that a missing or non-admin caller is published-only and that an admin token can see editorial rows. Rasika does not send a token. Schemas match the three DTOs field for field: request `query` (string), `composerId` and `ragaId` (uuid, nullable, optional), `limit` (integer, default 20, server clamps 1–100). Response `query`, `totalMatches` (integer; count returned, not a catalogue total), `items[]` with `krithiId`, `title`, `composerName`, `ragaName`, `talaName`, `documentKind`, `matchedContent`, `similarityScore`, `lexicalScore`, `rrfScore`. Document 200 and 503 (`{ message: string }`, matching `StatusPages.ErrorResponse`). Do not add `GET /v1/admin/krithis/search`.
- Docs, living guides only: `application_documentation/03-api/search.md` (Rasika Explore does call these two posts for Krithis Hybrid and Semantic; default Hybrid; limit 30; Lexical and the Raga/Composer tabs stay catalogue) and `application_documentation/03-api/ui-to-api-mapping.md` (Explore compositions row). Leave the TRACK-108 implementation record as history.

**Screens and states (Krithis)**

| State | What the rasika sees | Call |
|:---|:---|:---|
| Before the first submit | Existing idle copy. Mode chips visible. Hybrid selected. | None |
| Lexical submit | Existing catalogue list, filters applied, Load more, "N in this library", "Title order" | `GET /v2/catalogue/krithis` |
| Hybrid submit | Discovery cards, caption "Top matches", no Load more | `POST /v1/search/hybrid` with `limit` 30 and null filter ids |
| Semantic submit | Same chrome as Hybrid, semantic quiet rank | `POST /v1/search/semantic` with `limit` 30 and null filter ids |
| Empty items | Existing empty state | Whichever mode was submitted. Hybrid/Semantic empty is not replaced with catalogue browse |
| Loading | Existing loading state. A newer submit or mode change cancels the older request (the presenter already uses `generation`) | — |
| 503 / network failure | Existing retryable error | No invented rows |
| Filter sheet | Unchanged. Apply still commits facets and refetches. In Hybrid or Semantic the refetch still sends null filter ids | — |
| Switch to Ragas or Composers | Mode chips hidden. Existing directory search and paging | Catalogue GETs only |
| Switch back to Krithis | The selected mode is still the one the rasika left | Refetch in that mode if a search was already committed |

Mode chips sit with the Krithis tools (category row / filter row), labels **Lexical**, **Hybrid**, **Semantic**. Changing mode resets the page, clears the other mode's rows, and refetches when a query has been committed. `openDirectory` and `applyCommittedQuery` still land on Krithis and then follow the current mode (initial mode Hybrid).

**Rank fields, from the code**

`KrithiSearchRepository.toSemanticItem(hybridScores = true)` sets `lexicalScore` and `rrfScore` only for hybrid. Semantic calls `toSemanticItem(hybridScores = false)`, so those two are null and `similarityScore` is the cosine similarity (0 when the SQL value is null). Hybrid order is `rrf_score DESC`. Semantic order is `similarity DESC`.

### Flagged concerns

- **Filters stay on screen in Hybrid and Semantic and do not constrain those results.** The chips and sheet are unchanged, and the posts send null `composerId` / `ragaId`. A rasika can have a raga chip showing and still see an unfiltered discovery list. That is the locked "leave the filters as catalogue-lexical" decision. Accepting this Spec accepts that split.
- **Default Hybrid changes today's first submit.** `SearchPresenterTest.committedSearchLoadsFixtureMatches` assumes `submit()` fills `CatalogueKrithiSummaryDto` rows from the fixture catalogue. After this change that submit is Hybrid. Move the catalogue assertions under an explicit Lexical selection and add Hybrid/Semantic cases. Do not keep Lexical as the default to preserve the old test.
- **"N in this library" would misread `totalMatches`.** The caption for Hybrid and Semantic is "Top matches" with no number.
- **`ragaName` is the primary raga only.** Lexical cards show the ordered raga sequence. Discovery cards do not. Do not add a per-row catalogue fetch to fake the sequence.
- **Indexed snippet can show a plain-ASCII raga spelling.** That string is stored `matchedContent`. It is not a second raga identity and must not be parsed into the raga filter.
- **Semantic percent is a quiet rank of cosine similarity.** It must not be captioned as confidence, a match grade, or lakshana agreement. Hybrid must not turn `rrfScore` into a percent.
- **Empty Hybrid/Semantic is not catalogue browse.** The console treats an empty query as lexical browse on the client (`isBrowseMode`). Rasika must not copy that. The server returns an empty list for a blank query on both posts.
- **No profile, or a fresh `search_documents` index, can return no Hybrid rows while Lexical catalogue search still finds compositions.** Do not hide that by falling back.
- **Optional admin auth is a foot-gun if a bearer is attached later.** This client must not send one. Unpublished rows would otherwise become visible.
- **OpenAPI's global security is `bearerAuth`.** Both new operations have to override it, or the contract says Rasika's anonymous call is unauthorized.
- **No policy conflict on audit or Flyway** as long as the port stays a read of the existing routes and does not edit V58.

### Open questions carried forward

None. The Intent questions are closed:

| Question | Decision |
|:---|:---|
| Auth | No admin token. No new server auth test. Client test asserts no `Authorization` header. |
| Modes | Lexical, Hybrid, and Semantic on Krithis. Default Hybrid. |
| Score chrome | Snippet plus quiet rank. Hybrid shows unlabeled `rrfScore` to four decimals. Semantic shows `similarityScore` as a whole percent. No curator badges. |
| Offline | Unsupported. |
| Snippet | `matchedContent` as stored, including the plain-ASCII raga form. |
| Paging | `limit` 30. No next page. `totalMatches` is not a catalogue total. |
| Filters | Unchanged catalogue-lexical wiring. Not sent on Hybrid or Semantic. Raga and Composer tabs stay catalogue search. |
| OpenAPI | Add both posts in this port. |

## Plan
**Status:** Accepted
**Accepted by:** Seshadri
**Accepted at:** 2026-09-22

Seshadri authorized proceeding ("Go with the plan") in the same instruction that accepted the Spec. This Plan was written from the accepted Spec and then implemented. It does not reopen the closed Intent decisions.

`openDirectory(Ragas)` and `openDirectory(Composers)` stay catalogue directory loads. Home shortcuts and `openDirectoryLoadsRagasWithoutPriorQuery` depend on that. `applyCommittedQuery` lands on Krithis and follows the current mode (initial Hybrid). `openDirectory(Krithis)` follows the current mode as well. Switching away and back keeps the mode.

### Files that change

- `modules/shared/mobile-data/.../CatalogueApi.kt`, `KtorCatalogueApi.kt`, `CatalogueRepository.kt` — `searchHybrid` and `searchSemantic`. Same anonymous client, session and interaction headers, no `Authorization`. Body is `SemanticSearchRequest`. Non-2xx reads `{ message }`. HTTP 503 is `CatalogueFailure.Unavailable` with `serverMessage`.
- `modules/shared/mobile-data/.../CatalogueFailure.kt` and `modules/shared/presentation/.../LoadState.kt` — discovery `{ message }` is shown. Catalogue network failures still use the offline copy.
- `modules/shared/mobile-data/.../CatalogueFixtures.kt` — fixture posts. A blank query returns an empty list. Filter ids are honored only when the request actually carries them.
- `modules/shared/presentation/.../search/SearchPresenter.kt`, `SearchScreen.kt`, `RasikaCopy.kt` — mode on `SearchUiState` (initial Hybrid), immutable `discoveryItems`, chips only on Krithis, caption "Top matches", no Load more for Hybrid or Semantic.
- `modules/shared/presentation/.../components/DiscoveryKrithiCard.kt` (and `GlyphTile` visible to it from `KrithiCard.kt`) — title, primary `ragaName`, composer · tala, stored `matchedContent` (max 2 lines), quiet rank. Not mapped into `CatalogueKrithiSummaryDto`.
- Presenter and client tests, plus the Android and iOS empty-search paging journeys, which select Lexical before asserting "in this library" / Load more.
- `openapi/sangita-grantha.openapi.yaml` — both posts, Search tag, the three DTO schemas, `ErrorResponse`, `security: [{}, { bearerAuth: [] }]`, 200 and 503. No admin lexical route.
- `application_documentation/03-api/search.md` and `application_documentation/03-api/ui-to-api-mapping.md` only.

No backend route change, no Flyway, no V58 edit, no `AUDIT_LOG`, no re-embedding, no `KrithiList.tsx` edit, no new DTO.

### Order of work

1. Anonymous discovery posts on the catalogue client, repository, and fixtures, including the 503 `{ message }` mapping.
2. Presenter mode, separate discovery list, and the Krithis screen (chips, card, "Top matches").
3. Presenter and client tests. Lexical catalogue assertions sit under an explicit Lexical selection. Default submit is Hybrid.
4. OpenAPI and the two living API docs.
5. `./gradlew :modules:shared:mobile-data:jvmTest :modules:shared:presentation:jvmTest --console=plain`

### Risks

- Default Hybrid changes every Krithis `submit()` that assumed catalogue rows. Lexical paging, filter chips, and the native empty-search journeys must select Lexical. A blank Hybrid or Semantic query is an empty server list, not catalogue browse.
- Showing every `Unavailable` message would replace the catalogue offline copy. Only `serverMessage` from the discovery body is shown.
- Filter chips stay visible in Hybrid and Semantic and do not constrain those posts. That split is locked.
- `totalMatches` must not be stored in `SearchUiState.total`, or the header would read "N in this library".
- The shared JSON config omits null fields (`explicitNulls = false`). Null `composerId` and `ragaId` are absent on the wire and decode as null. `limit` 30 is written explicitly so the DTO default of 20 is not used.
- Mapping a discovery row into `CatalogueKrithiSummaryDto` was rejected: that DTO needs the raga sequence, language, form, incipit, and `isRagamalika`, which this response does not have.
- Keeping Lexical as the default to preserve `committedSearchLoadsFixtureMatches` was rejected by the Spec.

### Proof

`make test` is the backend suite and `make test-frontend` is the Curator web app. This change is shared KMP. The existing Gradle tasks, already used in CI, are the proof:

```bash
./gradlew :modules:shared:mobile-data:jvmTest :modules:shared:presentation:jvmTest --console=plain
```

## Implementation Plan

- [x] Discovery posts on `CatalogueApi`, `KtorCatalogueApi`, `CatalogueRepository`, and both fixture APIs. 503 `{ message }` is retryable `CatalogueFailure.Unavailable`.
- [x] `KrithiSearchMode` on `SearchUiState`, initial Hybrid. Lexical catalogue path unchanged. Hybrid and Semantic send limit 30 and null filter ids, `hasMore` false, no Load more, caption "Top matches".
- [x] `DiscoveryKrithiCard` with stored snippet and quiet rank. Hybrid shows unlabeled `rrfScore` to four decimals. Semantic shows half-up percent. No curator badges.
- [x] Presenter tests: catalogue assertions under explicit Lexical; Hybrid and Semantic cases added. Client test asserts no `Authorization` header.
- [x] OpenAPI Search operations and the two living docs. Android and iOS paging journeys select Lexical. Device journeys passed on 2026-09-22.

## Progress Log

- **2026-09-22**: Track created. Intent drafted from the Curator Kritis search and the Rasika Explore catalogue client. Spec, Plan, and implementation not started; Intent Status remains Draft.
- **2026-09-22**: Intent accepted by Seshadri. Spec drafted (Status Draft). Plan not written. No code, OpenAPI, or test edits.
- **2026-09-22**: Spec accepted by Seshadri. Plan written and marked Accepted because the same instruction authorized proceeding ("Go with the plan").
- **2026-09-22**: Implemented the Plan. `openDirectory(Ragas|Composers)` stays a catalogue GET so home directory shortcuts keep working; Krithis and `applyCommittedQuery` follow the current mode. Shared JVM tests passed (`mobile-data` 23, `presentation` 49, 0 failures). No commit.
- **2026-09-22**: Native journeys passed. Android `connectedDebugAndroidTest` on `Rasika_API34` (`emulator-5554`): 24 tests, 0 failures, including `exploreEmptySearchLoadsTheNextPage` after selecting Lexical. iOS `xcodebuild test` on iPhone 17 `C0104896-CD64-4533-A919-1E5F22036561`: 19 tests, 0 failures.

## UX validation follow-up — 2026-09-22

The user's follow-up request authorizes validation and reimagining the hybrid search UI without a required mode-button selection. This supersedes the original always-visible mode-chip design and the presentation of inactive discovery filters; API routing, ranks, cap, and filter semantics remain unchanged.

- Hybrid is the default everyday search, described as “Search titles, lyrics and meaning together.” No mode selection is required before submitting.
- An optional, collapsed **Search options** disclosure offers **All matches** (Hybrid), **Related meanings** (Semantic), and **Titles & lyrics** (Lexical). Each has a plain-language explanation and an accessible radio selection. Choosing an option closes the disclosure and reruns an already committed query.
- Filters and active filter chips appear only in Titles & lyrics. When saved facets exist in discovery, a short message explains that they apply only to Titles & lyrics. The existing filter sheet and catalogue-only request wiring are retained.
- Options disclosure state is hoisted in `SearchUiState`. Opening/closing it does not issue a request.
- Validation covers the shared client/presenter tests and native search-options/paging journeys. Results are recorded below once checks finish.

### Follow-up validation results

- Shared tests: `./gradlew :modules:shared:mobile-data:jvmTest :modules:shared:presentation:jvmTest --console=plain` — **BUILD SUCCESSFUL**; mobile-data 23 tests, presentation 50 tests, zero failures/errors.
- Android (`emulator-5554`): `exploreSearchesHybridWithoutChoosingAModeAndOffersRefinements` and `exploreEmptySearchLoadsTheNextPage` — passed in separate targeted instrumentation runs. Covers automatic Hybrid, Semantic refinement, Lexical results/filter visibility, return to Hybrid, and catalogue paging.
- iOS (iPhone 17 `C0104896-CD64-4533-A919-1E5F22036561`): `testExploreEmptySearchLoadsTheNextPage` — **TEST SUCCEEDED**. Initial run exposed a test-helper issue: it stopped scrolling once Load more existed, even when not hittable. The helper now waits for visibility before tapping; the paging assertion is retained.
- Anonymous live smoke test: `POST /v1/search/hybrid` with query “Dikshithar Kamalamba Navavarna Krithis” and limit 30 returned HTTP 200, 30 unique compositions, with Kamalamba compositions among the leading matches. This verifies local retrieval, not corpus completeness or relevance for all queries.
- Required development stack restart completed using `make dev-down` / `make dev`; backend `/health` returned OK and the frontend returned HTTP 200.
- `git diff --check` passed. No commit created.

### Bottom navigation follow-up

The user flagged cropped labels in the native screenshots. Retain icon-and-text navigation for discoverability. The custom tab bar now reserves navigation-bar insets, extends its teal background behind the system area, and uses a 72 dp minimum item height with vertical padding so labels can grow with text scaling. This fixes the edge-to-edge overlap rather than hiding the labels.

Bottom-bar validation: presentation JVM tests and Android debug build passed (`BUILD SUCCESSFUL`). Android screenshots show all four labels fully visible at 100% and 150% system text size; original text scale restored. Development restart verified with backend OK and frontend HTTP 200. iOS was not rerun for this inset-only follow-up.


## Critical validation — 2026-09-24

Reviewed commit `973e78a` together with the current search-options and bottom-navigation follow-ups against the accepted Spec and its UX amendment.

- **Bugs — fixed:** discovery response decoding was outside the client's exception boundary. An HTTP 200 body missing required fields raised `JsonConvertException` rather than `CatalogueFailure`, escaping the presenter's retryable error handling. A new MockEngine regression failed before the fix and passed afterward. Both discovery posts now wrap body decoding, preserve server error messages, and rethrow cancellation (including during error-body decoding).
- **Bugs — validation strengthened:** added an in-flight, cancellation-resistant Hybrid response test so late results cannot replace Semantic results; also covered committed-query preservation, no discovery paging, retry recovery, and client cancellation. The original immediate-submit test did not exercise a started older request.
- **Security:** verified the actual mobile client has no auth plugin/default bearer and both POST tests assert no Authorization header. Existing server queries enforce published/visible scope for anonymous callers. No new server auth behavior or auth test was introduced.
- **Compliance:** verified shared DTO reuse, explicit limit 30, null discovery facets, no catalogue fallback, distinct discovery rows, quiet rank formatting, and anonymous OpenAPI overrides. No backend, schema, indexing, or Curator changes.
- **Validation:** shared JVM suites passed: mobile-data 25 tests, presentation 53 tests, zero failures/errors. Android debug assembly and iOS simulator Kotlin compilation passed (`BUILD SUCCESSFUL`). Android's Hybrid → Semantic → Lexical → Hybrid search-options journey and empty Lexical paging journey passed on `emulator-5554` in separate runs (one test each). Documentation links passed (`doc links OK`), as did `git diff --check`. Required `make dev-down` / `make dev` restart completed; backend `/health` returned `OK` and frontend HTTP 200.
- **Limits:** iOS device UI tests were not rerun. Fixture journeys validate client behavior, not retrieval relevance. “Best in class” retrieval quality would require a judged multilingual query set and measured ranking/latency; that is not established by one live query or this client port. The pre-existing local-device endpoint configuration edits are separate from this review's fix.
