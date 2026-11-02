package com.sangita.grantha.shared.presentation.kshetra

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotSame
import kotlin.test.assertSame
import kotlin.uuid.Uuid

class KshetraScrollStatesTest {
    private val tiruvarur = Uuid.parse("01930000-0000-7000-8000-000000000001")
    private val shrine = Uuid.parse("01930000-0000-7000-8000-000000000002")

    @Test
    fun returningToATempleGivesBackTheSameState() {
        val states = KshetraScrollStates()
        states.retain(listOpen = true, openDetails = setOf(tiruvarur))
        val first = states.detail(tiruvarur)
        states.retain(listOpen = true, openDetails = setOf(tiruvarur, shrine))
        assertSame(first, states.detail(tiruvarur))
    }

    @Test
    fun openingAnotherTempleStartsAtTheTopWithItsOwnState() {
        val states = KshetraScrollStates()
        states.retain(listOpen = true, openDetails = setOf(tiruvarur, shrine))
        val parent = states.detail(tiruvarur)
        val child = states.detail(shrine)
        assertNotSame(parent, child)
        assertEquals(0, child.firstVisibleItemIndex)
        assertEquals(0, child.firstVisibleItemScrollOffset)
    }

    @Test
    fun aTemplePoppedOffTheStackIsForgottenSoAFreshVisitStartsAtTheTop() {
        val states = KshetraScrollStates()
        states.retain(listOpen = true, openDetails = setOf(tiruvarur))
        val first = states.detail(tiruvarur)
        states.retain(listOpen = false, openDetails = emptySet())
        assertEquals(0, states.detailCount)
        states.retain(listOpen = true, openDetails = setOf(tiruvarur))
        assertNotSame(first, states.detail(tiruvarur))
    }

    @Test
    fun aFadingOutScreenDoesNotRevivePoppedState() {
        val states = KshetraScrollStates()
        states.retain(listOpen = true, openDetails = setOf(tiruvarur))
        states.retain(listOpen = true, openDetails = emptySet())
        states.detail(tiruvarur)
        assertEquals(0, states.detailCount)
    }

    @Test
    fun theListPositionSurvivesWhileTheListIsOnTheStackAndResetsWhenItLeaves() {
        val states = KshetraScrollStates()
        val list = states.list
        states.retain(listOpen = true, openDetails = setOf(tiruvarur))
        assertSame(list, states.list)
        states.retain(listOpen = false, openDetails = emptySet())
        assertNotSame(list, states.list)
        assertEquals(0, states.list.value)
    }

    @Test
    fun cachedDetailsStayWithinTheBound() {
        val states = KshetraScrollStates(maxDetails = 3)
        repeat(10) { states.detail(Uuid.random()) }
        assertEquals(3, states.detailCount)
    }
}
