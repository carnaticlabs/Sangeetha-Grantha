package com.sangita.grantha.backend.api.di

import com.sangita.grantha.backend.api.clients.GeminiApiClient
import com.sangita.grantha.backend.api.config.ApiEnvironment
import com.sangita.grantha.backend.api.config.JwtConfig
import com.sangita.grantha.backend.api.services.AdminDashboardService
import com.sangita.grantha.backend.api.services.AuditLogService
import com.sangita.grantha.backend.api.services.AutoApprovalService
import com.sangita.grantha.backend.api.services.BulkImportOrchestrationService
import com.sangita.grantha.backend.api.services.DeduplicationService
import com.sangita.grantha.backend.api.services.EntityResolutionServiceImpl
import com.sangita.grantha.backend.api.services.IEntityResolver
import com.sangita.grantha.backend.api.services.IImportService
import com.sangita.grantha.backend.api.services.IKrithiService
import com.sangita.grantha.backend.api.services.IQualityScorer
import com.sangita.grantha.backend.api.services.IReferenceDataService
import com.sangita.grantha.backend.api.services.ITransliterator
import com.sangita.grantha.backend.api.services.IWebScraper
import com.sangita.grantha.backend.api.services.ImportReviewer
import com.sangita.grantha.backend.api.services.ImportServiceImpl
import com.sangita.grantha.backend.api.services.KrithiNotationService
import com.sangita.grantha.backend.api.services.KrithiServiceImpl
import com.sangita.grantha.backend.api.services.NameNormalizationService
import com.sangita.grantha.backend.api.services.QualityScoringServiceImpl
import com.sangita.grantha.backend.api.services.ReferenceDataServiceImpl
import com.sangita.grantha.backend.api.services.TransliterationServiceImpl
import com.sangita.grantha.backend.api.services.UserManagementService
import com.sangita.grantha.backend.api.services.WebScrapingServiceImpl
import com.sangita.grantha.backend.api.services.DeterministicWebScraper
import com.sangita.grantha.backend.api.services.GeocodingService
import com.sangita.grantha.backend.api.services.TempleScrapingService
import com.sangita.grantha.backend.api.services.bulkimport.BulkImportWorkerServiceImpl
import com.sangita.grantha.backend.api.services.bulkimport.IBulkImportWorker
import com.sangita.grantha.backend.api.services.IExtractionWorker
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry
import org.koin.dsl.module

fun appModule(env: ApiEnvironment, metricsRegistry: PrometheusMeterRegistry) = module {
    single { env }
    single { JwtConfig.fromEnvironment(env) }
    single {
        GeminiApiClient(
            apiKey = env.geminiApiKey ?: "",
            modelUrl = env.geminiModelUrl,
            minIntervalMs = env.geminiMinIntervalMs,
            qpsLimit = env.geminiQpsLimit,
            maxConcurrent = env.geminiMaxConcurrent,
            maxRetries = env.geminiMaxRetries,
            maxRetryWindowMs = env.geminiMaxRetryWindowMs,
            requestTimeoutMs = env.geminiRequestTimeoutMs,
            fallbackModelUrl = env.geminiFallbackModelUrl,
            useSchemaMode = env.geminiUseSchemaMode
        )
    }
    single<ITransliterator> { TransliterationServiceImpl(get()) }
    single { GeocodingService(get()) }
    single { TempleScrapingService(get(), get(), get()) }
    single<IWebScraper> {
        if (env.geminiStubMode) {
            DeterministicWebScraper()
        } else {
            WebScrapingServiceImpl(
                geminiClient = get(),
                templeScrapingService = get(),
                cacheTtlHours = env.scrapeCacheTtlHours,
                cacheMaxEntries = env.scrapeCacheMaxEntries,
                useSchemaMode = env.geminiUseSchemaMode
            )
        }
    }

    single { NameNormalizationService() }
    single<IEntityResolver> { EntityResolutionServiceImpl(get(), get()) }
    single { DeduplicationService(get(), get()) }
    single<IQualityScorer> { QualityScoringServiceImpl() }

    single<IImportService> {
        val scope = this
        ImportServiceImpl(get(), get(), get(), get()) { scope.get<AutoApprovalService>() }
    }
    single<ImportReviewer> { get<IImportService>() as ImportReviewer }
    single { AutoApprovalService(get()) }

    single<IKrithiService> { KrithiServiceImpl(get()) }
    single { KrithiNotationService(get()) }
    single<IReferenceDataService> { ReferenceDataServiceImpl(get()) }
    single { AuditLogService(get()) }
    single { AdminDashboardService(get()) }
    single { UserManagementService(get()) }

    single<IBulkImportWorker> {
        BulkImportWorkerServiceImpl(
            dal = get(),
            importService = get(),
            webScrapingService = get(),
            entityResolutionService = get(),
            deduplicationService = get(),
            autoApprovalService = get(),
            qualityScoringService = get()
        )
    }
    single { BulkImportOrchestrationService(get(), get()) }

    // TRACK-045: Sourcing service
    single { com.sangita.grantha.backend.api.services.SourcingService(get()) }

    // TRACK-039/040/041: Quality audit, remediation, and extraction processing
    single { com.sangita.grantha.backend.api.services.AuditRunnerService() }
    single { com.sangita.grantha.backend.api.services.MetadataCleanupService(get()) }
    single { com.sangita.grantha.backend.api.services.StructuralNormalizationService(get()) }
    single {
        com.sangita.grantha.backend.api.services.RemediationService(
            dal = get(),
            metadataCleanup = get(),
            structuralNormalization = get(),
            qualityScorer = get(),
            normalizer = get(),
        )
    }
    // TRACK-053: Krithi creation from extraction results
    single {
        com.sangita.grantha.backend.api.services.KrithiCreationFromExtractionService(
            dal = get(),
            normalizer = get(),
        )
    }
    // TRACK-056: Variant matching service
    single {
        com.sangita.grantha.backend.api.services.VariantMatchingService(
            dal = get(),
            normalizer = get(),
        )
    }
    single {
        com.sangita.grantha.backend.api.services.ExtractionResultProcessor(
            dal = get(),
            normalizer = get(),
            krithiCreationService = get(),
            variantMatchingService = get(),
        )
    }

    single<IExtractionWorker> { com.sangita.grantha.backend.api.services.ExtractionWorker(get()) }

    single<PrometheusMeterRegistry> { metricsRegistry }
}
