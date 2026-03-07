package com.sangita.grantha.backend.dal.repositories

import com.sangita.grantha.backend.dal.DatabaseFactory
import com.sangita.grantha.backend.dal.enums.WorkflowState
import com.sangita.grantha.backend.dal.models.toKrithiDto
import com.sangita.grantha.backend.dal.support.toKotlinUuid
import com.sangita.grantha.backend.dal.support.toJavaUuid
import com.sangita.grantha.backend.dal.tables.ComposersTable
import com.sangita.grantha.backend.dal.tables.KrithisTable
import com.sangita.grantha.backend.dal.tables.KrithiLyricVariantsTable
import com.sangita.grantha.backend.dal.tables.KrithiRagasTable
import com.sangita.grantha.backend.dal.tables.RagasTable
import com.sangita.grantha.shared.domain.model.KrithiDto
import com.sangita.grantha.shared.domain.model.KrithiSearchResult
import com.sangita.grantha.shared.domain.model.KrithiSummary
import com.sangita.grantha.shared.domain.model.RagaRefDto
import java.util.UUID
import org.jetbrains.exposed.v1.jdbc.*
import org.jetbrains.exposed.v1.core.*

/**
 * Repository for krithi search, duplicate detection, and counting.
 * Extracted from KrithiRepository as part of TRACK-074.
 */
class KrithiSearchRepository {
    companion object {
        const val MIN_PAGE_SIZE = 1
        const val MAX_PAGE_SIZE = 200
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
            summaries.getOrPut(krithiId) {
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
     */
    suspend fun findDuplicateCandidates(
        titleNormalized: String,
        composerId: UUID? = null,
        ragaId: UUID? = null
    ): List<KrithiDto> = DatabaseFactory.dbQuery {
        val titleCompressed = titleNormalized.replace(" ", "")
        val query = KrithisTable.selectAll()

        query.andWhere {
            (KrithisTable.titleNormalized eq titleNormalized) or
            (CustomStringFunction("REPLACE", KrithisTable.titleNormalized, stringParam(" "), stringParam("")) eq titleCompressed)
        }

        composerId?.let { query.andWhere { KrithisTable.composerId eq it } }
        ragaId?.let { query.andWhere { KrithisTable.primaryRagaId eq it } }

        query.map { it.toKrithiDto() }
    }

    /**
     * Find near-title candidates by compressed-title prefix.
     */
    suspend fun findNearTitleCandidates(
        titleNormalized: String,
        limit: Int = 200
    ): List<KrithiDto> = DatabaseFactory.dbQuery {
        val titleCompressed = titleNormalized.replace(" ", "")
        if (titleCompressed.length < 5) {
            return@dbQuery emptyList()
        }

        val prefixLength = minOf(12, titleCompressed.length)
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
