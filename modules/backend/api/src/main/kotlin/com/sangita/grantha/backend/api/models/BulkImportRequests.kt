package com.sangita.grantha.backend.api.models

import kotlinx.serialization.Serializable

@Serializable
data class BulkImportCreateBatchRequest(
    /**
     * Absolute (or backend-local) path to a CSV manifest.
     * Example: /Users/.../database/for_import/Dikshitar-Krithi-For-Import.csv
     */
    val sourceManifestPath: String,
)

@Serializable
data class BulkImportRetryRequest(
    /**
     * If true, requeue FAILED + RETRYABLE tasks. If false, requeue only RETRYABLE tasks.
     */
    val includeFailed: Boolean = true,
)

