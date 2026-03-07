package com.sangita.grantha.backend.dal.repositories

import com.sangita.grantha.backend.dal.DatabaseFactory
import com.sangita.grantha.backend.dal.enums.LanguageCode
import com.sangita.grantha.backend.dal.enums.MusicalForm
import com.sangita.grantha.backend.dal.enums.WorkflowState
import com.sangita.grantha.backend.dal.models.toKrithiDto
import com.sangita.grantha.backend.dal.models.toKrithiSectionDto
import com.sangita.grantha.backend.dal.models.toTagDto
import com.sangita.grantha.backend.dal.support.toJavaUuid
import com.sangita.grantha.backend.dal.tables.KrithisTable
import com.sangita.grantha.backend.dal.tables.KrithiRagasTable
import com.sangita.grantha.backend.dal.tables.KrithiSectionsTable
import com.sangita.grantha.backend.dal.tables.KrithiTagsTable
import com.sangita.grantha.backend.dal.tables.TagsTable
import com.sangita.grantha.shared.domain.model.KrithiDto
import com.sangita.grantha.shared.domain.model.KrithiSectionDto
import com.sangita.grantha.shared.domain.model.TagDto
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.util.UUID
import kotlin.uuid.Uuid
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
 * Repository for krithi CRUD, tags, and sections.
 * Search operations are in KrithiSearchRepository.
 * Lyric variant operations are in KrithiLyricRepository.
 */
class KrithiRepository {
    private val logger = LoggerFactory.getLogger(KrithiRepository::class.java)

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

            val existingRagas = KrithiRagasTable
                .selectAll()
                .where { KrithiRagasTable.krithiId eq javaKrithiId }
                .associateBy {
                    it[KrithiRagasTable.ragaId] to it[KrithiRagasTable.orderIndex]
                }

            val newRagasMap = ragas.withIndex().associateBy {
                it.value to it.index
            }

            val toInsert = mutableListOf<Pair<UUID, Int>>()
            val toDelete = mutableListOf<Pair<UUID, Int>>()

            existingRagas.forEach { (key, _) ->
                if (!newRagasMap.containsKey(key)) {
                    toDelete.add(key)
                }
            }

            newRagasMap.forEach { (key, _) ->
                if (!existingRagas.containsKey(key)) {
                    toInsert.add(key)
                }
            }

            if (toDelete.isNotEmpty()) {
                toDelete.forEach { (ragaId, orderIndex) ->
                    KrithiRagasTable.deleteWhere {
                        (KrithiRagasTable.krithiId eq javaKrithiId) and
                        (KrithiRagasTable.ragaId eq ragaId) and
                        (KrithiRagasTable.orderIndex eq orderIndex)
                    }
                }
            }

            if (toInsert.isNotEmpty()) {
                KrithiRagasTable.batchInsert(toInsert) { (ragaId, orderIndex) ->
                    this[KrithiRagasTable.krithiId] = javaKrithiId
                    this[KrithiRagasTable.ragaId] = ragaId
                    this[KrithiRagasTable.orderIndex] = orderIndex
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
     */
    suspend fun updateTags(krithiId: Uuid, tagIds: List<UUID>) = DatabaseFactory.dbQuery {
        val javaKrithiId = krithiId.toJavaUuid()
        logger.info("Updating tags for krithi $krithiId. New tags: $tagIds")

        val existingTags = KrithiTagsTable
            .selectAll()
            .where { KrithiTagsTable.krithiId eq javaKrithiId }
            .associateBy { it[KrithiTagsTable.tagId] }

        val newTagIdsSet = tagIds.toSet()

        val toInsert = mutableListOf<UUID>()
        val toDelete = mutableListOf<UUID>()

        existingTags.forEach { (tagId, _) ->
            if (!newTagIdsSet.contains(tagId)) {
                toDelete.add(tagId)
            }
        }

        newTagIdsSet.forEach { tagId ->
            if (!existingTags.containsKey(tagId)) {
                toInsert.add(tagId)
            }
        }

        if (toDelete.isNotEmpty()) {
            logger.info("Deleting tags: $toDelete")
            KrithiTagsTable.deleteWhere {
                (KrithiTagsTable.krithiId eq javaKrithiId) and
                (KrithiTagsTable.tagId inList toDelete)
            }
        }

        if (toInsert.isNotEmpty()) {
            logger.info("Inserting tags: $toInsert")
            KrithiTagsTable.batchInsert(toInsert) { tagId ->
                this[KrithiTagsTable.krithiId] = javaKrithiId
                this[KrithiTagsTable.tagId] = tagId
                this[KrithiTagsTable.sourceInfo] = "manual"
            }
        }
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
     * Saves krithi sections using efficient UPDATE/INSERT/DELETE operations.
     */
    suspend fun saveSections(krithiId: Uuid, sections: List<Triple<String, Int, String?>>) = DatabaseFactory.dbQuery {
        val now = OffsetDateTime.now(ZoneOffset.UTC)
        val javaKrithiId = krithiId.toJavaUuid()

        val existingSections = KrithiSectionsTable
            .selectAll()
            .where { KrithiSectionsTable.krithiId eq javaKrithiId }
            .associateBy { it[KrithiSectionsTable.orderIndex] }

        val newSectionsMap = sections.associateBy { it.second }

        val toUpdate = mutableListOf<Triple<UUID, String, String?>>()
        val toInsert = mutableListOf<Triple<String, Int, String?>>()
        val toDelete = mutableListOf<UUID>()

        existingSections.forEach { (orderIndex, row) ->
            val existingId = row[KrithiSectionsTable.id].value
            val existingType = row[KrithiSectionsTable.sectionType]
            val existingLabel = row[KrithiSectionsTable.label]
            val newSection = newSectionsMap[orderIndex]

            if (newSection != null) {
                val (newType, _, newLabel) = newSection
                if (newType != existingType || newLabel != existingLabel) {
                    toUpdate.add(Triple(existingId, newType, newLabel))
                }
            } else {
                toDelete.add(existingId)
            }
        }

        newSectionsMap.forEach { (orderIndex, section) ->
            if (!existingSections.containsKey(orderIndex)) {
                toInsert.add(section)
            }
        }

        toUpdate.forEach { (id, sectionType, label) ->
            KrithiSectionsTable.update({ KrithiSectionsTable.id eq id }) {
                it[KrithiSectionsTable.sectionType] = sectionType
                it[KrithiSectionsTable.label] = label
                it[KrithiSectionsTable.updatedAt] = now
            }
        }

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

        if (toDelete.isNotEmpty()) {
            KrithiSectionsTable.deleteWhere {
                KrithiSectionsTable.id inList toDelete
            }
        }
    }
}
