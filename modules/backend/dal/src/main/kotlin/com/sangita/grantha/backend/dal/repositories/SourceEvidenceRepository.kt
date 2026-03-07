package com.sangita.grantha.backend.dal.repositories

import com.sangita.grantha.backend.dal.DatabaseFactory
import com.sangita.grantha.backend.dal.support.toJavaUuid
import com.sangita.grantha.backend.dal.support.toKotlinUuid
import com.sangita.grantha.backend.dal.tables.ImportSourcesEnhancedTable
import com.sangita.grantha.backend.dal.tables.KrithiSourceEvidenceTable
import com.sangita.grantha.backend.dal.tables.KrithisTable
import com.sangita.grantha.backend.dal.tables.ComposersTable
import com.sangita.grantha.backend.dal.tables.RagasTable
import com.sangita.grantha.backend.dal.tables.TalasTable
import com.sangita.grantha.shared.domain.model.SourceEvidenceSummaryDto
import com.sangita.grantha.shared.domain.model.KrithiEvidenceSourceDto
import com.sangita.grantha.shared.domain.model.KrithiEvidenceResponseDto
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.util.UUID
import kotlin.uuid.Uuid
import org.jetbrains.exposed.v1.core.*
import org.jetbrains.exposed.v1.jdbc.*

/**
 * Repository for source evidence queries.
 */
class SourceEvidenceRepository {

    private val E = KrithiSourceEvidenceTable
    private val S = ImportSourcesEnhancedTable
    private val K = KrithisTable

    private fun toInstant(odt: java.time.OffsetDateTime): kotlin.time.Instant =
        odt.toInstant().let { kotlin.time.Instant.fromEpochSeconds(it.epochSecond, it.nano) }

    suspend fun listEvidenceSummaries(
        minSourceCount: Int? = null,
        search: String? = null,
        limit: Int = 20,
        offset: Int = 0,
    ): Pair<List<SourceEvidenceSummaryDto>, Int> = DatabaseFactory.dbQuery {
        val countExpr = E.krithiId.count()

        // Build grouped query with optional HAVING filter
        var groupedQuery = E.select(E.krithiId, countExpr)
            .groupBy(E.krithiId)
        if (minSourceCount != null && minSourceCount > 1) {
            groupedQuery = groupedQuery.having { countExpr greaterEq minSourceCount.toLong() }
        }

        // Count total matching groups
        val total = groupedQuery.count().toInt()

        // Apply pagination at SQL level instead of loading all then slicing in-memory
        val page = groupedQuery
            .orderBy(countExpr to SortOrder.DESC)
            .limit(limit)
            .offset(offset.toLong())
            .map { it[E.krithiId] to it[countExpr].toInt() }

        // Batch-load krithi titles in a single query (eliminates N+1)
        val krithiIds = page.map { it.first }
        val titleMap = if (krithiIds.isNotEmpty()) {
            K.select(K.id, K.title)
                .where { K.id inList krithiIds }
                .associate { it[K.id].value to it[K.title] }
        } else emptyMap()

        val summaries = page.map { (krithiId, count) ->
            SourceEvidenceSummaryDto(
                krithiId = krithiId.toKotlinUuid(),
                krithiTitle = titleMap[krithiId] ?: "Unknown",
                sourceCount = count,
                topSourceName = "",
                topSourceTier = 5,
                contributedFields = emptyList(),
                avgConfidence = 0.0,
            )
        }

        Pair(summaries, total)
    }

    suspend fun getKrithiEvidence(krithiId: Uuid): KrithiEvidenceResponseDto? = DatabaseFactory.dbQuery {
        val javaId = krithiId.toJavaUuid()

        val krithi = K.selectAll()
            .andWhere { K.id eq javaId }
            .singleOrNull() ?: return@dbQuery null

        val evidenceRows = E.selectAll()
            .andWhere { E.krithiId eq javaId }
            .orderBy(E.extractedAt to SortOrder.DESC)
            .toList()

        // Batch-load import sources in a single query (eliminates N+1)
        val sourceIds = evidenceRows.map { it[E.importSourceId].toKotlinUuid() }.distinct()
        val sourceMap = if (sourceIds.isNotEmpty()) {
            S.selectAll()
                .where { S.id inList sourceIds }
                .associate { it[S.id].toJavaUuid() to (it[S.name] to it[S.sourceTier]) }
        } else emptyMap()

        val sources = evidenceRows.map { row ->
            val sourceId = row[E.importSourceId]
            val (sourceName, sourceTier) = sourceMap[sourceId] ?: ("Unknown" to 5)

            KrithiEvidenceSourceDto(
                importSourceId = sourceId.toKotlinUuid(),
                sourceName = sourceName,
                sourceTier = sourceTier,
                sourceFormat = row[E.sourceFormat],
                sourceUrl = row[E.sourceUrl],
                extractionMethod = row[E.extractionMethod],
                confidence = row[E.confidence]?.toDouble() ?: 0.0,
                contributedFields = row[E.contributedFields].trim('{', '}').split(",").filter { it.isNotBlank() },
                extractedAt = toInstant(row[E.extractedAt]),
                rawExtraction = row[E.rawExtraction],
            )
        }

        KrithiEvidenceResponseDto(
            krithiId = krithiId,
            krithiTitle = krithi[K.title],
            sources = sources,
        )
    }

    /**
     * Count evidence records for each Krithi in a batch.
     */
    suspend fun countByKrithiIds(krithiIds: List<Uuid>): Map<Uuid, Int> = DatabaseFactory.dbQuery {
        if (krithiIds.isEmpty()) return@dbQuery emptyMap()
        val javaIds = krithiIds.map { it.toJavaUuid() }
        val countExpr = E.id.count()

        E.select(E.krithiId, countExpr)
            .where { E.krithiId inList javaIds }
            .groupBy(E.krithiId)
            .associate { row ->
                row[E.krithiId].toKotlinUuid() to row[countExpr].toInt()
            }
    }

    /**
     * TRACK-041 / TRACK-053: Create a source evidence record linking a Krithi to a contributing source.
     *
     * Resolves the import_source_id by:
     *  1. Matching by source name
     *  2. Matching by URL domain extracted from sourceUrl
     *  3. Auto-creating a new import_sources record if no match is found
     */
    suspend fun createEvidence(
        krithiId: Uuid,
        sourceUrl: String,
        sourceName: String,
        sourceTier: Int,
        sourceFormat: String,
        extractionMethod: String,
        pageRange: String? = null,
        checksum: String? = null,
        confidence: Double? = null,
        contributedFields: List<String> = emptyList(),
        rawExtraction: String? = null,
    ): Unit = DatabaseFactory.dbQuery {
        val now = OffsetDateTime.now(ZoneOffset.UTC)

        // Resolve import_source_id: name match → URL domain match → auto-create
        val importSourceId: UUID = resolveImportSourceId(sourceName, sourceUrl, sourceTier, now)

        // Check for duplicate: same Krithi + same source URL
        val existing = E.selectAll()
            .andWhere { E.krithiId eq krithiId.toJavaUuid() }
            .andWhere { E.sourceUrl eq sourceUrl }
            .count()

        if (existing > 0) return@dbQuery // Idempotent — skip if already exists

        E.insert {
            it[E.id] = UUID.randomUUID()
            it[E.krithiId] = krithiId.toJavaUuid()
            it[E.importSourceId] = importSourceId
            it[E.sourceUrl] = sourceUrl
            it[E.sourceFormat] = sourceFormat
            it[E.extractionMethod] = extractionMethod
            it[E.pageRange] = pageRange
            it[E.extractedAt] = now
            it[E.checksum] = checksum
            it[E.confidence] = confidence?.toBigDecimal()
            it[E.contributedFields] = "{${contributedFields.joinToString(",")}}"
            it[E.rawExtraction] = rawExtraction
            it[E.createdAt] = now
        }
    }

    /**
     * Resolve the import_source_id for a source evidence record.
     * Falls back to creating a new import_sources record if no match is found.
     */
    private fun resolveImportSourceId(
        sourceName: String,
        sourceUrl: String,
        sourceTier: Int,
        now: OffsetDateTime,
    ): UUID {
        // 1. Try matching by source name
        S.selectAll()
            .andWhere { S.name eq sourceName }
            .singleOrNull()
            ?.let { return it[S.id].toJavaUuid() }

        // 2. Try matching by domain extracted from source URL
        val domain = extractDomain(sourceUrl)
        if (domain != null) {
            S.selectAll()
                .andWhere { S.name eq domain }
                .singleOrNull()
                ?.let { return it[S.id].toJavaUuid() }
        }

        // 3. Auto-create a new import_sources record
        val newId = UUID.randomUUID()
        val displayName = domain ?: sourceName
        ImportSourcesEnhancedTable.insert {
            it[id] = newId.toKotlinUuid()
            it[name] = displayName
            it[baseUrl] = domain?.let { d -> "https://$d" }
            it[ImportSourcesEnhancedTable.sourceTier] = sourceTier
            it[supportedFormats] = "{}" // empty array
            it[composerAffinity] = "{}" // empty JSON
            it[createdAt] = now
            it[updatedAt] = now
        }
        return newId
    }

    /**
     * Extract the domain (e.g. "guruguha.org") from a URL string.
     */
    private fun extractDomain(url: String): String? {
        return try {
            val withoutProtocol = url
                .removePrefix("https://")
                .removePrefix("http://")
                .removePrefix("www.")
            val domain = withoutProtocol.substringBefore("/")
            domain.takeIf { it.contains(".") && it.isNotBlank() }
        } catch (_: Exception) {
            null
        }
    }
}
