package com.sangita.grantha.backend.dal

import com.sangita.grantha.backend.dal.models.CatalogueReadingDefaults
import com.sangita.grantha.shared.domain.model.LanguageCodeDto
import com.sangita.grantha.shared.domain.model.ScriptCodeDto
import com.sangita.grantha.shared.domain.model.catalogue.CatalogueVariantRefDto
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.uuid.Uuid

class CatalogueReadingDefaultsTest {
    @Test
    fun `empty inventory has no default`() {
        assertNull(CatalogueReadingDefaults.selectDefaultVariantId(emptyList()))
    }

    @Test
    fun `unambiguous primary wins`() {
        val primary = variant("11111111-1111-4111-8111-111111111111", LanguageCodeDto.EN, ScriptCodeDto.LATIN, true)
        val other = variant("22222222-2222-4222-8222-222222222222", LanguageCodeDto.SA, ScriptCodeDto.TELUGU, false)
        assertEquals(primary.id, CatalogueReadingDefaults.selectDefaultVariantId(listOf(other, primary)))
    }

    @Test
    fun `ambiguous primaries fall through to language then script then uuid`() {
        val later = variant("bbbbbbbb-bbbb-4bbb-8bbb-bbbbbbbbbbbb", LanguageCodeDto.SA, ScriptCodeDto.LATIN, true)
        val earlier = variant("aaaaaaaa-aaaa-4aaa-8aaa-aaaaaaaaaaaa", LanguageCodeDto.SA, ScriptCodeDto.TELUGU, true)
        assertEquals(earlier.id, CatalogueReadingDefaults.selectDefaultVariantId(listOf(later, earlier)))
    }

    @Test
    fun `no primary uses language order`() {
        val english = variant("11111111-1111-4111-8111-111111111111", LanguageCodeDto.EN, ScriptCodeDto.TELUGU, false)
        val sanskrit = variant("22222222-2222-4222-8222-222222222222", LanguageCodeDto.SA, ScriptCodeDto.LATIN, false)
        assertEquals(sanskrit.id, CatalogueReadingDefaults.selectDefaultVariantId(listOf(english, sanskrit)))
    }

    private fun variant(
        id: String,
        language: LanguageCodeDto,
        script: ScriptCodeDto,
        primary: Boolean,
    ) = CatalogueVariantRefDto(
        id = Uuid.parse(id),
        language = language,
        script = script,
        isPrimary = primary,
    )
}
