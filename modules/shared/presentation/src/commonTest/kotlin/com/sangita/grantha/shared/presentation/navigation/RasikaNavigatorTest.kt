package com.sangita.grantha.shared.presentation.navigation

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.uuid.Uuid

class RasikaNavigatorTest {
    @Test
    fun tabsHaveIndependentStacks() {
        val navigator = RasikaNavigator()
        val id = Uuid.parse("66666666-6666-4666-8666-666666666666")
        navigator.open(RasikaDestination.KrithiReader(id))
        assertEquals(RasikaDestination.KrithiReader(id), navigator.current)
        navigator.selectTab(RasikaTab.Browse)
        assertEquals(RasikaDestination.Browse, navigator.current)
        navigator.selectTab(RasikaTab.Search)
        assertEquals(RasikaDestination.KrithiReader(id), navigator.current)
    }

    @Test
    fun backPopsUntilTabRoot() {
        val navigator = RasikaNavigator()
        navigator.open(RasikaDestination.Preferences)
        assertTrue(navigator.back())
        assertEquals(RasikaDestination.Search, navigator.current)
        assertFalse(navigator.back())
    }

    @Test
    fun duplicateDestinationIsNotPushed() {
        val navigator = RasikaNavigator()
        navigator.open(RasikaDestination.Search)
        assertEquals(1, navigator.stackSnapshot().size)
    }
}
