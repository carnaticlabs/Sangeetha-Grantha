package com.sangita.grantha.shared.presentation.reader

import com.sangita.grantha.shared.domain.model.MusicalFormDto
import com.sangita.grantha.shared.domain.model.catalogue.CatalogueRagaRefDto
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.uuid.Uuid

class SectionLabelsTest {
    @Test
    fun storedLabelWinsOverFallback() {
        assertEquals("Pallavi of the charanam", SectionLabels.heading("CHARANAM", "Pallavi of the charanam"))
        assertEquals("Charanam", SectionLabels.heading("CHARANAM", null))
        assertEquals("Muktayi Swaram", SectionLabels.heading("MUKTAYI_SWARA", "  "))
        assertEquals("Madhyamakala", SectionLabels.heading("MADHYAMA_KALA", null))
        assertEquals("Ettugada Swaram", SectionLabels.heading("ETTUGADA_SWARA", null))
        assertEquals("UNKNOWN_TYPE", SectionLabels.heading("UNKNOWN_TYPE", null))
    }

    @Test
    fun varnamCharanamCaptionDoesNotRenameStoredLabel() {
        assertEquals(
            "pallavi of the charanam",
            SectionLabels.caption("CHARANAM", MusicalFormDto.VARNAM),
        )
        assertEquals("Charanam", SectionLabels.heading("CHARANAM", "Charanam"))
        assertNull(SectionLabels.caption("CHARANAM", MusicalFormDto.KRITHI))
    }

    @Test
    fun swaraSahityaAndMadhyamakalaAreNotPreformatted() {
        assertFalse(SectionLabels.isPreformattedSwara("SWARA_SAHITYA"))
        assertFalse(SectionLabels.isPreformattedSwara("MADHYAMA_KALA"))
        assertTrue(SectionLabels.isPreformattedSwara("MUKTAYI_SWARA"))
        assertTrue(SectionLabels.isPreformattedSwara("SOLKATTU_SWARA"))
    }

    @Test
    fun sectionRagaLabelRequiresExplicitSectionId() {
        val pallavi = Uuid.parse("bbbbbbbb-bbbb-4bbb-8bbb-bbbbbbbbbbbb")
        val charanam = Uuid.parse("cccccccc-cccc-4ccc-8ccc-cccccccccccc")
        val ragas = listOf(
            CatalogueRagaRefDto(
                id = Uuid.parse("33333333-3333-4333-8333-333333333333"),
                name = "Todi",
                orderIndex = 0,
                sectionId = pallavi,
            ),
            CatalogueRagaRefDto(
                id = Uuid.parse("44444444-4444-4444-8444-444444444444"),
                name = "Kalyani",
                orderIndex = 1,
            ),
        )
        assertEquals("Todi", SectionLabels.ragaNamesForSection(ragas, pallavi))
        assertNull(SectionLabels.ragaNamesForSection(ragas, charanam))
    }
}
