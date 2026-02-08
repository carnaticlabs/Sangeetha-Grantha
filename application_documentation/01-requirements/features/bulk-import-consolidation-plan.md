| Metadata | Value |
|:---|:---|
| **Status** | Active |
| **Version** | 1.1.0 |
| **Last Updated** | 2026-02-08 |
| **Author** | Sangeetha Grantha Team |

# Bulk Import Documentation Consolidation Plan


---



---

## Current State Analysis

### Newly Added Files (12 files)

#### Category 1: Koog Framework Evaluation (3 files)
- `koog-integration-analysis.md` - General Koog integration options
- `koog-technical-integration-proposal.md` - Technical integration details
- `koog-evaluation-for-import-pipeline-goose.md` - Import pipeline specific evaluation

**Consolidation Strategy**: Merge into single comprehensive Koog evaluation document

#### Category 2: General Bulk Import Analysis (4 files)
- `krithi-bulk-import-capability-analysis-gemini.md` - Gemini's analysis
- `krithi-bulk-import-capability-analysis-goose.md` - Goose's comprehensive analysis
- `krithi-import-analysis.md` - General import analysis
- `krithi-import-orchestration-comprehensive-analysis-claude.md` - Claude's orchestration analysis

**Consolidation Strategy**: Create master analysis document referencing all perspectives

#### Category 3: CSV Import Strategy (2 files)
- `krithi-bulk-import-from-csv-strategy-goose.md` - Comprehensive CSV strategy
- `analysis-bulk-import-google.md` - CSV manifest ingestion (Phase 1 focus)

**Consolidation Strategy**: Merge into single CSV import strategy document

#### Category 4: Technical Implementation (1 file)
- `import-pipeline-technical-implementation-guide-goose.md` - Detailed implementation guide

**Consolidation Strategy**: Keep as standalone technical reference

#### Category 5: Source Analysis (1 file)
- `web-source-detailed-analysis-goose.md` - Detailed web source analysis

**Consolidation Strategy**: Keep as standalone reference

#### Category 6: Updates/Corrections (1 file)
- `update-summary.md` - Guru Guha source correction

**Consolidation Strategy**: Integrate corrections into relevant documents, archive original

---

## Proposed Organization Structure

```text
features/
├── bulk-import/
│   ├── README.md (Master index)
│   ├── 01-strategy/
│   │   ├── master-analysis.md (Consolidated from 4 general analysis files)
│   │   ├── csv-import-strategy.md (Merged from 2 CSV files)
│   │   └── koog-evaluation.md (Merged from 3 Koog files)
│   ├── 02-implementation/
│   │   └── technical-implementation-guide.md (Keep as-is)
│   ├── 03-sources/
│   │   └── web-source-analysis.md (Keep as-is)
│   └── archive/
│       └── [Original files preserved for reference]
```

---

## Consolidation Actions

### Action 1: Create Master Analysis Document
**Source Files:**
- krithi-bulk-import-capability-analysis-goose.md (primary)
- krithi-import-orchestration-comprehensive-analysis-claude.md (supplement)
- krithi-bulk-import-capability-analysis-gemini.md (supplement)
- krithi-import-analysis.md (supplement)

**Output:** `bulk-import/01-strategy/master-analysis.md`

### Action 2: Merge CSV Import Strategy
**Source Files:**
- krithi-bulk-import-from-csv-strategy-goose.md (primary)
- analysis-bulk-import-google.md (Phase 1 details)

**Output:** `bulk-import/01-strategy/csv-import-strategy.md`

### Action 3: Consolidate Koog Evaluation
**Source Files:**
- koog-evaluation-for-import-pipeline-goose.md (primary)
- koog-integration-analysis.md (supplement)
- koog-technical-integration-proposal.md (supplement)

**Output:** `bulk-import/01-strategy/koog-evaluation.md`

### Action 4: Create Master README
**Output:** `bulk-import/README.md` (navigation and feature identification)

---

## Feature Identification for Implementation

### Feature 1: CSV Manifest Ingestion (Phase 1)
**Priority:** HIGH (Foundation)
**Complexity:** LOW
**Timeline:** 1 week
**Documents:**
- `csv-import-strategy.md` (Phase 1 section)
- `analysis-bulk-import-google.md` (detailed Phase 1 design)

**Description:** Parse CSV files and load into `imported_krithis` staging table

### Feature 2: Web Scraping & Enrichment (Phase 2)
**Priority:** HIGH (Core functionality)
**Complexity:** MEDIUM
**Timeline:** 2-3 weeks
**Documents:**
- `master-analysis.md` (scraping strategy)
- `web-source-analysis.md` (source-specific details)
- `technical-implementation-guide.md` (implementation patterns)

**Description:** Scrape URLs from CSV and extract full metadata

### Feature 3: Entity Resolution
**Priority:** HIGH (Data quality)
**Complexity:** MEDIUM-HIGH
**Timeline:** 2 weeks
**Documents:**
- `master-analysis.md` (entity resolution deep dive)
- `technical-implementation-guide.md` (EntityResolutionService)

**Description:** Map raw text to canonical entities (composer, raga, deity, temple)

### Feature 4: Review Workflow
**Priority:** MEDIUM (Human-in-loop)
**Complexity:** MEDIUM
**Timeline:** 1-2 weeks
**Documents:**
- `master-analysis.md` (moderation requirements)
- `technical-implementation-guide.md` (review UI)

**Description:** Admin interface for reviewing and approving imports

---

## Next Steps

1. Execute consolidation actions (create merged documents)
2. Create master README with feature navigation
3. Archive original files
4. Update main features README.md to reference new structure
5. Identify first feature for implementation (recommend: Feature 1 - CSV Manifest Ingestion)