package com.sangita.grantha.shared.presentation.collections

import com.sangita.grantha.shared.domain.model.catalogue.CatalogueCollectionRefDto
import com.sangita.grantha.shared.domain.model.catalogue.CatalogueCollectionRoleDto
import com.sangita.grantha.shared.domain.model.catalogue.CatalogueOrderBasisDto

/** User-facing wording for reviewed cycle collections (TRACK-150). */
object CollectionCopy {
    const val SECTION = "Collections"
    const val ALL = "All collections"
    const val SCREEN_LABEL = "COLLECTIONS"
    const val PART_OF = "PART OF"
    const val PREVIOUS = "Previous"
    const val NEXT = "Next"
    const val VIBHAKTI_KEY = "VIBHAKTI"
    const val OCCASION_KEY = "OCCASION"
    const val EMPTY_TITLE = "No collections yet"
    const val EMPTY_BODY = "Reviewed cycles appear here once their kritis are published."
    const val OPEN_SET = "An open set. This list is not claimed to be complete."
    const val UNORDERED = "Shown for reading. No fixed sequence is claimed."
    const val DISPUTED = "Attribution disputed"

    fun memberCount(count: Int): String = if (count == 1) "1 kriti" else "$count kritis"

    /** "6 of 9" is shown only when the cycle's own structure attests the position. */
    fun positionLabel(ref: CatalogueCollectionRefDto): String? =
        if (ref.orderBasis == CatalogueOrderBasisDto.SOURCE && ref.position != null) {
            "${ref.position} of ${ref.coreCount}"
        } else {
            null
        }

    fun orderNote(basis: CatalogueOrderBasisDto, convention: String?): String? = when (basis) {
        CatalogueOrderBasisDto.SOURCE -> null
        CatalogueOrderBasisDto.PERFORMANCE_CONVENTION ->
            "Traditional performance order" + convention?.takeIf { it.isNotBlank() }?.let { " · $it" }.orEmpty()
        CatalogueOrderBasisDto.UNSPECIFIED -> UNORDERED
    }

    fun roleLabel(role: CatalogueCollectionRoleDto): String? = when (role) {
        CatalogueCollectionRoleDto.CORE -> null
        CatalogueCollectionRoleDto.DHYANA -> "Dhyana · invocation"
        CatalogueCollectionRoleDto.MANGALAM -> "Mangalam · closing"
        CatalogueCollectionRoleDto.OPTIONAL -> "Optional"
        CatalogueCollectionRoleDto.DISPUTED_CONJECTURE -> DISPUTED
    }

    /** Grammatical case with a one-word gloss, since the case names are Sanskrit. */
    fun vibhakti(raw: String): String = when (raw) {
        "PRATHAMA" -> "Prathama · nominative"
        "DVITIYA" -> "Dvitiya · accusative"
        "TRITIYA" -> "Tritiya · instrumental"
        "CHATURTHI" -> "Chaturthi · dative"
        "PANCHAMI" -> "Panchami · ablative"
        "SHASHTHI" -> "Shashthi · genitive"
        "SAPTAMI" -> "Saptami · locative"
        "SAMBODHANA" -> "Sambodhana · vocative"
        "SARVA_VIBHAKTI" -> "All cases"
        else -> raw.lowercase().replace('_', ' ').replaceFirstChar { it.uppercase() }
    }

    fun chipLabel(ref: CatalogueCollectionRefDto): String =
        listOfNotNull(ref.name, positionLabel(ref)).joinToString(" · ")
}
