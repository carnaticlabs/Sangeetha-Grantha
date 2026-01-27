package com.sangita.grantha.backend.api.services.bulkimport

import com.sangita.grantha.backend.dal.SangitaDal
import java.time.Duration
import java.time.OffsetDateTime
import java.time.ZoneOffset
import kotlinx.coroutines.delay
import org.slf4j.Logger

class StuckTaskWatchdog(
    private val dal: SangitaDal,
    private val errorBuilder: TaskErrorBuilder,
    private val logger: Logger
) {
    suspend fun run(config: BulkImportWorkerConfig, isActive: () -> Boolean) {
        while (isActive()) {
            val threshold = OffsetDateTime.now(ZoneOffset.UTC).minus(Duration.ofMillis(config.stuckTaskThresholdMs))
            val stuckTasks = dal.bulkImport.markStuckRunningTasks(threshold, config.maxAttempts)
            if (stuckTasks.isNotEmpty()) {
                logger.warn("Watchdog marked {} stuck tasks as retryable (threshold={}ms)", stuckTasks.size, config.stuckTaskThresholdMs)
                stuckTasks.forEach { task ->
                    val job = dal.bulkImport.findJobById(task.jobId)
                    val batchId = job?.batchId
                    if (batchId != null) {
                        runCatching {
                            dal.bulkImport.createEvent(
                                refType = "batch",
                                refId = batchId,
                                eventType = "TASK_MARKED_RETRYABLE",
                                data = errorBuilder.build(
                                    code = "stuck_timeout",
                                    message = "Task exceeded watchdog threshold",
                                    url = task.sourceUrl,
                                    attempt = task.attempt
                                )
                            )
                        }
                    }
                }
            }
            delay(config.watchdogIntervalMs)
        }
    }
}
