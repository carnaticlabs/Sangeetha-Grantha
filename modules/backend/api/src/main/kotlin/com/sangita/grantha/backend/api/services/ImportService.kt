package com.sangita.grantha.backend.api.services

import com.sangita.grantha.backend.api.models.ImportKrithiRequest
import com.sangita.grantha.backend.api.models.ImportReviewRequest
import com.sangita.grantha.backend.dal.SangitaDal
import com.sangita.grantha.backend.dal.enums.ImportStatus
import com.sangita.grantha.backend.dal.enums.LanguageCode
import com.sangita.grantha.backend.dal.enums.MusicalForm
import com.sangita.grantha.backend.dal.enums.WorkflowState
import com.sangita.grantha.backend.dal.support.toJavaUuid
import com.sangita.grantha.shared.domain.model.ImportedKrithiDto
import com.sangita.grantha.backend.dal.enums.ScriptCode
import com.sangita.grantha.backend.dal.enums.RagaSection
import kotlinx.serialization.json.Json
import java.util.UUID
import kotlin.uuid.Uuid

/**
 * Service for managing imports and their review process.
 * 
 * Implements ImportReviewer interface to allow AutoApprovalService
 * to review imports without creating a circular dependency.
 */
class ImportService(
    private val dal: SangitaDal
) : ImportReviewer {
    private var autoApprovalService: AutoApprovalService? = null
    
    /**
     * Set the auto-approval service after initialization to break circular dependency.
     * This should be called once after both services are created.
     */
    fun setAutoApprovalService(service: AutoApprovalService) {
        this.autoApprovalService = service
    }
    
    private fun normalize(value: String): String =
        value.trim()
            .lowercase()
            .replace(Regex("\\s+"), " ")
            .replace(Regex("[^a-z0-9\\s]"), "")
    suspend fun getImports(status: ImportStatus? = null, batchId: Uuid? = null): List<ImportedKrithiDto> {
        return if (batchId != null) {
            dal.imports.listByBatch(batchId, status)
        } else {
            dal.imports.listImports(status)
        }
    }

    suspend fun approveAllInBatch(batchId: Uuid) {
        val pending = dal.imports.listByBatch(batchId, ImportStatus.PENDING)
        for (importRow in pending) {
            try {
                reviewImport(importRow.id, ImportReviewRequest(status = com.sangita.grantha.shared.domain.model.ImportStatusDto.APPROVED))
            } catch (e: Exception) {
                // Log and continue
            }
        }
    }

    suspend fun rejectAllInBatch(batchId: Uuid) {
        val pending = dal.imports.listByBatch(batchId, ImportStatus.PENDING)
        for (importRow in pending) {
            dal.imports.reviewImport(importRow.id, ImportStatus.REJECTED, null, "Bulk rejected")
        }
    }

    // TRACK-012: Get auto-approve queue with filtering
    suspend fun getAutoApproveQueue(
        batchId: Uuid? = null,
        qualityTier: String? = null,
        confidenceMin: Double? = null,
        limit: Int = 100,
        offset: Int = 0
    ): List<ImportedKrithiDto> {
        // Get pending imports with optional filters
        val allPending = if (batchId != null) {
            dal.imports.listByBatch(batchId, ImportStatus.PENDING)
        } else {
            dal.imports.listImports(ImportStatus.PENDING)
        }

        // Apply filters
        var filtered = allPending

        // Filter by quality tier
        if (qualityTier != null) {
            filtered = filtered.filter { it.qualityTier == qualityTier }
        }

        // Filter by confidence minimum
        if (confidenceMin != null) {
            filtered = filtered.filter {
                val score = it.qualityScore
                score != null && score >= confidenceMin
            }
        }

        // Filter by auto-approval eligibility
        filtered = filtered.filter { imported ->
            try {
                autoApprovalService?.shouldAutoApprove(imported) ?: false
            } catch (e: Exception) {
                false
            }
        }

        // Apply pagination
        return filtered.drop(offset).take(limit)
    }

    suspend fun submitImports(requests: List<ImportKrithiRequest>): List<ImportedKrithiDto> {
        if (requests.isEmpty()) return emptyList()

        val created = mutableListOf<ImportedKrithiDto>()
        for (request in requests) {
            val sourceId = dal.imports.findOrCreateSource(request.source)
            val parsedPayload = request.rawPayload?.toString()
            val importBatchId = request.batchId?.let { java.util.UUID.fromString(it) }
            val importRow = dal.imports.createImport(
                sourceId = sourceId,
                sourceKey = request.sourceKey,
                rawTitle = request.rawTitle,
                rawLyrics = request.rawLyrics,
                rawComposer = request.rawComposer,
                rawRaga = request.rawRaga,
                rawTala = request.rawTala,
                rawDeity = request.rawDeity,
                rawTemple = request.rawTemple,
                rawLanguage = request.rawLanguage,
                parsedPayload = parsedPayload,
                importBatchId = importBatchId
            )

            dal.auditLogs.append(
                action = "IMPORT_KRITHI",
                entityTable = "imported_krithis",
                entityId = importRow.id
            )

            created.add(importRow)
        }

        return created
    }

    override suspend fun reviewImport(id: Uuid, request: ImportReviewRequest): ImportedKrithiDto {
        val mappedId = request.mappedKrithiId?.let { parseUuidOrThrow(it, "mappedKrithiId") }
        val status = ImportStatus.valueOf(request.status.name)
        
        // If status is APPROVED and no mappedKrithiId is provided, create a new krithi
        var createdKrithiId: UUID? = null
        if (status == ImportStatus.APPROVED && mappedId == null) {
            try {
                // Get the import to extract data
                val importData = dal.imports.findById(id) ?: throw NoSuchElementException("Import not found")
                val overrides = request.overrides
                
                // Extract values, prioritizing overrides
                val effectiveComposer = overrides?.composer ?: importData.rawComposer
                val effectiveRaga = overrides?.raga ?: importData.rawRaga
                val effectiveTala = overrides?.tala ?: importData.rawTala
                val effectiveTitle = overrides?.title ?: importData.rawTitle ?: "Untitled"
                val effectiveLanguage = overrides?.language ?: importData.rawLanguage
                val effectiveLyrics = overrides?.lyrics ?: importData.rawLyrics
                val sourceKey = importData.sourceKey
                
                // Find or create composer
                val composerId = if (effectiveComposer != null) {
                    dal.composers.findOrCreate(
                        name = effectiveComposer
                    ).id.toJavaUuid()
                } else {
                    throw IllegalArgumentException("Composer is required to create krithi from import")
                }
                
                // Find or create raga (if provided)
                val ragaId = effectiveRaga?.let { ragaName ->
                    dal.ragas.findOrCreate(
                        name = ragaName
                    ).id.toJavaUuid()
                }
                
                // Find or create tala (if provided)
                val talaId = effectiveTala?.let { talaName ->
                    dal.talas.findOrCreate(
                        name = talaName
                    ).id.toJavaUuid()
                }
                
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
                    title = effectiveTitle,
                    titleNormalized = normalize(effectiveTitle),
                    incipit = null,
                    incipitNormalized = null,
                    composerId = composerId,
                    musicalForm = MusicalForm.KRITHI,
                    primaryLanguage = languageCode,
                    primaryRagaId = ragaId,
                    talaId = talaId,
                    deityId = null, // Deity/temple can be added later in editor
                    templeId = null,
                    isRagamalika = false,
                    ragaIds = if (ragaId != null) listOf(ragaId) else emptyList(),
                    workflowState = WorkflowState.DRAFT,
                    sahityaSummary = effectiveLyrics?.take(500), // First 500 chars as summary
                    notes = "Created from import: ${sourceKey ?: "unknown"}"
                )
                
                createdKrithiId = createdKrithi.id.toJavaUuid()
                
                dal.auditLogs.append(
                    action = "CREATE_KRITHI_FROM_IMPORT",
                    entityTable = "krithis",
                    entityId = createdKrithi.id
                )

                // Process sections and lyrics
                // If overrides.lyrics is present, we use it as the primary lyric variant content (unstructured)
                // If overrides.lyrics is NOT present, we try to use the structured metadata from scrape
                
                if (overrides?.lyrics != null) {
                     dal.krithis.createLyricVariant(
                        krithiId = createdKrithi.id,
                        language = LanguageCode.valueOf(createdKrithi.primaryLanguage.name),
                        script = ScriptCode.LATIN,
                        lyrics = overrides.lyrics,
                        isPrimary = true,
                        sourceReference = sourceKey
                    )
                } else if (importData.parsedPayload != null) {
                    try {
                        val metadata = Json.decodeFromString<ScrapedKrithiMetadata>(importData.parsedPayload!!)
                        
                        // 1. Save Structure (Sections)
                        if (!metadata.sections.isNullOrEmpty()) {
                            val sectionsToSave = metadata.sections.mapIndexed { index, section ->
                                Triple(section.type.name, index + 1, null as String?)
                            }
                            
                            dal.krithis.saveSections(createdKrithi.id, sectionsToSave)
                            
                            // 2. Get the saved sections to get their IDs
                            val savedSections = dal.krithis.getSections(createdKrithi.id)
                            
                            // 3. Create Lyric Variant
                            val lyricVariant = dal.krithis.createLyricVariant(
                                krithiId = createdKrithi.id,
                                language = LanguageCode.valueOf(createdKrithi.primaryLanguage.name),
                                script = ScriptCode.LATIN, // Defaulting to Latin/Diacritic for scraped content
                                lyrics = metadata.lyrics ?: "",
                                isPrimary = true,
                                sourceReference = sourceKey
                            )
                            
                            // 4. Map text to section IDs and save content
                            val lyricSections = savedSections.mapNotNull { savedSection ->
                                val originalSection = metadata.sections.getOrNull(savedSection.orderIndex - 1)
                                if (originalSection != null && !originalSection.text.isBlank()) {
                                    savedSection.id.toJavaUuid() to originalSection.text
                                } else {
                                    null
                                }
                            }
                            
                            if (lyricSections.isNotEmpty()) {
                                dal.krithis.saveLyricVariantSections(lyricVariant.id, lyricSections)
                            }
                        } else if (!metadata.lyrics.isNullOrBlank()) {
                             // Fallback if no sections but we have lyrics
                             dal.krithis.createLyricVariant(
                                krithiId = createdKrithi.id,
                                language = LanguageCode.valueOf(createdKrithi.primaryLanguage.name),
                                script = ScriptCode.LATIN,
                                lyrics = metadata.lyrics,
                                isPrimary = true,
                                sourceReference = sourceKey
                            )
                        }
                    } catch (e: Exception) {
                        // Log error but don't fail the whole import
                        println("Error processing scraped metadata: ${e.message}")
                        e.printStackTrace()
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

    private fun parseUuidOrThrow(value: String, label: String): UUID =
        try {
            UUID.fromString(value)
        } catch (ex: IllegalArgumentException) {
            throw IllegalArgumentException("Invalid $label")
        }

    // TRACK-004: Finalize Batch
    suspend fun finalizeBatch(batchId: Uuid): Map<String, Any> {
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
    suspend fun generateQAReport(batchId: Uuid, format: String): String {
        val imports = dal.imports.listByBatch(batchId, null)
        val batch = dal.bulkImport.findBatchById(batchId)

        return when (format.lowercase()) {
            "json" -> generateJsonReport(batch, imports)
            "csv" -> generateCsvReport(batch, imports)
            else -> throw IllegalArgumentException("Unsupported format: $format")
        }
    }

    private fun generateJsonReport(batch: Any?, imports: List<com.sangita.grantha.shared.domain.model.ImportedKrithiDto>): String {
        val avgQualityScore: Double? = imports.mapNotNull { it.qualityScore }.average().takeIf { !it.isNaN() }
        val qualityTierCounts: Map<String?, Int> = imports.groupBy { it.qualityTier }.mapValues { it.value.size }
        
        // Extract batch info safely
        val batchIdStr = when (batch) {
            is com.sangita.grantha.shared.domain.model.ImportBatchDto -> batch.id.toString()
            else -> null
        }
        val sourceManifest = when (batch) {
            is com.sangita.grantha.shared.domain.model.ImportBatchDto -> batch.sourceManifest
            else -> null
        }
        
        val summary = mapOf<String, Any?>(
            "batchId" to batchIdStr,
            "sourceManifest" to sourceManifest,
            "totalImports" to imports.size,
            "approved" to imports.count { it.importStatus == com.sangita.grantha.shared.domain.model.ImportStatusDto.APPROVED },
            "rejected" to imports.count { it.importStatus == com.sangita.grantha.shared.domain.model.ImportStatusDto.REJECTED },
            "pending" to imports.count { it.importStatus == com.sangita.grantha.shared.domain.model.ImportStatusDto.PENDING },
            "avgQualityScore" to avgQualityScore,
            "qualityTierCounts" to qualityTierCounts
        )

        val items = imports.map { import ->
            mapOf(
                "id" to import.id.toString(),
                "title" to import.rawTitle,
                "composer" to import.rawComposer,
                "raga" to import.rawRaga,
                "tala" to import.rawTala,
                "status" to import.importStatus.name,
                "qualityScore" to import.qualityScore,
                "qualityTier" to import.qualityTier,
                "sourceKey" to import.sourceKey
            )
        }

        return Json.encodeToString(
            mapOf(
                "summary" to summary,
                "items" to items
            )
        )
    }

    private fun generateCsvReport(batch: Any?, imports: List<com.sangita.grantha.shared.domain.model.ImportedKrithiDto>): String {
        val header = "ID,Title,Composer,Raga,Tala,Status,Quality Score,Quality Tier,Source\n"
        val rows = imports.joinToString("\n") { import ->
            listOf(
                import.id.toString(),
                escapeCsv(import.rawTitle ?: ""),
                escapeCsv(import.rawComposer ?: ""),
                escapeCsv(import.rawRaga ?: ""),
                escapeCsv(import.rawTala ?: ""),
                import.importStatus.name,
                import.qualityScore?.toString() ?: "",
                import.qualityTier ?: "",
                escapeCsv(import.sourceKey ?: "")
            ).joinToString(",")
        }
        return header + rows
    }

    private fun escapeCsv(value: String): String {
        return if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            "\"${value.replace("\"", "\"\"")}\""
        } else {
            value
        }
    }
}
