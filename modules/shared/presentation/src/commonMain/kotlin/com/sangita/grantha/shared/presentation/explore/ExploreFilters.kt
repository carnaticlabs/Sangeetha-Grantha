package com.sangita.grantha.shared.presentation.explore

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
