| Metadata | Value |
|:---|:---|
| **Status** | Active |
| **Version** | 1.0.0 |
| **Last Updated** | 2026-01-24 |
| **Author** | Sangeetha Grantha Team |

# Graph Explorer Requirement (Original Neo4j Proposal)

| Metadata | Value |
|:---|:---|
| **Status** | Draft |
| **Version** | 1.0 |
| **Last Updated** | 2026-01-20 |
| **Author** | Engineering Team |

---
title: Graph Explorer Requirement (Original Neo4j Proposal)
status: Superseded
version: 1.0
last_updated: 2025-01-27
note: This document outlines the detailed requirements for the Graph Explorer feature in Sangeetha Grantha. This feature will enable users to visualize and navigate the relationships between various entities in Carnatic music. This document is a refinement of the high-level requirements in the [Graph Explorer Feature Requirements](../../01-requirements/features/graph-explorer.md). for the approved implementation.
---


| Metadata | Value |
|:---|:---|
| **Status** | Draft |
| **Version** | 0.1.0 |
| **Last Updated** | 2026-01-20 |
| **Author** | System |

---


**⚠️ STATUS: SUPERSEDED**

This document was the original requirement specifying Neo4j implementation. After critical evaluation (see [Graph Database Evaluation](./graph-database-evaluation.md)), the architecture team approved the **PostgreSQL + Cytoscape.js** approach instead.

**For current requirements and implementation strategy, see:**- [Graph Explorer Feature Requirements](../../01-requirements/features/graph-explorer.md)

---

## Original Requirement (Historical Reference)

Goal: implement a **Music-Aware Graph Explorer** (Admin Web) backed by **Neo4j Community** (graph projection). Postgres remains the system-of-record; Neo4j is a derived projection for traversal + visualization.

## 0) Non-negotiables
- Do NOT remove or refactor existing Postgres DAL; Neo4j is additive.
- Do NOT expose raw Cypher execution to the browser.
- Use **idempotent node identity**: every Neo4j node has `id` = Postgres UUID.
- Keep security: Admin Web calls Ktor backend only. Backend calls Neo4j.
- Implement clean TypeScript types, zod validation where appropriate, and consistent error handling.
- Provide minimal but production-minded code (timeouts, retries, logging).

## 1) High level deliverables
A) Backend (Ktor) additions:
1. Add Neo4j driver wiring and config
2. Add "graph projection query" endpoints for explorer
3. Add a basic read-only "neighborhood" and "query by mode" API

B) Frontend (admin-web) additions:
1. New route `/graph-explorer`
2. A Cytoscape.js-based graph canvas (NOT Bloom)
3. Entity-shaped nodes (different shapes per entity label)
4. Domain-aware controls:
   - Explorer mode dropdown: `Krithi | Composer | Raga | Deity | Kshetram | Tag`
   - Search input (typeahead optional, but at least a search)
   - Depth slider (1..3)
   - Filters panel (optional v1: show/hide labels, relationship types)
5. A right-side details panel showing selected node properties and related links

C) Docs:
- Add/update docs: `application_documentation/02-architecture/graph-explorer.md`
- Add local dev notes: `application_documentation/08-operations/runbooks/graph-explorer-dev.md` (if missing)

## 2) Neo4j graph model (assume these labels/edges exist)
Node labels:
- `Krithi`, `Composer`, `Raga`, `Tala`, `Deity`, `Kshetram`, `Tag`

Relationships:
- (Krithi)-[:COMPOSED_BY]->(Composer)
- (Krithi)-[:IN_RAGA]->(Raga)
- (Krithi)-[:IN_TALA]->(Tala)
- (Krithi)-[:ADDRESSES]->(Deity)
- (Deity)-[:AT_KSHETRAM]->(Kshetram)
- (Krithi)-[:HAS_TAG]->(Tag)
- (Raga)-[:JANYA_OF]->(Raga)
- (Krithi)-[:VARIANT_OF]->(Krithi)

All nodes have: `id: UUID`, `name/title` (as applicable), and `workflow_state` (optional).

## 3) Backend implementation details (Ktor)
### 3.1 Configuration
- Add env vars (document them):
  - `NEO4J_URI` (e.g. bolt://localhost:7687)
  - `NEO4J_USER`
  - `NEO4J_PASSWORD`
  - `NEO4J_DATABASE` (optional; default neo4j)

### 3.2 Neo4j client module
Create a small module (keep idiomatic Kotlin):
- `backend/api/src/main/kotlin/.../graph/Neo4jClient.kt`
- Manage driver lifecycle (create on startup, close on shutdown)
- Provide `readSession` helper with timeout + logging
- Add basic retry for transient errors (bounded; 2 retries)

### 3.3 DTOs (backend)
Define DTOs for graph visualization:
- `GraphNodeDto { id: String, label: String, type: String, properties: Map<String, Any?> }`
- `GraphEdgeDto { id: String, source: String, target: String, type: String, properties: Map<String, Any?> }`
- `GraphResponseDto { nodes: List<GraphNodeDto>, edges: List<GraphEdgeDto> }`

### 3.4 API endpoints
Add under `/api/admin/graph` (auth same as admin APIs):
1) `GET /api/admin/graph/neighborhood`
Query params:
- `mode` = one of entity labels (Krithi/Composer/Raga/Deity/Kshetram/Tag)
- `id` = UUID
- `depth` = int (1..3)
Return: `GraphResponseDto`

2) `GET /api/admin/graph/search`
Query params:
- `mode`
- `q` (string)
Return: list of `{id,label,type}` for quick pick

3) `GET /api/admin/graph/preset`
Query params:
- `mode`
- `q` (optional)
- `depth`
This returns a curated query graph (e.g., for Raga mode, include JANYA_OF chain + kritis)

### 3.5 Cypher queries (safe + parameterized)
- Use parameterized Cypher only.
- Use a whitelist mapping `mode -> label` and allowed relationship types per mode.
- Neighborhood query: start from (n:Label {id:$id}) then expand up to depth with allowed relationships:
  - Use `apoc.path.subgraphAll` ONLY if APOC available; otherwise implement with variable-length pattern expansion carefully with relationship whitelist. Prefer not requiring APOC initially.
- Ensure result includes unique nodes + edges (dedupe by id).

## 4) Frontend implementation details (React + TS)
Assume admin-web is React+TS+Tailwind.

### 4.1 Route + page
Add:
- `frontend/admin-web/src/routes/GraphExplorerPage.tsx`
- Route entry in router.

Layout:
- Top bar: Mode dropdown + Search box + Depth slider + "Load"
- Main area: Cytoscape canvas (80%)
- Right panel: Node details + quick actions (open in entity editor page if exists)

### 4.2 Cytoscape.js setup
Use Cytoscape.js with a clean, minimal style.
- Use different shapes per entity label:
  - Krithi: round-rectangle
  - Composer: ellipse
  - Raga: hexagon
  - Tala: diamond
  - Deity: octagon
  - Kshetram: rectangle
  - Tag: triangle
- Edges: arrows, label = relationship type
- Use `fcose` layout (like the demo: https://ivis-at-bilkent.github.io/cytoscape.js-fcose/demo/demo-constraint.html)
  - Use fcose layout with reasonable defaults
  - Add optional constraint logic:
    - If mode=Raga, keep Raga nodes in a horizontal band, and Krithi nodes below.
    - If mode=Composer, keep Composer nodes centered, and related Krithis in orbit.

Notes:
- If constraint API is too heavy for v1, implement only fcose layout but keep code structured to add constraints later.

### 4.3 Data mapping
Backend returns nodes/edges; map to Cytoscape elements:
- node data: `{ id, label, type, ...properties }`
- edge data: `{ id, source, target, label: type }`

### 4.4 Interaction
- Click node: highlight neighborhood; populate right-side panel
- Search: calls `/search`, picks entity, then loads `/neighborhood`
- Depth slider triggers reload
- Provide "Reset view" button

### 4.5 Styling + DX
- No visual clutter, minimal neutral colors; do not hardcode fancy palettes.
- Use Tailwind for layout; no heavy UI framework unless already present.
- Include loading states and error toasts.

## 5) Repo integration / File structure expectations
Keep code aligned with existing patterns:
- Backend: match existing Ktor routing modules and service classes.
- Frontend: match existing routing + API client conventions.
- Add new API client functions in frontend:
  - `graphApi.search(mode,q)`
  - `graphApi.neighborhood(mode,id,depth)`
  - `graphApi.preset(mode,q,depth)`

## 6) Acceptance criteria
- I can run Neo4j locally via Docker.
- I can open `/graph-explorer` and:
  - Search for a node
  - Load neighborhood graph
  - See nodes with different shapes per entity type
  - Click nodes and inspect details
  - Layout is stable and readable using fcose
- Backend queries are parameterized and safe.
- Documentation exists and explains how to run it.

## 7) Implementation steps (do them in order)
1. Add backend Neo4j client + config
2. Add graph endpoints + DTOs + basic Cypher for neighborhood/search
3. Add frontend page + cytoscape + fcose layout + details panel
4. Add docs
5. Add small seed/dev helper notes (how to test with sample IDs)

Now implement all changes with working code.
