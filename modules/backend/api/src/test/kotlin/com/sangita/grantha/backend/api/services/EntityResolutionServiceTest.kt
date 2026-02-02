package com.sangita.grantha.backend.api.services

import com.sangita.grantha.backend.api.support.TestDatabaseFactory
import com.sangita.grantha.backend.api.support.TestFixtures
import com.sangita.grantha.backend.dal.SangitaDal
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.test.assertTrue

class EntityResolutionServiceTest {
    private lateinit var dal: SangitaDal
    private lateinit var service: IEntityResolver

    @BeforeEach
    fun setup() {
        TestDatabaseFactory.connectTestDb()
        dal = com.sangita.grantha.backend.dal.SangitaDalImpl()
        service = EntityResolutionServiceImpl(dal, NameNormalizationService())
    }

    @AfterEach
    fun teardown() {
        TestDatabaseFactory.reset()
    }

    @Test
    fun `resolve returns candidates for matching reference data`() = runTest {
        val seed = TestFixtures.seedReferenceData(dal)
        val sourceId = dal.imports.findOrCreateSource("TestSource")
        val imported = dal.imports.createImport(
            sourceId = sourceId,
            sourceKey = "import-1",
            rawTitle = "Test",
            rawLyrics = null,
            rawComposer = seed.composer.name,
            rawRaga = seed.raga.name,
            rawTala = seed.tala.name,
            rawDeity = null,
            rawTemple = null,
            rawLanguage = null,
            parsedPayload = null
        )

        val result = service.resolve(imported)

        assertTrue(result.composerCandidates.isNotEmpty())
        assertTrue(result.ragaCandidates.isNotEmpty())
        assertTrue(result.talaCandidates.isNotEmpty())
    }
}
