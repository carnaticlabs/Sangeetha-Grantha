# Graph Database Evaluation for Music-Aware Graph Explorer

> **Status**: Draft | **Version**: 0.1 | **Last Updated**: 2025-12-29
> **Owners**: Platform Team

**Related Documents**
- [Neo4J Graph Evaluation](../neo4j-graph-evaluation.md)
- [Domain Model](../../01-requirements/domain-model.md)
- [Sangita_Schema_Overview](../../04-database/schema.md)

# ADR-005: Graph Database Evaluation for Music-Aware Graph Explorer

## Context

The requirement in [`neo4j-graph-evaluation.md`](../neo4j-graph-evaluation.md) proposes a Neo4j
Community-backed graph projection to power an admin-only
Music-Aware Graph Explorer. Postgres remains the system of record, and
Neo4j is a derived read model used for traversal and visualization of
Krithi relationships (Composer, Raga, Tala, Deity, Kshetram, Tag, and
variants).

This document critically evaluates the requirement, outlining benefits,
risks, alternatives, and the gaps that should be addressed before
implementation.

## Scope and Assumptions

- Admin-only feature; no public end-user exposure in v1.
- Neo4j Community Edition; no clustering or enterprise features.
- Graph is a projection of Postgres; updates are derived, not canonical.
- Depth-limited traversal (1..3) is the primary interaction.
- No raw Cypher in the browser; backend-only queries.

## Evaluation Criteria

1. Query suitability: Does a graph model materially simplify or speed
   the required queries (1..3 hop traversal, neighborhood views)?
2. Operational cost: Additional infra, monitoring, backups, and
   on-call considerations.
3. Consistency and correctness: Staleness risk and debugging cost of a
   derived projection.
4. Security and safety: Query control, injection risk, and exposure.
5. Developer velocity: Net change in complexity across backend,
   frontend, and data pipelines.
6. Future expansion: Ability to support advanced graph features (path
   discovery, recommendations) without a rewrite.

## Pros

- Natural modeling of relationships already present in the domain.
  Traversals like Krithi -> Raga -> Janya are straightforward.
- Better UX for graph visualization: Neo4j returns nodes/edges in a
  graph shape without expensive join assembly on the backend.
- Reduced query complexity for multi-hop traversals compared to SQL
  joins, particularly when adding new relationship types.
- Supports future graph features (pathfinding, centrality, similar
  entities) without major schema changes.
- Keeps Postgres as the system of record while enabling specialized
  query patterns.

## Cons and Risks

- Data duplication and eventual consistency are unavoidable. The graph
  view can become stale or incorrect if sync pipelines fail.
- Operational overhead increases: running Neo4j, backups, monitoring,
  and incident handling for a new datastore.
- Neo4j Community lacks clustering and read replicas; availability and
  scaling options are limited.
- The requirement assumes APOC may not be available. Without APOC,
  path expansion and filtering logic becomes more complex and may be
  harder to optimize.
- Search requirements are underspecified. A useful search likely needs
  full-text indexes; otherwise, search may be slow or limited.
- Depth-limited traversal (1..3) may not justify a graph DB if Postgres
  can serve the same queries with indexed joins or recursive CTEs.
- Security risk remains if query construction is dynamic; requires
  strict whitelisting and type validation.

## Requirement Gaps and Clarifications

- Projection strategy: How is Neo4j kept in sync (batch job, CDC,
  outbox, or triggers)? What is the acceptable staleness window?
- Scale assumptions: Expected counts for Krithis, Tags, Ragas, and
  relationships. Graph density drives performance.
- Search behavior: Required matching rules (prefix, fuzzy, language
  variants) and index strategy.
- Error handling: What does the UI show if Neo4j is down or stale?
- Data governance: Which fields are projected, and which are excluded
  for privacy or security?
- Performance targets: p95 latency goals for depth 1..3 requests and
  refresh frequency.

## Alternatives

| Option | Pros | Cons | Fit for Requirements |
| --- | --- | --- | --- |
| Postgres joins + recursive CTE | No new infra, strong consistency | Complex SQL, slower for deep traversals | Likely sufficient for 1..2 hops |
| Materialized adjacency tables | Faster reads, simple API | Needs refresh pipeline, still SQL | Good for read-heavy admin UI |
| In-memory graph in backend | Fast traversal, no extra DB | Data size limits, restart costs | Viable for small datasets |
| Search engine (OpenSearch) | Good for text search and facets | Not ideal for graph traversal | Complementary, not a replacement |
| Neo4j projection | Best for graph traversal and visualization | New infra + sync complexity | Best if graph UX is core |

## Recommendation

Proceed only as a scoped pilot if graph exploration is a clear admin
priority. Limit v1 to a narrow set of relationships and measure
benefits against the operational and engineering cost. If the pilot
cannot meet performance and freshness targets, revert to a Postgres
read model.

## Success Metrics and Go/No-Go Criteria

- p95 response time: <= 500ms for depth 1..2, <= 1500ms for depth 3.
- Sync lag: <= 10 minutes between Postgres updates and graph projection.
- Error rate: < 1% on graph endpoints.
- Admin UX: Graph explorer used in at least 3 core workflows without
  falling back to SQL-based views.

## Open Questions

- What is the expected maximum node/edge count in the first year?
- How often do entities change and require graph updates?
- Do we need multilingual search or fuzzy matching in v1?
- Is the graph explorer only for admins, or a future public feature?

## References

- [Neo4j Graph Database Evaluation Proposal](../neo4j-graph-evaluation.md)
- [Domain Model](../requirements/domain-model.md)
- [Database Schema Overview](../database/SANGITA_SCHEMA_OVERVIEW.md)
