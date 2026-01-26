| Metadata | Value |
|:---|:---|
| **Status** | Active |
| **Version** | 1.0.0 |
| **Last Updated** | 2026-01-26 |
| **Author** | Sangeetha Grantha Team |

# Krithi Import Orchestration – Comprehensive Analysis & Strategic Recommendations


---



---

## Executive Summary

This document provides a comprehensive analysis of building a production-grade Krithi import capability for Sangeetha Grantha, incorporating data from multiple web sources with rich metadata associations (Krithi, Raga, Deity, Temple/Kshetra). The analysis builds upon existing evaluations and provides strategic recommendations tailored to the application's architecture and technology stack.

### Key Findings

1. **Architecture Alignment**: The application's Kotlin-based architecture (Ktor + Exposed + PostgreSQL) provides a solid foundation for building a custom import pipeline that aligns with existing patterns.

2. **Data Quality Challenge**: The primary challenge is not technical orchestration but **data quality, cleansing, and entity resolution** across heterogeneous sources with varying structures and completeness.

3. **Phased Approach Essential**: Given the complexity of musicological data and the need for expert validation, a phased approach starting with a single high-quality source is strongly recommended.

4. **Koog Position**: While Koog.ai offers sophisticated orchestration capabilities, the import pipeline's requirements are better served by custom Kotlin services leveraging coroutines, with potential Koog integration for specific AI-intensive stages.

5. **Human-in-Loop Critical**: Automated import must feed into a robust moderation workflow; musical accuracy and scholarly integrity cannot be fully automated.

---

## 1. Application Context & Constraints

### 1.1 Technology Stack Alignment

**Current Stack:**
- Backend: Kotlin + Ktor + Exposed ORM
- Database: PostgreSQL 15+ with full-text search
- Mobile: Kotlin Multiplatform (KMM)
- Admin: React + TypeScript
- Build: Gradle
- Migrations: Rust CLI (`sangita-cli`)

**Import Pipeline Technology Constraints:**
- Should leverage Kotlin for consistency with backend
- Must integrate with Exposed ORM and existing repository patterns
- Should use PostgreSQL's advanced features (trigrams, full-text search, JSONB)
- Must align with editorial workflow (`workflow_state_enum`)
- Should support the existing `ImportedKrithi` staging model

### 1.2 Domain Model Integration

**Critical Entities for Import:**

```kotlin
// Primary import staging
ImportedKrithi {
    id: UUID
    importSourceId: UUID
    rawTitle: String
    rawLyrics: String?
    rawComposer: String?
    rawRaga: String?
    rawTala: String?
    rawDeity: String?
    rawTemple: String?
    rawLanguage: String?
    parsedPayload: JSONB
    importStatus: ImportStatusEnum  // pending, in_review, mapped, rejected
    mappedKrithiId: UUID?
    reviewerUserId: UUID?
    reviewedAt: Instant?
}

// Target canonical entities
Krithi {
    id: UUID
    title: String
    incipit: String
    composerId: UUID
    primaryRagaId: UUID
    talaId: UUID?
    deityId: UUID?
    templeId: UUID?
    primaryLanguage: LanguageCodeEnum
    musicalForm: MusicalFormEnum
    isRagamalika: Boolean
    workflowState: WorkflowStateEnum
}
```

**Key Relationships:**
- Krithi → Composer (many-to-one)
- Krithi → Raga (many-to-one primary, many-to-many via `KrithiRaga` for ragamalika)
- Krithi → Deity (optional many-to-one)
- Krithi → Temple (optional many-to-one)
- Krithi → KrithiLyricVariant (one-to-many for language/script variants)
- Krithi → KrithiSection → KrithiLyricSection (structured lyrics)

---

## 2. Data Source Analysis & Challenges

### 2.1 Source Characterization

| Source | Structure | Metadata Quality | Primary Challenge | Estimated Volume |
|:---|:---|:---|:---|:---|
| **karnatik.com/lyrics** | Semi-structured HTML | High (Name, Composer, Raga, Tala, Language) | Transliteration variations | 1,000-3,000 krithis |
| **guru-guha.blogspot.com** (Dikshitar list) | List-based | Medium (Name, Raga, Composer implicit) | Composer implicit (Dikshitar), parsing list format | 300-700 krithis |
| **syamakrishnavaibhavam.blogspot.com** | List-based | Low-Medium (Name, Raga implied) | Deity implicit (Krishna), incomplete metadata | 400-1,000 krithis |
| **thyagaraja-vaibhavam.blogspot.com** | List-based | Medium (Composer implicit: Thyagaraja) | Large volume, varying completeness | 500-2,000 krithis |
| **templenet.com** | Semi-structured directory | High for temples | Linking temple → krithi requires inference | 3,000+ temples |

### 2.2 Data Quality Challenges

**Challenge 1: Name Normalization**
```
Example Variations:
- "Endaro Mahanubhavulu" / "Entaro Mahanubhavulu" / "Entāro Mahānubhāvulu"
- "Raghuvamsa Sudha" / "Raghuvamsha Sudha" / "Raghuvamśa Sudhā"

Solution:
- Phonetic normalization using `name_normalized` field
- Unicode normalization (NFC)
- Soundex/Metaphone for Indian languages
- Maintain variants in JSONB `parsedPayload`
```

**Challenge 2: Entity Resolution**

The most critical technical challenge is resolving raw text to canonical entities:

```kotlin
// Input from source
raw = {
    composer: "Tyagaraja"           // Could be: Tyagaraja, Thyagaraja, Tyāgarāja
    raga: "Mayamalava Gowla"        // Could be: Mayamalavagaula, Mayamalavagowla
    deity: "Rama"                   // Could be: Rama, Raghuvira, Raghunatha, Sitapati
    temple: "Tirupati"              // Could be: Tirupati, Tirumala, Sri Venkateswara Temple
}

// Must resolve to
canonical = {
    composerId: UUID("...")         // Must map to existing composer
    primaryRagaId: UUID("...")      // Must map to existing raga
    deityId: UUID("...")            // May need to create or map
    templeId: UUID("...")           // May need temple name lookup
}
```

**Complexity Factors:**
- Multiple transliteration schemes (IAST, ISO-15919, simplified)
- Regional variations (Tamil vs Telugu vs Sanskrit names)
- Aliases and epithets (especially for deities)
- Historical name changes (temples)

**Challenge 3: Deity-Temple Association**

Not all krithis explicitly mention temples. Association strategies:

1. **Explicit Mention**: Krithi lyrics reference a temple name
   - Example: "Venkatesam" → Tirupati Venkateswara Temple

2. **Deity-Based Inference**: Krithi is about specific deity form
   - Example: Krithi to Venkateswara → likely Tirupati
   - **Caveat**: One deity may have hundreds of temples

3. **Composer Geography**: Map composer's life location to temples
   - Example: Tyagaraja → Tiruvaiyaru region temples
   - **Caveat**: Requires comprehensive composer biography data

4. **Manual Curation**: Expert review for ambiguous cases
   - **Recommended**: Flag for expert review rather than auto-assign

**Challenge 4: Lyric Section Extraction**

Carnatic krithis have structured sections:
- Pallavi (refrain)
- Anupallavi (second section)
- Charanam (verses, often multiple)
- Optional: Chittaswaram, Madhyama-kala sahitya

**Extraction Challenge**: Sources may not clearly demarcate sections.

```
Example Raw Text:
"Endaro mahanubhavulu andariki vandanamulu
Endaro mahanubhavulu
[continues as single block]"

Must Parse To:
Pallavi: "Endaro mahanubhavulu"
Anupallavi: [may be combined with pallavi]
Charanam 1: [separate verse]
```

**Solution**: Combination of pattern matching and AI-assisted segmentation.

---

## 3. Architectural Options Analysis

### 3.1 Option A: Pure Custom Kotlin Pipeline

**Architecture:**
```
┌─────────────────────────────────────────────────────┐
│                Admin Import API                      │
│              (Ktor REST Endpoints)                   │
└──────────────────────┬──────────────────────────────┘
                       │
┌──────────────────────▼──────────────────────────────┐
│          ImportOrchestrationService                  │
│        (Kotlin Coroutines + Flow)                    │
└─┬────────┬─────────┬─────────┬──────────┬───────────┘
  │        │         │         │          │
  ▼        ▼         ▼         ▼          ▼
┌────┐  ┌────┐  ┌────────┐  ┌──────┐  ┌─────────┐
│Web │  │HTML│  │ Entity │  │ Data │  │De-dup   │
│Scr-│  │Pars│  │Resolu- │  │Clean-│  │& Valida-│
│aper│  │ing │  │tion    │  │sing  │  │tion     │
└────┘  └────┘  └────────┘  └──────┘  └─────────┘
  │        │         │         │          │
  └────────┴─────────┴─────────┴──────────┘
                     │
         ┌───────────▼────────────┐
         │  ImportedKrithi (DB)    │
         │  (staging table)        │
         └───────────┬─────────────┘
                     │
         ┌───────────▼─────────────┐
         │  Review Workflow UI      │
         │  (React Admin)           │
         └───────────┬──────────────┘
                     │
         ┌───────────▼──────────────┐
         │  Canonicalization        │
         │  (Create Krithi entity)  │
         └──────────────────────────┘
```

**Implementation Pattern:**

```kotlin
// Main orchestration service
class ImportOrchestrationService(
    private val webScrapingService: WebScrapingService,
    private val extractionService: MetadataExtractionService,
    private val entityResolutionService: EntityResolutionService,
    private val cleansingService: DataCleansingService,
    private val deduplicationService: DeduplicationService,
    private val validationService: ValidationService,
    private val importRepository: ImportRepository
) {
    suspend fun executeImport(sourceConfig: ImportSourceConfig): Flow<ImportProgress> = flow {
        emit(ImportProgress.Started)

        // Stage 1: Discovery & Scraping
        val urls = webScrapingService.discoverUrls(sourceConfig)
        emit(ImportProgress.DiscoveryComplete(urls.size))

        // Stage 2: Parallel scraping with rate limiting
        val scraped = urls.asFlow()
            .buffer(capacity = 10)
            .map { url ->
                retry(3) { webScrapingService.scrape(url) }
            }
            .toList()

        emit(ImportProgress.ScrapingComplete(scraped.size))

        // Stage 3: Metadata extraction (AI-assisted)
        val extracted = scraped.map { html ->
            extractionService.extract(html, sourceConfig.extractionRules)
        }

        // Stage 4: Entity resolution
        val resolved = extracted.map { metadata ->
            entityResolutionService.resolve(metadata)
        }

        // Stage 5: Data cleansing
        val cleansed = resolved.map { data ->
            cleansingService.cleanse(data)
        }

        // Stage 6: De-duplication
        val deduped = deduplicationService.detectDuplicates(cleansed)

        // Stage 7: Validation
        val validated = deduped.map { data ->
            validationService.validate(data)
        }

        // Stage 8: Save to staging
        importRepository.batchInsert(validated)

        emit(ImportProgress.Complete(validated.size))
    }
}

// Supporting services follow similar patterns
class EntityResolutionService(
    private val composerRepo: ComposerRepository,
    private val ragaRepo: RagaRepository,
    private val deityRepo: DeityRepository,
    private val templeRepo: TempleRepository
) {
    suspend fun resolve(metadata: RawMetadata): ResolvedMetadata {
        return ResolvedMetadata(
            composerId = resolveComposer(metadata.composerName),
            ragaId = resolveRaga(metadata.ragaName),
            deityId = metadata.deityName?.let { resolveDeity(it) },
            templeId = metadata.templeName?.let { resolveTemple(it) }
        )
    }

    private suspend fun resolveComposer(name: String): UUID {
        // Try exact match first
        composerRepo.findByName(name)?.let { return it.id }

        // Try normalized match
        val normalized = normalize(name)
        composerRepo.findByNormalizedName(normalized)?.let { return it.id }

        // Try fuzzy match with trigram similarity
        val candidates = composerRepo.findSimilar(name, threshold = 0.85)
        if (candidates.size == 1) return candidates.first().id

        // Multiple matches or no match - flag for review
        throw EntityResolutionException.AmbiguousComposer(name, candidates)
    }

    // Similar patterns for raga, deity, temple
}
```

**Advantages:**
- ✅ Full control over logic and performance
- ✅ Native integration with Kotlin/Exposed codebase
- ✅ Leverage coroutines for concurrency
- ✅ Direct database access via existing repositories
- ✅ Type-safe end-to-end
- ✅ Easy to test and debug
- ✅ No external orchestration dependencies
- ✅ Team familiarity with Kotlin

**Disadvantages:**
- ❌ More code to write and maintain
- ❌ Need to implement own retry/circuit breaker logic
- ❌ Observability requires custom instrumentation
- ❌ Workflow visualization not built-in
- ❌ State persistence for long-running imports manual

**Cost Estimate:**
- Development: 6-8 weeks (1 senior Kotlin developer)
- Infrastructure: Minimal (uses existing PostgreSQL, hosting)
- Maintenance: Medium (ongoing feature additions)

**Recommendation Fit**: ⭐⭐⭐⭐⭐ (5/5)
- Best fit for initial implementation
- Aligns with tech stack
- Maintains architectural consistency

---

### 3.2 Option B: Koog.ai Integration

**Architecture:**
```
┌─────────────────────────────────────────────────────┐
│            Admin Import Trigger API                  │
└──────────────────────┬──────────────────────────────┘
                       │
┌──────────────────────▼──────────────────────────────┐
│           Koog Agent Orchestrator                    │
│     (Graph-based workflow, LLM integration)          │
└─┬────────┬─────────┬─────────┬──────────┬───────────┘
  │        │         │         │          │
  ▼        ▼         ▼         ▼          ▼
┌────┐  ┌────┐  ┌────────┐  ┌──────┐  ┌─────────┐
│Scr-│  │Extr│  │ Entity │  │Clean-│  │Valida-  │
│ape │  │act │  │Resolve │  │sing  │  │tion     │
│Node│  │Node│  │Node    │  │Node  │  │Node     │
│    │  │(LLM│  │(Tools) │  │      │  │         │
└────┘  └────┘  └────────┘  └──────┘  └─────────┘
  │        │         │         │          │
  └────────┴─────────┴─────────┴──────────┘
                     │
         ┌───────────▼────────────┐
         │  Kotlin Services        │
         │  (Called as Tools)      │
         └────────────────────────┘
```

**Koog Agent Configuration:**

```kotlin
val importAgent = agent {
    name = "krithi-import-agent"
    description = "Orchestrates krithi import from web sources"

    llm = GeminiProvider(
        model = "gemini-2.0-flash-exp",
        apiKey = config.geminiApiKey
    )

    tools = listOf(
        scrapeUrlTool,
        extractMetadataTool,
        resolveComposerTool,
        resolveRagaTool,
        resolveDeityTool,
        validateKrithiTool
    )

    graph {
        val discover = node("discover") { discoverUrls(source) }
        val scrape = node("scrape") { scrapeUrl(url) }
        val extract = node("extract") {
            // LLM-powered extraction
            extractWithAI(html)
        }
        val resolve = node("resolve") {
            // Tool calling for entity resolution
            resolveEntities(metadata)
        }
        val validate = node("validate") { validateData(resolved) }
        val stage = node("stage") { saveToStaging(validated) }

        discover -> scrape -> extract -> resolve -> validate -> stage
    }

    retryPolicy {
        maxRetries = 3
        backoffStrategy = ExponentialBackoff(
            initialDelay = 1.seconds,
            maxDelay = 30.seconds
        )
    }

    tracing {
        exporter = OpenTelemetryExporter()
    }
}

// Tool definitions
val resolveComposerTool = tool("resolve_composer") {
    description = "Resolve composer name to canonical UUID"
    parameter<String>("name") {
        description = "Raw composer name from source"
    }

    execute { name ->
        entityResolutionService.resolveComposer(name)
    }
}
```

**Advantages:**
- ✅ Built-in workflow orchestration
- ✅ LLM integration for extraction stage
- ✅ Automatic retry and error handling
- ✅ OpenTelemetry tracing out-of-box
- ✅ Workflow visualization
- ✅ State persistence for long workflows
- ✅ Easy to add conditional branching

**Disadvantages:**
- ❌ Additional dependency and learning curve
- ❌ Framework overhead and potential performance impact
- ❌ Some architectural mismatch (agentic vs deterministic)
- ❌ Tool calling adds latency to entity resolution
- ❌ Debugging graph execution less intuitive
- ❌ Most pipeline stages are deterministic, not LLM-driven

**Cost Estimate:**
- Development: 4-6 weeks (including learning Koog)
- Infrastructure: Same as custom
- Maintenance: Lower for workflow changes, higher for framework updates

**Recommendation Fit**: ⭐⭐⭐ (3/5)
- Good for LLM-heavy extraction phase
- Over-engineered for deterministic stages
- Better as enhancement to custom pipeline for specific stages

**Strategic Use of Koog:**
Rather than using Koog for the entire pipeline, use it **selectively** for AI-intensive stages:

```kotlin
// Hybrid approach: Custom orchestration + Koog for extraction
suspend fun executeImport(source: ImportSource): ImportResult {
    val urls = webScrapingService.discover(source)
    val scraped = urls.map { scrape(it) }

    // Use Koog only for AI-powered extraction
    val extracted = scraped.map { html ->
        koogExtractionAgent.extract(html)  // LLM-powered
    }

    // Back to custom Kotlin for deterministic stages
    val resolved = extracted.map { entityResolutionService.resolve(it) }
    val validated = resolved.map { validationService.validate(it) }

    return importRepository.batchInsert(validated)
}
```

---

### 3.3 Option C: Apache Airflow Integration

**Architecture:**
```
┌─────────────────────────────────────────────────────┐
│              Airflow Web UI                          │
│        (Monitoring, Scheduling, Logs)                │
└──────────────────────┬──────────────────────────────┘
                       │
┌──────────────────────▼──────────────────────────────┐
│            Airflow DAG Orchestrator                  │
│         (Python-based workflow definition)           │
└─┬────────┬─────────┬─────────┬──────────┬───────────┘
  │        │         │         │          │
  ▼        ▼         ▼         ▼          ▼
┌────┐  ┌────┐  ┌────────┐  ┌──────┐  ┌─────────┐
│HTTP│  │HTTP│  │  HTTP  │  │ HTTP │  │  HTTP   │
│Call│  │Call│  │  Call  │  │ Call │  │  Call   │
└─┬──┘  └─┬──┘  └────┬───┘  └──┬───┘  └────┬────┘
  │        │         │         │          │
  └────────┴─────────┴─────────┴──────────┘
                     │
         ┌───────────▼────────────┐
         │  Ktor Import APIs       │
         │  (Kotlin Services)      │
         └────────────────────────┘
```

**DAG Definition (Python):**

```python
from airflow import DAG
from airflow.operators.http import SimpleHttpOperator
from datetime import datetime, timedelta

default_args = {
    'owner': 'sangita-grantha',
    'depends_on_past': False,
    'start_date': datetime(2026, 1, 1),
    'email_on_failure': True,
    'retries': 3,
    'retry_delay': timedelta(minutes=5),
}

with DAG(
    'krithi_import_pipeline',
    default_args=default_args,
    schedule_interval='@weekly',  # Run weekly
    catchup=False
) as dag:

    # Stage 1: Trigger discovery
    discover = SimpleHttpOperator(
        task_id='discover_urls',
        http_conn_id='sangita_api',
        endpoint='/api/admin/import/discover',
        method='POST',
        data=json.dumps({'source': 'karnatik_com'}),
        headers={"Content-Type": "application/json"},
    )

    # Stage 2: Scrape URLs
    scrape = SimpleHttpOperator(
        task_id='scrape_content',
        http_conn_id='sangita_api',
        endpoint='/api/admin/import/scrape',
        method='POST',
    )

    # Stage 3: Extract metadata
    extract = SimpleHttpOperator(
        task_id='extract_metadata',
        http_conn_id='sangita_api',
        endpoint='/api/admin/import/extract',
        method='POST',
    )

    # Stage 4: Resolve entities
    resolve = SimpleHttpOperator(
        task_id='resolve_entities',
        http_conn_id='sangita_api',
        endpoint='/api/admin/import/resolve',
        method='POST',
    )

    # Stage 5: De-duplicate
    dedupe = SimpleHttpOperator(
        task_id='deduplicate',
        http_conn_id='sangita_api',
        endpoint='/api/admin/import/dedupe',
        method='POST',
    )

    # Stage 6: Validate
    validate = SimpleHttpOperator(
        task_id='validate',
        http_conn_id='sangita_api',
        endpoint='/api/admin/import/validate',
        method='POST',
    )

    # Define dependencies
    discover >> scrape >> extract >> resolve >> dedupe >> validate
```

**Advantages:**
- ✅ Mature orchestration platform
- ✅ Rich UI for monitoring and logs
- ✅ Robust scheduling (cron, external triggers)
- ✅ Built-in retry and alerting
- ✅ Large community and plugins

**Disadvantages:**
- ❌ Requires Python expertise (team is Kotlin-focused)
- ❌ Additional infrastructure (Airflow server, workers, DB)
- ❌ HTTP call overhead for each stage
- ❌ Deployment complexity
- ❌ Stateful orchestration separate from application
- ❌ Overkill for initial small-scale imports

**Cost Estimate:**
- Development: 4-5 weeks
- Infrastructure: High (Airflow cluster, monitoring)
- Maintenance: Medium-High (Python DAGs, Airflow upgrades)

**Recommendation Fit**: ⭐⭐ (2/5)
- Over-engineered for current needs
- Better for high-frequency, complex data pipelines at scale
- Consider only if planning many different import pipelines

---

### 3.4 Recommended Hybrid Approach

**Phase 1: Custom Kotlin Orchestration (MVP)**
- Timeline: 6-8 weeks
- Scope: Single source (karnatik.com), 100-500 krithis
- Architecture: Pure Kotlin services + coroutines

**Phase 2: Enhanced with Selective Koog** (if extraction complexity warrants)
- Timeline: 2-3 weeks
- Scope: Add Koog for metadata extraction stage only
- Keep custom Kotlin for all other stages

**Phase 3: Scale & Optimize**
- Add remaining sources incrementally
- Refine entity resolution algorithms
- Optimize de-duplication
- Enhance review workflow

**Airflow Consideration**: Only if import frequency increases significantly (daily/hourly) and multiple parallel pipelines needed.

---

## 4. Entity Resolution Deep Dive

### 4.1 Composer Resolution

**Challenge**: Multiple composers with similar names, transliteration variations.

**Strategy:**

```kotlin
class ComposerResolutionService(
    private val composerRepo: ComposerRepository
) {
    suspend fun resolve(rawName: String): ComposerResolutionResult {
        // Step 1: Exact match
        composerRepo.findByName(rawName)?.let {
            return ComposerResolutionResult.Resolved(it.id, confidence = 1.0)
        }

        // Step 2: Normalized match
        val normalized = normalizeIndianName(rawName)
        composerRepo.findByNormalizedName(normalized)?.let {
            return ComposerResolutionResult.Resolved(it.id, confidence = 0.95)
        }

        // Step 3: Fuzzy match using PostgreSQL trigrams
        val candidates = composerRepo.findSimilar(rawName, threshold = 0.75)

        return when {
            candidates.isEmpty() ->
                ComposerResolutionResult.NotFound(rawName)

            candidates.size == 1 && candidates.first().similarity > 0.85 ->
                ComposerResolutionResult.Resolved(
                    candidates.first().id,
                    confidence = candidates.first().similarity
                )

            else ->
                ComposerResolutionResult.Ambiguous(rawName, candidates)
        }
    }

    private fun normalizeIndianName(name: String): String {
        return name
            .lowercase()
            .replace("ā", "a")
            .replace("ī", "i")
            .replace("ū", "u")
            .replace("ṭ", "t")
            .replace("ḍ", "d")
            .replace("ṇ", "n")
            .replace("ś", "s")
            .replace("ṣ", "s")
            // ... more normalizations
            .replace(Regex("\\s+"), " ")
            .trim()
    }
}

// Repository implementation using Exposed
class ComposerRepositoryImpl : ComposerRepository {
    suspend fun findSimilar(name: String, threshold: Double): List<ComposerSimilarity> {
        return transaction {
            // Use PostgreSQL pg_trgm extension
            Composers
                .select {
                    Composers.name.similarity(name) greaterEq threshold
                }
                .orderBy(Composers.name.similarity(name), SortOrder.DESC)
                .limit(5)
                .map { row ->
                    ComposerSimilarity(
                        id = row[Composers.id].value,
                        name = row[Composers.name],
                        similarity = calculateSimilarity(name, row[Composers.name])
                    )
                }
        }
    }
}
```

**Requires PostgreSQL Extension:**
```sql
CREATE EXTENSION IF NOT EXISTS pg_trgm;

CREATE INDEX idx_composers_name_trgm ON composers
USING gin (name gin_trgm_ops);

CREATE INDEX idx_composers_normalized_trgm ON composers
USING gin (name_normalized gin_trgm_ops);
```

### 4.2 Raga Resolution

**Additional Complexity**: Ragas have parent/child relationships (melakarta/janya).

```kotlin
class RagaResolutionService(
    private val ragaRepo: RagaRepository
) {
    suspend fun resolve(rawName: String): RagaResolutionResult {
        // Similar to composer, but also consider:
        // 1. Melakarta number if provided
        // 2. Parent raga relationships
        // 3. Arohanam/Avarohanam if available

        val normalized = normalizeRagaName(rawName)

        // Try exact match first
        ragaRepo.findByName(normalized)?.let {
            return RagaResolutionResult.Resolved(it.id, confidence = 1.0)
        }

        // Try alias/variant lookup
        ragaRepo.findByVariant(normalized)?.let {
            return RagaResolutionResult.Resolved(it.id, confidence = 0.95)
        }

        // Fuzzy match
        val candidates = ragaRepo.findSimilar(normalized, threshold = 0.80)

        return when {
            candidates.isEmpty() ->
                RagaResolutionResult.NotFound(rawName)

            candidates.size == 1 && candidates.first().similarity > 0.90 ->
                RagaResolutionResult.Resolved(
                    candidates.first().id,
                    confidence = candidates.first().similarity
                )

            else ->
                RagaResolutionResult.Ambiguous(rawName, candidates)
        }
    }

    private fun normalizeRagaName(name: String): String {
        return name
            .lowercase()
            .replace("ā", "a")
            .replace("ō", "o")
            // Common variations
            .replace("gaula", "goula")
            .replace("gowla", "goula")
            .trim()
    }
}
```

**Enhancement: AI-Assisted Ambiguity Resolution**

For ambiguous cases, use Gemini to help disambiguate:

```kotlin
suspend fun resolveAmbiguousRaga(
    rawName: String,
    candidates: List<Raga>,
    context: KrithiContext  // Composer, lyrics snippet
): RagaResolutionResult {
    val prompt = buildAmbiguityPrompt(rawName, candidates, context)
    val aiResponse = geminiService.analyze(prompt)

    return aiResponse.bestMatch?.let { matchedId ->
        RagaResolutionResult.Resolved(
            matchedId,
            confidence = aiResponse.confidence,
            method = ResolutionMethod.AI_ASSISTED
        )
    } ?: RagaResolutionResult.RequiresManualReview(rawName, candidates)
}
```

### 4.3 Deity Resolution

**Challenge**: One deity, many names (epithets, regional variations).

**Solution: Synonym Graph**

```kotlin
class DeityResolutionService(
    private val deityRepo: DeityRepository,
    private val deitySynonymGraph: DeitySynonymGraph
) {
    suspend fun resolve(rawName: String): DeityResolutionResult {
        // Step 1: Lookup in synonym graph
        val canonicalName = deitySynonymGraph.getCanonical(rawName)

        // Step 2: Lookup canonical entity
        deityRepo.findByName(canonicalName)?.let {
            return DeityResolutionResult.Resolved(it.id, confidence = 0.95)
        }

        // Step 3: Fuzzy match as fallback
        val candidates = deityRepo.findSimilar(rawName, threshold = 0.80)

        return when {
            candidates.isEmpty() ->
                DeityResolutionResult.NotFound(rawName)

            candidates.size == 1 ->
                DeityResolutionResult.Resolved(
                    candidates.first().id,
                    confidence = candidates.first().similarity
                )

            else ->
                DeityResolutionResult.Ambiguous(rawName, candidates)
        }
    }
}

// Synonym graph (can be loaded from JSONB config)
class DeitySynonymGraph {
    private val synonyms = mapOf(
        "vishnu" to setOf("perumal", "narayana", "hari", "govinda", "madhava", "venkateshwara", "venkateswara"),
        "rama" to setOf("raghuvira", "raghunatha", "dasarathi", "kodanda rama", "sitapati"),
        "krishna" to setOf("gopala", "madhava", "yadava", "devaki nandana", "vaasudeva", "keshava"),
        "shiva" to setOf("shankara", "ishwara", "mahadeva", "hara", "chandrashekara", "parameshwara"),
        "murugan" to setOf("kartikeya", "skanda", "subramanya", "guha", "shanmukha", "kumaraswamy"),
        "ganesha" to setOf("ganapati", "vinayaka", "pillayar", "vighneshwara", "lambodara")
    )

    fun getCanonical(input: String): String {
        val normalized = input.lowercase().trim()

        // Direct canonical match
        if (synonyms.containsKey(normalized)) return normalized

        // Find in synonyms
        synonyms.forEach { (canonical, syns) ->
            if (syns.contains(normalized)) return canonical
        }

        return normalized  // No match, return as-is
    }
}
```

**Requires Data Seeding:**
```sql
-- Seed with comprehensive synonym data
INSERT INTO deities (id, name, name_normalized, description) VALUES
    (gen_random_uuid(), 'Vishnu', 'vishnu', 'Primary deity in Vaishnavism'),
    (gen_random_uuid(), 'Rama', 'rama', 'Avatar of Vishnu'),
    (gen_random_uuid(), 'Krishna', 'krishna', 'Avatar of Vishnu'),
    (gen_random_uuid(), 'Shiva', 'shiva', 'Primary deity in Shaivism'),
    (gen_random_uuid(), 'Murugan', 'murugan', 'Son of Shiva, Subramanya'),
    (gen_random_uuid(), 'Ganesha', 'ganesha', 'Son of Shiva');
```

### 4.4 Temple Resolution

**Challenge**: Temples have multiple names (historical, regional, deity-based).

**Strategy: Use `temple_names` Table**

```kotlin
class TempleResolutionService(
    private val templeRepo: TempleRepository,
    private val templeNameRepo: TempleNameRepository
) {
    suspend fun resolve(rawName: String, deityContext: UUID?): TempleResolutionResult {
        // Step 1: Exact match in temple_names
        templeNameRepo.findByName(rawName)?.let { templeName ->
            return TempleResolutionResult.Resolved(
                templeName.templeId,
                confidence = 0.98
            )
        }

        // Step 2: Normalized match
        val normalized = normalizeTempleName(rawName)
        templeNameRepo.findByNormalizedName(normalized)?.let { templeName ->
            return TempleResolutionResult.Resolved(
                templeName.templeId,
                confidence = 0.95
            )
        }

        // Step 3: Fuzzy match with deity context
        val candidates = if (deityContext != null) {
            // Narrow search to temples with this deity
            templeRepo.findSimilarWithDeity(normalized, deityContext, threshold = 0.75)
        } else {
            templeRepo.findSimilar(normalized, threshold = 0.75)
        }

        return when {
            candidates.isEmpty() ->
                TempleResolutionResult.NotFound(rawName)

            candidates.size == 1 && candidates.first().similarity > 0.85 ->
                TempleResolutionResult.Resolved(
                    candidates.first().id,
                    confidence = candidates.first().similarity
                )

            else ->
                TempleResolutionResult.Ambiguous(rawName, candidates)
        }
    }

    private fun normalizeTempleName(name: String): String {
        return name
            .lowercase()
            .removePrefix("sri ")
            .removePrefix("shri ")
            .removeSuffix(" temple")
            .removeSuffix(" kovil")
            .removeSuffix(" devasthanam")
            .replace(Regex("\\s+"), " ")
            .trim()
    }
}
```

**Temple-Deity Linkage:**

Many krithis don't explicitly mention temples but do mention deities. The system should support:

1. **Explicit Temple Mention**: Krithi references temple by name
   - Store in `krithis.temple_id`

2. **Deity-Only Mention**: Krithi about deity, no specific temple
   - Store `deity_id`, leave `temple_id` NULL
   - Could later suggest temple via manual curation

3. **Ambiguous**: Multiple temples for same deity
   - Flag for expert review

---

## 5. De-duplication Strategy

### 5.1 Multi-Level De-duplication

**Level 1: Exact Hash Match**
```kotlin
class DeduplicationService(
    private val importedKrithiRepo: ImportedKrithiRepository
) {
    suspend fun detectDuplicates(batch: List<ImportedKrithi>): DeduplicationResult {
        // Level 1: Hash-based exact match
        val hashes = batch.map { it.computeHash() }
        val existingHashes = importedKrithiRepo.findByHashes(hashes)

        val exactDuplicates = batch.filter {
            it.computeHash() in existingHashes
        }

        // Remove exact duplicates
        val remaining = batch - exactDuplicates.toSet()

        // Level 2: Fuzzy match
        val fuzzyDuplicates = detectFuzzyDuplicates(remaining)

        // Level 3: Semantic similarity
        val semanticDuplicates = detectSemanticDuplicates(
            remaining - fuzzyDuplicates.toSet()
        )

        return DeduplicationResult(
            exactDuplicates = exactDuplicates,
            fuzzyDuplicates = fuzzyDuplicates,
            semanticDuplicates = semanticDuplicates,
            unique = remaining - fuzzyDuplicates.toSet() - semanticDuplicates.toSet()
        )
    }

    private fun ImportedKrithi.computeHash(): String {
        // Hash on normalized title + composer + raga
        val normalized = "${this.rawTitle.normalize()}_${this.rawComposer?.normalize()}_${this.rawRaga?.normalize()}"
        return normalized.sha256()
    }
}
```

**Level 2: Fuzzy String Matching**
```kotlin
private suspend fun detectFuzzyDuplicates(batch: List<ImportedKrithi>): List<ImportedKrithi> {
    val duplicates = mutableListOf<ImportedKrithi>()

    for (candidate in batch) {
        val similar = importedKrithiRepo.findSimilarByTitle(
            title = candidate.rawTitle,
            threshold = 0.90  // 90% similarity
        )

        if (similar.isNotEmpty()) {
            // Check if composer + raga also match
            val confirmedDupe = similar.any { existing ->
                composerMatch(candidate.rawComposer, existing.rawComposer) &&
                ragaMatch(candidate.rawRaga, existing.rawRaga)
            }

            if (confirmedDupe) {
                duplicates.add(candidate)
            }
        }
    }

    return duplicates
}

private fun composerMatch(a: String?, b: String?): Boolean {
    if (a == null || b == null) return false
    val simil = Levenshtein.ratio(a.normalize(), b.normalize())
    return simil > 0.85
}
```

**Level 3: Semantic Similarity (LLM-Assisted)**

For cases where title/composer/raga are unclear or transliterated differently:

```kotlin
private suspend fun detectSemanticDuplicates(batch: List<ImportedKrithi>): List<ImportedKrithi> {
    val duplicates = mutableListOf<ImportedKrithi>()

    for (candidate in batch) {
        // Use first line of lyrics (pallavi) for semantic comparison
        val pallavi = candidate.extractPallavi()

        if (pallavi != null && pallavi.length > 20) {
            // Search existing krithis by similar incipit
            val semanticallySimilar = krithiRepo.searchByIncipit(pallavi)

            if (semanticallySimilar.isNotEmpty()) {
                // Use LLM to confirm if they're the same krithi
                val isDuplicate = geminiService.compareLyrics(
                    lyric1 = pallavi,
                    lyric2 = semanticallySimilar.first().incipit
                )

                if (isDuplicate.confidence > 0.9) {
                    duplicates.add(candidate)
                }
            }
        }
    }

    return duplicates
}
```

### 5.2 Merge Strategy for Duplicates

When duplicates are detected, determine merge strategy:

```kotlin
sealed class DuplicateAction {
    data class DiscardNew(val reason: String) : DuplicateAction()
    data class MergeData(val existing: UUID, val newData: ImportedKrithi) : DuplicateAction()
    data class FlagForReview(val existingId: UUID, val newId: UUID) : DuplicateAction()
}

fun decideDuplicateAction(
    existing: ImportedKrithi,
    new: ImportedKrithi
): DuplicateAction {
    // If existing is already mapped, discard new
    if (existing.mappedKrithiId != null) {
        return DuplicateAction.DiscardNew("Already mapped to canonical krithi")
    }

    // If new has more complete data, prefer merge
    val existingScore = existing.completenessScore()
    val newScore = new.completenessScore()

    return when {
        newScore > existingScore + 0.2 ->
            DuplicateAction.MergeData(existing.id, new)

        abs(newScore - existingScore) < 0.2 ->
            DuplicateAction.FlagForReview(existing.id, new.id)

        else ->
            DuplicateAction.DiscardNew("Existing record is more complete")
    }
}

private fun ImportedKrithi.completenessScore(): Double {
    var score = 0.0
    if (rawTitle.isNotBlank()) score += 0.2
    if (rawComposer != null) score += 0.15
    if (rawRaga != null) score += 0.15
    if (rawTala != null) score += 0.1
    if (rawDeity != null) score += 0.1
    if (rawTemple != null) score += 0.1
    if (rawLyrics != null && rawLyrics.length > 100) score += 0.2
    return score
}
```

---

## 6. Validation & Quality Checks

### 6.1 Validation Rules

```kotlin
class ValidationService(
    private val composerRepo: ComposerRepository,
    private val ragaRepo: RagaRepository,
    private val talaRepo: TalaRepository
) {
    suspend fun validate(data: ImportedKrithi): ValidationResult {
        val errors = mutableListOf<ValidationError>()
        val warnings = mutableListOf<ValidationWarning>()

        // Required field validation
        if (data.rawTitle.isBlank()) {
            errors.add(ValidationError.MissingTitle)
        }

        // Entity existence validation
        if (data.resolvedComposerId != null) {
            if (!composerRepo.exists(data.resolvedComposerId)) {
                errors.add(ValidationError.InvalidComposer(data.resolvedComposerId))
            }
        } else {
            warnings.add(ValidationWarning.MissingComposer)
        }

        // Musicological validation
        if (data.resolvedRagaId != null) {
            val raga = ragaRepo.findById(data.resolvedRagaId)
            if (raga == null) {
                errors.add(ValidationError.InvalidRaga(data.resolvedRagaId))
            } else if (raga.parentRagaId != null) {
                // Janya raga - ensure it's known
                warnings.add(ValidationWarning.JanyaRaga(raga.name))
            }
        }

        // Deity-temple consistency
        if (data.resolvedTempleId != null && data.resolvedDeityId != null) {
            val temple = templeRepo.findById(data.resolvedTempleId)
            if (temple != null && temple.primaryDeityId != data.resolvedDeityId) {
                warnings.add(
                    ValidationWarning.DeityTempleMismatch(
                        deityId = data.resolvedDeityId,
                        templeId = data.resolvedTempleId
                    )
                )
            }
        }

        // Lyrics section structure (if available)
        if (data.rawLyrics != null) {
            val sectionCheck = validateLyricStructure(data.rawLyrics)
            warnings.addAll(sectionCheck.warnings)
        }

        return ValidationResult(
            isValid = errors.isEmpty(),
            errors = errors,
            warnings = warnings
        )
    }

    private fun validateLyricStructure(lyrics: String): SectionValidation {
        val warnings = mutableListOf<ValidationWarning>()

        // Check if sections can be identified
        if (!lyrics.contains("pallavi", ignoreCase = true) &&
            !lyrics.contains("anupallavi", ignoreCase = true)) {
            warnings.add(ValidationWarning.UnclearLyricStructure)
        }

        // Check for minimum content
        if (lyrics.split("\n").size < 3) {
            warnings.add(ValidationWarning.IncompleteLyrics)
        }

        return SectionValidation(warnings)
    }
}

sealed class ValidationError {
    object MissingTitle : ValidationError()
    data class InvalidComposer(val id: UUID) : ValidationError()
    data class InvalidRaga(val id: UUID) : ValidationError()
}

sealed class ValidationWarning {
    object MissingComposer : ValidationWarning()
    data class JanyaRaga(val name: String) : ValidationWarning()
    data class DeityTempleMismatch(val deityId: UUID, val templeId: UUID) : ValidationWarning()
    object UnclearLyricStructure : ValidationWarning()
    object IncompleteLyrics : ValidationWarning()
}
```

### 6.2 Quality Scoring

Assign quality scores to guide manual review prioritization:

```kotlin
fun ImportedKrithi.calculateQualityScore(): QualityScore {
    var score = 0.0
    val factors = mutableMapOf<String, Double>()

    // Completeness (40%)
    factors["completeness"] = this.completenessScore() * 0.4

    // Resolution confidence (30%)
    val resolutionConf = listOfNotNull(
        this.composerResolutionConfidence,
        this.ragaResolutionConfidence,
        this.deityResolutionConfidence,
        this.templeResolutionConfidence
    ).average()
    factors["resolution"] = resolutionConf * 0.3

    // Source reliability (20%)
    val sourceScore = when (this.importSourceId) {
        KARNATIK_COM_SOURCE -> 0.95  // High quality source
        GURUGUHA_BLOG_SOURCE -> 0.80
        THYAGARAJA_BLOG_SOURCE -> 0.85
        else -> 0.70
    }
    factors["source"] = sourceScore * 0.2

    // Validation pass (10%)
    factors["validation"] = if (this.validationErrors.isEmpty()) 0.1 else 0.0

    score = factors.values.sum()

    return QualityScore(
        overall = score,
        factors = factors,
        tier = when {
            score >= 0.9 -> QualityTier.EXCELLENT
            score >= 0.75 -> QualityTier.GOOD
            score >= 0.60 -> QualityTier.FAIR
            else -> QualityTier.POOR
        }
    )
}

enum class QualityTier {
    EXCELLENT,  // Auto-approve for publication
    GOOD,       // Quick review
    FAIR,       // Standard review
    POOR        // Detailed review or discard
}
```

---

## 7. Import Pipeline Implementation Plan

### 7.1 Phase 1: Foundation (Weeks 1-3)

**Objective**: Build core infrastructure and single-source POC

**Deliverables:**
1. `ImportOrchestrationService` (Kotlin coroutines)
2. `WebScrapingService` (JSoup/Ktor client)
3. `ImportedKrithi` repository & CRUD
4. Basic entity resolution (composer, raga)
5. Simple validation
6. Admin API endpoints

**Architecture:**
```kotlin
// Service layer
interface ImportOrchestrationService {
    suspend fun executeImport(config: ImportSourceConfig): Flow<ImportProgress>
    suspend fun getImportStatus(batchId: UUID): ImportBatchStatus
}

// API layer
fun Route.importRoutes() {
    authenticate("admin") {
        post("/api/admin/import/execute") {
            val config = call.receive<ImportSourceConfig>()
            val batchId = UUID.randomUUID()

            launch {
                importOrchestrationService
                    .executeImport(config)
                    .collect { progress ->
                        // Update batch status in DB
                        importBatchRepo.updateProgress(batchId, progress)
                    }
            }

            call.respond(HttpStatusCode.Accepted, ImportBatchResponse(batchId))
        }

        get("/api/admin/import/batches/{batchId}") {
            val batchId = UUID.fromString(call.parameters["batchId"]!!)
            val status = importOrchestrationService.getImportStatus(batchId)
            call.respond(status)
        }
    }
}
```

**Data Model Additions:**
```sql
-- Track import batches
CREATE TABLE import_batches (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    import_source_id UUID NOT NULL REFERENCES import_sources(id),
    initiated_by_user_id UUID NOT NULL REFERENCES users(id),
    status VARCHAR(50) NOT NULL,  -- running, completed, failed
    total_urls INT,
    processed_urls INT,
    successful_imports INT,
    failed_imports INT,
    started_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    completed_at TIMESTAMPTZ,
    error_message TEXT
);

-- Detailed import logs
CREATE TABLE import_logs (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    batch_id UUID NOT NULL REFERENCES import_batches(id),
    stage VARCHAR(50) NOT NULL,  -- scraping, extraction, validation, etc.
    level VARCHAR(20) NOT NULL,  -- info, warning, error
    message TEXT NOT NULL,
    metadata JSONB,
    logged_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
```

**Testing:**
- Unit tests for each service
- Integration test: Import 10 krithis from karnatik.com
- Verify data lands in `imported_krithis` with correct status

---

### 7.2 Phase 2: Entity Resolution & De-duplication (Weeks 4-5)

**Objective**: Robust entity resolution and duplicate detection

**Deliverables:**
1. `ComposerResolutionService` with fuzzy matching
2. `RagaResolutionService` with melakarta support
3. `DeityResolutionService` with synonym graph
4. `TempleResolutionService` with multi-name support
5. `DeduplicationService` with multi-level detection
6. PostgreSQL trigram indices

**Implementation:**
```kotlin
// Orchestration integration
suspend fun executeImport(config: ImportSourceConfig): Flow<ImportProgress> = flow {
    // ... (scraping, extraction stages)

    // Stage 4: Entity Resolution
    val resolved = extracted.map { metadata ->
        val composerId = try {
            composerResolutionService.resolve(metadata.composer).getOrThrow()
        } catch (e: EntityResolutionException) {
            emit(ImportProgress.EntityResolutionWarning(metadata.url, e))
            null
        }

        val ragaId = try {
            ragaResolutionService.resolve(metadata.raga).getOrThrow()
        } catch (e: EntityResolutionException) {
            emit(ImportProgress.EntityResolutionWarning(metadata.url, e))
            null
        }

        // ... deityId, templeId

        ResolvedMetadata(
            raw = metadata,
            composerId = composerId,
            ragaId = ragaId,
            deityId = deityId,
            templeId = templeId,
            resolutionWarnings = listOf(/* ... */)
        )
    }

    emit(ImportProgress.EntityResolutionComplete(resolved.size))

    // Stage 5: De-duplication
    val deduped = deduplicationService.detectDuplicates(resolved)

    emit(ImportProgress.DuplicatesDetected(
        exact = deduped.exactDuplicates.size,
        fuzzy = deduped.fuzzyDuplicates.size
    ))

    // Continue with unique records only
    val unique = deduped.unique
    // ...
}
```

**Testing:**
- Test entity resolution with known variations
- Test de-duplication with intentional duplicates
- Measure precision/recall of fuzzy matching

---

### 7.3 Phase 3: Validation & Staging (Week 6)

**Objective**: Quality checks and staging for review

**Deliverables:**
1. `ValidationService` with musicological rules
2. Quality scoring system
3. Batch insert to `imported_krithis`
4. Import status dashboard (React admin)

**Validation Integration:**
```kotlin
// Stage 6: Validation
val validated = unique.map { resolved ->
    val validationResult = validationService.validate(resolved)
    val qualityScore = resolved.calculateQualityScore()

    ImportedKrithi(
        id = UUID.randomUUID(),
        importSourceId = config.sourceId,
        batchId = batchId,
        rawTitle = resolved.raw.title,
        rawLyrics = resolved.raw.lyrics,
        rawComposer = resolved.raw.composer,
        rawRaga = resolved.raw.raga,
        rawDeity = resolved.raw.deity,
        rawTemple = resolved.raw.temple,
        parsedPayload = resolved.toJsonb(),
        resolvedComposerId = resolved.composerId,
        resolvedRagaId = resolved.ragaId,
        resolvedDeityId = resolved.deityId,
        resolvedTempleId = resolved.templeId,
        validationErrors = validationResult.errors,
        validationWarnings = validationResult.warnings,
        qualityScore = qualityScore.overall,
        qualityTier = qualityScore.tier,
        importStatus = if (validationResult.isValid)
            ImportStatusEnum.PENDING
        else
            ImportStatusEnum.IN_REVIEW
    )
}

// Stage 7: Batch insert
importedKrithiRepo.batchInsert(validated)

emit(ImportProgress.Complete(
    total = validated.size,
    excellent = validated.count { it.qualityTier == QualityTier.EXCELLENT },
    good = validated.count { it.qualityTier == QualityTier.GOOD },
    fair = validated.count { it.qualityTier == QualityTier.FAIR },
    poor = validated.count { it.qualityTier == QualityTier.POOR }
))
```

**Dashboard UI (React):**
- Display import batches
- Show progress of running imports
- Filter imported krithis by quality tier
- Quick stats: total imported, pending review, mapped, rejected

---

### 7.4 Phase 4: Review Workflow (Weeks 7-8)

**Objective**: Enable expert review and canonicalization

**Deliverables:**
1. Review UI for `ImportedKrithi` records
2. Side-by-side comparison with existing krithis
3. Manual entity mapping interface
4. One-click canonicalization
5. Bulk approval workflow

**Review UI Features:**

**1. Import Review Queue**
```typescript
// React component
interface ImportReviewQueueProps {
  qualityFilter?: QualityTier;
  sourceFilter?: string;
  statusFilter?: ImportStatus;
}

const ImportReviewQueue: React.FC<ImportReviewQueueProps> = ({ ... }) => {
  const { data: imports, isLoading } = useQuery(
    ['importedKrithis', qualityFilter, sourceFilter, statusFilter],
    () => fetchImportedKrithis({ qualityFilter, sourceFilter, statusFilter })
  );

  return (
    <div className="review-queue">
      <Filters ... />
      <Table>
        <thead>
          <tr>
            <th>Title</th>
            <th>Composer</th>
            <th>Raga</th>
            <th>Quality</th>
            <th>Warnings</th>
            <th>Actions</th>
          </tr>
        </thead>
        <tbody>
          {imports?.map(imp => (
            <ImportRow
              key={imp.id}
              import={imp}
              onReview={() => openReviewModal(imp)}
            />
          ))}
        </tbody>
      </Table>
    </div>
  );
};
```

**2. Detailed Review Modal**
```typescript
const ImportReviewModal: React.FC<{ import: ImportedKrithi }> = ({ import }) => {
  const [composerOverride, setComposerOverride] = useState<UUID | null>(null);
  const [ragaOverride, setRagaOverride] = useState<UUID | null>(null);

  const handleApprove = async () => {
    await api.canonicalizeImport({
      importId: import.id,
      composerId: composerOverride ?? import.resolvedComposerId,
      ragaId: ragaOverride ?? import.resolvedRagaId,
      // ... other overrides
    });
  };

  return (
    <Modal title={`Review: ${import.rawTitle}`}>
      <Section title="Raw Data">
        <Field label="Title">{import.rawTitle}</Field>
        <Field label="Composer">{import.rawComposer}</Field>
        <Field label="Raga">{import.rawRaga}</Field>
        <Field label="Lyrics">
          <pre>{import.rawLyrics}</pre>
        </Field>
      </Section>

      <Section title="Resolved Entities">
        <EntityField
          label="Composer"
          resolvedId={import.resolvedComposerId}
          onOverride={setComposerOverride}
        />
        <EntityField
          label="Raga"
          resolvedId={import.resolvedRagaId}
          onOverride={setRagaOverride}
        />
        {/* ... */}
      </Section>

      <Section title="Validation">
        {import.validationErrors.map(err => (
          <Alert severity="error">{err.message}</Alert>
        ))}
        {import.validationWarnings.map(warn => (
          <Alert severity="warning">{warn.message}</Alert>
        ))}
      </Section>

      <Actions>
        <Button onClick={handleApprove}>Approve & Create Krithi</Button>
        <Button onClick={handleReject}>Reject</Button>
        <Button onClick={handleEdit}>Edit Raw Data</Button>
      </Actions>
    </Modal>
  );
};
```

**3. Canonicalization API**
```kotlin
post("/api/admin/import/{id}/canonicalize") {
    val importId = UUID.fromString(call.parameters["id"]!!)
    val overrides = call.receive<CanonicalizationOverrides>()

    val imported = importedKrithiRepo.findById(importId)
        ?: throw NotFoundException("Import not found")

    // Create canonical Krithi
    val krithiId = transaction {
        // 1. Create Krithi entity
        val krithi = Krithi(
            id = UUID.randomUUID(),
            title = imported.rawTitle,
            incipit = imported.extractIncipit(),
            composerId = overrides.composerId ?: imported.resolvedComposerId!!,
            primaryRagaId = overrides.ragaId ?: imported.resolvedRagaId!!,
            talaId = overrides.talaId ?: imported.resolvedTalaId,
            deityId = overrides.deityId ?: imported.resolvedDeityId,
            templeId = overrides.templeId ?: imported.resolvedTempleId,
            primaryLanguage = imported.rawLanguage?.let { LanguageCodeEnum.valueOf(it) } ?: LanguageCodeEnum.SA,
            musicalForm = MusicalFormEnum.KRITHI,
            isRagamalika = false,
            workflowState = WorkflowStateEnum.DRAFT,  // Start as draft
            createdByUserId = call.principal<UserIdPrincipal>()!!.userId
        )
        krithiRepo.insert(krithi)

        // 2. Create lyric variant
        if (imported.rawLyrics != null) {
            val lyricVariant = KrithiLyricVariant(
                id = UUID.randomUUID(),
                krithiId = krithi.id,
                language = krithi.primaryLanguage,
                script = inferScript(krithi.primaryLanguage),
                isPrimary = true,
                lyrics = imported.rawLyrics,
                createdByUserId = krithi.createdByUserId
            )
            krithiLyricVariantRepo.insert(lyricVariant)

            // 3. Parse and create sections (if possible)
            val sections = lyricParser.parseIntoSections(imported.rawLyrics)
            sections.forEach { section ->
                krithiSectionRepo.insert(section)
            }
        }

        // 4. Update imported krithi with mapping
        importedKrithiRepo.update(
            imported.copy(
                mappedKrithiId = krithi.id,
                importStatus = ImportStatusEnum.MAPPED,
                reviewerUserId = krithi.createdByUserId,
                reviewedAt = Clock.System.now()
            )
        )

        krithi.id
    }

    call.respond(HttpStatusCode.Created, CanonicalizationResponse(krithiId))
}
```

**Testing:**
- Manual testing of review UI
- Test canonicalization creates correct entities
- Verify audit trail

---

### 7.5 Phase 5: Multi-Source Scale (Weeks 9-12)

**Objective**: Extend to all 4 sources, optimize, production-ready

**Deliverables:**
1. Source-specific scrapers for all 4 sources
2. Source-specific extraction rules
3. Performance optimization (caching, parallel processing)
4. Monitoring and alerting
5. Documentation

**Source-Specific Scrapers:**

```kotlin
// Abstract base
abstract class KrithiSourceScraper {
    abstract val sourceId: UUID
    abstract suspend fun discoverUrls(): List<String>
    abstract suspend fun scrape(url: String): RawHtml
    abstract suspend fun extract(html: RawHtml): RawMetadata
}

// Karnatik.com implementation
class KarnatikComScraper(
    private val httpClient: HttpClient
) : KrithiSourceScraper() {
    override val sourceId = KARNATIK_COM_SOURCE_ID

    override suspend fun discoverUrls(): List<String> {
        val indexPage = httpClient.get("https://karnatik.com/lyrics.shtml").bodyAsText()
        val doc = Jsoup.parse(indexPage)

        return doc.select("a[href*='lyrics']")
            .map { it.attr("abs:href") }
            .filter { it.contains("/lyrics/") }
            .distinct()
    }

    override suspend fun scrape(url: String): RawHtml {
        val html = httpClient.get(url).bodyAsText()
        return RawHtml(url = url, content = html, scrapedAt = Clock.System.now())
    }

    override suspend fun extract(html: RawHtml): RawMetadata {
        val doc = Jsoup.parse(html.content)

        // Karnatik.com has structured format
        val title = doc.selectFirst("h1")?.text() ?: throw ExtractionException("No title")
        val composer = doc.selectFirst("div.composer")?.text()
        val raga = doc.selectFirst("div.raga")?.text()
        val tala = doc.selectFirst("div.tala")?.text()
        val lyrics = doc.selectFirst("div.lyrics")?.text()

        return RawMetadata(
            sourceUrl = html.url,
            title = title,
            composer = composer,
            raga = raga,
            tala = tala,
            lyrics = lyrics
        )
    }
}

// Guru-Guha blog implementation (Dikshitar list - list-based, not unstructured blog posts)
class GuruGuhaBlogScraper(
    private val httpClient: HttpClient
) : KrithiSourceScraper() {
    override val sourceId = GURUGUHA_BLOG_SOURCE_ID

    override suspend fun discoverUrls(): List<String> {
        // This blog has composer-specific list pages
        // Known URLs:
        return listOf(
            "https://guru-guha.blogspot.com/2009/04/dikshitar-kritis-alphabetical-list.html",
            // May have other composer lists - discover by crawling blog archive
        )
    }

    override suspend fun scrape(url: String): RawHtml {
        val html = httpClient.get(url).bodyAsText()
        return RawHtml(url = url, content = html, scrapedAt = Clock.System.now())
    }

    override suspend fun extract(html: RawHtml): RawMetadata {
        val doc = Jsoup.parse(html.content)
        val postContent = doc.selectFirst("div.post-body")?.html()
            ?: throw ExtractionException("No post content")

        // Determine composer from URL or page title
        val composer = when {
            html.url.contains("dikshitar") -> "Muthuswami Dikshitar"
            html.url.contains("tyagaraja") -> "Thyagaraja"
            else -> extractComposerFromTitle(doc)
        }

        // Parse list format - typically structured as:
        // <p>Krithi Name - Raga Name</p> or
        // <li>Krithi Name (Raga Name)</li>
        val entries = parseListEntries(doc, postContent)

        // This scraper returns multiple krithis, so we need to handle differently
        // For now, return as single RawMetadata with all entries in parsedPayload
        return RawMetadata(
            sourceUrl = html.url,
            title = "Batch Import",
            composer = composer,
            raga = null,  // Multiple ragas in list
            lyrics = null,
            batchEntries = entries  // Special field for list-based sources
        )
    }

    private fun parseListEntries(doc: Document, html: String): List<KrithiEntry> {
        val entries = mutableListOf<KrithiEntry>()

        // Try multiple patterns for list parsing
        // Pattern 1: <p>Name - Raga</p>
        doc.select("div.post-body p").forEach { p ->
            val text = p.text()
            val match = Regex("(.+?)\\s*[-–—]\\s*(.+)").find(text)
            if (match != null) {
                entries.add(KrithiEntry(
                    title = match.groupValues[1].trim(),
                    raga = match.groupValues[2].trim()
                ))
            }
        }

        // Pattern 2: <li>Name (Raga)</li>
        if (entries.isEmpty()) {
            doc.select("div.post-body li").forEach { li ->
                val text = li.text()
                val match = Regex("(.+?)\\s*\\((.+?)\\)").find(text)
                if (match != null) {
                    entries.add(KrithiEntry(
                        title = match.groupValues[1].trim(),
                        raga = match.groupValues[2].trim()
                    ))
                }
            }
        }

        return entries
    }

    private fun extractComposerFromTitle(doc: Document): String? {
        val title = doc.title().lowercase()
        return when {
            "dikshitar" in title -> "Muthuswami Dikshitar"
            "tyagaraja" in title || "thyagaraja" in title -> "Thyagaraja"
            "syama sastri" in title -> "Syama Sastri"
            else -> null
        }
    }
}

data class KrithiEntry(
    val title: String,
    val raga: String?
)

// Register all scrapers
val scraperRegistry = mapOf(
    KARNATIK_COM_SOURCE_ID to KarnatikComScraper(httpClient),
    GURUGUHA_BLOG_SOURCE_ID to GuruGuhaBlogScraper(httpClient),  // List-based, no AI needed
    SYAMAKRISHNA_BLOG_SOURCE_ID to SyamaKrishnaBlogScraper(httpClient),  // Similar list format
    THYAGARAJA_BLOG_SOURCE_ID to ThyagarajaBlogScraper(httpClient)
)
```

**Performance Optimization:**

```kotlin
class ImportOrchestrationService(
    // ...
    private val scraperRegistry: Map<UUID, KrithiSourceScraper>,
    private val cacheService: CacheService
) {
    suspend fun executeImport(config: ImportSourceConfig): Flow<ImportProgress> = flow {
        val scraper = scraperRegistry[config.sourceId]
            ?: throw IllegalArgumentException("Unknown source")

        emit(ImportProgress.Started)

        // Stage 1: Discovery (with caching)
        val urls = cacheService.getOrCompute("discover:${config.sourceId}", ttl = 1.days) {
            scraper.discoverUrls()
        }

        emit(ImportProgress.DiscoveryComplete(urls.size))

        // Stage 2: Parallel scraping with rate limiting
        val scraped = urls.asFlow()
            .buffer(capacity = config.parallelism)  // Control concurrency
            .map { url ->
                // Check if already scraped recently
                cacheService.get<RawHtml>("scrape:$url")?.let { return@map it }

                // Rate limit
                delay(config.rateLimitMs)

                // Scrape with retry
                retry(3) {
                    val html = scraper.scrape(url)
                    cacheService.set("scrape:$url", html, ttl = 7.days)
                    html
                }
            }
            .toList()

        emit(ImportProgress.ScrapingComplete(scraped.size))

        // Stage 3: Extraction (parallelized)
        val extracted = scraped
            .asFlow()
            .buffer(capacity = config.parallelism)
            .map { html ->
                retry(3) {
                    scraper.extract(html)
                }
            }
            .toList()

        // ... continue with entity resolution, validation, etc.
    }
}
```

**Monitoring:**

```kotlin
// Metrics collection
class ImportMetricsCollector {
    private val meterRegistry: MeterRegistry = SimpleMeterRegistry()

    val importsStarted = meterRegistry.counter("imports.started")
    val importsCompleted = meterRegistry.counter("imports.completed")
    val importsFailed = meterRegistry.counter("imports.failed")
    val importDuration = meterRegistry.timer("imports.duration")
    val entitiesResolved = meterRegistry.counter("imports.entities.resolved")
    val entitiesUnresolved = meterRegistry.counter("imports.entities.unresolved")
    val duplicatesDetected = meterRegistry.counter("imports.duplicates.detected")
    val validationErrors = meterRegistry.counter("imports.validation.errors")
}

// Usage in service
suspend fun executeImport(config: ImportSourceConfig): Flow<ImportProgress> = flow {
    metrics.importsStarted.increment()
    val startTime = Clock.System.now()

    try {
        // ... pipeline execution

        metrics.importsCompleted.increment()
        metrics.importDuration.record(Duration.between(startTime, Clock.System.now()))
    } catch (e: Exception) {
        metrics.importsFailed.increment()
        throw e
    }
}
```

---

## 8. Koog Integration Strategy (Optional Enhancement)

### 8.1 When to Use Koog

**Recommendation**: Use Koog **selectively** for AI-intensive stages, not entire pipeline.

**Ideal Koog Use Cases:**
1. **Metadata Extraction from Semi-Structured Lists** (when list format varies significantly)
2. **Ambiguous Entity Resolution** (when fuzzy matching produces multiple candidates)
3. **Lyric Section Parsing** (determining pallavi vs anupallavi vs charanam boundaries)

**Keep Custom Kotlin For:**
1. Web scraping (deterministic)
2. Exact entity matching (database lookups)
3. De-duplication (algorithmic)
4. Validation rules (deterministic)
5. Database operations (transactional)

### 8.2 Hybrid Architecture

```kotlin
// Main orchestration remains custom Kotlin
class ImportOrchestrationService(
    private val webScrapingService: WebScrapingService,
    private val koogExtractionAgent: KrithiExtractionAgent,  // Koog agent
    private val entityResolutionService: EntityResolutionService,
    private val koogAmbiguityResolver: AmbiguityResolverAgent,  // Koog agent
    // ...
) {
    suspend fun executeImport(config: ImportSourceConfig): Flow<ImportProgress> = flow {
        // Stage 1-2: Custom (scraping)
        val scraped = webScrapingService.scrapeAll(config)

        // Stage 3: Koog (AI extraction for unstructured sources)
        val extracted = if (config.requiresAIExtraction) {
            scraped.map { html ->
                koogExtractionAgent.extract(html)
            }
        } else {
            scraped.map { html ->
                customExtraction(html)  // Deterministic extraction
            }
        }

        // Stage 4: Custom (entity resolution attempt)
        val resolved = extracted.map { metadata ->
            entityResolutionService.resolveAll(metadata)
        }

        // Stage 5: Koog (ambiguity resolution for unclear cases)
        val finalResolved = resolved.map { res ->
            if (res.hasAmbiguity()) {
                koogAmbiguityResolver.resolve(res)
            } else {
                res
            }
        }

        // Stage 6+: Custom (validation, staging)
        // ...
    }
}
```

### 8.3 Koog Agent Implementation

**Extraction Agent:**
```kotlin
val krithiExtractionAgent = agent {
    name = "krithi-metadata-extractor"
    description = "Extract structured krithi metadata from unstructured blog posts"

    llm = GeminiProvider(
        model = "gemini-2.0-flash-exp",
        apiKey = config.geminiApiKey
    )

    systemPrompt = """
    You are an expert in Carnatic music. Extract structured metadata from blog posts about krithis.

    Return JSON with fields:
    - title: krithi name (exact)
    - composer: composer name (standardized)
    - raga: raga name (standardized)
    - tala: tala name (if mentioned)
    - deity: deity addressed (if clear)
    - lyrics: full lyrics with sections separated

    If a field is not found, return null. Do not guess.
    """.trimIndent()

    tools = emptyList()  // No tools needed for extraction
}

// Usage
suspend fun extract(html: RawHtml): RawMetadata {
    val doc = Jsoup.parse(html.content)
    val postContent = doc.selectFirst("div.post-body")?.text()
        ?: throw ExtractionException("No content")

    val response = krithiExtractionAgent.invoke(
        message = "Extract krithi metadata from:\n\n$postContent"
    )

    val extracted = Json.decodeFromString<ExtractedMetadata>(response.content)

    return RawMetadata(
        sourceUrl = html.url,
        title = extracted.title,
        composer = extracted.composer,
        raga = extracted.raga,
        tala = extracted.tala,
        deity = extracted.deity,
        lyrics = extracted.lyrics
    )
}
```

**Ambiguity Resolution Agent:**
```kotlin
val ambiguityResolverAgent = agent {
    name = "entity-ambiguity-resolver"
    description = "Resolve ambiguous entity matches using musical knowledge"

    llm = GeminiProvider(
        model = "gemini-1.5-pro",  // Use Pro for reasoning
        apiKey = config.geminiApiKey
    )

    systemPrompt = """
    You are an expert in Carnatic music. Resolve ambiguous entity matches.

    Given:
    - Raw entity name from source
    - Multiple candidate matches from database
    - Context (composer, raga, other metadata)

    Determine the most likely match based on musical knowledge.
    Return the UUID of the best match and confidence (0-1).
    """.trimIndent()

    tools = listOf(
        getComposerDetailsTool,
        getRagaDetailsTool
    )
}

// Tool definitions
val getComposerDetailsTool = tool("get_composer_details") {
    description = "Get detailed information about a composer"
    parameter<UUID>("composerId")

    execute { composerId ->
        composerRepo.findById(composerId)?.let {
            ComposerDetails(
                id = it.id,
                name = it.name,
                period = "${it.birthYear}-${it.deathYear}",
                place = it.place,
                primaryLanguage = it.primaryLanguage,
                notes = it.notes
            )
        }
    }
}

// Usage
suspend fun resolveAmbiguousComposer(
    rawName: String,
    candidates: List<Composer>,
    context: KrithiContext
): UUID? {
    val prompt = """
    Resolve ambiguous composer match:

    Raw name from source: "$rawName"

    Candidate matches:
    ${candidates.mapIndexed { i, c -> "${i+1}. ${c.name} (${c.id}) - ${c.birthYear}-${c.deathYear}, ${c.place}" }.joinToString("\n")}

    Context:
    - Raga: ${context.ragaName}
    - Lyrics snippet: ${context.lyricsSnippet.take(100)}
    - Source: ${context.sourceUrl}

    Which composer is most likely? Return JSON: {"composerId": "UUID", "confidence": 0.0-1.0, "reasoning": "..."}
    """.trimIndent()

    val response = ambiguityResolverAgent.invoke(message = prompt)
    val result = Json.decodeFromString<AmbiguityResolution>(response.content)

    return if (result.confidence > 0.75) result.composerId else null
}
```

### 8.4 Cost-Benefit of Koog Integration

**Benefits:**
- ✅ Better handling of unstructured sources
- ✅ Intelligent ambiguity resolution
- ✅ Reduced manual review for complex cases
- ✅ Leverage Gemini's musicological knowledge

**Costs:**
- ❌ Additional dependency and learning curve
- ❌ LLM API costs (Gemini)
- ❌ Latency for AI stages
- ❌ Debugging complexity

**Decision Criteria:**
- If > 30% of imports require manual review due to ambiguity → Consider Koog
- If extraction quality from blogs is poor with regex/parsing → Use Koog extraction
- If team comfortable with Koog and budget allows → Integrate selectively

**Estimated LLM Costs:**
- Gemini 2.0 Flash: ~$0.10 per 1M tokens
- Extract 1000 krithis: ~500K tokens → $0.05
- Negligible cost, worth the quality improvement

---

## 9. Risk Assessment & Mitigation

### 9.1 Technical Risks

| Risk | Probability | Impact | Mitigation |
|:---|:---|:---|:---|
| **Website structure changes** | Medium | High | Version scrapers, automated tests, alerts on scrape failures |
| **Entity resolution accuracy < 80%** | Medium | High | Human review queue, confidence thresholds, expert validation |
| **De-duplication false positives** | Medium | Medium | Multi-level detection, manual review for uncertain matches |
| **Performance degradation at scale** | Low | Medium | Caching, parallel processing, database optimization |
| **Lyrics section parsing failures** | High | Medium | Accept as limitation, flag for manual parsing |
| **Temple association ambiguity** | High | Low | Accept many krithis won't have temples, manual curation |

### 9.2 Data Quality Risks

| Risk | Probability | Impact | Mitigation |
|:---|:---|:---|:---|
| **Incomplete metadata (50%+ missing fields)** | High | Medium | Prioritize high-quality sources, accept incompleteness |
| **Incorrect entity mappings** | Medium | High | Confidence scoring, manual review, audit trail |
| **Transliteration inconsistencies** | High | Medium | Normalization algorithms, accept variations |
| **Duplicate canonical krithis** | Low | High | Robust de-duplication, review before canonicalization |

### 9.3 Operational Risks

| Risk | Probability | Impact | Mitigation |
|:---|:---|:---|:---|
| **Manual review bottleneck** | High | Medium | Quality tiers (auto-approve excellent), prioritize review queue |
| **Source website downtime** | Low | Low | Cache scraped data, retry logic, multiple sources |
| **Import job failures** | Medium | Medium | Retry logic, progress tracking, alerting |
| **Team bandwidth for reviews** | High | High | Phased rollout, community contributions |

---

## 10. Success Metrics & KPIs

### 10.1 Import Pipeline Metrics

**Coverage:**
- ✅ 70%+ of well-known krithis imported within 6 months
- ✅ All 4 sources integrated and actively importing

**Throughput:**
- ✅ 100-500 krithis per import batch
- ✅ 1 import batch per week (initially)

**Quality:**
- ✅ 85%+ of imports have complete metadata (title, composer, raga)
- ✅ 90%+ entity resolution confidence for matched entities
- ✅ < 10% duplicate rate after de-duplication

**Efficiency:**
- ✅ < 20% of imports require manual entity mapping
- ✅ 50%+ of "excellent" tier imports auto-approved
- ✅ Average review time < 5 minutes per krithi

### 10.2 Data Quality Metrics

**Completeness by Field:**
- Title: 100%
- Composer: 90%+
- Raga: 90%+
- Tala: 70%+
- Deity: 60%+
- Temple: 30%+ (acceptable, hard to infer)
- Lyrics: 60%+

**Accuracy:**
- Expert-verified: 50%+ within 1 year
- User-reported errors: < 5%

### 10.3 User Impact Metrics

**Search & Discovery:**
- 2x increase in searchable krithis
- Improved search relevance (deity/temple filters)

**Editorial Productivity:**
- 70% reduction in manual data entry
- 3x faster krithi publication rate

---

## 11. Recommendations & Next Steps

### 11.1 Primary Recommendation

**Adopt a Phased Custom Kotlin Pipeline with Selective AI Enhancement:**

**Phase 1 (Weeks 1-8)**: MVP with Single Source
- Custom Kotlin orchestration using coroutines
- Basic entity resolution and de-duplication
- Manual review workflow
- Target: 100-500 krithis from karnatik.com

**Phase 2 (Weeks 9-12)**: Multi-Source Expansion
- Add remaining 3 sources
- Optimize entity resolution algorithms
- Performance tuning
- Target: 1,000+ krithis across all sources

**Phase 3 (Months 4-6)**: AI Enhancement (Optional)
- Integrate Gemini for unstructured extraction (blogs)
- Add AI-assisted ambiguity resolution
- Lyric section parsing with AI
- Target: 2,000+ krithis, reduced manual review burden

### 11.2 Koog.ai Recommendation

**Do NOT use Koog for entire pipeline orchestration.**

**Rationale:**
- Most pipeline stages are deterministic (scraping, validation, database ops)
- Kotlin coroutines provide sufficient orchestration
- Koog adds overhead without clear benefit for deterministic stages

**Consider Koog for specific AI-intensive stages:**
- Metadata extraction for complex list variations (if needed)
- Ambiguity resolution when multiple entity candidates
- Lyric section boundary detection

**Decision Point**: After Phase 1 completion, evaluate if extraction quality from unstructured sources warrants Koog integration.

### 11.3 Temple/Kshetra Integration

**Temple Data Import:**
- Separate pipeline to import temple data from templenet.com
- Populate `temples` and `temple_names` tables
- Build deity-temple associations

**Krithi-Temple Linking:**
- Phase 1: Explicit mentions only (parse from lyrics/title)
- Phase 2: Deity-based inference with confidence scores
- Phase 3: Manual curation for high-profile krithis

**Accept Limitation**: Many krithis will not have temple associations. This is acceptable.

### 11.4 Immediate Next Steps (Week 1)

1. **Architecture Review**: Review this document with team, align on approach
2. **Spike: Web Scraping**: Build quick scraper for karnatik.com (1 day)
3. **Database Schema**: Ensure `import_sources`, `imported_krithis`, `import_batches` tables exist
4. **Service Skeleton**: Create `ImportOrchestrationService` interface
5. **Admin API Stub**: Create `/api/admin/import/*` endpoints
6. **UI Mockup**: Design import review UI (wireframes)

### 11.5 Key Decisions Required

**Decision 1: Start with which source?**
- **Recommendation**: karnatik.com (highest quality, structured)

**Decision 2: Use Gemini for extraction now or later?**
- **Recommendation**: Later (Phase 3), start with deterministic extraction

**Decision 3: Auto-approve excellent tier imports?**
- **Recommendation**: Yes, but with audit trail and ability to revert

**Decision 4: Community contributions for review?**
- **Recommendation**: Yes, but Phase 4+ (after internal review workflow proven)

---

## 12. Cost & Timeline Summary

### 12.1 Development Costs

**Phase 1 (MVP - 8 weeks):**
- 1 Senior Kotlin Developer: ~$20,000
- Infrastructure: $0 (uses existing)
- **Total**: $20,000

**Phase 2 (Multi-Source - 4 weeks):**
- 1 Senior Kotlin Developer: ~$10,000
- **Total**: $10,000

**Phase 3 (AI Enhancement - 4 weeks, optional):**
- 1 Senior Kotlin Developer: ~$8,000
- Gemini API: ~$50
- **Total**: $8,050

**Grand Total: $38,050** (or $30,000 without AI enhancement)

### 12.2 Ongoing Costs

**Infrastructure:**
- PostgreSQL: Included in existing hosting
- Caching (Redis): ~$20/month
- **Total**: ~$240/year

**LLM API (if using Gemini):**
- ~1,000 krithis/month extraction: ~$0.50/month
- **Total**: ~$6/year (negligible)

**Maintenance:**
- Scraper updates: ~1 day/quarter
- Entity resolution refinement: ~2 days/quarter
- **Total**: ~$5,000/year

### 12.3 Timeline

```
Week 1-3:   Core infrastructure, single scraper, basic pipeline
Week 4-5:   Entity resolution, de-duplication
Week 6:     Validation, staging
Week 7-8:   Review workflow UI
            [Phase 1 Complete: 100-500 krithis from karnatik.com]

Week 9-10:  Add 3 additional scrapers
Week 11:    Performance optimization, monitoring
Week 12:    Testing, documentation
            [Phase 2 Complete: 1,000+ krithis from all sources]

Week 13-16: (Optional) Gemini integration for extraction & ambiguity
            [Phase 3 Complete: 2,000+ krithis, reduced manual review]
```

**Milestones:**
- Week 8: First 100 krithis reviewed and canonicalized
- Week 12: 1,000+ krithis in review queue
- Week 16: 2,000+ krithis, production-ready pipeline

---

## 13. Appendices

### Appendix A: Sample Import Configuration

```kotlin
data class ImportSourceConfig(
    val sourceId: UUID,
    val sourceName: String,
    val baseUrl: String,
    val scraperType: ScraperType,
    val extractionStrategy: ExtractionStrategy,
    val rateLimitMs: Long = 1000,
    val parallelism: Int = 5,
    val requiresAIExtraction: Boolean = false,
    val implicitDeity: String? = null,
    val implicitComposer: String? = null
)

// Example configs
val karnatikComConfig = ImportSourceConfig(
    sourceId = KARNATIK_COM_SOURCE_ID,
    sourceName = "Karnatik.com",
    baseUrl = "https://karnatik.com/lyrics.shtml",
    scraperType = ScraperType.STRUCTURED_HTML,
    extractionStrategy = ExtractionStrategy.CSS_SELECTORS,
    rateLimitMs = 1000,
    parallelism = 5
)

val guruGuhaBlogConfig = ImportSourceConfig(
    sourceId = GURUGUHA_BLOG_SOURCE_ID,
    sourceName = "Guru-Guha Blog (Dikshitar List)",
    baseUrl = "https://guru-guha.blogspot.com/2009/04/dikshitar-kritis-alphabetical-list.html",
    scraperType = ScraperType.LIST_BASED,
    extractionStrategy = ExtractionStrategy.PATTERN_MATCHING,
    requiresAIExtraction = false,  // List format is structured enough for regex
    implicitComposer = "Muthuswami Dikshitar",
    rateLimitMs = 2000,
    parallelism = 3
)

val thyagarajaBlogConfig = ImportSourceConfig(
    sourceId = THYAGARAJA_BLOG_SOURCE_ID,
    sourceName = "Thyagaraja Vaibhavam",
    baseUrl = "https://thyagaraja-vaibhavam.blogspot.com/",
    scraperType = ScraperType.LIST_BASED,
    extractionStrategy = ExtractionStrategy.PATTERN_MATCHING,
    implicitComposer = "Thyagaraja",
    rateLimitMs = 2000,
    parallelism = 3
)
```

### Appendix B: Entity Resolution Query Examples

```sql
-- Find composer by name with trigram similarity
SELECT
    id,
    name,
    similarity(name, 'Tyagaraja') AS sim_score
FROM composers
WHERE similarity(name, 'Tyagaraja') > 0.75
ORDER BY sim_score DESC
LIMIT 5;

-- Find raga with normalized name match
SELECT id, name
FROM ragas
WHERE name_normalized = normalize('Mayamalava Gowla');

-- Find temple by deity and name similarity
SELECT
    t.id,
    t.name,
    tn.name AS variant_name,
    similarity(tn.name, 'Tirupati') AS sim_score
FROM temples t
JOIN temple_names tn ON tn.temple_id = t.id
WHERE t.primary_deity_id = :deityId
  AND similarity(tn.name, 'Tirupati') > 0.80
ORDER BY sim_score DESC
LIMIT 5;
```

### Appendix C: Glossary

- **Krithi**: A form of Carnatic music composition with structured lyrics
- **Pallavi**: First section of a krithi (refrain)
- **Anupallavi**: Second section
- **Charanam**: Verse section(s), often multiple
- **Raga**: Melodic framework, scale
- **Melakarta**: Parent raga (72 fundamental ragas)
- **Janya**: Derived raga from melakarta
- **Tala**: Rhythmic cycle
- **Kshetra**: Sacred place, temple
- **Sampradaya**: Lineage, school of performance
- **Incipit**: Opening line of a krithi
- **IAST**: International Alphabet of Sanskrit Transliteration

---

## Conclusion

Building a robust Krithi import pipeline for Sangeetha Grantha requires careful attention to musicological accuracy, entity resolution complexity, and data quality. The recommended approach is a **custom Kotlin-based pipeline** leveraging the existing technology stack, with **selective AI enhancement** for unstructured sources.

**Key Success Factors:**
1. ✅ Start small with single high-quality source (karnatik.com)
2. ✅ Invest in robust entity resolution (composer, raga, deity, temple)
3. ✅ Multi-level de-duplication to avoid canonical duplicates
4. ✅ Quality scoring to prioritize review workflow
5. ✅ Accept limitations (not all krithis will have temples, lyrics may be incomplete)
6. ✅ Human-in-loop for validation, don't over-automate
7. ✅ Phased rollout, iterate based on learnings

**Avoid:**
- ❌ Over-engineering with Airflow or full Koog orchestration
- ❌ Trying to import all sources simultaneously
- ❌ Auto-publishing without expert review
- ❌ Attempting 100% completeness (accept 70-90% is excellent)

With disciplined execution, the import pipeline can achieve **1,000+ krithis within 3 months** and **2,000+ within 6 months**, significantly accelerating the growth of the Sangeetha Grantha catalog while maintaining musicological integrity.

---

**Document Status**: Complete and ready for team review.
**Next Action**: Architecture review meeting, decision on Phase 1 kickoff.
