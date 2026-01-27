package com.sangita.grantha.shared.domain.model

import kotlinx.serialization.Serializable

@Serializable
enum class WorkflowStateDto { DRAFT, IN_REVIEW, PUBLISHED, ARCHIVED }

@Serializable
enum class LanguageCodeDto { SA, TA, TE, KN, ML, HI, EN }

@Serializable
enum class ScriptCodeDto { DEVANAGARI, TAMIL, TELUGU, KANNADA, MALAYALAM, LATIN }

@Serializable
enum class RagaSectionDto {
    PALLAVI, ANUPALLAVI, CHARANAM, SAMASHTI_CHARANAM,
    CHITTASWARAM, SWARA_SAHITYA, MADHYAMA_KALA,
    SOLKATTU_SWARA, ANUBANDHA, MUKTAYI_SWARA,
    ETTUGADA_SWARA, ETTUGADA_SAHITYA, VILOMA_CHITTASWARAM,
    OTHER
}

@Serializable
enum class MusicalFormDto { KRITHI, VARNAM, SWARAJATHI }

@Serializable
enum class NotationTypeDto { SWARA, JATHI }
