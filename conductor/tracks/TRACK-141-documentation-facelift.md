| Metadata | Value |
|:---|:---|
| **Status** | Completed |
| **Version** | 1.1.0 |
| **Last Updated** | 2026-09-10 |
| **Author** | Sangeetha Grantha Team |

# TRACK-141 — Documentation facelift and feature reconciliation

---

## Intent

**Status:** Accepted for documentation-only work.  
**Authorization:** User request in this task, 2026-09-10: start with the root README and update documentation across the application for new features, with a significant improvement in presentation and writing.

Make the documentation useful to readers of the current product: explain what works, how to run it, how to curate it, and how to find the relevant contract or operating procedure.

## Specification

**Status:** Accepted within the requested documentation scope.

- Preserve pre-existing working-tree edits and historical evidence.
- Reconcile active guidance with routes, DTOs, source modules, migrations, package manifests, and CI.
- Cover Rasika V2/discovery, admin/sourcing, hybrid/semantic search, versioned canon, raga identity, extraction, authentication, configuration, and verification.
- Distinguish implemented behavior, incomplete surfaces, planned releases, and historical reports.
- Improve navigation, prose, diagrams, headers, and relative links throughout the documentation tree and relevant module READMEs.

## Plan

**Status:** Accepted within the user's explicit documentation-update request; no application feature implementation is included.

1. Inventory the documentation and inspect the current source and work registry.
2. Rewrite reader-facing entry points and active product, API, architecture, operations, and quality guides.
3. Add missing feature guides; preserve historical reports with clear lifecycle context and navigation.
4. Verify documentation links and formatting; record scope and validation in the documentation refresh report.

## Validation and closure

Closed 2026-09-10. No code, database content, dependency, or runtime changes are part of this track; the change set is Markdown only.

**Delivered**

- 292 Markdown files revised across the documentation tree, root entry points, agent instruction files, and module READMEs.
- Six new guides for feature areas that previously existed only inside dated implementation reports: [search](./../../application_documentation/03-api/search.md), [versioned canon](./../../application_documentation/04-database/versioned-canon.md), [raga identity](./../../application_documentation/04-database/raga-identity.md), [embeddings](./../../application_documentation/09-ai/embeddings.md), the [document catalog](./../../application_documentation/00-meta/document-catalog.md), and the [extraction worker README](./../../tools/krithi-extract-enrich-worker/README.md).
- Lifecycle labelling (`Document Type`) applied so dated evidence is no longer mistakable for a current release claim.
- Scope and verification recorded in the [September refresh report](./../../application_documentation/00-meta/documentation-refresh-2026-09.md).

**Verification**

| Check | Command | Result |
|:---|:---|:---|
| Relative link integrity | `make check-docs` | Pass — every relative Markdown link resolves |
| Agent configuration | `make agent-evals` | Pass — `CLAUDE.md` and `.agents/AGENTS.md` were touched |

Backend, frontend, and integration suites were not re-run: the change set contains no non-Markdown paths, so they are unaffected by this track.

No documentation file was deleted. The net line reduction is condensation of prose that restated source detail into links to the executable source of truth.
