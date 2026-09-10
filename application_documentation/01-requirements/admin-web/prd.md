| Metadata | Value |
|:---|:---|
| **Status** | Active |
| **Version** | 2.1.0 |
| **Last Updated** | 2026-09-10 |
| **Author** | Sangeetha Grantha Team |
| **Document Type** | Current guide |

# Curator Console — product requirements

---

The Curator Console gives editors and musicologists a structured way to build the canonical catalogue, review uncertain imports, and retain evidence behind their decisions. This PRD defines outcomes and acceptance scope; the [screen guide](../../05-frontend/admin-web/ui-specs.md) describes current navigation and use.

## 1. Users and desired outcomes

| Persona | Desired outcome |
|:---|:---|
| Editor | Correct composition metadata, sections, lyrics, and notation without losing source distinctions |
| Reviewer/musicologist | Compare evidence, resolve ambiguous identity/structure, and establish publication readiness |
| Ingestion operator | Run imports, identify failed or incomplete stages, and safely reprocess affected records |
| Administrator | Provision authorized access and maintain reference/configuration integrity |

These are workflow personas. Current backend authorization uses `grp_sangita_admin`; separate editor/reviewer/ingestion permission tiers are not implemented as the earlier PRD suggested.

## 2. Scope and implementation status

| Capability | Current scope | Boundary |
|:---|:---|:---|
| Dashboard | Catalogue statistics and editorial activity | Deployment data determines displayed counts |
| Composition search | Lexical browsing plus hybrid/semantic retrieval; composer/raga filters | Vector modes need index/profile configuration; language filter is lexical |
| Composition editor | Metadata, structure, lyrics, notation, tags, audit | Not every prospective validation/UI affordance is implemented |
| Reference data | Composers, ragas, talas, deities, temples; raga hierarchy and identity rules | Ambiguous raga names need controlled resolution |
| Imports | Source requests, imported records, review and reingest | Extraction success is not publication |
| Bulk import | CSV manifests, batch/task/event monitoring, controls, review/finalize/export | Large-scale campaigns require operational verification |
| Curator review | Review workflow, section issues, raga-resolution queue | Scholarly decisions remain human responsibilities |
| Sourcing | Sources/processing, evidence/verification, quality workspace | Coverage and audit-summary responses include placeholders |
| Users and roles | Backend management endpoints exist | Browser pages are placeholders |
| Settings and interactive authentication | Future work | No completed console settings or OAuth/OTP flow |

The [feature map](../features/README.md) is the cross-product status reference.

## 3. Catalogue editing requirements

### Composition and structure

Editors must be able to identify the composition, maintain its composer/tala/deity/temple/language fields, preserve ordered raga membership, and choose an evidence-supported musical form. `UNESTABLISHED` is valid when classification remains unknown.

Structure editing must preserve source labels, meaningful section ordering, Samashti Charanam/Madhyama Kala distinctions, and justified Ragamalika section associations. Do not force every source into a Pallavi–Anupallavi–Charanam template. The [domain model](../domain-model.md) supplies the musical correctness rules.

### Lyrics and notation

Maintain lyric variants by language, script, transliteration scheme, source, and optional tradition label. Save section text against the intended variant and composition. Preserve different source readings rather than silently merging them.

Notation remains independent of lyrics, with ordered rows, swara/jathi text, optional sahitya alignment, tala/kalai/eduppu context, and source labels. The admin notation editor does not imply a notation renderer in Rasika.

### Lifecycle and accountability

The lifecycle is Draft, In review, Published, and Archived. Public visibility is governed by publication state and the catalogue contract. Every editorial mutation must be authorized and audited; accepted content history should be captured through the [versioned canon](../../04-database/versioned-canon.md).

A badge is not an authorization rule. Server enforcement and route/service tests must determine permitted operations.

## 4. Search requirements

An editor should be able to locate a known title and also discover relevant passages. Lexical, Hybrid, and Semantic controls must make the selected mode clear. Ranked matches should identify the composition and the matched overview/passage context.

An empty query browses. Without an active embedding profile, hybrid uses lexical retrieval and semantic returns no matches. Provider/profile failures should be distinguishable from valid no-result responses. Ranking confidence is not proof that a raga, deity, or interpretation is correct. See [search behavior](../../03-api/search.md).

## 5. Ingestion and sourcing requirements

1. Register/select a source and submit a URL or CSV manifest.
2. Expose progress through batches, jobs, task attempts, events, and extraction details.
3. Provide pause/resume/cancel/retry controls with enough context to choose the right scope.
4. Review extracted metadata, source variants, sections, and reference candidates before accepting content.
5. Resolve uncertain raga identities through attach-alias, confirm-new, or disambiguation actions.
6. Retain source evidence, revision attribution, and audit entries when content is accepted or corrected.
7. Verify current catalogue state and publication visibility after the import or reingest.

The UI groups sources with processing and evidence with structural verification. Old review/extraction/voting list links redirect into those consolidated workspaces. The [curator guide](../../05-frontend/admin-web/ui-specs.md) owns the browser route map; the [ingestion guide](../features/bulk-import/02-implementation/technical-implementation-guide.md) owns pipeline responsibilities.

Quality summaries must state what is measured. Placeholder coverage/audit payloads are not successful diagnostic runs. Source tier, extraction confidence, and structural votes are inputs to review, not interchangeable measures of scholarly correctness.

## 6. Authentication and integration

Login currently submits an admin token and existing email to `POST /v1/auth/token`. The API also accepts a user ID. It returns a JWT whose roles come from storage; refresh reloads those assignments. Account provisioning stores an argon2id password hash, but the console does not use a password-login endpoint.

Use the [authentication reference](../../00-meta/quick-reference-auth.md). `/v1/admin/login` and earlier speculative `/v1/compositions` routes are not the mounted contract.

The [API guide](../../03-api/api-contract.md) and [UI-to-API mapping](../../03-api/ui-to-api-mapping.md) own endpoint details. Avoid duplicating a full route list here: that previously caused method, path, and pagination drift.

## 7. Technology and data

| Area | Current choice |
|:---|:---|
| Browser application | React, TypeScript, Tailwind, Vite, TanStack Query |
| Tooling | Bun; Vitest and Playwright |
| Backend | Kotlin, Ktor, Exposed |
| Data | PostgreSQL with pgvector and versioned canon |
| Migrations | **Flyway Community** through Make/Compose; `V__` schema and `R__` reference seeds |
| Extraction | Python worker; Kotlin orchestration/persistence |
| CI | GitHub Actions validation; production deployment remains separate |

Versions belong in [Current Versions](../../00-meta/current-versions.md). [ADR-013](../../02-architecture/decisions/ADR-013-db-migration-with-flyway.md) supersedes the earlier migration runners.

## 8. Acceptance and success measures

| Journey / concern | Evidence required |
|:---|:---|
| Edit and save | Correct entity/variant/section state after reload; mutation audit |
| Import review | Accepted/rejected state, canonical mapping, source evidence, intended publication state |
| Raga resolution | Correct identity/alias, queue transition, source attribution, collision handling |
| Search | Known title, thematic passage, no matches, missing profile, provider failure, anonymous/admin visibility |
| Sourcing | Run/task detail agrees with backend state; failed and incomplete cases remain actionable |
| UI integrity | Typecheck, component tests, build, and browser journeys across changed flows |

Measure catalogue completeness, review backlog, correction rate, and editorial throughput on a dated dataset. Earlier targets such as 2,000 curated compositions or 80% mapped imports are product goals, not current verified counts or guarantees.

[Main PRD](../product-requirements-document.md) · [Quality](../../07-quality/README.md) · [Current screen guide](../../05-frontend/admin-web/ui-specs.md)

---

[Section index](./README.md) · [Documentation home](./../../README.md) · [Feature status](./../features/README.md)
