package com.sangita.grantha.backend.dal.repositories

import com.sangita.grantha.backend.dal.DatabaseFactory
import com.sangita.grantha.backend.dal.enums.LanguageCode
import com.sangita.grantha.backend.dal.enums.WorkflowState
import com.sangita.grantha.backend.dal.models.toKrithiDto
import com.sangita.grantha.backend.dal.support.toJavaUuid
import com.sangita.grantha.backend.dal.tables.KrithisTable
import com.sangita.grantha.backend.dal.tables.KrithiLyricVariantsTable
import com.sangita.grantha.backend.dal.tables.KrithiRagasTable
import com.sangita.grantha.shared.domain.model.KrithiDto
import com.sangita.grantha.shared.domain.model.KrithiSearchResult
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.util.UUID
import kotlin.uuid.Uuid
import org.jetbrains.exposed.v1.jdbc.*
import org.jetbrains.exposed.v1.core.*

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
        composerId: UUID? = null,
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
            composerId?.let { value -> it[KrithisTable.composerId] = value }
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
            baseQuery.andWhere {
                (KrithisTable.titleNormalized like token) or (KrithisTable.incipitNormalized like token)
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

        val items = baseQuery
            .limit(safeSize)
            .offset(offset)
            .map { it.toKrithiDto() }

        KrithiSearchResult(
            items = items,
            total = total,
            page = safePage,
            pageSize = safeSize
        )
    }
}
