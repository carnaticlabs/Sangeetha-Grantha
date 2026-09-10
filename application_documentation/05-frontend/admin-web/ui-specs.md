| Metadata | Value |
|:---|:---|
| **Status** | Active |
| **Version** | 2.2.0 |
| **Last Updated** | 2026-09-10 |
| **Author** | Sangeetha Grantha Team |
| **Document Type** | Current guide |

# Curator Console guide

---

The Curator Console is the editorial workspace for Sangeetha Grantha. Use it to find compositions, compare source material, correct metadata and structure, and bring imports into the canonical catalogue. Public listening and reading journeys are documented separately in the [Rasika guide](../mobile/README.md).

## Start a session

Run the stack and open `http://localhost:5001`. The Login page exchanges an admin token and existing user email for a JWT. Account provisioning, token login, and role assignment are separate concerns; follow the [authentication reference](../../00-meta/quick-reference-auth.md).

The backend enforces the admin role. A visible screen or navigation link does not itself grant API access.

## Workspace map

| Area | Browser route | What you can do |
|:---|:---|:---|
| Dashboard | `/` | Inspect headline catalogue and review statistics |
| Compositions | `/krithis` | Browse/filter and choose Lexical, Hybrid, or Semantic search |
| Composition editor | `/krithis/new`, `/krithis/:id` | Edit metadata, structure, lyrics, notation, tags, and inspect audit history |
| Reference data | `/reference` | Manage reference entities and inspect raga hierarchy |
| Imports | `/imports` | Request source scraping and inspect imported records |
| Bulk import | `/bulk-import` | Upload manifests, monitor batches, control processing, export reports |
| Curator review | `/curator-review` | Review incoming content, section issues, and unresolved raga identities |
| Tags | `/tags` | Maintain the controlled tag catalogue |
| Sourcing overview | `/admin/sourcing` | Inspect source and extraction activity |
| Sources and processing | `/admin/sourcing/sources` | Manage sources and monitor extraction requests |
| Evidence and verification | `/admin/sourcing/evidence` | Inspect source contributions and structural voting |
| Quality | `/admin/sourcing/quality` | Inspect implemented quality summaries and distributions |

Source and extraction detail routes remain available beneath the sourcing workspace. The old `/bulk-import/review`, sourcing `/extractions`, and `/voting` list routes redirect to the consolidated screens. The source of truth for route names is [App.tsx](../../../modules/frontend/sangita-admin-web/src/App.tsx).

## Find a composition

Start with a known title or phrase and, where useful, constrain composer or raga. An empty query browses the catalogue. Lexical search uses the conventional paged list; hybrid and semantic search show ranked matches with overview/passage context. The language filter is offered for lexical search.

Hybrid is the initial mode in the current list. It needs a populated compatible vector index for vector retrieval; without an active profile it falls back to lexical matching. Semantic search without a profile returns no matches. Read [search behavior](../../03-api/search.md) before diagnosing an empty result as missing catalogue content.

## Edit with source context

The editor separates metadata, composition structure, lyric variants, notation, tags, and audit information. Choose the composition's musical form from evidence; `UNESTABLISHED` is valid when the classification is unknown.

- **Metadata:** maintain composer, ordered raga associations, tala, deity, temple, language, and editorial fields.
- **Structure:** preserve the source's section order and meaningful labels. A missing Anupallavi is not automatically an extraction error.
- **Lyrics:** select the intended language/script/source variant. Verify every saved section belongs to the intended composition and variant.
- **Notation:** keep swara/jathi rows separate from lyric text, with the intended section and tala context.
- **Audit:** use actor/action history to understand a mutation; use revision provenance when investigating the exact historical text.

Workflow states are Draft, In review, Published, and Archived. Public catalogue visibility depends on publication and contract version. State badges describe lifecycle; they do not establish a universal read-only rule for every editor operation.

The [domain model](../../01-requirements/domain-model.md) defines musical correctness. [Versioned canon](../../04-database/versioned-canon.md) explains accepted-change history.

## Import and review

1. Register or select the source and submit a source URL or a manifest.
2. Monitor extraction progress and inspect failures before retrying.
3. Review metadata candidates, lyric sections, variants, and source evidence.
4. Resolve uncertain reference identities, particularly ragas, through curator actions.
5. Approve/map or reject the imported record; inspect the resulting composition.
6. Verify publication visibility independently from extraction completion.

Batches expose jobs, tasks, events, and pause/resume/cancel/retry controls. Review and finalization are distinct from worker processing. Follow the [ingestion guide](../../01-requirements/features/bulk-import/02-implementation/technical-implementation-guide.md) for ownership and troubleshooting.

## Source evidence and quality

Sources and Processing consolidates source registration and extraction monitoring. Evidence and Verification consolidates evidence browsing and structural votes; detail screens preserve access to individual runs and decisions.

The quality workspace is implemented, but several sourcing coverage/audit endpoints still return placeholder structures. An empty coverage chart is not proof that a scan ran cleanly. Use [quality verification](../../07-quality/README.md) and the dedicated remediation/audit routes when investigating structural integrity.

Raga-resolution actions include attaching an alias, confirming a new identity, and disambiguating a queue item. Read the [raga identity guide](../../04-database/raga-identity.md) before interpreting similarity as equivalence.

## Implementation boundaries

User and role pages remain placeholders even though backend management endpoints exist. Settings is not a completed console preferences experience. OAuth/OTP, graph exploration, public read-only web, and media experiences are planned separately. Backend validation/transliteration operations are not a promise of automatic scholarly verification.

## Frontend implementation

[Source module](../../../modules/frontend/sangita-admin-web) uses React/TypeScript, Tailwind, TanStack Query, and Vite. Sourcing pages are lazy-loaded. API transport lives in [api/client.ts](../../../modules/frontend/sangita-admin-web/src/api/client.ts); request/response assumptions must match the [API contract](../../03-api/api-contract.md).

```bash
cd modules/frontend/sangita-admin-web
bun install --frozen-lockfile
bun run typecheck
bun run test:unit
bun run build
```

For browser journeys, use the existing [E2E guide](../../07-quality/qa/e2e-testing.md). Versions belong in [Current Versions](../../00-meta/current-versions.md).

---

[Section index](./README.md) · [Documentation home](./../../README.md) · [Feature status](./../../01-requirements/features/README.md)
