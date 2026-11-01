| Metadata | Value |
|:---|:---|
| **Status** | Complete |
| **Version** | 1.0.0 |
| **Last Updated** | 2026-11-01 |
| **Author** | Sangeetha Grantha Team |
| **Document Type** | Implementation Summary |

# TRACK-154: Temple Google Place IDs

## Purpose

Give each temple a reviewed Google `place_id` so the kshetra detail can open Google Maps without the database storing Google-derived content.

## What shipped

- **Schema (V68).** `temples.google_place_id`, `place_review_state` (`CONFIRMED`, `LOCALITY_ONLY`, `UNRESOLVED`; NULL = not reviewed), `place_reviewed_by`, `place_reviewed_at`, with CHECKs and a partial unique index on confirmed place ids. See [schema.md](../04-database/schema.md).
- **Lookup tooling.** `tools/krithi-extract-enrich-worker/scripts/temple_place_lookup.py` with `export-targets`, `lookup`, `triage` and `build`. Only `lookup` uses the network; the key comes from `GOOGLE_PLACES_API_KEY`, the field mask requests no geometry, and a hard call ceiling applies. Candidate and review files hold Google names and addresses, so they stay in the gitignored `storage/` and are deleted after review.
- **Reviewed data and seed.** `database/data/track154-temple-place-review.json` (84 `CONFIRMED`, 1 `LOCALITY_ONLY`, approver and date on every entry, place ids only) and the generated `R__seed_12_temple_place_review.sql`. The seed matches by natural key, never writes coordinates, protects values set by another actor, and writes one audit row per changed temple.
- **V2 contract.** `CatalogueRepository.findTemple` fills the optional `place` on the temple detail: only for `CONFIRMED`, `TEMPLE` for a temple's own place and `PARENT_COMPLEX` for a sannidhi or mandapam inheriting a confirmed parent. `LOCALITY_ONLY`, `UNRESOLVED`, unreviewed rows and sub-shrines of unconfirmed parents return none. No review state, approver, note or coordinate is on the DTO. See the [API contract](../03-api/api-contract.md).
- **Catalogue correction (V69).** The Saraswati Temple row moved from Basrur, Karnataka to Basara, Telangana.

## Operating notes

- Rerunning the seed is a no-op. A generated seed literal must not contain `${`, which Flyway reads as a placeholder (regression-tested).
- The place id can go stale. Clients must tolerate a link that no longer resolves; the refresh cadence is not decided.
- The `SarAvatI taTa` cycle membership in `R__seed_09` still carries a sthala note naming Basrur and the Sharavati river. It was not changed here because it is a musicological attribution (see Open follow-up).

## Open follow-up

Review the `SarAvatI taTa` sthala note against the Basara identification, ideally with the `carnatic-musicologist` reviewer.

## Verification

Worker tests (35), `make test`, `make test-integration`, `make check-docs`, the seed on a copy of the dev data (85 applied, 0 absent, 0 protected, rerun a no-op), and the Tiruvarur check: the complex returns `TEMPLE` and its sannidhis `PARENT_COMPLEX` with the same place id, and the id opens the Tyagarajaswami temple in Google Maps.
