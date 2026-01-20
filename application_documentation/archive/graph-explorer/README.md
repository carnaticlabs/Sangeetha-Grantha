# Graph Explorer Archive

| Metadata | Value |
|:---|:---|
| **Status** | Draft |
| **Version** | 0.1.0 |
| **Last Updated** | 2026-01-20 |
| **Author** | System |

---


> **Status**: Archived | **Date Archived**: 2026-01-27
> **Reason**: Historical evaluation and implementation planning documents for graph explorer feature

## Overview

This directory contains archived documents related to the Graph Explorer feature evaluation and implementation planning. These documents were created during the evaluation phase when Neo4j was being considered as a graph database solution.

**Current Status**: The Graph Explorer feature was approved using **PostgreSQL + Cytoscape.js** approach instead of Neo4j. See the current requirements document: [`application_documentation/01-requirements/features/graph-explorer.md`](../../01-requirements/features/graph-explorer.md)

## Archived Documents

### Evaluation Documents

1. **`graph-database-evaluation.md`** (from `01-requirements/features/`)
   - Comprehensive evaluation comparing Neo4j vs PostgreSQL approach
   - Status: Completed evaluation
   - Outcome: Recommended PostgreSQL + Cytoscape.js

2. **`graph-database-evaluation-decisions-version.md`** (from `02-architecture/decisions/`)
   - Duplicate version with ADR-style front matter
   - Contains similar evaluation content

### Implementation Planning

3. **`graph-explorer-implementation-plan.md`** (from `01-requirements/features/`)
   - Detailed implementation plan for PostgreSQL + Cytoscape.js approach
   - Status: Draft planning document
   - Contains step-by-step implementation guide

4. **`graph-explorer-implementation-plan-decisions-version.md`** (from `02-architecture/decisions/`)
   - Duplicate version with ADR-style front matter
   - Contains similar implementation plan content

### Original Requirements

5. **`graph-explorer-requirement.md`** (from `01-requirements/features/`)
   - Original requirement document specifying Neo4j implementation
   - Status: Superseded
   - Note: This was the original requirement before evaluation led to PostgreSQL approach

## Current Active Documents

For current requirements and architecture decisions, see:

- **Feature Requirements**: [`application_documentation/01-requirements/features/graph-explorer.md`](../../01-requirements/features/graph-explorer.md)
- **Architecture Decision**: [`application_documentation/02-architecture/decisions/ADR-005-graph-database-evaluation.md`](../../02-architecture/decisions/ADR-005-graph-database-evaluation.md)

## Archive Rationale

These documents were archived to:
- Consolidate historical evaluation documents in one location
- Remove duplicate files from multiple directories
- Preserve historical context while maintaining clear documentation structure
- Follow ADR-001 principles for document organization

## Note on Duplicates

Some documents exist in multiple versions (from `01-requirements/features/` and `02-architecture/decisions/`). Both versions are preserved here for completeness. The versions from `01-requirements/features/` are considered the primary versions as they contain more complete context and references.

