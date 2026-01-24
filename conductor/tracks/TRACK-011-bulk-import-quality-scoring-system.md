# TRACK-011: Bulk Import Quality Scoring System

| Metadata | Value |
|:---|:---|
| **Status** | Proposed |
| **Owner** | Backend Team |
| **Priority** | HIGH |
| **Created** | 2026-01-23 |
| **Related Tracks** | TRACK-001 (Bulk Import), TRACK-012 (Review Workflow) |
| **Implementation Plan** | [bulk-import-fixes-implementation-plan.md](../../application_documentation/07-quality/bulk-import-fixes-implementation-plan.md) |

## 1. Goal

Implement quality scoring system as specified in the CSV import strategy. Quality scores enable review prioritization, auto-approval automation, and data quality assessment.

## 2. Problem Statement

The strategy document (Section 8.2) specifies quality tiers (EXCELLENT, GOOD, FAIR, POOR) and scoring weights, but the implementation lacks:
- Quality score calculation logic
- Quality tier assignment
- Database persistence of scores
- Integration with review workflow and auto-approval

**Impact:**
- Cannot prioritize review queue by quality
- Auto-approval cannot use quality-based rules
- No data quality metrics for batch assessment

## 3. Implementation Plan

### 3.1 Database Schema Updates

**File:** `database/migrations/16__add_quality_scoring.sql`

**Changes:**
```sql
-- Add quality scoring columns to imported_krithis
ALTER TABLE imported_krithis
    ADD COLUMN IF NOT EXISTS quality_score DECIMAL(3,2),
    ADD COLUMN IF NOT EXISTS quality_tier VARCHAR(20),
    ADD COLUMN IF NOT EXISTS completeness_score DECIMAL(3,2),
    ADD COLUMN IF NOT EXISTS resolution_confidence DECIMAL(3,2),
    ADD COLUMN IF NOT EXISTS source_quality DECIMAL(3,2),
    ADD COLUMN IF NOT EXISTS validation_score DECIMAL(3,2);

-- Add index for quality tier filtering
CREATE INDEX IF NOT EXISTS idx_imported_krithis_quality_tier
    ON imported_krithis(quality_tier)
    WHERE quality_tier IS NOT NULL;

-- Add index for quality score sorting
CREATE INDEX IF NOT EXISTS idx_imported_krithis_quality_score
    ON imported_krithis(quality_score DESC NULLS LAST);
```

**Quality Tier Enum:**
```sql
DO $$
BEGIN
  IF NOT EXISTS (SELECT 1 FROM pg_type WHERE typname = 'quality_tier_enum') THEN
    CREATE TYPE quality_tier_enum AS ENUM ('EXCELLENT', 'GOOD', 'FAIR', 'POOR');
  END IF;
END$$;
```

---

### 3.2 Quality Scoring Service

**File:** `modules/backend/api/src/main/kotlin/com/sangita/grantha/backend/api/services/QualityScoringService.kt`

**Implementation:**
```kotlin
class QualityScoringService {
    
    data class QualityScore(
        val overall: Double,
        val completeness: Double,           // 40% weight
        val resolutionConfidence: Double,    // 30% weight
        val sourceQuality: Double,          // 20% weight
        val validationScore: Double,        // 10% weight
        val tier: QualityTier
    )
    
    enum class QualityTier {
        EXCELLENT,  // ≥ 0.90
        GOOD,       // ≥ 0.75
        FAIR,       // ≥ 0.60
        POOR        // < 0.60
    }
    
    /**
     * Calculate quality score for an imported krithi.
     * 
     * Scoring weights (per strategy Section 8.2):
     * - Completeness: 40% (has title, lyrics, composer, raga, tala)
     * - Resolution Confidence: 30% (entity resolution confidence scores)
     * - Source Quality: 20% (blogspot.com = 0.8, other = 0.6)
     * - Validation: 10% (passed header validation, URL valid)
     */
    suspend fun calculateQualityScore(
        imported: ImportedKrithiDto,
        resolutionData: JsonObject? = null
    ): QualityScore {
        val completeness = calculateCompleteness(imported)
        val resolutionConfidence = calculateResolutionConfidence(resolutionData)
        val sourceQuality = calculateSourceQuality(imported.sourceUrl)
        val validationScore = calculateValidationScore(imported)
        
        val overall = (completeness * 0.40) +
                     (resolutionConfidence * 0.30) +
                     (sourceQuality * 0.20) +
                     (validationScore * 0.10)
        
        val tier = determineTier(overall)
        
        return QualityScore(
            overall = overall,
            completeness = completeness,
            resolutionConfidence = resolutionConfidence,
            sourceQuality = sourceQuality,
            validationScore = validationScore,
            tier = tier
        )
    }
    
    private fun calculateCompleteness(imported: ImportedKrithiDto): Double {
        var score = 0.0
        var maxScore = 0.0
        
        // Title (required)
        maxScore += 1.0
        if (imported.title.isNotBlank()) score += 1.0
        
        // Lyrics (required)
        maxScore += 1.0
        if (imported.lyrics.isNotBlank()) score += 1.0
        
        // Composer (required)
        maxScore += 1.0
        if (imported.composerId != null) score += 1.0
        
        // Raga (required)
        maxScore += 1.0
        if (imported.ragaId != null) score += 1.0
        
        // Tala (optional but valuable)
        maxScore += 0.5
        if (imported.talaId != null) score += 0.5
        
        // Sections (optional but valuable)
        maxScore += 0.5
        if (imported.sections.isNotEmpty()) score += 0.5
        
        return if (maxScore > 0) score / maxScore else 0.0
    }
    
    private fun calculateResolutionConfidence(resolutionData: JsonObject?): Double {
        if (resolutionData == null) return 0.0
        
        val composerConfidence = resolutionData["composer_confidence"]?.jsonPrimitive?.doubleOrNull ?: 0.0
        val ragaConfidence = resolutionData["raga_confidence"]?.jsonPrimitive?.doubleOrNull ?: 0.0
        val talaConfidence = resolutionData["tala_confidence"]?.jsonPrimitive?.doubleOrNull ?: 0.0
        
        // Average of all confidence scores
        val count = listOf(composerConfidence, ragaConfidence, talaConfidence).count { it > 0 }
        return if (count > 0) {
            (composerConfidence + ragaConfidence + talaConfidence) / count
        } else {
            0.0
        }
    }
    
    private fun calculateSourceQuality(sourceUrl: String?): Double {
        if (sourceUrl == null) return 0.0
        
        // blogspot.com sources are considered higher quality
        return if (sourceUrl.contains("blogspot.com", ignoreCase = true)) {
            0.8
        } else {
            0.6
        }
    }
    
    private fun calculateValidationScore(imported: ImportedKrithiDto): Double {
        var score = 0.0
        
        // URL validation (syntax check passed)
        if (imported.sourceUrl != null && isValidUrl(imported.sourceUrl)) {
            score += 0.5
        }
        
        // Header validation (passed CSV header checks)
        // Assume all imported krithis passed header validation
        score += 0.5
        
        return score
    }
    
    private fun determineTier(overall: Double): QualityTier {
        return when {
            overall >= 0.90 -> QualityTier.EXCELLENT
            overall >= 0.75 -> QualityTier.GOOD
            overall >= 0.60 -> QualityTier.FAIR
            else -> QualityTier.POOR
        }
    }
    
    private fun isValidUrl(url: String): Boolean {
        return try {
            URI(url).scheme != null
        } catch (e: Exception) {
            false
        }
    }
}
```

---

### 3.3 Integration with Entity Resolution

**File:** `modules/backend/api/src/main/kotlin/com/sangita/grantha/backend/api/services/BulkImportWorkerService.kt`

**Changes:**
```kotlin
private suspend fun processResolutionTask(task: ImportTaskRunDto, config: WorkerConfig) {
    // ... existing resolution logic ...
    
    // ✅ NEW: Calculate quality score after resolution
    val qualityScore = qualityScoringService.calculateQualityScore(
        imported = importedKrithi,
        resolutionData = resolutionResult.resolutionData
    )
    
    // ✅ NEW: Persist quality score
    dal.imports.updateImportedKrithi(
        id = importedKrithi.id,
        qualityScore = qualityScore.overall,
        qualityTier = qualityScore.tier.name,
        completenessScore = qualityScore.completeness,
        resolutionConfidence = qualityScore.resolutionConfidence,
        sourceQuality = qualityScore.sourceQuality,
        validationScore = qualityScore.validationScore
    )
    
    // ... rest of existing logic ...
}
```

---

### 3.4 Repository Updates

**File:** `modules/backend/dal/src/main/kotlin/com/sangita/grantha/backend/dal/repositories/ImportRepository.kt`

**Changes:**
```kotlin
suspend fun updateImportedKrithi(
    id: Uuid,
    qualityScore: Double? = null,
    qualityTier: String? = null,
    completenessScore: Double? = null,
    resolutionConfidence: Double? = null,
    sourceQuality: Double? = null,
    validationScore: Double? = null,
    // ... other existing parameters ...
): ImportedKrithiDto? = DatabaseFactory.dbQuery {
    ImportedKrithisTable
        .updateReturning(
            where = { ImportedKrithisTable.id eq id.toJavaUuid() }
        ) { stmt ->
            qualityScore?.let { stmt[ImportedKrithisTable.qualityScore] = it.toBigDecimal() }
            qualityTier?.let { stmt[ImportedKrithisTable.qualityTier] = it }
            completenessScore?.let { stmt[ImportedKrithisTable.completenessScore] = it.toBigDecimal() }
            resolutionConfidence?.let { stmt[ImportedKrithisTable.resolutionConfidence] = it.toBigDecimal() }
            sourceQuality?.let { stmt[ImportedKrithisTable.sourceQuality] = it.toBigDecimal() }
            validationScore?.let { stmt[ImportedKrithisTable.validationScore] = it.toBigDecimal() }
            // ... other existing updates ...
        }
        .singleOrNull()
        ?.toImportedKrithiDto()
}
```

---

### 3.5 Table Schema Updates

**File:** `modules/backend/dal/src/main/kotlin/com/sangita/grantha/backend/dal/tables/CoreTables.kt`

**Changes:**
```kotlin
object ImportedKrithisTable : UUIDTable("imported_krithis") {
    // ... existing columns ...
    
    val qualityScore = decimal("quality_score", 3, 2).nullable()
    val qualityTier = varchar("quality_tier", 20).nullable()
    val completenessScore = decimal("completeness_score", 3, 2).nullable()
    val resolutionConfidence = decimal("resolution_confidence", 3, 2).nullable()
    val sourceQuality = decimal("source_quality", 3, 2).nullable()
    val validationScore = decimal("validation_score", 3, 2).nullable()
}
```

---

## 4. Progress Log

### 2026-01-23: Track Created
- ✅ Analyzed strategy document (Section 8.2)
- ✅ Designed quality scoring algorithm
- ✅ Created implementation plan

### Pending
- [ ] Create database migration (16__add_quality_scoring.sql)
- [ ] Implement QualityScoringService
- [ ] Update ImportRepository with quality score fields
- [ ] Update CoreTables schema
- [ ] Integrate with BulkImportWorkerService (resolution stage)
- [ ] Add unit tests for scoring logic
- [ ] Add integration tests for quality score persistence

---

## 5. Success Criteria

- ✅ Quality scores calculated for all imports (completeness, confidence, source, validation)
- ✅ Quality tiers assigned (EXCELLENT ≥0.90, GOOD ≥0.75, FAIR ≥0.60, POOR <0.60)
- ✅ Scores persisted to database
- ✅ Scores visible in review UI (via API)
- ✅ Unit tests pass (>80% coverage)
- ✅ Integration tests pass

---

## 6. References

- [CSV Import Strategy](../../application_documentation/01-requirements/features/bulk-import/01-strategy/csv-import-strategy.md) (Section 8.2)
- [Implementation Plan](../../application_documentation/07-quality/bulk-import-fixes-implementation-plan.md)
- [Claude Review](../../application_documentation/07-quality/bulk-import-implementation-review-claude.md)
- [Goose Review](../../application_documentation/07-quality/csv-import-strategy-implementation-review-goose.md)
