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
import com.sangita.grantha.shared.domain.model.ImportedKrithiDto
import com.sangita.grantha.shared.domain.model.RagaSectionDto
import com.sangita.grantha.backend.dal.enums.ScriptCode
import com.sangita.grantha.backend.dal.enums.RagaSection
import com.sangita.grantha.backend.dal.repositories.KrithiCreateParams
import com.sangita.grantha.backend.api.services.scraping.KrithiStructureParser
import com.sangita.grantha.backend.api.services.scraping.StructuralVotingEngine
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
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
    suspend fun getImports(status: ImportStatus? = null, batchId: Uuid? = null): List<ImportedKrithiDto>

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
    private val autoApprovalServiceProvider: () -> AutoApprovalService
) : ImportReviewer, IImportService {
    private val structuralVotingEngine = StructuralVotingEngine()
    private val composerSourcePriority = mapOf(
        "muthuswami dikshitar" to listOf("guruguha.org"),
        "tyagaraja" to listOf("thyagarajavaibhavam.blogspot.com"),
        "swathi thirunal" to listOf("swathithirunalfestival.org"),
        "general" to listOf("karnatik.com", "shivkumar.org")
    )
    
    private fun normalize(value: String): String =
        value.trim()
            .lowercase()
            .replace(Regex("\\s+"), " ")
            .replace(Regex("[^a-z0-9\\s]"), "")
    override suspend fun getImports(status: ImportStatus?, batchId: Uuid?): List<ImportedKrithiDto> {
        return if (batchId != null) {
            dal.imports.listByBatch(batchId, status)
        } else {
            dal.imports.listImports(status)
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
            
            // Create the import record
            val importDto = dal.imports.createImport(
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
                parsedPayload = request.rawPayload?.toString(),
                importBatchId = request.batchId?.let { 
                    try { 
                        java.util.UUID.fromString(it) 
                    } catch (e: Exception) { 
                        null 
                    } 
                }
            )
            
            // Trigger entity resolution for the newly created import
            val resolutionResult = entityResolver.resolve(importDto)
            
            // Save the resolution data back to the import record
            val resolutionJson = Json.encodeToString(resolutionResult)
            dal.imports.saveResolution(importDto.id, resolutionJson)
            
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

                val effectiveDeity = overrides?.deity ?: importData.rawDeity
                val effectiveTemple = overrides?.temple ?: importData.rawTemple

                // Resolve/Create Deity and Temple
                var deityId: UUID? = null
                var templeId: UUID? = null
                
                // 0. Manual Override or Raw Data (Highest Priority if manually set/confirmed)
                // Note: Currently we don't have a way to distinguish "user confirmed raw" vs "raw". 
                // But we can check if explicit override was sent.
                // For now, if override is present, we try to find/create based on that name.
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
                                // Implicit "HIGH" confidence if scraping succeeded, as we don't store numeric confidence for the scrape itself yet.
                                // But we can assume if Geocoding succeeded reasonably, it's safer.
                                // For now, we allow auto-creation if we have a cache hit, enforcing the environment threshold mostly on resolution logic if we had it,
                                // but here we trust the scraped source if configured to do so.
                                // Actually, environment.templeAutoCreateConfidence is a Double. Let's assume cache existence = 1.0 confidence for now if error is null.
                                canAutoCreate = cachedDetails.error == null && environment.templeAutoCreateConfidence <= 1.0
                            }
                        } else if (metadata.templeDetails != null) {
                             // Direct scraped details without external URL (less likely with new logic but possible)
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
                        titleNormalized = normalize(effectiveTitle),
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

                        // TRACK-032: Multi-language lyric variants from scrape
                        if (!metadata.lyricVariants.isNullOrEmpty()) {
                            val variants = metadata.lyricVariants.toMutableList()
                            
                            // 1. Recover missing sections for each variant using KrithiStructureParser
                            variants.forEachIndexed { idx, v ->
                                if (v.sections.isNullOrEmpty() && !v.lyrics.isNullOrBlank()) {
                                    val recovered = KrithiStructureParser().buildBlocks(v.lyrics.replace("\\n", "\n")).blocks
                                        .mapNotNull { block: KrithiStructureParser.TextBlock ->
                                            val type = parseRagaSectionDto(block.label)
                                            if (type != null && type != RagaSectionDto.OTHER) {
                                                ScrapedSectionDto(type = type, text = block.lines.joinToString("\n"))
                                            } else null
                                        }
                                    if (recovered.isNotEmpty()) {
                                        variants[idx] = v.copy(sections = recovered)
                                    }
                                }
                            }

                            // 2. Determine canonical section structure
                            // Deduplicate metadata.sections if they are concatenated language blocks
                            val rawSections = metadata.sections ?: emptyList()
                            val deduplicated = mutableListOf<ScrapedSectionDto>()
                            val seenTypes = mutableListOf<String>()
                            
                            // Heuristic: If we see a PALLAVI again after a CHARANAM, it's likely a new language block.
                            // We only take the first complete block.
                            var stopDedupe = false
                            for (s in rawSections) {
                                if (s.type == RagaSectionDto.PALLAVI && seenTypes.contains("CHARANAM")) {
                                    stopDedupe = true
                                }
                                if (stopDedupe) break
                                deduplicated.add(s)
                                seenTypes.add(s.type.name)
                            }

                            val authoritySource = isAuthoritySourceForComposer(sourceKey, effectiveComposer)
                            val candidates = mutableListOf<StructuralVotingEngine.SectionCandidate>()
                            variants.forEach { variant ->
                                val sections = variant.sections
                                if (!sections.isNullOrEmpty()) {
                                    candidates.add(
                                        StructuralVotingEngine.SectionCandidate(
                                            sections = sections,
                                            isAuthoritySource = authoritySource,
                                            label = "variant:${variant.language}"
                                        )
                                    )
                                }
                            }
                            if (deduplicated.isNotEmpty()) {
                                candidates.add(
                                    StructuralVotingEngine.SectionCandidate(
                                        sections = deduplicated,
                                        isAuthoritySource = authoritySource,
                                        label = "metadata"
                                    )
                                )
                            }

                            val sectionStructure = structuralVotingEngine.pickBestStructure(candidates)

                            if (sectionStructure.isNotEmpty()) {
                                val sectionsToSave = sectionStructure.mapIndexed { index, section ->
                                    Triple(section.type.name, index + 1, null as String?)
                                }
                                dal.krithis.saveSections(createdKrithi.id, sectionsToSave)
                            }
                            
                            val savedSections = dal.krithis.getSections(createdKrithi.id)
                            
                            // 3. Save each variant and link to sections
                            variants.forEachIndexed { index, scraped ->
                                val lang = parseLanguageCode(scraped.language) ?: LanguageCode.valueOf(createdKrithi.primaryLanguage.name)
                                val script = parseScriptCode(scraped.script) ?: ScriptCode.LATIN
                                val lyricsText = (scraped.lyrics?.takeIf { it.isNotBlank() }
                                    ?: scraped.sections?.joinToString("\n\n") { "[${it.type.name}]\n${it.text}" }.takeIf { it?.isNotBlank() == true }
                                    ?: "").replace("\\n", "\n")
                                
                                val createdVariant = dal.krithis.createLyricVariant(
                                    krithiId = createdKrithi.id,
                                    language = lang,
                                    script = script,
                                    lyrics = lyricsText,
                                    isPrimary = index == 0,
                                    sourceReference = sourceKey
                                )
                                
                                val variantSections = scraped.sections
                                if (!variantSections.isNullOrEmpty() && savedSections.isNotEmpty()) {
                                    val lyricSections = savedSections.mapNotNull { savedSection ->
                                        // Find matching section by type and approximate index
                                        // Use orderIndex to pick from variantSections
                                        val match = variantSections.getOrNull(savedSection.orderIndex - 1)
                                        if (match != null && match.text.isNotBlank()) {
                                            savedSection.id.toJavaUuid() to match.text
                                        } else null
                                    }
                                    if (lyricSections.isNotEmpty()) {
                                        dal.krithis.saveLyricVariantSections(createdVariant.id, lyricSections)
                                    }
                                } else if (!scraped.lyrics.isNullOrBlank() && savedSections.isNotEmpty()) {
                                    // FALLBACK: Stanza-based mapping if TextBlocker failed to find headers
                                    val stanzas = scraped.lyrics.replace("\\n", "\n").split(Regex("\\n\\s*\\n")).filter { it.isNotBlank() }
                                    val lyricSections = savedSections.mapIndexedNotNull { idx, savedSection ->
                                        // Find a stanza that matches this section. 
                                        // If no headers, we assume stanzas align with canonical sections.
                                        val stanza = stanzas.getOrNull(idx)
                                        if (stanza != null) {
                                            savedSection.id.toJavaUuid() to stanza
                                        } else null
                                    }
                                    if (lyricSections.isNotEmpty()) {
                                        dal.krithis.saveLyricVariantSections(createdVariant.id, lyricSections)
                                    }
                                }
                            }
                        } else {
                            // Single primary variant (existing behaviour)
                            val effectiveSections = when {
                                !metadata.sections.isNullOrEmpty() -> metadata.sections
                                !metadata.lyrics.isNullOrBlank() -> parseSectionHeadersFromLyrics(metadata.lyrics.replace("\\n", "\n"))
                                else -> emptyList()
                            }
                            if (effectiveSections.isNotEmpty()) {
                                val sectionsToSave = effectiveSections.mapIndexed { idx, section ->
                                    Triple(section.type.name, idx + 1, null as String?)
                                }
                                dal.krithis.saveSections(createdKrithi.id, sectionsToSave)
                                val savedSections = dal.krithis.getSections(createdKrithi.id)
                                val lyricVariant = dal.krithis.createLyricVariant(
                                    krithiId = createdKrithi.id,
                                    language = LanguageCode.valueOf(createdKrithi.primaryLanguage.name),
                                    script = ScriptCode.LATIN,
                                    lyrics = metadata.lyrics ?: effectiveSections.joinToString("\n\n") { "[${it.type.name}]\n${it.text}" },
                                    isPrimary = true,
                                    sourceReference = sourceKey
                                )
                                val lyricSections = savedSections.mapNotNull { savedSection ->
                                    val originalSection = effectiveSections.getOrNull(savedSection.orderIndex - 1)
                                    if (originalSection != null && !originalSection.text.isBlank()) {
                                        savedSection.id.toJavaUuid() to originalSection.text
                                    } else null
                                }
                                if (lyricSections.isNotEmpty()) {
                                    dal.krithis.saveLyricVariantSections(lyricVariant.id, lyricSections)
                                }
                            } else if (!metadata.lyrics.isNullOrBlank()) {
                                dal.krithis.createLyricVariant(
                                    krithiId = createdKrithi.id,
                                    language = LanguageCode.valueOf(createdKrithi.primaryLanguage.name),
                                    script = ScriptCode.LATIN,
                                    lyrics = metadata.lyrics,
                                    isPrimary = true,
                                    sourceReference = sourceKey
                                )
                            }
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

    /** TRACK-032: Map scraped language string (SA, TA, etc.) to LanguageCode. */
    private fun parseLanguageCode(value: String?): LanguageCode? {
        if (value.isNullOrBlank()) return null
        return when (value.trim().uppercase()) {
            "SA" -> LanguageCode.SA
            "TA" -> LanguageCode.TA
            "TE" -> LanguageCode.TE
            "KN" -> LanguageCode.KN
            "ML" -> LanguageCode.ML
            "HI" -> LanguageCode.HI
            "EN" -> LanguageCode.EN
            else -> null
        }
    }

    /** TRACK-032: Map scraped script string (devanagari, tamil, etc.) to ScriptCode. */
    private fun parseScriptCode(value: String?): ScriptCode? {
        if (value.isNullOrBlank()) return null
        return when (value.trim().lowercase()) {
            "devanagari" -> ScriptCode.DEVANAGARI
            "tamil" -> ScriptCode.TAMIL
            "telugu" -> ScriptCode.TELUGU
            "kannada" -> ScriptCode.KANNADA
            "malayalam" -> ScriptCode.MALAYALAM
            "latin" -> ScriptCode.LATIN
            else -> null
        }
    }

    /** TRACK-032: Map TextBlocker label to RagaSectionDto. */
    private fun parseRagaSectionDto(label: String): RagaSectionDto? {
        return when (label.uppercase()) {
            "PALLAVI" -> RagaSectionDto.PALLAVI
            "ANUPALLAVI" -> RagaSectionDto.ANUPALLAVI
            "CHARANAM", "CARANAM" -> RagaSectionDto.CHARANAM
            "SAMASHTI_CHARANAM" -> RagaSectionDto.SAMASHTI_CHARANAM
            "CHITTASWARAM" -> RagaSectionDto.CHITTASWARAM
            "SWARA_SAHITYA" -> RagaSectionDto.SWARA_SAHITYA
            "MADHYAMAKALA", "MADHYAMA_KALA" -> RagaSectionDto.MADHYAMA_KALA
            "SOLKATTU_SWARA" -> RagaSectionDto.SOLKATTU_SWARA
            "ANUBANDHA" -> RagaSectionDto.ANUBANDHA
            "MUKTAYI_SWARA" -> RagaSectionDto.MUKTAYI_SWARA
            "ETTUGADA_SWARA" -> RagaSectionDto.ETTUGADA_SWARA
            "ETTUGADA_SAHITYA" -> RagaSectionDto.ETTUGADA_SAHITYA
            "VILOMA_CHITTASWARAM" -> RagaSectionDto.VILOMA_CHITTASWARAM
            else -> null
        }
    }

    /**
     * Parse section headers (Pallavi, Anupallavi, Charanam, Samashti Charanam, etc.) from lyrics text
     * when the scraper did not return structured sections. Used as fallback so Lyrics tab shows sections.
     */
    private fun parseSectionHeadersFromLyrics(lyrics: String): List<ScrapedSectionDto> {
        val pattern = Regex("""(?mi)^\s*(Pallavi|Anupallavi|Charanam|Samashti\s+Charanam|Chittaswaram)\s*:?\s*$""")
        val matches = pattern.findAll(lyrics).toList()
        if (matches.isEmpty()) return emptyList()
        val sectionTypeMap: (String) -> RagaSectionDto = { raw ->
            when (raw.trim().lowercase()) {
                "pallavi" -> RagaSectionDto.PALLAVI
                "anupallavi" -> RagaSectionDto.ANUPALLAVI
                "charanam" -> RagaSectionDto.CHARANAM
                "samashti charanam" -> RagaSectionDto.SAMASHTI_CHARANAM
                "chittaswaram" -> RagaSectionDto.CHITTASWARAM
                else -> RagaSectionDto.OTHER
            }
        }
        return matches.mapIndexed { i, match ->
            val type = sectionTypeMap(match.groupValues[1])
            val text = lyrics.substring(
                match.range.last + 1,
                if (i + 1 < matches.size) matches[i + 1].range.first else lyrics.length
            ).trim()
            ScrapedSectionDto(type = type, text = text)
        }.filter { it.text.isNotBlank() }
    }

    private fun isAuthoritySourceForComposer(sourceKey: String?, composer: String?): Boolean {
        if (sourceKey.isNullOrBlank() || composer.isNullOrBlank()) return false
        val normalizedComposer = composer.trim().lowercase()
        val authorityHosts = composerSourcePriority[normalizedComposer] ?: composerSourcePriority["general"] ?: emptyList()
        val host = try {
            URI(sourceKey).host?.lowercase()
        } catch (e: Exception) {
            null
        }
        if (host.isNullOrBlank()) return false
        return authorityHosts.any { host.contains(it) }
    }
}
