package com.sangita.grantha.backend.api.clients

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

class GeminiThrottleException(message: String, val status: Int) : RuntimeException(message)

@Serializable
data class GeminiRequest(
    val contents: List<Content>,
    val generationConfig: GenerationConfig? = null
)

@Serializable
data class GenerationConfig(
    val responseMimeType: String? = null,
    val responseSchema: JsonObject? = null
)

@Serializable
data class Content(
    val parts: List<Part>,
    val role: String = "user"
)

@Serializable
data class Part(
    val text: String
)

@Serializable
data class GeminiResponse(
    val candidates: List<Candidate>
)

@Serializable
data class Candidate(
    val content: Content,
    val finishReason: String? = null,
    val index: Int = 0
)
