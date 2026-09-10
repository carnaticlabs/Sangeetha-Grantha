| Metadata | Value |
|:---|:---|
| **Status** | Active |
| **Version** | 1.2.0 |
| **Last Updated** | 2026-09-10 |
| **Author** | Sangeetha Grantha Team |
| **Document Type** | Navigation |

# Feature map

---

This is the current capability map for Sangeetha Grantha, reviewed against repository code on 2026-09-10. Use it to distinguish working behavior, operational prerequisites, and planned experiences. Track reports record past verification; they do not establish the state of a running deployment.

## Listener and student experience

| Capability | Implemented behavior | Boundary / next work |
|:---|:---|:---|
| Rasika shell | Home, Explore, Library, Settings; persistent appearance | Native-device acceptance remains open |
| Catalogue discovery | V2 discovery response; published selection and available feature content | Expanded editorial Home features remain later-release work |
| Search and browse | Explicit submitted query across compositions, ragas, composers; paging; UUID filters | Tala, deity, temple, language, and form directories are not mounted in V2 |
| Entity pages | Raga/composer detail and works lists; aliases and raga lineage where available | Displayed metadata depends on stored reference coverage |
| Lyric reader | One source variant at a time; script/source selection; stored section labels; ordered ragas | No generated transliteration, playback, or notation renderer in the Rasika reader |
| Library | Device-local bookmarks across restart | Account sync, collections, and recents remain planned |
| Accessibility | Large-text handling and reduced-motion behavior; native journey test targets | TalkBack, VoiceOver, and device runtime proof are still required |

Read the [mobile requirements](../mobile/README.md), [Rasika UI guide](../../05-frontend/mobile/README.md), and [TRACK-140 evidence](../../10-implementations/track-140-rasika-discovery.md).

## Editorial and sourcing experience

| Capability | Implemented behavior | Boundary / next work |
|:---|:---|:---|
| Catalogue editing | Metadata, structure, lyric variants, notation, tags, audit views | Musical form and section provenance require curator judgment |
| Search modes | Lexical, hybrid, and semantic selection in the admin list | Vector modes require a compatible active embedding profile and indexed documents |
| Bulk import | Manifest upload, batches, jobs/tasks/events, pause/resume/retry/cancel, review and finalization | Extraction success alone does not mean publication |
| Sourcing workspace | Sources/extractions, evidence/verification, quality summary screens | Coverage and audit-summary endpoints include placeholder responses |
| Curator review | Section issues and raga-resolution actions | Corpus corrections should be verified after reingestion |
| Reference data | Composer/raga/tala/deity/temple management; raga tree, aliases, identity rules | Ambiguous names are reviewed; similarity does not establish raga identity |
| Authentication | Admin-token exchange for JWT, stored role checks, JWT refresh; argon2id provisioning | Password provisioning is not an interactive password login; OAuth/OTP is deferred |

Read the [curator guide](../../05-frontend/admin-web/ui-specs.md), [bulk-import guide](./bulk-import/README.md), and [authentication reference](../../00-meta/quick-reference-auth.md).

## Platform capabilities

| Capability | Where to learn more |
|:---|:---|
| Public V1/V2 contracts, published visibility, strict query parsing | [API contract](../../03-api/api-contract.md) |
| Embedding profiles, overview/passage indexing, hybrid rank fusion | [Search guide](../../03-api/search.md) |
| Append-only revisions and per-section source provenance | [Versioned canon](../../04-database/versioned-canon.md) |
| Raga aliases, authority, match keys, resolution queue | [Raga identity](../../04-database/raga-identity.md) |
| Python extraction, canonical payloads, Kotlin persistence | [Ingestion architecture](./bulk-import/02-implementation/technical-implementation-guide.md) |
| Flyway schema and repeatable reference seeds | [Migrations](../../04-database/migrations.md) |
| Backend, frontend, worker, and mobile checks | [Quality guide](../../07-quality/README.md) |

## Plans and specialist documents

The documents below explain feature design or earlier implementation work. Check their lifecycle notice and the current guides before treating a proposal as shipped behavior.

- [Advanced notation and transliteration](./advanced-krithi-notation-transliteration.md)
- [Searchable deity and temple management](./searchable-deity-temple-management.md)
- [Content ingestion requirements](./intelligent-content-ingestion.md), [generic extraction history](./generic-scraping.md)
- [Database optimization](./database-layer-optimization.md), [development environment](./cross-platform-development-environment-standardisation.md)
- [Graph Explorer proposal](./graph-explorer.md), [global architecture proposal](./global-scale-architecture.md), [GCP strategy](./gcp-implementation-strategy.md)
- [Documentation integrity](./documentation-integrity-update.md), [commit workflow](./commit-guardrails-workflow.md)

The [product requirements](../product-requirements-document.md) describe intended outcomes. The [Conductor registry](../../../conductor/tracks.md) owns task status. The [documentation index](../../README.md) provides the complete reading map.

---

[Section index](./../README.md) · [Documentation home](./../../README.md)
