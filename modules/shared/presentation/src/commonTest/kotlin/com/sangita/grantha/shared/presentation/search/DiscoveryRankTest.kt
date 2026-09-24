package com.sangita.grantha.shared.presentation.search

import com.sangita.grantha.shared.presentation.components.hybridRankLabel
import com.sangita.grantha.shared.presentation.components.semanticRankLabel
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class DiscoveryRankTest {
    @Test
    fun hybridRrfIsFourDecimalsAndOmittedWhenNull() {
        assertEquals("0.0325", hybridRankLabel(0.0325))
        assertEquals("0.0164", hybridRankLabel(0.0164))
        assertNull(hybridRankLabel(null))
    }

    @Test
    fun semanticPercentIsHalfUpWithNoOtherWord() {
        assertEquals("87%", semanticRankLabel(0.874))
        assertEquals("88%", semanticRankLabel(0.875))
        assertEquals("87%", semanticRankLabel(0.8744))
    }
}
