package com.sangita.grantha.shared.mobile.fixture

import com.sangita.grantha.shared.domain.model.MusicalFormDto
import com.sangita.grantha.shared.domain.model.catalogue.CatalogueCollectionDetailDto
import com.sangita.grantha.shared.domain.model.catalogue.CatalogueCollectionListDto
import com.sangita.grantha.shared.domain.model.catalogue.CatalogueCollectionMemberDto
import com.sangita.grantha.shared.domain.model.catalogue.CatalogueCollectionNeighbourDto
import com.sangita.grantha.shared.domain.model.catalogue.CatalogueCollectionRefDto
import com.sangita.grantha.shared.domain.model.catalogue.CatalogueCollectionRoleDto
import com.sangita.grantha.shared.domain.model.catalogue.CatalogueCollectionSummaryDto
import com.sangita.grantha.shared.domain.model.catalogue.CatalogueComposerRefDto
import com.sangita.grantha.shared.domain.model.catalogue.CatalogueCycleContextDto
import com.sangita.grantha.shared.domain.model.catalogue.CatalogueKrithiSummaryDto
import com.sangita.grantha.shared.domain.model.catalogue.CatalogueOrderBasisDto
import com.sangita.grantha.shared.domain.model.catalogue.CatalogueRagaRefDto
import kotlin.uuid.Uuid

/**
 * TRACK-150 contract fixtures: Navavarana (dhyana, nine Avaranas, mangalam), Navagraha (seven core
 * plus two disputed) and Ghana-raga Pancharatnam (performance convention). Synthetic ids; titles
 * follow the reviewed seeds so presenter and UI tests read like the live catalogue.
 */
object CollectionFixtures {
    private data class Spec(
        val title: String,
        val role: CatalogueCollectionRoleDto,
        val sequence: Int,
        val axis: String? = null,
        val raga: String = "Fixture raga",
        val id: Uuid? = null,
        val attributes: Map<String, String> = emptyMap(),
    )

    private fun fixtureId(prefix: String, n: Int): Uuid =
        Uuid.parse("$prefix-0000-4000-8000-" + n.toString().padStart(12, '0'))

    private fun summary(spec: Spec, id: Uuid, composer: CatalogueComposerRefDto, form: MusicalFormDto) =
        CatalogueKrithiSummaryDto(
            id = id,
            title = spec.title,
            incipit = spec.title,
            composer = composer,
            ragas = listOf(CatalogueRagaRefDto(fixtureId("bb000000", spec.sequence + 1), spec.raga, orderIndex = 0)),
            tala = CatalogueFixtures.adiTala,
            musicalForm = form,
        )

    private class Collection(
        val id: Uuid,
        val slug: String,
        val name: String,
        val composer: CatalogueComposerRefDto,
        val basis: CatalogueOrderBasisDto,
        val convention: String?,
        val overview: String?,
        val isComplete: Boolean,
        val members: List<Pair<Spec, CatalogueKrithiSummaryDto>>,
    ) {
        val coreCount: Int = members.count { it.first.role == CatalogueCollectionRoleDto.CORE }

        fun summary() = CatalogueCollectionSummaryDto(
            id = id,
            slug = slug,
            name = name,
            composer = composer,
            memberCount = members.size,
            orderBasis = basis,
            hasOverview = overview != null,
        )

        fun detail(): CatalogueCollectionDetailDto {
            var core = 0
            return CatalogueCollectionDetailDto(
                id = id,
                slug = slug,
                name = name,
                composer = composer,
                overview = overview,
                orderBasis = basis,
                orderConvention = convention,
                isComplete = isComplete,
                coreCount = coreCount,
                members = members.map { (spec, krithi) ->
                    CatalogueCollectionMemberDto(
                        krithi = krithi,
                        role = spec.role,
                        sequenceOrder = spec.sequence,
                        position = if (spec.role == CatalogueCollectionRoleDto.CORE) ++core else null,
                        axisValue = spec.axis,
                        label = if (spec.role == CatalogueCollectionRoleDto.DISPUTED_CONJECTURE) "disputed" else null,
                        attributes = spec.attributes,
                    )
                },
            )
        }

        fun refFor(krithiId: Uuid): CatalogueCollectionRefDto? {
            val index = members.indexOfFirst { it.second.id == krithiId }
            if (index < 0) return null
            val (spec, _) = members[index]
            fun neighbour(i: Int) = members.getOrNull(i)?.second?.let {
                CatalogueCollectionNeighbourDto(it.id, it.title, it.ragas.firstOrNull()?.name)
            }
            return CatalogueCollectionRefDto(
                collectionId = id,
                slug = slug,
                name = name,
                role = spec.role,
                sequenceOrder = spec.sequence,
                position = if (spec.role == CatalogueCollectionRoleDto.CORE) {
                    members.take(index + 1).count { it.first.role == CatalogueCollectionRoleDto.CORE }
                } else {
                    null
                },
                coreCount = coreCount,
                axisValue = spec.axis,
                orderBasis = basis,
                orderConvention = convention,
                attributes = spec.attributes,
                previous = neighbour(index - 1),
                next = neighbour(index + 1),
            )
        }
    }

    private val core = CatalogueCollectionRoleDto.CORE

    private val navavarana = run {
        val chakras = listOf(
            "Trailokyamohana", "Sarvasiparipuraka", "Sarvasankshobhana", "Sarvasaubhagyadayaka", "Sarvarthasadhaka",
            "Sarvarakshakara", "Sarvarogahara", "Sarvasiddhiprada", "Sarvanandamaya",
        )
        val yoginis = listOf(
            "Prakata", "Gupta", "Guptatara", "Sampradaya", "Kulottirna", "Nigarbha", "Rahasya", "Atirahasya",
            "Parapararahasya",
        )
        val titles = listOf(
            "kamalAmbA saMrakshatu", "kamalAmbAM bhajarE", "SrI kamalAmbikayA kaTAkshitOhaM", "kamalAmbikAyai kanaka",
            "SrI kamalAmbikAyAH paraM", "kamalAmbikAyAstava", "SrI kamalAmbikAyAM bhaktiM", "SrI kamalAmbikE avAva",
            "SrI kamalAmbA jayati",
        )
        val specs = buildList {
            add(Spec("kamalAmbikE ASrita", CatalogueCollectionRoleDto.DHYANA, 0, raga = "Todi"))
            chakras.forEachIndexed { i, chakra ->
                add(
                    Spec(
                        titles[i], core, i + 1, axis = chakra, raga = "Navavarana raga ${i + 1}",
                        attributes = mapOf("avarana" to (i + 1).toString(), "yogini" to yoginis[i]),
                    ),
                )
            }
            add(Spec("SrI kamalAmbikE SivE", CatalogueCollectionRoleDto.MANGALAM, 10, raga = "Sri"))
        }
        Collection(
            id = fixtureId("c1000000", 1),
            slug = "kamalamba-navavarnam",
            name = "Kamalamba Navavarana",
            composer = CatalogueFixtures.dikshitar,
            basis = CatalogueOrderBasisDto.SOURCE,
            convention = null,
            overview = "Eleven kritis at Tiruvarur: a dhyana kriti, one for each of the nine Avaranas of the " +
                "Sri Chakra, and a closing mangalam.",
            isComplete = true,
            members = specs.mapIndexed { i, spec ->
                spec to summary(spec, fixtureId("c1100000", i + 1), CatalogueFixtures.dikshitar, MusicalFormDto.KRITHI)
            },
        )
    }

    private val navagraha = run {
        val titles = listOf(
            "sUrya mUrtE", "candraM bhaja", "angArakaM ASrayAmyahaM", "budhamASrayAmi", "bRhaspatE tArA patE",
            "SrI Sukra bhagavantaM", "divAkara tanujaM",
        )
        val specs = titles.mapIndexed { i, t -> Spec(t, core, i + 1) } +
            Spec("smarAmyahaM sadA", CatalogueCollectionRoleDto.DISPUTED_CONJECTURE, 8) +
            Spec("mahA suraM kEtuM", CatalogueCollectionRoleDto.DISPUTED_CONJECTURE, 9)
        Collection(
            id = fixtureId("c2000000", 1),
            slug = "navagraha-krithis",
            name = "Navagraha Kritis",
            composer = CatalogueFixtures.dikshitar,
            basis = CatalogueOrderBasisDto.UNSPECIFIED,
            convention = null,
            overview = null,
            isComplete = true,
            members = specs.mapIndexed { i, spec ->
                spec to summary(spec, fixtureId("c2100000", i + 1), CatalogueFixtures.dikshitar, MusicalFormDto.KRITHI)
            },
        )
    }

    private val pancharatnam = run {
        val specs = listOf(
            Spec("jagadAnanda kAraka", core, 1, raga = "Nattai"),
            Spec("duDuku gala", core, 2, raga = "Gowla"),
            Spec("sAdhincenE", core, 3, raga = "Arabhi"),
            Spec("kana kana rucirA", core, 4, raga = "Varali"),
            Spec("Endaro Mahaanubhaavulu", core, 5, raga = "Sri", id = CatalogueFixtures.endaroId),
        )
        Collection(
            id = fixtureId("c3000000", 1),
            slug = "tyagaraja-ghana-raga-pancharatnam",
            name = "Ghana-raga Pancharatnam",
            composer = CatalogueFixtures.tyagaraja,
            basis = CatalogueOrderBasisDto.PERFORMANCE_CONVENTION,
            convention = "Aradhana concert tradition (not composer sequence)",
            overview = "The five Ghana-raga kritis of Tyagaraja, sung together in a traditional performance order.",
            isComplete = true,
            members = specs.mapIndexed { i, spec ->
                spec to summary(
                    spec,
                    spec.id ?: fixtureId("c3100000", i + 1),
                    CatalogueFixtures.tyagaraja,
                    MusicalFormDto.KRITHI,
                )
            },
        )
    }

    private val all = listOf(navavarana, navagraha, pancharatnam)

    val navavaranaSlug: String = navavarana.slug
    val navagrahaSlug: String = navagraha.slug
    val pancharatnamSlug: String = pancharatnam.slug

    /** Ordered by composer then name, as the server does. */
    val list: CatalogueCollectionListDto =
        CatalogueCollectionListDto(
            all.map { it.summary() }.sortedWith(compareBy({ it.composer?.name ?: "" }, { it.name })),
        )

    fun detail(slug: String): CatalogueCollectionDetailDto? = all.firstOrNull { it.slug == slug }?.detail()

    /** Reader enrichment for a fixture kriti that belongs to one of the fixture collections. */
    fun cycleContextFor(krithiId: Uuid): CatalogueCycleContextDto? {
        val refs = all.mapNotNull { it.refFor(krithiId) }
        return refs.takeIf { it.isNotEmpty() }?.let { CatalogueCycleContextDto(collections = it) }
    }

    /** Every member id of the fixture collections, for tests that need a navigable kriti. */
    fun memberSummary(krithiId: Uuid): CatalogueKrithiSummaryDto? =
        all.firstNotNullOfOrNull { c -> c.members.firstOrNull { it.second.id == krithiId }?.second }

    val navavaranaSixthId: Uuid = navavarana.members[6].second.id
}
