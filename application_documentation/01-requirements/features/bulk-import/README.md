| Metadata | Value |
|:---|:---|
| **Status** | Active |
| **Version** | 1.2.0 |
| **Last Updated** | 2026-09-10 |
| **Author** | Sangeetha Grantha Team |
| **Document Type** | Navigation |

# Import compositions from source material

---

Bulk import is the curator workflow for turning a source manifest into reviewable composition records. The current pipeline combines Kotlin orchestration, a Python extraction worker, canonical payloads, reference resolution, and editorial acceptance.

## Choose your starting point

| Task | Guide |
|:---|:---|
| Understand the current pipeline and retry/reingest flow | [Technical guide](./02-implementation/technical-implementation-guide.md) |
| Operate the console | [Curator Console](../../../05-frontend/admin-web/ui-specs.md) |
| Configure sources, quality, and evidence expectations | [Sourcing quality strategy](../../krithi-data-sourcing/quality-strategy.md) |
| Investigate identity ambiguity | [Raga identity](../../../04-database/raga-identity.md) |
| Verify accepted content | [Post-import checks](../../../07-quality/qa/test-plan.md) |
| Run extraction locally | [Worker README](../../../../tools/krithi-extract-enrich-worker/README.md) |

## What a successful import means

A completed extraction is only one step. Verify that the correct composition was matched/created, lyric variants and section joins persisted, ordered raga membership survived, provenance/audit were recorded, and publication state is intentional. Search indexing is separate from import persistence.

The console supports manifests, batch/job/task/event inspection, processing controls, review, finalization, and export. Sources and Processing and Evidence and Verification consolidate the newer sourcing workflows. Legacy payload compatibility remains pending convergence under TRACK-096.

## Design and implementation history

The following preserve earlier decisions and investigations. The technical guide above is the current operating reference.

- [CSV strategy](./01-strategy/csv-import-strategy.md), [master analysis](./01-strategy/master-analysis.md), [Koog evaluation](./01-strategy/koog-evaluation.md)
- [Source analysis](./03-sources/web-source-analysis.md), [original UI plan](./ui-ux-plan.md)
- [Composer deduplication](./02-implementation/composer-deduplication-implementation.md), [temple mapping](./02-implementation/kshetra-temple-mapping-implementation.md)
- [Multilingual extraction](./02-implementation/multi-language-lyric-extraction-implementation.md), [parsing repair](./02-implementation/text-blocker-parsing-implementation.md), [scraping refactor](./02-implementation/scraping-robustness-refactor.md)
- [Archived investigations](./archive)

[Feature map](../README.md) · [Sourcing index](../../krithi-data-sourcing/README.md) · [API](../../../03-api/api-contract.md)

---

[Section index](./../README.md) · [Documentation home](./../../../README.md) · [Feature status](./../README.md)
