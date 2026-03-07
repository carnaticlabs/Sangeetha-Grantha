package com.sangita.grantha.backend.dal.repositories

import com.sangita.grantha.backend.dal.DatabaseFactory
import com.sangita.grantha.backend.dal.enums.LanguageCode
import com.sangita.grantha.backend.dal.enums.ScriptCode
import com.sangita.grantha.backend.dal.models.toKrithiLyricVariantDto
import com.sangita.grantha.backend.dal.models.toKrithiLyricSectionDto
import com.sangita.grantha.backend.dal.support.toJavaUuid
import com.sangita.grantha.backend.dal.tables.KrithiLyricVariantsTable
import com.sangita.grantha.backend.dal.tables.KrithiLyricSectionsTable
import com.sangita.grantha.shared.domain.model.KrithiLyricVariantDto
import com.sangita.grantha.shared.domain.model.KrithiLyricVariantWithSectionsDto
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.util.UUID
import kotlin.uuid.Uuid
import org.jetbrains.exposed.v1.exceptions.ExposedSQLException
import org.jetbrains.exposed.v1.jdbc.*
import org.jetbrains.exposed.v1.core.*
import org.slf4j.LoggerFactory

/**
 * Repository for krithi lyric variants and lyric sections.
 * Extracted from KrithiRepository as part of TRACK-074.
 */
class KrithiLyricRepository {
    companion object {
        private const val LYRICS_INDEX_SAFE_UTF8_BYTES = 1800
        private val logger = LoggerFactory.getLogger(KrithiLyricRepository::class.java)

        private fun truncateUtf8ByBytes(input: String, maxBytes: Int): String {
            val bytes = input.toByteArray(Charsets.UTF_8)
            if (bytes.size <= maxBytes) return input

            var end = maxBytes.coerceAtMost(bytes.size)
            while (end > 0 && (bytes[end - 1].toInt() and 0b1100_0000) == 0b1000_0000) {
                end--
            }
            if (end <= 0) return ""
            return String(bytes, 0, end, Charsets.UTF_8).trimEnd()
        }

        private fun isLyricsIndexRowSizeOverflow(error: Throwable): Boolean {
            val messages = buildList {
                var current: Throwable? = error
                while (current != null) {
                    current.message?.let(::add)
                    current = current.cause
                }
            }.joinToString(" ")

            return messages.contains("idx_krithi_lyric_variants_lyrics", ignoreCase = true) &&
                messages.contains("index row size", ignoreCase = true)
        }
    }

    /**
     * List lyric variants with their sections for a krithi.
     */
    suspend fun getLyricVariants(krithiId: Uuid): List<KrithiLyricVariantWithSectionsDto> = DatabaseFactory.dbQuery {
        val javaKrithiId = krithiId.toJavaUuid()

        val variants = KrithiLyricVariantsTable
            .selectAll()
            .where { KrithiLyricVariantsTable.krithiId eq javaKrithiId }
            .map { it.toKrithiLyricVariantDto() }

        val variantIds = variants.map { it.id.toJavaUuid() }
        val sections = if (variantIds.isNotEmpty()) {
            KrithiLyricSectionsTable
                .selectAll()
                .where { KrithiLyricSectionsTable.lyricVariantId inList variantIds }
                .map { it.toKrithiLyricSectionDto() }
                .groupBy { it.lyricVariantId }
        } else {
            emptyMap()
        }

        variants.map { variant ->
            KrithiLyricVariantWithSectionsDto(
                variant = variant,
                sections = sections[variant.id] ?: emptyList()
            )
        }
    }

    /**
     * Find a lyric variant by ID.
     */
    suspend fun findLyricVariantById(variantId: Uuid): KrithiLyricVariantDto? = DatabaseFactory.dbQuery {
        KrithiLyricVariantsTable
            .selectAll()
            .where { KrithiLyricVariantsTable.id eq variantId.toJavaUuid() }
            .map { it.toKrithiLyricVariantDto() }
            .singleOrNull()
    }

    /**
     * Create a lyric variant for a krithi.
     */
    suspend fun createLyricVariant(
        krithiId: Uuid,
        language: LanguageCode,
        script: ScriptCode,
        transliterationScheme: String? = null,
        sampradayaId: UUID? = null,
        variantLabel: String? = null,
        sourceReference: String? = null,
        lyrics: String,
        isPrimary: Boolean = false,
        createdByUserId: UUID? = null,
        updatedByUserId: UUID? = null
    ): KrithiLyricVariantDto = DatabaseFactory.dbQuery {
        val now = OffsetDateTime.now(ZoneOffset.UTC)
        val javaKrithiId = krithiId.toJavaUuid()

        // TRACK-062: Idempotency guard
        val existing = KrithiLyricVariantsTable.selectAll()
            .andWhere { KrithiLyricVariantsTable.krithiId eq javaKrithiId }
            .andWhere { KrithiLyricVariantsTable.language eq language }
            .andWhere { KrithiLyricVariantsTable.script eq script }
            .limit(1)
            .singleOrNull()

        if (existing != null) {
            return@dbQuery existing.toKrithiLyricVariantDto()
        }

        val variantId = UUID.randomUUID()

        fun insertVariant(lyricsValue: String): KrithiLyricVariantDto {
            return KrithiLyricVariantsTable.insert {
                it[id] = variantId
                it[KrithiLyricVariantsTable.krithiId] = javaKrithiId
                it[KrithiLyricVariantsTable.language] = language
                it[KrithiLyricVariantsTable.script] = script
                it[KrithiLyricVariantsTable.transliterationScheme] = transliterationScheme
                it[KrithiLyricVariantsTable.sampradayaId] = sampradayaId
                it[KrithiLyricVariantsTable.variantLabel] = variantLabel
                it[KrithiLyricVariantsTable.sourceReference] = sourceReference
                it[KrithiLyricVariantsTable.lyrics] = lyricsValue
                it[KrithiLyricVariantsTable.isPrimary] = isPrimary
                it[KrithiLyricVariantsTable.createdByUserId] = createdByUserId
                it[KrithiLyricVariantsTable.updatedByUserId] = updatedByUserId
                it[KrithiLyricVariantsTable.createdAt] = now
                it[KrithiLyricVariantsTable.updatedAt] = now
            }
                .resultedValues
                ?.single()
                ?.toKrithiLyricVariantDto()
                ?: error("Failed to insert lyric variant")
        }

        try {
            insertVariant(lyrics)
        } catch (e: ExposedSQLException) {
            if (!isLyricsIndexRowSizeOverflow(e)) throw e

            val truncated = truncateUtf8ByBytes(lyrics, LYRICS_INDEX_SAFE_UTF8_BYTES)
            if (truncated.isBlank()) throw e

            logger.warn(
                "Retrying lyric variant insert with truncated lyrics due to oversized btree index row (krithiId={}, language={}, script={}, originalBytes={}, truncatedBytes={})",
                krithiId,
                language,
                script,
                lyrics.toByteArray(Charsets.UTF_8).size,
                truncated.toByteArray(Charsets.UTF_8).size
            )
            insertVariant(truncated)
        }
    }

    /**
     * Update a lyric variant and return the updated record.
     */
    suspend fun updateLyricVariant(
        variantId: Uuid,
        language: LanguageCode? = null,
        script: ScriptCode? = null,
        transliterationScheme: String? = null,
        sampradayaId: UUID? = null,
        variantLabel: String? = null,
        sourceReference: String? = null,
        lyrics: String? = null,
        isPrimary: Boolean? = null,
        updatedByUserId: UUID? = null
    ): KrithiLyricVariantDto? = DatabaseFactory.dbQuery {
        val now = OffsetDateTime.now(ZoneOffset.UTC)
        val javaVariantId = variantId.toJavaUuid()

        KrithiLyricVariantsTable
            .updateReturning(
                where = { KrithiLyricVariantsTable.id eq javaVariantId }
            ) {
                language?.let { value -> it[KrithiLyricVariantsTable.language] = value }
                script?.let { value -> it[KrithiLyricVariantsTable.script] = value }
                transliterationScheme?.let { value -> it[KrithiLyricVariantsTable.transliterationScheme] = value }
                sampradayaId?.let { value -> it[KrithiLyricVariantsTable.sampradayaId] = value }
                variantLabel?.let { value -> it[KrithiLyricVariantsTable.variantLabel] = value }
                sourceReference?.let { value -> it[KrithiLyricVariantsTable.sourceReference] = value }
                lyrics?.let { value -> it[KrithiLyricVariantsTable.lyrics] = value }
                isPrimary?.let { value -> it[KrithiLyricVariantsTable.isPrimary] = value }
                updatedByUserId?.let { value -> it[KrithiLyricVariantsTable.updatedByUserId] = value }
                it[KrithiLyricVariantsTable.updatedAt] = now
            }
            .singleOrNull()
            ?.toKrithiLyricVariantDto()
    }

    /**
     * Saves lyric variant sections using efficient UPDATE/INSERT/DELETE operations.
     */
    suspend fun saveLyricVariantSections(
        variantId: Uuid,
        sections: List<Pair<UUID, String>> // (sectionId, text)
    ) = DatabaseFactory.dbQuery {
        val now = OffsetDateTime.now(ZoneOffset.UTC)
        val javaVariantId = variantId.toJavaUuid()

        val existingSections = KrithiLyricSectionsTable
            .selectAll()
            .where { KrithiLyricSectionsTable.lyricVariantId eq javaVariantId }
            .associateBy { it[KrithiLyricSectionsTable.sectionId] }

        val newSectionsMap = sections.associateBy { it.first }

        val toUpdate = mutableListOf<Pair<UUID, String>>()
        val toInsert = mutableListOf<Pair<UUID, String>>()
        val toDelete = mutableListOf<UUID>()

        existingSections.forEach { (sectionId, row) ->
            val existingId = row[KrithiLyricSectionsTable.id].value
            val existingText = row[KrithiLyricSectionsTable.text]
            val newSection = newSectionsMap[sectionId]

            if (newSection != null) {
                val (_, newText) = newSection
                if (newText != existingText) {
                    toUpdate.add(existingId to newText)
                }
            } else {
                toDelete.add(existingId)
            }
        }

        newSectionsMap.forEach { (sectionId, section) ->
            if (!existingSections.containsKey(sectionId)) {
                toInsert.add(section)
            }
        }

        toUpdate.forEach { (id, text) ->
            KrithiLyricSectionsTable.update({ KrithiLyricSectionsTable.id eq id }) {
                it[KrithiLyricSectionsTable.text] = text
                it[KrithiLyricSectionsTable.updatedAt] = now
            }
        }

        if (toInsert.isNotEmpty()) {
            KrithiLyricSectionsTable.batchInsert(toInsert) { (sectionId, text) ->
                this[KrithiLyricSectionsTable.id] = UUID.randomUUID()
                this[KrithiLyricSectionsTable.lyricVariantId] = javaVariantId
                this[KrithiLyricSectionsTable.sectionId] = sectionId
                this[KrithiLyricSectionsTable.text] = text
                this[KrithiLyricSectionsTable.normalizedText] = null
                this[KrithiLyricSectionsTable.createdAt] = now
                this[KrithiLyricSectionsTable.updatedAt] = now
            }
        }

        if (toDelete.isNotEmpty()) {
            KrithiLyricSectionsTable.deleteWhere {
                KrithiLyricSectionsTable.id inList toDelete
            }
        }
    }
}
