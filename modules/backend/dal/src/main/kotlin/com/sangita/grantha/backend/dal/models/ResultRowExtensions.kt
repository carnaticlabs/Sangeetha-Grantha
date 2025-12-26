package com.sangita.grantha.backend.dal.models

import java.time.Instant
import java.time.OffsetDateTime
import kotlinx.datetime.Instant as KotlinInstant
import org.jetbrains.exposed.v1.core.Column
import org.jetbrains.exposed.v1.core.ResultRow

fun ResultRow.instant(column: Column<OffsetDateTime>): Instant = this[column].toInstant()

fun ResultRow.instantOrNull(column: Column<OffsetDateTime?>): Instant? = this[column]?.toInstant()

fun ResultRow.kotlinInstant(column: Column<OffsetDateTime>): KotlinInstant {
    val javaInstant = this[column].toInstant()
    return KotlinInstant.fromEpochSeconds(javaInstant.epochSecond, javaInstant.nano)
}

fun ResultRow.kotlinInstantOrNull(column: Column<OffsetDateTime?>): KotlinInstant? {
    val javaInstant = this[column]?.toInstant() ?: return null
    return KotlinInstant.fromEpochSeconds(javaInstant.epochSecond, javaInstant.nano)
}
