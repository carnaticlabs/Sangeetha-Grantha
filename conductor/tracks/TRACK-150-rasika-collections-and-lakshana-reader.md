| Metadata | Value |
|:---|:---|
| **Status** | Completed |
| **Version** | 1.0.0 |
| **Last Updated** | 2026-10-16 |
| **Author** | Sangeetha Grantha Team |

# Track: Rasika Collections & Reader Cycle Context

**ID:** TRACK-150
**Status:** Completed
**Owner:** Seshadri
**Created:** 2026-10-16
**Updated:** 2026-10-16

## Goal

Make the reviewed Dikshitar, Syama Sastri and Tyagaraja classification data (TRACK-145/146/147) visible to Rasika mobile users: show a kriti's cycle context in the reader, and add browsable, ordered Collections (Navavarana, Navagraha, Pancha Bhuta Sthala, Ratnatrayam, Ghana-raga Pancharatnam and the other reviewed cycles).

## Context

- **Predecessors:** [TRACK-145](TRACK-145-dikshitar-kshetra-musicological-metadata.md), [TRACK-146](TRACK-146-syama-sastri-musicological-classification.md), [TRACK-147](TRACK-147-tyagaraja-musicological-classification.md). Data lives in `krithi_cycle_memberships`, `tags`, new `krithis`/`temples` columns and `CYCLE_OVERVIEW` search documents.
- **Current gap:** `CatalogueKrithiReader` ([openapi](../../openapi/sangita-grantha.openapi.yaml)) carries no cycle membership, vibhakti or occasion note, and `temple` is not populated. No endpoint lists cycles. Mobile has no Collections surface. The repository DTO `KrithiCycleMembershipDto` exists but is not part of the public catalogue contract.
- **Follow-ups not in scope:** kshetra exploration, search over macro documents, lakshana facet chips, public web. Coordinate with [TRACK-148](TRACK-148-syama-sastri-edition-consultation-adjudication.md) and [TRACK-149](TRACK-149-tyagaraja-edition-consultation-collision-split.md), which will add approved data later.

## Intent

**Status:** Accepted
**Accepted by:** Seshadri (explicit chat instruction, defaults confirmed for all open questions)
**Accepted at:** 2026-10-16

### Problem

Months of musicological curation are invisible to rasikas. A user reading *Kamalambikayastava* cannot learn that it is the sixth Avarana of the Navavarana, or move to the fifth or seventh. There is no way to browse a cycle in its proper order.

### Proposed outcome

1. **Reader cycle context (A).** The public reader payload gains cycle memberships (collection id, slug, name, role, sequence position, axis value and total members) plus vibhakti and occasion note where reviewed. The reader shows a "Part of" card with previous/next navigation inside the cycle.
2. **Collections (B).** New public, published-only endpoints list collections and return one collection with its ordered members, roles and overview text. Mobile adds a Collections shelf on Home/Browse, a collection detail screen with ordered members, and navigation into the reader.
3. **Shared contract.** DTOs live in `modules/shared/domain` and the OpenAPI spec, so Android and iOS use one change.

### Constraints

- Expose only reviewed data. `DISPUTED_CONJECTURE` members are labelled as such or withheld; unreviewed or deferred claims never appear. Empty geography means "not reviewed", not "no kshetra".
- Follow CLAUDE.md: `DatabaseFactory.dbQuery`, DTO boundary, OpenAPI sync, Flyway only (no schema change is expected), no hard-coded dependency versions.
- Cycles are open or partial by design (Shodasa Ganapati, Nottusvara, Tyagaraja Pancharatnam variants). The UI must not imply a completeness the data does not have.
- Web is curator-only for now. No public web surface in this track.

### Open questions (resolved 2026-10-16)

1. **Public cycles:** every cycle with reviewed memberships. Cycles with no reviewed members stay hidden.
2. **Overview text:** the existing `CYCLE_OVERVIEW` search document. No new editorial copy.
3. **Tyagaraja Pancharatnam order:** shown, labelled "traditional performance order".
4. **Curator web:** no cycle view in this track.
5. **Offline:** no new store. Online fetch with the existing catalogue `no-store` behaviour.
6. **Disputed members** (Rahu, Ketu): shown after core members with a "disputed" label.

### Affected users and systems

Rasika mobile users; backend catalogue routes and repositories; shared domain DTOs; OpenAPI spec; mobile presentation and fixtures.

### Completion evidence (to be fixed in Plan)

Backend and integration tests for the new endpoints, shared DTO serialisation tests, mobile presenter tests, `make test`, `make test-integration`, `make test-frontend` if touched, OpenAPI sync, and a live check on a seeded database that Navavarana returns 11 members in order.

## Spec

**Status:** Accepted
**Accepted by:** Seshadri (explicit chat instruction)
**Accepted at:** 2026-10-16

### Requirements

1. **Eligibility.** A collection is public when its tag has at least one membership whose kriti is `published` and whose membership was written by a reviewed seed (`R__seed_09`, `R__seed_10`, `R__seed_11`). Members whose kriti is not published are omitted. A collection with no remaining members is not listed. The Plan chooses the reviewed-ness guard (candidate: `krithi_tags.source = 'reviewed_cycle'` joined on the same krithi and tag); `krithi_cycle_memberships` has no review column, so the guard must not rely on row existence alone.
2. **Reader payload (A).** `CatalogueKrithiReader` gains optional fields, all backward compatible (defaults empty or null):
   - `collections`: list of `{collectionId, slug, name, role, sequenceOrder, position, memberCount, axisValue, orderBasis}`. `position` is the 1-based rank among visible members. `orderBasis` is `SOURCE` or `PERFORMANCE_CONVENTION` or `UNSPECIFIED`.
   - `previous` and `next`: the neighbouring visible member in the same collection, `{krithiId, title, ragaName}`, in `collections[i]`.
   - `vibhaktiCase` and `occasionNote`, only when non-null.
   - `temple`: populated from `temples` when `temple_id` is set (field already exists in the DTO).
3. **Collections list (B).** `GET /v1/catalogue/collections` returns `{items:[{id, slug, name, composer, memberCount, kind, hasOverview}], }` ordered by composer then name. No pagination parameters beyond the existing `CataloguePage` convention; the expected size is under 30. Unknown parameters return 400, as in other catalogue routes.
4. **Collection detail (B).** `GET /v1/catalogue/collections/{slug}` returns collection metadata, `overview` text, `orderBasis`, `isComplete` (false for open sets such as Shodasa Ganapati and Nottusvara), and ordered `members`: `{krithi:{id,title,ragas,tala}, role, sequenceOrder, position, axisValue, label}`. 404 for an unknown or ineligible slug.
5. **Ordering and roles.** Members sort by `sequence_order`, then normalized title, then UUID. Role order is `DHYANA`, `CORE`, `OPTIONAL`, `MANGALAM`, with sequence respected inside the collection; `DISPUTED_CONJECTURE` members always follow core members and carry `label = "disputed"`. `DHYANA` and `MANGALAM` carry no axis value and no enclosure number.
6. **Axis display.** `axisValue` is shown as the secondary line (Chakra name, lingam name or Nottusvara tune). Only an allow-list of `discriminative_attributes` keys is returned as typed fields; the raw JSON is never exposed. The allow-list is set in the Plan from the keys actually present in the seeds.
7. **Order basis.** Ghana-raga Pancharatnam returns `PERFORMANCE_CONVENTION` and mobile renders "traditional performance order". Cycles without a source-attested sequence must not display ordinal numbers; they show unnumbered lists.
8. **Overview text.** `overview` is read from the cycle's `CYCLE_OVERVIEW` search document. When absent, `overview` is null and `hasOverview` is false. The text is returned as stored and is not generated at request time.
9. **Mobile.**
   - Reader: a "Part of" card per collection with name, position ("6 of 9" only when `orderBasis` is `SOURCE`), axis value, and previous/next buttons that open the neighbouring reader.
   - Home or Browse: a Collections shelf, then a Collections list screen grouped by composer, then a detail screen with the overview and ordered members.
   - Empty, loading and error states reuse `LoadState`. Accessibility: each member row and navigation button has a content description; the disputed label is text, not colour only.
   - Fixtures in `CatalogueFixtures` cover Navavarana (11 members with dhyana and mangalam), Navagraha (7 core plus 2 disputed), and a performance-convention cycle.
10. **Shared contract.** New DTOs in `modules/shared/domain/.../catalogue/`, new endpoints in `openapi/sangita-grantha.openapi.yaml`, and a `CatalogueApi` extension in `modules/shared/mobile-data`. Existing clients that ignore unknown fields keep working.
11. **Analytics.** Reuse the optional `RasikaSessionHeader` and `RasikaInteractionHeader` conventions on the new routes; no credentials or personal data.
12. **Out of scope.** Kshetra/temple endpoints and screens, macro-document search, facet filters, a public web client, a curator cycle view, schema changes, writes of any kind, and embedding runs.

### Design

- Route handlers delegate to `CatalogueService`, which returns `CatalogueResult`. A repository method in the DAL runs inside `DatabaseFactory.dbQuery` and returns explicit DTOs, not Exposed rows. The existing `KrithiRepository.listCycleMemberships` is admin-shaped; the public path adds published-only, reviewed-only queries rather than widening it.
- Reader assembly adds one batched query for all memberships of the kriti and one for neighbours, avoiding per-collection round trips.
- No migration is expected. If an index is needed for `(tag_id, sequence_order)` it already exists (`idx_krithi_cycle_tag`). A migration would be a new `VNN__` file only.
- Collection slug comes from `tags.slug`. Composer for a collection is derived from its members, and a cycle with members of more than one composer shows the composer of the majority and is flagged in the Plan if it occurs.
- Read-only endpoints do not write to `AUDIT_LOG`; the CLAUDE.md rule covers mutations.

### Flagged concerns

- **Reviewed guard.** Because the membership table lacks a review flag, a manual or future import row could leak. The Plan must pick a guard and test it with an unreviewed fixture row.
- **Partial and disputed data.** Syama Sastri Ratnatrayam is the only reviewed Syama Sastri cycle, Tyagaraja has only 5 applied memberships, and Navaratnamalika membership is unresolved. The UI must show these as they are, not as incomplete bugs.
- **Tyagaraja Pancharatnam tag.** The legacy `STOTRA_STYLE` tag mirrors the cycle. The collection must be listed once, not once per mirror.
- **Vibhakti display.** `vibhaktiCase` is Sanskrit grammar terminology. Mobile copy needs a short gloss; the wording is a content decision for the Plan.
- **Performance.** Reader latency must not regress noticeably. Plan sets a measured bound.

### Open questions carried forward

1. Reviewed guard: `krithi_tags.source` join, or a new column in a later migration?
2. Allow-list of `discriminative_attributes` keys to expose.
3. Where the Collections shelf sits: Home, Browse, or both.
4. Glossary text for vibhakti and role labels.

## Plan

**Status:** Accepted
**Accepted by:** Seshadri (explicit chat instruction, including Spec amendments)
**Accepted at:** 2026-10-16

Written from the accepted Spec after reading `CatalogueService`, `CatalogueRepository`, `CatalogueV2Routes`, the V2 DTO contract, `KtorCatalogueApi`, the reader/home/browse presenters, and the `R__seed_09/10/11` seeds. Nothing is implemented.

### Spec amendments required (decided by this Plan, accepted with it)

1. **V2 only.** The mobile app reads `/v2/catalogue` (`CatalogueV2Contract.ROOT`). V1 is frozen by `CatalogueCompatibilityDecoderTest`. Collections endpoints are `GET /v2/catalogue/collections` and `/v2/catalogue/collections/{slug}`. They are added to `CatalogueV2Contract` and the OpenAPI spec, not `/v1`. This replaces the `/v1/catalogue/...` paths in Spec 3 and 4.
2. **One nullable field on the reader.** The server serialises with `encodeDefaults = true` and `explicitNulls = false`, so new defaulted list fields would appear in V1 responses. Spec 2 is amended: the reader gets a single `cycleContext: CatalogueCycleContextDto? = null`, populated only under `CatalogueVisibility.V2` and omitted when null. `collections`, `vibhaktiCase` and `occasionNote` live inside it. `temple` is already populated in both versions and is unchanged.
3. **Reviewed guard (Spec 1, open question 1).** A membership is visible when a `krithi_tags` row exists for the same `(krithi_id, tag_id)` with `source = 'reviewed_cycle'` (set by `R__seed_09`, `R__seed_10`, `R__seed_11`). No migration. The guard is one shared Exposed predicate used by both the reader and collection queries.
4. **Visibility.** Members use the existing `publishedComposition(V2)` predicate (published, including `UNESTABLISHED` form), matching what V2 readers already show.

### Files that change

| Path | Change |
|:---|:---|
| `modules/shared/domain/.../catalogue/CatalogueCollectionDtos.kt` (new) | `CatalogueCycleContextDto`, `CatalogueCollectionRefDto`, `CatalogueCollectionNeighbourDto`, `CatalogueCollectionSummaryDto`, `CatalogueCollectionDetailDto`, `CatalogueCollectionMemberDto`, `CatalogueOrderBasisDto` (`SOURCE`, `PERFORMANCE_CONVENTION`, `UNSPECIFIED`) |
| `.../catalogue/CatalogueDtos.kt` | Add `cycleContext: CatalogueCycleContextDto? = null` to `CatalogueKrithiReaderDto` |
| `.../catalogue/CatalogueV2Dtos.kt` | Add `COLLECTIONS_PATH` to `CatalogueV2Contract` |
| `modules/backend/dal/.../repositories/CatalogueRepository.kt` | `findCycleContext(krithiId)`, `listCollections()`, `findCollection(slug)`, reviewed-guard predicate; V2 reader fills `cycleContext` |
| `modules/backend/api/.../services/CatalogueService.kt` | `getCollections`, `getCollection`; extra-param rejection and slug validation like existing handlers |
| `modules/backend/api/.../routes/CatalogueV2Routes.kt` | Two `get` handlers delegating to the service |
| `openapi/sangita-grantha.openapi.yaml` | New V2 paths and schemas; reader schema gets `cycleContext` |
| `modules/shared/mobile-data/.../network/CatalogueApi.kt`, `KtorCatalogueApi.kt` | `listCollections`, `getCollection` |
| `modules/shared/mobile-data/.../fixture/CatalogueFixtures.kt`, `harness/RasikaUiTestHarness.kt` | Navavarana, Navagraha, Pancharatnam fixtures and a fake API |
| `modules/shared/presentation/.../reader/KrithiReaderScreen.kt`, `KrithiReaderPresenter.kt` | "Part of" card with previous/next |
| `modules/shared/presentation/.../collections/` (new: `CollectionsPresenter`, `CollectionsScreen`, `CollectionDetailPresenter`, `CollectionDetailScreen`) | List and detail |
| `.../home/HomeScreen.kt`, `HomePresenter.kt`, `navigation/RasikaDestination.kt`, `RasikaApp.kt`, `RasikaCopy.kt` | Collections shelf on Home, two destinations, copy and vibhakti/role glossary |
| `modules/backend/api/src/test/.../routes/CatalogueV2RoutesTest.kt` + new `CatalogueCollectionsTest.kt` | Route and guard tests |
| `modules/backend/dal/src/test/.../integration/CatalogueCollectionsIntegrationTest.kt` (new) | Flyway-seeded Postgres test of the repository queries |
| `modules/shared/mobile-data/src/commonTest/.../CatalogueV2ClientTest.kt`, presentation `commonTest` presenter tests | Client and presenter tests |
| `application_documentation/03-api/`, feature doc under `01-requirements/features/`, `conductor/tracks.md` | Docs sync and status |

No migration, no `AUDIT_LOG` write (read-only), no change to `modules/frontend`.

### Order of work

1. **Contract.** Shared DTOs and `CatalogueV2Contract`; OpenAPI paths and schemas; serialisation test with a frozen V1 decoder proving `cycleContext` is absent from V1 payloads.
2. **Repository.** Reviewed-guard predicate; `listCollections` (one grouped query on `tags` joined to guarded, published memberships); `findCollection` (members ordered by role rank, `sequence_order`, normalized title, UUID, with ragas and tala via the existing `hydrateSummaries`); `findCycleContext` (one query for the kriti's memberships, one for neighbours using `sequence_order` within visible members). Allow-listed attribute keys only: the Plan reads the keys present in the three seeds (`laya`, `structure`, `mudra` today) and exposes those as typed `attributes: Map<String,String>`, never raw JSON.
3. **Overview.** Read the `CYCLE_OVERVIEW` row for the tag from `search_documents`; null when absent.
4. **Order basis.** Derive from the seed's own data, not hard-coded names where possible: `SOURCE` when members carry axis values (Navavarana, Pancha Bhuta); `PERFORMANCE_CONVENTION` for `tyagaraja-ghana-raga-pancharatnam`; otherwise `UNSPECIFIED`. If derivation is not possible from stored attributes, use a small slug-to-basis map in one named constant, covered by a test. `isComplete` false for `shodasa-ganapati` and `nottusvara-sahitya`.
5. **Service and routes.** Wire both endpoints and the V2 reader.
6. **Mobile data layer.** API methods, fixtures, harness.
7. **Presenters, then screens.** Presenters first with tests; then reader card, shelf, list, detail; copy and accessibility strings.
8. **Docs and track.** API docs, feature document, track status.

### Risks and options rejected

- **Highest risk: leaking unreviewed memberships.** The table has no review column. The `reviewed_cycle` tag join is the guard; a test inserts an unreviewed membership and a published/draft mix and asserts neither appears. If the Tyagaraja legacy mirror tag has `source = 'reviewed_cycle'` without a membership, it never lists, because listing starts from memberships.
- **V1 regression.** Mitigated by the single nullable field and the V1 decoder test. Rejected: defaulted lists on the shared DTO (would serialise into V1).
- **Reader latency.** Two small indexed queries added. Measured bound: no more than 20 ms added at p95 on the seeded dev database; if exceeded, fold into one query.
- **Neighbours with gaps.** Neighbours are computed over visible members only, so unpublished members never produce dead links.
- **Misleading numbering.** "6 of 9" only for `SOURCE` order basis with a numeric axis; otherwise no ordinals. Tested in the presenter.
- **Disputed members.** Rahu and Ketu sort after core and carry the label as text. Tested.
- **Rejected options:** a new `collections` table or migration (data already exists); exposing raw `discriminative_attributes`; adding endpoints to `/v1`; generating overview text at request time; a curator-web view in this track.

### Proof and acceptance criteria

- `make test` (new API tests, V1 decoder test, shared serialisation and mobile client tests).
- `make test-integration` (Flyway-seeded repository test: Navavarana returns 11 members in order with dhyana first and mangalam last; Navagraha returns 7 core then 2 disputed; Ghana-raga Pancharatnam returns 5 with `PERFORMANCE_CONVENTION`; unreviewed and draft rows excluded).
- Shared and mobile modules: `./gradlew :modules:shared:domain:allTests :modules:shared:mobile-data:allTests :modules:shared:presentation:allTests` (or the module tasks present; confirm names before running).
- Live check against the local stack after `sangita-restart-on-kotlin-change`: `curl` `/v2/catalogue/collections` and one reader containing `cycleContext`; confirm V1 reader JSON has no `cycleContext`.
- UI: exercise reader, shelf, list and detail in the iOS Simulator or Android emulator with the harness fixtures and against the live API.
- `make check-docs`, `git diff --check`, and `make agent-evals` only if `CLAUDE.md` or `.claude/` changes (none planned).
- Data check per `verify-import`: collection member counts match `krithi_cycle_memberships` for each reviewed tag.

### Divergences from the accepted Plan (recorded 2026-10-16)

1. **Order basis rule.** The Plan derived `SOURCE` from any axis value. Live data showed that is wrong: Nottusvara and Tiruvarur Pancha Linga carry axis values (a European air, a lingam) that are labels, not a sequence. `SOURCE` now requires a numbered `avarana` attribute, so only Kamalamba Navavarana is `SOURCE`. Ratnatrayam, Pancha Bhuta and the vibhakti sets are `UNSPECIFIED` and show no ordinals. Covered by `an axis value alone does not claim a sequence`.
2. **Repository integration test.** The Plan listed a Flyway-seeded DAL test for Navavarana member counts. A blank test database has the seeds but no imported kritis, so it cannot hold Navavarana members. The reviewed guard, ordering, disputed labelling and order basis are instead covered by `CatalogueCollectionsRoutesTest` (Testcontainers, real Flyway schema), and the real seeded counts were checked against the live database below.
3. **Reader chip.** The header chip switches to the Details tab rather than opening the collection; the full card, previous/next and collection link live in Details.
4. **Position wording.** `position` and `coreCount` count CORE members only, so Navavarana reads "6 of 9" and Navagraha "7 core".

## Implementation Checklist

- [x] Shared DTOs, `CatalogueV2Contract.COLLECTIONS_PATH`, OpenAPI paths and schemas.
- [x] Reviewed-guarded repository queries, service and V2 routes; usage-recorder categories.
- [x] Mobile client, fixtures and harness fake; reader card, Home shelf, list and detail screens; `RasikaNavigator.replace`.
- [x] Backend, client and presenter tests.
- [x] API contract doc and implementation summary.

## Progress Log

- **2026-10-16**: Intent accepted with default answers to the open questions; Spec and Plan accepted by Seshadri, including the three Plan amendments (V2 only, one nullable `cycleContext`, `reviewed_cycle` guard).
- **2026-10-16**: Implemented. Proof:
  - `make test` and `make test-integration`: BUILD SUCCESSFUL (final state). `CatalogueCollectionsRoutesTest`: 7 tests, 0 failures. New shared tests: `CatalogueCollectionsClientTest` 5, `CollectionsPresenterTest` 7, 0 failures. `./gradlew :modules:shared:domain:allTests :modules:shared:mobile-data:allTests :modules:shared:presentation:allTests`: BUILD SUCCESSFUL.
  - `./gradlew :modules:shared:*:assemble` and an Xcode build of `RasikaApp` for the iOS simulator succeeded, so the Android and iOS targets compile.
  - Live API on the local database: 12 collections; every `memberCount` equals the number of reviewed, published memberships in `krithi_cycle_memberships` for that tag (for example Navavarana 11, Navagraha 9, Nottusvara 38, Shodasa Ganapati 17). Navavarana returns dhyana, Avaranas 1 to 9 with chakra and yogini, then mangalam. Navagraha returns seven core members then Rahu and Ketu labelled `disputed`. Ghana-raga Pancharatnam returns `PERFORMANCE_CONVENTION`. `/v2/catalogue/collections/missing` is 404, a bad slug and an unknown parameter are 400. The reader for *kamalAmbikAyAstava* returns position 6 of 9, previous Bhairavi (5th), next Sahana (7th) and vibhakti SHASHTHI. `/v1/catalogue/krithis/{id}` has no `cycleContext`, and `/v1/catalogue/collections` is 404.
  - iPhone 17 Pro simulator against the live backend: Home shelf, Navavarana detail with ordinals, reader chip "3 of 9", Details card with vibhakti, previous and next; Next opened the 4th kriti and one Back returned to the collection (stack not consumed).
  - `make check-docs` and `git diff --check`: clean. Not run: Android emulator, `make test-frontend` (no frontend change), a dark-mode or large-text visual pass.
