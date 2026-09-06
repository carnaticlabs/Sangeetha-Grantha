package com.sangita.grantha.shared.mobile.usage

import com.sangita.grantha.shared.domain.model.catalogue.CatalogueContract
import kotlin.uuid.Uuid

data class InteractionContext(
    val sessionId: Uuid,
    val interactionId: Uuid,
)

class MobileSession(
    private val clockMs: () -> Long = { defaultNowMs() },
    private val uuid: () -> Uuid = { Uuid.random() },
    private val inactivityMs: Long = CatalogueContract.SESSION_INACTIVITY_MS,
) {
    private var sessionId: Uuid = uuid()
    private var lastActivityMs: Long = clockMs()

    fun current(nowMs: Long = clockMs()): Uuid {
        if (nowMs - lastActivityMs >= inactivityMs) {
            sessionId = uuid()
        }
        lastActivityMs = nowMs
        return sessionId
    }

    fun beginInteraction(nowMs: Long = clockMs()): InteractionContext =
        InteractionContext(sessionId = current(nowMs), interactionId = uuid())

    fun onProcessLaunch() {
        sessionId = uuid()
        lastActivityMs = clockMs()
    }

    companion object {
        internal fun defaultNowMs(): Long = kotlin.time.Clock.System.now().toEpochMilliseconds()
    }
}
