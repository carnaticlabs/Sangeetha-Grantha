package com.sangita.grantha.backend.dal.repositories

import com.sangita.grantha.backend.dal.enums.WorkflowState
import org.jetbrains.exposed.v1.core.statements.StatementType
import org.jetbrains.exposed.v1.jdbc.transactions.TransactionManager
import java.util.UUID

/** Per-temple eligibility facts, computed in one query by [TempleEligibility.snapshot]. */
data class TempleEligibilityRow(
    val id: UUID,
    val parentId: UUID?,
    /** Published kritis recorded directly at this temple. */
    val publishedKrithiCount: Long,
    val hasPublishedOverview: Boolean,
    /** Eligible child temples. */
    val eligibleChildCount: Int,
    /** Some eligible child has a published kriti of its own. */
    val childHasPublishedKrithi: Boolean,
    val eligible: Boolean,
) {
    /** Direct eligibility: a published kriti or a published KSHETRA_OVERVIEW at this very temple. */
    val direct: Boolean get() = publishedKrithiCount > 0 || hasPublishedOverview

    /** Derived gap (TRACK-151 Spec 6): an overview but no published kriti here or in any eligible child. */
    val recordedGap: Boolean
        get() = eligible && hasPublishedOverview && publishedKrithiCount == 0L && !childHasPublishedKrithi
}

/**
 * The single place that decides which temples the public catalogue may show (TRACK-151 Spec A).
 * List, detail, and the later search (TRACK-152) and facet (TRACK-153) code call this and never
 * restate the rule.
 *
 * - Direct: a published kriti has `temple_id = temple.id`, or a published `KSHETRA_OVERVIEW`
 *   search document carries `temple_id = temple.id`.
 * - Eligible: direct, or at least one child (`parent_temple_id = temple.id`) is direct, so a nested
 *   child stays reachable when its parent has no data of its own.
 *
 * A non-null `krithis.temple_id` is trusted as reviewed because only published kritis count and
 * publishing is a curator step (imports create DRAFT rows).
 */
object TempleEligibility {
    private val published = "'${WorkflowState.PUBLISHED.dbValue}'"

    private val SNAPSHOT_SQL = """
        WITH k AS (
            SELECT temple_id, count(*) AS n FROM krithis
            WHERE temple_id IS NOT NULL AND workflow_state = $published GROUP BY temple_id
        ), o AS (
            SELECT DISTINCT temple_id FROM search_documents
            WHERE document_kind = 'KSHETRA_OVERVIEW' AND is_published AND temple_id IS NOT NULL
        ), d AS (
            SELECT t.id, t.parent_temple_id, COALESCE(k.n, 0) AS pub_k, (o.temple_id IS NOT NULL) AS has_ov
            FROM temples t LEFT JOIN k ON k.temple_id = t.id LEFT JOIN o ON o.temple_id = t.id
        ), e AS (
            SELECT d.*, (d.pub_k > 0 OR d.has_ov) AS direct,
                ((d.pub_k > 0 OR d.has_ov) OR EXISTS (
                    SELECT 1 FROM d c WHERE c.parent_temple_id = d.id AND (c.pub_k > 0 OR c.has_ov)
                )) AS eligible
            FROM d
        )
        SELECT e.id, e.parent_temple_id, e.pub_k, e.has_ov, e.eligible,
            (SELECT count(*) FROM e c WHERE c.parent_temple_id = e.id AND c.eligible) AS eligible_children,
            EXISTS (SELECT 1 FROM e c WHERE c.parent_temple_id = e.id AND c.eligible AND c.pub_k > 0) AS child_pub
        FROM e
    """.trimIndent()

    /** Facts for every temple, eligible or not. Must run inside `DatabaseFactory.dbQuery`. */
    fun snapshot(): Map<UUID, TempleEligibilityRow> {
        val rows = linkedMapOf<UUID, TempleEligibilityRow>()
        TransactionManager.current().exec(SNAPSHOT_SQL, emptyList(), StatementType.SELECT) { rs ->
            while (rs.next()) {
                val id = UUID.fromString(rs.getString("id"))
                rows[id] = TempleEligibilityRow(
                    id = id,
                    parentId = rs.getString("parent_temple_id")?.let(UUID::fromString),
                    publishedKrithiCount = rs.getLong("pub_k"),
                    hasPublishedOverview = rs.getBoolean("has_ov"),
                    eligibleChildCount = rs.getInt("eligible_children"),
                    childHasPublishedKrithi = rs.getBoolean("child_pub"),
                    eligible = rs.getBoolean("eligible"),
                )
            }
        }
        return rows
    }

    /** Ids of eligible temples. Must run inside `DatabaseFactory.dbQuery`. */
    fun eligibleIds(): Set<UUID> = snapshot().values.filter { it.eligible }.mapTo(linkedSetOf()) { it.id }
}
