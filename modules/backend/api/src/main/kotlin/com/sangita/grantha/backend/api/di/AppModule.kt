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
import com.sangita.grantha.backend.api.services.GeocodingService
import com.sangita.grantha.backend.api.services.TempleScrapingService
import com.sangita.grantha.backend.api.services.bulkimport.BulkImportWorkerServiceImpl
import com.sangita.grantha.backend.api.services.bulkimport.IBulkImportWorker
import io.micrometer.prometheus.PrometheusMeterRegistry
import org.koin.dsl.module

fun appModule(env: ApiEnvironment, metricsRegistry: PrometheusMeterRegistry) = module {
    single { env }
    single { JwtConfig.fromEnvironment(env) }
    single { GeminiApiClient(env.geminiApiKey ?: "", env.geminiModelUrl, env.geminiMinIntervalMs) }
    single<ITransliterator> { TransliterationServiceImpl(get()) }
    single { GeocodingService(get()) }
    single { TempleScrapingService(get(), get(), get()) }
    single<IWebScraper> { WebScrapingServiceImpl(get(), get()) }

    single { NameNormalizationService() }
    single<IEntityResolver> { EntityResolutionServiceImpl(get(), get()) }
    single { DeduplicationService(get(), get()) }
    single<IQualityScorer> { QualityScoringServiceImpl() }

    single<IImportService> {
        val scope = this
        ImportServiceImpl(get(), get(), get()) { scope.get<AutoApprovalService>() }
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

    single { metricsRegistry }
}
