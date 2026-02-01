package com.sangita.grantha.backend.api.services.scraping

import com.github.benmanes.caffeine.cache.Caffeine
import java.time.Duration

class ScrapeCache<T : Any>(
    ttl: Duration,
    maxEntries: Long,
    private val cacheName: String
) {
    private val cache = if (maxEntries > 0) {
        Caffeine.newBuilder()
            .expireAfterWrite(ttl)
            .maximumSize(maxEntries)
            .build<String, T>()
    } else {
        null
    }

    fun isEnabled(): Boolean = cache != null

    fun get(key: String): T? = cache?.getIfPresent(key)

    fun put(key: String, value: T) {
        cache?.put(key, value)
    }

    override fun toString(): String = "ScrapeCache(name=$cacheName, enabled=${cache != null})"
}
