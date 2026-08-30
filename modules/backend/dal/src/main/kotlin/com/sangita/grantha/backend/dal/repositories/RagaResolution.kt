package com.sangita.grantha.backend.dal.repositories

import kotlin.uuid.Uuid
import kotlinx.serialization.Serializable

/**
 * One occurrence of an unresolved raga name, held in [raga_resolution_queue.context].
 * After the krithi exists, [krithiId] + [orderIndex] are enough to insert `krithi_ragas` (D4).
 */
@Serializable
data class RagaQueueOccurrence(
    val krithiId: String? = null,
    val title: String? = null,
    val orderIndex: Int? = null,
    val isPrimary: Boolean = false,
    val sourceUrl: String? = null,
    val extractionRun: String? = null,
)

data class RagaResolveContext(
    val krithiId: String? = null,
    val title: String? = null,
    val orderIndex: Int? = null,
    val isPrimary: Boolean = false,
    val sourceUrl: String? = null,
    val extractionRun: String? = null,
) {
    fun toOccurrence(): RagaQueueOccurrence = RagaQueueOccurrence(
        krithiId = krithiId,
        title = title,
        orderIndex = orderIndex,
        isPrimary = isPrimary,
        sourceUrl = sourceUrl,
        extractionRun = extractionRun,
    )
}

sealed class RagaResolution {
    data class Resolved(val raga: com.sangita.grantha.shared.domain.model.RagaDto) : RagaResolution()
    data class Unresolved(
        val queueId: Uuid,
        val kind: String,
        val matchKey: String,
    ) : RagaResolution()
}

data class RagaIdentityHit(
    val raga: com.sangita.grantha.shared.domain.model.RagaDto,
    val melaDisambiguator: Int,
)

@Serializable
data class RagaQueueItemDto(
    val id: String,
    val rawName: String,
    val matchKey: String,
    val kind: String,
    val context: String?,
    val proposedLakshana: String?,
    val status: String,
    val resolvedRagaId: String?,
    val createdAt: String,
    val resolvedAt: String?,
)

@Serializable
data class RagaScaleCollisionGroup(
    val signature: String,
    val names: List<String>,
    val ragaIds: List<String>,
)
