package com.sangita.grantha.backend.api.services

import com.sangita.grantha.backend.api.models.KrithiCreateRequest
import com.sangita.grantha.backend.api.support.TestDatabaseFactory
import com.sangita.grantha.backend.api.support.TestFixtures
import com.sangita.grantha.backend.dal.SangitaDal
import com.sangita.grantha.shared.domain.model.KrithiSearchRequest
import com.sangita.grantha.shared.domain.model.LanguageCodeDto
import com.sangita.grantha.shared.domain.model.MusicalFormDto
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class KrithiServiceTest {
    private lateinit var dal: SangitaDal
    private lateinit var service: IKrithiService

    @BeforeEach
    fun setup() = runTest {
        TestDatabaseFactory.connectTestDb()
        dal = com.sangita.grantha.backend.dal.SangitaDalImpl()
        service = KrithiServiceImpl(dal)
    }

    @AfterEach
    fun teardown() {
        TestDatabaseFactory.reset()
    }

    @Test
    fun `createKrithi persists and returns DTO`() = runTest {
        val seed = TestFixtures.seedReferenceData(dal)

        val created = service.createKrithi(
            KrithiCreateRequest(
                title = "Vatapi Ganapatim",
                composerId = seed.composer.id.toString(),
                musicalForm = MusicalFormDto.KRITHI,
                primaryLanguage = LanguageCodeDto.TE,
                primaryRagaId = seed.raga.id.toString(),
                talaId = seed.tala.id.toString(),
                ragaIds = listOf(seed.raga.id.toString())
            )
        )

        assertNotNull(created.id)
        assertEquals("Vatapi Ganapatim", created.title)
        assertEquals(seed.composer.id, created.composerId)
    }

    @Test
    fun `search returns paginated results`() = runTest {
        val seed = TestFixtures.seedReferenceData(dal)
        repeat(3) { idx ->
            service.createKrithi(
                KrithiCreateRequest(
                    title = "Krithi $idx",
                    composerId = seed.composer.id.toString(),
                    musicalForm = MusicalFormDto.KRITHI,
                    primaryLanguage = LanguageCodeDto.TE,
                    primaryRagaId = seed.raga.id.toString(),
                    talaId = seed.tala.id.toString(),
                    ragaIds = listOf(seed.raga.id.toString())
                )
            )
        }

        val result = service.search(KrithiSearchRequest(page = 0, pageSize = 2), publishedOnly = false)

        assertEquals(2, result.items.size)
        assertTrue(result.total >= 3)
    }
}
