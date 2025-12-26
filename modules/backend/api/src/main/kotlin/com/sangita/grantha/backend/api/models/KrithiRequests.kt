package com.sangita.grantha.backend.api.models

import com.sangita.grantha.shared.domain.model.LanguageCodeDto
import com.sangita.grantha.shared.domain.model.WorkflowStateDto
import kotlinx.serialization.Serializable

@Serializable
data class KrithiCreateRequest(
    val title: String,
    val composerId: String,
    val primaryLanguage: LanguageCodeDto,
    val talaId: String? = null,
    val primaryRagaId: String? = null,
    val deityId: String? = null,
    val templeId: String? = null,
    val isRagamalika: Boolean = false,
    val ragaIds: List<String> = emptyList(),
    val sahityaSummary: String? = null,
    val notes: String? = null,
)

@Serializable
data class KrithiUpdateRequest(
    val title: String? = null,
    val composerId: String? = null,
    val primaryLanguage: LanguageCodeDto? = null,
    val talaId: String? = null,
    val primaryRagaId: String? = null,
    val deityId: String? = null,
    val templeId: String? = null,
    val isRagamalika: Boolean? = null,
    val ragaIds: List<String>? = null,
    val sahityaSummary: String? = null,
    val notes: String? = null,
    val workflowState: WorkflowStateDto? = null,
)
