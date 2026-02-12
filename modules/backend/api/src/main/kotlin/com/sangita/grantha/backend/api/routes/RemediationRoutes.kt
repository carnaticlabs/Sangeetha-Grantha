package com.sangita.grantha.backend.api.routes

import com.sangita.grantha.backend.api.services.AuditRunnerService
import com.sangita.grantha.backend.api.services.ExtractionResultProcessor
import com.sangita.grantha.backend.api.services.RemediationService
import io.ktor.http.HttpStatusCode
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import kotlinx.serialization.Serializable

/**
 * TRACK-039/040/041: Routes for data quality audit, remediation, and extraction processing.
 */
fun Route.remediationRoutes(
    auditRunner: AuditRunnerService,
    remediationService: RemediationService,
    extractionProcessor: ExtractionResultProcessor,
) {
    route("/v1/admin/quality") {
        // =====================================================================
        // TRACK-039: Data Quality Audit
        // =====================================================================

        route("/audit") {
            /**
             * Run the full data quality audit (all 3 audit queries).
             * GET /v1/admin/quality/audit/full
             */
            get("/full") {
                val report = auditRunner.runFullAudit()
                call.respond(report)
            }

            /**
             * Run only the section count mismatch audit.
             * GET /v1/admin/quality/audit/section-count
             */
            get("/section-count") {
                val result = auditRunner.runSectionCountAudit()
                call.respond(result)
            }

            /**
             * Run only the label sequence mismatch audit.
             * GET /v1/admin/quality/audit/label-sequence
             */
            get("/label-sequence") {
                val result = auditRunner.runLabelSequenceAudit()
                call.respond(result)
            }

            /**
             * Run only the orphaned lyric blobs audit.
             * GET /v1/admin/quality/audit/orphaned-blobs
             */
            get("/orphaned-blobs") {
                val result = auditRunner.runOrphanedBlobsAudit()
                call.respond(result)
            }
        }

        // =====================================================================
        // TRACK-040: Data Remediation
        // =====================================================================

        route("/remediation") {
            /**
             * Preview remediation: show what would be changed.
             * GET /v1/admin/quality/remediation/preview?composer=dikshitar
             */
            get("/preview") {
                val composerFilter = call.request.queryParameters["composer"]
                val report = remediationService.preview(composerFilter)
                call.respond(report)
            }

            /**
             * Execute remediation pipeline.
             * POST /v1/admin/quality/remediation/execute
             */
            post("/execute") {
                val request = call.receive<RemediationRequest>()
                val report = remediationService.execute(
                    composerFilter = request.composerFilter,
                    enableCleanup = request.enableCleanup,
                    enableDeduplication = request.enableDeduplication,
                )
                call.respond(report)
            }

            /**
             * Preview metadata cleanup only.
             * GET /v1/admin/quality/remediation/cleanup-preview?composer=dikshitar&limit=50
             */
            get("/cleanup-preview") {
                val composerFilter = call.request.queryParameters["composer"]
                val limit = call.request.queryParameters["limit"]?.toIntOrNull() ?: 100
                val preview = remediationService.preview(composerFilter)
                call.respond(preview.cleanup)
            }

            /**
             * Preview duplicate variants.
             * GET /v1/admin/quality/remediation/dedup-preview?composer=tyagaraja
             */
            get("/dedup-preview") {
                val composerFilter = call.request.queryParameters["composer"]
                val report = remediationService.findDuplicateVariants(composerFilter)
                call.respond(report)
            }

            /**
             * Structural normalization analysis.
             * GET /v1/admin/quality/remediation/normalization?composer=dikshitar
             */
            get("/normalization") {
                val composerFilter = call.request.queryParameters["composer"]
                val report = remediationService.preview(composerFilter)
                call.respond(report.normalization)
            }
        }

        // =====================================================================
        // TRACK-041: Extraction Result Processing
        // =====================================================================

        route("/extraction") {
            /**
             * Process completed extraction queue results.
             * POST /v1/admin/quality/extraction/process
             */
            post("/process") {
                val batchSize = call.request.queryParameters["batchSize"]?.toIntOrNull() ?: 50
                val report = extractionProcessor.processCompletedExtractions(batchSize)
                call.respond(report)
            }
        }
    }
}

@Serializable
data class RemediationRequest(
    val composerFilter: String? = null,
    val enableCleanup: Boolean = true,
    val enableDeduplication: Boolean = true,
)
