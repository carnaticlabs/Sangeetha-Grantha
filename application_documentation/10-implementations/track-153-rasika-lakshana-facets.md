| Metadata | Value |
|:---|:---|
| **Status** | In progress (Phase 0-1 merged; Phase 2 mobile on review) |
| **Version** | 1.0.0 |
| **Last Updated** | 2026-10-27 |
| **Author** | Sangeetha Grantha Team |
| **Document Type** | Implementation Summary |

# TRACK-153: Rasika Lakshana Facets

## Purpose

Let a rasika move from "this kriti is in the Tritiya vibhakti" to every kriti with that property, using only reviewed data.

## What shipped

- **API (V2 only).** Five optional filters on `GET /v2/catalogue/krithis` (`vibhakti`, `collection`, `tune`, `mandalam`, `element`), `GET /v2/catalogue/facets` (values offered for a scope, counts only on `counts=true`) and `facetChips` on the V2 reader. See the [API contract](../03-api/api-contract.md#lakshana-facets).
- **Mobile data.** `CatalogueApi.searchKrithisFaceted` and `CatalogueApi.listFacets` are additive interface methods with safe defaults, so fakes that predate facets keep compiling: an empty selection delegates to `searchKrithis`, anything else reports facets unavailable. `LakshanaFacetSelection` carries at most one value per facet. The unrelated Explore labels are not touched.
- **Browse.** When a composer or raga is open (or Browse was opened from a reader chip), a "Filter by" row lists only the facets the server returns for that scope. Opening a facet fetches counts for its values on demand; nothing counts until then. Selected values appear as removable chips. On a scope change, a selected value the new scope does not offer is dropped before searching. A facet-list failure hides the row and never blocks the kriti list.
- **Reader.** The Details tab shows the reader's `facetChips` (vibhakti, tune, region). A kriti with none shows no row. Tapping a chip opens Browse with that single facet and the chip's label as the title.
- **Copy.** "Region" glosses mandalam. A note states that filters cover only compositions with reviewed data for them.
- **Fixtures.** Synthetic: Dikshitar offers all five facets, Tyagaraja only `collection`; the Vatapi sample holds a vibhakti and a region chip. They exercise the UI and are not a statement about the real composition.

## Not included

A facet-only entry point from Home or Explore, facet counts in the search response, facets for other composers' data that is not yet reviewed (see the data-readiness table in the API contract).

## Verification

- `./gradlew :modules:shared:domain:allTests :modules:shared:mobile-data:allTests :modules:shared:presentation:allTests` (JVM, Android host and iOS simulator targets).
- `./gradlew :modules:mobile:androidApp:assembleDebug`.
- iOS simulator walk-through against the live backend (iPhone 17 Pro, Phase 1 backend): Home → Abhayamba Vibhakti → a Dikshitar kriti → Details shows `Vibhakti: Prathama · nominative` and `Region: Chola` chips; tapping the vibhakti chip opens Browse with that facet and the matching kritis; the Vibhakti picker lists counts on demand (Tritiya 24). The walk-through caught two defects, fixed here: the facet-only list rendered the empty raga directory, and raw enum values (`PRATHAMA`, `CHOLA`) were shown as labels.
- Browse is reached only through a reader chip today; no navigation path opens Browse for a composer, so the per-composer facet counts (five for Dikshitar, one for Tyagaraja) are proven by presenter tests and the API, not by tapping through the app.
- Presenter tests: facet list per composer, stale selection dropped, counts only on picker open, chip opens Browse with one facet, clearing a facet.
- Client tests: facet parameters sent by wire key, `counts` only when asked.
