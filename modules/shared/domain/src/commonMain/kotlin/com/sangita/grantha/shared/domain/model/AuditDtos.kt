package com.sangita.grantha.shared.domain.model

import com.sangita.grantha.shared.domain.serialization.UuidSerializer
import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable
import kotlin.uuid.Uuid

@Serializable
data class AuditLogDto(
    @Serializable(with = UuidSerializer::class)
    val id: Uuid,
    @Serializable(with = UuidSerializer::class)
    val actorUserId: Uuid? = null,
    val actorIp: String? = null,
    val action: String,
    val entityTable: String,
    @Serializable(with = UuidSerializer::class)
    val entityId: Uuid? = null,
    val changedAt: Instant,
    val diff: String? = null,
    val metadata: String? = null,
)
