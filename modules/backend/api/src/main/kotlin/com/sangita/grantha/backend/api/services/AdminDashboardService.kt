package com.sangita.grantha.backend.api.services

import com.sangita.grantha.backend.dal.SangitaDal
import com.sangita.grantha.backend.dal.enums.WorkflowState
import kotlinx.serialization.Serializable

@Serializable
data class DashboardStatsDto(
    val totalKrithis: Long,
    val totalComposers: Long,
    val totalRagas: Long,
    val pendingReview: Long
)

/**
 * Service that aggregates high-level admin dashboard statistics.
 */
class AdminDashboardService(private val dal: SangitaDal) {
    /**
     * Compute dashboard totals for krithis, composers, ragas, and pending review.
     */
    suspend fun getStats(): DashboardStatsDto {
        val krithis = dal.krithis.countAll()
        val composers = dal.composers.countAll()
        val ragas = dal.ragas.countAll()
        // Count both DRAFT and IN_REVIEW as pending review
        val draftCount = dal.krithis.countByState(WorkflowState.DRAFT)
        val inReviewCount = dal.krithis.countByState(WorkflowState.IN_REVIEW)
        val pending = draftCount + inReviewCount

        return DashboardStatsDto(
            totalKrithis = krithis,
            totalComposers = composers,
            totalRagas = ragas,
            pendingReview = pending
        )
    }
}
