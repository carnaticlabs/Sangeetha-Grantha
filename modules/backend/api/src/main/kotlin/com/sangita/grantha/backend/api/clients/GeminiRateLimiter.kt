package com.sangita.grantha.backend.api.clients

import java.util.concurrent.atomic.AtomicInteger
import kotlin.math.max
import kotlin.math.roundToLong
import kotlin.random.Random
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withLock

class GeminiRateLimiter(
    private val minIntervalMs: Long,
    private val qpsLimit: Double,
    private val maxConcurrent: Int,
    private val cooldownMaxMultiplier: Double,
    private val cooldownDecay: Double,
    private val jitterMs: Long
) {
    data class Snapshot(
        val minIntervalMs: Long,
        val qpsLimit: Double,
        val cooldownMultiplier: Double,
        val cooldownUntilMs: Long,
        val inFlight: Int,
        val maxConcurrent: Int
    )

    data class Permit(
        val waitMs: Long,
        val snapshot: Snapshot
    )

    private val mutex = Mutex()
    private val semaphore = Semaphore(maxConcurrent)
    private val inFlight = AtomicInteger(0)
    private var lastStartTimeMs: Long = 0L
    private var cooldownMultiplier: Double = 1.0
    private var cooldownUntilMs: Long = 0L

    suspend fun acquire(): Permit {
        val waitMs = mutex.withLock {
            val now = System.currentTimeMillis()
            val baseIntervalMs = max(minIntervalMs, qpsIntervalMs())
            val adjustedInterval = (baseIntervalMs * cooldownMultiplier).roundToLong()
            val earliestStart = max(lastStartTimeMs + adjustedInterval, cooldownUntilMs)
            val delayMs = max(0L, earliestStart - now)
            lastStartTimeMs = now + delayMs
            delayMs
        }

        val jitter = if (jitterMs > 0) Random.nextLong(0, jitterMs + 1) else 0L
        val totalWait = waitMs + jitter
        if (totalWait > 0) delay(totalWait)

        semaphore.acquire()
        inFlight.incrementAndGet()

        return Permit(totalWait, snapshot())
    }

    fun release() {
        inFlight.decrementAndGet()
        semaphore.release()
    }

    suspend fun onThrottle(retryAfterMs: Long?) {
        mutex.withLock {
            cooldownMultiplier = (cooldownMultiplier * 2.0).coerceAtMost(cooldownMaxMultiplier)
            if (retryAfterMs != null) {
                val now = System.currentTimeMillis()
                cooldownUntilMs = max(cooldownUntilMs, now + retryAfterMs)
            }
        }
    }

    suspend fun onSuccess() {
        mutex.withLock {
            cooldownMultiplier = max(1.0, cooldownMultiplier * cooldownDecay)
        }
    }

    fun snapshot(): Snapshot {
        return Snapshot(
            minIntervalMs = minIntervalMs,
            qpsLimit = qpsLimit,
            cooldownMultiplier = cooldownMultiplier,
            cooldownUntilMs = cooldownUntilMs,
            inFlight = inFlight.get(),
            maxConcurrent = maxConcurrent
        )
    }

    private fun qpsIntervalMs(): Long {
        if (qpsLimit <= 0.0) return 0L
        return (1000.0 / qpsLimit).roundToLong()
    }
}
