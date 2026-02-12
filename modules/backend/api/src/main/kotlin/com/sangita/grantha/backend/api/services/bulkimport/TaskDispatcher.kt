package com.sangita.grantha.backend.api.services.bulkimport

import com.sangita.grantha.backend.dal.SangitaDal
import com.sangita.grantha.backend.dal.enums.BatchStatus
import com.sangita.grantha.backend.dal.enums.JobType
import com.sangita.grantha.shared.domain.model.ImportTaskRunDto
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.withTimeoutOrNull
import org.slf4j.Logger

class TaskDispatcher(
    private val dal: SangitaDal,
    private val logger: Logger
) {
    suspend fun run(
        config: BulkImportWorkerConfig,
        manifestChannel: Channel<ImportTaskRunDto>,
        scrapeChannel: Channel<ImportTaskRunDto>,
        resolutionChannel: Channel<ImportTaskRunDto>,
        wakeUpChannel: Channel<Unit>,
        isActive: () -> Boolean
    ) {
        var currentDelay = config.pollIntervalMs
        while (isActive()) {
            var anyTaskFound = false

            val manifestTasks = dal.bulkImport.claimNextPendingTasks(
                jobType = JobType.MANIFEST_INGEST,
                allowedBatchStatuses = setOf(BatchStatus.PENDING, BatchStatus.RUNNING),
                limit = 1
            )
            if (manifestTasks.isNotEmpty()) {
                anyTaskFound = true
                manifestTasks.forEach { manifestChannel.send(it) }
            }

            val scrapeTasks = dal.bulkImport.claimNextPendingTasks(
                jobType = JobType.SCRAPE,
                allowedBatchStatuses = setOf(BatchStatus.RUNNING),
                limit = config.batchClaimSize
            )
            if (scrapeTasks.isNotEmpty()) {
                anyTaskFound = true
                scrapeTasks.forEach { scrapeChannel.send(it) }
            }

            val resolutionTasks = dal.bulkImport.claimNextPendingTasks(
                jobType = JobType.ENTITY_RESOLUTION,
                allowedBatchStatuses = setOf(BatchStatus.RUNNING),
                limit = config.batchClaimSize
            )
            if (resolutionTasks.isNotEmpty()) {
                logger.info("Claimed ${resolutionTasks.size} resolution tasks")
                anyTaskFound = true
                resolutionTasks.forEach { resolutionChannel.send(it) }
            } else {
                // Periodically log that we are looking
                if (System.currentTimeMillis() % 10000 < 500) {
                    logger.debug("Polling for resolution tasks... (anyTaskFound=$anyTaskFound)")
                }
            }

            if (anyTaskFound) {
                currentDelay = config.pollIntervalMs
                delay(config.pollIntervalMs)
            } else {
                val signal = withTimeoutOrNull(currentDelay) {
                    wakeUpChannel.receive()
                }

                if (signal != null) {
                    currentDelay = config.pollIntervalMs
                    logger.debug("Dispatcher woke up by signal")
                } else {
                    currentDelay = computeBackoff(currentDelay, config)
                }
            }
        }
    }

    private fun computeBackoff(currentDelay: Long, config: BulkImportWorkerConfig): Long {
        return (currentDelay * 2).coerceAtMost(config.backoffMaxIntervalMs)
    }
}
