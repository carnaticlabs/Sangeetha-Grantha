package com.sangita.grantha.backend.api.services.bulkimport

import com.sangita.grantha.backend.api.services.AutoApprovalService
import com.sangita.grantha.backend.api.services.DeduplicationService
import com.sangita.grantha.backend.api.services.IEntityResolver
import com.sangita.grantha.backend.api.services.IImportService
import com.sangita.grantha.backend.api.services.IQualityScorer
import com.sangita.grantha.backend.api.services.IWebScraper
import com.sangita.grantha.backend.api.services.bulkimport.workers.ManifestWorker
import com.sangita.grantha.backend.api.services.bulkimport.workers.ResolutionWorker
import com.sangita.grantha.backend.api.services.bulkimport.workers.ScrapeWorker
import com.sangita.grantha.backend.dal.SangitaDal
import com.sangita.grantha.shared.domain.model.ImportTaskRunDto
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.isActive
import org.slf4j.LoggerFactory

interface IBulkImportWorker {
    fun start(config: BulkImportWorkerConfig = BulkImportWorkerConfig())
    fun stop()
    fun wakeUp()
}

class BulkImportWorkerServiceImpl(
    private val dal: SangitaDal,
    private val importService: IImportService,
    private val webScrapingService: IWebScraper,
    private val entityResolutionService: IEntityResolver,
    private val deduplicationService: DeduplicationService,
    private val autoApprovalService: AutoApprovalService,
    private val qualityScoringService: IQualityScorer,
) : IBulkImportWorker {
    private val logger = LoggerFactory.getLogger(javaClass)

    private val wakeUpChannel = Channel<Unit>(Channel.CONFLATED)
    private var scope: CoroutineScope? = null

    private var manifestChannel: Channel<ImportTaskRunDto>? = null
    private var scrapeChannel: Channel<ImportTaskRunDto>? = null
    private var resolutionChannel: Channel<ImportTaskRunDto>? = null

    private val errorBuilder = TaskErrorBuilder()
    private val completionHandler = BatchCompletionHandler(dal)
    private val rateLimiter = RateLimiter()
    private val manifestParser = ManifestParser(logger)
    private val dispatcher = TaskDispatcher(dal, logger)
    private val watchdog = StuckTaskWatchdog(dal, errorBuilder, logger)

    private val manifestWorker = ManifestWorker(dal, manifestParser, errorBuilder, logger)
    private val scrapeWorker = ScrapeWorker(
        dal = dal,
        importService = importService,
        webScrapingService = webScrapingService,
        rateLimiter = rateLimiter,
        errorBuilder = errorBuilder,
        completionHandler = completionHandler
    )
    private val resolutionWorker = ResolutionWorker(
        dal = dal,
        entityResolutionService = entityResolutionService,
        deduplicationService = deduplicationService,
        autoApprovalService = autoApprovalService,
        qualityScoringService = qualityScoringService,
        errorBuilder = errorBuilder,
        completionHandler = completionHandler
    )

    private val workerPool = WorkerPool(
        dispatcher = dispatcher,
        manifestWorker = manifestWorker,
        scrapeWorker = scrapeWorker,
        resolutionWorker = resolutionWorker,
        watchdog = watchdog,
        logger = logger
    )

    override fun wakeUp() {
        wakeUpChannel.trySend(Unit)
    }

    override fun start(config: BulkImportWorkerConfig) {
        if (scope != null) return

        val mChannel = Channel<ImportTaskRunDto>(config.manifestChannelCapacity)
        val sChannel = Channel<ImportTaskRunDto>(config.scrapeChannelCapacity)
        val rChannel = Channel<ImportTaskRunDto>(config.resolutionChannelCapacity)

        manifestChannel = mChannel
        scrapeChannel = sChannel
        resolutionChannel = rChannel

        val workerScope = CoroutineScope(SupervisorJob() + Dispatchers.IO + CoroutineName("BulkImportWorkers"))
        scope = workerScope

        workerPool.start(
            scope = workerScope,
            config = config,
            manifestChannel = mChannel,
            scrapeChannel = sChannel,
            resolutionChannel = rChannel,
            wakeUpChannel = wakeUpChannel,
            isActive = { scope?.isActive == true }
        )
    }

    override fun stop() {
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
}
