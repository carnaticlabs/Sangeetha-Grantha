package com.sangita.grantha.shared.presentation.browse

import com.sangita.grantha.shared.domain.model.catalogue.CatalogueFacetKeyDto
import com.sangita.grantha.shared.presentation.RasikaCopy
import kotlin.test.Test
import kotlin.test.assertEquals

class FacetLabelTest {
    @Test
    fun serverLabelWinsOtherwiseValuesAreRenderedForReading() {
        assertEquals("Navavarana", RasikaCopy.facetLabel(CatalogueFacetKeyDto.COLLECTION, "navavarana", "Navavarana"))
        assertEquals("Prathama · nominative", RasikaCopy.facetLabel(CatalogueFacetKeyDto.VIBHAKTI, "PRATHAMA", null))
        assertEquals("Chola", RasikaCopy.facetLabel(CatalogueFacetKeyDto.MANDALAM, "CHOLA", null))
        assertEquals("Vayu", RasikaCopy.facetLabel(CatalogueFacetKeyDto.ELEMENT, "VAYU", ""))
    }
}
