package com.sangita.grantha.backend.dal.integration

import com.sangita.grantha.backend.dal.SangitaDalImpl
import com.sangita.grantha.backend.testsupport.IntegrationTestBase
import kotlin.test.assertEquals
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test

/**
 * TRACK-132: import `name_normalized` is space-stripped while the Wikipedia seed
 * keeps spaces. [com.sangita.grantha.backend.dal.repositories.RagaRepository.findOrCreate]
 * must hit the curated row instead of minting an ITRANS twin.
 */
class RagaFindOrCreateTrack132Test : IntegrationTestBase() {
    private val dal = SangitaDalImpl()

    @Test
    fun `ITRANS Yadukula Kambhoji resolves to the seeded Wikipedia row`() = runTest {
        val found = dal.ragas.findOrCreate(
            name = "yadukula kAmbhOji",
            nameNormalized = "yadukulakambhoji",
        )
        assertEquals("Yadukula Kāmbhoji", found.name)
        assertEquals("yadukula kambhoji", found.nameNormalized)
    }

    @Test
    fun `ITRANS Deva Manohari resolves to the seeded Wikipedia row`() = runTest {
        val found = dal.ragas.findOrCreate(
            name = "dEva manOhari",
            nameNormalized = "devamanohari",
        )
        assertEquals("Deva Manohari", found.name)
        assertEquals("deva manohari", found.nameNormalized)
    }
}
