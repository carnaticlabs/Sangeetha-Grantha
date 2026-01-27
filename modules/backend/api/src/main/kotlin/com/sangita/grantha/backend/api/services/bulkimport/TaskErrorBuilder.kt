package com.sangita.grantha.backend.api.services.bulkimport

import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class TaskErrorBuilder(private val json: Json = Json) {
    fun build(code: String, message: String, url: String? = null, attempt: Int? = null, cause: String? = null): String =
        json.encodeToString(TaskErrorPayload(code = code, message = message, url = url, attempt = attempt, cause = cause))
}

@Serializable
private data class TaskErrorPayload(
    val code: String,
    val message: String,
    val url: String? = null,
    val attempt: Int? = null,
    val cause: String? = null,
)
