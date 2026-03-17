package com.sangita.grantha.backend.api.services

import com.sangita.grantha.backend.api.models.ImportKrithiRequest
import com.sangita.grantha.backend.api.models.ImportReviewRequest
import com.sangita.grantha.backend.api.support.toJavaUuidOrThrow
import com.sangita.grantha.backend.dal.SangitaDal
import com.sangita.grantha.backend.dal.enums.ImportStatus
import com.sangita.grantha.backend.api.config.ApiEnvironment
import com.sangita.grantha.backend.dal.enums.LanguageCode
import com.sangita.grantha.backend.dal.enums.MusicalForm
import com.sangita.grantha.backend.dal.enums.WorkflowState
import com.sangita.grantha.backend.dal.support.toJavaUuid
import com.sangita.grantha.backend.dal.support.toKotlinUuid
import com.sangita.grantha.shared.domain.model.ImportedKrithiDto
import com.sangita.grantha.backend.dal.repositories.KrithiCreateParams
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import java.net.URI
import java.util.UUID
import kotlin.uuid.Uuid

/**
 * Service for managing imports and their review process.
 * 
 * Implements ImportReviewer interface to allow AutoApprovalService
 * to review imports without creating a circular dependency.
 */
interface IImportService {
    /**
     * List imports optionally filtered by status and/or batch.
     */
    suspend fun getImports(status: ImportStatus? = null, batchId: Uuid? = null, limit: Int? = null, offset: Int = 0): List<ImportedKrithiDto>

    /**
     * Approve all pending imports in the batch.
     */
    suspend fun approveAllInBatch(batchId: Uuid)

    /**
     * Reject all pending imports in the batch.
     */
    suspend fun rejectAllInBatch(batchId: Uuid)

    /**
     * Fetch imports eligible for auto-approval with optional filters.
     */
    suspend fun getAutoApproveQueue(
        batchId: Uuid? = null,
        qualityTier: String? = null,
        confidenceMin: Double? = null,
        limit: Int = 100,
        offset: Int = 0
    ): List<ImportedKrithiDto>

    /**
     * Submit one or more imports to the system.
     */
    suspend fun submitImports(requests: List<ImportKrithiRequest>): List<ImportedKrithiDto>

    /**
     * Review an import and optionally create/map a krithi.
     */
    suspend fun reviewImport(id: Uuid, request: ImportReviewRequest): ImportedKrithiDto

    /**
     * Calculate summary stats for a batch to decide if it can be finalized.
     */
    suspend fun finalizeBatch(batchId: Uuid): Map<String, Any>

    /**
     * Generate a QA report for a batch in JSON or CSV.
     */
    suspend fun generateQAReport(batchId: Uuid, format: String): String
}

class ImportServiceImpl(
    private val dal: SangitaDal,
    private val environment: ApiEnvironment,
    private val entityResolver: IEntityResolver,
    private val normalizer: NameNormalizationService,
    private val reportGenerator: ImportReportGenerator,
    private val lyricVariantPersistence: LyricVariantPersistenceService,
    private val autoApprovalServiceProvider: () -> AutoApprovalService
) : ImportReviewer, IImportService {
    
    /**
     * TRACK-062: Delegate to NameNormalizationService for transliteration-aware normalisation.
     * Used for title dedup and temple name matching.
     */
    private fun normalize(value: String): String =
        normalizer.normalizeTitle(value) ?: value.trim().lowercase()

    private fun parseBatchIdOrNull(batchId: String?): UUID? =
        batchId?.let {
            try {
                UUID.fromString(it)
            } catch (_: Exception) {
                null
            }
        }

    private fun inferSourceFormat(sourceKey: String?): String? {
        val url = sourceKey?.trim()?.lowercase() ?: return null
        if (url.isBlank()) return null

        return when {
            url.endsWith(".pdf") -> "PDF"
            url.endsWith(".docx") -> "DOCX"
            url.endsWith(".jpg") || url.endsWith(".jpeg") || url.endsWith(".png") || url.endsWith(".webp") -> "IMAGE"
            url.startsWith("http://") || url.startsWith("https://") -> "HTML"
            else -> null
        }
    }

    private fun shouldEnqueueHtmlExtraction(request: ImportKrithiRequest, existingImport: ImportedKrithiDto?): Boolean {
        if (existingImport != null) return false
        if (request.rawPayload != null) return false
        if (!request.rawLyrics.isNullOrBlank()) return false
        return inferSourceFormat(request.sourceKey) == "HTML"
    }

    private fun mapToIsoLanguage(rawLanguage: String?): String? = when (rawLanguage?.trim()?.lowercase()) {
        "sa", "sanskrit" -> "sa"
        "ta", "tamil" -> "ta"
        "te", "telugu" -> "te"
        "kn", "kannada" -> "kn"
        "ml", "malayalam" -> "ml"
        "hi", "hindi" -> "hi"
        "en", "english" -> "en"
        else -> null
    }

    override suspend fun getImports(status: ImportStatus?, batchId: Uuid?, limit: Int?, offset: Int): List<ImportedKrithiDto> {
        return if (batchId != null) {
            dal.imports.listByBatch(batchId, status)
        } else {
            dal.imports.listImports(status, limit, offset)
        }
    }

    override suspend fun approveAllInBatch(batchId: Uuid) {
        TODO("Not yet implemented")
    }

    override suspend fun rejectAllInBatch(batchId: Uuid) {
        TODO("Not yet implemented")
    }

    override suspend fun getAutoApproveQueue(
        batchId: Uuid?,
        qualityTier: String?,
        confidenceMin: Double?,
        limit: Int,
        offset: Int
    ): List<ImportedKrithiDto> {
        return emptyList()
    }

    override suspend fun submitImports(requests: List<ImportKrithiRequest>): List<ImportedKrithiDto> {
        return requests.map { request ->
            // Find or create the import source
            val sourceId = dal.imports.findOrCreateSource(request.source)
            val sourceKey = request.sourceKey?.trim()?.takeIf { it.isNotBlank() }
            val importBatchId = parseBatchIdOrNull(request.batchId)

            // createImport is idempotent (returns existing if source+key match),
            // so we rely on it for dedup instead of a separate findBySourceAndKey
            // which was susceptible to read-then-write race conditions.
            val importDto = dal.imports.createImport(
                sourceId = sourceId,
                sourceKey = sourceKey,
                rawTitle = request.rawTitle,
                rawLyrics = request.rawLyrics,
                rawComposer = request.rawComposer,
                rawRaga = request.rawRaga,
                rawTala = request.rawTala,
                rawDeity = request.rawDeity,
                rawTemple = request.rawTemple,
                rawLanguage = request.rawLanguage,
                parsedPayload = request.rawPayload?.toString(),
                importBatchId = importBatchId
            )

            // TRACK-064: HTML-first import flow.
            // When import payload has just the source URL (no scraped payload yet),
            // enqueue extraction_queue task and let Python perform HTML parsing.
            // Guard: check extraction_queue to avoid double-enqueuing from concurrent ScrapeWorker calls.
            val alreadyEnqueued = sourceKey?.let { url ->
                dal.extractionQueue.existsBySourceUrl(url)
            } ?: false
            if (!alreadyEnqueued && shouldEnqueueHtmlExtraction(request, null) && sourceKey != null) {
                val queuePayload = buildJsonObject {
                    put("importId", importDto.id.toString())
                    put("source", "import-flow")
                    request.rawTitle?.takeIf { it.isNotBlank() }?.let { put("titleHint", it) }
                    request.rawComposer?.takeIf { it.isNotBlank() }?.let { put("composerHint", it) }
                    request.rawRaga?.takeIf { it.isNotBlank() }?.let { put("ragaHint", it) }
                }.toString()

                val extraction = dal.extractionQueue.create(
                    sourceUrl = sourceKey,
                    sourceFormat = "HTML",
                    importBatchId = importBatchId,
                    composerHint = request.rawComposer,
                    requestPayload = queuePayload,
                    contentLanguage = mapToIsoLanguage(request.rawLanguage),
                )

                dal.auditLogs.append(
                    action = "ENQUEUE_HTML_EXTRACTION_FROM_IMPORT",
                    entityTable = "extraction_queue",
                    entityId = extraction.id,
                    metadata = """{"importId":"${importDto.id}","sourceKey":"$sourceKey"}""",
                )
            }
            
            // Trigger entity resolution for the newly created import
            val hasInlineMetadata = !request.rawTitle.isNullOrBlank()
                || !request.rawComposer.isNullOrBlank()
                || !request.rawRaga.isNullOrBlank()
                || !request.rawTala.isNullOrBlank()
                || !request.rawDeity.isNullOrBlank()
                || !request.rawTemple.isNullOrBlank()
                || !request.rawLanguage.isNullOrBlank()
                || request.rawPayload != null

            if (hasInlineMetadata) {
                val resolutionResult = entityResolver.resolve(importDto)

                // Save the resolution data back to the import record
                val resolutionJson = Json.encodeToString(resolutionResult)
                dal.imports.saveResolution(importDto.id, resolutionJson)
            }
            
            // Return the updated import with resolution data
            dal.imports.findById(importDto.id) ?: importDto
        }
    }
// ... [Retaining existing methods until reviewImport] ...

    override suspend fun reviewImport(id: Uuid, request: ImportReviewRequest): ImportedKrithiDto {
        val mappedId = request.mappedKrithiId?.toJavaUuidOrThrow("mappedKrithiId")
        val status = ImportStatus.valueOf(request.status.name)
        
        // If status is APPROVED and no mappedKrithiId is provided, create a new krithi
        var createdKrithiId: UUID? = null
        if (status == ImportStatus.APPROVED && mappedId == null) {
            try {
                // Get the import to extract data
                val importData = dal.imports.findById(id) ?: throw NoSuchElementException("Import not found")
                val overrides = request.overrides
                
                // Extract values, prioritizing overrides, then raw data, then scraped payload
                val metadata = importData.parsedPayload?.let { 
                    try { 
                        Json.decodeFromString<ScrapedKrithiMetadata>(it) 
                    } catch (e: Exception) { 
                        null 
                    } 
                }

                val effectiveComposer = overrides?.composer ?: importData.rawComposer ?: metadata?.composer
                val effectiveRaga = overrides?.raga ?: importData.rawRaga ?: metadata?.raga
                val effectiveTala = overrides?.tala ?: importData.rawTala ?: metadata?.tala
                val effectiveTitle = overrides?.title ?: importData.rawTitle ?: metadata?.title ?: "Untitled"
                val effectiveLanguage = overrides?.language ?: importData.rawLanguage ?: metadata?.language
                val effectiveLyrics = (overrides?.lyrics ?: importData.rawLyrics ?: metadata?.lyrics)?.replace("\\n", "\n")
                
                // Deity/Temple are handled below
                val sourceKey = importData.sourceKey
                
                // Find or create composer
                val composerId = if (effectiveComposer != null) {
                    dal.composers.findOrCreate(
                        name = effectiveComposer,
                        nameNormalized = effectiveComposer.let { normalizer.normalizeComposer(it) }
                    ).id.toJavaUuid()
                } else {
                    throw IllegalArgumentException("Composer is required to create krithi from import")
                }
                
                // Find or create raga (if provided)
                val ragaId = effectiveRaga?.let { ragaName ->
                    dal.ragas.findOrCreate(
                        name = ragaName,
                        nameNormalized = ragaName.let { normalizer.normalizeRaga(it) }
                    ).id.toJavaUuid()
                }
                
                // Find or create tala (if provided)
                val talaId = effectiveTala?.let { talaName ->
                    dal.talas.findOrCreate(
                        name = talaName,
                        nameNormalized = talaName.let { normalizer.normalizeTala(it) }
                    ).id.toJavaUuid()
                }

                val effectiveDeity = overrides?.deity ?: importData.rawDeity
                val effectiveTemple = overrides?.temple ?: importData.rawTemple

                // Resolve/Create Deity and Temple
                var deityId: UUID? = null
                var templeId: UUID? = null
                
                // 0. Manual Override or Raw Data (Highest Priority if manually set/confirmed)
                if (overrides?.deity != null && overrides.deity.isNotBlank()) {
                     val dName = overrides.deity
                     deityId = dal.deities.findByName(dName)?.id?.toJavaUuid() 
                        ?: dal.deities.create(name = dName).id.toJavaUuid()
                }
                
                if (overrides?.temple != null && overrides.temple.isNotBlank()) {
                     val tName = overrides.temple
                     val existing = dal.temples.findByName(tName) ?: dal.temples.findByNameNormalized(normalize(tName))
                     templeId = existing?.id?.toJavaUuid() 
                        ?: dal.temples.create(name = tName, primaryDeityId = deityId).id.toJavaUuid()
                }

                // 1. Try from Resolution Data (High Confidence Matches) - Only if not already set by override
                if (deityId == null && importData.resolutionData != null) {
                    try {
                        val resolution = Json.decodeFromString<ResolutionResult>(importData.resolutionData!!)
                        deityId = resolution.deityCandidates.firstOrNull { it.confidence == "HIGH" }?.entity?.id?.toJavaUuid()
                        templeId = resolution.templeCandidates.firstOrNull { it.confidence == "HIGH" }?.entity?.id?.toJavaUuid()
                    } catch (e: Exception) {
                        // Ignore resolution parsing errors
                    }
                }

                // 2. Fallback to Auto-Creation based on Scraped Metadata or Cache
                if ((deityId == null || templeId == null) && importData.parsedPayload != null) {
                    try {
                        val metadata = Json.decodeFromString<ScrapedKrithiMetadata>(importData.parsedPayload!!)
                        
                        // Check confidence via TempleSourceCache if URL is present (The Source of Truth)
                        var canAutoCreate = false
                        var cachedDetails: com.sangita.grantha.backend.dal.repositories.TempleSourceCacheDto? = null
                        
                        if (!metadata.templeUrl.isNullOrBlank()) {
                            cachedDetails = dal.templeSourceCache.findByUrl(metadata.templeUrl)
                            if (cachedDetails != null) {
                                canAutoCreate = cachedDetails.error == null && environment.templeAutoCreateConfidence <= 1.0
                            }
                        } else if (metadata.templeDetails != null) {
                             canAutoCreate = true 
                        }

                        if (canAutoCreate) {
                            val details = metadata.templeDetails

                            if (details != null) {
                                // Deity
                                if (deityId == null && !details.deity.isNullOrBlank()) {
                                    val existingDeity = dal.deities.findByName(details.deity)
                                    deityId = existingDeity?.id?.toJavaUuid() ?: dal.deities.create(
                                        name = details.deity,
                                        description = "Imported from ${metadata.templeUrl ?: "scrape"}"
                                    ).id.toJavaUuid()
                                }

                                // Temple
                                if (templeId == null && !details.name.isBlank()) {
                                    val existingTemple = dal.temples.findByName(details.name) ?:
                                        dal.temples.findByNameNormalized(normalize(details.name))
                                    
                                    if (existingTemple != null) {
                                        templeId = existingTemple.id.toJavaUuid()
                                    } else {
                                        // Use cached geocoding if available, else parsed details
                                        val lat = cachedDetails?.latitude ?: details.latitude
                                        val lon = cachedDetails?.longitude ?: details.longitude
                                        val loc = cachedDetails?.city ?: details.location
                                        
                                        templeId = dal.temples.create(
                                            name = details.name,
                                            city = loc,
                                            primaryDeityId = deityId,
                                            latitude = lat,
                                            longitude = lon,
                                            notes = details.description
                                        ).id.toJavaUuid()
                                    }
                                }
                            }
                        }
                    } catch (e: Exception) {
                        println("Error processing scraped metadata for deity/temple: ${e.message}")
                    }
                }
                
                // TRACK-062: Deduplication Check
                // Before creating, check if a krithi with same title, composer, and raga exists.
                // Search WITHOUT composer filter first (cross-composer dedup), then WITH.
                val normalizedTitle = normalize(effectiveTitle)

                val allCandidates = dal.krithiSearch.findDuplicateCandidates(
                    titleNormalized = normalizedTitle
                )
                val composerCandidates = dal.krithiSearch.findDuplicateCandidates(
                    titleNormalized = normalizedTitle,
                    composerId = composerId,
                    ragaId = ragaId
                )

                val candidates = (composerCandidates + allCandidates).distinctBy { it.id }

                val existingKrithi = candidates
                    .map { candidate ->
                        val titleScore = NameNormalizationService.ratio(normalizedTitle, candidate.titleNormalized)
                        val compressedScore = NameNormalizationService.ratio(
                            normalizedTitle.replace(" ", ""),
                            candidate.titleNormalized.replace(" ", "")
                        )
                        val bestScore = maxOf(titleScore, compressedScore)
                        val composerMatch = candidate.composerId.toJavaUuid() == composerId
                        candidate to (bestScore to composerMatch)
                    }
                    .filter { (_, scores) ->
                        val (score, composerMatch) = scores
                        if (composerMatch) score > 75 else score > 90
                    }
                    .maxByOrNull { it.second.first }
                    ?.first
                
                if (existingKrithi != null) {
                    // Match found! Use existing ID and skip creation
                    createdKrithiId = existingKrithi.id.toJavaUuid()
                    
                    // TRACK-062: Persist lyrics from this import as a new variant for the existing Krithi
                    lyricVariantPersistence.persistLyricVariants(existingKrithi.id, importData, request.overrides)

                    dal.auditLogs.append(
                        action = "LINK_IMPORT_TO_EXISTING_KRITHI",
                        entityTable = "krithis",
                        entityId = existingKrithi.id,
                        metadata = """{"matchedImportId":"$id","existingKrithiId":"${existingKrithi.id}"}"""
                    )
                } else {
                    // Start creation flow
                    
                    // Determine language code (default to TE if not provided)
                    val languageCode = when (effectiveLanguage?.lowercase()) {
                        "sanskrit", "sa" -> LanguageCode.SA
                        "tamil", "ta" -> LanguageCode.TA
                        "telugu", "te" -> LanguageCode.TE
                        "kannada", "kn" -> LanguageCode.KN
                        "malayalam", "ml" -> LanguageCode.ML
                        "hindi", "hi" -> LanguageCode.HI
                        "english", "en" -> LanguageCode.EN
                        else -> LanguageCode.TE // Default
                    }
                    
                    // Create the krithi
                    val createdKrithi = dal.krithis.create(
                        KrithiCreateParams(
                            title = effectiveTitle,
                            titleNormalized = normalizedTitle,
                            incipit = null,
                            incipitNormalized = null,
                            composerId = composerId,
                            musicalForm = MusicalForm.KRITHI,
                            primaryLanguage = languageCode,
                            primaryRagaId = ragaId,
                            talaId = talaId,
                            deityId = deityId,
                            templeId = templeId,
                            isRagamalika = false,
                            ragaIds = if (ragaId != null) listOf(ragaId) else emptyList(),
                            workflowState = WorkflowState.DRAFT,
                            sahityaSummary = effectiveLyrics?.take(500), // First 500 chars as summary
                            notes = "Created from import: ${sourceKey ?: "unknown"}"
                        )
                    )
                    
                    createdKrithiId = createdKrithi.id.toJavaUuid()
                    
                    dal.auditLogs.append(
                        action = "CREATE_KRITHI_FROM_IMPORT",
                        entityTable = "krithis",
                        entityId = createdKrithi.id
                    )
    
                    // Process sections and lyrics
                    lyricVariantPersistence.persistLyricVariants(createdKrithi.id, importData, request.overrides)
                }
                
                // TRACK-062: Create Source Evidence
                // Whether we created a new Krithi or matched an existing one, we should record the evidence.
                if (createdKrithiId != null) {
                    try {
                        val krithiId = createdKrithiId.toKotlinUuid()
                        val sourceUrl = sourceKey ?: "manual-import"
                        
                        // Heuristic to determine source name from URL
                        val sourceName = try {
                            if (sourceUrl.startsWith("http")) {
                                URI(sourceUrl).host?.lowercase() ?: "unknown-source"
                            } else {
                                "manual"
                            }
                        } catch (e: Exception) {
                            "unknown-source"
                        }

                        dal.sourceEvidence.createEvidence(
                            krithiId = krithiId,
                            sourceUrl = sourceUrl,
                            sourceName = sourceName,
                            sourceTier = 3, // Default to crowdsourced/low tier for imports
                            sourceFormat = "HTML",
                            extractionMethod = "HTML_JSOUP",
                            confidence = 1.0,
                            contributedFields = listOfNotNull(
                                "title".takeIf { effectiveTitle.isNotBlank() },
                                "composer".takeIf { effectiveComposer != null },
                                "raga".takeIf { effectiveRaga != null },
                                "tala".takeIf { effectiveTala != null },
                                "lyrics".takeIf { effectiveLyrics != null || importData.rawLyrics != null }
                            )
                        )
                    } catch (e: Exception) {
                        println("Failed to create source evidence: ${e.message}")
                    }
                }

            } catch (e: Exception) {
                // If it's a known error, rethrow it so route can handle it (or wrap it)
                // If it's unknown, wrap it in a custom exception that route can map to meaningful message
                throw RuntimeException("Failed to create krithi: ${e.message}", e)
            }
        }
        
        val updated = dal.imports.reviewImport(
            id = id,
            status = status,
            mappedKrithiId = createdKrithiId ?: mappedId,
            reviewerNotes = request.reviewerNotes
        ) ?: throw NoSuchElementException("Import not found")

        dal.auditLogs.append(
            action = "REVIEW_IMPORT",
            entityTable = "imported_krithis",
            entityId = updated.id
        )

        return updated
    }

    // TRACK-004: Finalize Batch
    override suspend fun finalizeBatch(batchId: Uuid): Map<String, Any> {
        val imports = dal.imports.listByBatch(batchId, null)

        val total = imports.size
        val approved = imports.count { it.importStatus == com.sangita.grantha.shared.domain.model.ImportStatusDto.APPROVED }
        val rejected = imports.count { it.importStatus == com.sangita.grantha.shared.domain.model.ImportStatusDto.REJECTED }
        val pending = imports.count { it.importStatus == com.sangita.grantha.shared.domain.model.ImportStatusDto.PENDING }

        // Calculate quality metrics
        val avgQualityScore: Double? = imports.mapNotNull { it.qualityScore }.average().takeIf { !it.isNaN() }
        val qualityTierCounts: Map<String?, Int> = imports.groupBy { it.qualityTier }.mapValues { it.value.size }

        // Check if batch can be finalized
        val canFinalize = pending == 0

        return mapOf(
            "batchId" to batchId.toString(),
            "total" to total,
            "approved" to approved,
            "rejected" to rejected,
            "pending" to pending,
            "canFinalize" to canFinalize,
            "avgQualityScore" to (avgQualityScore ?: 0.0),
            "qualityTierCounts" to qualityTierCounts,
            "message" to if (canFinalize) "Batch ready to finalize" else "Cannot finalize: $pending items still pending review"
        )
    }

    // TRACK-004: Generate QA Report
    override suspend fun generateQAReport(batchId: Uuid, format: String): String {
        val imports = dal.imports.listByBatch(batchId, null)
        val batch = dal.bulkImport.findBatchById(batchId)

        return when (format.lowercase()) {
            "json" -> reportGenerator.generateJsonReport(batch, imports)
            "csv" -> reportGenerator.generateCsvReport(batch, imports)
            else -> throw IllegalArgumentException("Unsupported format: $format")
        }
    }

}
