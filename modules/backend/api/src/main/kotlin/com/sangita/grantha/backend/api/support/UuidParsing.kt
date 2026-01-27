package com.sangita.grantha.backend.api.support

import java.util.UUID
import kotlin.uuid.Uuid

fun String?.toJavaUuidOrNull(label: String): UUID? {
    if (this.isNullOrBlank()) return null
    return runCatching { UUID.fromString(this) }
        .getOrElse { throw IllegalArgumentException("Invalid $label: must be a valid UUID") }
}

fun String.toJavaUuidOrThrow(label: String): UUID {
    require(this.isNotBlank()) { "$label must not be blank" }
    return runCatching { UUID.fromString(this) }
        .getOrElse { throw IllegalArgumentException("Invalid $label: must be a valid UUID") }
}

fun String?.toKotlinUuidOrNull(label: String): Uuid? {
    if (this.isNullOrBlank()) return null
    return runCatching { Uuid.parse(this) }
        .getOrElse { throw IllegalArgumentException("Invalid $label: must be a valid UUID") }
}

fun String.toKotlinUuidOrThrow(label: String): Uuid {
    require(this.isNotBlank()) { "$label must not be blank" }
    return runCatching { Uuid.parse(this) }
        .getOrElse { throw IllegalArgumentException("Invalid $label: must be a valid UUID") }
}
