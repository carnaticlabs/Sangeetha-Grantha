package com.sangita.grantha.backend.dal.repositories

import com.sangita.grantha.backend.dal.DatabaseFactory
import com.sangita.grantha.backend.dal.support.toJavaUuid
import com.sangita.grantha.backend.dal.support.toKotlinUuid
import com.sangita.grantha.backend.dal.tables.ImportSourcesEnhancedTable
import com.sangita.grantha.backend.dal.tables.KrithiSourceEvidenceTable
import com.sangita.grantha.shared.domain.model.ImportSourceDto
import com.sangita.grantha.shared.domain.model.ImportSourceDetailDto
import com.sangita.grantha.shared.domain.model.ContributionStatsDto
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.util.UUID
import kotlin.uuid.Uuid
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json
import org.jetbrains.exposed.v1.core.*
import org.jetbrains.exposed.v1.jdbc.*

/**
 * Repository for source registry operations.
 * Manages import_sources with enhanced fields from migration 23.
 */
class SourceRegistryRepository {

    private val T = ImportSourcesEnhancedTable

    private fun toInstant(odt: java.time.OffsetDateTime?): kotlin.time.Instant? =
        odt?.toInstant()?.let { kotlin.time.Instant.fromEpochSeconds(it.epochSecond, it.nano) }

    private fun ResultRow.toImportSourceDto(): ImportSourceDto {
        val formats = parseTextArray(this[T.supportedFormats])
        val affinity = parseJsonMap(this[T.composerAffinity])

        return ImportSourceDto(
            id = this[T.id],
            name = this[T.name],
            baseUrl = this[T.baseUrl],
            description = this[T.description],
            sourceTier = this[T.sourceTier],
            supportedFormats = formats,
            composerAffinity = affinity,
            lastHarvestedAt = toInstant(this[T.lastHarvestedAt]),
            isActive = true, // No active column in DB; all existing records are active
            createdAt = toInstant(this[T.createdAt])!!,
            updatedAt = toInstant(this[T.updatedAt])!!,
        )
    }

    suspend fun list(
        tier: List<Int>? = null,
        format: List<String>? = null,
        search: String? = null,
        limit: Int = 20,
        offset: Int = 0,
    ): Pair<List<ImportSourceDto>, Int> = DatabaseFactory.dbQuery {
        var query = T.selectAll()

        tier?.takeIf { it.isNotEmpty() }?.let { tiers ->
            query = query.andWhere { T.sourceTier inList tiers }
        }

        search?.takeIf { it.isNotBlank() }?.let { s ->
            query = query.andWhere {
                T.name like "%${s}%"
            }
        }

        val total = query.count().toInt()
        val items = query
            .orderBy(T.name to SortOrder.ASC)
            .limit(limit)
            .offset(offset.toLong())
            .map { it.toImportSourceDto() }

        Pair(items, total)
    }

    suspend fun findById(id: Uuid): ImportSourceDto? = DatabaseFactory.dbQuery {
        T.selectAll()
            .andWhere { T.id eq id }
            .map { it.toImportSourceDto() }
            .singleOrNull()
    }

    suspend fun findDetailById(id: Uuid): ImportSourceDetailDto? = DatabaseFactory.dbQuery {
        val source = T.selectAll()
            .andWhere { T.id eq id }
            .map { it.toImportSourceDto() }
            .singleOrNull() ?: return@dbQuery null

        // Get contribution stats
        val evidenceCount = KrithiSourceEvidenceTable.selectAll()
            .andWhere { KrithiSourceEvidenceTable.importSourceId eq id.toJavaUuid() }
            .count().toInt()

        val stats = ContributionStatsDto(
            totalKrithis = evidenceCount,
            fieldBreakdown = emptyMap(), // Simplified for initial implementation
            avgConfidence = 0.0,
            extractionSuccessRate = 0.0,
        )

        ImportSourceDetailDto(
            id = source.id,
            name = source.name,
            baseUrl = source.baseUrl,
            description = source.description,
            sourceTier = source.sourceTier,
            supportedFormats = source.supportedFormats,
            composerAffinity = source.composerAffinity,
            lastHarvestedAt = source.lastHarvestedAt,
            isActive = source.isActive,
            krithiCount = evidenceCount,
            createdAt = source.createdAt,
            updatedAt = source.updatedAt,
            contributionStats = stats,
        )
    }

    suspend fun create(
        name: String,
        baseUrl: String,
        description: String? = null,
        sourceTier: Int = 5,
        supportedFormats: List<String> = listOf("HTML"),
        composerAffinity: Map<String, Double> = emptyMap(),
    ): ImportSourceDto = DatabaseFactory.dbQuery {
        val now = OffsetDateTime.now(ZoneOffset.UTC)
        val sourceId = UUID.randomUUID()

        T.insert {
            it[T.id] = sourceId.toKotlinUuid()
            it[T.name] = name
            it[T.baseUrl] = baseUrl
            it[T.description] = description
            it[T.sourceTier] = sourceTier
            it[T.supportedFormats] = formatTextArray(supportedFormats)
            it[T.composerAffinity] = Json.encodeToString(MapSerializer(String.serializer(), Double.serializer()), composerAffinity)
            it[T.createdAt] = now
            it[T.updatedAt] = now
        }

        T.selectAll()
            .andWhere { T.id eq sourceId.toKotlinUuid() }
            .map { it.toImportSourceDto() }
            .single()
    }

    suspend fun update(
        id: Uuid,
        name: String? = null,
        baseUrl: String? = null,
        description: String? = null,
        sourceTier: Int? = null,
        supportedFormats: List<String>? = null,
        composerAffinity: Map<String, Double>? = null,
    ): ImportSourceDto? = DatabaseFactory.dbQuery {
        val now = OffsetDateTime.now(ZoneOffset.UTC)

        T.update({ T.id eq id }) { stmt ->
            name?.let { stmt[T.name] = it }
            baseUrl?.let { stmt[T.baseUrl] = it }
            description?.let { stmt[T.description] = it }
            sourceTier?.let { stmt[T.sourceTier] = it }
            supportedFormats?.let { stmt[T.supportedFormats] = formatTextArray(it) }
            composerAffinity?.let { stmt[T.composerAffinity] = Json.encodeToString(MapSerializer(String.serializer(), Double.serializer()), it) }
            stmt[T.updatedAt] = now
        }

        T.selectAll()
            .andWhere { T.id eq id }
            .map { it.toImportSourceDto() }
            .singleOrNull()
    }

    // --- Helpers ---

    private fun parseTextArray(raw: String): List<String> {
        // PostgreSQL TEXT[] comes as "{HTML,PDF}" format
        return raw.trim('{', '}').split(",").filter { it.isNotBlank() }
    }

    private fun formatTextArray(list: List<String>): String {
        return "{${list.joinToString(",")}}"
    }

    private fun parseJsonMap(raw: String): Map<String, Double> {
        return try {
            Json.decodeFromString<Map<String, Double>>(raw)
        } catch (_: Exception) {
            emptyMap()
        }
    }
}
