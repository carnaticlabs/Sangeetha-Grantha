| Metadata | Value |
|:---|:---|
| **Status** | Active |
| **Version** | 1.1.0 |
| **Last Updated** | 2026-09-10 |
| **Author** | Sangeetha Grantha Team |
| **Document Type** | Current guide |

# Delivery status and remaining work

---

Use [Conductor](../conductor/tracks.md) for task/node status and the [feature map](./01-requirements/features/README.md) for product boundaries. This page replaces the earlier undated backlog, which mixed implemented capabilities with future work.

## Implemented foundations

- Curator editing, imports/review, sourcing and reference workflows.
- Python extraction, canonical payload exchange, Kotlin matching/persistence.
- Flyway and shared Testcontainers infrastructure.
- Versioned canon and source attribution.
- Raga identity, aliases and curator resolution.
- Hybrid/semantic search and embedding indexing tools.
- Rasika public V1/V2 catalogue, shared/native hosts, and first discovery/reader experience.

## Work still open

| Area | Remaining boundary | Reference |
|:---|:---|:---|
| Rasika release acceptance | Native runtime journeys and manual TalkBack/VoiceOver proof | [TRACK-140](../conductor/tracks/TRACK-140-rasika-discovery-experience.md) |
| Later discovery | Additional metadata directories, collections/recents, broader editorial Home features | [Mobile PRD](./01-requirements/mobile/prd.md) |
| Payload convergence | Legacy payload compatibility remains | [TRACK-096](../conductor/tracks/TRACK-096-payload-format-convergence.md) |
| Corpus reingestion closure | Remaining verification/closure for retired data-fix migration work | [TRACK-139](../conductor/tracks/TRACK-139-retire-corpus-data-fix-migrations.md) |
| Interactive authentication | OAuth/OTP and finer persona roles | [TRACK-119](../conductor/tracks/TRACK-119-oauth-otp-auth.md) |
| Incomplete console surfaces | User/role UI placeholders and some sourcing quality responses | [Admin guide](./05-frontend/admin-web/ui-specs.md) |
| Production delivery | Environment-specific deployment, rollback/restore proof, store submission | [Deployment readiness](./08-operations/deployment.md) |

Conversational search, graph exploration, media and public web remain separate future work. Embedding/search code is already implemented; current [embedding operations](./09-ai/embeddings.md) describe coverage and profile prerequisites, not a feature awaiting its first implementation.

---

[Section index](./README.md) · [Documentation home](./README.md) · [Feature status](./01-requirements/features/README.md)
