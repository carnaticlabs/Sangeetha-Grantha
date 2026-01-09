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
import org.jetbrains.exposed.v1.jdbc.*
import org.jetbrains.exposed.v1.core.*
import org.jetbrains.exposed.v1.core.JoinType

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

class KrithiRepository {
    suspend fun findById(id: Uuid): KrithiDto? = DatabaseFactory.dbQuery {
        KrithisTable
            .selectAll()
            .where { KrithisTable.id eq id.toJavaUuid() }
            .map { it.toKrithiDto() }
            .singleOrNull()
    }

    suspend fun create(
        title: String,
        titleNormalized: String,
        incipit: String?,
        incipitNormalized: String?,
        composerId: UUID,
        musicalForm: MusicalForm,
        primaryLanguage: LanguageCode,
        primaryRagaId: UUID?,
        talaId: UUID?,
        deityId: UUID?,
        templeId: UUID?,
        isRagamalika: Boolean,
        ragaIds: List<UUID>,
        workflowState: WorkflowState,
        sahityaSummary: String?,
        notes: String?,
    ): KrithiDto = DatabaseFactory.dbQuery {
        val now = OffsetDateTime.now(ZoneOffset.UTC)
        val krithiId = UUID.randomUUID()

        val result = KrithisTable.insert {
            it[id] = krithiId
            it[KrithisTable.title] = title
            it[KrithisTable.titleNormalized] = titleNormalized
            it[KrithisTable.incipit] = incipit
            it[KrithisTable.incipitNormalized] = incipitNormalized
            it[KrithisTable.composerId] = composerId
            it[KrithisTable.musicalForm] = musicalForm
            it[KrithisTable.primaryLanguage] = primaryLanguage
            it[KrithisTable.primaryRagaId] = primaryRagaId
            it[KrithisTable.talaId] = talaId
            it[KrithisTable.deityId] = deityId
            it[KrithisTable.templeId] = templeId
            it[KrithisTable.isRagamalika] = isRagamalika
            it[KrithisTable.workflowState] = workflowState
            it[KrithisTable.sahityaSummary] = sahityaSummary
            it[KrithisTable.notes] = notes
            it[KrithisTable.createdAt] = now
            it[KrithisTable.updatedAt] = now
        }
            .resultedValues
            ?.single()
            ?.toKrithiDto()
            ?: error("Failed to insert krithi")

        if (ragaIds.isNotEmpty()) {
            KrithiRagasTable.batchInsert(ragaIds.withIndex()) { (index, ragaId) ->
                this[KrithiRagasTable.krithiId] = krithiId
                this[KrithiRagasTable.ragaId] = ragaId
                this[KrithiRagasTable.orderIndex] = index
            }
        }

        result
    }

    suspend fun update(
        id: Uuid,
        title: String? = null,
        titleNormalized: String? = null,
        incipit: String? = null,
        incipitNormalized: String? = null,
        composerId: UUID? = null,
        musicalForm: MusicalForm? = null,
        primaryLanguage: LanguageCode? = null,
        primaryRagaId: UUID? = null,
        talaId: UUID? = null,
        deityId: UUID? = null,
        templeId: UUID? = null,
        isRagamalika: Boolean? = null,
        ragaIds: List<UUID>? = null,
        workflowState: WorkflowState? = null,
        sahityaSummary: String? = null,
        notes: String? = null,
    ): KrithiDto? = DatabaseFactory.dbQuery {
        val now = OffsetDateTime.now(ZoneOffset.UTC)
        val javaId = id.toJavaUuid()

        // Use Exposed 1.0.0-rc-4 updateReturning to update and fetch the row in one round-trip
        val updatedKrithi = KrithisTable
            .updateReturning(
                where = { KrithisTable.id eq javaId }
            ) {
                title?.let { value -> it[KrithisTable.title] = value }
                titleNormalized?.let { value -> it[KrithisTable.titleNormalized] = value }
                incipit?.let { value -> it[KrithisTable.incipit] = value }
                incipitNormalized?.let { value -> it[KrithisTable.incipitNormalized] = value }
                composerId?.let { value -> it[KrithisTable.composerId] = value }
                musicalForm?.let { value -> it[KrithisTable.musicalForm] = value }
                primaryLanguage?.let { value -> it[KrithisTable.primaryLanguage] = value }
                primaryRagaId?.let { value -> it[KrithisTable.primaryRagaId] = value }
                talaId?.let { value -> it[KrithisTable.talaId] = value }
                deityId?.let { value -> it[KrithisTable.deityId] = value }
                templeId?.let { value -> it[KrithisTable.templeId] = value }
                isRagamalika?.let { value -> it[KrithisTable.isRagamalika] = value }
                workflowState?.let { value -> it[KrithisTable.workflowState] = value }
                sahityaSummary?.let { value -> it[KrithisTable.sahityaSummary] = value }
                notes?.let { value -> it[KrithisTable.notes] = value }
                it[KrithisTable.updatedAt] = now
            }
            .singleOrNull()
            ?.toKrithiDto()

        if (updatedKrithi == null) {
            return@dbQuery null
        }

        ragaIds?.let { ragas ->
            val javaKrithiId = id.toJavaUuid()
            
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

    suspend fun getTags(krithiId: Uuid): List<TagDto> = DatabaseFactory.dbQuery {
        val javaKrithiId = krithiId.toJavaUuid()
        (KrithiTagsTable innerJoin TagsTable)
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
            KrithiTagsTable.deleteWhere {
                (KrithiTagsTable.krithiId eq javaKrithiId) and
                (KrithiTagsTable.tagId inList toDelete)
            }
        }
        
        // Execute inserts - only new tags
        if (toInsert.isNotEmpty()) {
            KrithiTagsTable.batchInsert(toInsert) { tagId ->
                this[KrithiTagsTable.krithiId] = javaKrithiId
                this[KrithiTagsTable.tagId] = tagId
                this[KrithiTagsTable.sourceInfo] = "manual"
                // confidence defaults to null
            }
        }
        
        // Tags that remain unchanged preserve their sourceInfo and confidence automatically
    }

    suspend fun getSections(krithiId: Uuid): List<KrithiSectionDto> = DatabaseFactory.dbQuery {
        KrithiSectionsTable
            .selectAll()
            .where { KrithiSectionsTable.krithiId eq krithiId.toJavaUuid() }
            .orderBy(KrithiSectionsTable.orderIndex)
            .map { it.toKrithiSectionDto() }
    }

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

    suspend fun findLyricVariantById(variantId: Uuid): KrithiLyricVariantDto? = DatabaseFactory.dbQuery {
        KrithiLyricVariantsTable
            .selectAll()
            .where { KrithiLyricVariantsTable.id eq variantId.toJavaUuid() }
            .map { it.toKrithiLyricVariantDto() }
            .singleOrNull()
    }

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
        val variantId = UUID.randomUUID()
        
        KrithiLyricVariantsTable.insert {
            it[id] = variantId
            it[KrithiLyricVariantsTable.krithiId] = krithiId.toJavaUuid()
            it[KrithiLyricVariantsTable.language] = language
            it[KrithiLyricVariantsTable.script] = script
            it[KrithiLyricVariantsTable.transliterationScheme] = transliterationScheme
            it[KrithiLyricVariantsTable.sampradayaId] = sampradayaId
            it[KrithiLyricVariantsTable.variantLabel] = variantLabel
            it[KrithiLyricVariantsTable.sourceReference] = sourceReference
            it[KrithiLyricVariantsTable.lyrics] = lyrics
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

    suspend fun search(
        filters: KrithiSearchFilters,
        page: Int,
        pageSize: Int,
        publishedOnly: Boolean = true,
    ): KrithiSearchResult = DatabaseFactory.dbQuery {
        val baseQuery = KrithisTable.selectAll()
        val safePage = page.coerceAtLeast(0)
        val safeSize = pageSize.coerceIn(1, 200)

        if (publishedOnly) {
            baseQuery.andWhere { KrithisTable.workflowState eq WorkflowState.PUBLISHED }
        }

        filters.query?.trim()?.takeIf { it.isNotEmpty() }?.let { query ->
            val token = "%${query.lowercase()}%"
            
            // Search in composer names
            val matchingComposerIds = ComposersTable
                .selectAll()
                .where { ComposersTable.nameNormalized like token }
                .map { it[ComposersTable.id].value }
                .distinct()
            
            // Search in raga names (through krithi_ragas join)
            val matchingRagaIds = RagasTable
                .selectAll()
                .where { RagasTable.nameNormalized like token }
                .map { it[RagasTable.id].value }
                .distinct()
            
            val krithiIdsWithMatchingRagas = if (matchingRagaIds.isNotEmpty()) {
                KrithiRagasTable
                    .selectAll()
                    .where { KrithiRagasTable.ragaId inList matchingRagaIds }
                    .map { it[KrithiRagasTable.krithiId] }
                    .distinct()
            } else {
                emptyList()
            }
            
            // Build OR condition: title/incipit OR composer match OR raga match
            baseQuery.andWhere {
                var condition = (KrithisTable.titleNormalized like token) or 
                    (KrithisTable.incipitNormalized like token)
                
                if (matchingComposerIds.isNotEmpty()) {
                    condition = condition or (KrithisTable.composerId inList matchingComposerIds)
                }
                
                if (krithiIdsWithMatchingRagas.isNotEmpty()) {
                    condition = condition or (KrithisTable.id inList krithiIdsWithMatchingRagas)
                }
                
                condition
            }
        }

        filters.lyric?.trim()?.takeIf { it.isNotEmpty() }?.let { lyric ->
            val token = "%${lyric.lowercase()}%"
            val krithiIds = KrithiLyricVariantsTable
                .selectAll()
                .where { KrithiLyricVariantsTable.lyrics like token }
                .map { it[KrithiLyricVariantsTable.krithiId] }
                .distinct()

            if (krithiIds.isEmpty()) {
                return@dbQuery KrithiSearchResult(
                    items = emptyList(),
                    total = 0,
                    page = safePage,
                    pageSize = safeSize
                )
            }

            baseQuery.andWhere { KrithisTable.id inList krithiIds }
        }

        filters.composerId?.let { baseQuery.andWhere { KrithisTable.composerId eq it } }
        filters.ragaId?.let { baseQuery.andWhere { KrithisTable.primaryRagaId eq it } }
        filters.talaId?.let { baseQuery.andWhere { KrithisTable.talaId eq it } }
        filters.deityId?.let { baseQuery.andWhere { KrithisTable.deityId eq it } }
        filters.templeId?.let { baseQuery.andWhere { KrithisTable.templeId eq it } }
        filters.primaryLanguage?.let { baseQuery.andWhere { KrithisTable.primaryLanguage eq it } }

        val total = baseQuery.count()
        val offset = (safePage * safeSize).toLong()

        val krithiDtos = baseQuery
            .limit(safeSize)
            .offset(offset)
            .map { it.toKrithiDto() }

        // Enrich with composer names and ragas
        val krithiIds = krithiDtos.map { it.id.toJavaUuid() }
        val composerIds = krithiDtos.map { it.composerId.toJavaUuid() }.distinct()
        
        val composersMap = if (composerIds.isNotEmpty()) {
            ComposersTable
                .selectAll()
                .where { ComposersTable.id inList composerIds }
                .associate { it[ComposersTable.id].value to it[ComposersTable.name] }
        } else {
            emptyMap()
        }
        
        val ragasMap: Map<UUID, List<RagaRefDto>> = if (krithiIds.isNotEmpty()) {
            KrithiRagasTable
                .join(
                    RagasTable,
                    joinType = JoinType.INNER,
                    onColumn = KrithiRagasTable.ragaId,
                    otherColumn = RagasTable.id,
                )
                .select(KrithiRagasTable.krithiId, KrithiRagasTable.orderIndex, RagasTable.id, RagasTable.name)
                .where { KrithiRagasTable.krithiId inList krithiIds }
                .groupBy { it[KrithiRagasTable.krithiId] }
                .mapValues { (_, rows) ->
                    rows.map { row ->
                        RagaRefDto(
                            id = row[RagasTable.id].value.toKotlinUuid(),
                            name = row[RagasTable.name],
                            orderIndex = row[KrithiRagasTable.orderIndex]
                        )
                    }.sortedBy { it.orderIndex }
                }
        } else {
            emptyMap<UUID, List<RagaRefDto>>()
        }
        
        val items = krithiDtos.map { dto ->
            KrithiSummary(
                id = dto.id,
                name = dto.title,
                composerName = composersMap[dto.composerId.toJavaUuid()] ?: "Unknown",
                primaryLanguage = dto.primaryLanguage,
                ragas = ragasMap[dto.id.toJavaUuid()] ?: emptyList()
            )
        }

        KrithiSearchResult(
            items = items,
            total = total,
            page = safePage,
            pageSize = safeSize
        )
    }

    suspend fun countAll(): Long = DatabaseFactory.dbQuery {
        KrithisTable.selectAll().count()
    }

    suspend fun countByState(state: WorkflowState): Long = DatabaseFactory.dbQuery {
        KrithisTable.selectAll().where { KrithisTable.workflowState eq state }.count()
    }
}
