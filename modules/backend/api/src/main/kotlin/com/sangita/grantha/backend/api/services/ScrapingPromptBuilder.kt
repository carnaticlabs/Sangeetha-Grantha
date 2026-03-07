package com.sangita.grantha.backend.api.services

import com.sangita.grantha.backend.api.services.scraping.KrithiStructureParser
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject

/**
 * Builds the structured prompt text and JSON schema used by WebScrapingService
 * when calling the Gemini LLM for krithi metadata extraction.
 */
object ScrapingPromptBuilder {

    fun buildStructuredText(
        url: String,
        title: String?,
        promptBlocks: KrithiStructureParser.PromptBlocks,
        includeContent: Boolean = true
    ): String {
        val builder = StringBuilder()
        builder.appendLine("=== SOURCE URL ===")
        builder.appendLine(url)
        if (!title.isNullOrBlank()) {
            builder.appendLine()
            builder.appendLine("=== PAGE TITLE ===")
            builder.appendLine(title)
        }
        builder.appendLine()
        builder.appendLine("=== HEADER META ===")
        if (promptBlocks.metaLines.isNotEmpty()) {
            builder.appendLine(promptBlocks.metaLines.joinToString("\n"))
        } else {
            builder.appendLine("(none)")
        }
        builder.appendLine()
        builder.appendLine("=== CONTENT BLOCKS ===")

        val blocksToInclude = if (includeContent) {
            promptBlocks.blocks
        } else {
            // Only include meta-like content blocks (Meaning, Notes, etc.)
            val allowedLabels = setOf("MEANING", "GIST", "NOTES", "WORD_DIVISION")
            promptBlocks.blocks.filter { it.label in allowedLabels }
        }

        if (blocksToInclude.isEmpty()) {
            builder.appendLine("(none - filtered for metadata optimization)")
        } else {
            blocksToInclude.forEach { block ->
                builder.appendLine("=== ${block.label} ===")
                builder.appendLine(block.lines.joinToString("\n"))
                builder.appendLine()
            }
        }
        return builder.toString().trim()
    }

    fun buildPrompt(structuredText: String, hasDeterministicContent: Boolean): String {
        val sectionInstruction = if (hasDeterministicContent) {
            "Sections and Lyrics have already been deterministically extracted. Focus ONLY on extracting METADATA (Composer, Raga, Tala, Deity, Temple, Meaning) from the provided text headers/summaries."
        } else {
            "Extract sections (Pallavi, etc.) and metadata from the text."
        }

        return """
            Extract Carnatic krithi metadata from the structured text below.
            Keep null for missing fields. Preserve line breaks in lyrics with \\n.

            Extraction Scope:
            - $sectionInstruction
            - If explicit headers are absent, infer Pallavi then Anupallavi then Charanam from stanza order.
            - If sections are found, populate the 'sections' array.

            Value-Added Enrichment:
            - Identify 'temple' (shrine/kshetra) and 'deity' (primary god/goddess) mentioned.
            - Extract 'raga' and 'tala' from the text. **Check the === HEADER META === section first**, as it often contains these details (e.g., "rAgaM ... tALaM ...").
            - Provide a concise 'meaning' or 'gist' if present in the text.

            Multi-language Lyric Extraction:
            - The source text often contains lyric blocks in multiple scripts (Devanagari, Tamil, Telugu, Kannada, Malayalam, Latin/English).
            - Extract EACH distinct script block into a separate entry in 'lyricVariants'.
            - Map them to the correct language/script codes:
              - Language: SA (Sanskrit), TA (Tamil), TE (Telugu), KN (Kannada), ML (Malayalam), HI (Hindi), EN (English)
              - Script: devanagari, tamil, telugu, kannada, malayalam, latin

            Extract:
            - title, composer, raga, tala, deity, temple, templeUrl, language, lyrics, notation, sections, lyricVariants

            Structured Content:
            $structuredText
        """.trimIndent()
    }

    fun scrapedKrithiSchema(): JsonObject = buildJsonObject {
        put("type", JsonPrimitive("object"))
        put(
            "properties",
            buildJsonObject {
                put("title", stringSchema())
                put("composer", nullableStringSchema())
                put("raga", nullableStringSchema())
                put("tala", nullableStringSchema())
                put("deity", nullableStringSchema())
                put("temple", nullableStringSchema())
                put("templeUrl", nullableStringSchema())
                put("language", nullableStringSchema())
                put("lyrics", nullableStringSchema())
                put("notation", nullableStringSchema())
                put("sections", nullableArraySchema(sectionSchema()))
                put("lyricVariants", nullableArraySchema(lyricVariantSchema()))
                put("templeDetails", nullableObjectSchema(templeDetailsSchema()))
                put("warnings", nullableArraySchema(stringSchema()))
            }
        )
        put("required", JsonArray(listOf(JsonPrimitive("title"))))
        put("additionalProperties", JsonPrimitive(false))
    }

    private fun sectionSchema(): JsonObject = buildJsonObject {
        put("type", JsonPrimitive("object"))
        put(
            "properties",
            buildJsonObject {
                put("type", JsonPrimitive("string"))
                put("text", JsonPrimitive("string"))
                put("label", nullableStringSchema())
            }
        )
        put("required", JsonArray(listOf(JsonPrimitive("type"), JsonPrimitive("text"))))
        put("additionalProperties", JsonPrimitive(false))
    }

    private fun lyricVariantSchema(): JsonObject = buildJsonObject {
        put("type", JsonPrimitive("object"))
        put(
            "properties",
            buildJsonObject {
                put("language", JsonPrimitive("string"))
                put("script", JsonPrimitive("string"))
                put("lyrics", nullableStringSchema())
                put("sections", nullableArraySchema(sectionSchema()))
            }
        )
        put("required", JsonArray(listOf(JsonPrimitive("language"), JsonPrimitive("script"))))
        put("additionalProperties", JsonPrimitive(false))
    }

    private fun templeDetailsSchema(): JsonObject = buildJsonObject {
        put("type", JsonPrimitive("object"))
        put(
            "properties",
            buildJsonObject {
                put("name", JsonPrimitive("string"))
                put("deity", nullableStringSchema())
                put("location", nullableStringSchema())
                put("latitude", nullableNumberSchema())
                put("longitude", nullableNumberSchema())
                put("description", nullableStringSchema())
            }
        )
        put("required", JsonArray(listOf(JsonPrimitive("name"))))
        put("additionalProperties", JsonPrimitive(false))
    }

    private fun stringSchema(): JsonObject = buildJsonObject {
        put("type", JsonPrimitive("string"))
    }

    private fun nullableStringSchema(): JsonObject = buildJsonObject {
        put("type", buildJsonArray { add(JsonPrimitive("string")); add(JsonPrimitive("null")) })
    }

    private fun nullableNumberSchema(): JsonObject = buildJsonObject {
        put("type", buildJsonArray { add(JsonPrimitive("number")); add(JsonPrimitive("null")) })
    }

    private fun nullableObjectSchema(schema: JsonObject): JsonObject = buildJsonObject {
        put("type", buildJsonArray { add(JsonPrimitive("object")); add(JsonPrimitive("null")) })
        put("properties", schema["properties"] ?: JsonObject(emptyMap()))
        put("required", schema["required"] ?: JsonArray(emptyList()))
        put("additionalProperties", JsonPrimitive(false))
    }

    private fun nullableArraySchema(itemSchema: JsonObject): JsonObject = buildJsonObject {
        put("type", buildJsonArray { add(JsonPrimitive("array")); add(JsonPrimitive("null")) })
        put("items", itemSchema)
    }
}
