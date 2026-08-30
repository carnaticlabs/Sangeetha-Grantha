package com.sangita.grantha.backend.dal.tables

import com.sangita.grantha.backend.dal.support.jsonbText
import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.core.dao.id.java.UUIDTable
import org.jetbrains.exposed.v1.core.java.javaUUID
import org.jetbrains.exposed.v1.javatime.timestampWithTimeZone

/**
 * TRACK-136 / ADR-017: same-identity surface forms of a raga.
 */
object RagaAliasesTable : UUIDTable("raga_aliases") {
    val ragaId = javaUUID("raga_id")
    val alias = text("alias")
    val matchKey = text("match_key")
    val aliasType = text("alias_type")
    val tradition = text("tradition").nullable()
    val sourceInfo = text("source")
    val confidence = text("confidence")
    val createdAt = timestampWithTimeZone("created_at")
    val updatedAt = timestampWithTimeZone("updated_at")
}

/**
 * TRACK-136 / ADR-017: union of raga identity keys and differing alias keys.
 * PK (match_key, mela_disambiguator) is the transactional guardrail.
 */
object RagaIdentityKeysTable : Table("raga_identity_keys") {
    val matchKey = text("match_key")
    val melaDisambiguator = integer("mela_disambiguator")
    val ragaId = javaUUID("raga_id")

    override val primaryKey = PrimaryKey(matchKey, melaDisambiguator)
}

/**
 * TRACK-136 / ADR-017 Phase 2: unknown / ambiguous names held for curator resolution.
 */
object RagaResolutionQueueTable : UUIDTable("raga_resolution_queue") {
    val rawName = text("raw_name")
    val matchKey = text("match_key")
    val kind = text("kind")
    val context = jsonbText("context").nullable()
    val proposedLakshana = jsonbText("proposed_lakshana").nullable()
    val status = text("status")
    val resolvedRagaId = javaUUID("resolved_raga_id").nullable()
    val createdAt = timestampWithTimeZone("created_at")
    val resolvedAt = timestampWithTimeZone("resolved_at").nullable()
}

/**
 * TRACK-136 / ADR-017: distinct-scale nomenclature pairs (from_raga_id < to_raga_id).
 */
object RagaRelationsTable : Table("raga_relations") {
    val fromRagaId = javaUUID("from_raga_id")
    val toRagaId = javaUUID("to_raga_id")
    val relation = text("relation")
    val sourceInfo = text("source")

    override val primaryKey = PrimaryKey(fromRagaId, toRagaId, relation)
}
