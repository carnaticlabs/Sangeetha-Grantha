package com.sangita.grantha.shared.mobile.fixture

import com.sangita.grantha.shared.domain.model.LanguageCodeDto
import com.sangita.grantha.shared.domain.model.MusicalFormDto
import com.sangita.grantha.shared.domain.model.ScriptCodeDto
import com.sangita.grantha.shared.domain.model.catalogue.CatalogueCompletenessDto
import com.sangita.grantha.shared.domain.model.catalogue.CatalogueComposerDetailDto
import com.sangita.grantha.shared.domain.model.catalogue.CatalogueComposerRefDto
import com.sangita.grantha.shared.domain.model.catalogue.CatalogueComposerSummaryDto
import com.sangita.grantha.shared.domain.model.catalogue.CatalogueKrithiReaderDto
import com.sangita.grantha.shared.domain.model.catalogue.CatalogueKrithiSummaryDto
import com.sangita.grantha.shared.domain.model.catalogue.CatalogueLyricSectionDto
import com.sangita.grantha.shared.domain.model.catalogue.CatalogueLyricsDto
import com.sangita.grantha.shared.domain.model.catalogue.CataloguePagedResponse
import com.sangita.grantha.shared.domain.model.catalogue.CatalogueRagaDetailDto
import com.sangita.grantha.shared.domain.model.catalogue.CatalogueRagaRefDto
import com.sangita.grantha.shared.domain.model.catalogue.CatalogueRagaSummaryDto
import com.sangita.grantha.shared.domain.model.catalogue.CatalogueTalaRefDto
import com.sangita.grantha.shared.domain.model.catalogue.CatalogueVariantRefDto
import com.sangita.grantha.shared.mobile.network.CatalogueApi
import com.sangita.grantha.shared.mobile.network.CatalogueFailure
import com.sangita.grantha.shared.mobile.usage.InteractionContext
import kotlin.uuid.Uuid

/**
 * Contract-shaped fixtures for UI and client tests. Names are well-known
 * compositions used as synthetic samples — they are not a live corpus dump.
 */
object CatalogueFixtures {
    val dikshitarId: Uuid = Uuid.parse("11111111-1111-4111-8111-111111111111")
    val tyagarajaId: Uuid = Uuid.parse("22222222-2222-4222-8222-222222222222")
    val hamsadhvaniId: Uuid = Uuid.parse("33333333-3333-4333-8333-333333333333")
    val sriId: Uuid = Uuid.parse("44444444-4444-4444-8444-444444444444")
    val adiTalaId: Uuid = Uuid.parse("55555555-5555-4555-8555-555555555555")
    val vatapiId: Uuid = Uuid.parse("66666666-6666-4666-8666-666666666666")
    val endaroId: Uuid = Uuid.parse("77777777-7777-4777-8777-777777777777")
    val vatapiLatinId: Uuid = Uuid.parse("88888888-8888-4888-8888-888888888888")
    val vatapiDevanagariId: Uuid = Uuid.parse("99999999-9999-4999-8999-999999999999")
    val endaroLatinId: Uuid = Uuid.parse("aaaaaaaa-aaaa-4aaa-8aaa-aaaaaaaaaaaa")
    val vatapiPallaviId: Uuid = Uuid.parse("bbbbbbbb-bbbb-4bbb-8bbb-bbbbbbbbbbbb")
    val vatapiAnupallaviId: Uuid = Uuid.parse("cccccccc-cccc-4ccc-8ccc-cccccccccccc")

    val dikshitar = CatalogueComposerRefDto(dikshitarId, "Muttusvami Dikshitar")
    val tyagaraja = CatalogueComposerRefDto(tyagarajaId, "Tyagaraja")
    val adiTala = CatalogueTalaRefDto(adiTalaId, "Adi")
    val hamsadhvani = CatalogueRagaRefDto(hamsadhvaniId, "Hamsadhvani", orderIndex = 0)
    val sri = CatalogueRagaRefDto(sriId, "Sri", orderIndex = 0)

    val vatapiSummary = CatalogueKrithiSummaryDto(
        id = vatapiId,
        title = "Vatapi Ganapatim",
        incipit = "Vatapi Ganapatim Bhajeham",
        composer = dikshitar,
        ragas = listOf(hamsadhvani),
        tala = adiTala,
        musicalForm = MusicalFormDto.KRITHI,
    )

    val endaroSummary = CatalogueKrithiSummaryDto(
        id = endaroId,
        title = "Endaro Mahanubhavulu",
        incipit = "Endaro Mahanubhavulu",
        composer = tyagaraja,
        ragas = listOf(sri),
        tala = adiTala,
        musicalForm = MusicalFormDto.KRITHI,
    )

    val summaries = listOf(vatapiSummary, endaroSummary)

    val vatapiLatin = CatalogueVariantRefDto(
        id = vatapiLatinId,
        language = LanguageCodeDto.SA,
        script = ScriptCodeDto.LATIN,
        isPrimary = true,
        label = "Latin (primary)",
        sourceReference = "Fixture",
    )

    val vatapiDevanagari = CatalogueVariantRefDto(
        id = vatapiDevanagariId,
        language = LanguageCodeDto.SA,
        script = ScriptCodeDto.DEVANAGARI,
        isPrimary = false,
        label = "Devanagari",
        sourceReference = "Fixture",
    )

    val vatapiReader = CatalogueKrithiReaderDto(
        id = vatapiId,
        title = vatapiSummary.title,
        incipit = vatapiSummary.incipit,
        composer = dikshitar,
        ragas = listOf(hamsadhvani),
        tala = adiTala,
        musicalForm = MusicalFormDto.KRITHI,
        originalLanguage = LanguageCodeDto.SA,
        defaultVariantId = vatapiLatinId,
        variants = listOf(vatapiLatin, vatapiDevanagari),
        completeness = CatalogueCompletenessDto.PARTIAL,
    )

    val endaroReader = CatalogueKrithiReaderDto(
        id = endaroId,
        title = endaroSummary.title,
        incipit = endaroSummary.incipit,
        composer = tyagaraja,
        ragas = listOf(sri),
        tala = adiTala,
        musicalForm = MusicalFormDto.KRITHI,
        originalLanguage = LanguageCodeDto.SA,
        defaultVariantId = endaroLatinId,
        variants = listOf(
            CatalogueVariantRefDto(
                id = endaroLatinId,
                language = LanguageCodeDto.SA,
                script = ScriptCodeDto.LATIN,
                isPrimary = true,
                label = "Latin (primary)",
            ),
        ),
        completeness = CatalogueCompletenessDto.UNKNOWN,
    )

    fun vatapiLyrics(variantId: Uuid): CatalogueLyricsDto {
        val variant = vatapiReader.variants.first { it.id == variantId }
        val pallavi = if (variantId == vatapiDevanagariId) {
            "वातापि गणपतिं भजेहं"
        } else {
            "Vatapi Ganapatim Bhajeham"
        }
        return CatalogueLyricsDto(
            variantId = variant.id,
            krithiId = vatapiId,
            language = variant.language,
            script = variant.script,
            isPrimary = variant.isPrimary,
            label = variant.label,
            sourceReference = variant.sourceReference,
            sections = listOf(
                CatalogueLyricSectionDto(vatapiPallaviId, "PALLAVI", "Pallavi", 0, pallavi),
                CatalogueLyricSectionDto(
                    vatapiAnupallaviId,
                    "ANUPALLAVI",
                    "Anupallavi",
                    1,
                    if (variantId == vatapiDevanagariId) "प्रणवस्वरूपं वक्रतुण्डं" else "Pranava svarupam vakratundam",
                ),
            ),
        )
    }

    val ragaSummaries = listOf(
        CatalogueRagaSummaryDto(hamsadhvaniId, "Hamsadhvani", publishedCompositionCount = 1, melakartaNumber = 29),
        CatalogueRagaSummaryDto(sriId, "Sri", publishedCompositionCount = 1, parentRagaName = "Kharaharapriya"),
    )

    val composerSummaries = listOf(
        CatalogueComposerSummaryDto(dikshitarId, "Muttusvami Dikshitar", publishedCompositionCount = 1),
        CatalogueComposerSummaryDto(tyagarajaId, "Tyagaraja", publishedCompositionCount = 1),
    )
}

/**
 * Contract fixtures for JVM presenter/client tests and explicit offline UI review.
 * Production Android/iOS hosts use [com.sangita.grantha.shared.mobile.network.KtorCatalogueApi].
 */
class FixtureCatalogueApi : CatalogueApi {
    override suspend fun searchKrithis(
        query: String?,
        composerId: Uuid?,
        ragaId: Uuid?,
        page: Int,
        pageSize: Int,
        interaction: InteractionContext,
    ): CataloguePagedResponse<CatalogueKrithiSummaryDto> {
        val needle = query?.trim()?.lowercase().orEmpty()
        val filtered = CatalogueFixtures.summaries.filter { summary ->
            val matchesQuery = needle.isEmpty() ||
                summary.title.lowercase().contains(needle) ||
                (summary.incipit?.lowercase()?.contains(needle) == true)
            val matchesComposer = composerId == null || summary.composer.id == composerId
            val matchesRaga = ragaId == null || summary.ragas.any { it.id == ragaId }
            matchesQuery && matchesComposer && matchesRaga
        }
        val from = page * pageSize
        val slice = if (from >= filtered.size) emptyList() else filtered.drop(from).take(pageSize)
        return CataloguePagedResponse(slice, filtered.size.toLong(), page, pageSize)
    }

    override suspend fun getKrithi(id: Uuid, interaction: InteractionContext): CatalogueKrithiReaderDto =
        when (id) {
            CatalogueFixtures.vatapiId -> CatalogueFixtures.vatapiReader
            CatalogueFixtures.endaroId -> CatalogueFixtures.endaroReader
            else -> throw CatalogueFailure.NotFound()
        }

    override suspend fun getLyrics(
        krithiId: Uuid,
        variantId: Uuid,
        interaction: InteractionContext,
    ): CatalogueLyricsDto {
        if (krithiId != CatalogueFixtures.vatapiId) {
            if (krithiId == CatalogueFixtures.endaroId && variantId == CatalogueFixtures.endaroLatinId) {
                return CatalogueLyricsDto(
                    variantId = variantId,
                    krithiId = krithiId,
                    language = LanguageCodeDto.SA,
                    script = ScriptCodeDto.LATIN,
                    isPrimary = true,
                    label = "Latin (primary)",
                    unsegmentedText = "Endaro mahanubhavulu andariki vandanamu",
                    sections = emptyList(),
                )
            }
            throw CatalogueFailure.NotFound()
        }
        return CatalogueFixtures.vatapiLyrics(variantId)
    }

    override suspend fun searchRagas(
        query: String?,
        page: Int,
        pageSize: Int,
        interaction: InteractionContext,
    ): CataloguePagedResponse<CatalogueRagaSummaryDto> =
        pageOf(CatalogueFixtures.ragaSummaries.filterQuery(query) { it.name }, page, pageSize)

    override suspend fun getRaga(id: Uuid, interaction: InteractionContext): CatalogueRagaDetailDto {
        val summary = CatalogueFixtures.ragaSummaries.find { it.id == id } ?: throw CatalogueFailure.NotFound()
        return CatalogueRagaDetailDto(
            id = summary.id,
            name = summary.name,
            aliases = summary.matchingAliases,
            publishedCompositionCount = summary.publishedCompositionCount,
            melakartaNumber = summary.melakartaNumber,
            parentRagaName = summary.parentRagaName,
            arohanam = if (id == CatalogueFixtures.hamsadhvaniId) "S R2 G3 P N3 S" else null,
            avarohanam = if (id == CatalogueFixtures.hamsadhvaniId) "S N3 P G3 R2 S" else null,
        )
    }

    override suspend fun searchComposers(
        query: String?,
        page: Int,
        pageSize: Int,
        interaction: InteractionContext,
    ): CataloguePagedResponse<CatalogueComposerSummaryDto> =
        pageOf(CatalogueFixtures.composerSummaries.filterQuery(query) { it.name }, page, pageSize)

    override suspend fun getComposer(id: Uuid, interaction: InteractionContext): CatalogueComposerDetailDto {
        val summary = CatalogueFixtures.composerSummaries.find { it.id == id } ?: throw CatalogueFailure.NotFound()
        return CatalogueComposerDetailDto(
            id = summary.id,
            name = summary.name,
            publishedCompositionCount = summary.publishedCompositionCount,
        )
    }

    private fun <T> pageOf(
        items: List<T>,
        page: Int,
        pageSize: Int,
    ): CataloguePagedResponse<T> {
        val from = page * pageSize
        val slice = if (from >= items.size) emptyList() else items.drop(from).take(pageSize)
        return CataloguePagedResponse(slice, items.size.toLong(), page, pageSize)
    }

    private fun <T> List<T>.filterQuery(query: String?, name: (T) -> String): List<T> {
        val needle = query?.trim()?.lowercase().orEmpty()
        if (needle.isEmpty()) return this
        return filter { name(it).lowercase().contains(needle) }
    }
}
