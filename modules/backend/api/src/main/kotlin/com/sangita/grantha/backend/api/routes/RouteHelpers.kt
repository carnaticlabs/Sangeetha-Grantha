package com.sangita.grantha.backend.api.routes

import com.sangita.grantha.shared.domain.model.LanguageCodeDto
import kotlin.uuid.Uuid

fun parseUuidParam(value: String?, label: String): Uuid? {
    if (value.isNullOrBlank()) return null
    return try {
        Uuid.parse(value)
    } catch (ex: IllegalArgumentException) {
        throw IllegalArgumentException("Invalid $label")
    }
}

fun parseLanguageParam(value: String?, label: String): LanguageCodeDto? {
    if (value.isNullOrBlank()) return null
    return try {
        LanguageCodeDto.valueOf(value.uppercase())
    } catch (ex: IllegalArgumentException) {
        throw IllegalArgumentException("Invalid $label")
    }
}
