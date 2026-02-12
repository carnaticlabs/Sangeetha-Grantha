package com.sangita.grantha.backend.api.services

import com.sangita.grantha.backend.dal.DatabaseFactory
import com.sangita.grantha.backend.dal.SangitaDal
import com.sangita.grantha.backend.dal.tables.KrithiLyricSectionsTable
import com.sangita.grantha.backend.dal.tables.KrithiLyricVariantsTable
import com.sangita.grantha.backend.dal.tables.KrithisTable
import com.sangita.grantha.backend.dal.tables.ComposersTable
import kotlinx.serialization.Serializable
import org.jetbrains.exposed.v1.core.*
import org.jetbrains.exposed.v1.jdbc.*
import org.slf4j.LoggerFactory
import kotlin.uuid.Uuid

/**
 * TRACK-040: Strips boilerplate metadata pollution from lyric section text.
 *
 * Patterns detected and cleaned:
 * - Source attribution lines ("Source: ...", "From: ...")
 * - URLs embedded in lyric text
 * - Translation/meaning markers ("Meaning:", "Translation:", "Word-by-word:")
 * - Copyright notices
 * - Page numbers ("Page 42", "p. 42")
 * - Excessive whitespace / empty lines
 * - Unicode BOM characters
 * - Repetitive boilerplate headers
 */
class MetadataCleanupService(private val dal: SangitaDal) {
    private val logger = LoggerFactory.getLogger(javaClass)

    @Serializable
    data class CleanupPreview(
        val totalVariantsScanned: Int,
        val totalSectionsScanned: Int,
        val dirtyItems: List<DirtyItem>,
    )

    @Serializable
    data class DirtyItem(
        val krithiId: String,
        val krithiTitle: String,
        val composer: String,
        val variantId: String,
        val sectionId: String,
        val language: String,
        val patterns: List<String>,
        val originalSnippet: String,
        val cleanedSnippet: String,
    )

    @Serializable
    data class CleanupReport(
        val sectionsScanned: Int,
        val sectionsCleaned: Int,
        val variantsCleaned: Int,
        val patternsRemoved: Map<String, Int>,
    )

    /**
     * Preview what would be cleaned without making changes.
     */
    suspend fun preview(composerFilter: String? = null, limit: Int = 100): CleanupPreview =
        DatabaseFactory.dbQuery {
            val dirtyItems = mutableListOf<DirtyItem>()
            var variantsScanned = 0
            var sectionsScanned = 0

            // Build query for lyric sections with their parent variant and krithi info
            val query = KrithiLyricSectionsTable
                .innerJoin(KrithiLyricVariantsTable,
                    { KrithiLyricSectionsTable.lyricVariantId },
                    { KrithiLyricVariantsTable.id })
                .innerJoin(KrithisTable,
                    { KrithiLyricVariantsTable.krithiId },
                    { KrithisTable.id })
                .innerJoin(ComposersTable,
                    { KrithisTable.composerId },
                    { ComposersTable.id })
                .selectAll()

            composerFilter?.let { filter ->
                query.andWhere {
                    ComposersTable.nameNormalized like "%${filter.lowercase()}%"
                }
            }

            query.orderBy(ComposersTable.name to SortOrder.ASC, KrithisTable.title to SortOrder.ASC)

            val seenVariants = mutableSetOf<java.util.UUID>()
            for (row in query) {
                sectionsScanned++
                val variantId = row[KrithiLyricSectionsTable.lyricVariantId]
                if (variantId !in seenVariants) {
                    seenVariants.add(variantId)
                    variantsScanned++
                }

                val text = row[KrithiLyricSectionsTable.text]
                val patterns = detectPatterns(text)
                if (patterns.isNotEmpty() && dirtyItems.size < limit) {
                    val cleaned = cleanText(text)
                    dirtyItems.add(
                        DirtyItem(
                            krithiId = row[KrithisTable.id].value.toString(),
                            krithiTitle = row[KrithisTable.title],
                            composer = row[ComposersTable.name],
                            variantId = variantId.toString(),
                            sectionId = row[KrithiLyricSectionsTable.id].value.toString(),
                            language = row[KrithiLyricVariantsTable.language].dbValue,
                            patterns = patterns,
                            originalSnippet = text.take(200),
                            cleanedSnippet = cleaned.take(200),
                        )
                    )
                }
            }

            CleanupPreview(variantsScanned, sectionsScanned, dirtyItems)
        }

    /**
     * Execute cleanup: strip boilerplate from all matching lyric sections.
     */
    suspend fun execute(
        composerFilter: String? = null,
        dryRun: Boolean = false,
    ): CleanupReport = DatabaseFactory.dbQuery {
        var sectionsScanned = 0
        var sectionsCleaned = 0
        val cleanedVariants = mutableSetOf<java.util.UUID>()
        val patternCounts = mutableMapOf<String, Int>()

        val query = KrithiLyricSectionsTable.selectAll()

        for (row in query) {
            sectionsScanned++
            val sectionId = row[KrithiLyricSectionsTable.id].value
            val text = row[KrithiLyricSectionsTable.text]
            val patterns = detectPatterns(text)

            if (patterns.isNotEmpty()) {
                patterns.forEach { p ->
                    patternCounts[p] = (patternCounts[p] ?: 0) + 1
                }

                if (!dryRun) {
                    val cleaned = cleanText(text)
                    KrithiLyricSectionsTable.update({ KrithiLyricSectionsTable.id eq sectionId }) {
                        it[KrithiLyricSectionsTable.text] = cleaned
                    }
                }
                sectionsCleaned++
                cleanedVariants.add(row[KrithiLyricSectionsTable.lyricVariantId])
            }
        }

        CleanupReport(sectionsScanned, sectionsCleaned, cleanedVariants.size, patternCounts)
    }

    /**
     * Detect which boilerplate patterns are present in text.
     */
    internal fun detectPatterns(text: String): List<String> {
        val patterns = mutableListOf<String>()

        if (URL_PATTERN.containsMatchIn(text)) patterns.add("URL")
        if (SOURCE_ATTR_PATTERN.containsMatchIn(text)) patterns.add("SOURCE_ATTRIBUTION")
        if (MEANING_PATTERN.containsMatchIn(text)) patterns.add("MEANING_MARKER")
        if (COPYRIGHT_PATTERN.containsMatchIn(text)) patterns.add("COPYRIGHT")
        if (PAGE_NUMBER_PATTERN.containsMatchIn(text)) patterns.add("PAGE_NUMBER")
        if (BOM_PATTERN.containsMatchIn(text)) patterns.add("UNICODE_BOM")
        if (EXCESSIVE_WHITESPACE_PATTERN.containsMatchIn(text)) patterns.add("EXCESSIVE_WHITESPACE")
        if (BOILERPLATE_HEADER_PATTERN.containsMatchIn(text)) patterns.add("BOILERPLATE_HEADER")

        return patterns
    }

    /**
     * Clean text by removing detected boilerplate patterns.
     */
    internal fun cleanText(text: String): String {
        var cleaned = text

        // Remove BOM
        cleaned = cleaned.replace("\uFEFF", "")

        // Remove URLs
        cleaned = URL_PATTERN.replace(cleaned, "")

        // Remove source attribution lines
        cleaned = SOURCE_ATTR_PATTERN.replace(cleaned, "")

        // Remove meaning/translation markers and everything after on the same line
        cleaned = MEANING_PATTERN.replace(cleaned, "")

        // Remove copyright notices
        cleaned = COPYRIGHT_PATTERN.replace(cleaned, "")

        // Remove page numbers
        cleaned = PAGE_NUMBER_PATTERN.replace(cleaned, "")

        // Remove boilerplate headers
        cleaned = BOILERPLATE_HEADER_PATTERN.replace(cleaned, "")

        // Normalize whitespace: collapse multiple blank lines to single
        cleaned = cleaned.replace(Regex("\n{3,}"), "\n\n")

        // Trim leading/trailing whitespace per line
        cleaned = cleaned.lines().joinToString("\n") { it.trimEnd() }

        // Trim overall
        cleaned = cleaned.trim()

        return cleaned
    }

    companion object {
        private val URL_PATTERN = Regex(
            """https?://[^\s<>"{}|\\^`\[\]]+""",
            RegexOption.IGNORE_CASE,
        )

        private val SOURCE_ATTR_PATTERN = Regex(
            """^(?:Source|From|Ref(?:erence)?|Courtesy|Credit)\s*[:–—-]\s*.+$""",
            setOf(RegexOption.IGNORE_CASE, RegexOption.MULTILINE),
        )

        private val MEANING_PATTERN = Regex(
            """^(?:Meaning|Translation|Word[- ]by[- ]word|Explanation|Interpretation)\s*[:–—-]\s*.*$""",
            setOf(RegexOption.IGNORE_CASE, RegexOption.MULTILINE),
        )

        private val COPYRIGHT_PATTERN = Regex(
            """^(?:©|Copyright|\(c\)).*$""",
            setOf(RegexOption.IGNORE_CASE, RegexOption.MULTILINE),
        )

        private val PAGE_NUMBER_PATTERN = Regex(
            """^(?:Page|p\.?)\s*\d+\s*$""",
            setOf(RegexOption.IGNORE_CASE, RegexOption.MULTILINE),
        )

        private val BOM_PATTERN = Regex("\uFEFF")

        private val EXCESSIVE_WHITESPACE_PATTERN = Regex("\n{3,}")

        private val BOILERPLATE_HEADER_PATTERN = Regex(
            """^(?:Raga|Tala|Composer|Language|Script|Adi Tala|Rupaka Tala)\s*[:–—-]\s*.+$""",
            setOf(RegexOption.IGNORE_CASE, RegexOption.MULTILINE),
        )
    }
}
