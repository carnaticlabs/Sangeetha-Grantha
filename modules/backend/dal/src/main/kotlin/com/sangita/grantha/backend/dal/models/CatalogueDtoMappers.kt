package com.sangita.grantha.backend.dal.models

import com.sangita.grantha.shared.domain.model.LanguageCodeDto
import com.sangita.grantha.shared.domain.model.ScriptCodeDto
import com.sangita.grantha.shared.domain.model.catalogue.CatalogueVariantRefDto
import kotlin.uuid.Uuid

/**
 * Server default reading (TRACK-138): the unambiguous stored primary, otherwise a
 * stable language → script → variant UUID order. The mobile client overlays its
 * local script preference on the returned inventory; no preference query param.
 */
object CatalogueReadingDefaults {
    private val languageOrder = listOf(
        LanguageCodeDto.SA,
        LanguageCodeDto.TA,
        LanguageCodeDto.TE,
        LanguageCodeDto.KN,
        LanguageCodeDto.ML,
        LanguageCodeDto.HI,
        LanguageCodeDto.EN,
    )

    private val scriptOrder = listOf(
        ScriptCodeDto.TELUGU,
        ScriptCodeDto.TAMIL,
        ScriptCodeDto.KANNADA,
        ScriptCodeDto.MALAYALAM,
        ScriptCodeDto.DEVANAGARI,
        ScriptCodeDto.LATIN,
    )

    fun selectDefaultVariantId(variants: List<CatalogueVariantRefDto>): Uuid? {
        if (variants.isEmpty()) return null
        val primaries = variants.filter { it.isPrimary }
        if (primaries.size == 1) return primaries.single().id
        return variants.minWithOrNull(
            compareBy<CatalogueVariantRefDto> { languageRank(it.language) }
                .thenBy { scriptRank(it.script) }
                .thenBy { it.id.toString() },
        )?.id
    }

    private fun languageRank(language: LanguageCodeDto): Int {
        val index = languageOrder.indexOf(language)
        return if (index < 0) languageOrder.size else index
    }

    private fun scriptRank(script: ScriptCodeDto): Int {
        val index = scriptOrder.indexOf(script)
        return if (index < 0) scriptOrder.size else index
    }
}
