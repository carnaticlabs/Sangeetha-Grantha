package com.sangita.grantha.backend.dal.models

import java.time.OffsetDateTime
import kotlin.time.Instant
import kotlin.time.toKotlinInstant
import org.jetbrains.exposed.v1.core.Column
import org.jetbrains.exposed.v1.core.ResultRow

fun ResultRow.kotlinInstant(column: Column<OffsetDateTime>): Instant {
    return this[column].toInstant().toKotlinInstant()
}

fun ResultRow.kotlinInstantOrNull(column: Column<OffsetDateTime?>): Instant? {
    return this[column]?.toInstant()?.toKotlinInstant()
}
