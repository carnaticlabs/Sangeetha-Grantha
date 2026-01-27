package com.sangita.grantha.backend.api.routes

import com.sangita.grantha.backend.api.support.toKotlinUuidOrNull
import com.sangita.grantha.shared.domain.model.LanguageCodeDto
import kotlin.uuid.Uuid

fun parseUuidParam(value: String?, label: String): Uuid? {
    return value.toKotlinUuidOrNull(label)
}

fun parseLanguageParam(value: String?, label: String): LanguageCodeDto? {
    if (value.isNullOrBlank()) return null
    require(value.isNotBlank()) { "$label must not be blank" }
    return runCatching { LanguageCodeDto.valueOf(value.uppercase()) }
        .getOrElse { throw IllegalArgumentException("Invalid $label: must be a valid language code") }
}
