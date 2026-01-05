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

        KrithisTable.insert {
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

        if (ragaIds.isNotEmpty()) {
            KrithiRagasTable.batchInsert(ragaIds.withIndex()) { (index, ragaId) ->
                this[KrithiRagasTable.krithiId] = krithiId
                this[KrithiRagasTable.ragaId] = ragaId
                this[KrithiRagasTable.orderIndex] = index
            }
        }

        KrithisTable
            .selectAll()
            .where { KrithisTable.id eq krithiId }
            .map { it.toKrithiDto() }
            .single()
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
        val updated = KrithisTable.update({ KrithisTable.id eq id.toJavaUuid() }) {
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

        if (updated == 0) {
            return@dbQuery null
        }

        ragaIds?.let { ragas ->
            KrithiRagasTable.deleteWhere { KrithiRagasTable.krithiId eq id.toJavaUuid() }
            if (ragas.isNotEmpty()) {
                KrithiRagasTable.batchInsert(ragas.withIndex()) { (index, ragaId) ->
                    this[KrithiRagasTable.krithiId] = id.toJavaUuid()
                    this[KrithiRagasTable.ragaId] = ragaId
                    this[KrithiRagasTable.orderIndex] = index
                }
            }
        }

        KrithisTable
            .selectAll()
            .where { KrithisTable.id eq id.toJavaUuid() }
            .map { it.toKrithiDto() }
            .singleOrNull()
    }

    suspend fun getTags(krithiId: Uuid): List<TagDto> = DatabaseFactory.dbQuery {
        val javaKrithiId = krithiId.toJavaUuid()
        (KrithiTagsTable innerJoin TagsTable)
            .selectAll()
            .where { KrithiTagsTable.krithiId eq javaKrithiId }
            .map { it.toTagDto() }
    }

    suspend fun updateTags(krithiId: Uuid, tagIds: List<UUID>) = DatabaseFactory.dbQuery {
        // Delete existing tags
        KrithiTagsTable.deleteWhere { KrithiTagsTable.krithiId eq krithiId.toJavaUuid() }
        
        // Insert new tags
        if (tagIds.isNotEmpty()) {
            KrithiTagsTable.batchInsert(tagIds) { tagId ->
                this[KrithiTagsTable.krithiId] = krithiId.toJavaUuid()
                this[KrithiTagsTable.tagId] = tagId
                this[KrithiTagsTable.sourceInfo] = "manual"
            }
        }
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
        
        KrithiLyricVariantsTable
            .selectAll()
            .where { KrithiLyricVariantsTable.id eq variantId }
            .map { it.toKrithiLyricVariantDto() }
            .single()
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
        val updated = KrithiLyricVariantsTable.update({ KrithiLyricVariantsTable.id eq javaVariantId }) {
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
        
        if (updated == 0) {
            return@dbQuery null
        }
        
        KrithiLyricVariantsTable
            .selectAll()
            .where { KrithiLyricVariantsTable.id eq javaVariantId }
            .map { it.toKrithiLyricVariantDto() }
            .singleOrNull()
    }

    suspend fun saveLyricVariantSections(
        variantId: Uuid,
        sections: List<Pair<UUID, String>> // (sectionId, text)
    ) = DatabaseFactory.dbQuery {
        val now = OffsetDateTime.now(ZoneOffset.UTC)
        val javaVariantId = variantId.toJavaUuid()
        
        // Delete existing sections for this variant
        KrithiLyricSectionsTable.deleteWhere { 
            KrithiLyricSectionsTable.lyricVariantId eq javaVariantId 
        }
        
        // Insert new sections
        if (sections.isNotEmpty()) {
            KrithiLyricSectionsTable.batchInsert(sections) { (sectionId, text) ->
                this[KrithiLyricSectionsTable.id] = UUID.randomUUID()
                this[KrithiLyricSectionsTable.lyricVariantId] = javaVariantId
                this[KrithiLyricSectionsTable.sectionId] = sectionId
                this[KrithiLyricSectionsTable.text] = text
                this[KrithiLyricSectionsTable.normalizedText] = null // Can be computed later if needed
                this[KrithiLyricSectionsTable.createdAt] = now
                this[KrithiLyricSectionsTable.updatedAt] = now
            }
        }
    }

    suspend fun saveSections(krithiId: Uuid, sections: List<Pair<String, Int>>) = DatabaseFactory.dbQuery {
        val now = OffsetDateTime.now(ZoneOffset.UTC)
        val javaKrithiId = krithiId.toJavaUuid()
        
        // Delete existing sections
        KrithiSectionsTable.deleteWhere { KrithiSectionsTable.krithiId eq javaKrithiId }
        
        // Insert new sections
        if (sections.isNotEmpty()) {
            KrithiSectionsTable.batchInsert(sections.withIndex()) { (index, section) ->
                val (sectionType, orderIndex) = section
                this[KrithiSectionsTable.id] = UUID.randomUUID()
                this[KrithiSectionsTable.krithiId] = javaKrithiId
                this[KrithiSectionsTable.sectionType] = sectionType
                this[KrithiSectionsTable.orderIndex] = orderIndex
                this[KrithiSectionsTable.label] = null
                this[KrithiSectionsTable.notes] = null
                this[KrithiSectionsTable.createdAt] = now
                this[KrithiSectionsTable.updatedAt] = now
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
