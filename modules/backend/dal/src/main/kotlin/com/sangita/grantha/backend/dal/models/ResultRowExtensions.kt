package com.sangita.grantha.backend.dal.models

import java.time.OffsetDateTime
import kotlinx.datetime.Instant
import org.jetbrains.exposed.v1.core.Column
import org.jetbrains.exposed.v1.core.ResultRow

fun ResultRow.kotlinInstant(column: Column<OffsetDateTime>): Instant {
    val javaInstant = this[column].toInstant()
    return Instant.fromEpochSeconds(javaInstant.epochSecond, javaInstant.nano)
}

fun ResultRow.kotlinInstantOrNull(column: Column<OffsetDateTime?>): Instant? {
    val javaInstant = this[column]?.toInstant() ?: return null
    return Instant.fromEpochSeconds(javaInstant.epochSecond, javaInstant.nano)
}
