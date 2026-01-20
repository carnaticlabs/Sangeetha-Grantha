# Google Gemini & AI Integration Opportunities

> **Status**: Planned | **Version**: 1.0 | **Last Updated**: 2026-01-09
> **Owners**: Platform Team, Product Team

**Related Documents**
- [Admin Web Prd](../01-requirements/admin-web/prd.md)
- [Architecture](../02-architecture/backend-system-design.md)
- [Schema](../04-database/schema.md)
- [Api Contract](../03-api/api-contract.md)

# Google Gemini & AI Integration Opportunities for Sangita Grantha

## Executive Summary

This document identifies strategic opportunities to leverage **Google Gemini AI** and recent Google AI capabilities to enhance the Sangita Grantha platform. The analysis covers automatic transliteration, intelligent data extraction from external sources, content enhancement, and quality assurance workflows.

**Key Opportunities:**
1. **Automatic Transliteration** - Convert kritis between scripts (Devanagari, Tamil, Telugu, Kannada, Malayalam, Latin) with high accuracy
2. **Batch Web Scraping & Extraction** - Automate fetching and parsing kritis from sources like shivkumar.org/music/
3. **Metadata Extraction** - Automatically extract composer, raga, tala, deity, temple from unstructured text
4. **Content Validation** - AI-assisted quality checks for lyrics, notation, and metadata consistency
5. **Search Enhancement** - Semantic search capabilities beyond keyword matching
6. **Section Detection** - Automatic identification of pallavi, anupallavi, charanam sections in raw lyrics

---

## 1. Current State Analysis

### 1.1 Existing Infrastructure

**Database Schema:**
- `krithi_lyric_variants` table supports multiple language/script combinations
- `transliteration_scheme` field exists but is manually entered
- `imported_krithis` table stores raw scraped data awaiting review
- Section-based lyric storage via `krithi_sections` and `krithi_lyric_sections`

**Backend Services:**
- `ImportService` handles bulk import submissions
- `KrithiService` manages search and CRUD operations
- Import pipeline supports raw data ingestion with manual review workflow

**Frontend Admin Console:**
- `KrithiEditor.tsx` provides manual lyric variant entry
- Transliteration scheme dropdown exists (IAST, ISO-15919, ITRANS, etc.) but no automation
- Import review interface for mapping raw data to canonical entities

**Current Limitations:**
- No automated transliteration between scripts
- Manual data entry for lyric variants
- Web scraping requires custom scripts (not integrated)
- Metadata extraction is manual
- No AI-assisted validation or quality checks

---

## 2. AI Integration Opportunities

### 2.1 Automatic Transliteration Service

#### 2.1.1 Use Case

**Problem:** Editors must manually enter lyric variants in different scripts. A kriti in Devanagari needs separate manual entry for Tamil, Telugu, Kannada, Malayalam, and Latin transliteration.

**Solution:** Leverage Gemini's multilingual capabilities to automatically transliterate kritis between Indian language scripts with high accuracy.

#### 2.1.2 Technical Implementation

**New Backend Service: `TransliterationService`**

```kotlin
// modules/backend/api/src/main/kotlin/com/sangita/grantha/backend/api/services/TransliterationService.kt

class TransliterationService(
    private val geminiClient: GeminiApiClient,
    private val dal: SangitaDal
) {
    /**
     * Transliterate lyrics from source script to target script
     * @param sourceText Original lyrics text
     * @param sourceScript Current script (DEVANAGARI, TAMIL, TELUGU, etc.)
     * @param targetScript Desired output script
     * @param sourceLanguage Language code (SA, TA, TE, KN, ML)
     * @return Transliterated text
     */
    suspend fun transliterate(
        sourceText: String,
        sourceScript: ScriptCode,
        targetScript: ScriptCode,
        sourceLanguage: LanguageCode
    ): Result<String, TransliterationError>
    
    /**
     * Batch transliterate multiple sections
     */
    suspend fun transliterateSections(
        sections: List<SectionText>,
        targetScript: ScriptCode
    ): Map<String, String> // sectionId -> transliterated text
    
    /**
     * Generate multiple script variants from a single source
     */
    suspend fun generateAllScriptVariants(
        sourceVariant: KrithiLyricVariantDto,
        targetScripts: List<ScriptCode>
    ): List<GeneratedVariant>

    /**
     * Transliterate Sahitya within Notation rows, preserving alignment
     */
    suspend fun transliterateNotation(
        notation: NotationDto,
        targetScript: ScriptCode
    ): NotationDto
}
```

**Gemini API Integration:**

Use Gemini 2.0 Flash or Gemini 1.5 Pro with structured output for transliteration:

```kotlin
// Prompt engineering for transliteration
val prompt = """
You are an expert in Indian language transliteration for Carnatic music compositions.

Translate the following ${sourceScript} text to ${targetScript} script, preserving:
1. Exact meaning and pronunciation
2. Musical notation markers (if any)
3. Section boundaries (pallavi, anupallavi, charanam)
4. Line breaks and formatting

Source text (${sourceScript}):
${sourceText}

Provide ONLY the transliterated text in ${targetScript}, no explanations.
""".trimIndent()
```

**Integration Points:**

1. **Admin Console - One-Click Transliteration**
   - Add "Generate Variants" button in `KrithiEditor.tsx` Lyrics tab
   - Select target scripts (checkboxes: Tamil, Telugu, Kannada, Malayalam, Latin)
   - Show preview before saving
   - Save as new lyric variants with `isPrimary = false`

2. **Batch Processing Endpoint**
   - `POST /v1/admin/krithis/{id}/transliterate`
   - Accepts list of target scripts
   - Returns generated variants for review
   - Can be scheduled for bulk processing

3. **Import Pipeline Enhancement**
   - Auto-transliterate imported kritis to all supported scripts
   - Store in `imported_krithis.parsed_payload` as JSON
   - Reviewers can accept/reject AI-generated variants

#### 2.1.3 Benefits

- **Time Savings:** Reduces manual entry from hours to seconds per kriti
- **Consistency:** Ensures uniform transliteration across all variants
- **Coverage:** Enables multi-script support for entire catalog
- **Quality:** Gemini's training on Indian languages provides high accuracy

#### 2.1.4 Implementation Priority: **HIGH**

---

### 2.2 Intelligent Web Scraping & Data Extraction

#### 2.2.1 Use Case

**Problem:** Kritis exist on external sites (shivkumar.org/music/, karnatik.com) but require manual copying, parsing, and data entry.

**Solution:** Automated batch scraping with AI-powered extraction of structured data from HTML pages.

#### 2.2.2 Technical Implementation

**New Backend Service: `WebScrapingService`**

```kotlin
// modules/backend/api/src/main/kotlin/com/sangita/grantha/backend/api/services/WebScrapingService.kt

class WebScrapingService(
    private val httpClient: HttpClient,
    private val geminiClient: GeminiApiClient,
    private val dal: SangitaDal
) {
    /**
     * Scrape and extract kriti data from a URL
     */
    suspend fun scrapeKritiFromUrl(
        url: String,
        sourceId: UUID
    ): Result<ExtractedKritiData, ScrapingError>
    
    /**
     * Batch scrape multiple URLs from a source
     */
    suspend fun batchScrape(
        source: ImportSourceDto,
        urls: List<String>,
        concurrency: Int = 5
    ): List<ImportedKrithiDto>
    
    /**
     * Extract structured data from HTML using Gemini
     */
    private suspend fun extractStructuredData(
        htmlContent: String,
        url: String
    ): ExtractedKritiData
}
```

**Gemini-Powered Extraction:**

Use Gemini's multimodal capabilities to parse HTML and extract structured data:

```kotlin
val extractionPrompt = """
Extract Carnatic kriti information from the following HTML content.

Extract and return as JSON:
{
  "title": "Kriti title",
  "composer": "Composer name",
  "raga": "Raga name",
  "tala": "Tala name",
  "deity": "Deity name (if mentioned)",
  "temple": "Temple/kshetram (if mentioned)",
  "language": "Primary language code (sa/ta/te/kn/ml)",
  "lyrics": {
    "pallavi": "Pallavi text",
    "anupallavi": "Anupallavi text",
    "charanam": ["Charanam 1", "Charanam 2", ...]
  },
  "notation": "Swara notation if present",
  "metadata": {
    "source_url": "${url}",
    "extraction_confidence": 0.0-1.0
  }
}

HTML Content:
${htmlContent}
""".trimIndent()
```

**Batch Processing Workflow:**

1. **Scheduled Job** (using Ktor's coroutine scheduler or external cron)
   ```kotlin
   // Run daily at 2 AM
   suspend fun scheduledScrape() {
       val sources = dal.imports.getAllActiveSources()
       for (source in sources) {
           val urls = discoverKritiUrls(source.baseUrl)
           val imported = batchScrape(source, urls)
           // Store in imported_krithis table
       }
   }
   ```

2. **Admin Console Integration**
   - New page: "Import Sources" with "Scrape Now" button
   - Progress indicator for batch operations
   - Review queue shows extracted data with confidence scores

3. **URL Discovery**
   - Use Gemini to identify kriti listing pages
   - Extract individual kriti URLs from index pages
   - Handle pagination automatically

#### 2.2.3 Source-Specific Handlers

**Shivkumar.org Handler:**

```kotlin
class ShivkumarScraper : SourceSpecificScraper {
    override suspend fun discoverUrls(baseUrl: String): List<String> {
        // Navigate to https://www.shivkumar.org/music/
        // Extract all kriti detail page URLs
    }
    
    override suspend fun extractFromPage(html: String): ExtractedKritiData {
        // Use Gemini to parse shivkumar.org's specific HTML structure
        // Handle their notation format, section markers, etc.
    }
}
```

#### 2.2.4 Benefits

- **Automation:** Eliminates manual copy-paste workflow
- **Scale:** Process hundreds of kritis per hour
- **Accuracy:** AI extraction handles varied HTML structures
- **Provenance:** Maintains source URLs and extraction metadata

#### 2.2.5 Implementation Priority: **HIGH**

---

### 2.3 Metadata Extraction & Normalization

#### 2.3.1 Use Case

**Problem:** Imported raw data has unstructured metadata (e.g., "Composer: Tyagaraja, Raga: Kalyani") that needs parsing and mapping to canonical entities.

**Solution:** Use Gemini to extract and normalize metadata, then suggest mappings to existing reference data.

#### 2.3.2 Technical Implementation

**New Service: `MetadataExtractionService`**

```kotlin
class MetadataExtractionService(
    private val geminiClient: GeminiApiClient,
    private val dal: SangitaDal
) {
    /**
     * Extract structured metadata from raw text
     */
    suspend fun extractMetadata(
        rawText: String,
        context: ExtractionContext
    ): ExtractedMetadata
    
    /**
     * Suggest canonical entity mappings
     */
    suspend fun suggestMappings(
        extracted: ExtractedMetadata
    ): MappingSuggestions {
        // Query reference data tables
        // Use Gemini to match "Tyagaraja" -> Composer UUID
        // Handle name variations, aliases
    }
    
    /**
     * Validate and enrich existing metadata
     */
    suspend fun validateMetadata(
        krithi: KrithiDto
    ): ValidationReport
}
```

**Gemini Prompt for Extraction:**

```kotlin
val metadataPrompt = """
Extract Carnatic music metadata from the following text.

Identify:
1. Composer name (handle variations: "Tyagaraja", "Saint Tyagaraja", "Tyagayya")
2. Raga name (handle variations: "Kalyani", "Mecha Kalyani", "Yaman Kalyani")
3. Tala name (handle variations: "Adi", "Adi Tala", "Chaturasra Jati Triputa")
4. Deity name (if mentioned)
5. Temple/kshetram (if mentioned)
6. Language (Sanskrit, Tamil, Telugu, Kannada, Malayalam)
7. Musical form (Krithi, Varnam, Swarajathi)

Text:
${rawText}

Return as JSON with confidence scores (0.0-1.0) for each field.
""".trimIndent()
```

**Auto-Mapping Logic:**

```kotlin
suspend fun suggestComposerMapping(extractedName: String): ComposerMapping? {
    // 1. Exact match on normalized name
    val exact = dal.composers.findByNameNormalized(normalize(extractedName))
    if (exact != null) return ComposerMapping(exact.id, confidence = 1.0)
    
    // 2. Use Gemini for fuzzy matching with fallback to Trigram/Levenshtein
    val candidates = dal.composers.searchSimilar(extractedName, algorithm = SearchAlgorithm.TRIGRAM)
    val prompt = "Match '${extractedName}' to one of: ${candidates.map { it.name }}"
    val match = geminiClient.matchEntity(prompt, candidates)
    return match
}
```

#### 2.3.3 Admin Console Integration

- **Import Review Page Enhancement:**
  - Show extracted metadata with confidence scores
  - Display suggested mappings (click to accept)
  - Highlight fields requiring manual review (low confidence)

- **Bulk Processing:**
  - "Auto-map All" button for high-confidence extractions
  - Batch accept/reject workflow

#### 2.3.4 Benefits

- **Efficiency:** Reduces manual mapping time by 70-80%
- **Accuracy:** Handles name variations and aliases
- **Consistency:** Ensures uniform metadata across imports

#### 2.3.5 Implementation Priority: **MEDIUM**

---

### 2.4 Section Detection & Structure Analysis

#### 2.4.1 Use Case

**Problem:** Raw lyrics come as continuous text without section markers. Editors must manually identify pallavi, anupallavi, charanam boundaries.

**Solution:** Use Gemini to automatically detect section boundaries and structure.

#### 2.4.2 Technical Implementation

**New Service: `SectionDetectionService`**

```kotlin
class SectionDetectionService(
    private val geminiClient: GeminiApiClient
) {
    /**
     * Detect sections in raw lyrics text
     */
    suspend fun detectSections(
        rawLyrics: String,
        language: LanguageCode,
        musicalForm: MusicalForm
    ): DetectedSections {
        val prompt = """
Analyze this Carnatic composition and identify section boundaries.

Musical Form: ${musicalForm}
Language: ${language}

Identify:
- Pallavi (opening section)
- Anupallavi (second section, if present)
- Charanam(s) (subsequent sections)

Return JSON:
{
  "sections": [
    {
      "type": "PALLAVI",
      "text": "section text",
      "startLine": 1,
      "endLine": 4,
      "confidence": 0.95
    },
    ...
  ]
}

Raw Lyrics:
${rawLyrics}
""".trimIndent()
        
        return geminiClient.generateStructured(prompt, DetectedSections.serializer())
    }
}
```

**Integration:**

- **Import Pipeline:** Auto-detect sections for imported kritis
- **Admin Console:** "Detect Sections" button in Lyrics tab
- **Validation:** Compare AI-detected sections with manual entry

#### 2.4.3 Benefits

- **Speed:** Instant section detection vs. manual analysis
- **Consistency:** Standardized section identification
- **Quality:** Catches missing or mislabeled sections

#### 2.4.4 Implementation Priority: **MEDIUM**

---

### 2.5 Content Validation & Quality Assurance

#### 2.5.1 Use Case

**Problem:** Editors may introduce errors: typos in lyrics, incorrect raga/tala assignments, missing required fields.

**Solution:** AI-powered validation checks before publishing.

#### 2.5.2 Technical Implementation

**New Service: `ValidationService`**

```kotlin
class ValidationService(
    private val geminiClient: GeminiApiClient,
    private val dal: SangitaDal
) {
    /**
     * Comprehensive validation of a kriti
     */
    suspend fun validateKrithi(
        krithi: KrithiDto,
        variants: List<KrithiLyricVariantDto>
    ): ValidationReport {
        val issues = mutableListOf<ValidationIssue>()
        
        // 1. Check lyric consistency across variants
        issues.addAll(validateLyricConsistency(variants))
        
        // 2. Validate raga-lyrics alignment
        issues.addAll(validateRagaAlignment(krithi, variants))
        
        // 3. Check for common typos
        issues.addAll(checkTypos(variants))
        
        // 4. Validate section structure
        issues.addAll(validateSections(krithi, variants))
        
        return ValidationReport(issues)
    }
    
    private suspend fun validateRagaAlignment(
        krithi: KrithiDto,
        variants: List<KrithiLyricVariantDto>
    ): List<ValidationIssue> {
        val raga = dal.ragas.findById(krithi.primaryRagaId)
        val prompt = """
Check if the following lyrics are appropriate for Raga ${raga?.name}.

Raga Characteristics:
- Arohanam: ${raga?.arohanam}
- Avarohanam: ${raga?.avarohanam}

Lyrics:
${variants.firstOrNull()?.lyrics}

Identify any words or phrases that seem inconsistent with the raga's scale.
""".trimIndent()
        
        // Gemini analysis
    }
}
```

**Admin Console Integration:**

- **Pre-Publish Validation:**
  - "Validate" button in KrithiEditor
  - Show validation report with severity levels
  - Block publishing if critical issues exist

- **Batch Validation:**
  - Validate all draft kritis
  - Generate quality dashboard

#### 2.5.3 Benefits

- **Quality:** Catches errors before publication
- **Confidence:** Editors can trust AI validation
- **Efficiency:** Automated checks vs. manual review

#### 2.5.4 Implementation Priority: **MEDIUM**

---

### 2.6 Semantic Search Enhancement

#### 2.6.1 Use Case

**Problem:** Current search is keyword-based. Users searching for "devotional song to Ganesha" won't find kritis tagged with "Ganapati" or "Vigneshwara".

**Solution:** Use Gemini embeddings for semantic search.

#### 2.6.2 Technical Implementation

**Vector Search Integration:**

```kotlin
// Add embedding generation
class EmbeddingService(
    private val geminiClient: GeminiApiClient
) {
    suspend fun generateEmbedding(text: String): FloatArray {
        // Use Gemini's embedding API
    }
    
    suspend fun generateKrithiEmbedding(krithi: KrithiDto): FloatArray {
        val combined = """
        Title: ${krithi.title}
        Composer: ${composer.name}
        Raga: ${raga.name}
        Tala: ${tala.name}
        Deity: ${deity?.name}
        Summary: ${krithi.sahityaSummary}
        """.trimIndent()
        return generateEmbedding(combined)
    }
}

// Store embeddings in PostgreSQL with pgvector extension
// Add vector similarity search
```

**Search Enhancement:**

- Hybrid search: Combine keyword (trigram) + semantic (vector)
- "Find similar kritis" feature
- Thematic discovery beyond tags

#### 2.6.5 Implementation Priority: **LOW** (Future Enhancement)

---

## 3. Implementation Roadmap

### Phase 1: Core Transliteration (Weeks 1-4)

**Deliverables:**
- `TransliterationService` with Gemini API integration
- Admin console "Generate Variants" button
- Batch transliteration endpoint
- Unit tests and integration tests

**Success Criteria:**
- Transliterate 100 kritis across 5 scripts with >95% accuracy
- UI integration complete
- Documentation updated

### Phase 2: Web Scraping & Extraction (Weeks 5-8)

**Deliverables:**
- `WebScrapingService` with HTTP client
- Gemini-powered HTML parsing
- Shivkumar.org specific handler
- Batch processing scheduler
- Import review UI enhancements

**Success Criteria:**
- Successfully scrape 50+ kritis from shivkumar.org
- >80% extraction accuracy
- Review workflow functional

### Phase 3: Metadata Extraction (Weeks 9-12)

**Deliverables:**
- `MetadataExtractionService`
- Auto-mapping suggestions
- Import review UI with confidence scores
- Validation service foundation

**Success Criteria:**
- Extract metadata with >85% accuracy
- Auto-map 70%+ of reference entities
- Manual review time reduced by 60%

### Phase 4: Quality & Validation (Weeks 13-16)

**Deliverables:**
- `ValidationService` with multiple check types
- Section detection service
- Pre-publish validation in admin console
- Quality dashboard

**Success Criteria:**
- Catch 90%+ of common errors
- Section detection >90% accuracy
- Validation workflow integrated

---

## 4. Technical Architecture

### 4.1 Gemini API Integration

**Client Library:**

```kotlin
// modules/backend/api/src/main/kotlin/com/sangita/grantha/backend/api/clients/GeminiApiClient.kt

class GeminiApiClient(
    private val apiKey: String,
    private val httpClient: HttpClient
) {
    private val baseUrl = "https://generativelanguage.googleapis.com/v1beta"
    
    suspend fun generateContent(
        prompt: String,
        model: String = "gemini-2.0-flash-exp"
    ): String {
        // HTTP call to Gemini API
    }
    
    suspend fun generateStructured<T>(
        prompt: String,
        serializer: KSerializer<T>
    ): T {
        // Use Gemini's structured output (JSON mode)
    }
    
    suspend fun generateEmbedding(text: String): FloatArray {
        // Use Gemini's embedding API
    }
}
```

**Configuration:**

```toml
# config/application.local.toml
[ai]
gemini_auth_token = "${SG_GEMINI_AUTH_TOKEN}"
gemini_model = "gemini-2.0-flash-exp"
gemini_embedding_model = "text-embedding-004"
```

### 4.2 Error Handling & Retries

```kotlin
suspend fun <T> withRetry(
    maxRetries: Int = 3,
    delay: Long = 1000,
    block: suspend () -> T
): T {
    repeat(maxRetries) { attempt ->
        try {
            return block()
        } catch (e: Exception) {
            if (attempt == maxRetries - 1) throw e
            delay(delay * (attempt + 1))
        }
    }
    error("Should not reach here")
}
```

### 4.3 Rate Limiting

- Implement token bucket for Gemini API calls
- Batch requests when possible
- Queue system for large batch operations

### 4.4 Caching

- Cache transliterations (source script + target script + text hash)
- Cache extracted metadata for similar raw texts
- Use Redis or in-memory cache

---

## 5. API Integration Details

### 5.1 New Admin Endpoints

```kotlin
// Transliteration
POST /v1/admin/krithis/{id}/transliterate
Body: { "targetScripts": ["TAMIL", "TELUGU", "KANNADA"] }
Response: { "variants": [KrithiLyricVariantDto] }

// Web Scraping
POST /v1/admin/imports/scrape
Body: { "sourceId": "uuid", "urls": ["url1", "url2"] }
Response: { "imported": [ImportedKrithiDto] }

// Metadata Extraction
POST /v1/admin/imports/{id}/extract-metadata
Response: { "extracted": ExtractedMetadata, "suggestions": MappingSuggestions }

// Validation
POST /v1/admin/krithis/{id}/validate
Response: ValidationReport

// Section Detection
POST /v1/admin/krithis/{id}/detect-sections
Body: { "rawLyrics": "text", "language": "SA" }
Response: { "sections": [DetectedSection] }
```

### 5.2 Request/Response DTOs

```kotlin
@Serializable
data class TransliterationRequest(
    val targetScripts: List<ScriptCodeDto>
)

@Serializable
data class ExtractedMetadata(
    val composer: String?,
    val raga: String?,
    val tala: String?,
    val deity: String?,
    val temple: String?,
    val language: String?,
    val confidence: Map<String, Double>
)

@Serializable
data class ValidationReport(
    val issues: List<ValidationIssue>,
    val score: Double // 0.0-1.0
)
```

---

## 6. Cost & Performance Considerations

### 6.1 Gemini API Pricing (as of 2025-01)

**Gemini 2.0 Flash:**
- Input: $0.075 per 1M tokens
- Output: $0.30 per 1M tokens

**Gemini 1.5 Pro:**
- Input: $1.25 per 1M tokens
- Output: $5.00 per 1M tokens

**Estimated Costs:**

- **Transliteration:** ~500 tokens per kriti × 5 scripts = 2,500 tokens
  - Cost per kriti: ~$0.001 (Flash) or $0.003 (Pro)
  - 1,000 kritis: ~$1-3

- **Web Scraping:** ~2,000 tokens per page (HTML + extraction)
  - Cost per kriti: ~$0.002 (Flash)
  - 1,000 kritis: ~$2

- **Metadata Extraction:** ~300 tokens per kriti
  - Cost per kriti: ~$0.0002 (Flash)
  - 1,000 kritis: ~$0.20

**Total for 1,000 kritis (all features):** ~$3-5

### 6.2 Performance Targets

- **Transliteration:** < 2 seconds per kriti
- **Web Scraping:** < 5 seconds per page
- **Metadata Extraction:** < 1 second per kriti
- **Batch Operations:** Process 100 kritis in < 5 minutes

### 6.3 Optimization Strategies

1. **Batch API Calls:** Group multiple requests when possible
2. **Caching:** Cache results to avoid duplicate API calls
3. **Model Selection:** Use Flash for speed, Pro for accuracy when needed
4. **Async Processing:** Queue long-running operations
5. **Rate Limiting:** Respect API limits, implement backoff

---

## 7. Security & Privacy

### 7.1 API Key Management

- Store Gemini API key in environment variables
- Never commit keys to repository
- Use secret management (AWS Secrets Manager, GCP Secret Manager)

### 7.2 Data Privacy

- Lyrics may contain sensitive content
- Ensure Gemini API usage complies with data privacy policies
- Consider on-premise alternatives for sensitive data (future)

### 7.3 Audit Logging

- Log all AI operations in `audit_log`
- Track: operation type, input hash, output hash, cost, timestamp
- Enable cost tracking and alerts

---

## 8. Testing Strategy

### 8.1 Unit Tests

- Mock Gemini API responses
- Test error handling and retries
- Validate prompt engineering

### 8.2 Integration Tests

- Test with real Gemini API (sandbox key)
- Validate transliteration accuracy on known kritis
- Test web scraping on sample pages

### 8.3 Accuracy Validation

- **Transliteration:** Compare AI output with expert-validated translations
- **Extraction:** Measure precision/recall on test dataset
- **Section Detection:** Compare with manually labeled sections

---

## 9. Monitoring & Observability

### 9.1 Metrics

- API call count and latency
- Cost per operation
- Accuracy scores (where measurable)
- Error rates by operation type

### 9.2 Alerts

- API key expiration warnings
- Unusual cost spikes
- High error rates
- Rate limit approaching

### 9.3 Dashboards

- Cost tracking dashboard
- Operation success rates
- Processing throughput

---

## 10. Future Enhancements

### 10.1 Advanced Features

- **Notation Generation:** AI-assisted swara notation from lyrics
- **Raga Analysis:** Automatic raga identification from notation
- **Translation:** Translate kritis to English for international audience
- **Audio Analysis:** Extract metadata from audio recordings (multimodal)

### 10.2 Model Fine-Tuning

- Fine-tune Gemini on Carnatic music domain data
- Improve accuracy for music-specific terminology
- Custom models for transliteration

### 10.3 On-Premise Alternatives

- Evaluate open-source LLMs (Llama, Mistral) for sensitive data
- Hybrid approach: public data → Gemini, sensitive → on-premise

---

## 11. Dependencies & Setup

### 11.1 Required Dependencies

```kotlin
// build.gradle.kts
dependencies {
    // HTTP client for Gemini API
    implementation("io.ktor:ktor-client-core:$ktorVersion")
    implementation("io.ktor:ktor-client-cio:$ktorVersion")
    implementation("io.ktor:ktor-client-content-negotiation:$ktorVersion")
    implementation("io.ktor:ktor-serialization-kotlinx-json:$ktorVersion")
    
    // For vector search (future)
    // implementation("com.pgvector:pgvector:0.1.4")
}
```

### 11.2 Environment Variables

```bash
# .env or application.local.toml
SG_GEMINI_AUTH_TOKEN=your_token_here
SG_GEMINI_MODEL=gemini-2.0-flash-exp
SG_GEMINI_ENABLED=true
```

### 11.3 Configuration

```kotlin
// Application configuration
data class AiConfig(
    val geminiApiKey: String,
    val geminiModel: String = "gemini-2.0-flash-exp",
    val enabled: Boolean = true,
    val rateLimitPerMinute: Int = 60
)
```

---

## 12. Conclusion

Integrating Google Gemini AI into Sangita Grantha will significantly enhance productivity, accuracy, and scalability. The highest-impact opportunities are:

1. **Automatic Transliteration** - Immediate time savings for editors
2. **Web Scraping & Extraction** - Automate data ingestion pipeline
3. **Metadata Extraction** - Reduce manual mapping effort

These features align with the platform's goals of establishing Sangita Grantha as the authoritative system of record for Carnatic compositions while maintaining editorial quality and provenance.

**Next Steps:**
1. Obtain Gemini API key and set up development environment
2. Implement Phase 1 (Transliteration) as proof of concept
3. Measure accuracy and cost, iterate on prompts
4. Roll out to production with monitoring
5. Proceed with subsequent phases based on results

---

## Appendix A: Example Prompts

### Transliteration Prompt

```
You are an expert in Indian language transliteration for Carnatic music.

Translate the following Devanagari text to Tamil script, preserving:
- Exact pronunciation and meaning
- Musical notation markers (if present)
- Section boundaries (pallavi, anupallavi, charanam)
- Line breaks

Source (Devanagari):
वातापि गणपतिं भजेहं

Provide ONLY the transliterated text in Tamil, no explanations.
```

### Metadata Extraction Prompt

```
Extract Carnatic music metadata from this text:

"Vatapi Ganapatim - Composed by Muthuswami Dikshitar in Raga Hamsadhwani, Tala Adi. Dedicated to Lord Ganesha of Vatapi (Badami)."

Return JSON:
{
  "title": "Vatapi Ganapatim",
  "composer": "Muthuswami Dikshitar",
  "raga": "Hamsadhwani",
  "tala": "Adi",
  "deity": "Ganesha",
  "temple": "Vatapi/Badami",
  "confidence": {
    "title": 1.0,
    "composer": 1.0,
    "raga": 0.95,
    "tala": 0.9
  }
}
```

---

## Appendix B: Reference Links

- [Google Gemini API Documentation](https://ai.google.dev/docs)
- [Gemini 2.0 Flash Model](https://ai.google.dev/models/gemini)
- [Gemini Embeddings API](https://ai.google.dev/docs/embeddings)
- [Kotlin HTTP Client](https://ktor.io/docs/http-client.html)
- [Shivkumar.org Music Archive](https://www.shivkumar.org/music/)

---

**Document Status:** Draft for Review  
Last Updated: 2026-01-20
**Next Review:** After Phase 1 implementation

---

## 13. Comprehensive Implementation Checklist

This checklist tracks the granular tasks required to implement the AI features.

### Phase 1: Core Transliteration (Weeks 1-4)
- [ ] **Infrastructure Setup**
    - [ ] Create GCP Project and obtain Gemini API Key
    - [ ] Configure `GEMINI_AUTH_TOKEN` in local and production environments
    - [ ] Create `GeminiApiClient` module with `ktor-client`
    - [ ] Implement `rate_limiter` middleware (Token Bucket) using Redis
- [ ] **TransliterationService**
    - [ ] Define `ScriptCode` enum (Devanagari, Tamil, Telugu, Kannada, Malayalam, Latin)
    - [ ] Implement `transliterate(text, source, target)` method
    - [ ] Create prompt templates for all script pairs
    - [ ] Unit test with sample text for all languages
- [ ] **DB & API**
    - [ ] Review `krithi_lyric_variants` schema (ensure `script` column matches enum)
    - [ ] Implement `POST /v1/admin/krithis/{id}/transliterate` endpoint
    - [ ] Create DTOs: `TransliterationRequest`, `TransliterationResponse`
- [ ] **Admin UI (`sangita-admin-web`)**
    - [ ] Update `src/pages/KrithiEditor.tsx` (Lyrics Tab) to include "Generate Variants" button
    - [ ] Create `src/components/TransliterationModal.tsx` to select target scripts
    - [ ] Implement preview of generated variants in the modal
    - [ ] Implement Save/Apply callback to update `lyricVariants` state
    - [ ] Add visual indicator for "AI Generated" content
- [ ] **Verification**
    - [ ] Manual verification of 50 generated kritis
    - [ ] Validate alignment of notation transliteration (if implemented in this phase)

### Phase 2: Web Scraping (Weeks 5-8)
- [ ] **Scraping Infrastructure**
    - [ ] Create `WebScrapingService`
    - [ ] Implement generic HTML fetcher with retry logic
    - [ ] Create `ImportSource` configuration in DB (if not exists)
- [ ] **Shivkumar.org Integration**
    - [ ] Analyze invalid/malformatted HTML patterns on source site
    - [ ] Implement `ShivkumarScraper` strategy
    - [ ] Build Gemini prompt for extracting JSON from specific HTML structure
- [ ] **Import Pipeline UI (`sangita-admin-web`)**
    - [ ] Create `src/pages/ImportList.tsx` to manage import sources and history
    - [ ] Create `src/pages/ImportWizard.tsx` for the scraping/import workflow
    - [ ] Update `src/App.tsx` and `src/components/Sidebar.tsx` to enable `/imports` route
    - [ ] Create `ImportReview` UI component to display scraped JSON with `parsed_payload`
    - [ ] Add "Scrape Now" triggers in the UI
- [ ] **Verification**
    - [ ] Scrape 20 sample pages, verify JSON structure
    - [ ] Check for data loss (missing charanams, truncations)

### Phase 3: Metadata Extraction (Weeks 9-12)
- [ ] **Metadata Service**
    - [ ] Implement `MetadataExtractionService`
    - [ ] Build prompts for extracting Composer, Raga, Tala, Deity
    - [ ] Implement `searchSimilar(name)` using Trigram/Levenshtein in DAL
- [ ] **Auto-Mapping Logic**
    - [ ] Connect extraction to `ComposerRepository`, `RagaRepository`
    - [ ] Implement fuzzy matching logic: Extract -> Search Candidates -> Gemini Match
- [ ] **UI Enhancements**
    - [ ] Add "Auto-Map" button in Import Review
    - [ ] specific visual indicators for low-confidence matches (< 0.8)

### Phase 4: Quality & Validation (Weeks 13-16)
- [ ] **Validation Service**
    - [ ] Implement `ValidationService` shell
    - [ ] Add specific validators: `LyricConsistencyValidator`, `RagaAlignmentValidator`
- [ ] **Integration (`sangita-admin-web`)**
    - [ ] Hook validation into `KrithiService.publish()`
    - [ ] Add `ValidationStatus` component to `KrithiEditor` header
    - [ ] Display inline validation warnings in `KrithiEditor` (Lyrics and Metadata tabs)
    - [ ] Block "Publish" action if critical validation errors exist

