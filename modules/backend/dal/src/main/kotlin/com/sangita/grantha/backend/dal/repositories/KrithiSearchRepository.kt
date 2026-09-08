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
import com.sangita.grantha.shared.domain.model.SemanticSearchResultItem
import java.util.UUID
import org.jetbrains.exposed.v1.jdbc.*
import org.jetbrains.exposed.v1.core.*
import org.jetbrains.exposed.v1.core.java.UUIDColumnType
import org.jetbrains.exposed.v1.core.statements.StatementType

/**
 * The single embedding profile retrieval is bound to (TRACK-108).
 *
 * `activeCount` > 1 means the index is mid-switch or was activated outside
 * the scripts' atomic path; callers should warn but still use `id` (newest).
 */
data class EmbeddingProfileRef(
    val id: UUID,
    val modelName: String,
    val dimensions: Int,
    val activeCount: Int,
)

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
     * Resolves the active embedding profile, or null when nothing has been indexed yet.
     * Semantic/hybrid queries take the returned id so the vector filter and the
     * profile check cannot diverge within one request.
     */
    suspend fun activeEmbeddingProfile(): EmbeddingProfileRef? = DatabaseFactory.dbQuery {
        var profile: EmbeddingProfileRef? = null
        exec(
            """
            SELECT id, model_name, dimensions,
                   (SELECT count(*) FROM embedding_profiles WHERE is_active = true) AS active_count
            FROM embedding_profiles
            WHERE is_active = true
            ORDER BY created_at DESC
            LIMIT 1
            """.trimIndent(),
        ) { rs ->
            if (rs.next()) {
                profile = EmbeddingProfileRef(
                    id = UUID.fromString(rs.getString("id")),
                    modelName = rs.getString("model_name"),
                    dimensions = rs.getInt("dimensions"),
                    activeCount = rs.getInt("active_count"),
                )
            }
        }
        profile
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
        filters.ragaId?.let { idQuery.andWhere { KrithiRagasTable.ragaId eq it } }
        filters.talaId?.let { idQuery.andWhere { KrithisTable.talaId eq it } }
        filters.deityId?.let { idQuery.andWhere { KrithisTable.deityId eq it } }
        filters.templeId?.let { idQuery.andWhere { KrithisTable.templeId eq it } }
        filters.primaryLanguage?.let { idQuery.andWhere { KrithisTable.primaryLanguage eq it } }

        idQuery.orderBy(KrithisTable.titleNormalized to SortOrder.ASC)

        val total = idQuery.count()
        val orderedIds = idQuery.limit(safeSize).offset(offset).map { it[KrithisTable.id].value }
        if (orderedIds.isEmpty()) {
            return@dbQuery KrithiSearchResult(
                items = emptyList(),
                total = total,
                page = safePage,
                pageSize = safeSize,
            )
        }

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
            .where { KrithisTable.id inList orderedIds }

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

        val items = orderedIds.mapNotNull { id ->
            val summary = summaries[id] ?: return@mapNotNull null
            val ragas = ragasByKrithi[id]?.sortedBy { it.orderIndex } ?: emptyList()
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

    /**
     * Pure vector retrieval against one embedding profile (see [activeEmbeddingProfile]).
     */
    suspend fun searchSemantic(
        queryVector: List<Float>,
        profileId: UUID,
        composerId: UUID? = null,
        ragaId: UUID? = null,
        limit: Int = 20,
        publishedOnly: Boolean = true,
    ): List<SemanticSearchResultItem> = DatabaseFactory.dbQuery {
        val safeLimit = limit.coerceIn(1, 100)
        val candidateLimit = (safeLimit * 25).coerceAtLeast(500)
        val args = mutableListOf<Pair<IColumnType<*>, Any?>>()

        exec("SET LOCAL hnsw.iterative_scan = 'strict_order'")
        exec("SET LOCAL hnsw.ef_search = 100")

        val sql = buildString {
            append("""
                WITH params AS (
                    SELECT CAST(? AS text)::vector(768) AS qvec
                ),
                semantic_candidates AS (
                    SELECT 
                        k.id AS krithi_id,
                        k.title AS title,
                        c.name AS composer_name,
                        r.name AS raga_name,
                        t.name AS tala_name,
                        d.document_kind AS document_kind,
                        d.indexed_content AS matched_content,
                        1 - (e.embedding <=> params.qvec) AS similarity
                    FROM document_embeddings e
                    CROSS JOIN params
                    JOIN search_documents d ON e.document_id = d.id
                    JOIN krithis k ON d.krithi_id = k.id
                    JOIN composers c ON k.composer_id = c.id
                    LEFT JOIN ragas r ON k.primary_raga_id = r.id
                    LEFT JOIN talas t ON k.tala_id = t.id
                    WHERE e.profile_id = ?
            """.trimIndent())
            args += TextColumnType() to toVectorLiteral(queryVector)
            args += UUIDColumnType() to profileId
            appendScopeFilters(this, args, publishedOnly, composerId, ragaId)
            append("""
                    ORDER BY e.embedding <=> params.qvec ASC
                    LIMIT ?
                ),
                ranked_krithis AS (
                    SELECT 
                        *,
                        ROW_NUMBER() OVER (
                            PARTITION BY krithi_id 
                            ORDER BY 
                                (similarity + CASE WHEN document_kind = 'COMPOSITION_OVERVIEW' THEN 0.015 ELSE 0.0 END) DESC,
                                similarity DESC
                        ) AS krithi_doc_rank
                    FROM semantic_candidates
                )
                SELECT 
                    krithi_id,
                    title,
                    composer_name,
                    raga_name,
                    tala_name,
                    document_kind,
                    matched_content,
                    similarity
                FROM ranked_krithis
                WHERE krithi_doc_rank = 1
                ORDER BY similarity DESC
                LIMIT ?
            """.trimIndent())
            args += IntegerColumnType() to candidateLimit
            args += IntegerColumnType() to safeLimit
        }

        val items = mutableListOf<SemanticSearchResultItem>()
        exec(sql, args, StatementType.SELECT) { rs ->
            while (rs.next()) {
                items.add(rs.toSemanticItem(hybridScores = false))
            }
        }
        items
    }

    /**
     * RRF fusion of vector and trigram retrieval. When [profileId] is null (nothing indexed yet)
     * the vector branch is disabled and results are lexical-only; [queryVector] may then be null.
     */
    suspend fun searchHybrid(
        rawQuery: String,
        queryVector: List<Float>?,
        profileId: UUID?,
        composerId: UUID? = null,
        ragaId: UUID? = null,
        limit: Int = 20,
        publishedOnly: Boolean = true,
    ): List<SemanticSearchResultItem> = DatabaseFactory.dbQuery {
        val safeLimit = limit.coerceIn(1, 100)
        val tokens = rawQuery.trim().split("\\s+".toRegex()).filter { it.length >= 2 }
        val args = mutableListOf<Pair<IColumnType<*>, Any?>>()
        val vectorBranch = profileId != null && queryVector != null

        exec("SET LOCAL hnsw.iterative_scan = 'strict_order'")
        exec("SET LOCAL hnsw.ef_search = 100")

        val sql = buildString {
            append("""
                WITH params AS (
                    SELECT CAST(? AS text)::vector(768) AS qvec, CAST(? AS text) AS qtext
                ),
                semantic_search AS (
                    SELECT 
                        k.id AS krithi_id,
                        d.id AS doc_id,
                        k.title,
                        c.name AS composer_name,
                        r.name AS raga_name,
                        t.name AS tala_name,
                        d.document_kind,
                        d.indexed_content,
                        1 - (e.embedding <=> params.qvec) AS similarity,
                        ROW_NUMBER() OVER (ORDER BY e.embedding <=> params.qvec ASC) AS rank
                    FROM document_embeddings e
                    CROSS JOIN params
                    JOIN search_documents d ON e.document_id = d.id
                    JOIN krithis k ON d.krithi_id = k.id
                    JOIN composers c ON k.composer_id = c.id
                    LEFT JOIN ragas r ON k.primary_raga_id = r.id
                    LEFT JOIN talas t ON k.tala_id = t.id
            """.trimIndent())
            args += TextColumnType() to (if (vectorBranch) toVectorLiteral(queryVector!!) else null)
            args += TextColumnType() to rawQuery
            if (vectorBranch) {
                append("\n                    WHERE e.profile_id = ?")
                args += UUIDColumnType() to profileId
            } else {
                append("\n                    WHERE FALSE")
            }
            appendScopeFilters(this, args, publishedOnly, composerId, ragaId)
            append("""
                    ORDER BY e.embedding <=> params.qvec ASC
                    LIMIT 500
                ),
                lexical_search AS (
                    SELECT 
                        k.id AS krithi_id,
                        d.id AS doc_id,
                        k.title,
                        c.name AS composer_name,
                        r.name AS raga_name,
                        t.name AS tala_name,
                        d.document_kind,
                        d.indexed_content,
                        word_similarity(params.qtext, d.indexed_content) AS lex_score,
                        ROW_NUMBER() OVER (ORDER BY word_similarity(params.qtext, d.indexed_content) DESC) AS rank
                    FROM search_documents d
                    CROSS JOIN params
                    JOIN krithis k ON d.krithi_id = k.id
                    JOIN composers c ON k.composer_id = c.id
                    LEFT JOIN ragas r ON k.primary_raga_id = r.id
                    LEFT JOIN talas t ON k.tala_id = t.id
                    WHERE 1 = 1 
            """.trimIndent())
            appendScopeFilters(this, args, publishedOnly, composerId, ragaId)
            append("\n                    AND (\n")
            append("""
                            word_similarity(params.qtext, d.indexed_content) >= 0.3
                            OR d.indexed_content ILIKE '%' || params.qtext || '%'
            """.trimIndent())
            if (tokens.size > 1) {
                append(" OR (")
                append(tokens.joinToString(" AND ") {
                    args += TextColumnType() to it
                    "d.indexed_content ILIKE '%' || ? || '%'"
                })
                append(")")
            }
            append("""
                        )
                    ORDER BY word_similarity(params.qtext, d.indexed_content) DESC
                    LIMIT 500
                ),
                fused_docs AS (
                    SELECT 
                        COALESCE(s.krithi_id, l.krithi_id) AS krithi_id,
                        COALESCE(s.title, l.title) AS title,
                        COALESCE(s.composer_name, l.composer_name) AS composer_name,
                        COALESCE(s.raga_name, l.raga_name) AS raga_name,
                        COALESCE(s.tala_name, l.tala_name) AS tala_name,
                        COALESCE(s.document_kind, l.document_kind) AS document_kind,
                        COALESCE(s.indexed_content, l.indexed_content) AS matched_content,
                        s.similarity AS similarity,
                        l.lex_score AS lex_score,
                        (COALESCE(1.0 / (60 + s.rank), 0.0) + COALESCE(1.0 / (60 + l.rank), 0.0)) AS rrf_score
                    FROM semantic_search s
                    FULL OUTER JOIN lexical_search l ON s.doc_id = l.doc_id
                ),
                ranked_krithis AS (
                    SELECT 
                        *,
                        ROW_NUMBER() OVER (
                            PARTITION BY krithi_id 
                            ORDER BY 
                                (rrf_score + CASE WHEN document_kind = 'COMPOSITION_OVERVIEW' THEN 0.001 ELSE 0.0 END) DESC,
                                rrf_score DESC, 
                                similarity DESC NULLS LAST, 
                                lex_score DESC NULLS LAST
                        ) AS krithi_doc_rank
                    FROM fused_docs
                )
                SELECT 
                    krithi_id,
                    title,
                    composer_name,
                    raga_name,
                    tala_name,
                    document_kind,
                    matched_content,
                    similarity,
                    lex_score,
                    rrf_score
                FROM ranked_krithis
                WHERE krithi_doc_rank = 1
                ORDER BY rrf_score DESC
                LIMIT ?
            """.trimIndent())
            args += IntegerColumnType() to safeLimit
        }

        val items = mutableListOf<SemanticSearchResultItem>()
        exec(sql, args, StatementType.SELECT) { rs ->
            while (rs.next()) {
                items.add(rs.toSemanticItem(hybridScores = true))
            }
        }
        items
    }

    private fun toVectorLiteral(queryVector: List<Float>): String =
        queryVector.joinToString(prefix = "[", postfix = "]", separator = ",") { it.toString() }

    private fun appendScopeFilters(
        sql: StringBuilder,
        args: MutableList<Pair<IColumnType<*>, Any?>>,
        publishedOnly: Boolean,
        composerId: UUID?,
        ragaId: UUID?,
    ) {
        if (publishedOnly) {
            sql.append(" AND d.is_published = true AND k.workflow_state = 'published' ")
        }
        if (composerId != null) {
            sql.append(" AND k.composer_id = ? ")
            args += UUIDColumnType() to composerId
        }
        if (ragaId != null) {
            sql.append(
                " AND (k.primary_raga_id = ? OR EXISTS (SELECT 1 FROM krithi_ragas kr WHERE kr.krithi_id = k.id AND kr.raga_id = ?)) "
            )
            args += UUIDColumnType() to ragaId
            args += UUIDColumnType() to ragaId
        }
    }

    private fun java.sql.ResultSet.toSemanticItem(hybridScores: Boolean): SemanticSearchResultItem {
        val similarity = getObject("similarity")?.let { (it as Number).toDouble() } ?: 0.0
        return SemanticSearchResultItem(
            krithiId = UUID.fromString(getString("krithi_id")).toKotlinUuid(),
            title = getString("title"),
            composerName = getString("composer_name"),
            ragaName = getString("raga_name"),
            talaName = getString("tala_name"),
            documentKind = getString("document_kind"),
            matchedContent = getString("matched_content"),
            similarityScore = similarity,
            lexicalScore = if (hybridScores) getObject("lex_score")?.let { (it as Number).toDouble() } else null,
            rrfScore = if (hybridScores) getObject("rrf_score")?.let { (it as Number).toDouble() } else null,
        )
    }
}
