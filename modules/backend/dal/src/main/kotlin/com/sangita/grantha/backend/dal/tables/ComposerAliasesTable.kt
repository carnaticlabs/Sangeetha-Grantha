package com.sangita.grantha.backend.dal.tables

import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.core.java.javaUUID
import org.jetbrains.exposed.v1.javatime.timestampWithTimeZone

/**
 * TRACK-031: Maps alias_normalized (e.g. "dikshitar") to canonical composer_id.
 */
object ComposerAliasesTable : Table("composer_aliases") {
    val aliasNormalized = text("alias_normalized")
    val composerId = javaUUID("composer_id")
    val createdAt = timestampWithTimeZone("created_at")

    override val primaryKey = PrimaryKey(aliasNormalized)
}
