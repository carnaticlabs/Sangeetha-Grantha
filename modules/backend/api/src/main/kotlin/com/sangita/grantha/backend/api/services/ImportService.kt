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

class ImportService(private val dal: SangitaDal) {
    private fun normalize(value: String): String =
        value.trim()
            .lowercase()
            .replace(Regex("\\s+"), " ")
            .replace(Regex("[^a-z0-9\\s]"), "")
    suspend fun getImports(status: ImportStatus? = null): List<ImportedKrithiDto> {
        return dal.imports.listImports(status)
    }

    suspend fun submitImports(requests: List<ImportKrithiRequest>): List<ImportedKrithiDto> {
        if (requests.isEmpty()) return emptyList()

        val created = mutableListOf<ImportedKrithiDto>()
        for (request in requests) {
            val sourceId = dal.imports.findOrCreateSource(request.source)
            val parsedPayload = request.rawPayload?.toString()
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
                parsedPayload = parsedPayload
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

    suspend fun reviewImport(id: Uuid, request: ImportReviewRequest): ImportedKrithiDto {
        val mappedId = request.mappedKrithiId?.let { parseUuidOrThrow(it, "mappedKrithiId") }
        val status = ImportStatus.valueOf(request.status.name)
        
        // If status is APPROVED and no mappedKrithiId is provided, create a new krithi
        var createdKrithiId: UUID? = null
        if (status == ImportStatus.APPROVED && mappedId == null) {
            // Get the import to extract data
            val importData = dal.imports.findById(id) ?: throw NoSuchElementException("Import not found")
            
            // Extract nullable values to local variables for smart casting
            val rawComposer = importData.rawComposer
            val rawRaga = importData.rawRaga
            val rawTala = importData.rawTala
            val rawTitle = importData.rawTitle
            val rawLanguage = importData.rawLanguage
            val rawLyrics = importData.rawLyrics
            val sourceKey = importData.sourceKey
            
            // Find or create composer
            val composerId = if (rawComposer != null) {
                dal.composers.findByName(rawComposer)?.id?.toJavaUuid()
                    ?: dal.composers.create(
                        name = rawComposer,
                        nameNormalized = null, // Will be auto-normalized
                        birthYear = null,
                        deathYear = null,
                        place = null,
                        notes = null
                    ).id.toJavaUuid()
            } else {
                throw IllegalArgumentException("Composer is required to create krithi from import")
            }
            
            // Find or create raga (if provided)
            val ragaId = rawRaga?.let { ragaName ->
                dal.ragas.findByName(ragaName)?.id?.toJavaUuid()
                    ?: dal.ragas.create(
                        name = ragaName,
                        nameNormalized = null, // Will be auto-normalized
                        melakartaNumber = null,
                        parentRagaId = null,
                        arohanam = null,
                        avarohanam = null,
                        notes = null
                    ).id.toJavaUuid()
            }
            
            // Find or create tala (if provided)
            val talaId = rawTala?.let { talaName ->
                dal.talas.findByName(talaName)?.id?.toJavaUuid()
                    ?: dal.talas.create(
                        name = talaName,
                        nameNormalized = null, // Will be auto-normalized
                        beatCount = null,
                        angaStructure = null,
                        notes = null
                    ).id.toJavaUuid()
            }
            
            // Determine language code (default to TE if not provided)
            val languageCode = when (rawLanguage?.lowercase()) {
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
            val title = rawTitle ?: "Untitled"
            val createdKrithi = dal.krithis.create(
                title = title,
                titleNormalized = normalize(title),
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
                sahityaSummary = rawLyrics?.take(500), // First 500 chars as summary
                notes = "Created from import: ${sourceKey ?: "unknown"}"
            )
            
            createdKrithiId = createdKrithi.id.toJavaUuid()
            
            dal.auditLogs.append(
                action = "CREATE_KRITHI_FROM_IMPORT",
                entityTable = "krithis",
                entityId = createdKrithi.id
            )

            // Process sections and lyrics from scraped metadata
            if (importData.parsedPayload != null) {
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
                    // This creates a krithi but maybe without full details
                    println("Error processing scraped metadata: ${e.message}")
                    e.printStackTrace()
                }
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
}
