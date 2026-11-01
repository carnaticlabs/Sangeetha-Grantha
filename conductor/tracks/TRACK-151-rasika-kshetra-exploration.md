| Metadata | Value |
|:---|:---|
| **Status** | In Progress |
| **Version** | 1.0.0 |
| **Last Updated** | 2026-11-01 |
| **Author** | Sangeetha Grantha Team |

# Track: Rasika Kshetra Exploration

**ID:** TRACK-151
**Status:** In Review: Phases 0 to 2 merged; Phase 3 in PR #53
**Owner:** Seshadri
**Created:** 2026-10-16
**Updated:** 2026-11-01

## Goal

Let Rasika users explore the sacred places (kshetras) in the catalogue: a list of temples grouped by region or element, a detail page with the parent/child hierarchy (for example the Tiruvarur complex and its Kamalamba, Nilotpalamba and Pancha Linga shrines), the sthala vriksha, tirtha and bhuta, and the kritis sung there.

## Context

- **Predecessors:** [TRACK-145](TRACK-145-dikshitar-kshetra-musicological-metadata.md) (temple hierarchy and sthala columns, `KSHETRA_OVERVIEW` documents) and [TRACK-150](TRACK-150-rasika-collections-and-lakshana-reader.md) (collections API, reader cycle context, V2-only pattern).
- **Measured on the local database (2026-10-16):** 97 temples; 13 have a parent; all 97 carry `mandalam`; 5 carry `bhuta` (the Pancha Bhuta lingams); 15 carry `sthala_vriksha`; **none has latitude or longitude**. 481 kritis (all Dikshitar) have a temple, across 94 temples. 94 `KSHETRA_OVERVIEW` documents exist. No Syama Sastri or Tyagaraja kriti has a temple, and Syama Sastri geography is `UNRESOLVED` for all 70 compositions.
- **Map dependency:** the map waits on [TRACK-154](TRACK-154-temple-google-place-ids.md) (reviewed Google place ids; the database stores no Google coordinates). The list, hierarchy and detail pages do not.
- **Existing surface:** the reader DTO carries `temple` (id and name only). `/v1/temples` is an editorial route. There is no public temple endpoint.

## Intent

**Status:** Accepted
**Accepted by:** Seshadri (2026-10-24)

### Problem

The kshetra work in TRACK-145 is invisible to rasikas. A user reading a Dikshitar kriti cannot see which shrine it is sung at, which other kritis belong to that shrine, or how the Tiruvarur sub-shrines relate. Because three composers' temple data differ greatly in coverage, a naive page would make Dikshitar look like the only composer with geography.

### Proposed outcome

1. **V2 read endpoints.** `GET /v2/catalogue/temples` and `GET /v2/catalogue/temples/{id}`, published-only and anonymous. Detail returns place kind, region, element, sthala vriksha, tirtha, parent and children, an overview from `KSHETRA_OVERVIEW`, and the published kritis sung there.
2. **Mobile.** A Kshetras entry on Home or Explore, a list grouped by region (mandalam) with an element filter, and a detail screen. The reader's temple becomes a tappable link.
3. **Map.** Where TRACK-154 has a confirmed place, the detail page offers Google Maps (link first, embedded map per open question 4). No pin for unconfirmed temples.
4. **Honest gaps.** A kshetra recorded as a gap (Kanyakumari) shows as a recorded gap with its reason, not as an empty page. Kritis with no temple are not shown as "no kshetra".

### Constraints

- V2 only; V1 untouched. Reuse the TRACK-150 pattern (no migration expected, DTOs in shared domain, OpenAPI sync).
- Only reviewed data. `temple_id` empty means unreviewed, not "no kshetra" (TRACK-146 geography states).
- Map work needs [TRACK-154](TRACK-154-temple-google-place-ids.md) first. Google-sourced content must appear on a Google Map, so the in-app map is a Google Maps SDK map; our own database holds only place ids.
- Do not publish drafts: temples with no published kriti and no overview are not listed.

### Open questions

1. Group the list by mandalam, by element, or both with a toggle?
2. Is a gap temple (Kanyakumari) listed, or reachable only from search?
3. Should child shrines appear nested under the complex only, or also in the flat list?
4. Map delivery (decision pending, phased): (a) "Open in Google Maps" link built from the place id, with no app API key; (b) an embedded Google Maps SDK map on Android and iOS, which needs an app-side key restricted to the bundle id, billing, platform map code behind the shared UI, and a privacy review. Ship (a) with this track and (b) as a follow-up, or both together?

### Decisions on open questions (2026-10-24)

1. Group by mandalam, with an element filter (element is populated for 5 temples only, so it is a filter, not a grouping).
2. A gap temple (Kanyakumari) is listed and marked as a recorded gap with its reason.
3. Child shrines are nested under their parent only, not repeated in the flat list.
4. Map: ship the "Open in Google Maps" link built from the place id with this track; the embedded Google Maps SDK map is a follow-up track.

### Completion evidence (fixed in Plan)

Backend route and repository tests, shared and presenter tests, live check that Tiruvarur returns its children and every temple kriti count equals the database, iOS simulator walk-through, `make test`, `make test-integration`, `make check-docs`.

## Spec

**Status:** Accepted
**Accepted by:** Seshadri (chat instruction)
**Accepted at:** 2026-10-24

Written from the accepted Intent after reading `V66`, `R__seed_09`, `CatalogueRepository` (collections and `cycleOverview`), `CatalogueService`, `CatalogueV2Routes`, `CatalogueV2Dtos` and the TRACK-150 and TRACK-154 files. Nothing is implemented. Live eligibility counts could not be measured in this session (the dev database was not running); the Plan re-measures them before Phase 1 lands. The `carnatic-musicologist` review was not run: this track adds read-only endpoints over existing reviewed data and no seed, import, raga or tala content.

### Requirements

**A. Eligibility rule (the reusable contract; TRACK-152 and TRACK-153 consume it unchanged)**

1. A temple has **direct eligibility** when at least one of these holds:
   - a krithi with `krithis.temple_id = temple.id` satisfies the V2 visibility predicate `publishedComposition(V2)` (workflow state `published`, `UNESTABLISHED` form allowed, as in V2 readers);
   - a `search_documents` row exists with `document_kind = 'KSHETRA_OVERVIEW'`, `temple_id = temple.id` and `is_published`.
2. A temple is **eligible** (listed, reachable by detail, searchable, facetable) when it has direct eligibility **or** at least one child (`parent_temple_id = temple.id`) with direct eligibility. The second clause keeps a nested child reachable when its parent has no data of its own; the parent then appears as a container with `publishedKrithiCount = 0`.
3. An ineligible temple is never returned by list, detail, search or facets, whatever its columns hold. Detail of an ineligible or unknown id is 404.
4. The rule is implemented once, as a single named predicate in the DAL (`TempleEligibility`), and is the only place that decides eligibility. 152 and 153 must call it, not restate it.
5. A non-null `krithis.temple_id` is treated as reviewed (TRACK-145 and TRACK-146: `temple_id` is written only by reviewed seeds; empty means unreviewed). The Plan confirms this against the writers of `krithis.temple_id` and states the finding; if a non-reviewed writer exists, the Plan adds a guard like TRACK-150's `reviewed_cycle`.
6. **Recorded gap** is derived, not stored: an eligible temple with `publishedKrithiCount = 0` for itself and for all its eligible children, and a published `KSHETRA_OVERVIEW`. It is returned with `recordedGap = true`; the overview text is the recorded reason. A temple without an overview and without published kritis is not eligible and is never shown as a gap.

**B. Endpoints (V2 only, anonymous, read-only)**

7. `GET /v2/catalogue/temples` returns `{items:[CatalogueTempleSummaryDto]}` with **root temples only** (`parent_temple_id IS NULL`), eligible per A, ordered by `mandalam` (declaration order of the enum, nulls last), then normalised name, then UUID. About 97 temples exist, so there is no pagination. Query parameters: `element` (optional, one `CatalogueTempleElementDto`) and `mandalam` (optional, one `CatalogueMandalamDto`). The element filter applies to the root temple's own `bhuta`. Unknown or repeated parameters, or an invalid enum value, return 400, as in other catalogue routes.
8. `GET /v2/catalogue/temples/{id}` returns `CatalogueTempleDetailDto`. A malformed id is 400; an unknown or ineligible id is 404. Child temples are addressable here by their own id and carry a `parent` reference.
9. Children are nested under their parent in the detail payload (eligible children only) and are **not** repeated in the list (Intent decision 3).
10. Grouping by mandalam is a presentation concern: the list stays flat and every item carries `mandalam`; `null` mandalam maps to an "Other places" group in mobile. Element is a filter only (Intent decision 1).
11. The detail `krithis` are the published krithi with `temple_id = this temple` (direct only, not children), as `CatalogueKrithiSummaryDto`, ordered by normalised title then UUID. Each row already carries its composer, so a screen never implies Dikshitar is the only composer with geography.
12. Existing clients ignore the new endpoints. V1 is untouched: `/v1/catalogue/temples` stays 404, and the editorial `/v1/temples` route is unaffected. The reader's `temple` reference (`CatalogueReferenceDto`, id and name) is unchanged; mobile uses its id to open the detail.

**C. DTOs (shared domain, `modules/shared/domain/.../catalogue/CatalogueTempleDtos.kt`, new)**

13. Enums, serialised as the database codes:
    - `CatalogueMandalamDto`: `CHOLA, PANDYA, TONDAI, NADU, CHERA, KONGU, UTTARA`.
    - `CataloguePlaceKindDto`: `LOCALITY, COMPLEX, SANNIDHI, MANDAPAM`.
    - `CatalogueTempleElementDto`: `AKASHA, VAYU, AGNI, PRITHVI, APPU` (the `bhuta_enum` values; mobile copy glosses them, `APPU` is water).
    - `CatalogueDeityPostureDto`: `SAYANA, STHANAKA, ASINA, TANDAVA`.
    - `CatalogueTemplePlacePrecisionDto`: `TEMPLE`, `PARENT_COMPLEX`.
14. `CatalogueTempleSummaryDto`: `id`, `name`, `city?`, `state?`, `placeKind`, `mandalam?`, `element?`, `publishedKrithiCount` (direct, `Long`), `childCount` (eligible children, `Int`), `recordedGap` (`Boolean`).
15. `CatalogueTemplePlaceDto`: `googlePlaceId` (`String`), `precision` (`CatalogueTemplePlacePrecisionDto`). Nothing else: no coordinates, name or address from Google.
16. `CatalogueTempleDetailDto`: all summary fields plus `country?`, `sthalaVriksha?`, `sthalaTirtha?`, `nadiTirtha?`, `deityPosture?`, `overview?` (`String`), `parent?` (`CatalogueReferenceDto`), `children` (`List<CatalogueTempleSummaryDto>`, default empty), `krithis` (`List<CatalogueKrithiSummaryDto>`, default empty), and `place?` (`CatalogueTemplePlaceDto`, default `null`).
17. **Place contract (for TRACK-154).** `place` is the optional place id and precision field. It is non-null only when the temple's place review state is `CONFIRMED`: `precision = TEMPLE` for a temple with its own confirmed place, `PARENT_COMPLEX` for a sannidhi or mandapam that inherits its parent's place. `LOCALITY_ONLY`, `UNRESOLVED` and unreviewed temples return `place = null`, so a consumer never receives a guessed place. Until TRACK-154's migration adds the column, the repository returns `null` and the DTO field already exists, so the contract does not change when 154 lands. Only `detail` carries `place`; the list does not.
18. Serialisation follows the server's `encodeDefaults = true`, `explicitNulls = false`. The new DTOs are V2-only types, so no V1 payload gains a field. `CatalogueV2Contract` gains `TEMPLES_PATH = "$ROOT/temples"`.
19. `overview` is the `original_content` of the temple's published `KSHETRA_OVERVIEW` row at the lowest `source_chunk_index`, returned as stored and never generated at request time. It is `null` when absent.

**D. OpenAPI**

20. `openapi/sangita-grantha.openapi.yaml` gains `/v2/catalogue/temples` and `/v2/catalogue/temples/{id}` (tag `Catalogue`, `security: []`, `RasikaSessionHeader` and `RasikaInteractionHeader` parameters, `CatalogueValidationError` and not-found responses, as for collections) and the schemas for items 13 to 16. The `place` schema description states the CONFIRMED-only rule and that the id is for building a Maps link, with no key.

**E. Mobile (Phase 2 and 3)**

21. Entry: a Kshetras shelf on Home beside Collections. Screens: Kshetras list (grouped by mandalam, with an element filter chip row), Kshetra detail.
22. List group headers use plain mandalam names with a short gloss. Rows show name, place, kriti count, and a gap marker (text, not colour only). Parents with children show "N shrines".
23. Detail shows the overview, the sthala fields that are present (absent fields are omitted, never "unknown"), nested child shrines (each opens its own detail), and the kritis sung there; each kriti opens the reader. A recorded gap shows the overview as "Recorded gap" with its reason and no empty kriti list.
24. The reader's temple chip becomes tappable and opens the detail. A temple that returns 404 (ineligible) falls back to a non-link label.
25. **Map (Phase 3).** When `place` is non-null, detail shows "Open in Google Maps", built client-side as `https://www.google.com/maps/search/?api=1&query=<temple name>&query_place_id=<googlePlaceId>`. It needs no API key. With `place = null` there is no map control and no pin. The embedded SDK map is a separate follow-up track.
26. Empty, loading and error states reuse `LoadState`. Each row and button has a content description. Fixtures cover Tiruvarur (parent with children, direct kritis), a Pancha Bhuta temple with an element, a gap temple (Kanyakumari) and a temple with a `PARENT_COMPLEX` place.

**F. Out of scope**

27. Search hits of kind KSHETRA (TRACK-152), region and element facets in search (TRACK-153), Google place-id data and its migration (TRACK-154), the embedded SDK map, a curator view, writes of any kind, V1 changes, any migration, and any embedding run.

### Design

- Route handlers delegate to `CatalogueService` (`listTemples`, `getTemple`), which returns `CatalogueResult` and rejects extra parameters and malformed ids like the collections handlers. The repository methods in `CatalogueRepository` (DAL) run in `DatabaseFactory.dbQuery` and return explicit DTOs.
- `TempleEligibility` is one internal predicate/CTE: `direct = EXISTS published kriti OR EXISTS published KSHETRA_OVERVIEW`, `eligible = direct OR EXISTS child WHERE direct`. List, detail and the later search and facet code call it. Counts come from one grouped query over eligible ids, not per-row queries.
- Detail loads the temple row, its eligible children (summary rows, one grouped count query), the overview (reusing the `cycleOverview` query shape with `temple_id`), and kritis through the existing `hydrateSummaries`, so ragas, tala and composer come back in the same shape as elsewhere.
- No migration is expected. `idx_temples_parent` and `idx_search_docs_temple_id` already exist; `krithis.temple_id` indexing is checked in the Plan against the explain output.
- Read-only endpoints do not write to `AUDIT_LOG`.
- Mobile follows the TRACK-150 shape: `CatalogueApi.listTemples/getTemple`, fixtures and fake API in the harness, presenters before screens, `RasikaDestination` entries, copy in `RasikaCopy`.
- Analytics reuse the optional Rasika session and interaction headers and the usage-recorder categories added for collections; no personal data.

### Flagged concerns

- **Eligibility drift.** Three tracks depend on one rule. Mitigation: one predicate, one contract test file (`TempleEligibilityTest`) that 152 and 153 extend rather than copy.
- **Reviewed-ness of `temple_id`.** The column has no review flag. Requirement 5 makes the Plan prove only reviewed writers set it.
- **Coverage skew.** Only Dikshitar kritis have temples (measured 2026-10-16). The list is true to the data but must not read as "kshetras of Carnatic music". Copy and the per-row composer avoid that claim; kritis with no temple are never labelled "no kshetra" (Intent).
- **Derived gap.** `recordedGap` depends on the worker having written `KSHETRA_OVERVIEW` and on there being no published kriti. A temple with an overview and only draft kritis would also read as a gap. The Plan checks the live data returns Kanyakumari only and states the result.
- **Kriti list size.** Detail has no pagination. The Plan measures the largest direct kriti count (Tiruvarur is the likely maximum) and sets a bound; above it the contract needs a page parameter before Phase 1 lands, not after.
- **Mandalam nulls.** All 97 temples carry `mandalam` today, but `KONGU` and `UTTARA` are defined and may be unassigned; empty groups must not render.
- **Google terms.** Only the place id is stored and exposed; the link carries the temple's own catalogue name and the id. Whether showing the place on a Maps link alone satisfies TRACK-154's reading of the display policy is TRACK-154's to confirm; this track ships no embedded Google content.
- **Terminology.** Mandalam and bhuta display names and the Appu/water gloss are content decisions for the Plan; no lakshana data is changed.
- **Existing `/v1/temples`.** Different audience (editorial, authenticated); not reused, to avoid leaking draft temples.

### Open questions carried forward

All four Intent questions are decided (2026-10-24). New for acceptance:

1. Is the container rule in requirement 2 (a parent is eligible through an eligible child) acceptable, or should an ineligible parent hide its eligible children?
2. Is `publishedKrithiCount` as direct-only (not rolled up over children) acceptable for a complex such as Tiruvarur, whose list row then shows its direct count plus "N shrines"?
3. Expose `PARENT_COMPLEX` precision for inherited places (as TRACK-154 describes), or return the parent's place as `TEMPLE`?

**Resolution (2026-10-24):** Seshadri accepted the Spec without changing these three, so the Spec as written stands: the container rule applies, `publishedKrithiCount` is direct-only, and inherited places report `PARENT_COMPLEX`.

## Plan

**Status:** Accepted
**Accepted by:** Seshadri
**Accepted at:** 2026-10-25

Written from the Spec above; it stays provisional until the Spec is accepted. Nothing is implemented. The three phases land separately. **Phase 1 is the contract phase that TRACK-152 and TRACK-153 wait for.**

### Files that change

| Path | Phase | Change |
|:---|:---|:---|
| `modules/shared/domain/.../catalogue/CatalogueTempleDtos.kt` (new) | 1 | Enums and `CatalogueTempleSummaryDto`, `CatalogueTemplePlaceDto`, `CatalogueTempleDetailDto`, list response |
| `.../catalogue/CatalogueV2Dtos.kt` (file renamed in Phase 0) | 1 | `TEMPLES_PATH` in `RasikaCatalogueContract` |
| `modules/backend/dal/.../repositories/CatalogueRepository.kt` | 1 | `TempleEligibility` predicate, `listTemples`, `findTemple`, overview read by temple, `place` returning null |
| `modules/backend/api/.../services/CatalogueService.kt` | 1 | `listTemples`, `getTemple` with parameter and id validation |
| `modules/backend/api/.../routes/RasikaCatalogueRoutes.kt` (renamed in Phase 0) | 1 | Two `get` handlers; usage-recorder categories |
| `openapi/sangita-grantha.openapi.yaml` | 1 | Two V2 paths and the schemas |
| `modules/backend/api/src/test/.../routes/CatalogueTemplesRoutesTest.kt` (new) | 1 | Testcontainers route tests on the real Flyway schema, including `TempleEligibility` cases |
| `application_documentation/03-api/` | 1 | Temple endpoints and the eligibility contract, marked as the shared rule for search and facets |
| `modules/shared/mobile-data/.../network/CatalogueApi.kt`, `KtorCatalogueApi.kt` | 2 | `listTemples`, `getTemple` |
| `.../fixture/CatalogueFixtures.kt`, `harness/RasikaUiTestHarness.kt` | 2 | Fixtures from Spec item 26 and a fake API |
| `modules/shared/presentation/.../kshetra/` (new: list and detail presenters and screens) | 2 | Grouping, element filter, nested children, gap state |
| `.../home/HomeScreen.kt`, `HomePresenter.kt`, `navigation/RasikaDestination.kt`, `RasikaApp.kt`, `RasikaCopy.kt`, `reader/KrithiReaderScreen.kt` | 2 | Kshetras shelf, destinations, copy and glossary, tappable temple chip |
| `CatalogueRepository.kt` (place read), detail screen, mobile tests | 3 | Read `google_place_id` for CONFIRMED rows after TRACK-154; "Open in Google Maps" link |
| `application_documentation/01-requirements/features/`, `conductor/tracks.md` | 2 | Feature doc; status line (the registry is touched only for status) |

No migration, no `AUDIT_LOG` write, no change to `modules/frontend`.

### Order of work

**Phase 0: rename away from numeric class names (own commit, no behaviour change; decided 2026-10-25, lands before Phase 1)**

0. Pure Kotlin renames, with URL paths (`/v2/catalogue/...`), OpenAPI and wire format unchanged:
   - `CatalogueV2Contract` to `RasikaCatalogueContract`; `catalogueV2Routes` and `CatalogueV2Routes.kt` to `rasikaCatalogueRoutes` and `RasikaCatalogueRoutes.kt`; `CatalogueV2DtoMappers` to `RasikaCatalogueMappers`; `CatalogueV2Dtos.kt` to a matching name.
   - `CatalogueVisibility.V1` to `LEGACY_KNOWN_FORMS` and `CatalogueVisibility.V2` to `ALL_PUBLISHED`.
   - Test classes follow their subjects (`CatalogueV2RoutesTest`, `CatalogueV2ClientTest`); test method names containing `V2` are kept unless they name a class.
   - Proof: `make test`, `make test-integration`, the shared module tests, and a diff showing no change to OpenAPI or JSON fixtures. Wherever this Spec and Plan say `CatalogueV2Contract`, `publishedComposition(V2)` or "V2 visibility", read the new names; the `/v2` URLs stay.
   - TRACK-150's accepted text is left as written (historical).

**Phase 1: contract and backend (separately landable; gates TRACK-152 and TRACK-153)**

1. Re-measure on the live database: eligible count, root count, overview-only temples, gap candidates, largest direct kriti count, and the writers of `krithis.temple_id`. Record results in the Progress Log; resolve Spec items 5, 6 and the kriti-list-size concern before coding.
2. Shared DTOs and `RasikaCatalogueContract.TEMPLES_PATH`; OpenAPI paths and schemas; serialisation test for all DTOs, including `place` omitted when null.
3. `TempleEligibility` and the list query: grouped counts in one query, ordering by mandalam, `element` and `mandalam` filters.
4. Detail query: row, eligible children, overview, kritis through `hydrateSummaries`, derived `recordedGap`.
5. Service and routes: extra-parameter 400, malformed id 400, ineligible 404.
6. Tests (below), docs, then stop. Phase 1 merges alone; 152 and 153 start from it.

**Phase 2: Rasika mobile**

7. Mobile data layer and fixtures, presenters with tests, then screens, shelf, reader link, copy and accessibility strings. Gap and no-place states included.

**Phase 3: map link (blocked on TRACK-154 landing CONFIRMED place ids)**

8. Repository reads the place column and review state; `place` populated for CONFIRMED only, with `PARENT_COMPLEX` for inheriting sub-shrines. Mobile link builder with a unit test for the URL; hidden when `place` is null.

### Risks

- **Eligibility mistakes leak drafts.** Tests cover: draft-only kriti (not eligible), published kriti, overview-only, unpublished overview, child-only-eligible parent, neither, plus a draft/published mix in the same temple counts only published.
- **Contract change after 152/153 start.** Mitigated by landing Phase 1 first and fixing the open questions in the Spec before it.
- **N+1 queries.** Counts and children are grouped queries; the route test asserts the response, and explain output is checked for `temple_id` lookups.
- **Place field before 154.** Always null; a test pins that no coordinate or Google field exists on the DTO.
- **Misleading coverage.** Copy review in Phase 2; no "all composers" claim.
- **Rejected:** a new table or migration; pagination for ~97 rows; repeating children in the flat list; sharing the editorial `/v1/temples` route; returning a Maps URL from the server; storing Google coordinates.

### Completion evidence (fixed)

Phase 1:
- `make test` and `make test-integration` with `CatalogueTemplesRoutesTest` covering the eligibility cases above, Tiruvarur-style parent/child nesting, gap derivation, 400 and 404 cases, enum filters, and V1 `/v1/catalogue/temples` returning 404.
- DTO serialisation tests in `:modules:shared:domain`.
- Live check on the local stack after `sangita-restart-on-kotlin-change`: `curl /v2/catalogue/temples` returns only root eligible temples; `/v2/catalogue/temples/{Tiruvarur id}` returns its children; for every listed temple, `publishedKrithiCount` equals a direct SQL count of published krithi with that `temple_id` (script output pasted); Kanyakumari is `recordedGap = true` with its overview; an ineligible temple is 404.
- `make check-docs` with the OpenAPI and API doc updated.

Phase 2:
- `./gradlew :modules:shared:domain:allTests :modules:shared:mobile-data:allTests :modules:shared:presentation:allTests` and the Android and iOS targets assembling.
- Presenter tests: mandalam grouping, empty groups hidden, element filter, gap state, nested children, 404 fallback.
- iOS simulator walk-through against the live backend: Home shelf, list with element filter, Tiruvarur detail with children and kritis, a kriti opening the reader, the reader temple chip opening the detail, the Kanyakumari gap.

Phase 3:
- Unit test for the Maps URL; route test that `place` is present for CONFIRMED (own and inherited) and absent for `LOCALITY_ONLY`, `UNRESOLVED` and unreviewed rows; simulator check that the link opens and that no control shows without a place.

All phases: `git diff --check`; `make agent-evals` only if `CLAUDE.md` or `.claude/` change (none planned).

## Implementation Checklist

- [x] Phase 1 (PR #43, 2026-10-25): shared DTOs, `TEMPLES_PATH`, OpenAPI, `TempleEligibility`, list and detail queries, service, routes, tests, docs.
- [x] Phase 2 (PR #49, 2026-11-01): mobile client, fixtures, presenters, screens, reader link.
- [x] Phase 3 (PR #53, open; mark Done when merged): place read and Google Maps link.

## Progress Log

- **2026-10-24**: Intent accepted with decisions on all four open questions. Spec written (Draft) and Plan written (Proposed); awaiting acceptance. No code changed.
- **2026-10-24**: Spec accepted by Seshadri; the three Spec questions resolved as written. Plan still Proposed.
- **2026-10-25**: Seshadri approved replacing numeric class names (`CatalogueV2*`, `CatalogueVisibility.V1/V2`) with `RasikaCatalogue*` and `LEGACY_KNOWN_FORMS` / `ALL_PUBLISHED`. Added to the Plan as Phase 0, ahead of Phase 1, rather than a separate track. TRACK-152 and TRACK-153 should use the new names. Plan still Proposed.
- **2026-10-25**: Phase 0 (rename to `RasikaCatalogue*`, `LEGACY_KNOWN_FORMS` / `ALL_PUBLISHED`) and Phase 1 (shared temple DTOs, `TempleEligibility`, `GET /v2/catalogue/temples` and `/{id}`, OpenAPI, tests, docs) merged in PR #43 (`3cb61ee`). `place` stayed null until TRACK-154.
- **2026-10-31**: Live API check against the dev database restored from the 27 Oct backup (V68 applied; 97 temples, 1,226 krithis). The list returns 84 root temples (82 COMPLEX, 2 LOCALITY); Tiruvarur has 114 direct kritis and 10 child shrines; Kanyakumari (Bhagavati Amman Temple) is the only `recordedGap`; `publishedKrithiCount` equals a direct SQL count for all 96 eligible temples; element and mandalam filters work; a bad enum, unknown or repeated parameter and a malformed id return 400, an unknown id returns 404, `/v1/catalogue/temples` returns 404; no `place` before TRACK-154 data. Phase 2 (PR #49) walked in the iOS simulator (iPhone 17, iOS 27.2) against the live backend: Home shelf, list grouped by region, element filter (Agni leaves only Arunachaleswarar), the recorded-gap row, detail pages, nested child shrines and the "Part of" link, a search card (TRACK-152) opening the detail, a kriti opening the reader from a detail, and the reader's "Sung at" chip opening the detail. Not walked: the Kanyakumari gap detail screen.
- **2026-11-01**: PR #49 (Phase 2, mobile) merged (`d9153f5`). TRACK-154 data and mapper merged (PRs #48, #50): 84 CONFIRMED, 1 LOCALITY_ONLY, 12 sub-shrines inheriting. Phase 3 (the "Open in Google Maps" link built from the place id, no key) is in PR #53, open: JVM tests, `make test`, `make check-docs` and an iOS simulator walk-through pass per the implementation session; the Android assemble was not run locally (no Android SDK) and is covered by CI.
- **Findings, not yet fixed**: the kshetra list loses its scroll position on back; the Tiruvarur detail renders 114 kritis in one long list and keeps scrolling after a swipe (possible performance issue); the reader's Tamil lyrics for hEramb Aya show stray "3" characters (a lyric or transliteration data issue unrelated to this track).
- **Follow-up**: the embedded Google Maps SDK map is a separate track, not yet created.
