| Metadata | Value |
|:---|:---|
| **Status** | Active |
| **Version** | 1.0.0 |
| **Last Updated** | 2026-01-26 |
| **Author** | Sangeetha Grantha Team |

# Bulk Import Master Analysis


---

## Document Consolidation Note

This document consolidates analysis from multiple sources:
- **Primary Source**: `krithi-bulk-import-capability-analysis-goose.md` (Goose comprehensive analysis)
- **Supplemental Sources**:
  - `krithi-import-orchestration-comprehensive-analysis-claude.md` (Claude's orchestration deep dive)
  - `krithi-bulk-import-capability-analysis-gemini.md` (Gemini's analysis)
  - `krithi-import-analysis.md` (General import analysis)

Key insights from all sources have been integrated into this master document.

---

## 1. Executive Summary

This document provides a comprehensive analysis of building a bulk import capability for Krithis from multiple web sources. The analysis covers:

- **Data Sources**: Analysis of five primary web sources (Karnatik.com, Guru Guha Blog, Dikshitar Kritis List, Syama Krishna Vaibhavam, Thyagaraja Vaibhavam) and TempleNet
- **Import Requirements**: Core features needed (Krithi, Raga, Deity, Temple/Kshetra)
- **Data Quality Challenges**: Cleansing, de-duplication, moderation requirements
- **Orchestration Options**: Evaluation of Koog and alternative approaches
- **Implementation Recommendations**: Phased approach with risk mitigation

**Note**: The Dikshitar Kritis List source is particularly important for Trinity composer coverage (Tyagaraja, Dikshitar, Shyama Shastri).

**Key Finding**: The import capability requires a multi-stage pipeline with AI-powered extraction, validation, and human-in-the-loop moderation. Koog offers strong orchestration capabilities but may be over-engineered for initial phases. A hybrid approach combining existing Gemini integration with targeted workflow orchestration is recommended.

---

## 2. Data Source Analysis

### 2.1 Source Overview

**Note on Trinity Composers**: The Carnatic music tradition recognizes three composers as the "Trinity" (Trimurti):
- **Tyagaraja** (Thyagaraja)
- **Muthuswami Dikshitar** (Dikshitar)
- **Shyama Shastri** (Syama Shastri)

Comprehensive coverage of these composers is critical for the Sangeetha Grantha project. The Dikshitar Kritis List source is particularly important for ensuring Trinity composer completeness.

### 2.2 Source Overview Table

| Source | URL | Content Type | Structure | Estimated Volume |
|:---|:---|:---|:---|:---|
| **Karnatik.com** | https://karnatik.com/lyrics.shtml | Structured HTML | Well-organized, consistent format | ~10,000+ compositions |
| **Guru Guha Blog** | https://guru-guha.blogspot.com/ | Blog posts | Semi-structured, variable format | ~500-1,000 posts |
| **Dikshitar Kritis List** | https://guru-guha.blogspot.com/2009/04/dikshitar-kritis-alphabetical-list.html | Blog archive | List-based, alphabetical | ~500+ compositions |
| **Syama Krishna Vaibhavam** | https://syamakrishnavaibhavam.blogspot.com/2011/03/alphabetica-list-of-kritis.html | Blog archive | List-based, alphabetical | ~1,000+ compositions |
| **Thyagaraja Vaibhavam** | https://thyagaraja-vaibhavam.blogspot.com/2009/03/tyagaraja-kritis-alphabetical-list.html | Blog archive | List-based, alphabetical | ~700+ compositions |
| **TempleNet** | http://templenet.com/ | Temple database | Structured temple information | ~1,000+ temples |

### 2.3 Source-Specific Characteristics

#### 2.2.1 Karnatik.com (https://karnatik.com/lyrics.shtml)

**Strengths:**
- Most authoritative and comprehensive source
- Consistent HTML structure
- Well-maintained and regularly updated
- Includes composer, raga, tala metadata
- Often includes multiple language/script variants

**Challenges:**
- Large volume requires efficient scraping
- Rate limiting considerations
- May require pagination handling
- Copyright/attribution requirements

**Data Quality:**
- **High**: Professional curation
- **Consistency**: Very high
- **Completeness**: High (metadata usually complete)

**Extraction Complexity:** ⭐⭐ (Low-Medium)
- Predictable HTML structure
- Clear metadata patterns
- Section identification is straightforward

---

#### 2.2.2 Guru Guha Blog (https://guru-guha.blogspot.com/)

**Strengths:**
- Rich content with detailed explanations
- Often includes sampradaya information
- May include notation or audio references
- Community-contributed insights

**Challenges:**
- Variable post structure
- Mixed content (explanations + lyrics)
- Blogspot platform limitations
- Inconsistent metadata presentation
- May include commentary mixed with lyrics

**Data Quality:**
- **Medium-High**: Good content but less structured
- **Consistency**: Medium (varies by post)
- **Completeness**: Medium (some metadata may be missing)

**Extraction Complexity:** ⭐⭐⭐⭐ (High)
- Requires AI to distinguish lyrics from commentary
- Section boundaries may be implicit
- Metadata extraction needs context understanding

---

#### 2.2.3 Syama Krishna Vaibhavam Blog (https://syamakrishnavaibhavam.blogspot.com/)

**Strengths:**
- Focused on specific composer (Syama Krishna)
- Alphabetical organization aids discovery
- May include rare compositions

**Challenges:**
- Blogspot platform (similar to Guru Guha)
- List format may lack detailed lyrics
- Links to external sources may be broken
- Older content (2011) may have link rot

**Data Quality:**
- **Medium**: Good for discovery, may need cross-referencing
- **Consistency**: Medium
- **Completeness**: Medium-Low (may be title-only lists)

**Extraction Complexity:** ⭐⭐⭐ (Medium-High)
- List parsing required
- May need to follow links for full content
- Link validation and fallback strategies needed

---

#### 2.2.4 Dikshitar Kritis List (https://guru-guha.blogspot.com/2009/04/dikshitar-kritis-alphabetical-list.html)

**Strengths:**
- Comprehensive Dikshitar kriti collection (Trinity composer)
- Alphabetical organization
- Focused composer scope (Muthuswami Dikshitar)
- Part of Guru Guha blog (same domain as main blog)
- Important for Trinity composer coverage

**Challenges:**
- Similar to other alphabetical list sources
- Older content (2009)
- Blogspot platform limitations
- May require link following for full lyrics
- May be title-only entries

**Data Quality:**
- **Medium**: Good for Dikshitar-specific discovery
- **Consistency**: Medium
- **Completeness**: Medium (may need cross-referencing with Karnatik)

**Extraction Complexity:** ⭐⭐⭐ (Medium-High)
- List parsing
- Link following required
- Cross-reference validation recommended
- Similar to Thyagaraja and Syama Krishna lists

**Note**: This source is critical for comprehensive Trinity composer coverage (Tyagaraja, Dikshitar, Shyama Shastri).

---

#### 2.2.5 Thyagaraja Vaibhavam Blog (https://thyagaraja-vaibhavam.blogspot.com/)

**Strengths:**
- Comprehensive Tyagaraja kriti collection
- Alphabetical organization
- Focused composer scope

**Challenges:**
- Similar to Syama Krishna Vaibhavam
- Older content (2009)
- Blogspot platform limitations
- May require link following for full lyrics

**Data Quality:**
- **Medium**: Good for Tyagaraja-specific discovery
- **Consistency**: Medium
- **Completeness**: Medium (may need cross-referencing with Karnatik)

**Extraction Complexity:** ⭐⭐⭐ (Medium-High)
- List parsing
- Link following required
- Cross-reference validation recommended

---

#### 2.2.6 TempleNet (http://templenet.com/)

**Strengths:**
- Comprehensive temple database
- Geographic information
- Deity associations
- Historical context
- Multilingual names

**Challenges:**
- Separate data source (not Krithi-specific)
- Requires entity matching/linking
- May need manual verification for accuracy
- Website structure may vary by section

**Data Quality:**
- **High**: Authoritative temple information
- **Consistency**: High
- **Completeness**: High for major temples

**Extraction Complexity:** ⭐⭐⭐ (Medium)
- Structured data but separate domain
- Entity resolution required
- Integration with Krithi import pipeline

---

### 2.4 Cross-Source Data Quality Challenges

#### 2.3.1 Metadata Inconsistencies

**Composer Name Variations:**
- "Tyagaraja" vs "Thyagaraja" vs "Tyagayya" vs "Saint Tyagaraja"
- "Muthuswami Dikshitar" vs "Muthuswamy Dikshitar" vs "Dikshitar" vs "Muthuswami Dikshithar"
- "Shyama Shastri" vs "Syama Shastri" vs "Shyama Sastri"
- Note: Dikshitar source adds importance for Trinity composer coverage

**Raga Name Variations:**
- "Kalyani" vs "Yaman" (Hindustani equivalent)
- "Bhairavi" vs "Bhairavi" (different spellings)
- "Shankarabharanam" vs "Sankarabharanam"
- Janya raga naming conventions

**Tala Variations:**
- "Adi Tala" vs "Adi" vs "Adi Talam"
- "Rupaka" vs "Rupakam"
- Regional naming differences

**Deity Name Variations:**
- "Venkateshwara" vs "Venkateswara" vs "Balaji"
- "Meenakshi" vs "Meenakshi Amman"
- Multilingual deity names

#### 2.3.2 Structural Variations

**Section Identification:**
- Explicit labels: "Pallavi:", "Anupallavi:", "Charanam:"
- Implicit structure: Line breaks, indentation
- Missing sections: Some sources only have Pallavi
- Multiple Charanams: Numbering conventions vary

**Language/Script Variations:**
- Same composition in different scripts
- Transliteration schemes vary
- Mixed scripts within single source

#### 2.3.3 Completeness Issues

**Missing Metadata:**
- Some sources lack tala information
- Deity associations may be implicit
- Temple/Kshetra information often missing
- Composer attribution may be unclear

**Incomplete Lyrics:**
- Partial compositions (first line only)
- Missing Charanams
- Notation vs. lyrics confusion

---

## 3. Import Capability Requirements

### 3.1 Core Data Entities to Import

Based on the Sangeetha Grantha domain model, the import pipeline must extract and map:

#### 3.1.1 Krithi Core Data
- **Title**: Composition name (may have variations)
- **Incipit**: Opening line (first line of Pallavi)
- **Composer**: Must map to canonical `composers` table
- **Musical Form**: KRITHI, VARNAM, SWARAJATHI
- **Primary Language**: Language code (sa, ta, te, kn, ml, hi, en)
- **Workflow State**: Always starts as `DRAFT` or `pending` in import pipeline

#### 3.1.2 Raga Association
- **Primary Raga**: Single raga for non-ragamalika compositions
- **Ragamalika Support**: Ordered list of ragas
- **Section-Level Raga Mapping**: Optional association of ragas to specific sections
- **Mapping Challenge**: Name normalization and canonical raga resolution

#### 3.1.3 Deity Association
- **Deity**: Optional but important for thematic search
- **Extraction Challenge**: May be implicit in lyrics or title
- **Mapping Challenge**: Name variations and multilingual names

#### 3.1.4 Temple/Kshetra Association
- **Temple**: Optional association with physical temple location
- **Source**: TempleNet integration for temple metadata
- **Mapping Challenge**: 
  - Linking temple names from lyrics to TempleNet database
  - Handling multilingual temple names
  - Geographic disambiguation (multiple temples with same deity)

#### 3.1.5 Lyric Structure
- **Sections**: Pallavi, Anupallavi, Charanam(s)
- **Section Text**: Per-section lyrics
- **Language/Script**: Support for multiple scripts
- **Sampradaya**: Optional lineage/school information

#### 3.1.6 Additional Metadata
- **Tala**: Rhythmic cycle (may be missing)
- **Tags**: Thematic tags (BHAVA, FESTIVAL, etc.)
- **Source Attribution**: Track origin for each imported piece

---

### 3.2 Import Pipeline Stages

#### Stage 1: Discovery & URL Collection
- **Input**: Source URLs or sitemap discovery
- **Output**: List of URLs to scrape
- **Challenges**: 
  - Pagination handling
  - Link validation
  - Duplicate detection

#### Stage 2: Web Scraping
- **Input**: Individual URLs
- **Output**: Raw HTML content
- **Challenges**:
  - Rate limiting
  - Error handling
  - Content extraction (removing navigation, ads, etc.)

#### Stage 3: Content Extraction
- **Input**: Raw HTML
- **Output**: Structured JSON (ScrapedKrithiMetadata)
- **AI-Powered**: Gemini-based extraction
- **Challenges**:
  - Section identification
  - Metadata extraction
  - Handling variable formats

#### Stage 4: Entity Resolution & Mapping
- **Input**: Extracted metadata
- **Output**: Mapped to canonical entities
- **Challenges**:
  - Composer name normalization
  - Raga name resolution
  - Deity name matching
  - Temple name matching (with TempleNet)

#### Stage 5: Data Cleansing
- **Input**: Mapped entities
- **Output**: Cleaned, normalized data
- **Operations**:
  - Text normalization
  - Whitespace cleanup
  - Encoding fixes
  - Section boundary refinement

#### Stage 6: De-duplication
- **Input**: Cleaned data
- **Output**: Unique records with duplicate detection
- **Challenges**:
  - Title variations
  - Incipit matching
  - Composer + Raga + Title combinations
  - Cross-source duplicate detection

#### Stage 7: Validation
- **Input**: Processed data
- **Output**: Validation results
- **Checks**:
  - Required fields present
  - Entity references valid
  - Section structure valid
  - Musicological consistency (optional)

#### Stage 8: Staging & Review
- **Input**: Validated data
- **Output**: Records in `imported_krithis` table
- **Status**: `pending` → `in_review` → `mapped`/`rejected`

#### Stage 9: Human Moderation
- **Input**: Staged records
- **Output**: Approved mappings or rejections
- **UI**: Import review interface
- **Actions**: Map to existing Krithi or create new

#### Stage 10: Canonicalization
- **Input**: Approved imports
- **Output**: Records in `krithis` and related tables
- **Workflow**: Create Krithi entities with proper relationships

---

### 3.3 Data Cleansing Requirements

#### 3.3.1 Text Normalization
- **Whitespace**: Normalize multiple spaces, tabs, newlines
- **Encoding**: Fix character encoding issues (UTF-8 normalization)
- **Special Characters**: Handle quotes, dashes, apostrophes consistently
- **Line Breaks**: Preserve meaningful breaks, remove excessive breaks

#### 3.3.2 Name Normalization
- **Composer Names**: 
  - Remove honorifics ("Saint", "Sri", "Swami")
  - Standardize spellings
  - Handle initials vs. full names
- **Raga Names**:
  - Remove "Raga" prefix
  - Standardize spellings
  - Handle janya raga parent relationships
- **Deity Names**:
  - Handle honorifics and titles
  - Multilingual name resolution
- **Temple Names**:
  - Geographic disambiguation
  - Multilingual name handling
  - Alias resolution

#### 3.3.3 Structural Cleanup
- **Section Headers**: Normalize labels ("Pallavi", "Pallavi:", "PALLAVI")
- **Numbering**: Standardize Charanam numbering
- **Metadata Formatting**: Extract from various formats (key-value, inline, etc.)

---

### 3.4 De-duplication Strategy

#### 3.4.1 Duplicate Detection Criteria

**Primary Keys for Matching:**
1. **Composer + Title + Incipit**: Strong match
2. **Composer + Title**: Medium match (verify incipit)
3. **Title + Incipit**: Weak match (may be different composers)
4. **Incipit Only**: Very weak (many compositions share opening lines)

**Fuzzy Matching:**
- Title similarity (Levenshtein distance)
- Incipit similarity
- Normalized name matching

#### 3.4.2 Cross-Source Duplicate Handling

**Scenario**: Same Krithi from multiple sources
- **Strategy**: Merge metadata, preserve all source attributions
- **Priority**: Prefer higher-quality source for primary data
- **Conflict Resolution**: Human review for conflicting information

#### 3.4.3 Duplicate Resolution Workflow

1. **Automatic Detection**: Flag potential duplicates during import
2. **Confidence Scoring**: Assign confidence to duplicate matches
3. **Review Queue**: High-confidence duplicates auto-merged, others flagged
4. **Manual Review**: Human verification for edge cases

---

### 3.5 Moderation Requirements

#### 3.5.1 Pre-Moderation Checks

**Automatic Validation:**
- Required fields present (title, composer, at least one section)
- Entity references valid (composer, raga exist in database)
- Section structure valid (Pallavi typically required)
- No obvious data corruption

**Confidence Scoring:**
- Extraction confidence (from AI)
- Entity mapping confidence
- Completeness score
- Quality score

#### 3.5.2 Human Moderation Workflow

**Review Interface Requirements:**
- Side-by-side comparison (source vs. extracted)
- Entity mapping visualization
- Edit capabilities for corrections
- Batch operations (approve/reject multiple)
- Notes and annotations

**Moderation Actions:**
- **Approve & Map**: Link to existing Krithi
- **Approve & Create**: Create new Krithi entity
- **Reject**: Mark as rejected with reason
- **Request Revision**: Send back for re-extraction
- **Merge**: Combine with another import

#### 3.5.3 Quality Gates

**Before Publishing:**
- All required fields validated
- Entity mappings confirmed
- Duplicate checks passed
- Human review completed
- Source attribution recorded

---

## 4. Orchestration Options Analysis

### 4.1 Option A: Koog-Based Orchestration

#### 4.1.1 Koog Capabilities for Import Pipeline

**Strengths:**
- **Graph Workflows**: Perfect for multi-stage pipeline
- **Tool Calling**: Can integrate scraping, extraction, validation as tools
- **Retry & Persistence**: Handle failures gracefully
- **Observability**: OpenTelemetry tracing
- **Provider Flexibility**: Switch LLM providers if needed
- **Kotlin DSL**: Type-safe, fits existing stack

**Workflow Example:**
```
Discovery Node → Scraping Node → Extraction Node → 
Entity Resolution Node → Cleansing Node → 
Deduplication Node → Validation Node → Staging Node
```

#### 4.1.2 Koog Integration Points

**Existing Infrastructure:**
- Ktor backend (Koog has native Ktor support)
- Gemini integration (Koog supports Gemini)
- Database access (can create tools for DAL operations)

**New Components Needed:**
- Koog agent definitions
- Tool implementations (scraping, extraction, validation)
- Workflow orchestration logic
- Error handling and retry policies

#### 4.1.3 Pros & Cons

**Pros:**
- ✅ Enterprise-grade orchestration
- ✅ Built-in retry and fault tolerance
- ✅ Excellent observability
- ✅ Type-safe Kotlin DSL
- ✅ Fits existing stack (Ktor + Kotlin)
- ✅ Provider flexibility for future

**Cons:**
- ❌ Additional dependency and learning curve
- ❌ May be over-engineered for initial phases
- ❌ Requires refactoring existing Gemini integration
- ❌ Operational complexity (another system to monitor)
- ❌ Potential performance overhead

**Effort**: High (2-3 weeks for initial implementation)

---

### 4.2 Option B: Custom Workflow Engine (Kotlin Coroutines)

#### 4.2.1 Approach

Build a lightweight workflow engine using Kotlin Coroutines and structured concurrency:

```kotlin
class ImportPipeline {
    suspend fun processImport(source: ImportSource, urls: List<String>) {
        urls.map { url ->
            async {
                val html = scrape(url)
                val extracted = extractMetadata(html)
                val mapped = resolveEntities(extracted)
                val cleaned = cleanse(mapped)
                val validated = validate(cleaned)
                stageForReview(validated)
            }
        }.awaitAll()
    }
}
```

#### 4.2.2 Pros & Cons

**Pros:**
- ✅ Full control over workflow
- ✅ No external dependencies
- ✅ Lightweight and performant
- ✅ Easy to debug and maintain
- ✅ Can leverage existing Gemini integration

**Cons:**
- ❌ Manual retry logic needed
- ❌ Limited observability (need to build)
- ❌ No built-in persistence for long-running workflows
- ❌ More code to maintain

**Effort**: Medium (1-2 weeks)

---

### 4.3 Option C: Hybrid Approach (Recommended)

#### 4.3.1 Strategy

**Phase 1: Custom Workflow (Immediate)**
- Build lightweight coroutine-based pipeline
- Leverage existing `WebScrapingService` and `TransliterationService`
- Add entity resolution and validation layers
- Implement basic retry and error handling

**Phase 2: Evaluate Koog (After Phase 1)**
- Build POC with Koog for one workflow stage
- Compare performance, observability, maintainability
- Decide on full adoption or selective use

**Phase 3: Optimize Based on Learnings**
- Either enhance custom workflow or adopt Koog
- Add advanced features (persistence, complex retries)

#### 4.3.2 Implementation Plan

**Immediate (Weeks 1-4):**
1. Enhance `WebScrapingService` for multi-source support
2. Build `EntityResolutionService` for composer/raga/deity/temple mapping
3. Create `ImportPipelineService` using coroutines
4. Add de-duplication logic
5. Enhance import review UI

**Evaluation (Weeks 5-6):**
1. Build Koog POC for extraction stage
2. Compare with custom implementation
3. Document findings

**Decision Point:**
- If Koog adds significant value → Adopt for complex workflows
- If custom solution sufficient → Continue enhancement

---

### 4.4 Option D: Third-Party ETL Tools

#### 4.4.1 Options Considered

- **Apache Airflow**: Overkill, Python-based
- **Temporal**: Good for workflows but adds infrastructure
- **Prefect**: Python-focused
- **n8n / Zapier**: Not suitable for programmatic workflows

**Verdict**: Not recommended for this use case. Kotlin-native solutions preferred.

---

## 5. Technical Architecture Recommendations

### 5.1 Recommended Architecture (Hybrid Approach)

```
┌─────────────────────────────────────────────────────────┐
│                   Import API Endpoints                   │
│  POST /v1/admin/imports/sources/{id}/scrape            │
│  POST /v1/admin/imports/batch                           │
└──────────────────────┬──────────────────────────────────┘
                       │
┌──────────────────────▼──────────────────────────────────┐
│              ImportPipelineService                      │
│  (Kotlin Coroutines-based workflow orchestration)      │
└──────┬──────────┬──────────┬──────────┬───────────────┘
       │          │          │          │
       ▼          ▼          ▼          ▼
┌──────────┐ ┌──────────┐ ┌──────────┐ ┌──────────┐
│ Scraping │ │Extraction│ │  Entity  │ │Validation│
│ Service  │ │ Service  │ │Resolver  │ │ Service  │
└────┬─────┘ └────┬─────┘ └────┬─────┘ └────┬─────┘
     │           │            │            │
     ▼           ▼            ▼            ▼
┌─────────────────────────────────────────────────────┐
│         Existing Services (Enhanced)                 │
│  - WebScrapingService (Gemini-powered)              │
│  - TransliterationService                           │
│  - Entity Repositories (Composer, Raga, etc.)       │
└─────────────────────────────────────────────────────┘
```

### 5.2 Service Layer Design

#### 5.2.1 ImportPipelineService

**Responsibilities:**
- Orchestrate multi-stage import workflow
- Handle retries and error recovery
- Track progress and status
- Batch processing

**Key Methods:**
```kotlin
suspend fun importFromSource(
    sourceId: UUID,
    urls: List<String>,
    options: ImportOptions
): ImportBatchResult

suspend fun processImportBatch(
    batchId: UUID
): ImportBatchStatus
```

#### 5.2.2 EntityResolutionService (New)

**Responsibilities:**
- Map extracted names to canonical entities
- Fuzzy matching for variations
- Confidence scoring
- Handle ambiguous matches

**Key Methods:**
```kotlin
suspend fun resolveComposer(name: String): EntityMatch<Composer>
suspend fun resolveRaga(name: String): EntityMatch<Raga>
suspend fun resolveDeity(name: String): EntityMatch<Deity>
suspend fun resolveTemple(name: String, context: String?): EntityMatch<Temple>
```

#### 5.2.3 DeduplicationService (New)

**Responsibilities:**
- Detect duplicates within import batch
- Detect duplicates against existing Krithis
- Confidence scoring
- Merge strategy recommendations

**Key Methods:**
```kotlin
suspend fun findDuplicates(
    imported: ImportedKrithiDto
): List<DuplicateMatch>

suspend fun mergeDuplicates(
    primaryId: UUID,
    duplicateIds: List<UUID>
): MergeResult
```

#### 5.2.4 ValidationService (Enhanced)

**Responsibilities:**
- Validate extracted data structure
- Check entity references
- Musicological validation (optional)
- Quality scoring

---

### 5.3 Database Schema Enhancements

#### 5.3.1 Import Batch Tracking

**New Table: `import_batches`**
```sql
CREATE TABLE import_batches (
  id UUID PRIMARY KEY,
  import_source_id UUID REFERENCES import_sources(id),
  status TEXT, -- pending, processing, completed, failed
  total_urls INT,
  processed_urls INT,
  successful_imports INT,
  failed_imports INT,
  started_at TIMESTAMPTZ,
  completed_at TIMESTAMPTZ,
  error_summary JSONB
);
```

#### 5.3.2 Enhanced Imported Krithis

**Add Fields to `imported_krithis`:**
- `extraction_confidence` DECIMAL (0-1)
- `entity_mapping_confidence` DECIMAL (0-1)
- `duplicate_candidates` JSONB (array of potential duplicate IDs)
- `quality_score` DECIMAL (0-1)
- `processing_errors` JSONB (array of errors encountered)

---

### 5.4 TempleNet Integration

#### 5.4.1 Integration Strategy

**Option 1: Pre-fetch and Cache**
- Scrape TempleNet once
- Store in `temples` table
- Use for matching during import

**Option 2: On-Demand Lookup**
- Query TempleNet API (if available) during import
- Cache results
- Fallback to fuzzy matching if API unavailable

**Option 3: Hybrid**
- Pre-fetch major temples
- On-demand lookup for rare temples
- Manual curation for edge cases

#### 5.4.2 Temple Matching Logic

```kotlin
suspend fun matchTemple(
    name: String,
    deity: String?,
    location: String?
): List<TempleMatch> {
    // 1. Exact name match
    // 2. Fuzzy name match
    // 3. Deity + location match
    // 4. Multilingual name match
    // Return ranked matches with confidence
}
```

---

## 6. Implementation Roadmap

### Phase 1: Foundation (Weeks 1-4)

**Goals:**
- Multi-source scraping support
- Basic entity resolution
- Import pipeline skeleton
- Enhanced import review UI

**Deliverables:**
1. Enhanced `WebScrapingService` with source-specific handlers
2. `EntityResolutionService` with fuzzy matching
3. `ImportPipelineService` (coroutine-based)
4. Basic de-duplication logic
5. Import batch tracking
6. Enhanced import review UI with confidence scores

**Success Criteria:**
- Can import from Karnatik.com successfully
- Entity resolution accuracy >80%
- Import review workflow functional

---

### Phase 2: Data Quality (Weeks 5-8)

**Goals:**
- Advanced de-duplication
- Data cleansing pipeline
- Validation rules
- Quality scoring

**Deliverables:**
1. `DeduplicationService` with cross-source detection
2. `DataCleansingService` for normalization
3. `ValidationService` with musicological checks
4. Quality scoring system
5. Batch operations in review UI

**Success Criteria:**
- Duplicate detection accuracy >90%
- Data quality score >85% for approved imports
- Batch processing supports 100+ URLs

---

### Phase 3: TempleNet Integration & Trinity Sources (Weeks 9-10)

**Goals:**
- TempleNet data ingestion
- Temple matching during import
- Geographic disambiguation
- Dikshitar List integration (Trinity composer coverage)

**Deliverables:**
1. TempleNet scraper/cache
2. Temple matching service
3. Integration with import pipeline
4. Temple entity management UI enhancements
5. Dikshitar List handler (similar to Thyagaraja handler)

**Success Criteria:**
- Temple matching accuracy >75%
- Major temples pre-loaded
- Manual curation workflow for edge cases
- Dikshitar compositions imported (Trinity completeness)

---

### Phase 4: Advanced Features (Weeks 11-12)

**Goals:**
- Koog POC evaluation
- Advanced retry logic
- Observability improvements
- Performance optimization

**Deliverables:**
1. Koog POC for extraction stage
2. Comparison analysis
3. Enhanced error handling
4. Performance optimizations
5. Monitoring and alerting

**Success Criteria:**
- Decision on Koog adoption
- Import pipeline handles 1000+ URLs efficiently
- Error rate <5%

---

## 7. Risk Assessment & Mitigation

### 7.1 Technical Risks

| Risk | Impact | Probability | Mitigation |
|:---|:---|:---|:---|
| **Source website changes** | High | Medium | Version source handlers, cache HTML, graceful degradation |
| **Rate limiting** | Medium | High | Implement backoff, respect robots.txt, batch processing |
| **Entity resolution accuracy** | High | Medium | Human review workflow, confidence thresholds, manual override |
| **Duplicate detection false positives** | Medium | Medium | Conservative matching, human review, merge workflow |
| **TempleNet availability** | Low | Low | Cache data, fallback matching, manual curation |
| **Performance at scale** | Medium | Low | Batch processing, async operations, database optimization |

### 7.2 Data Quality Risks

| Risk | Impact | Probability | Mitigation |
|:---|:---|:---|:---|
| **Incorrect metadata extraction** | High | Medium | Multi-pass validation, human review, source comparison |
| **Missing sections** | Medium | Medium | Section detection validation, manual correction workflow |
| **Language/script misidentification** | Medium | Low | Language detection validation, manual override |
| **Composer misattribution** | High | Low | Cross-reference validation, expert review for edge cases |

### 7.3 Operational Risks

| Risk | Impact | Probability | Mitigation |
|:---|:---|:---|:---|
| **Import pipeline failures** | Medium | Medium | Comprehensive error handling, retry logic, manual recovery |
| **Database performance** | Medium | Low | Batch processing, indexing, connection pooling |
| **Human review bottleneck** | Medium | Medium | Batch operations, confidence-based prioritization, automation |

---

## 8. Success Metrics

### 8.1 Import Volume Metrics

- **Target**: Import 10,000+ Krithis from all sources within 6 months
- **Throughput**: Process 100+ URLs per batch
- **Success Rate**: >90% successful extraction

### 8.2 Data Quality Metrics

- **Entity Resolution Accuracy**: >85% automatic, >95% with human review
- **Duplicate Detection**: >90% true positive rate, <5% false positive rate
- **Data Completeness**: >80% of imports have all required fields
- **Human Review Efficiency**: <2 minutes per import on average

### 8.3 Operational Metrics

- **Pipeline Reliability**: >99% uptime
- **Error Recovery**: <5% manual intervention required
- **Processing Time**: <30 seconds per URL on average
- **Review Queue**: <24 hours average time to review

---

## 9. Open Questions & Decisions Needed

### 9.1 Technical Decisions

1. **Koog Adoption**: Proceed with POC or defer?
2. **TempleNet Integration**: Pre-fetch vs. on-demand?
3. **De-duplication Strategy**: Automatic merge vs. always review?
4. **Retry Policy**: How many retries? Exponential backoff parameters?

### 9.2 Data Quality Decisions

1. **Confidence Thresholds**: What scores trigger automatic approval?
2. **Source Priority**: How to handle conflicting information?
3. **Missing Data**: Allow incomplete imports or require all fields?
4. **Validation Strictness**: How strict should musicological validation be?

### 9.3 Operational Decisions

1. **Review Workflow**: Single reviewer or multi-stage review?
2. **Batch Size**: Optimal batch size for processing?
3. **Scheduling**: Scheduled imports or on-demand only?
4. **Attribution**: How to handle source attribution in UI?

---

## 10. Recommendations

### 10.1 Immediate Actions (Next 2 Weeks)

1. **Start with Custom Workflow**: Build coroutine-based pipeline for immediate needs
2. **Focus on Karnatik.com First**: Highest quality, most structured source
3. **Build Entity Resolution Service**: Critical for data quality
4. **Enhance Import Review UI**: Human-in-the-loop is essential

### 10.2 Medium-Term (Next 2-3 Months)

1. **Evaluate Koog**: Build POC after custom workflow is stable
2. **Add Trinity Sources**: Dikshitar List (important for Trinity composer coverage)
3. **Add Remaining Sources**: Expand to other blog sources
4. **TempleNet Integration**: Pre-fetch and cache temple data
5. **Advanced De-duplication**: Cross-source duplicate detection

### 10.3 Long-Term (6+ Months)

1. **Koog Decision**: Adopt if value is clear, otherwise enhance custom solution
2. **Automation**: Increase confidence thresholds for auto-approval
3. **Continuous Import**: Scheduled imports for updated sources
4. **Quality Improvements**: Machine learning for better entity resolution

---

## 11. Conclusion

The bulk import capability is a critical feature for scaling Sangeetha Grantha's content. The recommended **hybrid approach** balances:

- **Immediate Needs**: Custom workflow gets us started quickly
- **Future Flexibility**: Koog evaluation keeps options open
- **Risk Mitigation**: Phased approach reduces risk
- **Quality Focus**: Human-in-the-loop ensures data quality

**Key Success Factors:**
1. Strong entity resolution (composer, raga, deity, temple)
2. Effective de-duplication across sources
3. Intuitive human review workflow
4. Robust error handling and recovery
5. Performance at scale

The import pipeline will be a significant engineering effort but is essential for building a comprehensive Carnatic music compendium.

---

## 12. References

- [Koog Integration Analysis](../archive/koog-integration-analysis.md)
- [Koog Technical Integration Proposal](../archive/koog-technical-integration-proposal.md)
- [Intelligent Content Ingestion](../../intelligent-content-ingestion.md)
- [Generic Scraping](../../generic-scraping.md)
- [Domain Model](../../../domain-model.md)
- [Database Schema](../../../../04-database/schema.md)
- [Import Pipeline Migration](../../../../../database/migrations/04__import-pipeline.sql)
