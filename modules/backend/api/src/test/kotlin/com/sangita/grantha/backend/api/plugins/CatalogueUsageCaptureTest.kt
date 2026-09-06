package com.sangita.grantha.backend.api.plugins

import com.sangita.grantha.shared.domain.model.catalogue.CatalogueKrithiSummaryDto
import com.sangita.grantha.shared.domain.model.catalogue.CataloguePagedResponse
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class CatalogueUsageCaptureTest {
    @Test
    fun `paged catalogue payloads expose total including zero`() {
        val empty = CataloguePagedResponse<CatalogueKrithiSummaryDto>(emptyList(), 0, 0, 30)
        val hits = CataloguePagedResponse<CatalogueKrithiSummaryDto>(emptyList(), 4, 0, 30)
        assertEquals(0, resultCountFromPayload(empty))
        assertEquals(4, resultCountFromPayload(hits))
        assertNull(resultCountFromPayload("reader-dto"))
    }
}
