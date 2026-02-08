| Metadata | Value |
|:---|:---|
| **Status** | Active |
| **Version** | 1.1.0 |
| **Last Updated** | 2026-02-08 |
| **Author** | Sangeetha Grantha Team |

# Feature: Graph Explorer


---


## 1. Executive Summary

The Graph Explorer provides a visual, interactive interface for administrators to explore and understand the rich network of relationships between musical entities in the Sangeetha Grantha database. This feature enables users to discover connections between Krithis, Composers, Ragas, Talas, Deities, Kshetras, and Tags through an intuitive graph visualization, making it easier to understand the interconnected nature of Carnatic music knowledge.

**Key Objectives:**
- Enable visual exploration of relationships between musical entities
- Support interactive neighborhood exploration (1-3 hops from any starting entity)
- Provide entity search and filtering capabilities
- Facilitate discovery of musical patterns and connections
- Administration-only feature (initially) for content curation and quality assurance

**Technology Stack:** PostgreSQL with Recursive CTEs (backend) + Cytoscape.js (frontend visualization)

---

## 2. Targeted User Personas

**Primary Users:** Administrators and content curators working with the Sangeetha Grantha database.

**User Stories:**
- As an **administrator**, I want to visualize how krithis are connected to composers, ragas, and other entities so that I can understand the structure of our music database at a glance.
- As a **content curator**, I want to explore the neighborhood around a specific raga (e.g., "Kalyani") to see all related krithis, janya ragas, and composers so that I can ensure data consistency and completeness.
- As an **administrator**, I want to search for a specific entity and see its immediate connections so that I can quickly verify relationships and identify missing links.
- As a **content reviewer**, I want to filter the graph by entity type (e.g., show only Composers and Krithis) so that I can focus on specific relationship patterns without visual clutter.
- As an **administrator**, I want to expand nodes interactively to explore deeper connections (2-3 hops) so that I can discover indirect relationships and musical patterns.

---

## 3. Functional Requirements

### 3.1 Visual Graph Representation (High Priority)

**Requirement:** Display musical entities as nodes and their relationships as edges in an interactive graph visualization.

**Details:**
- **Node Types:** Support visualization of `Krithi`, `Composer`, `Raga`, `Tala`, `Deity`, `Kshetram`, and `Tag` entities.
- **Node Identity:** Each node is identified by its UUID (matching PostgreSQL primary key).
- **Node Styling:** Different visual styles (colors, shapes, sizes) for different entity types to enable quick visual distinction.
- **Edge Types:** Display relationships such as:
  - `(Krithi)-[:COMPOSED_BY]->(Composer)`
  - `(Krithi)-[:IN_RAGA]->(Raga)`
  - `(Krithi)-[:IN_TALA]->(Tala)`
  - `(Raga)-[:JANYA_OF]->(Raga)`
  - And other relationships defined in the database schema.
- **Layout:** Automatic graph layout algorithms (force-directed, hierarchical, or circular) that position nodes for optimal readability.
- **Performance:** Support visualization of up to 1,000 nodes and 5,000 edges with smooth interaction (< 100ms response time for layout updates).

### 3.2 Interactive Node Expansion (High Priority)

**Requirement:** Allow users to start from a single entity and progressively expand the graph to explore connected entities.

**Details:**
- **Starting Point:** Users can search for and select any entity (by name, UUID, or type) as the initial node.
- **Neighborhood Exploration:** Support expansion to 1-3 hops (depth levels) from the starting entity.
  - **Depth 1:** Directly connected entities (e.g., a Krithi's Composer, Ragas, and Tala).
  - **Depth 2:** Entities connected to the depth-1 entities (e.g., other Krithis by the same Composer, or Janya ragas of a parent Raga).
  - **Depth 3:** Entities connected to depth-2 entities (e.g., Composers of related Krithis).
- **Progressive Loading:** Load and display connections on-demand as users expand nodes, rather than loading the entire graph upfront.
- **Visual Feedback:** Clear indication of expansion depth (e.g., color coding or labels) and loading states during data fetching.

### 3.3 Entity Search and Selection (High Priority)

**Requirement:** Provide search functionality to find and select entities as starting points for graph exploration.

**Details:**
- **Search Interface:** Text-based search input that supports:
  - Entity name search (fuzzy matching for typos and variations)
  - Entity type filtering (e.g., "Show only Ragas")
  - UUID lookup (for direct entity access)
- **Search Results:** Display matching entities with their type and key attributes (e.g., for a Raga: name, melakarta number, arohana/avarohana).
- **Selection:** Click on a search result to set it as the graph's starting node and load its immediate neighborhood.

### 3.4 Filtering by Entity Type (Medium Priority)

**Requirement:** Allow users to filter the visible graph to show only specific entity types and their relationships.

**Details:**
- **Type Filters:** Toggle buttons or checkboxes to show/hide:
  - Krithis
  - Composers
  - Ragas
  - Talas
  - Deities
  - Kshetras
  - Tags
- **Relationship Preservation:** When filtering, maintain visible edges between remaining entity types (e.g., if both Krithis and Composers are visible, show COMPOSED_BY edges).
- **Dynamic Updates:** Graph layout automatically adjusts when filters are applied/removed.

### 3.5 Graph Navigation and Interaction (Medium Priority)

**Requirement:** Provide intuitive controls for navigating and interacting with the graph visualization.

**Details:**
- **Pan and Zoom:** Support mouse/touch gestures for panning and zooming the graph view.
- **Node Selection:** Click on nodes to highlight them and display detailed information in a sidebar or tooltip.
- **Edge Inspection:** Hover or click on edges to see relationship details (e.g., relationship type, metadata).
- **Reset View:** Button to reset zoom/pan to fit all visible nodes.
- **Export:** Option to export the current graph view as an image (PNG/SVG) for documentation or sharing.

### 3.6 Performance and Scalability (High Priority)

**Requirement:** Ensure the graph explorer performs efficiently even with large datasets.

**Details:**
- **Query Performance:** Backend queries must complete within:
  - < 100ms for depth-1 neighborhood queries
  - < 500ms for depth-3 neighborhood queries
- **Client-Side Rendering:** Efficient rendering of up to 1,000 nodes without browser lag.
- **Lazy Loading:** Load additional nodes and edges only when needed (on expansion or scroll).
- **Caching:** Cache frequently accessed neighborhoods to reduce database queries.

---

## 4. Technical Approach

### 4.1 Architecture Overview

The implementation follows a "Client-Side Graph" model as documented in [ADR-005](../../02-architecture/decisions/ADR-005-graph-database-evaluation.md):

```text
┌─────────────────┐
│  Admin Web UI   │
│  (Cytoscape.js) │
└────────┬────────┘
         │ JSON Graph DTOs (Nodes/Edges)
┌────────▼────────┐
│  GraphService   │
│ (Ktor Backend)  │
└────────┬────────┘
         │ Recursive SQL
┌────────▼────────┐
│   PostgreSQL    │
└─────────────────┘
```

### 4.2 Backend Implementation

**Database Strategy:** Use PostgreSQL with Recursive Common Table Expressions (WITH RECURSIVE) to perform graph traversals. This approach was selected over Neo4j for operational simplicity, team familiarity, and cost efficiency (see [ADR-005](../../02-architecture/decisions/ADR-005-graph-database-evaluation.md) for full rationale).

**Query Strategy:**
1. **Base Case:** Select the starting entity (e.g., a specific Krithi by UUID).
2. **Recursive Step:** Join with relationship tables (`krithi_ragas`, `composers`, `krithi_talas`, etc.) to find connected entities.
3. **Termination:** Stop when `depth` reaches the requested limit (1-3).
4. **Result:** Return distinct nodes and edges as JSON DTOs to the frontend.

**API Endpoints:**
- `GET /v1/graph/entities/{entityId}/neighborhood?depth={1|2|3}` - Get neighborhood graph for a specific entity
- `GET /v1/graph/search?query={searchTerm}&type={entityType}` - Search for entities
- `GET /v1/graph/entities/{entityId}` - Get details for a specific entity

### 4.3 Frontend Implementation

**Visualization Library:** Use **Cytoscape.js** for graph visualization (as per ADR-005 decision).

**Key Components:**
- `GraphExplorer.tsx` - Main graph visualization component
- `GraphSearch.tsx` - Entity search interface
- `GraphFilters.tsx` - Entity type filter controls
- `GraphNodeDetails.tsx` - Sidebar for displaying selected node information

**State Management:**
- Track loaded nodes and edges in React state
- Manage expansion depth and filter state
- Handle loading states and error conditions

### 4.4 Data Model

**Graph DTOs:**
```typescript
interface GraphNode {
  id: string;        // UUID
  type: EntityType;  // 'Krithi' | 'Composer' | 'Raga' | 'Tala' | 'Deity' | 'Kshetram' | 'Tag'
  label: string;     // Display name
  metadata?: Record<string, any>; // Additional entity-specific data
}

interface GraphEdge {
  id: string;
  source: string;    // Source node UUID
  target: string;    // Target node UUID
  type: string;      // Relationship type (e.g., 'COMPOSED_BY', 'IN_RAGA')
  metadata?: Record<string, any>;
}

interface GraphResponse {
  nodes: GraphNode[];
  edges: GraphEdge[];
}
```

---

## 5. Success Metrics

- **Adoption Rate:** 80% of administrators use the graph explorer at least once per week within 1 month of launch.
- **Query Performance:** 95% of depth-1 queries complete in < 100ms, 95% of depth-3 queries in < 500ms.
- **User Satisfaction:** Average user rating of 4+ out of 5 for ease of use and visual clarity.
- **Discovery Value:** Users discover at least 2 new relationships per session that were not immediately obvious from list views.

---

## 6. Implementation Roadmap

- **Phase 1 (Weeks 1-2):** Backend API endpoints for entity search and neighborhood queries using Recursive CTEs.
- **Phase 2 (Weeks 3-4):** Frontend graph visualization with Cytoscape.js, basic node expansion (depth-1).
- **Phase 3 (Weeks 5-6):** Multi-depth expansion (depth-2, depth-3), entity type filtering, and search integration.
- **Phase 4 (Weeks 7-8):** Polish, performance optimization, and user testing.

---

## 7. References

- [ADR-005: Graph Database Strategy](../../02-architecture/decisions/ADR-005-graph-database-evaluation.md) - Technical architecture decision
- [Database Schema](../../04-database/schema.md) - Entity relationship definitions
- [Domain Model](../domain-model.md) - Entity type definitions and relationships