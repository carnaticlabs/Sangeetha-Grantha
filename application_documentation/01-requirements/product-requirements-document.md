| Metadata | Value |
|:---|:---|
| **Status** | Active |
| **Version** | 1.3.0 |
| **Last Updated** | 2026-09-10 |
| **Author** | Sangeetha Grantha Team |
| **Document Type** | Current guide |

# Sangeetha Grantha — product requirements

---

Sangeetha Grantha is a catalogue of Carnatic compositions built for discovery, careful reading, and accountable curation. It connects composition metadata, source-specific lyric variants, musical structure, notation, and provenance so that readers can find a work and curators can explain the evidence behind it.

This PRD defines product intent and the current delivery boundary. The [feature map](./features/README.md) summarizes implemented behavior; [Conductor](../../conductor/tracks.md) records execution and acceptance. A product requirement is not evidence that a feature has passed its release gate.

## 1. Problem and opportunity

Composition information is spread across static websites, composer archives, PDFs, and scanned books. Names vary by spelling and script; section boundaries are inconsistent; a source's variant can be mistaken for a correction to another source. Readers struggle to find an unfamiliar work, and curators need to preserve disagreements without silently inventing a single authoritative text.

The platform should make the catalogue easier to discover while preserving that scholarly context. Structured data, source attribution, and human review are essential product behavior, not merely import tooling.

## 2. Product surfaces and users

| Surface | Users | Primary job |
|:---|:---|:---|
| **Rasika** — Android and iOS | Listeners, students, performers, teachers | Find a composition, understand its context, read a chosen lyric variant, save a bookmark |
| **Curator Console** — admin web | Editors, musicologists, ingestion operators | Establish metadata and structure, review imports, resolve identities, maintain source evidence |
| **Backend and ingestion platform** | Client developers and operators | Provide stable public reads, controlled editorial writes, repeatable extraction, and traceable changes |

Editor, reviewer, and ingestion operator are workflow personas. The implemented admin API currently uses the stored `grp_sangita_admin` role; separate fine-grained persona permissions are future work.

## 3. Goals and scope

### Goals

- Provide a searchable, normalized catalogue while preserving language, script, and source distinctions.
- Support established musical forms and explicitly unknown classification.
- Preserve ordered raga membership, including Ragamalika and justified section associations.
- Represent lyric variants and notation independently.
- Give curators useful import controls, source evidence, structural checks, and identity-resolution workflows.
- Record accepted content history and source provenance alongside audit events.
- Make public reading reliable, accessible, and clear about incomplete content.

### Outside the current release

Audio playback, animated notation/tala, public editing, annotations, monetization, public account sync, downloadable offline catalogue packs, and store distribution are not part of the current working Rasika release. A public read-only web application, Graph Explorer, conversational search, and production cloud rollout require separate delivery decisions.

## 4. Musical and editorial correctness

`KRITHI`, `VARNAM`, and `SWARAJATHI` are established classifications. `UNESTABLISHED` is the explicit state when classification has not been established; it must not be replaced with a guess. Classification informs validation and presentation, but source evidence determines the actual composition structure.

Keep Pallavi, Anupallavi, Charanam, Samashti Charanam, Madhyama Kala, Chittaswaram, and other source-supported sections ordered and labeled. Do not manufacture a missing section to satisfy a generic template. The full [domain model and lakshana rules](./domain-model.md) remain the musical correctness contract.

Language, script, transliteration scheme, pathantaram/sampradaya, and source reference are separate properties. A transliteration is not a new language or proof of a different source tradition. Swara/jathi notation belongs in notation structures, with sahitya alignment where appropriate.

Raga identity uses canonical names, aliases, mela context, and controlled resolution. Similar spelling is insufficient evidence for a merge. Read the [raga identity guide](../04-database/raga-identity.md).

## 5. Rasika requirements

| Journey | Current requirement and behavior | Release boundary |
|:---|:---|:---|
| Home | Search remains available independently of discovery; display available feature/collection content | Broader editorial features are later-release work |
| Explore | Submit a query across compositions, ragas, composers; apply exact composer/raga filters; preserve paging state | Other metadata directories are not mounted in V2 yet |
| Entity detail | Explain available raga/composer metadata and open exact-ID related works | Missing reference fields remain absent rather than fabricated |
| Read | Select a stored lyric variant; retain source and script identity; preserve labels, ordering, and justified section-raga links | No playback or mobile notation renderer |
| Library | Save/remove device-local bookmarks and restore them after restart | No cross-device sync, recents, or collections yet |
| Settings | Persist System/Light/Dark appearance and respect reduced-motion behavior | Public user accounts are unnecessary for these local preferences |

Public reads use allowlisted catalogue DTOs. Rasika consumes `/v2/catalogue`, including published unclassified compositions. Catalogue V1 retains its older form boundary and excludes `UNESTABLISHED`. Both keep editorial fields out of reader payloads.

See the [mobile PRD](./mobile/prd.md) and [Rasika experience](../05-frontend/mobile/rasika-discovery-experience.md). Native builds and shared tests exist; native journey execution and TalkBack/VoiceOver proof remain outstanding acceptance work.

## 6. Curator Console requirements

The console must support catalogue editing, reference management, import review, and source verification as a connected workflow.

- **Composition editor:** metadata, structure, lyric variants, notation, tags, and audit context.
- **Search:** conventional lexical browsing plus hybrid/semantic retrieval; show matched overview/passage context and make index failures distinguishable from no matches.
- **Reference management:** maintain composers, ragas, talas, deities, temples, and tags; retain aliases and raga lineage.
- **Bulk import:** upload manifests, inspect batches/jobs/tasks/events, pause/resume/retry/cancel, review results, finalize, and export reports.
- **Curator review:** inspect section issues and resolve uncertain raga identities using evidence.
- **Sourcing:** manage sources/extractions, inspect contributing evidence and structural votes, and present implemented quality summaries.

Workflow states are Draft → In review → Published → Archived. Extraction completion, import mapping/approval, and public publication are separate conditions. Product policy requires deliberate publication; users must verify actual workflow state after ingestion.

Some sourcing coverage/audit endpoints return placeholder payloads. User/role web pages are also placeholders despite backend management routes. Those surfaces must not be described as complete. The [admin PRD](./admin-web/prd.md) gives the detailed acceptance scope.

## 7. Ingestion, search, and provenance

### Ingestion

Kotlin owns requests, matching, review, and canonical persistence. Python owns supported source parsing and optional enrichment. They exchange validated canonical payloads through a database queue. Local file extraction requires a path visible to the executing worker. Retrying a request must be evaluated with idempotency and source evidence in mind.

The new-feature baseline includes multilingual/source-aware parsing, ordered Ragamalika persistence, controlled raga resolution, targeted reingestion, and structural diagnostics. Legacy payload compatibility still exists; TRACK-096 tracks convergence. See [ingestion architecture](./features/bulk-import/02-implementation/technical-implementation-guide.md).

### Search

Hybrid and semantic search operate over separately indexed composition overviews and lyric passages in PostgreSQL/pgvector. Profiles record model and dimensions; query vectors must match the active profile. With no active profile, hybrid falls back to lexical retrieval and semantic returns empty results. A mismatched active profile produces an availability failure.

Retrieval scores are relevance signals, not scholarly validation. Rasika currently uses catalogue queries rather than these vector-search endpoints. See [search](../03-api/search.md).

### Versioned canon

Accepted content changes can be represented as append-only composition and section revisions, linked to actors, source documents, and extraction runs. Current tables remain the read projection. Audit events record who/what/when; revisions retain the historical content. Transaction-time history is implemented; full bitemporal semantics and public history browsing are not assumed.

Corpus corrections belong in parser → extraction → import/reingest → curator workflows. Flyway manages schema/reference evolution, not one-off composition repairs. See [versioned canon](../04-database/versioned-canon.md).

## 8. API and data requirements

Use the [API contract](../03-api/api-contract.md) for mounted endpoints and the [schema guide](../04-database/schema.md) for storage. Product-facing “composition” is represented by the existing `krithis` API/table naming; `/v1/compositions` is not the current route family.

Public clients must receive only their allowed read model. Editorial writes require server-side authorization, validation, controlled transactions, and audit logging. New public enum values and capabilities must preserve older client compatibility through the versioned contract.

## 9. Quality and acceptance

These are targets and acceptance obligations, not claims of measured deployment performance:

| Area | Acceptance evidence |
|:---|:---|
| Discovery and reading | Known-query, no-result, paging, incomplete-content, unavailable-content, and variant-switch journeys |
| Musical fidelity | Source comparison for sections, language/script, raga order, and attribution |
| Editorial integrity | Import/edit acceptance produces intended current state, provenance, and audit records |
| Visibility | Anonymous/admin boundaries and V1/V2 compatibility tests |
| Performance | Measure search/API latency under representative corpus/load; earlier targets were search p95 <300 ms and API p95 <500 ms |
| Accessibility | Large text, reduced motion, native journey tests, manual TalkBack/VoiceOver review |
| Reliability | Deterministic backend/DAL/worker/frontend/mobile checks and rehearsed migrations |
| Operations | Explicit environment configuration, diagnostic signals, restore proof, deployment-specific release record |

Success should be evaluated through catalogue completeness, successful discovery, source coverage, review throughput, and verified user journeys. Historical corpus counts and test totals must retain their dates. See [Quality](../07-quality/README.md).

## 10. Technology stack

| Layer | Current implementation |
|:---|:---|
| Mobile | Kotlin Multiplatform + Compose Multiplatform; Android and iOS hosts |
| Backend | Kotlin + Ktor + Exposed; shared serializable DTOs |
| Database | PostgreSQL with pgvector, structured canon/provenance, reference aliases |
| **Migrations** | **Flyway Community**, via `make migrate` / `make db-reset`; versioned `V__` schema and repeatable `R__` reference seeds ([ADR-013](../02-architecture/decisions/ADR-013-db-migration-with-flyway.md)) |
| Admin web | React + TypeScript + Tailwind + Vite; TanStack Query; Bun tooling |
| Extraction | Python worker with canonical Pydantic payloads and optional Gemini enrichment |
| Semantic retrieval | Gemini embeddings, pgvector HNSW, lexical/vector rank fusion |
| Local orchestration | Makefile + Docker Compose; mise toolchain |
| CI | GitHub Actions for backend, database, frontend, worker, mobile, docs, and repository checks |
| Cloud deployment | Not established by this PRD; proposed cloud designs require environment-specific implementation and verification |

Dependency versions are maintained in [Current Versions](../00-meta/current-versions.md). Earlier Rust CLI and custom Python migration runners are archived; neither is part of the current migration workflow.

## 11. Delivery priorities

The working implementation includes Rasika's first discovery/reading release, curator/sourcing workflows, search, versioned canon, raga identity, and test infrastructure. Current release work focuses on native acceptance and remaining ingestion closure. Later work includes expanded discovery categories, collections/recents, interactive authentication, media experiences, and deployment.

Use [Conductor](../../conductor/tracks.md) for current node status. Do not infer release completion from a feature's code presence or an older “completed” report.

## 12. Related requirements

[Admin PRD](./admin-web/prd.md) · [Mobile PRD](./mobile/prd.md) · [Domain model](./domain-model.md) · [Feature map](./features/README.md) · [Documentation index](../README.md)

For document construction, indexing commands, profile activation, and coverage checks, read [Embedding pipeline and index operations](./../09-ai/embeddings.md).

---

[Section index](./README.md) · [Documentation home](./../README.md) · [Feature status](./features/README.md)
