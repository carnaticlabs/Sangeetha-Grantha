| Metadata | Value |
|:---|:---|
| **Status** | Complete |
| **Version** | 1.0.0 |
| **Last Updated** | 2026-10-16 |
| **Author** | Sangeetha Grantha Team |
| **Document Type** | Implementation Summary |

# TRACK-150: Rasika Collections & Reader Cycle Context

## Purpose

Show the reviewed Dikshitar, Syama Sastri and Tyagaraja cycle data (TRACK-145/146/147) to Rasika users: which cycle a kriti belongs to, where it sits, and an ordered way to read each cycle.

## What shipped

- **API (V2 only).** `GET /v2/catalogue/collections`, `GET /v2/catalogue/collections/{slug}`, and an optional `cycleContext` on the V2 reader. See the [API contract](../03-api/api-contract.md#public-catalogue) and `openapi/sangita-grantha.openapi.yaml`. V1 is unchanged, and the V1 reader carries no new field.
- **Reviewed guard.** `krithi_cycle_memberships` has no review column, so `CatalogueRepository` joins a `krithi_tags` row with `source = 'reviewed_cycle'` for the same kriti and tag, and applies the published V2 predicate. Raw `discriminative_attributes` stay server side; only an allow-list is returned.
- **Order honesty.** `orderBasis` is derived from stored data: any `order_convention` attribute gives `PERFORMANCE_CONVENTION`, any axis value gives `SOURCE`, otherwise `UNSPECIFIED`. The mobile app numbers members only for `SOURCE`.
- **Mobile.** A Collections shelf on Home, a collections list, a collection detail screen, a "Part of" chip in the reader header and full cards (position, previous, next, vibhakti, occasion) in the Details tab. Previous and next replace the top reader destination so cycle navigation does not exhaust the eight-deep back stack.
- **Fixtures.** `CollectionFixtures` covers Navavarana (dhyana, nine Avaranas, mangalam), Navagraha (seven core, two disputed) and the Ghana-raga Pancharatnam.

## Not included

Kshetra pages, search over cycle and kshetra overview documents, lakshana facet chips, any public web client, and a curator-web cycle view. Tyagaraja kshetra, ritual and dramatic data and the deferred Syama Sastri mudras are not shown because they are not applied (TRACK-148, TRACK-149).

## Verification

See the track's progress log for the commands and results.
