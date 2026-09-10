| Metadata | Value |
|:---|:---|
| **Status** | Active |
| **Version** | 1.0.0 |
| **Last Updated** | 2026-09-10 |
| **Author** | Sangeetha Grantha Team |
| **Document Type** | Evidence record |

# Documentation refresh — September 2026

---

This record closes the documentation work described in [TRACK-141](./../../conductor/tracks/TRACK-141-documentation-facelift.md). It states what changed, what was deliberately preserved, and which checks were run. It is a record of editorial work only: no application code, database content, dependency, or runtime behavior changed in this track.

## Goal

Make the documentation useful to a reader of the current product. A reader should be able to learn what works today, how to run it, how to curate content, and where to find the governing contract or operating procedure — without having to infer which of several dated reports still applies.

## Scope

The refresh covered 292 Markdown files across the documentation tree, the root entry points, the agent instruction files, and the module READMEs.

| Area | Files touched |
|:---|---:|
| `01-requirements` | 56 |
| `10-implementations` | 42 |
| `07-quality` | 38 |
| `02-architecture` | 33 |
| `archive` | 24 |
| `00-meta` | 17 |
| `08-operations` | 12 |
| `11-retrospective` | 11 |
| `09-ai` | 9 |
| `03-api`, `05-frontend` | 8 each |
| `04-database` | 7 |
| `06-backend` | 6 |
| `00-onboarding` | 4 |
| Root, `conductor`, `config`, `modules`, `tools` | 12 |

The net line change is a reduction — roughly 5,900 lines added against 16,500 removed. That reduction is condensation, not removal of subject matter: no documentation file was deleted in this track. Long narrative reports were rewritten into shorter guides that link to the executable source of truth (routes, DTOs, migrations, manifests, CI) instead of restating it in prose that drifts.

## New guides

Six feature areas were previously documented only inside dated implementation reports, which left no current-guide answer for a new reader. Each now has a guide:

- [Hybrid and semantic search](./../03-api/search.md)
- [Versioned canon](./../04-database/versioned-canon.md)
- [Raga identity](./../04-database/raga-identity.md)
- [Embeddings](./../09-ai/embeddings.md)
- [Complete document catalog](./document-catalog.md)
- [Extraction worker README](./../../tools/krithi-extract-enrich-worker/README.md)

The [retrospective section index](./../11-retrospective/README.md) was added so that section is reachable from navigation rather than by path guessing.

## Lifecycle labelling

The central problem this refresh addressed was that a dated report and a current guide looked alike. Every touched document now carries a `Document Type` in its metadata header — `Current guide`, `Navigation`, `Evidence record`, or `Historical` — and historical documents open with a note stating that their counts, commands, and observations belong to the original work and are not a claim about the present release.

This preserves the evidence. A test total or corpus count recorded in February 2026 remains readable as what was true in February 2026, and is no longer mistakable for a live release claim.

## Verification

Link integrity is enforced by `tools/check-doc-links.py`, which resolves every relative link against **git-tracked** files rather than the working tree. A link to a file that exists locally but was never added resolves on the author's machine and nowhere else; the checker treats that as broken, which is the intended behavior.

```bash
make check-docs
```

The check passes with zero broken relative links across the tree.

Documentation-only scope was confirmed by inspecting the change set for non-Markdown paths; there are none, so the backend, frontend, and integration suites are unaffected by this track and were not re-run as evidence for it.

## Maintaining this state

- A link is a promise that clicking it works. Do not link a file that does not exist yet — write it as inline code and convert it to a link in the change that creates the file.
- Keep version facts in [current versions](./current-versions.md) and link to it. Do not restate version numbers in other documents.
- When adding a document, give it a metadata header with a `Document Type`, and add it to its section index and the [document catalog](./document-catalog.md).
- When a report ages into history, change its `Document Type` and add the historical note rather than deleting it.

---

[Section index](./README.md) · [Documentation home](./../README.md) · [Writing standards](./standards.md) · [Document catalog](./document-catalog.md)
