# ADR-005: Graph Database Strategy (PostgreSQL vs Neo4j)

| Metadata | Value |
|:---|:---|
| **Status** | Accepted |
| **Version** | 1.0 |
| **Last Updated** | 2026-01-20 |
| **Author** | Engineering Team |

> **Deciders**: Platform Team, Backend Team

## Context

The "Music-Aware Graph Explorer" feature requires the ability to visualize and traverse relationships between musical entities (Krithis, Composers, Ragas, Talas, Deities, Kshetras, Tags). We needed to decide on the underlying data store and architecture to support:
-   Interactive graph visualization in the Admin UI.
-   Neighborhood exploration (1-3 hops).
-   Entity search.
-   Administration-only usage (initially).

The primary choice was between introducing a dedicated graph database (**Neo4j**) as a projection, or leveraging the existing relational database (**PostgreSQL**) with recursive queries.

## Decision

We have decided to use **PostgreSQL with Recursive CTEs** as the backend data source, coupled with **Cytoscape.js** for client-side visualization.

We formally **reject** the proposal to introduce Neo4j Community Edition at this stage.

## Rationale

### 1. Operational Simplicity (Single Source of Truth)
Using PostgreSQL avoids the operational burden of managing a second database system. There is no need for:
-   Synchronization pipelines (and handling race conditions/lag).
-   Separate backups and monitoring for Neo4j.
-   Additional infrastructure costs.

### 2. Team Familiarity
The team has deep expertise in PostgreSQL and SQL. Introducing Cypher (Neo4j's query language) would increase the cognitive load and onboarding time for new developers.

### 3. Performance Sufficiency
Our analysis indicates that PostgreSQL is sufficient for our current and projected scale:
-   **Data Dimensions**: < 100k Krithis, < 5k Ragas. This volume fits easily in memory.
-   **Query Complexity**: Admin queries are typically limited to "neighborhood" (depth 1-3). Recursive Common Table Expressions (CTEs) in PostgreSQL can handle these checks efficiently (< 100ms for depth 1, < 500ms for depth 3).
-   The "Graph" is built on demand for the specific view, rather than needing an always-on graph engine.

### 4. Cost Efficiency
Neo4j Enterprise is expensive, and Community Edition lacks critical features like clustering and hot backups. PostgreSQL is already provisioned and incurs no marginal cost.

## Architecture

The implementation follows a "Client-Side Graph" model:

```
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

### Graph Model

Nodes and Edges are derived directly from relative tables:

**Nodes:**
-   Types: `Krithi`, `Composer`, `Raga`, `Tala`, `Deity`, `Kshetram`, `Tag`.
-   Identity: `UUID` (Matches PostgreSQL Primary Key).

**Relationships:**
-   `(Krithi)-[:COMPOSED_BY]->(Composer)`
-   `(Krithi)-[:IN_RAGA]->(Raga)`
-   `(Krithi)-[:IN_TALA]->(Tala)`
-   `(Raga)-[:JANYA_OF]->(Raga)`
-   ...and others defined in the schema.

### Query Strategy

We will use **Recursive Common Table Expressions (WITH RECURSIVE)** to perform traversals.

*Example Depth-N Traversal Logic:*
1.  **Base Case**: Select the starting entity (e.g., a specific Krithi).
2.  **Recursive Step**: Join with relationship tables (`krithi_ragas`, `composers`, etc.) to find connected entities.
3.  **Termination**: Stop when `depth` reaches the requested limit (e.g., 3).
4.  **Result**: Return distinct nodes and edges to the frontend.

## Consequences

### Positive
-   **Zero Infrastructure Drift**: The "graph" is always consistent with the system of record.
-   **Simplified Deployment**: No new containers or services to deploy.
-   **ACID Compliance**: Graph updates are atomic with data updates.

### Negative
-   **Complex SQL**: Recursive SQL can be verbose and harder to debug than Cypher.
-   **Traversal Limits**: Deep traversals (e.g., "shortest path between two arbitrary nodes") are inefficient in SQL compared to native graph DBs. This is acceptable as such queries are not in the v1 requirements.

## References

-   [Feature Requirements](../../01-requirements/features/graph-explorer.md)
-   [Domain Model](../../01-requirements/domain-model.md)
-   [Database Schema](../../04-database/schema.md)
