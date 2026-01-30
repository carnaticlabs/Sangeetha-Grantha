package com.sangita.grantha.backend.api.services.bulkimport

import java.net.URI
import kotlin.math.max
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class RateLimiter {
    private data class RateWindow(var windowStartedAtMs: Long = 0, var count: Int = 0)

    private val rateLimiterMutex = Mutex()
    private var globalWindow = RateWindow()
    private val perDomainWindows = object : LinkedHashMap<String, RateWindow>(100, 0.75f, true) {
        override fun removeEldestEntry(eldest: Map.Entry<String, RateWindow>): Boolean {
            val now = System.currentTimeMillis()
            val age = now - eldest.value.windowStartedAtMs
            return size > 100 || age > 3600_000
        }
    }

    suspend fun throttle(url: String, config: BulkImportWorkerConfig, isActive: () -> Boolean) {
        val host = runCatching { URI(url).host ?: "unknown" }.getOrDefault("unknown")
        while (isActive()) {
            val waitMs = rateLimiterMutex.withLock {
                val now = System.currentTimeMillis()
                val domainWindow = synchronized(perDomainWindows) {
                    perDomainWindows.getOrPut(host) { RateWindow(windowStartedAtMs = now, count = 0) }
                }

                val limit = if (host.contains("templenet.com") || host.contains("indiantemples.com") || host.contains("wikipedia")) {
                    config.templeRateLimitPerMinute
                } else {
                    config.perDomainRateLimitPerMinute
                }

                val domainWait = computeWait(domainWindow, now, limit)
                val globalWait = computeWait(globalWindow, now, config.globalRateLimitPerMinute)
                val maxWait = max(globalWait, domainWait)
                if (maxWait <= 0) {
                    incrementWindow(globalWindow, now)
                    incrementWindow(domainWindow, now)
                    synchronized(perDomainWindows) {
                        perDomainWindows[host] = domainWindow
                    }
                    return
                }
                maxWait
            }

            if (waitMs <= 0) return
            delay(waitMs)
        }
    }

    private fun computeWait(window: RateWindow, now: Long, limitPerMinute: Int): Long {
        if (limitPerMinute <= 0) return 0
        val windowMs = 60_000L
        if (now - window.windowStartedAtMs >= windowMs) {
            window.windowStartedAtMs = now
            window.count = 0
            return 0
        }
        return if (window.count >= limitPerMinute) windowMs - (now - window.windowStartedAtMs) else 0
    }

    private fun incrementWindow(window: RateWindow, now: Long) {
        val windowMs = 60_000L
        if (now - window.windowStartedAtMs >= windowMs) {
            window.windowStartedAtMs = now
            window.count = 0
        }
        window.count += 1
    }
}
