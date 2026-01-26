| Metadata | Value |
|:---|:---|
| **Status** | Active |
| **Version** | 1.0.0 |
| **Last Updated** | 2026-01-26 |
| **Author** | Sangeetha Grantha Team |

# Bulk Import Feature - Master Index


---

## Overview

This directory contains comprehensive analysis, strategy, and implementation guidance for bulk importing Krithis from multiple web sources and CSV files into Sangeetha Grantha.

**Goal**: Import 1,200+ Krithis from CSV files and web sources with proper entity resolution, de-duplication, and human review workflow.

---

## Document Organization

### 📋 Strategy Documents (`01-strategy/`)

1. **[Master Analysis](./01-strategy/master-analysis.md)**
   - Comprehensive analysis of bulk import capability
   - Data source evaluation
   - Architecture options (Custom Kotlin vs Koog vs Airflow)
   - Entity resolution strategies
   - De-duplication approaches
   - **Status**: Ready for review

2. **[CSV Import Strategy](./01-strategy/csv-import-strategy.md)**
   - Phase 1: CSV manifest ingestion
   - Phase 2: Batch scraping and enrichment
   - Phase 3: Entity resolution
   - Phase 4: Review workflow
   - **Status**: Ready for implementation

3. **[Koog Evaluation](./01-strategy/koog-evaluation.md)**
   - Koog framework analysis for import pipeline
   - Integration options and recommendations
   - Cost-benefit analysis
   - **Status**: Evaluation complete, decision pending

### 🔧 Implementation Documents (`02-implementation/`)

1. **[Technical Implementation Guide](./02-implementation/technical-implementation-guide.md)**
   - Service layer architecture
   - Code examples (Kotlin)
   - Database schema enhancements
   - API endpoint design
   - Testing strategy
   - **Status**: Ready for development

### 🌐 Source Analysis (`03-sources/`)

1. **[Web Source Detailed Analysis](./03-sources/web-source-analysis.md)**
   - Karnatik.com structure and extraction
   - Blogspot sources (Guru Guha, Syama Krishna, Thyagaraja)
   - TempleNet integration
   - Source priority matrix
   - **Status**: Complete

---

## Feature Implementation Roadmap

### ✅ Phase 1: CSV Manifest Ingestion (Completed & Revamped)
**Priority**: HIGH | **Status**: **Complete** (See TRACK-005)

**Objective**: Parse CSV files and load into `imported_krithis` staging table

**Implementation Details**:
- **Mechanism**: Runtime API Upload (`POST /v1/admin/bulk-import/upload`)
- **Parsing**: Robust `Apache Commons CSV` parsing with header validation
- **Validation**: Pre-flight URL syntax checks
- **Architecture**: Part of the Unified Dispatcher pipeline

**Key Documents**:
- [CSV Import Strategy - Phase 1](./01-strategy/csv-import-strategy.md#41-phase-1-csv-parsing--validation-week-1)
- [Technical Implementation](./02-implementation/technical-implementation-guide.md)

---

### 🔄 Phase 2: Web Scraping & Enrichment (In Progress)
**Priority**: HIGH | **Status**: **In Progress** (Backend Complete, Optimization Complete)

**Objective**: Scrape URLs from CSV and extract full metadata

**Status**:
- **Backend**: Scrape workers implemented and integrated with Dispatcher (TRACK-007).
- **Optimization**: Adaptive polling and batch claiming implemented (TRACK-006).
- **UI**: Stage visualization added (TRACK-005).

**Key Documents**:
- [Web Source Analysis](./03-sources/web-source-analysis.md)

---

### 🎯 Phase 3: Entity Resolution (In Progress)
**Priority**: HIGH | **Status**: **In Progress**

**Objective**: Map raw text to canonical entities

**Status**:
- **Backend**: Resolution workers implemented (Levenshtein based).
- **Architecture**: Integrated into Unified Dispatcher pipeline (TRACK-007).

---

### 👥 Phase 4: Review Workflow (In Progress)
**Priority**: MEDIUM | **Status**: **In Progress**

**Objective**: Admin interface for reviewing and approving imports

**Status**:
- **UI**: Review queue and diff viewer implemented (TRACK-004).
- **Backend**: Approval/Rejection APIs active.

---

## Key Decisions Made

### Architecture Decision
✅ **Custom Kotlin Pipeline** (not Koog for entire pipeline)
- Leverages existing stack (Kotlin, Ktor, Exposed)
- Full control over workflow
- Team familiarity
- **Reference**: [Master Analysis - Architecture Options](./01-strategy/master-analysis.md#3-architectural-options-analysis)

### Koog Decision
⏸️ **Deferred** - Evaluate after Phase 1
- Use Koog selectively for AI-intensive stages if needed
- Not required for initial implementation
- **Reference**: [Koog Evaluation](./01-strategy/koog-evaluation.md)

### Source Priority
1. **Karnatik.com** (highest quality, structured)
2. **TempleNet** (for temple associations)
3. **Blogspot sources** (CSV files - Guru Guha, Syama Krishna, Thyagaraja)
- **Reference**: [Web Source Analysis](./03-sources/web-source-analysis.md#71-source-priority-matrix)

---

## Quick Reference

### For Strategy & Planning
→ Start with [Master Analysis](./01-strategy/master-analysis.md)

### For CSV Import Implementation
→ Start with [CSV Import Strategy](./01-strategy/csv-import-strategy.md)

### For Technical Development
→ Start with [Technical Implementation Guide](./02-implementation/technical-implementation-guide.md)

### For Source-Specific Details
→ See [Web Source Analysis](./03-sources/web-source-analysis.md)

### For Framework Evaluation
→ See [Koog Evaluation](./01-strategy/koog-evaluation.md)

---

## Next Steps

1. **Immediate**: Review [CSV Import Strategy - Phase 1](./01-strategy/csv-import-strategy.md#41-phase-1-csv-parsing--validation-week-1)
2. **This Week**: Begin Phase 1 implementation (CSV manifest ingestion)
3. **Next Week**: Plan Phase 2 (web scraping)

---

## Related Documents

- [Conductor Track](../../../../conductor/tracks/TRACK-001-bulk-import-krithis.md)
- [Database Schema](../../../04-database/schema.md)
- [Import Pipeline Migration](../../../../database/migrations/04__import-pipeline.sql)
- [API Contract](../../../03-api/api-contract.md)

---

## Archive

Original analysis files are preserved in `archive/` for reference:
- Original Gemini, Goose, and Claude analysis documents
- Individual Koog evaluation documents
- Source-specific analysis files
