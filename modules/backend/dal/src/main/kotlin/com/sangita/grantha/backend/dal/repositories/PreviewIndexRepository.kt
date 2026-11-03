package com.sangita.grantha.backend.dal.repositories

import com.sangita.grantha.backend.dal.DatabaseFactory
import java.time.OffsetDateTime
import java.util.UUID
import com.sangita.grantha.backend.dal.enums.WorkflowState
import com.sangita.grantha.backend.dal.tables.KrithisTable
import org.jetbrains.exposed.v1.core.IColumnType
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.inList
import org.jetbrains.exposed.v1.core.neq
import org.jetbrains.exposed.v1.jdbc.select
import org.jetbrains.exposed.v1.core.java.UUIDColumnType
import org.jetbrains.exposed.v1.core.statements.StatementType

/** What the database says about one krithi's search documents and the source rows they were built from. */
data class PreviewIndexFacts(
    val workflowState: String,
    val documentCount: Int,
    val publishedDocumentCount: Int,
    val embeddedDocumentCount: Int,
    val indexedAt: OffsetDateTime?,
    val sourceUpdatedAt: OffsetDateTime?,
)

/**
 * Read-only index freshness facts for the curator preview (TRACK-158). Timestamps only: the content hash is
 * computed by the Python context formatter and is deliberately not re-implemented here.
 */
class PreviewIndexRepository {
    /** [profileId] null (nothing indexed yet) is matched with the nil UUID, which no embedding row carries. */
    suspend fun facts(krithiId: UUID, profileId: UUID?): PreviewIndexFacts? = DatabaseFactory.dbQuery {
        var result: PreviewIndexFacts? = null
        val args: List<Pair<IColumnType<*>, Any?>> = listOf(
            UUIDColumnType() to krithiId, // documents
            UUIDColumnType() to krithiId, // published documents
            UUIDColumnType() to krithiId, // indexed_at
            UUIDColumnType() to krithiId, // embedded documents: krithi
            UUIDColumnType() to (profileId ?: UUID(0L, 0L)), // embedded documents: profile
            UUIDColumnType() to krithiId, // the krithi row
        )
        exec(
            """
            SELECT k.workflow_state::text AS workflow_state,
                   (SELECT count(*) FROM search_documents d WHERE d.krithi_id = ?) AS docs,
                   (SELECT count(*) FROM search_documents d WHERE d.krithi_id = ? AND d.is_published) AS published_docs,
                   (SELECT max(d.updated_at) FROM search_documents d WHERE d.krithi_id = ?) AS indexed_at,
                   GREATEST(
                       k.updated_at,
                       COALESCE((SELECT max(s.updated_at) FROM krithi_sections s WHERE s.krithi_id = k.id), k.updated_at),
                       COALESCE((SELECT max(v.updated_at) FROM krithi_lyric_variants v WHERE v.krithi_id = k.id), k.updated_at),
                       COALESCE((SELECT max(ls.updated_at) FROM krithi_lyric_sections ls
                                 JOIN krithi_lyric_variants v ON v.id = ls.lyric_variant_id WHERE v.krithi_id = k.id), k.updated_at)
                   ) AS source_updated_at,
                   (SELECT count(*) FROM document_embeddings e JOIN search_documents d ON d.id = e.document_id
                     WHERE d.krithi_id = ? AND e.profile_id = ?) AS embedded_docs
            FROM krithis k WHERE k.id = ?
            """.trimIndent(),
            args,
            StatementType.SELECT,
        ) { rs ->
            if (rs.next()) {
                result = PreviewIndexFacts(
                    workflowState = rs.getString("workflow_state"),
                    documentCount = rs.getInt("docs"),
                    publishedDocumentCount = rs.getInt("published_docs"),
                    embeddedDocumentCount = rs.getInt("embedded_docs"),
                    indexedAt = rs.getObject("indexed_at", OffsetDateTime::class.java),
                    sourceUpdatedAt = rs.getObject("source_updated_at", OffsetDateTime::class.java),
                )
            }
        }
        result
    }

    /** Which of [ids] are krithis a mobile user can never see: any workflow state other than published. */
    suspend fun unpublishedAmong(ids: Collection<UUID>): Set<UUID> {
        if (ids.isEmpty()) return emptySet()
        return DatabaseFactory.dbQuery {
            KrithisTable
                .select(KrithisTable.id)
                .where { (KrithisTable.id inList ids.toList()) and (KrithisTable.workflowState neq WorkflowState.PUBLISHED) }
                .map { it[KrithisTable.id].value }
                .toSet()
        }
    }
}
