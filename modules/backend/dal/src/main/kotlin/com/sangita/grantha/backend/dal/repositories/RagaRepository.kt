package com.sangita.grantha.backend.dal.repositories

import com.sangita.grantha.backend.dal.DatabaseFactory
import com.sangita.grantha.backend.dal.errors.DuplicateKeyException
import com.sangita.grantha.backend.dal.models.toRagaDto
import com.sangita.grantha.backend.dal.support.toJavaUuid
import com.sangita.grantha.backend.dal.support.toKotlinUuid
import com.sangita.grantha.backend.dal.tables.KrithiRagasTable
import com.sangita.grantha.backend.dal.tables.KrithisTable
import com.sangita.grantha.backend.dal.tables.RagaAliasesTable
import com.sangita.grantha.backend.dal.tables.RagaIdentityKeysTable
import com.sangita.grantha.backend.dal.tables.RagaRelationsTable
import com.sangita.grantha.backend.dal.tables.RagaResolutionQueueTable
import com.sangita.grantha.backend.dal.tables.RagasTable
import com.sangita.grantha.shared.domain.model.RagaDto
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.util.UUID
import kotlin.uuid.Uuid
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.put
import org.jetbrains.exposed.v1.core.*
import org.jetbrains.exposed.v1.jdbc.*
import org.slf4j.LoggerFactory

/**
 * Repository for raga reference data and TRACK-136 identity resolution.
 *
 * Ingestion uses [resolveRaga] — there is no silent mint. Curated creates go through [create].
 */
class RagaRepository {
    private val logger = LoggerFactory.getLogger(javaClass)
    private val json = Json { ignoreUnknownKeys = true }

    private fun normalize(value: String): String =
        value.trim()
            .lowercase()
            .replace(Regex("\\s+"), " ")

    private fun matchKeyExpr(name: String) = CustomFunction<String>(
        "raga_match_key",
        TextColumnType(),
        stringLiteral(name),
    )

    suspend fun computeMatchKey(name: String): String = DatabaseFactory.dbQuery {
        exec("SELECT raga_match_key('${name.replace("'", "''")}')") { rs ->
            if (rs.next()) rs.getString(1) else error("raga_match_key returned no row")
        } ?: error("raga_match_key returned null")
    }

    suspend fun listAll(): List<RagaDto> = DatabaseFactory.dbQuery {
        RagasTable
            .selectAll()
            .orderBy(RagasTable.nameNormalized to SortOrder.ASC)
            .map { row -> row.toRagaDto() }
    }

    suspend fun findById(id: Uuid): RagaDto? = DatabaseFactory.dbQuery {
        RagasTable
            .selectAll()
            .where { RagasTable.id eq id.toJavaUuid() }
            .map { it.toRagaDto() }
            .singleOrNull()
    }

    suspend fun findByName(name: String): RagaDto? = DatabaseFactory.dbQuery {
        RagasTable
            .selectAll()
            .where { RagasTable.name eq name }
            .map { it.toRagaDto() }
            .singleOrNull()
    }

    suspend fun findByNameNormalized(nameNormalized: String): RagaDto? = DatabaseFactory.dbQuery {
        RagasTable
            .selectAll()
            .where { RagasTable.nameNormalized eq nameNormalized }
            .map { it.toRagaDto() }
            .singleOrNull()
    }

    /**
     * Identity hits for [name] via `raga_identity_keys` + `raga_match_key`. Distinct ragas.
     */
    suspend fun lookupIdentityHits(name: String): List<RagaIdentityHit> {
        if (name.isBlank()) return emptyList()
        return DatabaseFactory.dbQuery {
            val keyExpr = matchKeyExpr(name)
            RagaIdentityKeysTable
                .innerJoin(RagasTable, { RagaIdentityKeysTable.ragaId }, { RagasTable.id })
                .selectAll()
                .where { RagaIdentityKeysTable.matchKey eq keyExpr }
                .map { row ->
                    RagaIdentityHit(
                        raga = row.toRagaDto(),
                        melaDisambiguator = row[RagaIdentityKeysTable.melaDisambiguator],
                    )
                }
                .distinctBy { it.raga.id }
        }
    }

    /**
     * Single resolution entry point (TRACK-136 §2.1). Never inserts a `ragas` row.
     *
     * - 1 identity hit → Resolved
     * - >1 → enqueue `ambiguous`
     * - 0 → enqueue `unknown`
     *
     * When [mela] is supplied, a singleton *among that mela* still resolves (homonyms at other
     * melas are ignored). Remaining multi-hits at that mela stay ambiguous.
     */
    suspend fun resolveRaga(
        name: String,
        mela: Int? = null,
        context: RagaResolveContext? = null,
    ): RagaResolution {
        val trimmed = name.trim()
        if (trimmed.isBlank()) {
            val key = computeMatchKey(name)
            val queueId = enqueueUnresolved(trimmed.ifBlank { name }, key, "unknown", context, candidateIds = null)
            return RagaResolution.Unresolved(queueId, "unknown", key)
        }

        val hits = lookupIdentityHits(trimmed)
        val filtered = if (mela != null) hits.filter { it.melaDisambiguator == mela } else hits
        val key = computeMatchKey(trimmed)

        return when {
            filtered.size == 1 -> RagaResolution.Resolved(filtered.single().raga)
            filtered.size > 1 -> {
                val queueId = enqueueUnresolved(
                    rawName = trimmed,
                    matchKey = key,
                    kind = "ambiguous",
                    context = context,
                    candidateIds = filtered.map { it.raga.id.toString() },
                )
                RagaResolution.Unresolved(queueId, "ambiguous", key)
            }
            hits.isNotEmpty() -> {
                // Name known at a different mela than the one supplied — still a homonym set.
                val queueId = enqueueUnresolved(
                    rawName = trimmed,
                    matchKey = key,
                    kind = "ambiguous",
                    context = context,
                    candidateIds = hits.map { it.raga.id.toString() },
                )
                RagaResolution.Unresolved(queueId, "ambiguous", key)
            }
            else -> {
                val queueId = enqueueUnresolved(trimmed, key, "unknown", context, candidateIds = null)
                RagaResolution.Unresolved(queueId, "unknown", key)
            }
        }
    }

    /**
     * Compatibility wrapper: singleton identity hit. Unresolved names do **not** mint;
     * they enqueue via [resolveRaga] and this throws so existing call sites cannot ignore a miss.
     * Ingestion callers should use [resolveRaga] to hold D4 slots.
     */
    suspend fun findOrCreate(
        name: String,
        nameNormalized: String? = null,
        melakartaNumber: Int? = null,
        parentRagaId: UUID? = null,
        arohanam: String? = null,
        avarohanam: String? = null,
        notes: String? = null,
    ): RagaDto {
        @Suppress("UNUSED_VARIABLE")
        val ignored = listOf(nameNormalized, parentRagaId, arohanam, avarohanam, notes)
        return when (val r = resolveRaga(name, mela = melakartaNumber)) {
            is RagaResolution.Resolved -> r.raga
            is RagaResolution.Unresolved ->
                error("Raga '$name' is ${r.kind} (queue ${r.queueId}); silent mint is forbidden")
        }
    }

    suspend fun enqueueUnresolved(
        rawName: String,
        matchKey: String,
        kind: String,
        context: RagaResolveContext?,
        candidateIds: List<String>?,
    ): Uuid = DatabaseFactory.dbQuery {
        enqueueUnresolvedInTxn(rawName, matchKey, kind, context, candidateIds)
    }

    private fun enqueueUnresolvedInTxn(
        rawName: String,
        matchKey: String,
        kind: String,
        context: RagaResolveContext?,
        candidateIds: List<String>?,
    ): Uuid {
        val occurrence = context?.toOccurrence()
        val occurrenceJson = occurrence?.let { json.encodeToString(RagaQueueOccurrence.serializer(), it) }
        val proposed = candidateIds?.let { ids ->
            buildJsonObject {
                put("candidateRagaIds", JsonArray(ids.map { JsonPrimitive(it) }))
            }.toString()
        }
        val now = OffsetDateTime.now(ZoneOffset.UTC)

        val existing = RagaResolutionQueueTable
            .selectAll()
            .where {
                (RagaResolutionQueueTable.matchKey eq matchKey) and
                    (RagaResolutionQueueTable.kind eq kind) and
                    (RagaResolutionQueueTable.status eq "pending")
            }
            .singleOrNull()

        if (existing != null) {
            if (occurrenceJson != null) {
                val merged = appendJsonArray(existing[RagaResolutionQueueTable.context], occurrenceJson)
                RagaResolutionQueueTable.update({
                    RagaResolutionQueueTable.id eq existing[RagaResolutionQueueTable.id]
                }) {
                    it[RagaResolutionQueueTable.context] = merged
                }
            }
            if (proposed != null && existing[RagaResolutionQueueTable.proposedLakshana] == null) {
                RagaResolutionQueueTable.update({
                    RagaResolutionQueueTable.id eq existing[RagaResolutionQueueTable.id]
                }) {
                    it[RagaResolutionQueueTable.proposedLakshana] = proposed
                }
            }
            return existing[RagaResolutionQueueTable.id].value.toKotlinUuid()
        }

        return try {
            val inserted = RagaResolutionQueueTable.insert {
                it[RagaResolutionQueueTable.rawName] = rawName
                it[RagaResolutionQueueTable.matchKey] = matchKey
                it[RagaResolutionQueueTable.kind] = kind
                it[RagaResolutionQueueTable.context] =
                    occurrenceJson?.let { o -> "[$o]" }
                it[RagaResolutionQueueTable.proposedLakshana] = proposed
                it[RagaResolutionQueueTable.status] = "pending"
                it[RagaResolutionQueueTable.createdAt] = now
            }.resultedValues?.single() ?: error("Failed to insert raga_resolution_queue")
            inserted[RagaResolutionQueueTable.id].value.toKotlinUuid()
        } catch (e: DuplicateKeyException) {
            val retry = RagaResolutionQueueTable
                .selectAll()
                .where {
                    (RagaResolutionQueueTable.matchKey eq matchKey) and
                        (RagaResolutionQueueTable.kind eq kind) and
                        (RagaResolutionQueueTable.status eq "pending")
                }
                .single()
            if (occurrenceJson != null) {
                val merged = appendJsonArray(retry[RagaResolutionQueueTable.context], occurrenceJson)
                RagaResolutionQueueTable.update({
                    RagaResolutionQueueTable.id eq retry[RagaResolutionQueueTable.id]
                }) {
                    it[RagaResolutionQueueTable.context] = merged
                }
            }
            retry[RagaResolutionQueueTable.id].value.toKotlinUuid()
        }
    }

    suspend fun appendQueueContext(queueId: Uuid, context: RagaResolveContext): Unit =
        DatabaseFactory.dbQuery {
            val row = RagaResolutionQueueTable
                .selectAll()
                .where { RagaResolutionQueueTable.id eq queueId.toJavaUuid() }
                .singleOrNull()
                ?: return@dbQuery
            val occurrenceJson = json.encodeToString(RagaQueueOccurrence.serializer(), context.toOccurrence())
            val merged = appendJsonArray(row[RagaResolutionQueueTable.context], occurrenceJson)
            RagaResolutionQueueTable.update({ RagaResolutionQueueTable.id eq queueId.toJavaUuid() }) {
                it[RagaResolutionQueueTable.context] = merged
            }
        }

    suspend fun countPendingQueue(): Long = DatabaseFactory.dbQuery {
        RagaResolutionQueueTable
            .selectAll()
            .where { RagaResolutionQueueTable.status eq "pending" }
            .count()
    }

    suspend fun listPendingQueue(limit: Int = 50, offset: Int = 0): List<RagaQueueItemDto> =
        DatabaseFactory.dbQuery {
            RagaResolutionQueueTable
                .selectAll()
                .where { RagaResolutionQueueTable.status eq "pending" }
                .orderBy(RagaResolutionQueueTable.createdAt to SortOrder.ASC)
                .limit(limit)
                .offset(offset.toLong())
                .map { it.toQueueItem() }
        }

    suspend fun findQueueItem(id: Uuid): RagaQueueItemDto? = DatabaseFactory.dbQuery {
        RagaResolutionQueueTable
            .selectAll()
            .where { RagaResolutionQueueTable.id eq id.toJavaUuid() }
            .map { it.toQueueItem() }
            .singleOrNull()
    }

    suspend fun insertAlias(
        ragaId: Uuid,
        alias: String,
        aliasType: String,
        source: String,
        confidence: String = "high",
        tradition: String? = null,
    ): Unit = DatabaseFactory.dbQuery {
        val now = OffsetDateTime.now(ZoneOffset.UTC)
        RagaAliasesTable.insert {
            it[RagaAliasesTable.ragaId] = ragaId.toJavaUuid()
            it[RagaAliasesTable.alias] = alias
            it[RagaAliasesTable.aliasType] = aliasType
            it[RagaAliasesTable.tradition] = tradition
            it[RagaAliasesTable.sourceInfo] = source
            it[RagaAliasesTable.confidence] = confidence
            it[RagaAliasesTable.createdAt] = now
            it[RagaAliasesTable.updatedAt] = now
        }
    }

    /**
     * Apply held `krithi_ragas` / `primary_raga_id` from queue context (D4), then mark resolved.
     */
    suspend fun resolveQueueItem(
        queueId: Uuid,
        ragaId: Uuid,
        status: String,
    ): Unit = DatabaseFactory.dbQuery {
        val row = RagaResolutionQueueTable
            .selectAll()
            .where { RagaResolutionQueueTable.id eq queueId.toJavaUuid() }
            .singleOrNull()
            ?: error("Queue item $queueId not found")
        applyHeldLinksInTxn(row[RagaResolutionQueueTable.context], ragaId)
        val now = OffsetDateTime.now(ZoneOffset.UTC)
        RagaResolutionQueueTable.update({ RagaResolutionQueueTable.id eq queueId.toJavaUuid() }) {
            it[RagaResolutionQueueTable.status] = status
            it[RagaResolutionQueueTable.resolvedRagaId] = ragaId.toJavaUuid()
            it[RagaResolutionQueueTable.resolvedAt] = now
        }
    }

    suspend fun applyHeldLinks(contextJson: String?, ragaId: Uuid): Unit = DatabaseFactory.dbQuery {
        applyHeldLinksInTxn(contextJson, ragaId)
    }

    private fun applyHeldLinksInTxn(contextJson: String?, ragaId: Uuid) {
        val occurrences = parseOccurrences(contextJson)
        val javaRaga = ragaId.toJavaUuid()
        for (occ in occurrences) {
            val krithiId = occ.krithiId ?: continue
            val order = occ.orderIndex ?: continue
            val krithiUuid = UUID.fromString(krithiId)
            val already = KrithiRagasTable
                .selectAll()
                .where {
                    (KrithiRagasTable.krithiId eq krithiUuid) and
                        (KrithiRagasTable.ragaId eq javaRaga) and
                        (KrithiRagasTable.orderIndex eq order)
                }
                .count()
            if (already == 0L) {
                KrithiRagasTable.insert {
                    it[KrithiRagasTable.krithiId] = krithiUuid
                    it[KrithiRagasTable.ragaId] = javaRaga
                    it[KrithiRagasTable.orderIndex] = order
                }
            }
            if (occ.isPrimary || order == 0) {
                KrithisTable.update({
                    (KrithisTable.id eq krithiUuid) and KrithisTable.primaryRagaId.isNull()
                }) {
                    it[KrithisTable.primaryRagaId] = javaRaga
                }
            }
        }
    }

    /**
     * Scale-collision groups (identical normalised swara-set). Never auto-merged.
     * Skips pairs already linked in [raga_relations].
     */
    suspend fun listScaleCollisions(): List<RagaScaleCollisionGroup> = DatabaseFactory.dbQuery {
        exec(
            """
            SELECT raga_swara_signature(arohanam, avarohanam) AS sig,
                   array_agg(name ORDER BY name) AS names,
                   array_agg(id::text ORDER BY name) AS ids
              FROM ragas
             WHERE arohanam IS NOT NULL
               AND avarohanam IS NOT NULL
               AND raga_swara_signature(arohanam, avarohanam) <> ''
             GROUP BY 1
            HAVING count(*) > 1
            """.trimIndent(),
        ) { rs ->
            buildList {
                while (rs.next()) {
                    val names = (rs.getArray("names").array as Array<*>).map { it.toString() }
                    val ids = (rs.getArray("ids").array as Array<*>).map { it.toString() }
                    add(
                        RagaScaleCollisionGroup(
                            signature = rs.getString("sig"),
                            names = names,
                            ragaIds = ids,
                        ),
                    )
                }
            }
        } ?: emptyList()
    }

    /**
     * Enqueue each scale-collision group as a pending `ambiguous` row (synthetic match_key).
     * Does not auto-merge. Returns how many new pending rows were inserted.
     */
    suspend fun scanScaleCollisionsIntoQueue(): Int {
        val groups = listScaleCollisions()
        var inserted = 0
        for (group in groups) {
            if (group.ragaIds.size < 2) continue
            val related = DatabaseFactory.dbQuery {
                val ids = group.ragaIds.map { UUID.fromString(it) }
                RagaRelationsTable
                    .selectAll()
                    .where {
                        (RagaRelationsTable.fromRagaId inList ids) and
                            (RagaRelationsTable.toRagaId inList ids)
                    }
                    .count()
            }
            if (related > 0L && group.ragaIds.size == 2) continue
            val key = "scale:${group.signature}"
            val before = findPendingByKey(key, "ambiguous")
            enqueueUnresolved(
                rawName = group.names.joinToString(" / "),
                matchKey = key,
                kind = "ambiguous",
                context = null,
                candidateIds = group.ragaIds,
            )
            val after = findPendingByKey(key, "ambiguous")
            if (before == null && after != null) inserted++
        }
        return inserted
    }

    private suspend fun findPendingByKey(matchKey: String, kind: String): Uuid? =
        DatabaseFactory.dbQuery {
            RagaResolutionQueueTable
                .selectAll()
                .where {
                    (RagaResolutionQueueTable.matchKey eq matchKey) and
                        (RagaResolutionQueueTable.kind eq kind) and
                        (RagaResolutionQueueTable.status eq "pending")
                }
                .map { it[RagaResolutionQueueTable.id].value.toKotlinUuid() }
                .singleOrNull()
        }

    suspend fun create(
        name: String,
        nameNormalized: String? = null,
        melakartaNumber: Int? = null,
        parentRagaId: UUID? = null,
        arohanam: String? = null,
        avarohanam: String? = null,
        notes: String? = null,
        source: String = "curator",
        confidence: String = "high",
    ): RagaDto = DatabaseFactory.dbQuery {
        val now = OffsetDateTime.now(ZoneOffset.UTC)
        val ragaId = UUID.randomUUID()
        val normalized = nameNormalized ?: normalize(name)

        RagasTable.insert {
            it[id] = ragaId
            it[RagasTable.name] = name
            it[RagasTable.nameNormalized] = normalized
            it[RagasTable.melakartaNumber] = melakartaNumber
            it[RagasTable.parentRagaId] = parentRagaId
            it[RagasTable.arohanam] = arohanam
            it[RagasTable.avarohanam] = avarohanam
            it[RagasTable.notes] = notes
            it[RagasTable.sourceInfo] = source
            it[RagasTable.confidence] = confidence
            it[RagasTable.createdAt] = now
            it[RagasTable.updatedAt] = now
        }
            .resultedValues
            ?.single()
            ?.toRagaDto()
            ?: error("Failed to insert raga")
    }

    suspend fun update(
        id: Uuid,
        name: String? = null,
        nameNormalized: String? = null,
        melakartaNumber: Int? = null,
        parentRagaId: UUID? = null,
        arohanam: String? = null,
        avarohanam: String? = null,
        notes: String? = null,
    ): RagaDto? = DatabaseFactory.dbQuery {
        val now = OffsetDateTime.now(ZoneOffset.UTC)
        val javaId = id.toJavaUuid()

        RagasTable
            .updateReturning(
                where = { RagasTable.id eq javaId },
            ) {
                name?.let { value ->
                    it[RagasTable.name] = value
                    it[RagasTable.nameNormalized] = nameNormalized ?: normalize(value)
                }
                nameNormalized?.let { value -> it[RagasTable.nameNormalized] = value }
                melakartaNumber?.let { value -> it[RagasTable.melakartaNumber] = value }
                parentRagaId?.let { value -> it[RagasTable.parentRagaId] = value }
                arohanam?.let { value -> it[RagasTable.arohanam] = value }
                avarohanam?.let { value -> it[RagasTable.avarohanam] = value }
                notes?.let { value -> it[RagasTable.notes] = value }
                it[RagasTable.updatedAt] = now
            }
            .singleOrNull()
            ?.toRagaDto()
    }

    suspend fun delete(id: Uuid): Boolean = DatabaseFactory.dbQuery {
        val deleted = RagasTable.deleteWhere { RagasTable.id eq id.toJavaUuid() }
        deleted > 0
    }

    suspend fun countAll(): Long = DatabaseFactory.dbQuery {
        RagasTable.selectAll().count()
    }

    suspend fun countJanyaNotSubsetOfParent(): Long = DatabaseFactory.dbQuery {
        exec(
            """
            SELECT count(*) FROM ragas j
              JOIN ragas p ON p.id = j.parent_raga_id
             WHERE j.parent_raga_id IS NOT NULL
               AND j.parent_raga_id <> j.id
               AND p.melakarta_number IS NOT NULL
               AND j.arohanam IS NOT NULL AND j.avarohanam IS NOT NULL
               AND p.arohanam IS NOT NULL AND p.avarohanam IS NOT NULL
               AND j.name NOT LIKE '%{%'
               AND j.arohanam !~* 'anya'
               AND j.avarohanam !~* 'anya'
               AND EXISTS (
                   SELECT 1
                     FROM unnest(raga_swara_tokens(j.arohanam) || raga_swara_tokens(j.avarohanam)) AS js(tok)
                    WHERE js.tok NOT IN ('S', 'P')
                      AND NOT (js.tok = ANY (raga_swara_tokens(p.arohanam) || raga_swara_tokens(p.avarohanam)))
               )
            """.trimIndent(),
        ) { rs -> if (rs.next()) rs.getLong(1) else 0L } ?: 0L
    }

    suspend fun countMelaAsOwnJanya(): Long = DatabaseFactory.dbQuery {
        exec(
            """
            SELECT count(*) FROM ragas j
             WHERE j.parent_raga_id = j.id
               AND j.melakarta_number IS NULL
               AND EXISTS (
                   SELECT 1 FROM ragas m
                    WHERE m.melakarta_number IS NOT NULL
                      AND raga_swara_signature(j.arohanam, j.avarohanam)
                        = raga_swara_signature(m.arohanam, m.avarohanam)
                      AND raga_swara_signature(j.arohanam, j.avarohanam) <> ''
               )
            """.trimIndent(),
        ) { rs -> if (rs.next()) rs.getLong(1) else 0L } ?: 0L
    }

    private fun appendJsonArray(existing: String?, elementJson: String): String {
        val current = if (existing.isNullOrBlank()) JsonArray(emptyList()) else
            json.parseToJsonElement(existing).jsonArray
        val next = json.parseToJsonElement(elementJson)
        return JsonArray(current + next).toString()
    }

    private fun parseOccurrences(contextJson: String?): List<RagaQueueOccurrence> {
        if (contextJson.isNullOrBlank()) return emptyList()
        return runCatching {
            json.decodeFromString(ListSerializer(RagaQueueOccurrence.serializer()), contextJson)
        }.getOrElse { emptyList() }
    }

    private fun ResultRow.toQueueItem(): RagaQueueItemDto {
        val created = this[RagaResolutionQueueTable.createdAt]
        val resolved = this[RagaResolutionQueueTable.resolvedAt]
        return RagaQueueItemDto(
            id = this[RagaResolutionQueueTable.id].value.toString(),
            rawName = this[RagaResolutionQueueTable.rawName],
            matchKey = this[RagaResolutionQueueTable.matchKey],
            kind = this[RagaResolutionQueueTable.kind],
            context = this[RagaResolutionQueueTable.context],
            proposedLakshana = this[RagaResolutionQueueTable.proposedLakshana],
            status = this[RagaResolutionQueueTable.status],
            resolvedRagaId = this[RagaResolutionQueueTable.resolvedRagaId]?.toString(),
            createdAt = created.toInstant().toString(),
            resolvedAt = resolved?.toInstant()?.toString(),
        )
    }
}
