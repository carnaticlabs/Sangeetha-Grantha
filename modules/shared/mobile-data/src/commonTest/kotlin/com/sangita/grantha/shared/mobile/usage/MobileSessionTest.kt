package com.sangita.grantha.shared.mobile.usage

import com.sangita.grantha.shared.domain.model.catalogue.CatalogueContract
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.uuid.Uuid

class MobileSessionTest {
    @Test
    fun inactivityStartsANewSession() {
        var now = 1_000L
        var next = 1
        val session = MobileSession(
            clockMs = { now },
            uuid = { Uuid.parse("00000000-0000-4000-8000-00000000000${next++}") },
            inactivityMs = CatalogueContract.SESSION_INACTIVITY_MS,
        )
        val first = session.current()
        now += CatalogueContract.SESSION_INACTIVITY_MS - 1
        assertEquals(first, session.current())
        now += CatalogueContract.SESSION_INACTIVITY_MS
        val second = session.current()
        assertNotEquals(first, second)
    }

    @Test
    fun processLaunchRotatesSession() {
        var next = 1
        val session = MobileSession(
            clockMs = { 0L },
            uuid = { Uuid.parse("00000000-0000-4000-8000-00000000000${next++}") },
        )
        val first = session.current()
        session.onProcessLaunch()
        assertNotEquals(first, session.current())
    }

    @Test
    fun retrySharesANewInteractionNotANewSession() {
        val session = MobileSession(clockMs = { 10L }, uuid = { Uuid.random() })
        val first = session.beginInteraction()
        val retry = session.beginInteraction()
        assertEquals(first.sessionId, retry.sessionId)
        assertNotEquals(first.interactionId, retry.interactionId)
    }
}
