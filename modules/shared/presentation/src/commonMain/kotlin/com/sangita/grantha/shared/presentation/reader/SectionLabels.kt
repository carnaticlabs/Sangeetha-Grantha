package com.sangita.grantha.shared.presentation.reader

import com.sangita.grantha.shared.domain.model.MusicalFormDto
import com.sangita.grantha.shared.domain.model.catalogue.CatalogueRagaRefDto
import com.sangita.grantha.shared.presentation.RasikaCopy
import kotlin.uuid.Uuid

/**
 * Stored labels win. Fallbacks never title-case the enum token (M4).
 * SWARA_SAHITYA and MADHYAMA_KALA are not treated as notation (M3).
 */
object SectionLabels {
    fun heading(sectionType: String, storedLabel: String?): String {
        val trimmed = storedLabel?.trim().orEmpty()
        if (trimmed.isNotEmpty()) return trimmed
        return when (sectionType) {
            "PALLAVI" -> "Pallavi"
            "ANUPALLAVI" -> "Anupallavi"
            "CHARANAM" -> "Charanam"
            "SAMASHTI_CHARANAM" -> "Samashti Charanam"
            "CHITTASWARAM" -> "Chittaswaram"
            "SWARA_SAHITYA" -> "Swara Sahitya"
            "MADHYAMA_KALA" -> "Madhyamakala"
            "SOLKATTU_SWARA" -> "Solkattu Swaram"
            "ANUBANDHA" -> "Anubandha"
            "MUKTAYI_SWARA" -> "Muktayi Swaram"
            "ETTUGADA_SWARA" -> "Ettugada Swaram"
            "ETTUGADA_SAHITYA" -> "Ettugada Sahitya"
            "VILOMA_CHITTASWARAM" -> "Viloma Chittaswaram"
            else -> sectionType
        }
    }

    fun caption(sectionType: String, musicalForm: MusicalFormDto): String? =
        if (musicalForm == MusicalFormDto.VARNAM && sectionType == "CHARANAM") {
            RasikaCopy.VARNAM_CHARANAM_CAPTION
        } else {
            null
        }

    fun isPreformattedSwara(sectionType: String): Boolean = when (sectionType) {
        "MUKTAYI_SWARA", "SOLKATTU_SWARA", "ETTUGADA_SWARA",
        "CHITTASWARAM", "VILOMA_CHITTASWARAM",
        -> true
        else -> false
    }

    /** M1: only an explicit sectionId binding may label a lyric section. */
    fun ragaNamesForSection(ragas: List<CatalogueRagaRefDto>, sectionId: Uuid): String? {
        val bound = ragas.filter { it.sectionId == sectionId }.sortedBy { it.orderIndex }
        if (bound.isEmpty()) return null
        return bound.joinToString(" · ") { it.name }
    }
}
