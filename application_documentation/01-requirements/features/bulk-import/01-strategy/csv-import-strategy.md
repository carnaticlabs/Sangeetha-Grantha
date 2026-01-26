| Metadata | Value |
|:---|:---|
| **Status** | Active |
| **Version** | 1.0.0 |
| **Last Updated** | 2026-01-26 |
| **Author** | Sangeetha Grantha Team |

# Krithi Bulk Import from CSV - Comprehensive Strategy & Design


---

## 1. Executive Summary

This document provides a comprehensive strategy and detailed design for bulk importing Krithis from CSV files located in `/database/for_import/`. The CSV files contain Krithi names, optional Raga values for authoring validation, and hyperlinks to source pages where full details can be scraped. This strategy builds upon existing research and leverages the current import infrastructure.

### Key Findings

1. **CSV Data Structure**: Three CSV files contain ~1,700+ Krithi entries with basic metadata (name, raga) and source URLs
2. **Existing Infrastructure**: The application already has `WebScrapingService` and `ImportService` that can be extended
3. **Source Analysis**: URLs point to blogspot.com pages (Thyagaraja Vaibhavam, Guru Guha, Syama Krishna Vaibhavam) with varying structure
4. **Recommended Approach**: Phased implementation starting with CSV parsing, then batch scraping, followed by entity resolution and review workflow

### Strategic Recommendations

- **Phase 1**: CSV parsing and URL syntax validation (Week 1)
- **Phase 2**: Batch scraping with rate limiting (Week 2)
- **Phase 3**: Entity resolution and de-duplication (Week 3)
- **Phase 4**: Review workflow integration (Week 4)

---

## 2. CSV Data Analysis

### 2.1 File Inventory

| File | Composer | Estimated Entries | Source Domain |
|:---|:---|:---|:---|
| `Thyagaraja-Krithi-For-Import.csv` | Thyagaraja | ~690 | thyagaraja-vaibhavam.blogspot.com |
| `Dikshitar-Krithi-For-Import.csv` | Muthuswami Dikshitar | ~480 | guru-guha.blogspot.com |
| `Syama-Sastri-Krithi-For-Import.csv` | Syama Sastri | ~70 | syamakrishnavaibhavam.blogspot.com |

**Total**: ~1,240+ Krithi entries across the Trinity composers

### 2.2 CSV Structure

```csv
Krithi,Raga,Hyperlink
abhimAnamennaDu,kunjari,http://thyagaraja-vaibhavam.blogspot.com/2007/11/thyagaraja-kriti-abhimaanamennadu-raga.html
```

**Fields**:
- **Krithi**: Composition name (transliterated, may have variations)
- **Raga**: Optional at ingest; used only for CSV authoring validation. Scraped values are the source of truth.
- **Hyperlink**: Direct URL to source page with full details

### 2.3 Data Quality Observations

**Strengths**:
- ✅ Direct URLs to source pages (no discovery needed)
- ✅ Basic metadata (name, raga) pre-identified
- ✅ Composer context implicit from filename
- ✅ Trinity composers well-represented

**Challenges**:
- ⚠️ Transliteration variations (e.g., "kunjari" vs "Kunjari" vs "Kunjarī")
- ⚠️ Raga name variations (e.g., "yadukula kAmbhOji" vs "Yadukula Kambhoji")
- ⚠️ Some entries may have broken links (older blogspot content from 2007-2011)
- ⚠️ Source pages may have varying HTML structure
- ⚠️ Some entries may be duplicates across files

### 2.4 Source URL Patterns

**Thyagaraja Vaibhavam**:
- Pattern: `http://thyagaraja-vaibhavam.blogspot.com/YYYY/MM/thyagaraja-kriti-{name}-raga-{raga}.html`
- Date range: 2007-2011
- Structure: Blogspot posts with structured content

**Guru Guha (Dikshitar)**:
- Pattern: `http://guru-guha.blogspot.com/YYYY/MM/dikshitar-kriti-{name}-raga-{raga}.html`
- Date range: 2007-2009
- Structure: Blogspot posts, may include list formats

**Syama Krishna Vaibhavam**:
- Pattern: `http://syamakrishnavaibhavam.blogspot.com/YYYY/MM/syama-sastry-kriti-{name}-raga-{raga}.html`
- Date range: 2011
- Structure: Blogspot posts

---

## 3. Architecture & Integration

### 3.1 Existing Infrastructure

The application already has:

1. **WebScrapingService**: 
   - Uses Gemini AI to extract structured metadata from URLs
   - Returns `ScrapedKrithiMetadata` with title, composer, raga, tala, deity, temple, lyrics, sections
   - Handles HTML cleaning and content extraction

2. **ImportService**:
   - `submitImports()`: Creates `ImportedKrithi` records in staging table
   - `reviewImport()`: Approves imports and creates canonical `Krithi` entities
   - Handles entity creation (composer, raga, tala) if not found

3. **ImportRepository**:
   - Database operations for `imported_krithis` and `import_sources` tables
   - Status tracking (PENDING, APPROVED, REJECTED)

4. **ImportRoutes**:
   - `/v1/admin/imports/scrape`: Single URL scraping endpoint
   - `/v1/admin/imports/krithis`: Batch import submission
   - `/v1/admin/imports/{id}/review`: Review workflow

### 3.2 Proposed Architecture

```
┌─────────────────────────────────────────────────────────┐
│              CSV Bulk Import Service                     │
│  (New: CsvBulkImportService)                            │
└──────┬──────────┬──────────┬──────────┬─────────────────┘
       │          │          │          │
       ▼          ▼          ▼          ▼
┌──────────┐ ┌──────────┐ ┌──────────┐ ┌──────────┐
│ CSV      │ │ URL      │ │ Batch   │ │ Entity   │
│ Parser   │ │ Validator│ │ Scraper │ │ Resolver │
└────┬─────┘ └────┬─────┘ └────┬────┘ └────┬────┘
     │            │             │            │
     └────────────┴─────────────┴────────────┘
                    │
       ┌────────────▼────────────┐
       │  Existing Services      │
       │  - WebScrapingService   │
       │  - ImportService        │
       │  - ImportRepository     │
       └─────────────────────────┘
```

### 3.3 Service Layer Design

**New Service: `CsvBulkImportService`**

```kotlin
class CsvBulkImportService(
    private val webScrapingService: WebScrapingService,
    private val importService: ImportService,
    private val entityResolutionService: EntityResolutionService, // New
    private val deduplicationService: DeduplicationService, // New
    private val csvParser: CsvParser // New
) {
    suspend fun importFromCsv(
        csvFilePath: String,
        composerContext: String? = null,
        options: BulkImportOptions = BulkImportOptions.default()
    ): BulkImportResult
    
    suspend fun validateCsvFile(csvFilePath: String): CsvValidationResult
    
    suspend fun processBatch(
        entries: List<CsvKrithiEntry>,
        batchId: UUID
    ): Flow<ImportProgress>
}
```

**New Service: `EntityResolutionService`**

```kotlin
class EntityResolutionService(
    private val composerRepo: ComposerRepository,
    private val ragaRepo: RagaRepository,
    private val deityRepo: DeityRepository,
    private val templeRepo: TempleRepository
) {
    suspend fun resolveComposer(name: String): EntityMatch<ComposerDto>
    suspend fun resolveRaga(name: String): EntityMatch<RagaDto>
    suspend fun resolveDeity(name: String): EntityMatch<DeityDto>
    suspend fun resolveTemple(name: String, deityContext: String?): EntityMatch<TempleDto>
}
```

**New Service: `DeduplicationService`**

```kotlin
class DeduplicationService(
    private val importRepo: ImportRepository,
    private val krithiRepo: KrithiRepository
) {
    suspend fun findDuplicates(
        imported: ImportedKrithiDto,
        batchContext: List<ImportedKrithiDto> = emptyList()
    ): List<DuplicateMatch>
    
    suspend fun detectBatchDuplicates(batch: List<ImportedKrithiDto>): DeduplicationResult
}
```

---

## 4. Implementation Plan

### 4.1 Phase 1: CSV Parsing & Validation (Completed)

**Objective**: Parse CSV files via Admin UI and load into `imported_krithis` staging table.

**Approach**: 
Implemented as a **Runtime API Upload** mechanism (see `TRACK-005`).
- **Endpoint**: `POST /v1/admin/bulk-import/upload` (Multipart)
- **Parser**: `Apache Commons CSV` for robust handling of quotes and delimiters.
- **Validation**: Header check ("Krithi", "Hyperlink" required; "Raga" optional) and URL syntax validation only.

#### 4.1.1 Data Mapping

| CSV Column | Database Column (`imported_krithis`) | Logic |
|:---|:---|:---|
| `Hyperlink` | `source_key` | Primary Identifier. Must be unique per source. |
| `Krithi` | `raw_title` | Direct map. |
| `Raga` | `raw_raga` | Optional; stored for provenance only. Scraped raga is authoritative. |
| *Derived* | `import_source_id` | Mapped to "BulkImportCSV" source. |

#### 4.1.2 Deliverables (Implemented)

1. **Backend API**: 
   - Handles multipart file uploads.
   - Saves file to `storage/imports/`.
   - Creates `import_batch` and `MANIFEST_INGEST` job.

2. **Manifest Worker**:
   - Asynchronous processing of the uploaded CSV.
   - Header validation (fails fast if invalid).
   - Row-by-row task creation (`SCRAPE` tasks).

3. **Frontend UI**:
   - File Upload Widget (Drag & Drop).
   - Real-time progress polling.

**Status**: ✅ Complete (TRACK-005)

---

### 4.2 Phase 2: Batch Scraping (Week 2)

**Objective**: Scrape all valid URLs with rate limiting and error handling

**Deliverables**:

1. **Batch Scraper**
```kotlin
class BatchScrapingService(
    private val webScrapingService: WebScrapingService,
    private val rateLimiter: RateLimiter
) {
    suspend fun scrapeBatch(
        urls: List<String>,
        options: BatchScrapingOptions
    ): Flow<ScrapingResult>
    
    suspend fun scrapeWithRetry(
        url: String,
        maxRetries: Int = 3
    ): ScrapedKrithiMetadata
}
```

2. **Rate Limiter**
```kotlin
class RateLimiter(
    private val requestsPerSecond: Int = 2, // Conservative for blogspot
    private val maxConcurrency: Int = 3
) {
    suspend fun <T> withRateLimit(block: suspend () -> T): T
}
```

3. **Progress Tracking**
```kotlin
data class BulkImportBatch(
    val id: UUID,
    val sourceFile: String,
    val totalEntries: Int,
    val processedEntries: Int,
    val successfulScrapes: Int,
    val failedScrapes: Int,
    val status: BatchStatus,
    val startedAt: Instant,
    val completedAt: Instant?
)
```

**Implementation Steps**:

1. Extend `WebScrapingService` with retry logic
2. Implement rate limiting (2 requests/second, 3 concurrent)
3. Create batch scraping service with progress tracking
4. Add database table for batch tracking
5. Implement resume capability (if batch fails, resume from last successful)

**Success Criteria**:
- ✅ 90%+ of valid URLs successfully scraped
- ✅ Rate limiting prevents IP blocking
- ✅ Failed scrapes logged with retry capability
- ✅ Progress tracking functional

---

### 4.3 Phase 3: Entity Resolution & De-duplication (Week 3)

**Objective**: Resolve entities and detect duplicates with strict normalization.

**Analysis Finding**: Preliminary analysis of `imported_krithis` shows significant Raga duplication due to transliteration variations (e.g., "Kalyani" vs "Kalyaani", "Kedaara Gaula" vs "Kedara Gaula").

**Deliverables**:

1. **Entity Resolution Service**
   - Enhanced with aggressive normalization rules.

2. **Name Normalization Logic**
   ```kotlin
   class NameNormalizationService {
       fun normalizeRagaName(name: String): String {
           return name.lowercase()
               .replace(Regex("\\s+"), "") // Remove all spaces: "kedara gaula" -> "kedaragaula"
               .replace("aa", "a")         // Vowel reduction: "kalyaani" -> "kalyani"
               .replace("ee", "i")
               .replace("oo", "o")
               .replace("uu", "u")
               .replace(Regex("[^a-z]"), "") // Remove special chars
       }
       // ... other normalizers
   }
   ```

3. **Fuzzy Matching Strategy**
   - **Step 1**: Exact match on *Normalized Name*.
   - **Step 2**: Levenshtein Distance on *Normalized Name* (threshold 0.90).
   - **Step 3**: Manual Alias Table (e.g., "Sankarabharanam" = "Sankaraabharanam").

4. **De-duplication Service**
   - Must check against *both* `ragas` table (canonical) and `imported_krithis` (staging) to prevent race conditions during batch processing.

**Implementation Steps**:

1. Implement `NameNormalizationService` with specific rules for Ragas (vowel reduction, space removal).
2. Update `EntityResolutionService` to use normalization before DB lookup.
3. Add `name_normalized` column to `ragas` table (if not present) and backfill.
4. Create a "Raga Alias" configuration for known exceptions.
5. Implement de-duplication logic that merges duplicate Raga candidates in memory before DB insertion.

#### 4.3.1 Specific Logic for Talas (New Findings)

**Observation**: The database contains variations like "Rupaka"/"Rupakam" and "Misra Chapu"/"miSra cApu".

**Tala Normalization Rules**:
1. **Suffix Removal**: Handle common endings.
   - "rupakam" -> "rupaka"
   - "ekam" -> "eka"
   - "tripuTa" -> "triputa"
2. **Transliteration Standardization**:
   - "cApu" -> "chapu"
   - "miSra" -> "misra"
   - "khaNDa" -> "khanda"
   - "tiSra" -> "tisra"
3. **Alias Mapping**:
   - "Desadi" -> "Adi" (Context dependent, but often mapped)
   - "Madhyadi" -> "Adi"

#### 4.3.2 Specific Logic for Composers (New Findings)

**Observation**: Database contains duplicates like "Dikshitar"/"Muthuswami Dikshitar" and "Syama Sastri"/"Syama Sastry".

**Composer Normalization Rules**:
1. **Canonical Mapping**:
   - "dikshitar" -> "Muthuswami Dikshitar"
   - "muthuswami dikshitar" -> "Muthuswami Dikshitar"
   - "thyagaraja" -> "Tyagaraja"
   - "tyagaraja" -> "Tyagaraja"
   - "syama sastri" -> "Syama Sastri"
   - "syama sastry" -> "Syama Sastri"
   - "shyama sastri" -> "Syama Sastri"
2. **Suffix Standardization**:
   - "sastry" -> "sastri"
   - "shastri" -> "sastri"

**Success Criteria**:
- ✅ "Kalyaani" and "Kalyani" resolve to the same Raga entity.
- ✅ "Kedaara Gaula" and "Kedara Gaula" resolve to the same Raga entity.
- ✅ >95% reduction in duplicate Raga creation.
- ✅ Confidence scores assigned to all resolutions.

---

### 4.4 Phase 4: Review Workflow Integration (Week 4)

**Objective**: Integrate with existing review workflow and enhance UI

**Deliverables**:

1. **Enhanced Import Review UI**
   - Batch import status dashboard
   - Filter by composer, raga, confidence score
   - Bulk approval for high-confidence imports
   - Side-by-side comparison with existing krithis

2. **Auto-approval Rules**
```kotlin
data class AutoApprovalRules(
    val minConfidenceScore: Double = 0.95,
    val requireComposerMatch: Boolean = true,
    val requireRagaMatch: Boolean = true,
    val allowAutoCreateEntities: Boolean = false
)
```

3. **Batch Operations**
```kotlin
POST /v1/admin/imports/batch/{id}/approve-all
POST /v1/admin/imports/batch/{id}/reject-all
POST /v1/admin/imports/batch/{id}/bulk-review
```

**Implementation Steps**:

1. Enhance existing import review UI with batch context
2. Add auto-approval logic for high-confidence imports
3. Implement bulk review operations
4. Add batch statistics dashboard
5. Create import quality report

**Success Criteria**:
- ✅ Batch imports visible in review UI
- ✅ Auto-approval working for high-confidence imports
- ✅ Bulk operations functional
- ✅ Quality metrics displayed

---

## 5. Data Flow

### 5.1 Complete Import Flow

```
1. CSV File Upload/Selection
   ↓
2. CSV Parsing & Validation
   ├─ Parse entries
   ├─ Validate URL syntax only
   ├─ If manifest parse fails or zero valid rows -> mark batch FAILED and stop
   └─ Generate validation report
   ↓
3. Batch Creation
   ├─ Create import_batch record
   └─ Initialize progress tracking
   ↓
4. Batch Scraping (Parallel with rate limiting)
   ├─ For each URL:
   │  ├─ Scrape with WebScrapingService
   │  ├─ Extract metadata (Gemini AI)
   │  ├─ Handle errors/retries
   │  └─ Update progress
   └─ Collect all scraped metadata
   ↓
5. Entity Resolution
   ├─ For each scraped entry:
   │  ├─ Resolve composer (from CSV context + scraped)
   │  ├─ Resolve raga (scraped; CSV raga optional for validation only)
   │  ├─ Resolve deity (from scraped)
   │  ├─ Resolve temple (from scraped)
   │  └─ Assign confidence scores
   └─ Flag ambiguous resolutions
   ↓
6. De-duplication
   ├─ Check against existing imported_krithis
   ├─ Check against existing krithis
   ├─ Check within batch
   └─ Generate duplicate matches
   ↓
7. Data Cleansing
   ├─ Normalize text
   ├─ Clean whitespace
   ├─ Fix encoding issues
   └─ Validate structure
   ↓
8. Staging
   ├─ Create imported_krithi records
   ├─ Store raw + resolved data
   ├─ Set status = PENDING
   └─ Link to batch
   ↓
9. Quality Scoring
   ├─ Calculate completeness score
   ├─ Calculate resolution confidence
   ├─ Calculate source quality
   └─ Assign quality tier
   ↓
10. Review Queue
    ├─ High confidence → Auto-approve (optional)
    ├─ Medium confidence → Review queue
    └─ Low confidence → Detailed review
    ↓
11. Canonicalization (via existing reviewImport)
    ├─ Create Krithi entity
    ├─ Create lyric variants
    ├─ Create sections
    └─ Link entities
```

### 5.2 Error Handling

**Scraping Errors**:
- Network failures → Retry with exponential backoff
- 404/403 errors → Mark as failed, log URL
- Timeout → Retry up to 3 times
- Invalid HTML → Log and skip
- If scrape fails after preview, discard CSV-seeded metadata and require a new batch

**Entity Resolution Errors**:
- Ambiguous matches → Flag for manual review
- No match found → Flag for manual creation
- Low confidence → Flag for review

**De-duplication Errors**:
- False positives → Manual review
- False negatives → Accept (will be caught in review)

---

## 6. Database Schema Enhancements

### 6.1 Import Batch Tracking

```sql
CREATE TABLE import_batch (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  source_file TEXT NOT NULL, -- CSV filename
  composer_context TEXT, -- Implicit composer from filename
  total_entries INT NOT NULL,
  processed_entries INT NOT NULL DEFAULT 0,
  successful_scrapes INT NOT NULL DEFAULT 0,
  failed_scrapes INT NOT NULL DEFAULT 0,
  successful_imports INT NOT NULL DEFAULT 0,
  failed_imports INT NOT NULL DEFAULT 0,
  status VARCHAR(50) NOT NULL, -- pending, processing, completed, failed, partial
  started_at TIMESTAMPTZ,
  completed_at TIMESTAMPTZ,
  error_summary JSONB,
  created_at TIMESTAMPTZ NOT NULL DEFAULT timezone('UTC', now())
);

CREATE INDEX idx_import_batch_status ON import_batch(status);
CREATE INDEX idx_import_batch_source_file ON import_batch(source_file);
```

### 6.2 Enhanced Imported Krithis

```sql
-- Add columns to existing imported_krithis table
ALTER TABLE imported_krithis
  ADD COLUMN IF NOT EXISTS import_batch_id UUID REFERENCES import_batch(id),
  ADD COLUMN IF NOT EXISTS csv_row_number INT,
  ADD COLUMN IF NOT EXISTS csv_krithi_name TEXT,
  ADD COLUMN IF NOT EXISTS csv_raga TEXT,
  ADD COLUMN IF NOT EXISTS extraction_confidence DECIMAL(3,2),
  ADD COLUMN IF NOT EXISTS entity_mapping_confidence DECIMAL(3,2),
  ADD COLUMN IF NOT EXISTS duplicate_candidates JSONB,
  ADD COLUMN IF NOT EXISTS quality_score DECIMAL(3,2),
  ADD COLUMN IF NOT EXISTS quality_tier VARCHAR(20), -- excellent, good, fair, poor
  ADD COLUMN IF NOT EXISTS processing_errors JSONB;

CREATE INDEX idx_imported_krithis_batch ON imported_krithis(import_batch_id);
CREATE INDEX idx_imported_krithis_quality ON imported_krithis(quality_tier, quality_score);
```

### 6.3 Entity Resolution Cache

```sql
CREATE TABLE entity_resolution_cache (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  entity_type VARCHAR(50) NOT NULL, -- composer, raga, deity, temple
  raw_name TEXT NOT NULL,
  normalized_name TEXT NOT NULL,
  resolved_entity_id UUID NOT NULL,
  confidence DECIMAL(3,2) NOT NULL,
  resolution_method VARCHAR(50), -- exact, fuzzy, ai_assisted
  created_at TIMESTAMPTZ NOT NULL DEFAULT timezone('UTC', now()),
  UNIQUE(entity_type, normalized_name)
);

CREATE INDEX idx_entity_cache_type_name ON entity_resolution_cache(entity_type, normalized_name);
```

---

## 7. API Design

### 7.1 CSV Import Endpoints

```kotlin
// Upload and validate CSV
POST /v1/admin/imports/csv/validate
Request: {
  "filePath": "database/for_import/Thyagaraja-Krithi-For-Import.csv",
  "composerContext": "Thyagaraja" // Optional, inferred from filename
}
Response: {
  "totalEntries": 690,
  "validUrls": 685,
  "brokenUrls": 5,
  "duplicates": 2,
  "validationReport": [...]
}

// Process CSV import
POST /v1/admin/imports/csv/process
Request: {
  "filePath": "database/for_import/Thyagaraja-Krithi-For-Import.csv",
  "composerContext": "Thyagaraja",
  "options": {
    "rateLimitPerSecond": 2,
    "maxConcurrency": 3,
    "maxRetries": 3,
    "autoApproveThreshold": 0.95,
    "skipBrokenUrls": true
  }
}
Response: {
  "batchId": "uuid",
  "status": "processing",
  "totalEntries": 690
}

// Get batch status
GET /v1/admin/imports/batches/{batchId}
Response: {
  "id": "uuid",
  "sourceFile": "Thyagaraja-Krithi-For-Import.csv",
  "status": "processing",
  "totalEntries": 690,
  "processedEntries": 450,
  "successfulScrapes": 445,
  "failedScrapes": 5,
  "successfulImports": 440,
  "failedImports": 5,
  "startedAt": "2026-01-20T10:00:00Z",
  "progress": 65.2
}

// List all batches
GET /v1/admin/imports/batches
Query params: status, sourceFile, composerContext
Response: List<BulkImportBatch>

// Cancel batch
POST /v1/admin/imports/batches/{batchId}/cancel
```

### 7.2 Enhanced Import Review Endpoints

```kotlin
// List imports with batch context
GET /v1/admin/imports
Query params: 
  - batchId: Filter by batch
  - qualityTier: excellent, good, fair, poor
  - confidenceMin: Minimum confidence score
  - composer: Filter by composer
  - raga: Filter by raga

// Bulk review
POST /v1/admin/imports/batch/{batchId}/bulk-review
Request: {
  "action": "approve" | "reject" | "flag",
  "importIds": ["uuid1", "uuid2", ...],
  "reviewerNotes": "Optional notes"
}

// Auto-approve high confidence
POST /v1/admin/imports/batch/{batchId}/auto-approve
Request: {
  "minConfidence": 0.95,
  "requireComposerMatch": true,
  "requireRagaMatch": true
}
```

---

## 8. Quality Assurance

### 8.1 Validation Rules

**CSV Validation**:
- ✅ Required columns present (Krithi, Hyperlink). Raga optional if provided.
- ✅ No empty rows
- ✅ URLs are valid HTTP/HTTPS
- ✅ URL validation is syntax-only (no HEAD/GET)

**Scraping Validation**:
- ✅ Metadata extraction successful (title, composer, raga)
- ✅ HTML structure parseable
- ✅ No timeout errors

**Entity Resolution Validation**:
- ✅ Composer resolved with confidence >0.85
- ✅ Raga resolved with confidence >0.80
- ✅ Deity/Temple resolved with confidence >0.75 (if present)

**Data Quality Validation**:
- ✅ Title is non-empty
- ✅ At least one section (Pallavi) present
- ✅ Lyrics are not empty
- ✅ No obvious data corruption

### 8.2 Quality Scoring

```kotlin
data class QualityScore(
    val overall: Double, // 0.0 - 1.0
    val completeness: Double, // 40% weight
    val resolutionConfidence: Double, // 30% weight
    val sourceQuality: Double, // 20% weight
    val validationPass: Double, // 10% weight
    val tier: QualityTier
)

enum class QualityTier {
    EXCELLENT, // >= 0.90, auto-approve candidate
    GOOD,      // >= 0.75, quick review
    FAIR,      // >= 0.60, standard review
    POOR       // < 0.60, detailed review
}
```

### 8.3 Testing Strategy

**Unit Tests**:
- CSV parsing with various formats
- URL validation logic
- Name normalization
- Fuzzy matching algorithms
- Entity resolution logic

**Integration Tests**:
- End-to-end CSV import flow
- Batch scraping with rate limiting
- Entity resolution with database
- De-duplication across batches

**Manual Testing**:
- Import 10-20 entries from each CSV
- Verify scraping accuracy
- Verify entity resolution
- Test review workflow
- Test bulk operations

---

## 9. Performance & Data Quality Considerations

### 9.1 Rate Limiting

**Blogspot.com Considerations**:
- Conservative rate: 2 requests/second
- Max concurrency: 3 parallel requests
- Exponential backoff on errors
- Respect robots.txt (if present)

**Estimated Time**:
- 1,240 entries × 0.5 seconds = ~620 seconds = ~10 minutes
- With retries and errors: ~15-20 minutes per CSV file

### 9.2 Data Quality & Normalization Overhead

**Normalization Cost**:
- Aggressive normalization (regex, string manipulation) adds negligible CPU overhead compared to DB IO.
- **Critical**: Doing it *synchronously* during import prevents "pollution" of the canonical Raga database.

**Strategy**:
- **Pre-load Canonical Ragas**: Cache the `id` -> `normalized_name` map of all existing Ragas (small dataset, <5000 entries) in memory during the batch job to avoid N+1 DB lookups.
- **Batch Resolution**: Resolve all unique scraped Raga names once per batch (CSV Raga values are optional and not authoritative).

### 9.3 Caching Strategy

**Entity Resolution Cache**:
- Cache resolved entities (composer, raga) to avoid repeated lookups
- Cache normalized names for faster matching
- TTL: 7 days (entities don't change frequently)

**Scraped Content Cache**:
- Cache scraped HTML/metadata for retry scenarios
- TTL: 24 hours
- Key: URL hash

### 9.3 Batch Processing

**Batch Size**:
- Process all entries from one CSV as single batch
- Track progress incrementally
- Support resume on failure

**Database Optimization**:
- Batch inserts for imported_krithis
- Use transactions for atomicity
- Index on batch_id for fast queries

---

## 10. Risk Assessment & Mitigation

### 10.1 Technical Risks

| Risk | Probability | Impact | Mitigation |
|:---|:---|:---|:---|
| **Broken URLs** | High | Medium | Validate URL syntax before scraping; handle 404/403 during scrape and log for review |
| **Rate Limiting/IP Blocking** | Medium | High | Conservative rate limiting, exponential backoff, user-agent rotation |
| **HTML Structure Changes** | Low | High | Use AI extraction (Gemini) which is more resilient, version scrapers |
| **Entity Resolution Accuracy** | Medium | High | Confidence thresholds, manual review for ambiguous cases, cache resolutions |
| **Duplicate Detection False Positives** | Medium | Medium | Multi-level detection, manual review for uncertain matches |
| **Performance at Scale** | Low | Medium | Batch processing, caching, database optimization |

### 10.2 Data Quality Risks

| Risk | Probability | Impact | Mitigation |
|:---|:---|:---|:---|
| **Incomplete Metadata** | High | Medium | Accept incompleteness, flag for manual completion |
| **Incorrect Entity Mappings** | Medium | High | Confidence scoring, manual review, audit trail |
| **Transliteration Variations** | High | Medium | Normalization algorithms, fuzzy matching |
| **Missing Lyrics/Sections** | Medium | Medium | Flag for manual review, accept partial data |

### 10.3 Operational Risks

| Risk | Probability | Impact | Mitigation |
|:---|:---|:---|:---|
| **Long Import Times** | High | Low | Background processing, progress tracking, resume capability |
| **Manual Review Bottleneck** | High | Medium | Auto-approval for high confidence, prioritize review queue |
| **Storage Growth** | Low | Low | Archive old batches, cleanup rejected imports |

---

## 11. Success Metrics

### 11.1 Import Volume

- **Target**: Import 1,200+ Krithis from all 3 CSV files within 1 month
- **Throughput**: Process 1 CSV file per day (with review)
- **Success Rate**: >90% successful scraping, >85% successful import

### 11.2 Data Quality

- **Entity Resolution**: >85% automatic resolution, >95% with manual review
- **Duplicate Detection**: >90% true positive rate, <5% false positive rate
- **Completeness**: >80% of imports have all required fields
- **Quality Distribution**: 
  - Excellent: 30%+
  - Good: 40%+
  - Fair: 20%+
  - Poor: <10%

### 11.3 Operational Efficiency

- **Review Time**: <5 minutes per krithi on average
- **Auto-approval Rate**: 30%+ of imports auto-approved
- **Error Recovery**: <5% manual intervention required

---

## 12. Implementation Checklist

### Phase 1: CSV Parsing & Validation
- [ ] Create `CsvParser` service
- [ ] Create `UrlValidator` service
- [ ] Add CSV import API endpoints
- [ ] Implement CSV file reading from `/database/for_import/`
- [ ] Create validation report generation
- [ ] Add unit tests for CSV parsing
- [ ] Test with all 3 CSV files

### Phase 2: Batch Scraping
- [ ] Extend `WebScrapingService` with retry logic
- [ ] Implement `RateLimiter` service
- [ ] Create `BatchScrapingService`
- [ ] Add `import_batch` table migration
- [ ] Implement progress tracking
- [ ] Add resume capability
- [ ] Test batch scraping with 10-20 URLs

### Phase 3: Entity Resolution & De-duplication
- [ ] Create `NameNormalizationService`
- [ ] Create `FuzzyMatchingService`
- [ ] Create `EntityResolutionService`
- [ ] Add PostgreSQL trigram indexes
- [ ] Create `DeduplicationService`
- [ ] Implement confidence scoring
- [ ] Add entity resolution cache
- [ ] Test entity resolution with known variations

### Phase 4: Review Workflow Integration
- [ ] Enhance import review UI with batch context
- [ ] Add quality tier filtering
- [ ] Implement auto-approval logic
- [ ] Add bulk review operations
- [ ] Create batch statistics dashboard
- [ ] Add import quality report
- [ ] Test end-to-end workflow

---

## 13. Recommendations

### 13.1 Immediate Actions (Week 1)

1. **Start with CSV Parsing**: Build CSV parser and validator first
2. **Validate URL Syntax**: Run syntax-only validation on all 3 CSV files (no HEAD/GET)
3. **Test with Small Batch**: Import 10-20 entries to validate end-to-end flow
4. **Set Up Monitoring**: Add logging and progress tracking from day one

### 13.2 Medium-Term (Weeks 2-4)

1. **Implement Batch Scraping**: Build robust batch scraping with rate limiting
2. **Entity Resolution**: Focus on composer and raga resolution (most critical)
3. **De-duplication**: Implement multi-level duplicate detection
4. **Review Workflow**: Enhance UI for efficient batch review

### 13.3 Long-Term (Months 2-3)

1. **Optimize Performance**: Cache, batch operations, database optimization
2. **Improve Accuracy**: Refine entity resolution algorithms based on learnings
3. **Automation**: Increase auto-approval threshold as confidence grows
4. **Documentation**: Create runbooks and troubleshooting guides

---

## 14. Conclusion

This strategy provides a comprehensive plan for bulk importing Krithis from CSV files. The phased approach balances speed of implementation with quality assurance, leveraging existing infrastructure while adding necessary enhancements.

**Key Success Factors**:
1. ✅ Leverage existing `WebScrapingService` and `ImportService`
2. ✅ Implement robust entity resolution for composer and raga
3. ✅ Multi-level de-duplication to avoid canonical duplicates
4. ✅ Quality scoring to prioritize review workflow
5. ✅ Batch processing with progress tracking and resume capability
6. ✅ Conservative rate limiting to avoid IP blocking

**Expected Outcomes**:
- 1,200+ Krithis imported within 1 month
- 85%+ entity resolution accuracy
- 90%+ duplicate detection accuracy
- 30%+ auto-approval rate for high-quality imports

The implementation can begin immediately with Phase 1 (CSV parsing), building incrementally toward a production-ready bulk import system.

---

## 15. References

- [Krithi Bulk Import Capability Analysis](../archive/krithi-bulk-import-capability-analysis-goose.md)
- [Import Pipeline Technical Implementation Guide](../02-implementation/technical-implementation-guide.md)
- [Koog Evaluation](../archive/koog-evaluation-for-import-pipeline-goose.md)
- [Krithi Import Orchestration](../archive/krithi-import-orchestration-comprehensive-analysis-claude.md)
- [Database Schema](../../../../04-database/schema.md)
- Existing Import Infrastructure: `ImportService`, `WebScrapingService`, `ImportRepository`
