package com.sangita.grantha.backend.dal.repositories

import com.sangita.grantha.backend.dal.DatabaseFactory
import com.sangita.grantha.backend.dal.enums.LanguageCode
import com.sangita.grantha.backend.dal.enums.MusicalForm
import com.sangita.grantha.backend.dal.enums.ScriptCode
import com.sangita.grantha.backend.dal.enums.WorkflowState
import com.sangita.grantha.backend.dal.models.toKrithiDto
import com.sangita.grantha.backend.dal.models.toKrithiSectionDto
import com.sangita.grantha.backend.dal.models.toKrithiLyricVariantDto
import com.sangita.grantha.backend.dal.models.toKrithiLyricSectionDto
import com.sangita.grantha.backend.dal.models.toRagaDto
import com.sangita.grantha.backend.dal.models.toTagDto
import com.sangita.grantha.shared.domain.model.KrithiSummary
import com.sangita.grantha.shared.domain.model.RagaRefDto
import com.sangita.grantha.backend.dal.support.toJavaUuid
import com.sangita.grantha.backend.dal.support.toKotlinUuid
import com.sangita.grantha.backend.dal.tables.ComposersTable
import com.sangita.grantha.backend.dal.tables.KrithisTable
import com.sangita.grantha.backend.dal.tables.KrithiLyricVariantsTable
import com.sangita.grantha.backend.dal.tables.KrithiLyricSectionsTable
import com.sangita.grantha.backend.dal.tables.KrithiRagasTable
import com.sangita.grantha.backend.dal.tables.KrithiSectionsTable
import com.sangita.grantha.backend.dal.tables.KrithiTagsTable
import com.sangita.grantha.backend.dal.tables.RagasTable
import com.sangita.grantha.backend.dal.tables.TagsTable
import com.sangita.grantha.shared.domain.model.KrithiDto
import com.sangita.grantha.shared.domain.model.KrithiSearchResult
import com.sangita.grantha.shared.domain.model.KrithiSectionDto
import com.sangita.grantha.shared.domain.model.KrithiLyricVariantDto
import com.sangita.grantha.shared.domain.model.KrithiLyricSectionDto
import com.sangita.grantha.shared.domain.model.KrithiLyricVariantWithSectionsDto
import com.sangita.grantha.shared.domain.model.TagDto
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.util.UUID
import kotlin.uuid.Uuid
import org.jetbrains.exposed.v1.exceptions.ExposedSQLException
import org.jetbrains.exposed.v1.jdbc.*
import org.jetbrains.exposed.v1.core.*
import org.jetbrains.exposed.v1.core.JoinType
import org.slf4j.LoggerFactory

data class KrithiSearchFilters(
    val query: String? = null,
    val lyric: String? = null,
    val composerId: UUID? = null,
    val ragaId: UUID? = null,
    val talaId: UUID? = null,
    val deityId: UUID? = null,
    val templeId: UUID? = null,
    val primaryLanguage: LanguageCode? = null,
)

data class KrithiCreateParams(
    val title: String,
    val titleNormalized: String,
    val incipit: String? = null,
    val incipitNormalized: String? = null,
    val composerId: UUID,
    val musicalForm: MusicalForm,
    val primaryLanguage: LanguageCode,
    val primaryRagaId: UUID? = null,
    val talaId: UUID? = null,
    val deityId: UUID? = null,
    val templeId: UUID? = null,
    val isRagamalika: Boolean,
    val ragaIds: List<UUID> = emptyList(),
    val workflowState: WorkflowState,
    val sahityaSummary: String? = null,
    val notes: String? = null,
    val createdByUserId: UUID? = null,
    val updatedByUserId: UUID? = null,
)

data class KrithiUpdateParams(
    val id: Uuid,
    val title: String? = null,
    val titleNormalized: String? = null,
    val incipit: String? = null,
    val incipitNormalized: String? = null,
    val composerId: UUID? = null,
    val musicalForm: MusicalForm? = null,
    val primaryLanguage: LanguageCode? = null,
    val primaryRagaId: UUID? = null,
    val talaId: UUID? = null,
    val deityId: UUID? = null,
    val templeId: UUID? = null,
    val isRagamalika: Boolean? = null,
    val ragaIds: List<UUID>? = null,
    val workflowState: WorkflowState? = null,
    val sahityaSummary: String? = null,
    val notes: String? = null,
    val updatedByUserId: UUID? = null,
)

/**
 * Repository for krithi entities, tags, sections, and lyric variants.
 */
class KrithiRepository {
    companion object {
        const val MIN_PAGE_SIZE = 1
        const val MAX_PAGE_SIZE = 200
        private const val LYRICS_INDEX_SAFE_UTF8_BYTES = 1800

        private val logger = LoggerFactory.getLogger(KrithiRepository::class.java)

        private fun truncateUtf8ByBytes(input: String, maxBytes: Int): String {
            val bytes = input.toByteArray(Charsets.UTF_8)
            if (bytes.size <= maxBytes) return input

            var end = maxBytes.coerceAtMost(bytes.size)
            while (end > 0 && (bytes[end - 1].toInt() and 0b1100_0000) == 0b1000_0000) {
                end--
            }
            if (end <= 0) return ""
            return String(bytes, 0, end, Charsets.UTF_8).trimEnd()
        }

        private fun isLyricsIndexRowSizeOverflow(error: Throwable): Boolean {
            val messages = buildList {
                var current: Throwable? = error
                while (current != null) {
                    current.message?.let(::add)
                    current = current.cause
                }
            }.joinToString(" ")

            return messages.contains("idx_krithi_lyric_variants_lyrics", ignoreCase = true) &&
                messages.contains("index row size", ignoreCase = true)
        }
    }
    /**
     * Find a krithi by ID.
     */
    suspend fun findById(id: Uuid): KrithiDto? = DatabaseFactory.dbQuery {
        KrithisTable
            .selectAll()
            .where { KrithisTable.id eq id.toJavaUuid() }
            .map { it.toKrithiDto() }
            .singleOrNull()
    }

    /**
     * Create a new krithi record.
     */
    suspend fun create(params: KrithiCreateParams): KrithiDto = DatabaseFactory.dbQuery {
        val now = OffsetDateTime.now(ZoneOffset.UTC)
        val krithiId = UUID.randomUUID()

        val result = KrithisTable.insert {
            it[id] = krithiId
            it[KrithisTable.title] = params.title
            it[KrithisTable.titleNormalized] = params.titleNormalized
            it[KrithisTable.incipit] = params.incipit
            it[KrithisTable.incipitNormalized] = params.incipitNormalized
            it[KrithisTable.composerId] = params.composerId
            it[KrithisTable.musicalForm] = params.musicalForm
            it[KrithisTable.primaryLanguage] = params.primaryLanguage
            it[KrithisTable.primaryRagaId] = params.primaryRagaId
            it[KrithisTable.talaId] = params.talaId
            it[KrithisTable.deityId] = params.deityId
            it[KrithisTable.templeId] = params.templeId
            it[KrithisTable.isRagamalika] = params.isRagamalika
            it[KrithisTable.workflowState] = params.workflowState
            it[KrithisTable.sahityaSummary] = params.sahityaSummary
            it[KrithisTable.notes] = params.notes
            it[KrithisTable.createdByUserId] = params.createdByUserId
            it[KrithisTable.updatedByUserId] = params.updatedByUserId
            it[KrithisTable.createdAt] = now
            it[KrithisTable.updatedAt] = now
        }
            .resultedValues
            ?.single()
            ?.toKrithiDto()
            ?: error("Failed to insert krithi")

        if (params.ragaIds.isNotEmpty()) {
            KrithiRagasTable.batchInsert(params.ragaIds.withIndex()) { (index, ragaId) ->
                this[KrithiRagasTable.krithiId] = krithiId
                this[KrithiRagasTable.ragaId] = ragaId
                this[KrithiRagasTable.orderIndex] = index
            }
        }

        result
    }

    /**
     * Update an existing krithi and return the updated record.
     */
    suspend fun update(params: KrithiUpdateParams): KrithiDto? = DatabaseFactory.dbQuery {
        val now = OffsetDateTime.now(ZoneOffset.UTC)
        val javaId = params.id.toJavaUuid()

        // Use Exposed 1.0.0-rc-4 updateReturning to update and fetch the row in one round-trip
        val updatedKrithi = KrithisTable
            .updateReturning(
                where = { KrithisTable.id eq javaId }
            ) {
                params.title?.let { value -> it[KrithisTable.title] = value }
                params.titleNormalized?.let { value -> it[KrithisTable.titleNormalized] = value }
                params.incipit?.let { value -> it[KrithisTable.incipit] = value }
                params.incipitNormalized?.let { value -> it[KrithisTable.incipitNormalized] = value }
                params.composerId?.let { value -> it[KrithisTable.composerId] = value }
                params.musicalForm?.let { value -> it[KrithisTable.musicalForm] = value }
                params.primaryLanguage?.let { value -> it[KrithisTable.primaryLanguage] = value }
                params.primaryRagaId?.let { value -> it[KrithisTable.primaryRagaId] = value }
                params.talaId?.let { value -> it[KrithisTable.talaId] = value }
                params.deityId?.let { value -> it[KrithisTable.deityId] = value }
                params.templeId?.let { value -> it[KrithisTable.templeId] = value }
                params.isRagamalika?.let { value -> it[KrithisTable.isRagamalika] = value }
                params.workflowState?.let { value -> it[KrithisTable.workflowState] = value }
                params.sahityaSummary?.let { value -> it[KrithisTable.sahityaSummary] = value }
                params.notes?.let { value -> it[KrithisTable.notes] = value }
                params.updatedByUserId?.let { value -> it[KrithisTable.updatedByUserId] = value }
                it[KrithisTable.updatedAt] = now
            }
            .singleOrNull()
            ?.toKrithiDto()

        if (updatedKrithi == null) {
            return@dbQuery null
        }

        params.ragaIds?.let { ragas ->
            val javaKrithiId = params.id.toJavaUuid()
            
            // Get existing ragas indexed by (ragaId, orderIndex) - composite key
            val existingRagas = KrithiRagasTable
                .selectAll()
                .where { KrithiRagasTable.krithiId eq javaKrithiId }
                .associateBy { 
                    it[KrithiRagasTable.ragaId] to it[KrithiRagasTable.orderIndex] 
                }
            
            // Build map of new ragas by (ragaId, orderIndex)
            val newRagasMap = ragas.withIndex().associateBy { 
                it.value to it.index 
            } // (ragaId, orderIndex) -> IndexedValue
            
            // Determine what to insert and delete
            // Note: For composite primary key (krithiId, ragaId, orderIndex),
            // if ragaId or orderIndex changes, we must delete and reinsert.
            // However, we only delete/insert what actually changed.
            val toInsert = mutableListOf<Pair<UUID, Int>>() // (ragaId, orderIndex)
            val toDelete = mutableListOf<Pair<UUID, Int>>() // (ragaId, orderIndex)
            
            // Find ragas to delete (existing (ragaId, orderIndex) not in new list)
            existingRagas.forEach { (key, _) ->
                if (!newRagasMap.containsKey(key)) {
                    toDelete.add(key)
                }
            }
            
            // Find ragas to insert (new (ragaId, orderIndex) combinations)
            newRagasMap.forEach { (key, _) ->
                if (!existingRagas.containsKey(key)) {
                    toInsert.add(key)
                }
            }
            
            // Execute deletes - only removed/changed ragas
            if (toDelete.isNotEmpty()) {
                toDelete.forEach { (ragaId, orderIndex) ->
                    KrithiRagasTable.deleteWhere {
                        (KrithiRagasTable.krithiId eq javaKrithiId) and
                        (KrithiRagasTable.ragaId eq ragaId) and
                        (KrithiRagasTable.orderIndex eq orderIndex)
                    }
                }
            }
            
            // Execute inserts - only new/changed ragas
            if (toInsert.isNotEmpty()) {
                KrithiRagasTable.batchInsert(toInsert) { (ragaId, orderIndex) ->
                    this[KrithiRagasTable.krithiId] = javaKrithiId
                    this[KrithiRagasTable.ragaId] = ragaId
                    this[KrithiRagasTable.orderIndex] = orderIndex
                    // section and notes default to null (can be set separately if needed)
                    // Note: Metadata (section, notes) is lost when raga position changes
                    // This is acceptable as ragas are typically reordered, not updated in place
                }
            }
        }

        updatedKrithi
    }

    /**
     * List tags for a krithi.
     */
    suspend fun getTags(krithiId: Uuid): List<TagDto> = DatabaseFactory.dbQuery {
        val javaKrithiId = krithiId.toJavaUuid()
        KrithiTagsTable
            .join(TagsTable, JoinType.INNER, onColumn = KrithiTagsTable.tagId, otherColumn = TagsTable.id)
            .selectAll()
            .where { KrithiTagsTable.krithiId eq javaKrithiId }
            .map { it.toTagDto() }
    }

    /**
     * Updates krithi tags using efficient INSERT/DELETE operations.
     * Preserves existing metadata (sourceInfo, confidence) for tags that remain.
     * 
     * @param krithiId The krithi ID
     * @param tagIds List of tag IDs to associate with the krithi
     */
    suspend fun updateTags(krithiId: Uuid, tagIds: List<UUID>) = DatabaseFactory.dbQuery {
        val javaKrithiId = krithiId.toJavaUuid()
        org.slf4j.LoggerFactory.getLogger(KrithiRepository::class.java).info("Updating tags for krithi $krithiId. New tags: $tagIds")
        
        // Get existing tags indexed by tag_id
        val existingTags = KrithiTagsTable
            .selectAll()
            .where { KrithiTagsTable.krithiId eq javaKrithiId }
            .associateBy { it[KrithiTagsTable.tagId] }
        
        // Build set of new tag IDs
        val newTagIdsSet = tagIds.toSet()
        
        // Determine what to insert and delete
        val toInsert = mutableListOf<UUID>()
        val toDelete = mutableListOf<UUID>()
        
        // Find tags to delete (existing tags not in new list)
        existingTags.forEach { (tagId, _) ->
            if (!newTagIdsSet.contains(tagId)) {
                toDelete.add(tagId)
            }
        }
        
        // Find tags to insert (new tags not in existing list)
        newTagIdsSet.forEach { tagId ->
            if (!existingTags.containsKey(tagId)) {
                toInsert.add(tagId)
            }
        }
        
        // Execute deletes - only removed tags
        if (toDelete.isNotEmpty()) {
            org.slf4j.LoggerFactory.getLogger(KrithiRepository::class.java).info("Deleting tags: $toDelete")
            KrithiTagsTable.deleteWhere {
                (KrithiTagsTable.krithiId eq javaKrithiId) and
                (KrithiTagsTable.tagId inList toDelete)
            }
        }
        
        // Execute inserts - only new tags
        if (toInsert.isNotEmpty()) {
            org.slf4j.LoggerFactory.getLogger(KrithiRepository::class.java).info("Inserting tags: $toInsert")
            KrithiTagsTable.batchInsert(toInsert) { tagId ->
                this[KrithiTagsTable.krithiId] = javaKrithiId
                this[KrithiTagsTable.tagId] = tagId
                this[KrithiTagsTable.sourceInfo] = "manual"
                // confidence defaults to null
            }
        }
        
        // Tags that remain unchanged preserve their sourceInfo and confidence automatically
    }

    /**
     * List structured sections for a krithi.
     */
    suspend fun getSections(krithiId: Uuid): List<KrithiSectionDto> = DatabaseFactory.dbQuery {
        KrithiSectionsTable
            .selectAll()
            .where { KrithiSectionsTable.krithiId eq krithiId.toJavaUuid() }
            .orderBy(KrithiSectionsTable.orderIndex)
            .map { it.toKrithiSectionDto() }
    }

    /**
     * List lyric variants with their sections for a krithi.
     */
    suspend fun getLyricVariants(krithiId: Uuid): List<KrithiLyricVariantWithSectionsDto> = DatabaseFactory.dbQuery {
        val javaKrithiId = krithiId.toJavaUuid()
        
        // Get all variants for this krithi
        val variants = KrithiLyricVariantsTable
            .selectAll()
            .where { KrithiLyricVariantsTable.krithiId eq javaKrithiId }
            .map { it.toKrithiLyricVariantDto() }
        
        // Get all sections for these variants
        val variantIds = variants.map { it.id.toJavaUuid() }
        val sections = if (variantIds.isNotEmpty()) {
            KrithiLyricSectionsTable
                .selectAll()
                .where { KrithiLyricSectionsTable.lyricVariantId inList variantIds }
                .map { it.toKrithiLyricSectionDto() }
                .groupBy { it.lyricVariantId }
        } else {
            emptyMap()
        }
        
        // Combine variants with their sections
        variants.map { variant ->
            KrithiLyricVariantWithSectionsDto(
                variant = variant,
                sections = sections[variant.id] ?: emptyList()
            )
        }
    }

    /**
     * Find a lyric variant by ID.
     */
    suspend fun findLyricVariantById(variantId: Uuid): KrithiLyricVariantDto? = DatabaseFactory.dbQuery {
        KrithiLyricVariantsTable
            .selectAll()
            .where { KrithiLyricVariantsTable.id eq variantId.toJavaUuid() }
            .map { it.toKrithiLyricVariantDto() }
            .singleOrNull()
    }

    /**
     * Create a lyric variant for a krithi.
     */
    suspend fun createLyricVariant(
        krithiId: Uuid,
        language: LanguageCode,
        script: ScriptCode,
        transliterationScheme: String? = null,
        sampradayaId: UUID? = null,
        variantLabel: String? = null,
        sourceReference: String? = null,
        lyrics: String,
        isPrimary: Boolean = false,
        createdByUserId: UUID? = null,
        updatedByUserId: UUID? = null
    ): KrithiLyricVariantDto = DatabaseFactory.dbQuery {
        val now = OffsetDateTime.now(ZoneOffset.UTC)
        val javaKrithiId = krithiId.toJavaUuid()

        // TRACK-062: Idempotency guard — skip if variant with same (krithiId, language, script) already exists
        val existing = KrithiLyricVariantsTable.selectAll()
            .andWhere { KrithiLyricVariantsTable.krithiId eq javaKrithiId }
            .andWhere { KrithiLyricVariantsTable.language eq language }
            .andWhere { KrithiLyricVariantsTable.script eq script }
            .limit(1)
            .singleOrNull()
        
        if (existing != null) {
            return@dbQuery existing.toKrithiLyricVariantDto()
        }

        val variantId = UUID.randomUUID()

        fun insertVariant(lyricsValue: String): KrithiLyricVariantDto {
            return KrithiLyricVariantsTable.insert {
                it[id] = variantId
                it[KrithiLyricVariantsTable.krithiId] = javaKrithiId
                it[KrithiLyricVariantsTable.language] = language
                it[KrithiLyricVariantsTable.script] = script
                it[KrithiLyricVariantsTable.transliterationScheme] = transliterationScheme
                it[KrithiLyricVariantsTable.sampradayaId] = sampradayaId
                it[KrithiLyricVariantsTable.variantLabel] = variantLabel
                it[KrithiLyricVariantsTable.sourceReference] = sourceReference
                it[KrithiLyricVariantsTable.lyrics] = lyricsValue
                it[KrithiLyricVariantsTable.isPrimary] = isPrimary
                it[KrithiLyricVariantsTable.createdByUserId] = createdByUserId
                it[KrithiLyricVariantsTable.updatedByUserId] = updatedByUserId
                it[KrithiLyricVariantsTable.createdAt] = now
                it[KrithiLyricVariantsTable.updatedAt] = now
            }
                .resultedValues
                ?.single()
                ?.toKrithiLyricVariantDto()
                ?: error("Failed to insert lyric variant")
        }

        try {
            insertVariant(lyrics)
        } catch (e: ExposedSQLException) {
            if (!isLyricsIndexRowSizeOverflow(e)) throw e

            val truncated = truncateUtf8ByBytes(lyrics, LYRICS_INDEX_SAFE_UTF8_BYTES)
            if (truncated.isBlank()) throw e

            logger.warn(
                "Retrying lyric variant insert with truncated lyrics due to oversized btree index row (krithiId={}, language={}, script={}, originalBytes={}, truncatedBytes={})",
                krithiId,
                language,
                script,
                lyrics.toByteArray(Charsets.UTF_8).size,
                truncated.toByteArray(Charsets.UTF_8).size
            )
            insertVariant(truncated)
        }
    }

    /**
     * Update a lyric variant and return the updated record.
     */
    suspend fun updateLyricVariant(
        variantId: Uuid,
        language: LanguageCode? = null,
        script: ScriptCode? = null,
        transliterationScheme: String? = null,
        sampradayaId: UUID? = null,
        variantLabel: String? = null,
        sourceReference: String? = null,
        lyrics: String? = null,
        isPrimary: Boolean? = null,
        updatedByUserId: UUID? = null
    ): KrithiLyricVariantDto? = DatabaseFactory.dbQuery {
        val now = OffsetDateTime.now(ZoneOffset.UTC)
        val javaVariantId = variantId.toJavaUuid()
        
        // Use Exposed 1.0.0-rc-4 updateReturning to update and fetch the row in one round-trip
        KrithiLyricVariantsTable
            .updateReturning(
                where = { KrithiLyricVariantsTable.id eq javaVariantId }
            ) {
                language?.let { value -> it[KrithiLyricVariantsTable.language] = value }
                script?.let { value -> it[KrithiLyricVariantsTable.script] = value }
                transliterationScheme?.let { value -> it[KrithiLyricVariantsTable.transliterationScheme] = value }
                sampradayaId?.let { value -> it[KrithiLyricVariantsTable.sampradayaId] = value }
                variantLabel?.let { value -> it[KrithiLyricVariantsTable.variantLabel] = value }
                sourceReference?.let { value -> it[KrithiLyricVariantsTable.sourceReference] = value }
                lyrics?.let { value -> it[KrithiLyricVariantsTable.lyrics] = value }
                isPrimary?.let { value -> it[KrithiLyricVariantsTable.isPrimary] = value }
                updatedByUserId?.let { value -> it[KrithiLyricVariantsTable.updatedByUserId] = value }
                it[KrithiLyricVariantsTable.updatedAt] = now
            }
            .singleOrNull()
            ?.toKrithiLyricVariantDto()
    }

    /**
     * Saves lyric variant sections using efficient UPDATE/INSERT/DELETE operations.
     * Only sections that actually changed are updated, preserving metadata like created_at.
     * 
     * @param variantId The lyric variant ID
     * @param sections List of (sectionId, text) pairs
     */
    suspend fun saveLyricVariantSections(
        variantId: Uuid,
        sections: List<Pair<UUID, String>> // (sectionId, text)
    ) = DatabaseFactory.dbQuery {
        val now = OffsetDateTime.now(ZoneOffset.UTC)
        val javaVariantId = variantId.toJavaUuid()
        
        // Get existing sections indexed by section_id (unique constraint: variant_id + section_id)
        val existingSections = KrithiLyricSectionsTable
            .selectAll()
            .where { KrithiLyricSectionsTable.lyricVariantId eq javaVariantId }
            .associateBy { it[KrithiLyricSectionsTable.sectionId] }
        
        // Build map of new sections by section_id
        val newSectionsMap = sections.associateBy { it.first } // sectionId -> (sectionId, text)
        
        // Determine what to update, insert, and delete
        val toUpdate = mutableListOf<Pair<UUID, String>>() // (id, text)
        val toInsert = mutableListOf<Pair<UUID, String>>() // (sectionId, text)
        val toDelete = mutableListOf<UUID>() // lyric section IDs
        
        // Process existing sections
        existingSections.forEach { (sectionId, row) ->
            val existingId = row[KrithiLyricSectionsTable.id].value
            val existingText = row[KrithiLyricSectionsTable.text]
            val newSection = newSectionsMap[sectionId]
            
            if (newSection != null) {
                val (_, newText) = newSection
                // Check if text changed
                if (newText != existingText) {
                    // Text changed - update it
                    toUpdate.add(existingId to newText)
                }
                // If same, no change needed - preserve existing data
            } else {
                // Section removed from new list
                toDelete.add(existingId)
            }
        }
        
        // Find sections to insert (new section_id values)
        newSectionsMap.forEach { (sectionId, section) ->
            if (!existingSections.containsKey(sectionId)) {
                toInsert.add(section)
            }
        }
        
        // Execute updates - only changed sections
        toUpdate.forEach { (id, text) ->
            KrithiLyricSectionsTable.update({ KrithiLyricSectionsTable.id eq id }) {
                it[KrithiLyricSectionsTable.text] = text
                it[KrithiLyricSectionsTable.updatedAt] = now
                // Preserve: created_at, normalized_text (can be computed later)
            }
        }
        
        // Execute inserts - only new sections
        if (toInsert.isNotEmpty()) {
            KrithiLyricSectionsTable.batchInsert(toInsert) { (sectionId, text) ->
                this[KrithiLyricSectionsTable.id] = UUID.randomUUID()
                this[KrithiLyricSectionsTable.lyricVariantId] = javaVariantId
                this[KrithiLyricSectionsTable.sectionId] = sectionId
                this[KrithiLyricSectionsTable.text] = text
                this[KrithiLyricSectionsTable.normalizedText] = null // Can be computed later if needed
                this[KrithiLyricSectionsTable.createdAt] = now
                this[KrithiLyricSectionsTable.updatedAt] = now
            }
        }
        
        // Execute deletes - only removed sections
        if (toDelete.isNotEmpty()) {
            KrithiLyricSectionsTable.deleteWhere { 
                KrithiLyricSectionsTable.id inList toDelete 
            }
        }
    }

    /**
     * Saves krithi sections using efficient UPDATE/INSERT/DELETE operations.
     * Only sections that actually changed are updated, preserving metadata like created_at.
     * 
     * @param krithiId The krithi ID
     * @param sections List of (sectionType, orderIndex, label) tuples
     */
    suspend fun saveSections(krithiId: Uuid, sections: List<Triple<String, Int, String?>>) = DatabaseFactory.dbQuery {
        val now = OffsetDateTime.now(ZoneOffset.UTC)
        val javaKrithiId = krithiId.toJavaUuid()
        
        // Get existing sections indexed by order_index
        val existingSections = KrithiSectionsTable
            .selectAll()
            .where { KrithiSectionsTable.krithiId eq javaKrithiId }
            .associateBy { it[KrithiSectionsTable.orderIndex] }
        
        // Build map of new sections by order_index
        val newSectionsMap = sections.associateBy { it.second } // orderIndex -> Triple
        
        // Determine what to update, insert, and delete
        val toUpdate = mutableListOf<Triple<UUID, String, String?>>() // (id, sectionType, label)
        val toInsert = mutableListOf<Triple<String, Int, String?>>() // (sectionType, orderIndex, label)
        val toDelete = mutableListOf<UUID>()
        
        // Process existing sections
        existingSections.forEach { (orderIndex, row) ->
            val existingId = row[KrithiSectionsTable.id].value
            val existingType = row[KrithiSectionsTable.sectionType]
            val existingLabel = row[KrithiSectionsTable.label]
            val newSection = newSectionsMap[orderIndex]
            
            if (newSection != null) {
                val (newType, _, newLabel) = newSection
                // Check if section type or label changed
                if (newType != existingType || newLabel != existingLabel) {
                    // Section changed - update it
                    toUpdate.add(Triple(existingId, newType, newLabel))
                }
                // If same, no change needed - preserve existing data
            } else {
                // Section removed from new list
                toDelete.add(existingId)
            }
        }
        
        // Find sections to insert (new order_index values)
        newSectionsMap.forEach { (orderIndex, section) ->
            if (!existingSections.containsKey(orderIndex)) {
                toInsert.add(section)
            }
        }
        
        // Execute updates - only changed sections
        toUpdate.forEach { (id, sectionType, label) ->
            KrithiSectionsTable.update({ KrithiSectionsTable.id eq id }) {
                it[KrithiSectionsTable.sectionType] = sectionType
                it[KrithiSectionsTable.label] = label
                it[KrithiSectionsTable.updatedAt] = now
                // Preserve: created_at, notes (not in request)
            }
        }
        
        // Execute inserts - only new sections
        if (toInsert.isNotEmpty()) {
            KrithiSectionsTable.batchInsert(toInsert) { section ->
                val (sectionType, orderIndex, label) = section
                this[KrithiSectionsTable.id] = UUID.randomUUID()
                this[KrithiSectionsTable.krithiId] = javaKrithiId
                this[KrithiSectionsTable.sectionType] = sectionType
                this[KrithiSectionsTable.orderIndex] = orderIndex
                this[KrithiSectionsTable.label] = label
                this[KrithiSectionsTable.notes] = null
                this[KrithiSectionsTable.createdAt] = now
                this[KrithiSectionsTable.updatedAt] = now
            }
        }
        
        // Execute deletes - only removed sections
        if (toDelete.isNotEmpty()) {
            KrithiSectionsTable.deleteWhere { 
                KrithiSectionsTable.id inList toDelete 
            }
        }
    }

    /**
     * Search krithis with filters and pagination.
     */
    suspend fun search(
        filters: KrithiSearchFilters,
        page: Int,
        pageSize: Int,
        publishedOnly: Boolean = true,
    ): KrithiSearchResult = DatabaseFactory.dbQuery {
        val safePage = page.coerceAtLeast(0)
        val safeSize = pageSize.coerceIn(MIN_PAGE_SIZE, MAX_PAGE_SIZE)
        val offset = (safePage * safeSize).toLong()

        var join = KrithisTable
            .leftJoin(ComposersTable, { KrithisTable.composerId }, { ComposersTable.id })
            .leftJoin(KrithiRagasTable, { KrithisTable.id }, { KrithiRagasTable.krithiId })
            .leftJoin(RagasTable, { KrithiRagasTable.ragaId }, { RagasTable.id })

        if (!filters.lyric.isNullOrBlank()) {
            join = join.leftJoin(KrithiLyricVariantsTable, { KrithisTable.id }, { KrithiLyricVariantsTable.krithiId })
        }

        val idQuery = join
            .select(KrithisTable.id)
            .groupBy(KrithisTable.id, KrithisTable.titleNormalized)

        if (publishedOnly) {
            idQuery.andWhere { KrithisTable.workflowState eq WorkflowState.PUBLISHED }
        }

        filters.query?.trim()?.takeIf { it.isNotEmpty() }?.let { query ->
            val token = "%${query.lowercase()}%"
            idQuery.andWhere {
                (KrithisTable.titleNormalized like token) or
                    (KrithisTable.incipitNormalized like token) or
                    (ComposersTable.nameNormalized like token) or
                    (RagasTable.nameNormalized like token)
            }
        }

        filters.lyric?.trim()?.takeIf { it.isNotEmpty() }?.let { lyric ->
            val token = "%${lyric.lowercase()}%"
            idQuery.andWhere { KrithiLyricVariantsTable.lyrics like token }
        }

        filters.composerId?.let { idQuery.andWhere { KrithisTable.composerId eq it } }
        filters.ragaId?.let { idQuery.andWhere { KrithisTable.primaryRagaId eq it } }
        filters.talaId?.let { idQuery.andWhere { KrithisTable.talaId eq it } }
        filters.deityId?.let { idQuery.andWhere { KrithisTable.deityId eq it } }
        filters.templeId?.let { idQuery.andWhere { KrithisTable.templeId eq it } }
        filters.primaryLanguage?.let { idQuery.andWhere { KrithisTable.primaryLanguage eq it } }

        idQuery.orderBy(KrithisTable.titleNormalized to SortOrder.ASC)

        val total = idQuery.count()
        val pagedIds = idQuery.limit(safeSize).offset(offset)

        val dataQuery = join
            .select(
                KrithisTable.columns +
                    listOf(
                        ComposersTable.name,
                        RagasTable.id,
                        RagasTable.name,
                        KrithiRagasTable.orderIndex
                    )
            )
            .where { KrithisTable.id inSubQuery pagedIds }

        val summaries = linkedMapOf<UUID, KrithiSummary>()
        val ragasByKrithi = linkedMapOf<UUID, MutableList<RagaRefDto>>()

        dataQuery.forEach { row ->
            val krithiId = row[KrithisTable.id].value
            val composerName = row[ComposersTable.name]
            val summary = summaries.getOrPut(krithiId) {
                KrithiSummary(
                    id = krithiId.toKotlinUuid(),
                    name = row[KrithisTable.title],
                    composerName = composerName,
                    primaryLanguage = com.sangita.grantha.shared.domain.model.LanguageCodeDto.valueOf(row[KrithisTable.primaryLanguage].name),
                    ragas = emptyList()
                )
            }

            val ragaId = row.getOrNull(RagasTable.id)?.value
            val ragaName = row.getOrNull(RagasTable.name)
            val orderIndex = row.getOrNull(KrithiRagasTable.orderIndex)
            if (ragaId != null && ragaName != null && orderIndex != null) {
                val list = ragasByKrithi.getOrPut(krithiId) { mutableListOf() }
                if (list.none { it.id.toJavaUuid() == ragaId && it.orderIndex == orderIndex }) {
                    list.add(RagaRefDto(id = ragaId.toKotlinUuid(), name = ragaName, orderIndex = orderIndex))
                }
            }
        }

        val items = summaries.values.map { summary ->
            val ragas = ragasByKrithi[summary.id.toJavaUuid()]?.sortedBy { it.orderIndex } ?: emptyList()
            summary.copy(ragas = ragas)
        }

        KrithiSearchResult(
            items = items,
            total = total,
            page = safePage,
            pageSize = safeSize
        )
    }

    /**
     * Count all krithis.
     */
    suspend fun countAll(): Long = DatabaseFactory.dbQuery {
        KrithisTable.selectAll().count()
    }

    /**
     * Count krithis by workflow state.
     */
    suspend fun countByState(state: WorkflowState): Long = DatabaseFactory.dbQuery {
        KrithisTable.selectAll().where { KrithisTable.workflowState eq state }.count()
    }

    /**
     * Find potential duplicate krithis by normalized title and optional composer/raga.
     *
     * ALWAYS filters by compressed title (space-insensitive) to prevent duplicates caused by
     * spacing differences (e.g. "abayambanayaka" vs "abayamba nayaka").
     * Optionally narrows further by composer/raga when provided.
     */
    suspend fun findDuplicateCandidates(
        titleNormalized: String,
        composerId: UUID? = null,
        ragaId: UUID? = null
    ): List<KrithiDto> = DatabaseFactory.dbQuery {
        val titleCompressed = titleNormalized.replace(" ", "")
        val query = KrithisTable.selectAll()

        // ALWAYS match by compressed title (handles spacing variants)
        query.andWhere {
            (KrithisTable.titleNormalized eq titleNormalized) or
            (CustomStringFunction("REPLACE", KrithisTable.titleNormalized, stringParam(" "), stringParam("")) eq titleCompressed)
        }

        // Optionally narrow further by metadata
        composerId?.let { query.andWhere { KrithisTable.composerId eq it } }
        ragaId?.let { query.andWhere { KrithisTable.primaryRagaId eq it } }

        query.map { it.toKrithiDto() }
    }

    /**
     * Find near-title candidates by compressed-title prefix.
     *
     * This is a bounded fallback for OCR/transliteration drift where exact compressed title
     * comparison fails (for example, single-vowel substitutions in Devanagari OCR output).
     */
    suspend fun findNearTitleCandidates(
        titleNormalized: String,
        limit: Int = 200
    ): List<KrithiDto> = DatabaseFactory.dbQuery {
        val titleCompressed = titleNormalized.replace(" ", "")
        if (titleCompressed.length < 5) {
            return@dbQuery emptyList()
        }

        val prefixLength = minOf(8, titleCompressed.length)
        val prefix = titleCompressed.take(prefixLength)
        val compressedTitleExpr = CustomStringFunction(
            "REPLACE",
            KrithisTable.titleNormalized,
            stringParam(" "),
            stringParam("")
        )

        KrithisTable
            .selectAll()
            .where { compressedTitleExpr like "$prefix%" }
            .limit(limit)
            .map { it.toKrithiDto() }
    }

    /**
     * Find candidates by metadata (composer + optional raga) without title filtering.
     * Used by ExtractionResultProcessor for broad metadata-based search when title matching
     * alone may miss candidates due to significant title differences.
     */
    suspend fun findCandidatesByMetadata(
        composerId: UUID,
        ragaId: UUID? = null
    ): List<KrithiDto> = DatabaseFactory.dbQuery {
        val query = KrithisTable.selectAll()
        query.andWhere { KrithisTable.composerId eq composerId }
        ragaId?.let { query.andWhere { KrithisTable.primaryRagaId eq it } }
        query.map { it.toKrithiDto() }
    }
}
