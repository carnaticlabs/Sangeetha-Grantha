package com.sangita.grantha.backend.api.services

import com.sangita.grantha.backend.dal.SangitaDal
import com.sangita.grantha.backend.dal.errors.DuplicateKeyException
import com.sangita.grantha.backend.dal.repositories.RagaQueueItemDto
import com.sangita.grantha.backend.dal.support.toJavaUuid
import com.sangita.grantha.shared.domain.model.RagaDto
import kotlin.uuid.Uuid
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

@Serializable
data class RagaQueuePage(
    val items: List<RagaQueueItemDto>,
    val total: Long,
    val page: Int,
    val size: Int,
)

@Serializable
data class AttachAliasRequest(
    val ragaId: String,
    val aliasType: String = "transliteration",
    val source: String = "curator",
)

@Serializable
data class ConfirmNewRagaRequest(
    val parentRagaId: String,
    val arohanam: String,
    val avarohanam: String,
    val melakartaNumber: Int? = null,
    val name: String? = null,
)

@Serializable
data class DisambiguateRagaRequest(
    val ragaId: String,
)

@Serializable
data class ScanScaleCollisionsResult(
    val inserted: Int,
)

sealed class RagaQueueActionResult {
    data class Ok(val raga: RagaDto) : RagaQueueActionResult()
    data class NotFound(val message: String) : RagaQueueActionResult()
    data class Invalid(val message: String) : RagaQueueActionResult()
}

/**
 * Curator actions for TRACK-136 Unresolved ragas queue. Audited; role-gated at the route.
 */
class RagaResolutionService(
    private val dal: SangitaDal,
    private val entityResolver: IEntityResolver,
) {
    suspend fun listPending(page: Int, size: Int): RagaQueuePage {
        val total = dal.ragas.countPendingQueue()
        val items = dal.ragas.listPendingQueue(limit = size, offset = page * size)
        return RagaQueuePage(items = items, total = total, page = page, size = size)
    }

    suspend fun attachAlias(
        queueId: Uuid,
        request: AttachAliasRequest,
        actorUserId: Uuid?,
    ): RagaQueueActionResult {
        val item = dal.ragas.findQueueItem(queueId)
            ?: return RagaQueueActionResult.NotFound("Queue item not found")
        if (item.status != "pending") {
            return RagaQueueActionResult.Invalid("Queue item is not pending")
        }
        if (item.kind != "unknown") {
            return RagaQueueActionResult.Invalid("attach-alias applies to unknown names, not ${item.kind}")
        }
        val ragaId = runCatching { Uuid.parse(request.ragaId) }.getOrNull()
            ?: return RagaQueueActionResult.Invalid("Invalid ragaId")
        val raga = dal.ragas.findById(ragaId)
            ?: return RagaQueueActionResult.NotFound("Raga not found")
        return try {
            dal.ragas.insertAlias(
                ragaId = ragaId,
                alias = item.rawName,
                aliasType = request.aliasType,
                source = request.source,
            )
            dal.ragas.resolveQueueItem(queueId, ragaId, status = "attached")
            entityResolver.invalidateCache("raga", ragaId)
            dal.auditLogs.append(
                action = "ATTACH_RAGA_ALIAS",
                entityTable = "raga_resolution_queue",
                entityId = queueId,
                actorUserId = actorUserId,
                metadata = buildJsonObject {
                    put("alias", item.rawName)
                    put("ragaId", ragaId.toString())
                    put("ragaName", raga.name)
                }.toString(),
            )
            RagaQueueActionResult.Ok(raga)
        } catch (e: DuplicateKeyException) {
            RagaQueueActionResult.Invalid("Alias collides with an existing identity (${e.constraint})")
        } catch (e: Exception) {
            val msg = generateSequence<Throwable>(e) { it.cause }.mapNotNull { it.message }.joinToString(" ")
            RagaQueueActionResult.Invalid(msg.ifBlank { "Failed to attach alias" })
        }
    }

    suspend fun confirmNew(
        queueId: Uuid,
        request: ConfirmNewRagaRequest,
        actorUserId: Uuid?,
    ): RagaQueueActionResult {
        val item = dal.ragas.findQueueItem(queueId)
            ?: return RagaQueueActionResult.NotFound("Queue item not found")
        if (item.status != "pending") {
            return RagaQueueActionResult.Invalid("Queue item is not pending")
        }
        if (item.kind != "unknown") {
            return RagaQueueActionResult.Invalid("confirm-new applies to unknown names, not ${item.kind}")
        }
        if (request.arohanam.isBlank() || request.avarohanam.isBlank()) {
            return RagaQueueActionResult.Invalid("arohanam and avarohanam are required")
        }
        val parentId = runCatching { Uuid.parse(request.parentRagaId) }.getOrNull()
            ?: return RagaQueueActionResult.Invalid("Invalid parentRagaId")
        val parent = dal.ragas.findById(parentId)
            ?: return RagaQueueActionResult.NotFound("Parent raga not found")
        val name = request.name?.trim()?.takeIf { it.isNotBlank() } ?: item.rawName
        return try {
            val created = dal.ragas.create(
                name = name,
                parentRagaId = parentId.toJavaUuid(),
                arohanam = request.arohanam.trim(),
                avarohanam = request.avarohanam.trim(),
                melakartaNumber = request.melakartaNumber,
                source = "curator confirm-new",
                confidence = "high",
            )
            dal.ragas.resolveQueueItem(queueId, created.id, status = "created")
            entityResolver.invalidateCache("raga", created.id)
            dal.auditLogs.append(
                action = "CONFIRM_NEW_RAGA",
                entityTable = "ragas",
                entityId = created.id,
                actorUserId = actorUserId,
                metadata = buildJsonObject {
                    put("queueId", queueId.toString())
                    put("parentRagaId", parent.id.toString())
                    put("rawName", item.rawName)
                }.toString(),
            )
            RagaQueueActionResult.Ok(created)
        } catch (e: DuplicateKeyException) {
            RagaQueueActionResult.Invalid("Identity collides with an existing raga (${e.constraint})")
        }
    }

    suspend fun disambiguate(
        queueId: Uuid,
        request: DisambiguateRagaRequest,
        actorUserId: Uuid?,
    ): RagaQueueActionResult {
        val item = dal.ragas.findQueueItem(queueId)
            ?: return RagaQueueActionResult.NotFound("Queue item not found")
        if (item.status != "pending") {
            return RagaQueueActionResult.Invalid("Queue item is not pending")
        }
        if (item.kind != "ambiguous") {
            return RagaQueueActionResult.Invalid("disambiguate applies to ambiguous names, not ${item.kind}")
        }
        val ragaId = runCatching { Uuid.parse(request.ragaId) }.getOrNull()
            ?: return RagaQueueActionResult.Invalid("Invalid ragaId")
        val raga = dal.ragas.findById(ragaId)
            ?: return RagaQueueActionResult.NotFound("Raga not found")
        dal.ragas.resolveQueueItem(queueId, ragaId, status = "disambiguated")
        entityResolver.invalidateCache("raga", ragaId)
        dal.auditLogs.append(
            action = "DISAMBIGUATE_RAGA",
            entityTable = "raga_resolution_queue",
            entityId = queueId,
            actorUserId = actorUserId,
            metadata = buildJsonObject {
                put("ragaId", ragaId.toString())
                put("ragaName", raga.name)
                put("rawName", item.rawName)
            }.toString(),
        )
        return RagaQueueActionResult.Ok(raga)
    }

    suspend fun scanScaleCollisions(actorUserId: Uuid?): ScanScaleCollisionsResult {
        val inserted = dal.ragas.scanScaleCollisionsIntoQueue()
        dal.auditLogs.append(
            action = "SCAN_RAGA_SCALE_COLLISIONS",
            entityTable = "raga_resolution_queue",
            actorUserId = actorUserId,
            metadata = buildJsonObject { put("inserted", inserted) }.toString(),
        )
        return ScanScaleCollisionsResult(inserted = inserted)
    }
}
