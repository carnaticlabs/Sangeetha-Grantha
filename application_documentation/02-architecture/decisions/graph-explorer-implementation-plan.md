# Graph Explorer Implementation Plan - PostgreSQL + Cytoscape.js

> **Status**: Draft | **Version**: 1.0 | **Last Updated**: 2025-01-27
> **Owners**: Backend Team, Frontend Team

**Related Documents**
- [Graph Database Evaluation](./graph-database-evaluation.md)
- [Neo4J Graph Evaluation](../neo4j-graph-evaluation.md)
- [Sangita_Schema_Overview](../../04-database/schema.md)
- [Architecture](../backend/architecture.md)

# Graph Explorer Implementation Plan
## PostgreSQL + Cytoscape.js Approach

## Executive Summary

This document provides a comprehensive implementation plan for building a **Music-Aware Graph Explorer** using **PostgreSQL** (as the single source of truth) and **Cytoscape.js** (for client-side visualization). This approach avoids the operational complexity of Neo4j while delivering the required graph visualization functionality.

**Key Design Decisions:**
- PostgreSQL recursive CTEs for graph traversal queries
- Client-side graph construction and visualization
- RESTful API endpoints following existing patterns
- Reuse existing DTOs and service patterns
- Minimal new infrastructure (no additional databases)

**Estimated Timeline:** 11-15 days

---

## Table of Contents

1. [Architecture Overview](#1-architecture-overview)
2. [Backend Implementation](#2-backend-implementation)
3. [Frontend Implementation](#3-frontend-implementation)
4. [Database Query Strategy](#4-database-query-strategy)
5. [Step-by-Step Implementation Guide](#5-step-by-step-implementation-guide)
6. [Testing Strategy](#6-testing-strategy)
7. [Documentation Requirements](#7-documentation-requirements)
8. [Acceptance Criteria](#8-acceptance-criteria)

---

## 1. Architecture Overview

### 1.1 System Architecture

```
┌─────────────────┐
│  Admin Web UI   │
│  (React + TS)   │
│                 │
│  GraphExplorer  │
│  + Cytoscape.js │
└────────┬────────┘
         │ HTTP/REST
         │
┌────────▼────────┐
│  Ktor Backend   │
│                 │
│  GraphService   │
│  + Routes       │
└────────┬────────┘
         │
┌────────▼────────┐
│   PostgreSQL    │
│  (Single Source)│
│                 │
│  Recursive CTEs │
│  + Joins        │
└─────────────────┘
```

### 1.2 Data Flow

1. **User Interaction:** User selects mode, searches, sets depth
2. **Frontend:** Calls Graph API endpoints
3. **Backend:** Executes PostgreSQL queries (recursive CTEs or multi-query)
4. **Response:** Returns nodes + edges as JSON
5. **Frontend:** Constructs graph, renders with Cytoscape.js
6. **User Interaction:** Clicks nodes → fetches neighborhood → updates graph

### 1.3 Graph Model

**Node Types:**
- `Krithi`, `Composer`, `Raga`, `Tala`, `Deity`, `Kshetram` (Temple), `Tag`

**Relationships:**
- `(Krithi)-[:COMPOSED_BY]->(Composer)`
- `(Krithi)-[:IN_RAGA]->(Raga)` (via `krithi_ragas`)
- `(Krithi)-[:IN_TALA]->(Tala)`
- `(Krithi)-[:ADDRESSES]->(Deity)`
- `(Krithi)-[:AT_KSHETRAM]->(Kshetram)` (via temple)
- `(Krithi)-[:HAS_TAG]->(Tag)` (via `krithi_tags`)
- `(Raga)-[:JANYA_OF]->(Raga)` (via `parent_raga_id`)
- `(Deity)-[:AT_KSHETRAM]->(Kshetram)` (via temple's `primary_deity_id`)

**Node Properties:**
- `id: UUID` (matches PostgreSQL)
- `label: String` (display name)
- `type: String` (entity type)
- `properties: Map<String, Any?>` (additional metadata)

---

## 2. Backend Implementation

### 2.1 Shared Domain DTOs

**File:** `modules/shared/domain/src/commonMain/kotlin/com/sangita/grantha/shared/domain/model/GraphDtos.kt`

```kotlin
package com.sangita.grantha.shared.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class GraphNodeDto(
    val id: String,
    val label: String,
    val type: String,
    val properties: Map<String, String> = emptyMap()
)

@Serializable
data class GraphEdgeDto(
    val id: String,
    val source: String,
    val target: String,
    val type: String,
    val properties: Map<String, String> = emptyMap()
)

@Serializable
data class GraphResponseDto(
    val nodes: List<GraphNodeDto>,
    val edges: List<GraphEdgeDto>
)

@Serializable
data class GraphSearchResultDto(
    val id: String,
    val label: String,
    val type: String
)

@Serializable
enum class GraphEntityMode {
    KRITHI,
    COMPOSER,
    RAGA,
    TALA,
    DEITY,
    KSHETRAM,
    TAG
}
```

### 2.2 Graph Service

**File:** `modules/backend/api/src/main/kotlin/com/sangita/grantha/backend/api/services/GraphService.kt`

**Responsibilities:**
- Execute PostgreSQL queries for graph traversal
- Transform database results to graph DTOs
- Handle search queries
- Validate inputs and handle errors

**Key Methods:**
```kotlin
class GraphService(private val dal: SangitaDal) {
    suspend fun getNeighborhood(
        mode: GraphEntityMode,
        id: Uuid,
        depth: Int
    ): GraphResponseDto
    
    suspend fun search(
        mode: GraphEntityMode,
        query: String
    ): List<GraphSearchResultDto>
    
    suspend fun getPresetGraph(
        mode: GraphEntityMode,
        query: String?,
        depth: Int
    ): GraphResponseDto
}
```

### 2.3 Graph Repository

**File:** `modules/backend/dal/src/main/kotlin/com/sangita/grantha/backend/dal/repositories/GraphRepository.kt`

**Responsibilities:**
- Execute complex PostgreSQL queries
- Use recursive CTEs for multi-depth traversal
- Handle relationship queries efficiently
- Return raw data for service layer transformation

**Query Strategy:**
- **Depth 1:** Direct joins (fast, simple)
- **Depth 2-3:** Recursive CTEs or application-level expansion
- **Search:** Use existing normalized indexes

### 2.4 API Routes

**File:** `modules/backend/api/src/main/kotlin/com/sangita/grantha/backend/api/routes/graphRoutes.kt`

**Endpoints:**
```kotlin
fun Route.graphRoutes(graphService: GraphService) {
    route("/v1/admin/graph") {
        // GET /v1/admin/graph/neighborhood?mode=Krithi&id={uuid}&depth=2
        get("/neighborhood") { ... }
        
        // GET /v1/admin/graph/search?mode=Raga&q=shankarabharanam
        get("/search") { ... }
        
        // GET /v1/admin/graph/preset?mode=Raga&q=shankarabharanam&depth=2
        get("/preset") { ... }
    }
}
```

**Authentication:** Uses existing `authenticate("admin-auth")` middleware

### 2.5 Route Registration

**Update:** `modules/backend/api/src/main/kotlin/com/sangita/grantha/backend/api/plugins/Routing.kt`

```kotlin
fun Application.configureRouting(
    // ... existing services
    graphService: GraphService,
) {
    // ... existing routes
    authenticate("admin-auth") {
        // ... existing routes
        graphRoutes(graphService)
    }
}
```

---

## 3. Frontend Implementation

### 3.1 API Client

**File:** `modules/frontend/sangita-admin-web/src/api/graphApi.ts`

```typescript
import { request } from './client';

export interface GraphNode {
    id: string;
    label: string;
    type: string;
    properties: Record<string, string>;
}

export interface GraphEdge {
    id: string;
    source: string;
    target: string;
    type: string;
    properties: Record<string, string>;
}

export interface GraphResponse {
    nodes: GraphNode[];
    edges: GraphEdge[];
}

export interface GraphSearchResult {
    id: string;
    label: string;
    type: string;
}

export type GraphEntityMode = 
    | 'KRITHI' 
    | 'COMPOSER' 
    | 'RAGA' 
    | 'TALA' 
    | 'DEITY' 
    | 'KSHETRAM' 
    | 'TAG';

export const graphApi = {
    search: (mode: GraphEntityMode, q: string): Promise<GraphSearchResult[]> => {
        const params = new URLSearchParams({ mode, q });
        return request<GraphSearchResult[]>(`/admin/graph/search?${params}`);
    },
    
    neighborhood: (
        mode: GraphEntityMode, 
        id: string, 
        depth: number
    ): Promise<GraphResponse> => {
        const params = new URLSearchParams({ 
            mode, 
            id, 
            depth: depth.toString() 
        });
        return request<GraphResponse>(`/admin/graph/neighborhood?${params}`);
    },
    
    preset: (
        mode: GraphEntityMode, 
        q: string | null, 
        depth: number
    ): Promise<GraphResponse> => {
        const params = new URLSearchParams({ 
            mode, 
            depth: depth.toString() 
        });
        if (q) params.append('q', q);
        return request<GraphResponse>(`/admin/graph/preset?${params}`);
    },
};
```

### 3.2 Graph Explorer Page

**File:** `modules/frontend/sangita-admin-web/src/pages/GraphExplorer.tsx`

**Layout:**
```
┌─────────────────────────────────────────────────┐
│  [Mode ▼] [Search...] [Depth: 1─●─3] [Load]    │
├──────────────────────────┬──────────────────────┤
│                          │                      │
│   Cytoscape Canvas       │   Details Panel      │
│   (80% width)            │   (20% width)        │
│                          │                      │
│   [Graph Visualization] │   [Node Properties]  │
│                          │   [Quick Actions]    │
│                          │                      │
└──────────────────────────┴──────────────────────┘
```

**Key Features:**
- Mode dropdown (Krithi, Composer, Raga, etc.)
- Search input with typeahead
- Depth slider (1-3)
- Load button
- Cytoscape.js canvas
- Right-side details panel
- Reset view button

### 3.3 Cytoscape.js Integration

**Dependencies:**
```json
{
  "cytoscape": "^3.27.0",
  "cytoscape-fcose": "^2.2.0"
}
```

**Component:** `modules/frontend/sangita-admin-web/src/components/graph/CytoscapeGraph.tsx`

**Node Shapes:**
- Krithi: `round-rectangle`
- Composer: `ellipse`
- Raga: `hexagon`
- Tala: `diamond`
- Deity: `octagon`
- Kshetram: `rectangle`
- Tag: `triangle`

**Layout:** Use `fcose` layout algorithm with reasonable defaults

**Styling:** Minimal, neutral colors using Tailwind CSS

### 3.4 Graph State Management

**File:** `modules/frontend/sangita-admin-web/src/pages/GraphExplorer.tsx`

**State:**
- Selected mode
- Search query
- Depth setting
- Current graph data (nodes + edges)
- Selected node
- Loading state
- Error state

**Interactions:**
- Search → fetch results → select → load neighborhood
- Click node → highlight → fetch neighborhood → update graph
- Depth change → reload current graph
- Reset → clear graph

### 3.5 Route Registration

**Update:** `modules/frontend/sangita-admin-web/src/App.tsx`

```typescript
import GraphExplorer from './pages/GraphExplorer';

// In Routes:
<Route path="/graph-explorer" element={<GraphExplorer />} />
```

**Update Sidebar:** Add navigation link to Graph Explorer

---

## 4. Database Query Strategy

### 4.1 Neighborhood Query (Depth 1)

**Simple Case:** Direct relationships from a single entity

**Example: Krithi → Related Entities**
```sql
-- Get Krithi and direct relationships
WITH krithi_node AS (
    SELECT id, title as label, 'KRITHI' as type
    FROM krithis
    WHERE id = $id
),
composer_edge AS (
    SELECT 
        k.id as source,
        c.id as target,
        'COMPOSED_BY' as rel_type
    FROM krithis k
    JOIN composers c ON k.composer_id = c.id
    WHERE k.id = $id
),
raga_edges AS (
    SELECT 
        kr.krithi_id as source,
        r.id as target,
        'IN_RAGA' as rel_type
    FROM krithi_ragas kr
    JOIN ragas r ON kr.raga_id = r.id
    WHERE kr.krithi_id = $id
),
-- ... other relationships
all_nodes AS (
    SELECT id, label, type FROM krithi_node
    UNION
    SELECT id, name as label, 'COMPOSER' FROM composers WHERE id IN (SELECT target FROM composer_edge)
    UNION
    SELECT id, name as label, 'RAGA' FROM ragas WHERE id IN (SELECT target FROM raga_edges)
    -- ... other node types
),
all_edges AS (
    SELECT source, target, rel_type FROM composer_edge
    UNION
    SELECT source, target, rel_type FROM raga_edges
    -- ... other edges
)
SELECT * FROM all_nodes, all_edges;
```

### 4.2 Neighborhood Query (Depth 2-3)

**Complex Case:** Multi-hop traversal using recursive CTEs

**Example: Raga → Krithis → Composers (Depth 2)**
```sql
WITH RECURSIVE graph_path AS (
    -- Base: Start node
    SELECT 
        r.id,
        r.name as label,
        'RAGA' as type,
        0 as depth,
        r.id::text as path
    FROM ragas r
    WHERE r.id = $id
    
    UNION ALL
    
    -- Depth 1: Raga → Krithis
    SELECT 
        k.id,
        k.title as label,
        'KRITHI' as type,
        1 as depth,
        gp.path || '->' || k.id::text
    FROM graph_path gp
    JOIN krithi_ragas kr ON kr.raga_id = gp.id
    JOIN krithis k ON k.id = kr.krithi_id
    WHERE gp.depth = 0
      AND gp.type = 'RAGA'
      AND k.workflow_state = 'published'  -- Filter published only if needed
    
    UNION ALL
    
    -- Depth 2: Krithis → Composers
    SELECT 
        c.id,
        c.name as label,
        'COMPOSER' as type,
        2 as depth,
        gp.path || '->' || c.id::text
    FROM graph_path gp
    JOIN krithis k ON k.id = gp.id
    JOIN composers c ON c.id = k.composer_id
    WHERE gp.depth = 1
      AND gp.type = 'KRITHI'
)
SELECT DISTINCT id, label, type, depth FROM graph_path WHERE depth <= $max_depth;
```

**Note:** This approach works but may be complex. Alternative: Use application-level expansion (fetch depth 1, then fetch neighbors of those nodes).

### 4.3 Search Query

**Strategy:** Use existing normalized indexes

**Example: Raga Search**
```sql
SELECT id, name as label, 'RAGA' as type
FROM ragas
WHERE name_normalized LIKE '%' || lower($query) || '%'
ORDER BY name
LIMIT 20;
```

**Example: Krithi Search**
```sql
SELECT id, title as label, 'KRITHI' as type
FROM krithis
WHERE title_normalized LIKE '%' || lower($query) || '%'
  AND workflow_state = 'published'  -- Or allow all for admin
ORDER BY title
LIMIT 20;
```

### 4.4 Preset Query

**Strategy:** Mode-specific curated queries

**Example: Raga Preset (Janya chain + Krithis)**
```sql
-- Get raga and its janya hierarchy
WITH RECURSIVE raga_hierarchy AS (
    SELECT id, name, parent_raga_id, 0 as level
    FROM ragas
    WHERE id = $id OR name_normalized LIKE '%' || lower($query) || '%'
    
    UNION ALL
    
    SELECT r.id, r.name, r.parent_raga_id, rh.level + 1
    FROM ragas r
    JOIN raga_hierarchy rh ON r.parent_raga_id = rh.id
    WHERE rh.level < 3
),
-- Get krithis in these ragas
krithi_connections AS (
    SELECT DISTINCT kr.krithi_id, kr.raga_id
    FROM krithi_ragas kr
    JOIN raga_hierarchy rh ON kr.raga_id = rh.id
)
-- Combine nodes and edges
SELECT ...;
```

### 4.5 Query Optimization

**Indexes (should already exist):**
- `krithis.id` (primary key)
- `krithis.title_normalized` (for search)
- `krithi_ragas.krithi_id`, `krithi_ragas.raga_id` (for joins)
- `ragas.name_normalized` (for search)
- `ragas.parent_raga_id` (for janya hierarchy)

**Performance Targets:**
- Depth 1: < 100ms
- Depth 2: < 300ms
- Depth 3: < 500ms
- Search: < 200ms

---

## 5. Step-by-Step Implementation Guide

### Phase 1: Backend Foundation (Days 1-3)

#### Day 1: DTOs and Service Structure
1. ✅ Create `GraphDtos.kt` in shared domain
2. ✅ Add `GraphEntityMode` enum
3. ✅ Create `GraphService.kt` skeleton
4. ✅ Create `GraphRepository.kt` skeleton
5. ✅ Add basic error handling

#### Day 2: Database Queries
1. ✅ Implement depth 1 neighborhood query
2. ✅ Test with sample data
3. ✅ Implement search query
4. ✅ Add query result transformation to DTOs

#### Day 3: API Routes
1. ✅ Create `graphRoutes.kt`
2. ✅ Implement `/neighborhood` endpoint
3. ✅ Implement `/search` endpoint
4. ✅ Add route registration
5. ✅ Test endpoints with curl/Postman

### Phase 2: Backend Advanced (Days 4-5)

#### Day 4: Multi-Depth Queries
1. ✅ Implement depth 2-3 queries (recursive CTE or app-level)
2. ✅ Add relationship whitelist per mode
3. ✅ Implement preset queries
4. ✅ Add query result deduplication

#### Day 5: Polish and Testing
1. ✅ Add input validation
2. ✅ Add error handling and logging
3. ✅ Write unit tests for service
4. ✅ Write integration tests for routes
5. ✅ Performance testing

### Phase 3: Frontend Foundation (Days 6-8)

#### Day 6: API Client and Types
1. ✅ Install Cytoscape.js and fcose
2. ✅ Create `graphApi.ts`
3. ✅ Add TypeScript types
4. ✅ Test API client with mock data

#### Day 7: Graph Component
1. ✅ Create `CytoscapeGraph.tsx` component
2. ✅ Set up Cytoscape.js instance
3. ✅ Configure node shapes and styles
4. ✅ Implement fcose layout
5. ✅ Add basic interaction (click, hover)

#### Day 8: Graph Explorer Page
1. ✅ Create `GraphExplorer.tsx` page
2. ✅ Add mode dropdown
3. ✅ Add search input
4. ✅ Add depth slider
5. ✅ Wire up API calls
6. ✅ Add route registration

### Phase 4: Frontend Advanced (Days 9-11)

#### Day 9: Interactions and Details Panel
1. ✅ Implement node click → fetch neighborhood
2. ✅ Implement search → select → load
3. ✅ Create details panel component
4. ✅ Add node property display
5. ✅ Add quick actions (link to entity editor)

#### Day 10: UI Polish
1. ✅ Add loading states
2. ✅ Add error handling and toasts
3. ✅ Add reset view button
4. ✅ Improve styling with Tailwind
5. ✅ Add filters panel (optional)

#### Day 11: Testing and Documentation
1. ✅ Test all user flows
2. ✅ Fix bugs
3. ✅ Add JSDoc comments
4. ✅ Update documentation
5. ✅ Code review

### Phase 5: Documentation (Day 12)

#### Day 12: Documentation
1. ✅ Create `docs/architecture/graph-explorer.md`
2. ✅ Add API documentation
3. ✅ Add user guide
4. ✅ Add developer notes
5. ✅ Update README if needed

---

## 6. Testing Strategy

### 6.1 Backend Tests

**Unit Tests:**
- `GraphService` methods with mocked DAL
- Query result transformation
- Input validation

**Integration Tests:**
- API endpoints with test database
- Query performance
- Error handling

**Test Data:**
- Use existing test fixtures
- Create specific graph test scenarios

### 6.2 Frontend Tests

**Component Tests:**
- Graph component rendering
- User interactions
- API client calls

**E2E Tests (Optional):**
- Full user flow: search → load → interact

### 6.3 Manual Testing Checklist

- [ ] Search for each entity type
- [ ] Load neighborhood for each type
- [ ] Test depth 1, 2, 3
- [ ] Click nodes to expand
- [ ] View details panel
- [ ] Test error cases (invalid ID, network error)
- [ ] Test with large graphs (100+ nodes)
- [ ] Test layout stability

---

## 7. Documentation Requirements

### 7.1 Architecture Documentation

**File:** `application_documentation/architecture/graph-explorer.md`

**Contents:**
- Architecture overview
- Query strategy
- API endpoints
- Frontend component structure
- Performance considerations

### 7.2 API Documentation

**Update:** `application_documentation/api/api-contract.md`

**Add:**
- Graph API endpoints
- Request/response formats
- Error codes
- Examples

### 7.3 User Guide

**File:** `application_documentation/user-guides/graph-explorer.md`

**Contents:**
- How to use the graph explorer
- Feature overview
- Tips and tricks
- Known limitations

### 7.4 Developer Notes

**File:** `application_documentation/dev/graph-explorer-dev.md`

**Contents:**
- Local setup
- Testing with sample data
- Debugging tips
- Common issues

---

## 8. Acceptance Criteria

### 8.1 Functional Requirements

- [ ] User can search for entities by type (Krithi, Composer, Raga, etc.)
- [ ] User can load neighborhood graph for any entity
- [ ] Graph displays nodes with correct shapes per entity type
- [ ] User can click nodes to expand neighborhood
- [ ] Depth slider controls traversal depth (1-3)
- [ ] Details panel shows selected node properties
- [ ] Layout is stable and readable using fcose
- [ ] Reset view button clears graph

### 8.2 Technical Requirements

- [ ] All queries are parameterized and safe
- [ ] API endpoints follow existing patterns
- [ ] Frontend follows existing component patterns
- [ ] Error handling is consistent
- [ ] Loading states are shown
- [ ] Performance targets are met

### 8.3 Documentation Requirements

- [ ] Architecture documentation exists
- [ ] API documentation is updated
- [ ] User guide exists
- [ ] Developer notes exist

### 8.4 Quality Requirements

- [ ] Code follows project style guide
- [ ] Tests pass
- [ ] No critical bugs
- [ ] Performance is acceptable
- [ ] UI is responsive and accessible

---

## 9. Implementation Details

### 9.1 Backend File Structure

```
modules/backend/
├── api/
│   ├── routes/
│   │   └── graphRoutes.kt          # NEW
│   ├── services/
│   │   └── GraphService.kt         # NEW
│   └── models/
│       └── GraphModels.kt          # NEW (if needed)
├── dal/
│   └── repositories/
│       └── GraphRepository.kt      # NEW
└── shared/
    └── domain/
        └── model/
            └── GraphDtos.kt         # NEW
```

### 9.2 Frontend File Structure

```
modules/frontend/sangita-admin-web/src/
├── api/
│   └── graphApi.ts                 # NEW
├── pages/
│   └── GraphExplorer.tsx          # NEW
├── components/
│   └── graph/
│       ├── CytoscapeGraph.tsx      # NEW
│       └── GraphDetailsPanel.tsx   # NEW
└── types.ts                        # UPDATE (add graph types)
```

### 9.3 Dependencies

**Backend:**
- No new dependencies (uses existing PostgreSQL driver)

**Frontend:**
```json
{
  "dependencies": {
    "cytoscape": "^3.27.0",
    "cytoscape-fcose": "^2.2.0"
  }
}
```

### 9.4 Configuration

**No new configuration needed** (uses existing database connection)

---

## 10. Risk Mitigation

### 10.1 Performance Risks

**Risk:** Recursive CTEs may be slow for depth 3
**Mitigation:**
- Use application-level expansion for depth 2-3
- Add query timeouts
- Cache frequently accessed neighborhoods

### 10.2 Complexity Risks

**Risk:** Complex queries may be hard to maintain
**Mitigation:**
- Keep queries well-documented
- Use helper functions for common patterns
- Consider materialized views for common queries (future)

### 10.3 Frontend Risks

**Risk:** Large graphs may cause performance issues
**Mitigation:**
- Limit maximum nodes displayed (e.g., 500)
- Implement pagination or chunking
- Use virtual scrolling if needed

---

## 11. Future Enhancements

### 11.1 Phase 2 Features

- Graph filters (show/hide relationship types)
- Export graph as image
- Save graph views
- Advanced layout constraints
- Graph analytics (centrality, clustering)

### 11.2 Performance Optimizations

- Materialized graph views
- Graph caching
- Incremental graph updates
- WebSocket for real-time updates

### 11.3 Advanced Features

- Path finding between nodes
- Similarity search
- Graph-based recommendations
- Timeline view (temporal relationships)

---

## 12. Success Metrics

### 12.1 Performance Metrics

- Depth 1 queries: < 100ms (p95)
- Depth 2 queries: < 300ms (p95)
- Depth 3 queries: < 500ms (p95)
- Search queries: < 200ms (p95)
- Graph rendering: < 1s for 100 nodes

### 12.2 Usage Metrics

- Feature adoption rate
- Average session duration
- Most used entity types
- Most common traversal patterns

### 12.3 Quality Metrics

- Error rate < 1%
- Test coverage > 80%
- Zero critical bugs
- User satisfaction score

---

## Appendix A: Example Queries

### A.1 Krithi Neighborhood (Depth 1)

```sql
-- Get Krithi with all direct relationships
SELECT 
    'node' as element_type,
    k.id::text as id,
    k.title as label,
    'KRITHI' as type,
    jsonb_build_object(
        'workflow_state', k.workflow_state,
        'musical_form', k.musical_form
    ) as properties
FROM krithis k
WHERE k.id = $id

UNION ALL

SELECT 
    'node' as element_type,
    c.id::text,
    c.name,
    'COMPOSER',
    jsonb_build_object('birth_year', c.birth_year)
FROM krithis k
JOIN composers c ON k.composer_id = c.id
WHERE k.id = $id

UNION ALL

SELECT 
    'edge' as element_type,
    k.id::text || '-COMPOSED_BY->' || c.id::text,
    k.id::text,
    c.id::text,
    'COMPOSED_BY',
    '{}'::jsonb
FROM krithis k
JOIN composers c ON k.composer_id = c.id
WHERE k.id = $id;
```

### A.2 Raga Janya Chain (Preset)

```sql
WITH RECURSIVE raga_tree AS (
    SELECT id, name, parent_raga_id, 0 as depth
    FROM ragas
    WHERE id = $id
    
    UNION ALL
    
    SELECT r.id, r.name, r.parent_raga_id, rt.depth + 1
    FROM ragas r
    JOIN raga_tree rt ON r.parent_raga_id = rt.id
    WHERE rt.depth < 5
)
SELECT * FROM raga_tree;
```

---

## Appendix B: Cytoscape.js Configuration

### B.1 Basic Setup

```typescript
import cytoscape from 'cytoscape';
import fcose from 'cytoscape-fcose';

cytoscape.use(fcose);

const cy = cytoscape({
    container: document.getElementById('cy'),
    elements: {
        nodes: nodes,
        edges: edges
    },
    style: [
        {
            selector: 'node',
            style: {
                'label': 'data(label)',
                'width': 60,
                'height': 60,
                'text-valign': 'center',
                'text-halign': 'center',
                'font-size': '12px',
                'background-color': '#e8e8e8',
                'border-width': 2,
                'border-color': '#888'
            }
        },
        {
            selector: 'node[type="KRITHI"]',
            style: {
                'shape': 'round-rectangle',
                'background-color': '#4a90e2'
            }
        },
        {
            selector: 'node[type="RAGA"]',
            style: {
                'shape': 'hexagon',
                'background-color': '#7b68ee'
            }
        },
        // ... other node types
        {
            selector: 'edge',
            style: {
                'width': 2,
                'line-color': '#999',
                'target-arrow-color': '#999',
                'target-arrow-shape': 'triangle',
                'curve-style': 'bezier',
                'label': 'data(type)',
                'font-size': '10px'
            }
        }
    ],
    layout: {
        name: 'fcose',
        quality: 'default',
        randomize: false,
        animate: true,
        animationDuration: 1000,
        fit: true,
        padding: 30
    }
});
```

---

**Document Status:** Ready for implementation

**Next Steps:**
1. Review and approve this plan
2. Create implementation tickets
3. Begin Phase 1 implementation
4. Regular progress reviews

