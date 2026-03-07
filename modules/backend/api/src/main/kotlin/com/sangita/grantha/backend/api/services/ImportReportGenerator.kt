package com.sangita.grantha.backend.api.services

import com.sangita.grantha.shared.domain.model.ImportedKrithiDto
import com.sangita.grantha.shared.domain.model.ImportBatchDto
import com.sangita.grantha.shared.domain.model.ImportStatusDto
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * Generates QA reports (JSON and CSV) for import batches.
 * Stateless — all data is passed in as parameters.
 */
class ImportReportGenerator {

    fun generateJsonReport(batch: Any?, imports: List<ImportedKrithiDto>): String {
        val avgQualityScore: Double? = imports.mapNotNull { it.qualityScore }.average().takeIf { !it.isNaN() }
        val qualityTierCounts: Map<String?, Int> = imports.groupBy { it.qualityTier }.mapValues { it.value.size }

        val batchIdStr = when (batch) {
            is ImportBatchDto -> batch.id.toString()
            else -> null
        }
        val sourceManifest = when (batch) {
            is ImportBatchDto -> batch.sourceManifest
            else -> null
        }

        val summary = mapOf<String, Any?>(
            "batchId" to batchIdStr,
            "sourceManifest" to sourceManifest,
            "totalImports" to imports.size,
            "approved" to imports.count { it.importStatus == ImportStatusDto.APPROVED },
            "rejected" to imports.count { it.importStatus == ImportStatusDto.REJECTED },
            "pending" to imports.count { it.importStatus == ImportStatusDto.PENDING },
            "avgQualityScore" to avgQualityScore,
            "qualityTierCounts" to qualityTierCounts
        )

        val items = imports.map { import ->
            mapOf(
                "id" to import.id.toString(),
                "title" to import.rawTitle,
                "composer" to import.rawComposer,
                "raga" to import.rawRaga,
                "tala" to import.rawTala,
                "status" to import.importStatus.name,
                "qualityScore" to import.qualityScore,
                "qualityTier" to import.qualityTier,
                "sourceKey" to import.sourceKey
            )
        }

        return Json.encodeToString(
            mapOf(
                "summary" to summary,
                "items" to items
            )
        )
    }

    fun generateCsvReport(batch: Any?, imports: List<ImportedKrithiDto>): String {
        val header = "ID,Title,Composer,Raga,Tala,Status,Quality Score,Quality Tier,Source\n"
        val rows = imports.joinToString("\n") { import ->
            listOf(
                import.id.toString(),
                escapeCsv(import.rawTitle ?: ""),
                escapeCsv(import.rawComposer ?: ""),
                escapeCsv(import.rawRaga ?: ""),
                escapeCsv(import.rawTala ?: ""),
                import.importStatus.name,
                import.qualityScore?.toString() ?: "",
                import.qualityTier ?: "",
                escapeCsv(import.sourceKey ?: "")
            ).joinToString(",")
        }
        return header + rows
    }

    fun escapeCsv(value: String): String {
        return if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            "\"${value.replace("\"", "\"\"")}\""
        } else {
            value
        }
    }
}
