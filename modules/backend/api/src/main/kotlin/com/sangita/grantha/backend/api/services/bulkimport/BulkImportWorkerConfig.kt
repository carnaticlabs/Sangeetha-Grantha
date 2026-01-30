package com.sangita.grantha.backend.api.services.bulkimport

data class BulkImportWorkerConfig(
    val manifestWorkerCount: Int = DEFAULT_MANIFEST_WORKER_COUNT,
    val scrapeWorkerCount: Int = DEFAULT_SCRAPE_WORKER_COUNT,
    val resolutionWorkerCount: Int = DEFAULT_RESOLUTION_WORKER_COUNT,
    val pollIntervalMs: Long = DEFAULT_POLL_INTERVAL_MS,
    val backoffMaxIntervalMs: Long = DEFAULT_BACKOFF_MAX_INTERVAL_MS,
    val batchClaimSize: Int = DEFAULT_BATCH_CLAIM_SIZE,
    val manifestChannelCapacity: Int = DEFAULT_MANIFEST_CHANNEL_CAPACITY,
    val scrapeChannelCapacity: Int = DEFAULT_SCRAPE_CHANNEL_CAPACITY,
    val resolutionChannelCapacity: Int = DEFAULT_RESOLUTION_CHANNEL_CAPACITY,
    val maxAttempts: Int = DEFAULT_MAX_ATTEMPTS,
    val perDomainRateLimitPerMinute: Int = DEFAULT_PER_DOMAIN_RATE_LIMIT,
    val templeRateLimitPerMinute: Int = DEFAULT_TEMPLE_RATE_LIMIT,
    val globalRateLimitPerMinute: Int = DEFAULT_GLOBAL_RATE_LIMIT,
    val stuckTaskThresholdMs: Long = DEFAULT_STUCK_TASK_THRESHOLD_MS,
    val watchdogIntervalMs: Long = DEFAULT_WATCHDOG_INTERVAL_MS,
) {
    companion object {
        const val DEFAULT_MANIFEST_WORKER_COUNT = 1
        const val DEFAULT_SCRAPE_WORKER_COUNT = 3
        const val DEFAULT_RESOLUTION_WORKER_COUNT = 2
        const val DEFAULT_POLL_INTERVAL_MS = 10_000L
        const val DEFAULT_BACKOFF_MAX_INTERVAL_MS = 30_000L
        const val DEFAULT_BATCH_CLAIM_SIZE = 5
        const val DEFAULT_MANIFEST_CHANNEL_CAPACITY = 5
        const val DEFAULT_SCRAPE_CHANNEL_CAPACITY = 20
        const val DEFAULT_RESOLUTION_CHANNEL_CAPACITY = 20
        const val DEFAULT_MAX_ATTEMPTS = 3
        // TRACK-013: Tuned rate limits (was 12/50, now 60/120 for better throughput)
        const val DEFAULT_PER_DOMAIN_RATE_LIMIT = 60
        const val DEFAULT_TEMPLE_RATE_LIMIT = 20 // Lower limit for external temple sites
        const val DEFAULT_GLOBAL_RATE_LIMIT = 120
        const val DEFAULT_STUCK_TASK_THRESHOLD_MS = 10 * 60 * 1000L
        const val DEFAULT_WATCHDOG_INTERVAL_MS = 60_000L
    }
}
