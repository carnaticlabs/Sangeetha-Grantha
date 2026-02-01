package com.sangita.grantha.backend.api.clients

import kotlin.test.assertTrue
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test

class GeminiRateLimiterTest {

    @Test
    fun `cooldown multiplier increases and decays`() = runTest {
        val limiter = GeminiRateLimiter(
            minIntervalMs = 0,
            qpsLimit = 10.0,
            maxConcurrent = 1,
            cooldownMaxMultiplier = 4.0,
            cooldownDecay = 0.5,
            jitterMs = 0
        )

        val initial = limiter.snapshot().cooldownMultiplier
        limiter.onThrottle(null)
        val increased = limiter.snapshot().cooldownMultiplier
        assertTrue(increased > initial)

        limiter.onSuccess()
        val decayed = limiter.snapshot().cooldownMultiplier
        assertTrue(decayed <= increased)
    }
}
