| Metadata | Value |
|:---|:---|
| **Status** | Completed |
| **Version** | 1.0.0 |
| **Last Updated** | 2026-11-02 |
| **Author** | Sangeetha Grantha Team |

# Track: Lexical Lyric Search and Searchable Raga Picker

**ID:** TRACK-159
**Status:** Completed (2026-11-02): implemented and verified; changes are uncommitted on branch `track-159-lexical-lyric-search-and-raga-picker` (branched from `track-158-mobile-preview`, which is also unpushed)
**Owner:** Seshadri
**Created:** 2026-11-02
**Updated:** 2026-11-02

## Goal

Fix the two Rasika mobile defects that [TRACK-158](TRACK-158-mobile-preview.md) found and confirmed against the dev catalogue, in the mobile app and in Mobile Preview together.

## Context

- **D7:** Rasika's "Titles & lyrics" mode (`RasikaCopy.MODE_LEXICAL`) is backed by `GET /v2/catalogue/krithis?query=`, whose filter (`CatalogueRepository.krithiFilter`) matches only title, incipit, composer and raga names and aliases. A phrase taken from a charanam of *Aada Modi Galadae* returned `total: 0`.
- **D8:** The filter sheet's raga picker (`SearchPresenter.loadFilterOptions`, `SearchScreen.FilterSheet`) loads one page of 30 ragas. The dev catalogue has 1,014, so 984 cannot be chosen. The composer picker has the same one-page limit (3 composers today).
- Mobile Preview reproduces both on purpose (TRACK-158 Spec), so it changes with the app: it reads the same route for D7 and has its own copy of the filter sheet for D8.

## Intent

**Status:** Accepted
**Accepted by:** Seshadri (chat instruction: "Fix the below defects both in Mobile and Mobile preview feature")
**Accepted at:** 2026-11-02

### Problem

1. A rasika who types a line of sahitya in "Titles & lyrics" finds nothing, though the mode promises lyrics.
2. A rasika filtering by raga can pick only from the first 30 of 1,014 ragas.

### Proposed outcome

1. Lexical krithi search also matches stored lyrics, in every script, so the label is true. Result order stays title order.
2. The raga picker can find any raga by name and page through the rest; the composer picker can page. A selected raga stays visible even when it is outside the loaded page.
3. Mobile Preview behaves the same, and its filter-sheet note about the 30-raga limit is removed.

### Constraints

- No change to the DTOs or to hybrid and semantic search.
- No new migration if the existing index serves the query.
- Read-only catalogue paths: no `AUDIT_LOG` change.
- Fixes land in the shared repository, presenter and Preview together so they cannot drift.

## Spec

**Status:** Accepted
**Accepted by:** Seshadri (chat instruction)
**Accepted at:** 2026-11-02

### D7: lyric clause in the lexical filter

- **Where:** `CatalogueRepository.krithiFilter` is the single filter behind `searchKrithis` (V1 and V2) and `listFacets`, so one change covers mobile, preview and facet counts.
- **Clause:** a krithi matches when any of its lyric variants has `lyrics ILIKE` the query. `krithi_lyric_variants.lyrics` holds the full text of every script and already has a trigram GIN index (`idx_krithi_lyrics_trgm`, V03), so no migration is needed. (`krithi_lyric_sections.normalized_text` is not usable: dev has it populated for 1 row of 26,251.) Measured on the dev data: about 2 ms for an indexed `ILIKE` over 6,814 variants.
- **Whitespace:** stored lyrics break lines with newlines, so a phrase spanning a line break would not match a plain substring. In the lyric clause each whitespace run in the query becomes `_` (one character), so "a b" matches "a b" and "a\nb". The title, incipit, composer and raga clauses are unchanged.
- **Visibility:** unchanged: the `publishedComposition` condition still wraps the whole filter, so a draft's lyrics never match for mobile; the preview scope shows them.
- **Behaviour change to note:** V1 clients get the same wider matching (the filter is shared); their DTOs are unchanged.
- **Not in scope:** transliteration across scripts (a Latin query will not match Tamil text); ranking by relevance.

### D8: searchable, pageable pickers

- **Presenter (`SearchPresenter`, commonMain):** new state `filterRagaQuery`, `filterRagasTotal`, `filterRagasPage`, `filterComposersTotal`, `filterComposersPage`. New `searchFilterRagas(query)` (replaces the list with page 0 of the query), `loadMoreFilterRagas()` and `loadMoreFilterComposers()` (append the next page of 30). Stale responses are dropped with a generation counter. Opening the sheet resets the raga query.
- **Sheet (`SearchScreen.FilterSheet`):** a search field above the raga chips, a "Load more" button while loaded < total, and a chip for the draft-selected raga even if it is not in the loaded list. Same for composers' "Load more".
- **Preview:** the `<select>` of 30 becomes a search box with a result list and "Load more", driven by the same route; the D8 note is removed.

### Alternatives rejected

- **Rename the mode to "Titles":** hides the gap instead of fixing it and drops a feature the label already promises.
- **A new `lyric` query parameter:** the mode has one search box; a second parameter would change the contract.
- **A `lyrics_normalized` column and migration:** unnecessary while the trigram index serves the query.
- **A bigger first page for the picker:** raises the cap but does not remove it.

### Risks

| Risk | Mitigation |
|:--|:--|
| Very short queries match many lyrics | Already true for titles; paging caps results at 90 |
| Wider V1 matching surprises an old client | DTOs unchanged; documented |
| Slow lyric scan on a larger catalogue | Trigram index; a test asserts the clause is a single subquery |

## Plan

**Plan Status: Accepted** (2026-11-02, Seshadri, chat instruction)

| # | Step | Check |
|:--|:--|:--|
| 1 | D7 repository: lyric clause and pattern helper | DAL integration tests: phrase in the middle of a lyric, across a line break, in another script, case-insensitive; a draft's lyric is hidden; title search unchanged |
| 2 | D7 route and preview: same route, no code | API route test: `GET /v2/catalogue/krithis?query=<lyric phrase>` and the preview mounts return it |
| 3 | D8 mobile presenter and sheet | commonTest presenter tests: search replaces the list, load more appends, stale response dropped, selected raga stays visible, composers page |
| 4 | D8 Mobile Preview picker | Vitest component tests; typecheck, lint, build |
| 5 | Docs and verification | `make test`, `make test-integration`, `make test-frontend`, shared jvmTest, `make check-docs`; live check on the dev stack for both defects |

## Progress Log

- 2026-11-02: Track opened from the TRACK-158 findings; Intent, Spec and Plan accepted by Seshadri's chat instruction.
- 2026-11-02 **Implemented and verified (uncommitted).**
  - **D7:** `CatalogueRepository.krithiFilter` gained a lyric clause: a krithi matches when any lyric variant's `lyrics` is `ILIKE` the phrase, using `CatalogueLike.lyricPhrasePattern` (whitespace runs become one-character wildcards so a phrase can span a line break; `%`, `_` and `\` stay literal). Exposed core has no ILIKE operator, so a small `IlikeOp` binds the pattern as a parameter and keeps the existing trigram index (`idx_krithi_lyrics_trgm`) usable. No migration. One filter serves V1, V2, facet counts, the preview mounts and mobile.
  - **D8, app:** `SearchPresenter` now tracks `filterRagaText`, `filterRagaQuery`, totals and pages; new `onFilterRagaTextChange`, `searchFilterRagas`, `loadMoreFilterRagas`, `loadMoreFilterComposers`, with a generation guard against stale responses and a reset when the sheet reopens. `SearchScreen.FilterSheet` has a "Find a rāga" search field, "Showing n of total", "Load more" for ragas and composers, keeps a chosen raga visible when the current page does not list it, and now scrolls. New copy in `RasikaCopy`.
  - **D8, preview:** the filter sheet is the same design (search box, chips, Load more, chosen raga kept); the "cannot be picked" note is gone.
  - **Tests:** `CatalogueLyricSearchTest` (7: mid-lyric phrase, case, line-break span, another script, literal wildcards, draft lyric hidden, title/order unchanged, one row for several matching variants), a route-level lyric test in `PreviewCatalogueRoutesTest` (V2, mirror byte-identical, preview, draft only on preview), `SearchFilterPickerTest` (10 presenter tests), and a rewritten picker test in `SearchPane.test.tsx`. `make test`, `make test-integration`, DAL unit and integration, shared-module `jvmTest`, `make test-frontend` (154), typecheck, lint (0 errors, same 188 warnings), build and `make check-docs` all pass.
  - **Live on the dev stack:** the phrase `nIvE(y)anucunu bhakti kUDina` that returned 0 now returns *Aada Modi Galadae* on `/v2/catalogue/krithis` and on V1; `bhak%ti` and `bhak_ti` still match nothing; title search still returns the same krithis; all 1,014 ragas are reachable by paging 30 at a time and `kalyani` finds 17; in Mobile Preview the picker showed "Showing 30 of 1014", "Load more" went to 60, searching `kalyani` showed "Showing 17 of 17", and the lyric phrase found the krithi in Titles & lyrics.
  - **Docs:** API contract and OpenAPI describe what `query` matches; the Mobile Preview guide lists D7 and D8 as fixed.
  - **Not verified:** the Android and iOS builds (no Android SDK or Xcode build was run here; the changed code is `commonMain` and compiles and passes tests on the JVM target); the Compose sheet was not looked at on a simulator or device.
  - **Limits kept on purpose:** a Latin query does not match Tamil or Devanagari text (no cross-script transliteration), and results stay in title order, not relevance order.
- 2026-11-03 **Simulator check (iPhone 17, iOS simulator build of `main` after PR #63) and two follow-up fixes (uncommitted, on `fix/home-quiet-feature-failure-and-filter-sheet-keyboard`).**
  - **Verified on the simulator:** in "Titles & lyrics" the phrase `nIvE(y)anucunu bhakti kUDina` returns "1 in this library" (*Aada Modi Galadae*); the filter sheet shows the "Find a rāga" field; searching `kalyani` lists the 17 matching ragas (including ones outside the first 30) with "Showing 17 of 17"; the sheet scrolls; choosing *Nāndhakalyāni* and pressing Apply shows it as an applied chip.
  - **Defect found by the check:** while typing in "Find a rāga" the on-screen keyboard covered Apply, Cancel and Reset and would not dismiss. Fix: the sheet now uses `imePadding()` and submitting the rāga search clears focus, which closes the keyboard. Re-checked on the simulator: the keyboard closes after the search and Apply, Cancel and Reset are reachable.
  - **Reported by Seshadri (screenshot):** Home showed "The featured composition could not be loaded. Search and browse still work." with a Retry button when the backend was unreachable (here Docker and the database had stopped). The featured composition is optional, so Home now shows nothing in its place on failure and retries quietly the next time Home appears (`HomePresenter` re-arms `loadIfNeeded`); `FEATURE_UNAVAILABLE` is removed. Mobile Preview behaves the same. New test `aFailedFeatureIsRetriedQuietlyTheNextTimeHomeAppears`.
  - **Still true:** the main Explore search field leaves the keyboard open after a search (not changed here). Android was not run (no SDK).
