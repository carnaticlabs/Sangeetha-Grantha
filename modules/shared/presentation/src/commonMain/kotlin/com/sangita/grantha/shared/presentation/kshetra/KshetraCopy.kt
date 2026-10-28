package com.sangita.grantha.shared.presentation.kshetra

import com.sangita.grantha.shared.domain.model.catalogue.CatalogueDeityPostureDto
import com.sangita.grantha.shared.domain.model.catalogue.CatalogueMandalamDto
import com.sangita.grantha.shared.domain.model.catalogue.CataloguePlaceKindDto
import com.sangita.grantha.shared.domain.model.catalogue.CatalogueTempleElementDto
import com.sangita.grantha.shared.domain.model.catalogue.CatalogueTempleSummaryDto

/**
 * User-facing wording for kshetras (TRACK-151). Coverage differs by composer, so no line here claims
 * the list is every kshetra of Carnatic music, and a kriti with no temple is never called "no kshetra".
 */
object KshetraCopy {
    const val SECTION = "Kshetras"
    const val SCREEN_LABEL = "KSHETRAS"
    const val ALL = "All kshetras"
    const val SHELF_BODY = "Sacred places in the catalogue, and the kritis sung there."
    const val LIST_INTRO = "Places recorded in the catalogue, grouped by region. Only places with published kritis or a reviewed note appear."
    const val OTHER_PLACES = "Other places"
    const val ELEMENT_FILTER = "ELEMENT"
    const val ELEMENT_ALL = "All"
    const val EMPTY_TITLE = "No kshetras yet"
    const val EMPTY_BODY = "Places appear here once their kritis or notes are published."
    const val FILTER_EMPTY_TITLE = "No kshetra with that element"
    const val FILTER_EMPTY_BODY = "Clear the element filter to see every place."
    const val CLEAR_FILTER = "Clear filter"
    const val RECORDED_GAP = "Recorded gap"
    const val GAP_NOTE = "No kriti is recorded here. The reason from the catalogue:"
    const val ABOUT = "ABOUT"
    const val SHRINES_HEADING = "SHRINES HERE"
    const val KRITIS_HEADING = "SUNG HERE"
    const val PART_OF = "PART OF"
    const val REGION = "REGION"
    const val ELEMENT = "ELEMENT"
    const val STHALA_VRIKSHA = "STHALA VRIKSHA"
    const val STHALA_TIRTHA = "TIRTHA"
    const val NADI_TIRTHA = "NADI TIRTHA"
    const val POSTURE = "DEITY"
    const val TEMPLE_LABEL_PREFIX = "Sung at"

    fun kritiCount(count: Long): String = if (count == 1L) "1 kriti" else "$count kritis"

    fun shrineCount(count: Int): String = if (count == 1) "1 shrine" else "$count shrines"

    /** Plain names with a short gloss; [CatalogueMandalamDto.NADU] has no gloss because none is settled. */
    fun mandalam(value: CatalogueMandalamDto?): String = when (value) {
        CatalogueMandalamDto.CHOLA -> "Chola Nadu · the Kaveri delta"
        CatalogueMandalamDto.PANDYA -> "Pandya Nadu · the far south"
        CatalogueMandalamDto.TONDAI -> "Tondai Nadu · the northern plain"
        CatalogueMandalamDto.NADU -> "Nadu Desam"
        CatalogueMandalamDto.CHERA -> "Chera Nadu · the west coast"
        CatalogueMandalamDto.KONGU -> "Kongu Nadu · the western uplands"
        CatalogueMandalamDto.UTTARA -> "Uttara · northern places"
        null -> OTHER_PLACES
    }

    /** The bhuta, glossed; `APPU` is water. */
    fun element(value: CatalogueTempleElementDto): String = when (value) {
        CatalogueTempleElementDto.AKASHA -> "Akasha · space"
        CatalogueTempleElementDto.VAYU -> "Vayu · air"
        CatalogueTempleElementDto.AGNI -> "Agni · fire"
        CatalogueTempleElementDto.PRITHVI -> "Prithvi · earth"
        CatalogueTempleElementDto.APPU -> "Appu · water"
    }

    fun posture(value: CatalogueDeityPostureDto): String = when (value) {
        CatalogueDeityPostureDto.SAYANA -> "Sayana · reclining"
        CatalogueDeityPostureDto.STHANAKA -> "Sthanaka · standing"
        CatalogueDeityPostureDto.ASINA -> "Asina · seated"
        CatalogueDeityPostureDto.TANDAVA -> "Tandava · dancing"
    }

    fun placeKind(value: CataloguePlaceKindDto): String? = when (value) {
        CataloguePlaceKindDto.LOCALITY -> null
        CataloguePlaceKindDto.COMPLEX -> "Temple complex"
        CataloguePlaceKindDto.SANNIDHI -> "Sannidhi"
        CataloguePlaceKindDto.MANDAPAM -> "Mandapam"
    }

    fun place(city: String?, state: String?): String? =
        listOfNotNull(city, state).filter { it.isNotBlank() }.joinToString(", ").ifBlank { null }

    /** Row sub line: place, kriti count (or the gap marker as text) and shrine count. */
    fun rowSubtitle(item: CatalogueTempleSummaryDto): String = listOfNotNull(
        place(item.city, item.state),
        if (item.recordedGap) RECORDED_GAP else item.publishedKrithiCount.takeIf { it > 0 }?.let(::kritiCount),
        item.childCount.takeIf { it > 0 }?.let(::shrineCount),
    ).joinToString(" · ")

    fun rowDescription(item: CatalogueTempleSummaryDto): String =
        listOf(item.name, rowSubtitle(item)).filter { it.isNotBlank() }.joinToString(", ")

    fun templeChipLabel(name: String): String = "$TEMPLE_LABEL_PREFIX $name"
}
