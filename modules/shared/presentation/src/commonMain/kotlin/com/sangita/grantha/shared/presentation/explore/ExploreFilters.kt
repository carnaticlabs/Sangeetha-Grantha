package com.sangita.grantha.shared.presentation.explore

import com.sangita.grantha.shared.domain.model.catalogue.CatalogueRagaSummaryDto
import kotlin.uuid.Uuid

enum class ExploreCategory { Krithis, Ragas, Composers }

data class ExploreFacets(
    val ragaId: Uuid? = null,
    val ragaLabel: String? = null,
    val composerId: Uuid? = null,
    val composerLabel: String? = null,
) {
    val isEmpty: Boolean get() = ragaId == null && composerId == null
}

/** M5: never treat a janya's inherited number as its own melakarta claim. */
fun ragaRelationshipCaption(
    ownMelakartaNumber: Int?,
    parentRagaName: String?,
    parentMelakartaNumber: Int?,
): String? {
    val parent = parentRagaName?.trim().orEmpty()
    if (parent.isNotEmpty()) {
        val parentMela = parentMelakartaNumber
        return if (parentMela != null) {
            "Janya of $parent (Mela $parentMela)"
        } else {
            "Janya of $parent"
        }
    }
    return ownMelakartaNumber?.let { "Melakarta $it" }
}

data class RagaDirectoryGroups(
    val melakartas: List<CatalogueRagaSummaryDto>,
    val janyas: List<CatalogueRagaSummaryDto>,
    val unclassified: List<CatalogueRagaSummaryDto>,
)

/** Missing hierarchy is not evidence of janya status (for example, alternate raganga names). */
fun groupRagas(ragas: List<CatalogueRagaSummaryDto>): RagaDirectoryGroups {
    val (melakartas, others) = ragas.partition { raga ->
        raga.melakartaNumber != null && raga.parentRagaName.isNullOrBlank()
    }
    val (janyas, unclassified) = others.partition { !it.parentRagaName.isNullOrBlank() }
    return RagaDirectoryGroups(
        melakartas = melakartas.sortedWith(compareBy({ it.melakartaNumber }, { it.name })),
        janyas = janyas,
        unclassified = unclassified,
    )
}
