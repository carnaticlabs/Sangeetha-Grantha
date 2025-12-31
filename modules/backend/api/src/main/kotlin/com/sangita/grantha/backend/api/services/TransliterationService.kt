package com.sangita.grantha.backend.api.services

import com.sangita.grantha.backend.api.clients.GeminiApiClient

class TransliterationService(private val geminiClient: GeminiApiClient) {

    suspend fun transliterate(content: String, sourceScript: String?, targetScript: String): String {
        val prompt = """
            You are an expert in Carnatic music notation and Indian scripts.
            Transliterate the following text to $targetScript.
            Source script: ${sourceScript ?: "Detect automatically"}
            
            Content to transliterate:
            START_CONTENT
            $content
            END_CONTENT
            
            Strict Requirements:
            1. Preserve strict formatting, spacing, and alignment (this is critical for music notation).
            2. Do not translate the meaning, only transliterate the script.
            3. For Carnatic notation (S R G M P D N), use the standard symbols appropriate for the target script.
            4. Keep non-text markers, punctuation, and layout exactly as is.
            5. If the target is ISO-15919 or Latin, follow standard diacritical conventions for Carnatic music.
            
            Return ONLY the transliterated text. Do not include markdown code blocks or explanations.
        """.trimIndent()
        
        return geminiClient.generateContent(prompt).trim()
    }
}
