package com.sangita.grantha.shared.presentation.explore

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ExploreFiltersTest {
    @Test
    fun janyaUsesParentMelakartaNeverChildNumber() {
        assertEquals(
            "Janya of Kharaharapriya (Mela 22)",
            ragaRelationshipCaption(
                ownMelakartaNumber = 22,
                parentRagaName = "Kharaharapriya",
                parentMelakartaNumber = 22,
            ),
        )
        assertEquals(
            "Janya of Dheerasankarabharanam (Mela 29)",
            ragaRelationshipCaption(
                ownMelakartaNumber = 29,
                parentRagaName = "Dheerasankarabharanam",
                parentMelakartaNumber = 29,
            ),
        )
        assertEquals(
            "Janya of Kharaharapriya",
            ragaRelationshipCaption(
                ownMelakartaNumber = 15,
                parentRagaName = "Kharaharapriya",
                parentMelakartaNumber = null,
            ),
        )
    }

    @Test
    fun ownMelakartaOnlyWhenNoParent() {
        assertEquals("Melakarta 15", ragaRelationshipCaption(15, null, null))
        assertNull(ragaRelationshipCaption(null, null, null))
        assertNull(ragaRelationshipCaption(null, "  ", 22))
    }
}
