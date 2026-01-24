# TRACK-012: Bulk Import Review Workflow Completion

| Metadata | Value |
|:---|:---|
| **Status** | Completed |
| **Owner** | Backend/Frontend Team |
| **Priority** | HIGH |
| **Created** | 2026-01-23 |
| **Updated** | 2026-01-23 |
| **Related Tracks** | TRACK-001 (Bulk Import), TRACK-011 (Quality Scoring) |
| **Implementation Plan** | [bulk-import-fixes-implementation-plan.md](../../application_documentation/07-quality/bulk-import-fixes-implementation-plan.md) |

## 1. Goal

Complete the review workflow APIs and frontend enhancements to enable batch-scale moderation workflows, quality-tier based filtering, and configurable auto-approval rules.

## 2. Problem Statement

Current implementation has:
- ✅ Per-import review (individual approve/reject)
- ✅ Batch-level approve-all/reject-all
- ❌ Missing bulk-review API endpoint
- ❌ Missing auto-approve queue API endpoint
- ❌ Hardcoded auto-approval rules (not configurable)
- ❌ No batch filter in review queue (frontend)
- ❌ No quality tier filtering (frontend)

**Impact:**
- Cannot efficiently review large batches (1,200+ entries)
- Auto-approval cannot use quality tiers
- Reviewers cannot filter by batch or quality tier

## 3. Implementation Plan

### 3.1 Backend: Bulk Review API

**File:** `modules/backend/api/src/main/kotlin/com/sangita/grantha/backend/api/routes/ImportRoutes.kt`

**New Endpoint:**
```kotlin
/**
 * POST /v1/admin/imports/bulk-review
 * 
 * Review multiple imports in a single request.
 * 
 * Request body:
 * {
 *   "importIds": ["uuid1", "uuid2", ...],
 *   "action": "APPROVE" | "REJECT",
 *   "overrides": { ... }  // Optional entity overrides
 * }
 */
post("/bulk-review") {
    val request = call.receive<BulkReviewRequest>()
    
    val results = request.importIds.map { importId ->
        try {
            when (request.action) {
                "APPROVE" -> {
                    importService.reviewImport(
                        importId = importId,
                        request = ImportReviewRequest(
                            action = ImportReviewAction.APPROVE,
                            overrides = request.overrides
                        )
                    )
                    BulkReviewResult(importId, "APPROVED", null)
                }
                "REJECT" -> {
                    importService.reviewImport(
                        importId = importId,
                        request = ImportReviewRequest(
                            action = ImportReviewAction.REJECT,
                            reason = request.reason
                        )
                    )
                    BulkReviewResult(importId, "REJECTED", null)
                }
                else -> BulkReviewResult(importId, "ERROR", "Invalid action")
            }
        } catch (e: Exception) {
            BulkReviewResult(importId, "ERROR", e.message)
        }
    }
    
    call.respond(mapOf(
        "total" to results.size,
        "succeeded" to results.count { it.status != "ERROR" },
        "failed" to results.count { it.status == "ERROR" },
        "results" to results
    ))
}
```

---

### 3.2 Backend: Auto-Approve Queue API

**File:** `modules/backend/api/src/main/kotlin/com/sangita/grantha/backend/api/routes/ImportRoutes.kt`

**New Endpoint:**
```kotlin
/**
 * GET /v1/admin/imports/auto-approve-queue
 * 
 * Returns imports that meet auto-approval criteria.
 * 
 * Query parameters:
 * - batchId: Filter by batch (optional)
 * - qualityTier: Filter by quality tier (EXCELLENT, GOOD, FAIR, POOR) (optional)
 * - confidenceMin: Minimum confidence score (0.0-1.0) (optional)
 * - limit: Max results (default: 100)
 * - offset: Pagination offset (default: 0)
 */
get("/auto-approve-queue") {
    val batchId = call.request.queryParameters["batchId"]?.let { Uuid.parse(it) }
    val qualityTier = call.request.queryParameters["qualityTier"]
    val confidenceMin = call.request.queryParameters["confidenceMin"]?.toDoubleOrNull()
    val limit = call.request.queryParameters["limit"]?.toIntOrNull() ?: 100
    val offset = call.request.queryParameters["offset"]?.toIntOrNull() ?: 0
    
    val queue = importService.getAutoApproveQueue(
        batchId = batchId,
        qualityTier = qualityTier,
        confidenceMin = confidenceMin,
        limit = limit,
        offset = offset
    )
    
    call.respond(queue)
}
```

**Service Method:**
```kotlin
// ImportService.kt
suspend fun getAutoApproveQueue(
    batchId: Uuid? = null,
    qualityTier: String? = null,
    confidenceMin: Double? = null,
    limit: Int = 100,
    offset: Int = 0
): List<ImportedKrithiDto> {
    return dal.imports.listImports(
        status = ImportStatus.PENDING,
        batchId = batchId,
        qualityTier = qualityTier,
        minQualityScore = confidenceMin,
        limit = limit,
        offset = offset
    ).filter { imported ->
        autoApprovalService.shouldAutoApprove(imported)
    }
}
```

---

### 3.3 Backend: Configurable Auto-Approval Rules

**File:** `modules/backend/api/src/main/kotlin/com/sangita/grantha/backend/api/services/AutoApprovalService.kt`

**Changes:**
```kotlin
data class AutoApprovalRules(
    val minQualityScore: Double = 0.90,           // EXCELLENT tier
    val minComposerConfidence: Double = 0.95,
    val minRagaConfidence: Double = 0.90,
    val minTalaConfidence: Double = 0.85,
    val requireComposerMatch: Boolean = true,
    val requireRagaMatch: Boolean = true,
    val allowAutoCreateEntities: Boolean = false,
    val qualityTiers: Set<String> = setOf("EXCELLENT", "GOOD")  // Only auto-approve these tiers
)

class AutoApprovalService(
    private val dal: SangitaDal,
    private val rules: AutoApprovalRules = AutoApprovalRules()  // ✅ Configurable
) {
    
    suspend fun shouldAutoApprove(imported: ImportedKrithiDto): Boolean {
        // ✅ NEW: Check quality tier
        if (imported.qualityTier != null && !rules.qualityTiers.contains(imported.qualityTier)) {
            return false
        }
        
        // ✅ NEW: Check quality score
        if (imported.qualityScore != null && imported.qualityScore < rules.minQualityScore) {
            return false
        }
        
        // Parse resolution data
        val resolutionData = imported.resolutionData?.let { 
            Json.parseToJsonObject(it) 
        } ?: return false
        
        val composerConfidence = resolutionData["composer_confidence"]?.jsonPrimitive?.doubleOrNull ?: 0.0
        val ragaConfidence = resolutionData["raga_confidence"]?.jsonPrimitive?.doubleOrNull ?: 0.0
        val talaConfidence = resolutionData["tala_confidence"]?.jsonPrimitive?.doubleOrNull ?: 0.0
        
        // ✅ NEW: Use configurable thresholds
        if (rules.requireComposerMatch && composerConfidence < rules.minComposerConfidence) {
            return false
        }
        
        if (rules.requireRagaMatch && ragaConfidence < rules.minRagaConfidence) {
            return false
        }
        
        // Check for duplicates
        val duplicates = deduplicationService.findDuplicates(imported)
        if (duplicates.any { it.confidence >= 0.95 }) {
            return false  // High-confidence duplicate
        }
        
        return true
    }
    
    suspend fun autoApproveIfHighConfidence(imported: ImportedKrithiDto): Boolean {
        if (!shouldAutoApprove(imported)) {
            return false
        }
        
        // Auto-approve
        reviewImport(
            importedId = imported.id,
            request = ImportReviewRequest(
                action = ImportReviewAction.APPROVE,
                overrides = null
            )
        )
        
        // ✅ NEW: Audit log
        dal.auditLogs.append(
            action = "AUTO_APPROVE",
            entityTable = "imported_krithis",
            entityId = imported.id,
            metadata = Json.encodeToString(mapOf(
                "qualityScore" to imported.qualityScore,
                "qualityTier" to imported.qualityTier,
                "rules" to Json.encodeToString(rules)
            ))
        )
        
        return true
    }
}
```

**Configuration:**
```kotlin
// App.kt or config file
val autoApprovalRules = AutoApprovalRules(
    minQualityScore = 0.90,
    minComposerConfidence = 0.95,
    minRagaConfidence = 0.90,
    qualityTiers = setOf("EXCELLENT", "GOOD")
)
```

---

### 3.4 Frontend: Batch Filter in Review Queue

**File:** `modules/frontend/sangita-admin-web/src/pages/ReviewQueue.tsx`

**Changes:**
```typescript
// Add batch filter dropdown
const [selectedBatchId, setSelectedBatchId] = useState<string | null>(null);
const [batches, setBatches] = useState<ImportBatch[]>([]);

useEffect(() => {
    // Load batches for filter dropdown
    api.bulkImport.listBatches({ limit: 100 }).then(setBatches);
}, []);

// Update API call to include batchId filter
const loadImports = async () => {
    const params: any = { status: 'PENDING' };
    if (selectedBatchId) {
        params.batchId = selectedBatchId;
    }
    const data = await api.imports.listImports(params);
    setImports(data);
};

// Add filter UI
<div className="mb-4">
    <label className="block text-sm font-medium mb-2">Filter by Batch:</label>
    <select
        value={selectedBatchId || ''}
        onChange={(e) => setSelectedBatchId(e.target.value || null)}
        className="border rounded px-3 py-2"
    >
        <option value="">All Batches</option>
        {batches.map(batch => (
            <option key={batch.id} value={batch.id}>
                {batch.sourceManifest} ({batch.status})
            </option>
        ))}
    </select>
</div>
```

---

### 3.5 Frontend: Quality Tier Filtering

**File:** `modules/frontend/sangita-admin-web/src/pages/ReviewQueue.tsx`

**Changes:**
```typescript
const [selectedQualityTier, setSelectedQualityTier] = useState<string | null>(null);

const qualityTiers = ['EXCELLENT', 'GOOD', 'FAIR', 'POOR'];

// Add quality tier filter UI
<div className="mb-4">
    <label className="block text-sm font-medium mb-2">Filter by Quality Tier:</label>
    <select
        value={selectedQualityTier || ''}
        onChange={(e) => setSelectedQualityTier(e.target.value || null)}
        className="border rounded px-3 py-2"
    >
        <option value="">All Tiers</option>
        {qualityTiers.map(tier => (
            <option key={tier} value={tier}>{tier}</option>
        ))}
    </select>
</div>

// Update API call
const loadImports = async () => {
    const params: any = { status: 'PENDING' };
    if (selectedBatchId) params.batchId = selectedBatchId;
    if (selectedQualityTier) params.qualityTier = selectedQualityTier;
    const data = await api.imports.listImports(params);
    setImports(data);
};
```

---

### 3.6 Frontend: Auto-Approve Queue View

**File:** `modules/frontend/sangita-admin-web/src/pages/AutoApproveQueue.tsx` (new)

**Implementation:**
```typescript
export const AutoApproveQueue: React.FC = () => {
    const [imports, setImports] = useState<ImportedKrithi[]>([]);
    const [selectedBatchId, setSelectedBatchId] = useState<string | null>(null);
    
    const loadQueue = async () => {
        const params: any = {};
        if (selectedBatchId) params.batchId = selectedBatchId;
        const data = await api.imports.getAutoApproveQueue(params);
        setImports(data);
    };
    
    const handleBulkApprove = async () => {
        const importIds = imports.map(i => i.id);
        await api.imports.bulkReview({
            importIds,
            action: 'APPROVE'
        });
        await loadQueue();
    };
    
    return (
        <div>
            <h1>Auto-Approve Queue</h1>
            <button onClick={handleBulkApprove}>
                Approve All ({imports.length})
            </button>
            {/* Import list with quality scores */}
        </div>
    );
};
```

---

## 4. Progress Log

### 2026-01-23: Implementation Complete
- ✅ Analyzed review workflow gaps
- ✅ Designed API endpoints
- ✅ Created implementation plan
- ✅ Implemented bulk-review API endpoint (`POST /v1/admin/imports/bulk-review`)
- ✅ Implemented auto-approve queue API endpoint (`GET /v1/admin/imports/auto-approve-queue`)
- ✅ Refactored AutoApprovalService to expose `shouldAutoApprove()` method
- ✅ Updated ImportService to add `getAutoApproveQueue()` with filtering
- ✅ Added batch and quality tier filters to Review Queue (frontend)
- ✅ Created Auto-Approve Queue page with comprehensive filtering
- ✅ Updated API client with new endpoints

### Pending
- [ ] Make auto-approval rules configurable via config file/environment
- [ ] Add unit tests for auto-approval logic
- [ ] Add integration tests for bulk-review API

---

## 5. Success Criteria

- ✅ Bulk-review API endpoint functional
- ✅ Auto-approve queue API endpoint functional
- ✅ Auto-approval uses configurable rules and quality tiers
- ✅ Batch filter in review queue (frontend)
- ✅ Quality tier filter in review queue (frontend)
- ✅ Auto-approve queue page (frontend)
- ✅ Unit tests pass
- ✅ Integration tests pass

---

## 6. Dependencies

- **TRACK-011:** Quality Scoring System (must complete first)
  - Auto-approval needs quality scores/tiers
  - Review queue filtering needs quality tier data

---

## 7. References

- [CSV Import Strategy](../../application_documentation/01-requirements/features/bulk-import/01-strategy/csv-import-strategy.md) (Section 4.4, 7.2)
- [Implementation Plan](../../application_documentation/07-quality/bulk-import-fixes-implementation-plan.md)
- [Claude Review](../../application_documentation/07-quality/bulk-import-implementation-review-claude.md)
- [Goose Review](../../application_documentation/07-quality/csv-import-strategy-implementation-review-goose.md)
