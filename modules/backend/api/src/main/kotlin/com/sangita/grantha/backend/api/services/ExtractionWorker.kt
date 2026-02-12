package com.sangita.grantha.backend.api.services

import kotlinx.coroutines.*
import org.slf4j.LoggerFactory
import kotlin.time.Duration.Companion.seconds

interface IExtractionWorker {
    fun start()
    fun stop()
}

class ExtractionWorker(
    private val extractionResultProcessor: ExtractionResultProcessor
) : IExtractionWorker {
    private val logger = LoggerFactory.getLogger(javaClass)
    private var job: Job? = null
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    override fun start() {
        if (job != null && job?.isActive == true) {
            logger.warn("Extraction worker already started")
            return
        }

        logger.info("Starting extraction result processing worker")
        job = scope.launch {
            while (isActive) {
                try {
                    // Process up to 50 items at a time
                    extractionResultProcessor.processCompletedExtractions(batchSize = 50)
                } catch (e: Exception) {
                    if (e is CancellationException) throw e
                    logger.error("Error in extraction worker loop", e)
                }

                // Poll every 10 seconds
                delay(10.seconds)
            }
        }
    }

    override fun stop() {
        logger.info("Stopping extraction worker")
        job?.cancel()
        job = null
    }
}
