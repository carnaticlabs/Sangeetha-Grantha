| Metadata | Value |
|:---|:---|
| **Status** | Completed |
| **Version** | 1.0.0 |
| **Last Updated** | 2026-11-01 |
| **Author** | Sangeetha Grantha Team |

# Track: Rasika Lakshana Facets

**ID:** TRACK-153
**Status:** Complete: Phases 0 to 2 merged; later discriminators wait on TRACK-148 and 149
**Owner:** Seshadri
**Created:** 2026-10-16
**Updated:** 2026-11-01

## Goal

Surface reviewed musicological discriminators as tappable chips in the reader and as filters on Browse and Explore, so a rasika can move from "this kriti is in the Tritiya vibhakti" to every kriti with that property.

## Context

- **Predecessors:** [TRACK-150](TRACK-150-rasika-collections-and-lakshana-reader.md) shows `vibhaktiCase`, occasion and cycle attributes as read-only text in the reader.
- **Measured on the local database (2026-10-16), which decides what can honestly be a facet:**
  - `vibhakti_case`: set on all 481 Dikshitar kritis, none elsewhere (Dvitiya 311, Saptami 54, Chaturthi 41, Sambodhana 26, Tritiya 24, Shashthi 11, Prathama 9, Panchami 5).
  - `raga_mudra_kind`: `NONE` on all 1,226 kritis that have a value; no real data. `yati_pattern`: `NONE` on 1,225, one `GOPUCCHA`. **Not usable as facets.**
  - `is_manipravala`: one kriti. Not worth a facet.
  - Nottusvara tune name is `axis_value` on 38 memberships of one cycle.
  - Viloma Chapu, anagata eduppu and swarasahitya are **not stored anywhere in the catalogue**; they live only in the TRACK-146 master and are deferred to [TRACK-148](TRACK-148-syama-sastri-edition-consultation-adjudication.md).
  - Temple-level facets: `mandalam` on all 97 temples, `bhuta` on 5.
- The catalogue krithi search filters only by composer and raga (`/v2/catalogue/krithis`).

## Intent

**Status:** Accepted
**Accepted by:** Seshadri (2026-10-24)

### Problem

The idea of "insight chips" for Dikshitar, Syama Sastri and Tyagaraja overstates what the data supports today. Only Dikshitar has populated discriminators, and several named in the request (Viloma Chapu, Swarasahitya, raga mudra) are not in the database. Building chips over missing data would show empty or misleading filters.

### Proposed outcome

1. **Facets limited to reviewed, populated data:** vibhakti case, cycle membership (collection), Nottusvara tune, and kshetra region/element. Each facet is listed only when it has at least one published value.
2. **Facet filter on the V2 krithi search**, with counts per facet value, and unknown or empty facets rejected with a validation error.
3. **Reader chips** that apply the filter on Browse (vibhakti, tune, region). Chips appear only for populated values.
4. **A data-readiness table in the Spec** naming which discriminators wait on which track (Viloma Chapu, eduppu and swarasahitya wait on TRACK-148/149), so later data lands in the same facet mechanism without redesign.

### Constraints

- V2 only; `UNESTABLISHED` handling follows the V2 contract; no change to V1.
- Facets are derived from stored, reviewed columns and memberships; none may be computed from text.
- A facet is never shown for composers where it has no data (no "Vibhakti" on Tyagaraja).
- Filtering must combine with the existing composer and raga filters using AND, like the current endpoint.

### Open questions

1. Is vibhakti a filter on its own, or only inside a cycle (Dikshitar vibhakti cycles)?
2. Facet counts: always returned, or only on request?
3. Where do chips go in the reader: header, Details, or both?
4. Should this wait for TRACK-148/149 so Viloma Chapu and swarasahitya can launch in the same change?

### Decisions on open questions (2026-10-24)

1. Vibhakti is a standalone filter (shown only for composers with data).
2. Facet counts are returned only on request.
3. Chips appear in the reader's Details section.
4. Ship now; Viloma Chapu, eduppu and swarasahitya reuse the mechanism when TRACK-148/149 land the data.

### Completion evidence (fixed in Plan)

Repository and route tests for each facet and for combined filters, shared and presenter tests, live counts matching the database, simulator walk-through, `make test`, `make test-integration`, `make check-docs`.

## Spec

**Status:** Accepted
**Accepted by:** Seshadri (chat instruction)
**Accepted at:** 2026-10-25

Written from the accepted Intent after reading `V66`, `R__seed_09` (vibhakti writer), `CatalogueRepository` (`reviewedMemberRows`, `cycleContext`, `krithiFilter`, `publishedComposition`), `CatalogueParameters`, `CatalogueService`, the reader DTOs and `DetailsPane`, and TRACK-151's Spec, Plan and **Phase 0-1 code on `claude/track-151-kshetra-contract-df8da3`** (read with `git show`, not checked out): `TempleEligibility` (`snapshot()` / `eligibleIds()`), `RasikaCatalogueContract`, `CatalogueVisibility.ALL_PUBLISHED`. Nothing is implemented. Counts below were measured on 2026-10-25 against the dev database volume (`sangeetha-grantha_pgdata18`, Flyway V67, 1,226 kritis) started read-only on a spare port, because another session's DB held 5432. The `carnatic-musicologist` review was not run: no seed, import, raga or tala content changes; the track only exposes already-reviewed columns and memberships. Vibhakti display names reuse the existing `CollectionCopy.vibhakti`.

**Names.** This Spec uses TRACK-151's Phase 0 names (`RasikaCatalogueContract`, `RasikaCatalogueRoutes`, `CatalogueVisibility.ALL_PUBLISHED`). Wherever TRACK-152/151 wording says "V2 visibility", read `ALL_PUBLISHED`. `/v2` URLs are unchanged.

### Measured facet data (dev DB, 2026-10-25)

Published krithi: Tyagaraja 674, Dikshitar 481, Syama Sastri 70 (one further Tyagaraja kriti is draft and carries no facet data). All 481 vibhakti rows are published; `krithi_cycle_memberships` has 124 rows, **all** carrying the `reviewed_cycle` guard row (0 unreviewed).

| Facet | Source | Values with ≥1 published krithi | Composers with data |
|:---|:---|:---|:---|
| `vibhakti` | `krithis.vibhakti_case` | 8 of 9 enum values: DVITIYA 311, SAPTAMI 54, CHATURTHI 41, SAMBODHANA 26, TRITIYA 24, SHASHTHI 11, PRATHAMA 9, PANCHAMI 5 (`SARVA_VIBHAKTI` unpopulated) | Dikshitar only (481) |
| `collection` | reviewed cycle memberships (all roles) | 12 cycles: nottusvara-sahitya 38, shodasa-ganapati 17, kamalamba-navavarnam 11, navagraha-krithis 9, abhayamba-vibhakti 8, nilotpalamba-vibhakti 8, tyagaraja-vibhakti 8, guruguha-vibhakti 7, tiruvarur-panchalinga 5, pancha-bhuta-sthala 5, tyagaraja-pancharatnam 5, syama-sastri-swarajathi-ratnatrayam 3 | Dikshitar 10 cycles, Tyagaraja 1, Syama Sastri 1 |
| `tune` | `axis_value` of `nottusvara-sahitya` | 7 values, 1 kriti each (Ah vous dirai-je Maman, Castilian Maid, Galopede, God Save the King, Lord MacDonald's Reel, Rakes of Mallow, Voulez-vous danser) | Dikshitar only |
| `mandalam` | `temples.mandalam` via the kriti's temple | CHOLA 322, TONDAI 88, PANDYA 48, UTTARA 17, CHERA 4, NADU 1, KONGU 1 (sum 481) | Dikshitar only |
| `element` | `temples.bhuta` | AKASHA 8, PRITHVI 9, APPU 5, VAYU 2, AGNI 1 (sum 25) | Dikshitar only |

Corrections to the Intent's context: the Nottusvara cycle has 38 members but the tune is stored on **7** of them, not 38; `KONGU` has one temple and one kriti (TRACK-151 assumed it unassigned); all 97 temples are eligible under `TempleEligibility` (84 root, 13 child). `raga_mudra_kind`, `yati_pattern` and `is_manipravala` stay out, as the Intent already decided.

### Requirements

**A. Facet catalogue (what exists, and when)**

1. Exactly five facets exist in this track. Wire keys are the query-parameter names: `vibhakti`, `collection`, `tune`, `mandalam`, `element`. User-facing copy calls `mandalam` "Region". No other discriminator is a facet.
2. Each facet is derived only from stored, reviewed data, never from text:
   - `vibhakti`: `krithis.vibhakti_case IS NOT NULL`. Its only writer is `R__seed_09` (a reviewed seed); the Plan re-confirms no import or worker path writes it.
   - `collection`: a reviewed membership, using the same guard as collections (`krithi_cycle_memberships` joined to `krithi_tags.source = 'reviewed_cycle'` on the same krithi and tag). Value is the tag **slug**, as in `/v2/catalogue/collections/{slug}`. All roles count, as in collection `memberCount`.
   - `tune`: `axis_value IS NOT NULL` on a reviewed membership of the cycle slug `nottusvara-sahitya`, and only that cycle (other cycles' axis values are avaranas, lingas, ragas, not tunes). Value is the stored string, unmodified.
   - `mandalam` / `element`: the krithi's own `temple_id` temple's `mandalam` / `bhuta`, **only when that temple is eligible per `TempleEligibility`** (TRACK-151 Spec A). Not inherited from a parent complex: a krithi at a child shrine uses the child's own columns. A krithi with no temple has no value, which means unreviewed, never "no region".
3. Only **published** krithi count (`publishedComposition(ALL_PUBLISHED)`; `UNESTABLISHED` follows the TRACK-150/151 V2 rule). Draft or unreviewed rows never contribute a value or a count.
4. A facet is **listed only when it has at least one published value in the request's scope** (below); a value is listed only when it has at least one published krithi. A facet with no values is omitted entirely, so there is no "Vibhakti" for Tyagaraja or Syama Sastri, no tune outside Dikshitar, no region for composers without temples.
5. Adding a later facet (Viloma Chapu, eduppu, swarasahitya, raga mudra, yati) is adding one `FacetDefinition` entry (key, value source, label rule) in the DAL and one enum member in the shared DTO. It needs no new endpoint, parameter parser or response shape. This is the reuse promise of Intent decision 4.

**B. Filter on the V2 krithi search**

6. `GET /v2/catalogue/krithis` accepts optional `vibhakti`, `collection`, `tune`, `mandalam`, `element` (one value each) alongside `query`, `composerId`, `ragaId`, `page`, `pageSize`. All supplied filters combine with **AND**, across facets and with composer, raga and `query`. Within one facet only one value is allowed (a repeated parameter is already a 400).
7. Validation (400, `CatalogueValidationError` shape, as today). Unknown facet parameters stay "Unsupported parameter". A facet value is rejected when it is: blank or whitespace; longer than 64 code points; an enum facet value that is not a member of the enum; or **not a value currently offered by that facet over the whole published catalogue** (a `collection` slug that is not a reviewed published cycle, a `tune` not among the stored tunes, an enum member with zero published krithi such as `SARVA_VIBHAKTI`). The message names the facet and, for enums, lists the valid members. A *valid* value that matches nothing once combined with other filters is **not** an error: it returns 200 with `total = 0` (for example `vibhakti=DVITIYA&composerId=<Tyagaraja>`).
8. `/v1/catalogue/krithis` is unchanged and rejects the five parameters as unsupported, as today.
9. Ordering, paging and the item shape (`CatalogueKrithiSummaryDto`) are unchanged. No facet fields are added to items.
10. Existing clients that send none of the new parameters get byte-identical responses.

**C. Facet listing and counts (on request only)**

11. New read endpoint `GET /v2/catalogue/facets`, anonymous, V2 only. Parameters: `query`, `composerId`, `ragaId`, the five facet filters, and `counts` (`true` | `false`, default `false`). Anything else, repeats and malformed values are 400 by the same rules as B.7.
12. Response `CatalogueFacetListDto { facets: List<CatalogueFacetDto> }`; `CatalogueFacetDto { key: CatalogueFacetKeyDto, values: List<CatalogueFacetValueDto> }`; `CatalogueFacetValueDto { value: String, label: String? = null, count: Long? = null }`. `key` serialises as the wire keys in A.1 (enum `VIBHAKTI, COLLECTION, TUNE, MANDALAM, ELEMENT` with explicit serial names). `label` is set for `collection` (tag display name) and `tune` (the stored string); it is `null` for enum facets, whose display copy lives in `RasikaCopy` / `CollectionCopy` like TRACK-151's element glosses. `count` is `null` unless `counts=true`.
13. **Which values are listed** is computed over the scope of `query`, `composerId` and `ragaId` only, ignoring the five facet filters, so choosing a value does not make its siblings disappear. **Counts**, when requested, are computed over scope plus every *other* facet's active filter but not the facet's own (standard drill-down), so a listed value may legitimately show `count = 0`. Facets and values are ordered: `vibhakti` by enum declaration order, `collection` by name, `tune` by name, `mandalam` and `element` by enum declaration order; facet order is the A.1 order.
14. Counts are never computed unless `counts=true`; the default listing runs no grouped count query. The search endpoint never returns facets.
15. The endpoint returns `facets = []` (200) when no facet has a value in scope, never an error.
16. Counting uses `COUNT(DISTINCT krithi)`; a krithi that is a member of two cycles counts once per cycle value but never twice within one value.

**D. Reader chips**

17. `CatalogueKrithiReaderDto` gains `facetChips: List<CatalogueFacetChipDto> = emptyList()` where `CatalogueFacetChipDto { key: CatalogueFacetKeyDto, value: String, label: String? = null }`. The server derives chips from the same `FacetDefinition`s as the filter, so a chip value is always one the filter accepts. Chips are emitted only for `vibhakti`, `tune` and `mandalam` (Intent decision 3 scope) and only when the kriti has a stored value. `collection` membership keeps its existing `CycleContextSection` link; `element` is a filter only.
18. Chips render in the reader's **Details** section only, not the header. Tapping a chip opens Browse with that single facet applied (composer and raga cleared), titled with the chip label. Chips for facets the kriti has no data for are not rendered; a kriti with no chips shows no empty row.
19. V1 reader payloads gain no field (`facetChips` is on the V2-only reader DTO and the V1 route still emits the pre-existing DTO).

**E. Mobile Browse and Explore**

20. The krithi search request model gains the five optional facet selections. `CatalogueApi.searchKrithis` and its fakes carry them; `CatalogueApi.listFacets(...)` is new.
21. Browse shows a facet filter row only for facets returned by `GET /v2/catalogue/facets` for the current scope. Selecting Dikshitar shows Vibhakti, Cycle, Tune, Region, Element; selecting Tyagaraja shows Cycle only; with no composer chosen the facets reflect the whole catalogue. Counts are requested only when the user opens a facet's picker.
22. A selected facet shows as a removable chip. Clearing composer or raga recomputes the facet list and drops any selected value no longer offered.
23. The existing mobile type `ExploreFacets` (composer and raga labels) is unrelated; new types are named `LakshanaFacetSelection` / `LakshanaFacetUi` to avoid confusion.
24. Empty and error states reuse `LoadState`. Each chip and picker row has a content description. Copy lives in `RasikaCopy`/`CollectionCopy`; "Region" glosses mandalam; no claim that facets cover all composers.

**F. Data-readiness table (what waits on what)**

25. The Spec's table is also written to `application_documentation/03-api/` so later tracks update it.

| Discriminator | Stored today? | Facet status | Waits on |
|:---|:---|:---|:---|
| Vibhakti case | Yes, 481 Dikshitar (`krithis.vibhakti_case`) | Ships here | none (Tyagaraja/Syama Sastri have no vibhakti data) |
| Cycle membership | Yes, 124 reviewed memberships, 12 cycles, 3 composers | Ships here | none |
| Nottusvara tune | Yes, 7 of 38 memberships (`axis_value`) | Ships here | The 31 untuned memberships need curator review |
| Kshetra region (`mandalam`) | Yes, 97 temples, 481 Dikshitar kritis | Ships here | Syama Sastri geography is `UNRESOLVED` (TRACK-146); Tyagaraja kritis carry no temple |
| Kshetra element (`bhuta`) | Yes, 5 temples, 25 kritis | Ships here | none (small by nature) |
| Viloma Chapu | **No** (TRACK-146 master only) | Not a facet | TRACK-148 (Syama Sastri adjudication) / TRACK-149 |
| Anagata eduppu | **No** | Not a facet | TRACK-148 / TRACK-149 |
| Swarasahitya | **No** | Not a facet | TRACK-148 / TRACK-149 |
| Raga mudra kind | `NONE` on all 1,226 | Not a facet | A reviewed raga-mudra pass (no track yet) |
| Yati pattern | `NONE` on 1,225, one `GOPUCCHA` | Not a facet | A reviewed yati pass (no track yet) |
| Manipravala | 1 kriti | Not a facet | Not worth a facet |

**G. Out of scope**

26. Facet-aware search over text (TRACK-152), temple endpoints (TRACK-151), place ids (TRACK-154), V1 changes, any migration, any write or audit event, curator-side facet management, Viloma Chapu / eduppu / swarasahitya data, a cycle chip in the reader, and facets on ragas or composers.

### Design

- **Where it lives.** `FacetDefinition` registry in the DAL beside `CatalogueRepository` (a new `CatalogueFacets.kt` to avoid growing that file): for each key, the value query, the per-krithi value query, the filter predicate, and the validation set. `CatalogueRepository.krithiFilter` takes an optional `FacetSelection` and ANDs each predicate onto the existing condition, so `searchKrithis`, the facet counts and the reader chips share one predicate per facet.
- **Eligibility reuse.** `mandalam`/`element` predicates call `TempleEligibility` and never restate the rule. `TempleEligibility.eligibleIds()` returns a set (at most 97 ids) used as `temple_id IN (...)`; the Plan decides between that and exposing the CTE as a SQL fragment after reading the explain output. If TRACK-151 Phase 1 changes the predicate's shape before merge, this track adapts to it.
- **Reviewed guard reuse.** The reviewed-membership join in `reviewedMemberRows` is extracted into a shared `reviewedMembershipPredicate` so facets and collections cannot drift. `REVIEWED_CYCLE_SOURCE` stays the single constant.
- **Tune binding.** The tune facet is pinned to the cycle slug `nottusvara-sahitya` in one named constant in the facet registry. This is the one hard-coded slug (see Flagged concerns).
- **Parsing.** `CatalogueParameters.parseKrithiSearch` gains an `allowFacets` flag (false for V1, true for V2) and a `parseFacets` for the new endpoint. Enum values are checked against the shared DTO entries as `parseTempleList` does. Dynamic values (slug, tune) are validated in the service against the repository's `facetValueSets()` result, so a stale slug is a 400, not an empty page.
- **Service/route.** `CatalogueService.listFacets(params)` returns `CatalogueResult<CatalogueFacetListDto>`; the route is `get("/facets")` under `RasikaCatalogueRoutes` with a new `FACETS_PATH` in `RasikaCatalogueContract`. Usage-recorder categories for the endpoint and the facet parameters follow the collections pattern, with no personal data.
- **Queries.** Value listing: one `UNION ALL` of five small grouped queries under the scope predicate. Counts: one grouped query per facet, each with that facet's own filter removed, run only when `counts=true`. At 1,226 kritis and 124 memberships no new index is justified; the Plan checks `EXPLAIN` and records it. No migration.
- **Reader chips.** `findPublishedReader` already loads the krithi row, temple and cycle memberships; chips are built from the same row (`vibhakti_case`, the Nottusvara membership's `axis_value`, the temple's `mandalam` when eligible). No extra query beyond one temple lookup the reader already performs.
- **Mobile.** Shared domain DTOs in `CatalogueFacetDtos.kt` (new). `mobile-data`: `CatalogueApi.listFacets`, extended `searchKrithis`, fixtures and fake. Presentation: `BrowsePresenter` holds the selection and refreshes facets on scope change; `KrithiReaderScreen.DetailsPane` renders chips; `RasikaDestination` gets a Browse-with-facet entry or Browse parameters.
- **Read-only.** No `AUDIT_LOG` writes.

### Flagged concerns

- **Dependency on TRACK-151 Phase 0-1 (not merged).** This Spec relies on `TempleEligibility`, `RasikaCatalogueContract` and the renamed route/service files, all on `claude/track-151-kshetra-contract-df8da3`. The Plan starts only after that branch merges (or this track is rebased onto it). Files overlap heavily (`CatalogueRepository`, `CatalogueParameters`, `CatalogueService`, `RasikaCatalogueRoutes`, OpenAPI), so two parallel branches would conflict.
- **Possible overlap with TRACK-152.** If 152 also adds a facet or filter vocabulary to search, the wire keys and `FacetDefinition` registry here should be the one source; the Plan checks 152's Spec before coding. Not read here because its file is being edited by another session.
- **Hard-coded tune slug.** `tune` is bound to `nottusvara-sahitya`, because the schema has no "axis kind" on a cycle. A cycle-level `axis_kind` column is the clean fix but needs a migration and a seed change, which this read-only track rejects. If the cycle is renamed, the tune facet would silently vanish; a test pins the slug.
- **Weak tune facet.** Seven values with one kriti each is honest but low-value as a filter. Kept per Intent; the picker treats it as a list, not a count-ranked facet.
- **"Reviewed" for vibhakti/mandalam/element.** These columns have no review flag. Vibhakti is written only by `R__seed_09`; `temple_id` is trusted because only published kritis count (TRACK-151 Spec A.5). The Plan proves the vibhakti writer set and records it.
- **Coverage skew.** Four of five facets exist only for Dikshitar. The UI must not read as "facets of Carnatic music"; showing only populated facets per composer (A.4) is the mitigation, and copy avoids all-composer claims.
- **Cycles whose members include DISPUTED_CONJECTURE / OPTIONAL roles.** All roles count in `collection` (as in collection `memberCount`: 2 and 2 rows today). The alternative (CORE only) would make filter counts disagree with the collection screen. Flagged for Seshadri.
- **Region on child shrines.** A kriti at a child shrine uses the child's own `mandalam`; a child lacking one would drop out of region counts though its parent has one. All 97 temples carry `mandalam` today, so no effect now.
- **Terminology.** Vibhakti labels reuse `CollectionCopy.vibhakti`; mandalam and bhuta glosses ("Appu" = water) are content decisions the Plan puts in copy review. No lakshana data is changed.
- **Privacy.** Filter values are logged only as category/parameter names, not free text.

### Open questions carried forward

All four Intent questions are decided (2026-10-24). New for acceptance:

1. Is an enum value with zero published krithi (for example `SARVA_VIBHAKTI`) correctly a 400, while a valid value that matches nothing in combination is a 200 with `total = 0` (B.7)?
2. Should `collection` count every role (including OPTIONAL and DISPUTED_CONJECTURE) as the collection screen does, or CORE only?
3. Is a separate `GET /v2/catalogue/facets` endpoint acceptable, or should facets be embedded in the search response when `counts=true`? (The Spec chose the separate endpoint so the search payload and V1 stay untouched.)
4. Is it acceptable to pin the tune facet to the `nottusvara-sahitya` slug until a cycle `axis_kind` exists?

**Resolution (2026-10-25):** Seshadri accepted the Spec without changing these four, so the Spec as written stands: zero-published enum values are 400, `collection` counts every role, facets are a separate `/v2/catalogue/facets` endpoint, and `tune` is pinned to `nottusvara-sahitya`.

## Plan

**Status:** Accepted
**Accepted by:** Seshadri (chat instruction)
**Accepted at:** 2026-10-25

Written from the Spec above; it is provisional until the Spec is accepted. Nothing is implemented. **It starts only after TRACK-151 Phase 0-1 is merged to `main`.**

### Files that change

| Path | Phase | Change |
|:---|:---|:---|
| `modules/shared/domain/.../catalogue/CatalogueFacetDtos.kt` (new) | 1 | `CatalogueFacetKeyDto`, `CatalogueFacetValueDto`, `CatalogueFacetDto`, `CatalogueFacetListDto`, `CatalogueFacetChipDto` |
| `.../catalogue/CatalogueDtos.kt` | 1 | `facetChips` on `CatalogueKrithiReaderDto` |
| `.../catalogue/RasikaCatalogueDtos.kt` | 1 | `FACETS_PATH` in `RasikaCatalogueContract` |
| `modules/backend/dal/.../repositories/CatalogueFacets.kt` (new) | 1 | `FacetDefinition` registry, value sets, scoped listing, counts, chip builder |
| `modules/backend/dal/.../repositories/CatalogueRepository.kt` | 1 | `krithiFilter` takes `FacetSelection`; shared `reviewedMembershipPredicate`; chips in `findPublishedReader` |
| `modules/backend/dal/.../repositories/TempleEligibility.kt` | 1 | Only if a SQL-fragment accessor is needed (decided by explain output); no change to the rule |
| `modules/backend/api/.../catalogue/CatalogueParameters.kt` | 1 | `allowFacets` flag, facet parsing, facets-endpoint parser |
| `modules/backend/api/.../services/CatalogueService.kt` | 1 | Facet validation, `listFacets`, pass-through on V2 search |
| `modules/backend/api/.../routes/RasikaCatalogueRoutes.kt`, `services/CatalogueUsageRecorder.kt` | 1 | `get("/facets")`; usage categories |
| `openapi/sangita-grantha.openapi.yaml` | 1 | Five search parameters, `/v2/catalogue/facets`, schemas, `facetChips` |
| `modules/backend/api/src/test/.../routes/CatalogueFacetsRoutesTest.kt` (new), `RasikaCatalogueRoutesTest.kt` | 1 | Route tests (below) |
| `modules/backend/dal/src/test/.../integration/CatalogueFacetsTest.kt` (new) | 1 | Repository facet and AND-combination tests on the real Flyway schema |
| `modules/shared/domain/src/commonTest/.../CatalogueFacetDtosTest.kt` (new) | 1 | Serialisation, explicit nulls, wire keys |
| `application_documentation/03-api/` | 1 | Facet contract and the readiness table (Spec F) |
| `modules/shared/mobile-data/.../network/CatalogueApi.kt`, `KtorCatalogueApi.kt`, `repository/CatalogueRepository.kt` | 2 | Facet params on `searchKrithis`, `listFacets` |
| `.../fixture/CatalogueFixtures.kt`, `harness/RasikaUiTestHarness.kt` | 2 | Facet fixtures (Dikshitar five facets, Tyagaraja cycle only) and fake API |
| `modules/shared/presentation/.../browse/BrowsePresenter.kt`, `BrowseScreen.kt` | 2 | `LakshanaFacetSelection`, facet row, removable chips, picker with on-demand counts |
| `.../reader/KrithiReaderScreen.kt`, `KrithiReaderPresenter.kt`, `navigation/RasikaDestination.kt`, `RasikaApp.kt`, `RasikaCopy.kt` | 2 | Details chips, chip tap opens Browse with the facet, copy |
| `application_documentation/01-requirements/features/`, `conductor/tracks.md` | 2 | Feature doc; status line only |

No migration, no `AUDIT_LOG` write, no change to `modules/frontend` or V1 routes.

### Order of work

**Phase 0 (gate): confirm the base.** Verify TRACK-151 Phase 0-1 is on `main`; re-read `TempleEligibility`, `CatalogueParameters` and the route file; read TRACK-152's Spec for overlap with the facet vocabulary. Re-run the Spec's measurements after the base lands (`docker exec ... psql`) and record them in the Progress Log; confirm no writer of `vibhakti_case` other than `R__seed_09` (`rg vibhakti_case database modules tools`). Stop and update the Spec if any count differs.

**Phase 1: contract and backend (lands alone; mobile depends on it)**

1. Shared DTOs, `FACETS_PATH`, OpenAPI paths and schemas; serialisation test.
2. `CatalogueFacets.kt`: definitions, value sets, scoped listing and counts; extract `reviewedMembershipPredicate`; checks `EXPLAIN` for `temple_id`, `vibhakti_case` and membership scans and records the result.
3. Wire `FacetSelection` into `krithiFilter` and `searchKrithis`; parameters and service validation; V1 still rejects the parameters.
4. `listFacets` service and route; usage categories.
5. Reader chips in `findPublishedReader`.
6. Tests (below), API docs, then stop.

**Phase 2: Rasika mobile**

7. Data layer and fixtures, then presenters and tests, then Browse UI, reader chips and copy; accessibility strings last.

### Risks

- **Eligibility leak or drift.** Region/element tests include a draft-only temple kriti, an ineligible temple and a child shrine; the facet code calls `TempleEligibility` only. Riskiest step: Phase 1 step 3 (changing the shared `krithiFilter` used by every krithi search); mitigated by a regression test that a request with no facets returns the same page as before.
- **Wrong "reviewed" assumption.** A test inserts a membership without the `reviewed_cycle` row and asserts it never appears in `collection` or `tune`.
- **Count/filter disagreement.** A test asserts, for every listed value, that `counts=true` equals the `total` of the matching search under the same other filters, and that live counts equal direct SQL (Spec table).
- **Hard-coded tune slug** vanishing; pinned by a test that fails if the slug stops resolving.
- **Merge conflicts with TRACK-151/152** in shared files; mitigated by the gate above.
- **Rejected:** facets in the search response (changes the payload); a facet table or migration; computing any facet from text; a CORE-only collection facet (disagrees with collection counts); a separate vibhakti-inside-cycle filter (Intent decision 1); region facets from a parent complex; including raga mudra, yati or manipravala.

### Proof

Phase 1:
- `make test` and `make test-integration`, with `CatalogueFacetsRoutesTest`, `CatalogueFacetsTest` and `RasikaCatalogueRoutesTest` covering: each facet's value set; omission for composers without data (Tyagaraja has only `collection`, Syama Sastri only `collection`); AND with composer, raga, `query` and across facets; 400 for unknown parameter, blank, overlong, bad enum, unlisted slug/tune and zero-published enum; 200 with `total = 0` for a valid combination that matches nothing; `counts` absent by default and equal to direct counts when requested; V1 rejects facet parameters; chips present only for populated values; a no-facet search returns the pre-change page.
- `./gradlew :modules:shared:domain:allTests` for the DTO tests.
- Live check after `sangita-restart-on-kotlin-change`: `curl '/v2/catalogue/facets?composerId=<Dikshitar>&counts=true'` matches the Spec table; `curl '/v2/catalogue/krithis?vibhakti=TRITIYA&composerId=<Dikshitar>'` has `total` 24; `vibhakti=TRITIYA&composerId=<Tyagaraja>` returns 200, `total` 0; `/v2/catalogue/facets?composerId=<Tyagaraja>` lists only `collection`; a direct SQL count script output is pasted.
- `make check-docs`.

Phase 2:
- `./gradlew :modules:shared:domain:allTests :modules:shared:mobile-data:allTests :modules:shared:presentation:allTests`, and the Android and iOS targets assembling.
- Presenter tests: facet list per composer, selection cleared when no longer offered, counts requested only on picker open, chip tap navigates to Browse with one facet.
- iOS simulator walk-through against the live backend: Browse → Dikshitar shows five facets, Tyagaraja one; vibhakti Tritiya shows 24; a Dikshitar kriti's Details shows vibhakti and region chips and a chip opens the filtered Browse.

All phases: `git diff --check`; `make agent-evals` only if `CLAUDE.md` or `.claude/` change (none planned).

## Implementation Checklist

- [x] Phase 0: base confirmed, counts re-measured, vibhakti writers confirmed.
- [x] Phase 1 (PR #45, 2026-10-27): DTOs, OpenAPI, facet registry, filter, `/v2/catalogue/facets`, reader chips, tests, docs.
- [x] Phase 2 (PR #47, 2026-11-01): mobile client, fixtures, Browse facets, reader chips.

## Progress Log

- **2026-10-24**: Intent accepted with decisions on all four open questions.
- **2026-10-25**: Spec and Plan written (both Draft) from the accepted Intent and TRACK-151's Phase 0-1 branch. Facet counts re-measured on the dev DB (see Spec); two Intent facts corrected (tune stored on 7 of 38 Nottusvara memberships; KONGU has one temple). No code changed; awaiting Seshadri's acceptance of the Spec.
- **2026-10-25**: Spec accepted by Seshadri; the four Spec questions resolved as written. Plan still Draft.
- **2026-10-27**: Phase 0 and Phase 1 merged in PR #45 (`7c068cd`): DTOs, OpenAPI, facet registry, the facet filter on the V2 krithi search, `/v2/catalogue/facets` and the reader chip data, with tests and docs. Facets are limited to reviewed, populated data.
- **2026-11-01**: Phase 2 (mobile Browse facets and reader chips) merged in PR #47 (`f275aff`) after the branch was merged with `main` to resolve conflicts with TRACK-151 Phase 2 (kshetra screens kept alongside the facets). All CI checks passed, including the Android debug APK and iOS simulator host build; Android host tests could not run locally (no Android SDK) and ran in CI. Track marked Completed. Open: Viloma Chapu, eduppu and swarasahitya facets wait on TRACK-148 and 149 data and will reuse the same facet mechanism.
