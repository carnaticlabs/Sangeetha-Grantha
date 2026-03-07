package com.sangita.grantha.backend.dal.models

import com.sangita.grantha.backend.dal.support.toKotlinUuid
import com.sangita.grantha.backend.dal.tables.KrithisTable
import com.sangita.grantha.backend.dal.tables.KrithiNotationRowsTable
import com.sangita.grantha.backend.dal.tables.KrithiNotationVariantsTable
import com.sangita.grantha.backend.dal.tables.KrithiSectionsTable
import com.sangita.grantha.backend.dal.tables.KrithiLyricVariantsTable
import com.sangita.grantha.backend.dal.tables.KrithiLyricSectionsTable
import com.sangita.grantha.shared.domain.model.KrithiDto
import com.sangita.grantha.shared.domain.model.KrithiNotationRowDto
import com.sangita.grantha.shared.domain.model.KrithiNotationVariantDto
import com.sangita.grantha.shared.domain.model.KrithiSectionDto
import com.sangita.grantha.shared.domain.model.KrithiLyricVariantDto
import com.sangita.grantha.shared.domain.model.KrithiLyricSectionDto
import com.sangita.grantha.shared.domain.model.NotationTypeDto
import kotlin.uuid.ExperimentalUuidApi
import org.jetbrains.exposed.v1.core.ResultRow

// =============================================================================
// Krithi Domain Mappers: Krithi, Notation, Section, LyricVariant, LyricSection
// (Extracted from DtoMappers.kt as part of TRACK-073)
// =============================================================================

@OptIn(ExperimentalUuidApi::class)
fun ResultRow.toKrithiDto(): KrithiDto = KrithiDto(
    id = this[KrithisTable.id].value.toKotlinUuid(),
    title = this[KrithisTable.title],
    incipit = this[KrithisTable.incipit],
    titleNormalized = this[KrithisTable.titleNormalized],
    incipitNormalized = this[KrithisTable.incipitNormalized],
    composerId = this[KrithisTable.composerId].toKotlinUuid(),
    primaryRagaId = this[KrithisTable.primaryRagaId]?.toKotlinUuid(),
    talaId = this[KrithisTable.talaId]?.toKotlinUuid(),
    deityId = this[KrithisTable.deityId]?.toKotlinUuid(),
    templeId = this[KrithisTable.templeId]?.toKotlinUuid(),
    musicalForm = this[KrithisTable.musicalForm].toDto(),
    primaryLanguage = this[KrithisTable.primaryLanguage].toDto(),
    isRagamalika = this[KrithisTable.isRagamalika],
    workflowState = this[KrithisTable.workflowState].toDto(),
    sahityaSummary = this[KrithisTable.sahityaSummary],
    notes = this[KrithisTable.notes],
    createdByUserId = this[KrithisTable.createdByUserId]?.toKotlinUuid(),
    updatedByUserId = this[KrithisTable.updatedByUserId]?.toKotlinUuid(),
    createdAt = this.kotlinInstant(KrithisTable.createdAt),
    updatedAt = this.kotlinInstant(KrithisTable.updatedAt)
)

@OptIn(ExperimentalUuidApi::class)
fun ResultRow.toKrithiNotationVariantDto(): KrithiNotationVariantDto = KrithiNotationVariantDto(
    id = this[KrithiNotationVariantsTable.id].value.toKotlinUuid(),
    krithiId = this[KrithiNotationVariantsTable.krithiId].toKotlinUuid(),
    notationType = NotationTypeDto.valueOf(this[KrithiNotationVariantsTable.notationType]),
    talaId = this[KrithiNotationVariantsTable.talaId]?.toKotlinUuid(),
    kalai = this[KrithiNotationVariantsTable.kalai],
    eduppuOffsetBeats = this[KrithiNotationVariantsTable.eduppuOffsetBeats],
    variantLabel = this[KrithiNotationVariantsTable.variantLabel],
    sourceReference = this[KrithiNotationVariantsTable.sourceReference],
    isPrimary = this[KrithiNotationVariantsTable.isPrimary],
    createdAt = this.kotlinInstant(KrithiNotationVariantsTable.createdAt),
    updatedAt = this.kotlinInstant(KrithiNotationVariantsTable.updatedAt)
)

@OptIn(ExperimentalUuidApi::class)
fun ResultRow.toKrithiNotationRowDto(): KrithiNotationRowDto = KrithiNotationRowDto(
    id = this[KrithiNotationRowsTable.id].value.toKotlinUuid(),
    notationVariantId = this[KrithiNotationRowsTable.notationVariantId].toKotlinUuid(),
    sectionId = this[KrithiNotationRowsTable.sectionId].toKotlinUuid(),
    orderIndex = this[KrithiNotationRowsTable.orderIndex],
    swaraText = this[KrithiNotationRowsTable.swaraText],
    sahityaText = this[KrithiNotationRowsTable.sahityaText],
    talaMarkers = this[KrithiNotationRowsTable.talaMarkers],
    createdAt = this.kotlinInstant(KrithiNotationRowsTable.createdAt),
    updatedAt = this.kotlinInstant(KrithiNotationRowsTable.updatedAt)
)

@OptIn(ExperimentalUuidApi::class)
fun ResultRow.toKrithiSectionDto(): KrithiSectionDto = KrithiSectionDto(
    id = this[KrithiSectionsTable.id].value.toKotlinUuid(),
    krithiId = this[KrithiSectionsTable.krithiId].toKotlinUuid(),
    sectionType = this[KrithiSectionsTable.sectionType],
    orderIndex = this[KrithiSectionsTable.orderIndex],
    label = this[KrithiSectionsTable.label],
    notes = this[KrithiSectionsTable.notes],
    createdAt = this.kotlinInstant(KrithiSectionsTable.createdAt),
    updatedAt = this.kotlinInstant(KrithiSectionsTable.updatedAt)
)

@OptIn(ExperimentalUuidApi::class)
fun ResultRow.toKrithiLyricVariantDto(): KrithiLyricVariantDto = KrithiLyricVariantDto(
    id = this[KrithiLyricVariantsTable.id].value.toKotlinUuid(),
    krithiId = this[KrithiLyricVariantsTable.krithiId].toKotlinUuid(),
    language = this[KrithiLyricVariantsTable.language].toDto(),
    script = this[KrithiLyricVariantsTable.script].toDto(),
    transliterationScheme = this[KrithiLyricVariantsTable.transliterationScheme],
    isPrimary = this[KrithiLyricVariantsTable.isPrimary],
    variantLabel = this[KrithiLyricVariantsTable.variantLabel],
    sourceReference = this[KrithiLyricVariantsTable.sourceReference],
    lyrics = this[KrithiLyricVariantsTable.lyrics],
    createdByUserId = this[KrithiLyricVariantsTable.createdByUserId]?.toKotlinUuid(),
    updatedByUserId = this[KrithiLyricVariantsTable.updatedByUserId]?.toKotlinUuid(),
    createdAt = this.kotlinInstant(KrithiLyricVariantsTable.createdAt),
    updatedAt = this.kotlinInstant(KrithiLyricVariantsTable.updatedAt)
)

@OptIn(ExperimentalUuidApi::class)
fun ResultRow.toKrithiLyricSectionDto(): KrithiLyricSectionDto = KrithiLyricSectionDto(
    id = this[KrithiLyricSectionsTable.id].value.toKotlinUuid(),
    lyricVariantId = this[KrithiLyricSectionsTable.lyricVariantId].toKotlinUuid(),
    sectionId = this[KrithiLyricSectionsTable.sectionId].toKotlinUuid(),
    text = this[KrithiLyricSectionsTable.text],
    normalizedText = this[KrithiLyricSectionsTable.normalizedText],
    createdAt = this.kotlinInstant(KrithiLyricSectionsTable.createdAt),
    updatedAt = this.kotlinInstant(KrithiLyricSectionsTable.updatedAt)
)
