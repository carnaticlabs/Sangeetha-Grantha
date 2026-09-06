package com.sangita.grantha.shared.domain.model.catalogue

import com.sangita.grantha.shared.domain.model.LanguageCodeDto
import com.sangita.grantha.shared.domain.model.MusicalFormDto
import com.sangita.grantha.shared.domain.model.RagaSectionDto
import com.sangita.grantha.shared.domain.model.ScriptCodeDto
import com.sangita.grantha.shared.domain.serialization.UuidSerializer
import kotlinx.serialization.Serializable
import kotlin.uuid.Uuid

/**
 * Public Rasika catalogue contract (TRACK-138). These DTOs are the mobile/OpenAPI
 * allowlist — do not reuse editorial [com.sangita.grantha.shared.domain.model.KrithiDto]
 * fields such as notes, author IDs or workflow state.
 */
object CatalogueContract {
    const val QUERY_MAX_CODE_POINTS: Int = 200
    const val DEFAULT_PAGE: Int = 0
    const val DEFAULT_PAGE_SIZE: Int = 30
    const val MAX_PAGE_SIZE: Int = 100
    const val BOOKMARK_LABEL_MAX_CODE_POINTS: Int = 160
    const val SESSION_INACTIVITY_MS: Long = 30L * 60L * 1000L
    const val SESSION_HEADER: String = "X-Rasika-Session-ID"
    const val INTERACTION_HEADER: String = "X-Rasika-Interaction-ID"
    const val KRITHIS_PATH: String = "/v1/catalogue/krithis"
    const val RAGAS_PATH: String = "/v1/catalogue/ragas"
    const val COMPOSERS_PATH: String = "/v1/catalogue/composers"
}

@Serializable
enum class CatalogueCompletenessDto {
    UNKNOWN,
    PARTIAL,
    COMPLETE,
}

@Serializable
enum class CatalogueErrorCodeDto {
    VALIDATION_ERROR,
    NOT_FOUND,
    UNAVAILABLE,
}

@Serializable
data class CatalogueErrorDto(
    val code: CatalogueErrorCodeDto,
    val message: String,
)

@Serializable
data class CataloguePagedResponse<T>(
    val items: List<T>,
    val total: Long,
    val page: Int,
    val pageSize: Int,
)

@Serializable
data class CatalogueComposerRefDto(
    @Serializable(with = UuidSerializer::class)
    val id: Uuid,
    val name: String,
)

@Serializable
data class CatalogueTalaRefDto(
    @Serializable(with = UuidSerializer::class)
    val id: Uuid,
    val name: String,
)

@Serializable
data class CatalogueRagaRefDto(
    @Serializable(with = UuidSerializer::class)
    val id: Uuid,
    val name: String,
    val orderIndex: Int,
    val section: RagaSectionDto? = null,
)

@Serializable
data class CatalogueKrithiSummaryDto(
    @Serializable(with = UuidSerializer::class)
    val id: Uuid,
    val title: String,
    val incipit: String? = null,
    val composer: CatalogueComposerRefDto,
    val ragas: List<CatalogueRagaRefDto>,
    val tala: CatalogueTalaRefDto? = null,
    val musicalForm: MusicalFormDto = MusicalFormDto.KRITHI,
    val isRagamalika: Boolean = false,
)

@Serializable
data class CatalogueVariantRefDto(
    @Serializable(with = UuidSerializer::class)
    val id: Uuid,
    val language: LanguageCodeDto,
    val script: ScriptCodeDto,
    val transliterationScheme: String? = null,
    val isPrimary: Boolean = false,
    val label: String? = null,
    val sourceReference: String? = null,
)

@Serializable
data class CatalogueKrithiReaderDto(
    @Serializable(with = UuidSerializer::class)
    val id: Uuid,
    val title: String,
    val incipit: String? = null,
    val composer: CatalogueComposerRefDto,
    val ragas: List<CatalogueRagaRefDto>,
    val tala: CatalogueTalaRefDto? = null,
    val musicalForm: MusicalFormDto = MusicalFormDto.KRITHI,
    val originalLanguage: LanguageCodeDto,
    val isRagamalika: Boolean = false,
    @Serializable(with = UuidSerializer::class)
    val defaultVariantId: Uuid? = null,
    val variants: List<CatalogueVariantRefDto> = emptyList(),
    val completeness: CatalogueCompletenessDto = CatalogueCompletenessDto.UNKNOWN,
)

@Serializable
data class CatalogueLyricSectionDto(
    @Serializable(with = UuidSerializer::class)
    val sectionId: Uuid,
    val sectionType: String,
    val label: String? = null,
    val orderIndex: Int,
    val text: String,
)

@Serializable
data class CatalogueLyricsDto(
    @Serializable(with = UuidSerializer::class)
    val variantId: Uuid,
    @Serializable(with = UuidSerializer::class)
    val krithiId: Uuid,
    val language: LanguageCodeDto,
    val script: ScriptCodeDto,
    val transliterationScheme: String? = null,
    val isPrimary: Boolean = false,
    val label: String? = null,
    val sourceReference: String? = null,
    val unsegmentedText: String? = null,
    val sections: List<CatalogueLyricSectionDto> = emptyList(),
)

@Serializable
data class CatalogueNomenclatureLinkDto(
    @Serializable(with = UuidSerializer::class)
    val relatedRagaId: Uuid,
    val relatedRagaName: String,
    val relationLabel: String,
)

@Serializable
data class CatalogueRagaSummaryDto(
    @Serializable(with = UuidSerializer::class)
    val id: Uuid,
    val name: String,
    val matchingAliases: List<String> = emptyList(),
    val publishedCompositionCount: Long,
    val melakartaNumber: Int? = null,
    val parentRagaName: String? = null,
)

@Serializable
data class CatalogueRagaDetailDto(
    @Serializable(with = UuidSerializer::class)
    val id: Uuid,
    val name: String,
    val aliases: List<String> = emptyList(),
    val publishedCompositionCount: Long,
    val melakartaNumber: Int? = null,
    @Serializable(with = UuidSerializer::class)
    val parentRagaId: Uuid? = null,
    val parentRagaName: String? = null,
    val arohanam: String? = null,
    val avarohanam: String? = null,
    val nomenclatureLinks: List<CatalogueNomenclatureLinkDto> = emptyList(),
)

@Serializable
data class CatalogueComposerSummaryDto(
    @Serializable(with = UuidSerializer::class)
    val id: Uuid,
    val name: String,
    val matchingAliases: List<String> = emptyList(),
    val publishedCompositionCount: Long,
)

@Serializable
data class CatalogueComposerDetailDto(
    @Serializable(with = UuidSerializer::class)
    val id: Uuid,
    val name: String,
    val aliases: List<String> = emptyList(),
    val publishedCompositionCount: Long,
    val birthYear: Int? = null,
    val deathYear: Int? = null,
    val place: String? = null,
)
