package com.sangita.grantha.backend.api.services.scraping

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

object ScrapeJsonSanitizer {
    private val json = Json { ignoreUnknownKeys = true }

    fun sanitizeScrapedKrithiJson(rawJson: String): String {
        val element = runCatching { json.parseToJsonElement(rawJson) }.getOrElse { return rawJson }
        val root = element as? JsonObject ?: return rawJson
        val mutable = root.toMutableMap()

        sanitizeSections(root["sections"])?.let { mutable["sections"] = it } ?: mutable.remove("sections")
        sanitizeLyricVariants(root["lyricVariants"])?.let { mutable["lyricVariants"] = it } ?: mutable.remove("lyricVariants")

        return JsonObject(mutable).toString()
    }

    private fun sanitizeSections(value: JsonElement?): JsonElement? {
        val array = value as? JsonArray ?: return null
        val cleaned = array.mapNotNull { element ->
            val obj = element as? JsonObject ?: return@mapNotNull null
            val type = obj["type"].asStringOrNull()?.trim()
            val text = obj["text"].asStringOrNull()?.trim()
            if (type.isNullOrBlank() || text.isNullOrBlank()) return@mapNotNull null
            JsonObject(mapOf("type" to JsonPrimitive(type), "text" to JsonPrimitive(text)))
        }
        return if (cleaned.isEmpty()) null else JsonArray(cleaned)
    }

    private fun sanitizeLyricVariants(value: JsonElement?): JsonElement? {
        val array = value as? JsonArray ?: return null
        val cleaned = array.mapNotNull { element ->
            val obj = element as? JsonObject ?: return@mapNotNull null
            val language = obj["language"].asStringOrNull()?.trim()
            val script = obj["script"].asStringOrNull()?.trim()
            if (language.isNullOrBlank() || script.isNullOrBlank()) return@mapNotNull null
            val lyrics = obj["lyrics"].asStringOrNull()
            val sections = sanitizeSections(obj["sections"])
            val fields = mutableMapOf<String, JsonElement>(
                "language" to JsonPrimitive(language),
                "script" to JsonPrimitive(script)
            )
            lyrics?.takeIf { it.isNotBlank() }?.let { fields["lyrics"] = JsonPrimitive(it) }
            if (sections != null) fields["sections"] = sections
            JsonObject(fields)
        }
        return if (cleaned.isEmpty()) null else JsonArray(cleaned)
    }

    private fun JsonElement?.asStringOrNull(): String? {
        if (this == null || this is kotlinx.serialization.json.JsonNull) return null
        val primitive = this as? JsonPrimitive ?: return null
        return primitive.content
    }
}
