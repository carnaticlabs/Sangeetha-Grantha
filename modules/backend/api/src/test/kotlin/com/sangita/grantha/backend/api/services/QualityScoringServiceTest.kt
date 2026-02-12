package com.sangita.grantha.backend.api.services

import com.sangita.grantha.backend.api.support.IntegrationTestBase
import com.sangita.grantha.backend.dal.SangitaDal
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.test.assertTrue

class QualityScoringServiceTest : IntegrationTestBase() {
    private lateinit var dal: SangitaDal
    private lateinit var service: IQualityScorer

    @BeforeEach
    fun setup() {
        dal = com.sangita.grantha.backend.dal.SangitaDalImpl()
        service = QualityScoringServiceImpl()
    }

    @Test
    fun `quality score returns tier and non-zero for complete data`() = runTest {
        val sourceId = dal.imports.findOrCreateSource("TestSource")
        val imported = dal.imports.createImport(
            sourceId = sourceId,
            sourceKey = "quality-1",
            rawTitle = "Test",
            rawLyrics = "Lyrics",
            rawComposer = "Composer",
            rawRaga = "Raga",
            rawTala = "Tala",
            rawDeity = null,
            rawTemple = null,
            rawLanguage = null,
            parsedPayload = null
        )

        val score = service.calculateQualityScore(imported)

        assertTrue(score.overall > 0.0)
        assertTrue(score.tier.name.isNotBlank())
    }
}
