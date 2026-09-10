| Metadata | Value |
|:---|:---|
| **Status** | Active |
| **Version** | 2.1.0 |
| **Last Updated** | 2026-09-10 |
| **Author** | Sangeetha Grantha Team |
| **Document Type** | Navigation |

# Sangeetha Grantha documentation

---

Use this library to understand the product, run it locally, curate source material, or change the implementation. The [root README](../README.md) gives a product overview; this page organizes the deeper guides by reader task.

## Start with your task

| You want to… | Read |
|:---|:---|
| Understand what is built and what remains open | [Feature map](./01-requirements/features/README.md), [main PRD](./01-requirements/product-requirements-document.md) |
| Start the application | [Getting started](./00-onboarding/getting-started.md), [troubleshooting](./00-onboarding/troubleshooting.md) |
| Use or improve Rasika | [Mobile PRD](./01-requirements/mobile/prd.md), [mobile implementation](./05-frontend/mobile/README.md) |
| Curate compositions and imports | [Curator Console](./05-frontend/admin-web/ui-specs.md), [admin PRD](./01-requirements/admin-web/prd.md) |
| Understand semantic search | [Search modes and API](./03-api/search.md) |
| Build or refresh embeddings | [Embedding pipeline](./09-ai/embeddings.md) |
| Integrate a client | [API contract](./03-api/api-contract.md), [examples](./03-api/api-examples.md) |
| Trace extraction to accepted text | [Ingestion guide](./01-requirements/features/bulk-import/02-implementation/technical-implementation-guide.md), [versioned canon](./04-database/versioned-canon.md) |
| Resolve raga identity | [Raga identity](./04-database/raga-identity.md), [domain rules](./01-requirements/domain-model.md) |
| Verify a change or import | [Quality gates](./07-quality/README.md), [post-import checks](./07-quality/qa/test-plan.md) |
| Operate the system | [Configuration](./08-operations/config.md), [runbooks](./08-operations/runbooks/README.md), [deployment readiness](./08-operations/deployment.md) |
| Find any retained document | [Complete document catalog](./00-meta/document-catalog.md) |

## Browse by area

| Area | What it contains |
|:---|:---|
| [00 · Onboarding](./00-onboarding/README.md) | Setup, IDEs, local diagnostics |
| [00 · Documentation and versions](./00-meta/README.md) | Version reference, standards, lifecycle, audit history |
| [01 · Requirements](./01-requirements/README.md) | Product/client PRDs, musicological model, feature requirements |
| [02 · Architecture](./02-architecture/README.md) | Runtime responsibilities, diagrams, decisions, future scaling proposals |
| [03 · API](./03-api/README.md) | Mounted contracts, examples, client integration and search |
| [04 · Database](./04-database/README.md) | Schema, Flyway, raga identity, versioned canon and audit |
| [05 · Frontend](./05-frontend/README.md) | Curator Console and Rasika implementation/design |
| [06 · Backend](./06-backend/README.md) | Service conventions, mutation/security requirements and technical history |
| [07 · Quality](./07-quality/README.md) | Checks, integration tests, browser/native journeys and dated reports |
| [08 · Operations](./08-operations/README.md) | Configuration, monitoring, deployment boundaries and runbooks |
| [09 · AI](./09-ai/README.md) | Extraction/enrichment, embeddings, evaluation and agentic delivery references |
| [10 · Implementation evidence](./10-implementations/README.md) | Track-specific results, observed tests, known limitations |
| [11 · Retrospectives](./11-retrospective/README.md) | Source/import investigations and lessons |
| [Archive](./archive/README.md) | Superseded designs and retained historical material |

## Which document is authoritative?

- **Current guides and PRDs** explain present behavior, scope, and requirements. The [feature map](./01-requirements/features/README.md) calls out incomplete and planned surfaces.
- **Source code, DTOs, migrations, and manifests** establish implemented routes, storage, and dependency values. The [API guide](./03-api/api-contract.md) explicitly distinguishes planned OpenAPI entries from mounted endpoints.
- **ADRs** retain the rationale and lifecycle of architectural decisions. A superseded decision is historical, not an alternative operating instruction.
- **Conductor tracks** own work status and acceptance. [The registry](../conductor/tracks.md) is the starting point for work in flight.
- **Reports and retrospectives** record dated observations. Their test totals, corpus counts, source observations, and old commands are not live release claims.

The September refresh preserves historical evidence and links it back to the current guides. See the [refresh report](./00-meta/documentation-refresh-2026-09.md) for scope and verification.

## Maintain the library

Use descriptive titles, one primary heading, linked source references, explicit planned/implemented boundaries, and dated evidence. Centralize versions in [Current Versions](./00-meta/current-versions.md). Follow [standards](./00-meta/standards.md) and [retention](./00-meta/retention-plan.md), then run `make check-docs`.

---

[Section index](./../README.md) · [Feature status](./01-requirements/features/README.md)
