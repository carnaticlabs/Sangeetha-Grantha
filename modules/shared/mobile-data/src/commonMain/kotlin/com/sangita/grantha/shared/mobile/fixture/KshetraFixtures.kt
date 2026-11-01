package com.sangita.grantha.shared.mobile.fixture

import com.sangita.grantha.shared.domain.model.MusicalFormDto
import com.sangita.grantha.shared.domain.model.catalogue.CatalogueDeityPostureDto
import com.sangita.grantha.shared.domain.model.catalogue.CatalogueKrithiSummaryDto
import com.sangita.grantha.shared.domain.model.catalogue.CatalogueMandalamDto
import com.sangita.grantha.shared.domain.model.catalogue.CataloguePlaceKindDto
import com.sangita.grantha.shared.domain.model.catalogue.CatalogueRagaRefDto
import com.sangita.grantha.shared.domain.model.catalogue.CatalogueReferenceDto
import com.sangita.grantha.shared.domain.model.catalogue.CatalogueTempleDetailDto
import com.sangita.grantha.shared.domain.model.catalogue.CatalogueTempleElementDto
import com.sangita.grantha.shared.domain.model.catalogue.CatalogueTempleListDto
import com.sangita.grantha.shared.domain.model.catalogue.CatalogueTemplePlaceDto
import com.sangita.grantha.shared.domain.model.catalogue.CatalogueTemplePlacePrecisionDto
import com.sangita.grantha.shared.domain.model.catalogue.CatalogueTempleSummaryDto
import kotlin.uuid.Uuid

/**
 * TRACK-151 contract fixtures: Tiruvarur (a complex with nested shrines and direct kritis), two
 * Pancha Bhuta temples with an element, Kanyakumari as a recorded gap and a root temple with no
 * mandalam. Synthetic ids and place ids; names follow the reviewed seeds. `place` is populated on
 * Tiruvarur (own place) and the Kamalamba shrine (inherited, `PARENT_COMPLEX`) so Phase 3 has both.
 */
object KshetraFixtures {
    private fun templeId(n: Int): Uuid =
        Uuid.parse("01930000-0000-7000-8000-" + n.toString().padStart(12, '0'))

    val tiruvarurId: Uuid = CatalogueFixtures.TIRUVARUR_TEMPLE_ID
    val kamalambaShrineId: Uuid = templeId(2)
    val nilotpalambaShrineId: Uuid = templeId(3)
    val panchaLingaMandapamId: Uuid = templeId(4)
    val chidambaramId: Uuid = templeId(5)
    val tiruvannamalaiId: Uuid = templeId(6)
    val noMandalamId: Uuid = templeId(7)
    val kanyakumariId: Uuid = CatalogueFixtures.KANYAKUMARI_TEMPLE_ID

    /** An id no fixture knows, for 404 paths. */
    val unknownId: Uuid = templeId(99)

    private fun kriti(n: Int, title: String, raga: String) = CatalogueKrithiSummaryDto(
        id = Uuid.parse("e1000000-0000-4000-8000-" + n.toString().padStart(12, '0')),
        title = title,
        incipit = title,
        composer = CatalogueFixtures.dikshitar,
        ragas = listOf(CatalogueRagaRefDto(Uuid.parse("e2000000-0000-4000-8000-" + n.toString().padStart(12, '0')), raga, orderIndex = 0)),
        tala = CatalogueFixtures.adiTala,
        musicalForm = MusicalFormDto.KRITHI,
    )

    val chidambaramKriti = kriti(1, "AnandanaTana prakASam", "Kedaram")
    val tiruvannamalaiKriti = kriti(2, "aruNAcalanAtham", "Saranga")

    private val tiruvarurKritis = listOf(CatalogueFixtures.vatapiSummary)
    private val kamalambaKritis = listOfNotNull(CollectionFixtures.memberSummary(CollectionFixtures.navavaranaSixthId))

    private fun summary(detail: CatalogueTempleDetailDto) = CatalogueTempleSummaryDto(
        id = detail.id,
        name = detail.name,
        city = detail.city,
        state = detail.state,
        placeKind = detail.placeKind,
        mandalam = detail.mandalam,
        element = detail.element,
        publishedKrithiCount = detail.publishedKrithiCount,
        childCount = detail.childCount,
        recordedGap = detail.recordedGap,
    )

    private val kamalambaShrine = CatalogueTempleDetailDto(
        id = kamalambaShrineId,
        name = "Kamalamba Sannidhi",
        city = "Tiruvarur",
        state = "Tamil Nadu",
        country = "India",
        placeKind = CataloguePlaceKindDto.SANNIDHI,
        mandalam = CatalogueMandalamDto.CHOLA,
        publishedKrithiCount = kamalambaKritis.size.toLong(),
        parent = CatalogueReferenceDto(tiruvarurId, "Tyagaraja Temple, Tiruvarur"),
        krithis = kamalambaKritis,
        place = CatalogueTemplePlaceDto("FIXTURE_PLACE_TIRUVARUR", CatalogueTemplePlacePrecisionDto.PARENT_COMPLEX),
    )

    private val nilotpalambaShrine = CatalogueTempleDetailDto(
        id = nilotpalambaShrineId,
        name = "Nilotpalamba Sannidhi",
        city = "Tiruvarur",
        state = "Tamil Nadu",
        country = "India",
        placeKind = CataloguePlaceKindDto.SANNIDHI,
        mandalam = CatalogueMandalamDto.CHOLA,
        publishedKrithiCount = 0,
        parent = CatalogueReferenceDto(tiruvarurId, "Tyagaraja Temple, Tiruvarur"),
        overview = "A shrine within the Tiruvarur complex, recorded for its place in the hierarchy.",
    )

    private val panchaLingaMandapam = CatalogueTempleDetailDto(
        id = panchaLingaMandapamId,
        name = "Pancha Linga Mandapam",
        city = "Tiruvarur",
        state = "Tamil Nadu",
        country = "India",
        placeKind = CataloguePlaceKindDto.MANDAPAM,
        mandalam = CatalogueMandalamDto.CHOLA,
        parent = CatalogueReferenceDto(tiruvarurId, "Tyagaraja Temple, Tiruvarur"),
        overview = "A mandapam of five lingams within the Tiruvarur complex.",
    )

    private val children = listOf(kamalambaShrine, nilotpalambaShrine, panchaLingaMandapam)

    private val tiruvarur = CatalogueTempleDetailDto(
        id = tiruvarurId,
        name = "Tyagaraja Temple, Tiruvarur",
        city = "Tiruvarur",
        state = "Tamil Nadu",
        country = "India",
        placeKind = CataloguePlaceKindDto.COMPLEX,
        mandalam = CatalogueMandalamDto.CHOLA,
        element = CatalogueTempleElementDto.PRITHVI,
        publishedKrithiCount = tiruvarurKritis.size.toLong(),
        childCount = children.size,
        sthalaVriksha = "Pathiri",
        sthalaTirtha = "Kamalalayam",
        deityPosture = CatalogueDeityPostureDto.ASINA,
        overview = "The Tiruvarur complex holds the Tyagaraja shrine, the Kamalamba and Nilotpalamba " +
            "sannidhis and the Pancha Linga mandapam.",
        children = children.map(::summary),
        krithis = tiruvarurKritis,
        place = CatalogueTemplePlaceDto("FIXTURE_PLACE_TIRUVARUR", CatalogueTemplePlacePrecisionDto.TEMPLE),
    )

    private val chidambaram = CatalogueTempleDetailDto(
        id = chidambaramId,
        name = "Thillai Nataraja Temple, Chidambaram",
        city = "Chidambaram",
        state = "Tamil Nadu",
        country = "India",
        placeKind = CataloguePlaceKindDto.LOCALITY,
        mandalam = CatalogueMandalamDto.CHOLA,
        element = CatalogueTempleElementDto.AKASHA,
        publishedKrithiCount = 1,
        sthalaVriksha = "Thillai",
        sthalaTirtha = "Sivaganga",
        deityPosture = CatalogueDeityPostureDto.TANDAVA,
        overview = "The akasha kshetra among the Pancha Bhuta temples.",
        krithis = listOf(chidambaramKriti),
    )

    private val tiruvannamalai = CatalogueTempleDetailDto(
        id = tiruvannamalaiId,
        name = "Arunachaleswarar Temple, Tiruvannamalai",
        city = "Tiruvannamalai",
        state = "Tamil Nadu",
        country = "India",
        placeKind = CataloguePlaceKindDto.LOCALITY,
        mandalam = CatalogueMandalamDto.TONDAI,
        element = CatalogueTempleElementDto.AGNI,
        publishedKrithiCount = 1,
        krithis = listOf(tiruvannamalaiKriti),
    )

    private val kanyakumari = CatalogueTempleDetailDto(
        id = kanyakumariId,
        name = "Bhagavati Amman Temple",
        city = "Kanyakumari",
        state = "Tamil Nadu",
        country = "India",
        placeKind = CataloguePlaceKindDto.LOCALITY,
        mandalam = CatalogueMandalamDto.PANDYA,
        recordedGap = true,
        overview = "No Dikshitar kriti is recorded at Kanyakumari. The gap is documented, not an omission.",
    )

    private val noMandalam = CatalogueTempleDetailDto(
        id = noMandalamId,
        name = "Fixture shrine without a mandalam",
        city = "Fixtureville",
        placeKind = CataloguePlaceKindDto.LOCALITY,
        publishedKrithiCount = 1,
        krithis = listOf(CatalogueFixtures.vatapiSummary),
    )

    private val roots = listOf(tiruvarur, chidambaram, tiruvannamalai, kanyakumari, noMandalam)
    private val all = roots + children

    /** Root temples only, ordered by mandalam (declaration order, nulls last), then name, as the server does. */
    val list: CatalogueTempleListDto = CatalogueTempleListDto(
        roots.map(::summary).sortedWith(
            compareBy<CatalogueTempleSummaryDto> { it.mandalam?.ordinal ?: Int.MAX_VALUE }.thenBy { it.name },
        ),
    )

    fun detail(id: Uuid): CatalogueTempleDetailDto? = all.firstOrNull { it.id == id }
}
