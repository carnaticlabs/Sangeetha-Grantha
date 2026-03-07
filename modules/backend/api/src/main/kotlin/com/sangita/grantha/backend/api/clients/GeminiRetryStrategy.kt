package com.sangita.grantha.backend.api.clients

import java.io.IOException
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import kotlin.random.Random

class GeminiRetryStrategy(
    private val maxRetries: Int,
    private val maxRetryWindowMs: Long
) {
    fun shouldRetry(attempt: Int, startTimeMs: Long): Boolean {
        if (attempt >= maxRetries) return false
        return System.currentTimeMillis() - startTimeMs < maxRetryWindowMs
    }

    fun handleThrottle(
        requestId: String,
        attempt: Int,
        status: Int,
        bodyText: String,
        currentDelayMs: Long,
        startTimeMs: Long,
        logger: org.slf4j.Logger
    ) {
        val elapsed = System.currentTimeMillis() - startTimeMs
        logger.warn(
            "Gemini throttle {} status={} attempt={}/{} elapsedMs={} nextDelayMs={} body={}",
            requestId,
            status,
            attempt,
            maxRetries,
            elapsed,
            currentDelayMs,
            truncate(bodyText)
        )
    }

    fun parseRetryAfterMs(header: String?): Long? {
        val value = header?.trim() ?: return null
        return value.toLongOrNull()?.let { seconds -> seconds * 1000L }
    }

    fun jitteredDelay(baseDelayMs: Long): Long {
        val jitter = Random.nextLong(0, 1000L)
        return baseDelayMs + jitter
    }

    fun isRetriableException(e: Exception): Boolean {
        return e is IOException ||
            e is SocketTimeoutException ||
            e is ConnectException ||
            e is UnknownHostException
    }

    fun truncate(body: String, maxChars: Int = 1200): String {
        return if (body.length <= maxChars) body else body.take(maxChars) + "..."
    }
}
