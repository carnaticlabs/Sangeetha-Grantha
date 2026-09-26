package com.sangita.grantha.shared.presentation.explore

import com.sangita.grantha.shared.domain.model.catalogue.CatalogueRagaSummaryDto
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.uuid.Uuid

class GroupRagasTest {
    @Test
    fun melakartasSortByNumberAndJanyasStayOut() {
        val mayamalava = raga("Mayamalavagowla", melakarta = 15)
        val kharahara = raga("Kharaharapriya", melakarta = 22)
        val bowli = raga("Bowli", parent = "Mayamalavagowla", parentMela = 15)
        val bhoopalam = raga("Bhoopalam", parent = "Hanumatodi", parentMela = 8)
        val inherited = raga("Inherited", melakarta = 22, parent = "Kharaharapriya", parentMela = 22)

        val groups = groupRagas(listOf(bowli, kharahara, bhoopalam, mayamalava, inherited))

        assertEquals(listOf("Mayamalavagowla", "Kharaharapriya"), groups.melakartas.map { it.name })
        assertEquals(listOf("Bowli", "Bhoopalam", "Inherited"), groups.janyas.map { it.name })
    }

    @Test
    fun missingHierarchyDoesNotMakeSumadyutiAJanya() {
        val sumadyuti = raga("sumadyuti")
        val unknown = raga("Unclassified", parent = "  ")
        val groups = groupRagas(listOf(sumadyuti, unknown))
        assertEquals(emptyList(), groups.melakartas)
        assertEquals(emptyList(), groups.janyas)
        assertEquals(listOf(sumadyuti, unknown), groups.unclassified)
    }

    private fun raga(
        name: String,
        melakarta: Int? = null,
        parent: String? = null,
        parentMela: Int? = null,
    ) = CatalogueRagaSummaryDto(
        id = Uuid.parse("00000000-0000-4000-8000-${name.hashCode().toUInt().toString(16).padStart(12, '0').take(12)}"),
        name = name,
        publishedCompositionCount = 1,
        melakartaNumber = melakarta,
        parentRagaName = parent,
        parentMelakartaNumber = parentMela,
    )
}
