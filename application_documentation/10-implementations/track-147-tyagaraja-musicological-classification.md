| Metadata | Value |
|:---|:---|
| **Status** | Complete |
| **Version** | 1.0.0 |
| **Last Updated** | 2026-10-16 |
| **Author** | Sangeetha Grantha Team |
| **Document Type** | Implementation Summary |

# TRACK-147: Tyagaraja Corpus Concordance & Musicological Classification

## Purpose

Classify the reconciled Tyagaraja catalogue at source-row grain, approve the five Ghana-raga Pancharatnam memberships, and apply only those memberships to the catalogue. Edition consultation, collision splits, and deferred non-cycle claims stay with [TRACK-149](../../conductor/tracks/TRACK-149-tyagaraja-edition-consultation-collision-split.md).

The readable concordance is [tyagaraja-classification-master.md](../01-requirements/features/tyagaraja-classification-master.md). The shared evidence contract remains [musicological-classification-contract.md](../01-requirements/features/musicological-classification-contract.md).

## 1. Classification master

`tools/krithi-extract-enrich-worker/scripts/populate_tyagaraja_master.py` writes:

- `database/data/track147-catalogue-snapshot.json` — 675 identities and 684 imports, including each import's extraction checksum.
- `database/data/track147-tyagaraja-master.json` — 684 subjects, 4,104 claims, and 694 witnesses.
- `database/data/track147-tyagaraja-application-manifest.json` — 53 approved changes (`status: ready`).
- The generated concordance above.

Witness rules in `tyagaraja_classification.py`:

- A reviewed claim cites a `fetched` witness with its own SHA-256. The database snapshot is a census hash, not an approval witness.
- `NO_PLACE_STATED` cannot be reviewed. Approved non-kshetra address uses `NON_SPECIFIC_ADDRESS`.
- The eight collision identities stay `collision_not_classified`. Their 17 source rows are the classification subjects.

Approved and applied: the five Ghana-raga Pancharatnam compositions, with Aradhana order stored as concert convention. Fourteen further compositions have approved claims marked `application: deferred`. Print editions and the Journal of the Music Academy volumes named in the track stay `unavailable`.

## 2. Catalogue application

- `R__seed_07_canonical_cycle_tags.sql` no longer inserts Tyagaraja Pancharatnam tags from title patterns. The tag dictionary row and Syama Sastri safeguards remain.
- `R__seed_11_tyagaraja_reviewed_memberships.sql` writes the five approved memberships by exact UUID, upgrades their `krithi_tags` rows to `reviewed_cycle`, and writes `system:TRACK-147` audit rows only when data changes.
- `TyagarajaMembershipSeedTest` reruns both repeatable migrations in an isolated schema and checks idempotency and cross-composer protection.

After `make migrate` on the local database: five `krithi_cycle_memberships` rows for `tyagaraja-pancharatnam`, five `reviewed_cycle` tag links, ten audit events. Tyagaraja remains 675 identities. Dikshitar and Syama Sastri memberships are unchanged.

## 3. Search headers and retrieval probes

`context_formatter.py` and `macro_indexer.py` render the Tyagaraja Ghana-raga cycle only when the membership is human-approved and the composer is Tyagaraja. `evals/retrieval_benchmarks.json` adds probes `TY-147-01` through `TY-147-11`, including the separator between Dikshitar's `tyagaraja-vibhakti` and saint-composer Tyagaraja.

## Verification

- `uv run pytest` in `tools/krithi-extract-enrich-worker`: 492 passed.
- `uv run ruff check` and `uv run mypy` on the changed Python files: clean.
- `generate_tyagaraja_master.py --check`: concordance and manifest match the master.
- `./gradlew :modules:backend:dal:integrationTest --tests com.sangita.grantha.backend.dal.integration.TyagarajaMembershipSeedTest`: passed.
- `make check-docs` and `git diff --check`: clean.
