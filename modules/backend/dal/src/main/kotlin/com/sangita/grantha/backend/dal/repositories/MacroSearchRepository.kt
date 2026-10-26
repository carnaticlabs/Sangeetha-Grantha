package com.sangita.grantha.backend.dal.repositories

import com.sangita.grantha.backend.dal.DatabaseFactory
import java.util.UUID
import org.jetbrains.exposed.v1.core.IColumnType
import org.jetbrains.exposed.v1.core.IntegerColumnType
import org.jetbrains.exposed.v1.core.TextColumnType
import org.jetbrains.exposed.v1.core.java.UUIDColumnType
import org.jetbrains.exposed.v1.core.statements.StatementType
import org.jetbrains.exposed.v1.jdbc.JdbcTransaction

enum class MacroAnchorKind { COLLECTION, KSHETRA }

/** One best-chunk hit per anchor (a cycle tag or a temple). Eligibility and cards are the caller's concern. */
data class MacroCandidate(
    val kind: MacroAnchorKind,
    val anchorId: UUID,
    /** Stored overview text of the matching chunk (`original_content`). */
    val content: String,
    val similarity: Double?,
    val lexicalScore: Double?,
    val rrfScore: Double?,
)

/**
 * Retrieval over `CYCLE_OVERVIEW` / `KSHETRA_OVERVIEW` documents (TRACK-152), kept beside
 * [KrithiSearchRepository] so the kriti SQL, ranks and `krithi_doc_rank` partition are never edited.
 *
 * Callers pass the eligible anchors ([CatalogueRepository.searchableCollections],
 * [CatalogueRepository.searchableTemples]); this class never restates an eligibility rule. The
 * eligible documents are materialised first and scored by exact distance, because an HNSW scan
 * with a join filter can under-return over a ~100-document subset.
 */
class MacroSearchRepository {
    /**
     * RRF of a vector branch and a trigram/containment branch. With a null [profileId] the vector
     * branch is disabled and the result is lexical-only (the no-profile fallback).
     */
    suspend fun searchHybrid(
        rawQuery: String,
        queryVector: List<Float>?,
        profileId: UUID?,
        tagIds: Collection<UUID>,
        templeIds: Collection<UUID>,
        limit: Int = DEFAULT_LIMIT,
    ): List<MacroCandidate> {
        if (tagIds.isEmpty() && templeIds.isEmpty()) return emptyList()
        return DatabaseFactory.dbQuery {
            val tokens = rawQuery.trim().split("\\s+".toRegex()).filter { it.length >= 2 }
            val vectorBranch = profileId != null && queryVector != null
            val args = mutableListOf<Pair<IColumnType<*>, Any?>>()
            val sql = buildString {
                append(PARAMS_AND_ELIGIBLE)
                args += TextColumnType() to queryVector?.let(::toVectorLiteral)
                args += TextColumnType() to rawQuery
                args += TextColumnType() to uuidArray(tagIds)
                args += TextColumnType() to uuidArray(templeIds)
                append(
                    """
                    ,
                    semantic AS (
                        SELECT x.id AS doc_id,
                               1 - (e.embedding <=> params.qvec) AS similarity,
                               ROW_NUMBER() OVER (ORDER BY e.embedding <=> params.qvec ASC) AS rnk
                        FROM eligible x
                        CROSS JOIN params
                        JOIN document_embeddings e ON e.document_id = x.id
                        WHERE
                    """.trimIndent(),
                )
                if (vectorBranch) {
                    append(" e.profile_id = ?")
                    args += UUIDColumnType() to profileId
                } else {
                    append(" FALSE")
                }
                append(
                    """

                    ),
                    lexical AS (
                        SELECT x.id AS doc_id,
                               word_similarity(params.qtext, x.indexed_content) AS lex_score,
                               ROW_NUMBER() OVER (ORDER BY word_similarity(params.qtext, x.indexed_content) DESC) AS rnk
                        FROM eligible x
                        CROSS JOIN params
                        WHERE (word_similarity(params.qtext, x.indexed_content) >= 0.3
                           OR x.indexed_content ILIKE '%' || params.qtext || '%'
                    """.trimIndent(),
                )
                if (tokens.size > 1) {
                    append(" OR (")
                    append(
                        tokens.joinToString(" AND ") {
                            args += TextColumnType() to it
                            "x.indexed_content ILIKE '%' || ? || '%'"
                        },
                    )
                    append(")")
                }
                append(
                    """
                    )
                    ),
                    fused AS (
                        SELECT COALESCE(s.doc_id, l.doc_id) AS doc_id,
                               s.similarity, l.lex_score,
                               (COALESCE(1.0 / (60 + s.rnk), 0.0) + COALESCE(1.0 / (60 + l.rnk), 0.0)) AS rrf_score
                        FROM semantic s FULL OUTER JOIN lexical l ON s.doc_id = l.doc_id
                    ),
                    per_anchor AS (
                        SELECT x.document_kind, COALESCE(x.tag_id, x.temple_id) AS anchor_id, x.original_content,
                               f.similarity, f.lex_score, f.rrf_score,
                               ROW_NUMBER() OVER (
                                   PARTITION BY COALESCE(x.tag_id, x.temple_id)
                                   ORDER BY f.rrf_score DESC, f.similarity DESC NULLS LAST,
                                            f.lex_score DESC NULLS LAST, x.source_chunk_index
                               ) AS anchor_rank
                        FROM fused f JOIN eligible x ON x.id = f.doc_id
                    )
                    SELECT document_kind, anchor_id, original_content, similarity, lex_score, rrf_score
                    FROM per_anchor
                    WHERE anchor_rank = 1
                    ORDER BY rrf_score DESC, similarity DESC NULLS LAST, anchor_id
                    LIMIT ?
                    """.trimIndent(),
                )
                args += IntegerColumnType() to limit.coerceIn(1, MAX_LIMIT)
            }
            collect(sql, args, hybridScores = true)
        }
    }

    /** Vector branch only. */
    suspend fun searchSemantic(
        queryVector: List<Float>,
        profileId: UUID,
        tagIds: Collection<UUID>,
        templeIds: Collection<UUID>,
        limit: Int = DEFAULT_LIMIT,
    ): List<MacroCandidate> {
        if (tagIds.isEmpty() && templeIds.isEmpty()) return emptyList()
        return DatabaseFactory.dbQuery {
            val args = listOf<Pair<IColumnType<*>, Any?>>(
                TextColumnType() to toVectorLiteral(queryVector),
                TextColumnType() to "",
                TextColumnType() to uuidArray(tagIds),
                TextColumnType() to uuidArray(templeIds),
                UUIDColumnType() to profileId,
                IntegerColumnType() to limit.coerceIn(1, MAX_LIMIT),
            )
            val sql = PARAMS_AND_ELIGIBLE + """
                ,
                scored AS (
                    SELECT x.document_kind, COALESCE(x.tag_id, x.temple_id) AS anchor_id, x.original_content,
                           1 - (e.embedding <=> params.qvec) AS similarity,
                           ROW_NUMBER() OVER (
                               PARTITION BY COALESCE(x.tag_id, x.temple_id)
                               ORDER BY e.embedding <=> params.qvec ASC, x.source_chunk_index
                           ) AS anchor_rank
                    FROM eligible x
                    CROSS JOIN params
                    JOIN document_embeddings e ON e.document_id = x.id
                    WHERE e.profile_id = ?
                )
                SELECT document_kind, anchor_id, original_content, similarity,
                       CAST(NULL AS double precision) AS lex_score, CAST(NULL AS double precision) AS rrf_score
                FROM scored
                WHERE anchor_rank = 1
                ORDER BY similarity DESC, anchor_id
                LIMIT ?
            """.trimIndent()
            collect(sql, args, hybridScores = false)
        }
    }

    /**
     * The published kriti whose normalised title or incipit equals the normalised query, ignoring
     * spaces (stored values split compounds: `vatapi gana patim`). Narrow on purpose: a partial
     * title never claims the top slot (TRACK-152 Spec 11).
     */
    suspend fun exactTitleMatchId(rawQuery: String): UUID? = DatabaseFactory.dbQuery {
        var id: UUID? = null
        exec(
            """
            WITH q AS (
                SELECT replace(lower(strip_diacritics(btrim(CAST(? AS text)))), ' ', '') AS n
            )
            SELECT k.id FROM krithis k CROSS JOIN q
            WHERE k.workflow_state = 'published' AND q.n <> ''
              AND (replace(k.title_normalized, ' ', '') = q.n OR replace(COALESCE(k.incipit_normalized, ''), ' ', '') = q.n)
            ORDER BY k.id LIMIT 1
            """.trimIndent(),
            listOf(TextColumnType() to rawQuery),
            StatementType.SELECT,
        ) { rs -> if (rs.next()) id = UUID.fromString(rs.getString("id")) }
        id
    }

    private fun JdbcTransaction.collect(
        sql: String,
        args: List<Pair<IColumnType<*>, Any?>>,
        hybridScores: Boolean,
    ): List<MacroCandidate> {
        val out = mutableListOf<MacroCandidate>()
        exec(sql, args, StatementType.SELECT) { rs ->
            while (rs.next()) {
                out += MacroCandidate(
                    kind = if (rs.getString("document_kind") == "CYCLE_OVERVIEW") MacroAnchorKind.COLLECTION else MacroAnchorKind.KSHETRA,
                    anchorId = UUID.fromString(rs.getString("anchor_id")),
                    content = rs.getString("original_content"),
                    similarity = rs.getObject("similarity")?.let { (it as Number).toDouble() },
                    lexicalScore = if (hybridScores) rs.getObject("lex_score")?.let { (it as Number).toDouble() } else null,
                    rrfScore = if (hybridScores) rs.getObject("rrf_score")?.let { (it as Number).toDouble() } else null,
                )
            }
        }
        return out
    }

    private fun toVectorLiteral(v: List<Float>): String = v.joinToString(prefix = "[", postfix = "]", separator = ",")

    private fun uuidArray(ids: Collection<UUID>): String = ids.joinToString(prefix = "{", postfix = "}", separator = ",")

    companion object {
        const val DEFAULT_LIMIT = 10
        private const val MAX_LIMIT = 50

        /**
         * Parameters, then the published macro documents of eligible anchors only. A document whose
         * anchor columns are both null, both set, or mismatch its kind is never selected.
         */
        private val PARAMS_AND_ELIGIBLE = """
            WITH params AS (
                SELECT CAST(? AS text)::vector(768) AS qvec, CAST(? AS text) AS qtext
            ),
            eligible AS MATERIALIZED (
                SELECT d.id, d.document_kind, d.tag_id, d.temple_id, d.source_chunk_index,
                       d.original_content, d.indexed_content
                FROM search_documents d
                WHERE d.is_published
                  AND (
                      (d.document_kind = 'CYCLE_OVERVIEW' AND d.tag_id IS NOT NULL AND d.temple_id IS NULL
                          AND d.tag_id = ANY (CAST(? AS uuid[])))
                      OR
                      (d.document_kind = 'KSHETRA_OVERVIEW' AND d.temple_id IS NOT NULL AND d.tag_id IS NULL
                          AND d.temple_id = ANY (CAST(? AS uuid[])))
                  )
            )
        """.trimIndent()
    }
}
