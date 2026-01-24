package com.sangita.grantha.backend.api.services

import com.sangita.grantha.backend.api.models.ImportKrithiRequest
import com.sangita.grantha.backend.dal.SangitaDal
import com.sangita.grantha.backend.dal.enums.BatchStatus
import com.sangita.grantha.backend.dal.enums.JobType
import com.sangita.grantha.backend.dal.enums.TaskStatus
import com.sangita.grantha.shared.domain.model.ImportJobDto
import com.sangita.grantha.shared.domain.model.ImportTaskRunDto
import com.sangita.grantha.shared.domain.model.JobTypeDto
import java.io.FileReader
import java.net.URI
import java.nio.file.Files
import java.nio.file.Path
import java.time.Duration
import java.time.OffsetDateTime
import java.time.ZoneOffset
import kotlin.io.path.exists
import kotlin.math.max
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.encodeToJsonElement
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import org.apache.commons.csv.CSVFormat
import org.slf4j.LoggerFactory

class BulkImportWorkerService(
    private val dal: SangitaDal,
    private val importService: ImportService,
    private val webScrapingService: WebScrapingService,
    private val entityResolutionService: EntityResolutionService,
    private val deduplicationService: DeduplicationService,
    private val autoApprovalService: AutoApprovalService,
    private val qualityScoringService: QualityScoringService = QualityScoringService(),
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    data class WorkerConfig(
        val manifestWorkerCount: Int = 1,
        val scrapeWorkerCount: Int = 3,
        val resolutionWorkerCount: Int = 2,
        val pollIntervalMs: Long = 750,
        val backoffMaxIntervalMs: Long = 15_000,
        val batchClaimSize: Int = 5,
        val manifestChannelCapacity: Int = 5,
        val scrapeChannelCapacity: Int = 20,
        val resolutionChannelCapacity: Int = 20,
        val maxAttempts: Int = 3,
        // TRACK-013: Tuned rate limits (was 12/50, now 60/120 for better throughput)
        val perDomainRateLimitPerMinute: Int = 60,  // 1 req/sec per domain
        val globalRateLimitPerMinute: Int = 120,    // 2 req/sec global
        val stuckTaskThresholdMs: Long = 10 * 60 * 1000, // 10 minutes
        val watchdogIntervalMs: Long = 60_000, // 1 minute
    )

    private fun computeBackoff(currentDelay: Long, taskFound: Boolean, config: WorkerConfig): Long {
        return if (taskFound) {
            config.pollIntervalMs
        } else {
            (currentDelay * 2).coerceAtMost(config.backoffMaxIntervalMs)
        }
    }

    private data class RateWindow(var windowStartedAtMs: Long = 0, var count: Int = 0)

    private val rateLimiterMutex = Mutex()
    private var globalWindow = RateWindow()
    // Fix: Use LRU cache with bounded size and TTL to prevent memory leak
    // Max 100 entries or 1 hour TTL per domain window
    private val perDomainWindows = object : LinkedHashMap<String, RateWindow>(100, 0.75f, true) {
        override fun removeEldestEntry(eldest: Map.Entry<String, RateWindow>): Boolean {
            val now = System.currentTimeMillis()
            val age = now - eldest.value.windowStartedAtMs
            return size > 100 || age > 3600_000 // Max 100 entries or 1 hour TTL
        }
    }

    // Channels for push-based architecture
    private var manifestChannel: Channel<ImportTaskRunDto>? = null
    private var scrapeChannel: Channel<ImportTaskRunDto>? = null
    private var resolutionChannel: Channel<ImportTaskRunDto>? = null
    
    // Signal channel for immediate wakeup (Conflated so we don't buffer multiple signals)
    private val wakeUpChannel = Channel<Unit>(Channel.CONFLATED)

    private var scope: CoroutineScope? = null

    fun wakeUp() {
        wakeUpChannel.trySend(Unit)
    }

    fun start(config: WorkerConfig = WorkerConfig()) {
        if (scope != null) return
        
        val mChannel = Channel<ImportTaskRunDto>(config.manifestChannelCapacity)
        val sChannel = Channel<ImportTaskRunDto>(config.scrapeChannelCapacity)
        val rChannel = Channel<ImportTaskRunDto>(config.resolutionChannelCapacity)
        
        manifestChannel = mChannel
        scrapeChannel = sChannel
        resolutionChannel = rChannel

        val workerScope = CoroutineScope(SupervisorJob() + Dispatchers.IO + CoroutineName("BulkImportWorkers"))
        scope = workerScope

        // Start single Unified Dispatcher
        workerScope.launch(CoroutineName("BulkImportDispatcher")) {
            runDispatcherLoop(config, mChannel, sChannel, rChannel)
        }

        repeat(config.manifestWorkerCount) { idx ->
            workerScope.launch(CoroutineName("BulkImportManifestWorker-$idx")) {
                processManifestLoop(config, mChannel)
            }
        }
        repeat(config.scrapeWorkerCount) { idx ->
            workerScope.launch(CoroutineName("BulkImportScrapeWorker-$idx")) {
                processScrapeLoop(config, sChannel)
            }
        }
        repeat(config.resolutionWorkerCount) { idx ->
            workerScope.launch(CoroutineName("BulkImportResolutionWorker-$idx")) {
                processEntityResolutionLoop(config, rChannel)
            }
        }

        workerScope.launch(CoroutineName("BulkImportWatchdog")) {
            runWatchdogLoop(config)
        }

        logger.info("Bulk import workers started (manifest={}, scrape={}, resolution={})", 
            config.manifestWorkerCount, config.scrapeWorkerCount, config.resolutionWorkerCount)
    }

    fun stop() {
        scope?.cancel("Stopping bulk import workers")
        scope = null
        
        manifestChannel?.close()
        scrapeChannel?.close()
        resolutionChannel?.close()
        
        manifestChannel = null
        scrapeChannel = null
        resolutionChannel = null
        
        logger.info("Bulk import workers stopped")
    }

    @Serializable
    private data class ManifestJobPayload(
        val sourceManifestPath: String,
    )

    @Serializable
    private data class TaskErrorPayload(
        val code: String,
        val message: String,
        val url: String? = null,
        val attempt: Int? = null,
        val cause: String? = null,
    )

    @Serializable
    private data class CsvRow(
        val krithi: String,
        val raga: String?,
        val hyperlink: String,
    )

    private suspend fun runDispatcherLoop(
        config: WorkerConfig,
        manifestChannel: Channel<ImportTaskRunDto>,
        scrapeChannel: Channel<ImportTaskRunDto>,
        resolutionChannel: Channel<ImportTaskRunDto>
    ) {
        var currentDelay = config.pollIntervalMs
        while (scope?.isActive == true) {
            var anyTaskFound = false

            // 1. Manifest
            val manifestTasks = dal.bulkImport.claimNextPendingTasks(
                jobType = JobType.MANIFEST_INGEST,
                allowedBatchStatuses = setOf(BatchStatus.PENDING, BatchStatus.RUNNING),
                limit = 1
            )
            if (manifestTasks.isNotEmpty()) {
                anyTaskFound = true
                manifestTasks.forEach { manifestChannel.send(it) }
            }

            // 2. Scrape
            val scrapeTasks = dal.bulkImport.claimNextPendingTasks(
                jobType = JobType.SCRAPE,
                allowedBatchStatuses = setOf(BatchStatus.RUNNING),
                limit = config.batchClaimSize
            )
            if (scrapeTasks.isNotEmpty()) {
                anyTaskFound = true
                scrapeTasks.forEach { scrapeChannel.send(it) }
            }

            // 3. Resolution
            val resolutionTasks = dal.bulkImport.claimNextPendingTasks(
                jobType = JobType.ENTITY_RESOLUTION,
                allowedBatchStatuses = setOf(BatchStatus.RUNNING),
                limit = config.batchClaimSize
            )
            if (resolutionTasks.isNotEmpty()) {
                anyTaskFound = true
                resolutionTasks.forEach { resolutionChannel.send(it) }
            }

            // Adaptive Backoff with Wakeup Support
            if (anyTaskFound) {
                currentDelay = config.pollIntervalMs
                delay(config.pollIntervalMs)
            } else {
                val signal = withTimeoutOrNull(currentDelay) {
                    wakeUpChannel.receive()
                }
                
                if (signal != null) {
                    currentDelay = config.pollIntervalMs // Reset backoff
                    logger.debug("Dispatcher woke up by signal")
                } else {
                    currentDelay = computeBackoff(currentDelay, false, config)
                }
            }
        }
    }

    private suspend fun processManifestLoop(config: WorkerConfig, channel: Channel<ImportTaskRunDto>) {
        for (task in channel) {
            processManifestTask(task, config)
        }
    }

    private suspend fun processManifestTask(task: ImportTaskRunDto, config: WorkerConfig) {
        val startedAt = OffsetDateTime.now(ZoneOffset.UTC)
        // Mark execution start when the worker actually begins processing
        dal.bulkImport.markTaskStarted(task.id, startedAt)
        val job: ImportJobDto = dal.bulkImport.findJobById(task.jobId)
            ?: run {
                logger.warn("Manifest task {} has missing job {}", task.id, task.jobId)
                dal.bulkImport.updateTaskStatus(id = task.id, status = TaskStatus.FAILED, error = """{"message":"Missing job"}""")
                return
            }

        val batchId = job.batchId
        dal.bulkImport.updateJobStatus(id = job.id, status = TaskStatus.RUNNING, startedAt = startedAt)
        dal.bulkImport.updateBatchStatus(id = batchId, status = BatchStatus.RUNNING, startedAt = startedAt)

        val attemptRow = dal.bulkImport.incrementTaskAttempt(task.id)
        val attempt = attemptRow?.attempt ?: task.attempt
        if (attempt > config.maxAttempts) {
            dal.bulkImport.updateTaskStatus(
                id = task.id,
                status = TaskStatus.FAILED,
                error = buildErrorPayload(
                    code = "max_attempts_exceeded",
                    message = "Manifest ingest exceeded max attempts",
                    attempt = attempt
                ),
                completedAt = OffsetDateTime.now(ZoneOffset.UTC)
            )
            dal.bulkImport.updateJobStatus(id = job.id, status = TaskStatus.FAILED, completedAt = OffsetDateTime.now(ZoneOffset.UTC))
            return
        }

        val payload = job.payload ?: run {
            failManifestTask(task, job, startedAt, buildErrorPayload(code = "missing_payload", message = "Missing job payload"))
            return
        }

        val manifestPath = try {
            Json.decodeFromString<ManifestJobPayload>(payload).sourceManifestPath
        } catch (e: Exception) {
            failManifestTask(
                task,
                job,
                startedAt,
                buildErrorPayload(code = "invalid_payload", message = "Invalid manifest payload", cause = e.message)
            )
            return
        }

        val csvPath = Path.of(manifestPath)
        if (!csvPath.exists()) {
            failManifestTask(
                task,
                job,
                startedAt,
                buildErrorPayload(code = "manifest_missing", message = "Manifest not found", cause = manifestPath)
            )
            return
        }

        try {
            val rows = parseCsvManifest(csvPath)
            if (rows.isEmpty()) {
                failManifestTask(
                    task,
                    job,
                    startedAt,
                    buildErrorPayload(code = "manifest_empty", message = "Manifest contained no rows", cause = manifestPath)
                )
                return
            }

            val dedupedRows = rows.distinctBy { it.hyperlink.trim().lowercase() }

            val existingScrapeJob = dal.bulkImport.listJobsByBatch(batchId).firstOrNull { it.jobType == JobTypeDto.SCRAPE }
            val scrapeJob = existingScrapeJob ?: dal.bulkImport.createJob(
                batchId = batchId,
                jobType = JobType.SCRAPE,
                payload = Json.encodeToString(mapOf("sourceManifestPath" to manifestPath))
            )

            val createdTasks = dal.bulkImport.createTasks(
                jobId = scrapeJob.id,
                batchId = batchId,
                tasks = dedupedRows.map { row ->
                    val key = "${row.krithi}|${row.raga ?: ""}".trim()
                    key to row.hyperlink
                }
            )

            val totalTasks = dal.bulkImport.listTasksByJob(scrapeJob.id).size
            dal.bulkImport.setBatchTotals(id = batchId, totalTasks = totalTasks)
            dal.bulkImport.createEvent(
                refType = "batch",
                refId = batchId,
                eventType = "MANIFEST_INGEST_SUCCEEDED",
                data = kotlinx.serialization.json.buildJsonObject {
                    put("rows", dedupedRows.size)
                    put("newTasks", createdTasks.size)
                    put("totalTasks", totalTasks)
                    put("manifestPath", manifestPath)
                }.toString()
            )

            dal.bulkImport.updateTaskStatus(
                id = task.id,
                status = TaskStatus.SUCCEEDED,
                durationMs = elapsedMsSince(startedAt),
                completedAt = OffsetDateTime.now(ZoneOffset.UTC)
            )
            dal.bulkImport.updateJobStatus(
                id = job.id,
                status = TaskStatus.SUCCEEDED,
                result = kotlinx.serialization.json.buildJsonObject {
                    put("rows", dedupedRows.size)
                    put("newTasks", createdTasks.size)
                    put("totalTasks", totalTasks)
                }.toString(),
                completedAt = OffsetDateTime.now(ZoneOffset.UTC)
            )
        } catch (e: Exception) {
            failManifestTask(
                task,
                job,
                startedAt,
                buildErrorPayload(code = "manifest_ingest_failed", message = "Manifest ingest failed", cause = e.message)
            )
        }
    }

    private suspend fun failManifestTask(task: ImportTaskRunDto, job: ImportJobDto, startedAt: OffsetDateTime, errorJson: String) {
        val now = OffsetDateTime.now(ZoneOffset.UTC)
        dal.bulkImport.updateTaskStatus(
            id = task.id,
            status = TaskStatus.FAILED,
            error = errorJson,
            durationMs = elapsedMsSince(startedAt),
            completedAt = now
        )
        dal.bulkImport.updateJobStatus(id = job.id, status = TaskStatus.FAILED, result = errorJson, completedAt = now)
        dal.bulkImport.createEvent(refType = "batch", refId = job.batchId, eventType = "MANIFEST_INGEST_FAILED", data = errorJson)
        // If manifest ingest fails (including zero-task scenarios), the whole batch
        // must be marked FAILED to satisfy the clarified requirements.
        dal.bulkImport.updateBatchStatus(id = job.batchId, status = BatchStatus.FAILED, completedAt = now)
    }

    private suspend fun processScrapeLoop(config: WorkerConfig, channel: Channel<ImportTaskRunDto>) {
        for (task in channel) {
            processScrapeTask(task, config)
        }
    }

    private suspend fun processScrapeTask(task: ImportTaskRunDto, config: WorkerConfig) {
        val startedAt = OffsetDateTime.now(ZoneOffset.UTC)
        // Mark execution start when the worker actually begins processing
        dal.bulkImport.markTaskStarted(task.id, startedAt)

        val job = dal.bulkImport.findJobById(task.jobId)
            ?: run {
                dal.bulkImport.updateTaskStatus(id = task.id, status = TaskStatus.FAILED, error = """{"message":"Missing job"}""")
                return
            }

        val batchId = job.batchId

        val attemptRow = dal.bulkImport.incrementTaskAttempt(task.id)
        val attempt = attemptRow?.attempt ?: task.attempt

        val url = task.sourceUrl
        if (url.isNullOrBlank()) {
            dal.bulkImport.updateTaskStatus(
                id = task.id,
                status = TaskStatus.FAILED,
                error = buildErrorPayload(code = "missing_source_url", message = "Task missing sourceUrl", attempt = attempt),
                completedAt = OffsetDateTime.now(ZoneOffset.UTC)
            )
            dal.bulkImport.incrementBatchCounters(id = batchId, processedDelta = 1, failedDelta = 1)
            checkAndTriggerNextStage(job.id)
            return
        }

        if (attempt > config.maxAttempts) {
            dal.bulkImport.updateTaskStatus(
                id = task.id,
                status = TaskStatus.FAILED,
                error = buildErrorPayload(
                    code = "max_attempts_exceeded",
                    message = "Task exceeded max attempts",
                    url = url,
                    attempt = attempt
                ),
                completedAt = OffsetDateTime.now(ZoneOffset.UTC)
            )
            dal.bulkImport.incrementBatchCounters(id = batchId, processedDelta = 1, failedDelta = 1)
            checkAndTriggerNextStage(job.id)
            return
        }

        if (TaskStatus.valueOf(job.status.name) != TaskStatus.RUNNING) {
            dal.bulkImport.updateJobStatus(id = job.id, status = TaskStatus.RUNNING, startedAt = startedAt)
        }

        try {
            throttleForRateLimit(url, config)
            val scraped = webScrapingService.scrapeKrithi(url)
            val importRequest = ImportKrithiRequest(
                source = "BulkImportCSV",
                sourceKey = url,
                batchId = batchId.toString(),
                rawTitle = scraped.title,
                rawLyrics = scraped.lyrics,
                rawComposer = scraped.composer,
                rawRaga = scraped.raga,
                rawTala = scraped.tala,
                rawDeity = scraped.deity,
                rawTemple = scraped.temple,
                rawLanguage = scraped.language,
                rawPayload = Json.encodeToJsonElement(scraped)
            )

            importService.submitImports(listOf(importRequest))

            dal.bulkImport.updateTaskStatus(
                id = task.id,
                status = TaskStatus.SUCCEEDED,
                durationMs = elapsedMsSince(startedAt),
                completedAt = OffsetDateTime.now(ZoneOffset.UTC)
            )
            dal.bulkImport.incrementBatchCounters(id = batchId, processedDelta = 1, succeededDelta = 1)
            checkAndTriggerNextStage(job.id)
        } catch (e: Exception) {
            val errorJson = buildErrorPayload(
                code = "scrape_failed",
                message = "Scrape/import failed",
                url = url,
                attempt = attempt,
                cause = e.message
            )
            val finalAttempt = attempt >= config.maxAttempts
            dal.bulkImport.updateTaskStatus(
                id = task.id,
                status = if (finalAttempt) TaskStatus.FAILED else TaskStatus.RETRYABLE,
                error = errorJson,
                durationMs = elapsedMsSince(startedAt),
                completedAt = if (finalAttempt) OffsetDateTime.now(ZoneOffset.UTC) else null
            )

            if (finalAttempt) {
                dal.bulkImport.incrementBatchCounters(id = batchId, processedDelta = 1, failedDelta = 1)
                checkAndTriggerNextStage(job.id)
            } else {
                dal.bulkImport.createEvent(
                    refType = "batch",
                    refId = batchId,
                    eventType = "TASK_RETRY_SCHEDULED",
                    data = errorJson
                )
            }
        }
    }

    private suspend fun processEntityResolutionLoop(config: WorkerConfig, channel: Channel<ImportTaskRunDto>) {
        for (task in channel) {
            processEntityResolutionTask(task, config)
        }
    }

    private suspend fun processEntityResolutionTask(task: ImportTaskRunDto, config: WorkerConfig) {
        val startedAt = OffsetDateTime.now(ZoneOffset.UTC)
        // Mark execution start when the worker actually begins processing
        dal.bulkImport.markTaskStarted(task.id, startedAt)
        val job = dal.bulkImport.findJobById(task.jobId)
            ?: run {
                dal.bulkImport.updateTaskStatus(id = task.id, status = TaskStatus.FAILED, error = """{"message":"Missing job"}""")
                return
            }
        val batchId = job.batchId

        if (TaskStatus.valueOf(job.status.name) != TaskStatus.RUNNING) {
            dal.bulkImport.updateJobStatus(id = job.id, status = TaskStatus.RUNNING, startedAt = startedAt)
        }

        try {
            val sourceId = dal.imports.findOrCreateSource("BulkImportCSV")
            val importedKrithi = dal.imports.findBySourceAndKey(sourceId, task.sourceUrl ?: "")
            
            if (importedKrithi == null) {
                 dal.bulkImport.updateTaskStatus(
                    id = task.id,
                    status = TaskStatus.FAILED,
                    error = buildErrorPayload(code = "import_missing", message = "ImportedKrithi not found", url = task.sourceUrl),
                    completedAt = OffsetDateTime.now(ZoneOffset.UTC)
                )
                dal.bulkImport.incrementBatchCounters(id = batchId, processedDelta = 1, failedDelta = 1)
                checkAndTriggerNextStage(job.id)
                return
            }
            
            val resolution = entityResolutionService.resolve(importedKrithi)
            val resolutionJson = Json.encodeToString(resolution)
            
            dal.imports.saveResolution(importedKrithi.id, resolutionJson)

            // Step 2: Deduplication
            val composerId = resolution.composerCandidates.firstOrNull { it.confidence == "HIGH" }?.entity?.id
            val ragaId = resolution.ragaCandidates.firstOrNull { it.confidence == "HIGH" }?.entity?.id
            
            val duplicates = deduplicationService.findDuplicates(
                imported = importedKrithi,
                resolvedComposerId = composerId,
                resolvedRagaId = ragaId
            )
            
            if (duplicates.matches.isNotEmpty()) {
                val duplicatesJson = Json.encodeToString(duplicates)
                dal.imports.saveDuplicates(importedKrithi.id, duplicatesJson)
            }

            // Step 3: Quality Scoring (TRACK-011)
            val updatedImported = dal.imports.findById(importedKrithi.id)
            if (updatedImported != null) {
                val qualityScore = qualityScoringService.calculateQualityScore(
                    imported = updatedImported,
                    resolutionDataJson = resolutionJson
                )
                dal.imports.updateQualityScores(
                    id = updatedImported.id,
                    qualityScore = qualityScore.overall,
                    qualityTier = qualityScore.tier.name,
                    completenessScore = qualityScore.completeness,
                    resolutionConfidence = qualityScore.resolutionConfidence,
                    sourceQuality = qualityScore.sourceQuality,
                    validationScore = qualityScore.validationScore
                )
            }

            // Step 4: Auto-Approval
            val finalImported = dal.imports.findById(importedKrithi.id)
            if (finalImported != null) {
                autoApprovalService.autoApproveIfHighConfidence(finalImported)
            }
            
            dal.bulkImport.updateTaskStatus(
                id = task.id,
                status = TaskStatus.SUCCEEDED,
                durationMs = elapsedMsSince(startedAt),
                completedAt = OffsetDateTime.now(ZoneOffset.UTC)
            )
            dal.bulkImport.incrementBatchCounters(id = batchId, processedDelta = 1, succeededDelta = 1)
            checkAndTriggerNextStage(job.id)
            
        } catch (e: Exception) {
             dal.bulkImport.updateTaskStatus(
                id = task.id,
                status = TaskStatus.FAILED,
                error = buildErrorPayload(code = "resolution_failed", message = "Resolution failed", cause = e.message),
                completedAt = OffsetDateTime.now(ZoneOffset.UTC)
            )
            dal.bulkImport.incrementBatchCounters(id = batchId, processedDelta = 1, failedDelta = 1)
            checkAndTriggerNextStage(job.id)
        }
    }

    private suspend fun checkAndTriggerNextStage(jobId: kotlin.uuid.Uuid) {
        val job = dal.bulkImport.findJobById(jobId) ?: return
        val batch = dal.bulkImport.findBatchById(job.batchId) ?: return
        
        // TRACK-013: Use batch counters instead of loading all tasks (O(1) instead of O(N))
        // Only verify completion if counters suggest it's possible
        if (batch.processedTasks < batch.totalTasks) {
            return // Not complete yet, skip expensive task loading
        }
        
        // Only load tasks if counters suggest completion (defensive check)
        val tasks = dal.bulkImport.listTasksByJob(jobId)
        val isComplete = tasks.all { 
            val s = TaskStatus.valueOf(it.status.name)
            s == TaskStatus.SUCCEEDED || s == TaskStatus.FAILED || s == TaskStatus.BLOCKED || s == TaskStatus.CANCELLED
        }

        if (isComplete) {
            if (TaskStatus.valueOf(job.status.name) != TaskStatus.SUCCEEDED) {
                 dal.bulkImport.updateJobStatus(
                    id = jobId, 
                    status = TaskStatus.SUCCEEDED, 
                    completedAt = OffsetDateTime.now(ZoneOffset.UTC)
                )
            }

            if (job.jobType == JobTypeDto.SCRAPE) {
                val existing = dal.bulkImport.listJobsByBatch(job.batchId).find { it.jobType == JobTypeDto.ENTITY_RESOLUTION }
                if (existing == null) {
                    val resolutionJob = dal.bulkImport.createJob(
                        batchId = job.batchId,
                        jobType = JobType.ENTITY_RESOLUTION,
                        payload = null
                    )
                    
                    val succeededTasks = tasks.filter { TaskStatus.valueOf(it.status.name) == TaskStatus.SUCCEEDED }
                    val newTasks = succeededTasks.mapNotNull { t ->
                        if (t.krithiKey != null && t.sourceUrl != null) {
                            t.krithiKey to t.sourceUrl!!
                        } else null
                    }
                    
                    if (newTasks.isNotEmpty()) {
                         dal.bulkImport.createTasks(
                            jobId = resolutionJob.id,
                            batchId = job.batchId,
                            tasks = newTasks
                         )
                         
                         val batch = dal.bulkImport.findBatchById(job.batchId)
                         if (batch != null) {
                             dal.bulkImport.setBatchTotals(batch.id, batch.totalTasks + newTasks.size)
                         }
                    } else {
                         maybeCompleteBatch(job.batchId)
                    }
                }
            } else if (job.jobType == JobTypeDto.ENTITY_RESOLUTION) {
                maybeCompleteBatch(job.batchId)
            }
        }
    }

    private suspend fun maybeCompleteBatch(batchId: kotlin.uuid.Uuid) {
        val batch = dal.bulkImport.findBatchById(batchId) ?: return
        if (batch.status != com.sangita.grantha.shared.domain.model.BatchStatusDto.RUNNING) return
        if (batch.totalTasks <= 0) return
        if (batch.processedTasks < batch.totalTasks) return

        val finalStatus = if (batch.failedTasks == 0 && batch.blockedTasks == 0) BatchStatus.SUCCEEDED else BatchStatus.FAILED
        dal.bulkImport.updateBatchStatus(id = batchId, status = finalStatus, completedAt = OffsetDateTime.now(ZoneOffset.UTC))
        dal.bulkImport.createEvent(
            refType = "batch",
            refId = batchId,
            eventType = "BATCH_COMPLETED",
            data = Json.encodeToString(
                mapOf(
                    "status" to finalStatus.name,
                    "totalTasks" to batch.totalTasks,
                    "processedTasks" to batch.processedTasks,
                    "succeededTasks" to batch.succeededTasks,
                    "failedTasks" to batch.failedTasks
                )
            )
        )
    }

    private fun buildErrorPayload(code: String, message: String, url: String? = null, attempt: Int? = null, cause: String? = null): String =
        Json.encodeToString(TaskErrorPayload(code = code, message = message, url = url, attempt = attempt, cause = cause))

    private suspend fun throttleForRateLimit(url: String, config: WorkerConfig) {
        val host = runCatching { URI(url).host ?: "unknown" }.getOrDefault("unknown")
        while (scope?.isActive == true) {
            val waitMs = rateLimiterMutex.withLock {
                val now = System.currentTimeMillis()
                val globalWait = computeWait(globalWindow, now, config.globalRateLimitPerMinute)
                // Fix: Synchronized access to LRU map
                val domainWindow = synchronized(perDomainWindows) {
                    perDomainWindows.getOrPut(host) { RateWindow(windowStartedAtMs = now, count = 0) }
                }
                val domainWait = computeWait(domainWindow, now, config.perDomainRateLimitPerMinute)
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

    private suspend fun runWatchdogLoop(config: WorkerConfig) {
        while (scope?.isActive == true) {
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
                                data = buildErrorPayload(
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

    private fun elapsedMsSince(startedAt: OffsetDateTime): Int {
        val now = OffsetDateTime.now(ZoneOffset.UTC)
        val ms = java.time.Duration.between(startedAt, now).toMillis()
        return ms.coerceIn(0, Int.MAX_VALUE.toLong()).toInt()
    }

    private fun parseCsvManifest(path: Path): List<CsvRow> {
        // Use explicit UTF-8 charset and ensure the file handle is always closed.
        path.toFile().bufferedReader(Charsets.UTF_8).use { reader ->
            val parser = CSVFormat.DEFAULT.builder()
                .setHeader()
                .setSkipHeaderRecord(true)
                .setIgnoreHeaderCase(true)
                .setTrim(true)
                .build()
                .parse(reader)

            // Validate Headers
            val headerMap = parser.headerMap
            if (headerMap != null) {
                // Check case-insensitive presence (though parser handles mapping, we want to fail fast)
                val keys = headerMap.keys.map { it.lowercase() }.toSet()
                val required = listOf("krithi", "hyperlink")
                val missing = required.filter { !keys.contains(it) }

                if (missing.isNotEmpty()) {
                    throw IllegalArgumentException(
                        "Missing required columns: ${
                            missing.map { it.replaceFirstChar(Char::titlecase) }
                        }. Found: ${headerMap.keys}"
                    )
                }
            }

            return parser.mapNotNull { record ->
                val krithi = if (record.isMapped("Krithi")) record.get("Krithi") else null
                val hyperlink = if (record.isMapped("Hyperlink")) record.get("Hyperlink") else null

                if (krithi.isNullOrBlank() || hyperlink.isNullOrBlank()) return@mapNotNull null

                // Basic URL Validation (Pre-flight safety)
                if (!isValidUrl(hyperlink)) {
                    logger.warn("Skipping invalid URL in manifest: $hyperlink")
                    return@mapNotNull null
                }

                val raga = if (record.isMapped("Raga")) record.get("Raga")?.takeIf { it.isNotBlank() } else null

                CsvRow(krithi = krithi, raga = raga, hyperlink = hyperlink)
            }
        }
    }

    private fun isValidUrl(url: String): Boolean {
        return try {
            val uri = URI(url)
            val scheme = uri.scheme
            val host = uri.host
            (scheme.equals("http", ignoreCase = true) || scheme.equals("https", ignoreCase = true)) && !host.isNullOrBlank()
        } catch (e: Exception) {
            false
        }
    }
}