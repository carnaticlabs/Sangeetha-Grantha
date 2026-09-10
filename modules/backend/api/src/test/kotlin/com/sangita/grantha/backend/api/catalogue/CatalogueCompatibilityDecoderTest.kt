package com.sangita.grantha.backend.api.catalogue

import com.sangita.grantha.backend.api.services.CatalogueService
import com.sangita.grantha.shared.domain.model.MusicalFormDto
import com.sangita.grantha.shared.domain.model.catalogue.CatalogueDiscoveryDto
import com.sangita.grantha.shared.domain.model.catalogue.CatalogueKrithiSummaryDto
import io.ktor.http.parametersOf
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFails
import kotlin.test.assertIs
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * TRACK-140 M2: a frozen V1 decoder must reject UNESTABLISHED; V2 accepts it.
 * Fixture payloads are synthetic contract examples, not live catalogue records.
 */
class CatalogueCompatibilityDecoderTest {
    private val json = Json { ignoreUnknownKeys = true }

    @Serializable
    private enum class FrozenV1MusicalForm { KRITHI, VARNAM, SWARAJATHI }

    @Serializable
    private data class FrozenV1Summary(val musicalForm: FrozenV1MusicalForm)

    @Test
    fun `frozen v1 decoder accepts known-form fixture`() {
        val text = java.nio.file.Files.readString(
            locate("shared/domain/model/catalogue/fixtures/v1-known-summary.json"),
        )
        val decoded = json.decodeFromString(FrozenV1Summary.serializer(), text)
        assertEquals(FrozenV1MusicalForm.KRITHI, decoded.musicalForm)
    }

    @Test
    fun `frozen v1 decoder cannot read unestablished v2 discovery`() {
        val text = java.nio.file.Files.readString(
            locate("shared/domain/model/catalogue/fixtures/v2-unestablished-discovery.json"),
        )
        val discovery = json.decodeFromString(CatalogueDiscoveryDto.serializer(), text)
        val payload = json.encodeToString(
            CatalogueKrithiSummaryDto.serializer(),
            discovery.feature!!.krithi,
        )
        assertFails {
            json.decodeFromString(FrozenV1Summary.serializer(), payload)
        }
    }

    @Test
    fun `v2 decoder reads unestablished discovery`() {
        val text = java.nio.file.Files.readString(
            locate("shared/domain/model/catalogue/fixtures/v2-unestablished-discovery.json"),
        )
        val discovery = json.decodeFromString(CatalogueDiscoveryDto.serializer(), text)
        assertEquals(MusicalFormDto.UNESTABLISHED, discovery.feature!!.krithi.musicalForm)
        assertIs<CatalogueDiscoveryDto>(discovery)
    }

    @Test
    fun `repeated query parameter is invalid`() {
        val parsed = CatalogueParameters.parseKrithiSearch(parametersOf("query" to listOf("a", "b")))
        assertIs<com.sangita.grantha.backend.api.catalogue.CatalogueParseResult.Invalid>(parsed)
    }

    private fun locate(relative: String): java.nio.file.Path {
        var dir = java.nio.file.Path.of(System.getProperty("user.dir")).toAbsolutePath()
        while (true) {
            val candidate = dir.resolve(relative)
            if (candidate.toFile().exists()) return candidate
            dir = dir.parent ?: error("missing $relative")
        }
    }
}
