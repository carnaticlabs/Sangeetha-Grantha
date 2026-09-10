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
        navigator.selectTab(RasikaTab.Explore)
        assertEquals(RasikaDestination.Explore, navigator.current)
        navigator.selectTab(RasikaTab.Home)
        assertEquals(RasikaDestination.KrithiReader(id), navigator.current)
    }

    @Test
    fun backPopsUntilTabRoot() {
        val navigator = RasikaNavigator()
        navigator.open(RasikaDestination.Settings)
        assertTrue(navigator.back())
        assertEquals(RasikaDestination.Home, navigator.current)
        assertFalse(navigator.back())
    }

    @Test
    fun duplicateDestinationIsNotPushed() {
        val navigator = RasikaNavigator()
        navigator.open(RasikaDestination.Home)
        assertEquals(1, navigator.stackSnapshot().size)
    }

    @Test
    fun ninthPushAsksToReturnToRoot() {
        val navigator = RasikaNavigator()
        repeat(7) { index ->
            val suffix = index.toString().padStart(12, '0')
            navigator.open(RasikaDestination.KrithiReader(Uuid.parse("00000000-0000-4000-8000-$suffix")))
        }
        assertEquals(8, navigator.stackSnapshot().size)
        assertFalse(navigator.open(RasikaDestination.Browse))
        assertTrue(navigator.stackLimitReached)
        navigator.returnToRoot()
        assertEquals(RasikaDestination.Home, navigator.current)
        assertFalse(navigator.stackLimitReached)
    }
}
