package com.sangita.grantha.backend.api.services.bulkimport

import com.sangita.grantha.backend.api.services.bulkimport.workers.ManifestWorker
import com.sangita.grantha.backend.api.services.bulkimport.workers.ResolutionWorker
import com.sangita.grantha.backend.api.services.bulkimport.workers.ScrapeWorker
import com.sangita.grantha.shared.domain.model.ImportTaskRunDto
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import org.slf4j.Logger

class WorkerPool(
    private val dispatcher: TaskDispatcher,
    private val manifestWorker: ManifestWorker,
    private val scrapeWorker: ScrapeWorker,
    private val resolutionWorker: ResolutionWorker,
    private val watchdog: StuckTaskWatchdog,
    private val logger: Logger
) {
    fun start(
        scope: CoroutineScope,
        config: BulkImportWorkerConfig,
        manifestChannel: Channel<ImportTaskRunDto>,
        scrapeChannel: Channel<ImportTaskRunDto>,
        resolutionChannel: Channel<ImportTaskRunDto>,
        wakeUpChannel: Channel<Unit>,
        isActive: () -> Boolean
    ) {
        scope.launch(CoroutineName("BulkImportDispatcher")) {
            dispatcher.run(config, manifestChannel, scrapeChannel, resolutionChannel, wakeUpChannel, isActive)
        }

        repeat(config.manifestWorkerCount) { idx ->
            scope.launch(CoroutineName("BulkImportManifestWorker-$idx")) {
                manifestWorker.run(config, manifestChannel)
            }
        }

        repeat(config.scrapeWorkerCount) { idx ->
            scope.launch(CoroutineName("BulkImportScrapeWorker-$idx")) {
                scrapeWorker.run(config, scrapeChannel, isActive)
            }
        }

        repeat(config.resolutionWorkerCount) { idx ->
            scope.launch(CoroutineName("BulkImportResolutionWorker-$idx")) {
                resolutionWorker.run(config, resolutionChannel)
            }
        }

        scope.launch(CoroutineName("BulkImportWatchdog")) {
            watchdog.run(config, isActive)
        }

        logger.info(
            "Bulk import workers started (manifest={}, scrape={}, resolution={})",
            config.manifestWorkerCount,
            config.scrapeWorkerCount,
            config.resolutionWorkerCount
        )
    }
}
