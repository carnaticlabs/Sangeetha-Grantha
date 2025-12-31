package com.sangita.grantha.backend.dal.models

import com.sangita.grantha.backend.dal.enums.ImportStatus
import com.sangita.grantha.backend.dal.enums.LanguageCode
import com.sangita.grantha.backend.dal.enums.MusicalForm
import com.sangita.grantha.backend.dal.enums.ScriptCode
import com.sangita.grantha.backend.dal.enums.WorkflowState
import com.sangita.grantha.backend.dal.support.toKotlinUuid
import com.sangita.grantha.backend.dal.tables.AuditLogTable
import com.sangita.grantha.backend.dal.tables.ComposersTable
import com.sangita.grantha.backend.dal.tables.DeitiesTable
import com.sangita.grantha.backend.dal.tables.ImportSourcesTable
import com.sangita.grantha.backend.dal.tables.ImportedKrithisTable
import com.sangita.grantha.backend.dal.tables.KrithisTable
import com.sangita.grantha.backend.dal.tables.KrithiNotationRowsTable
import com.sangita.grantha.backend.dal.tables.KrithiNotationVariantsTable
import com.sangita.grantha.backend.dal.tables.RagasTable
import com.sangita.grantha.backend.dal.tables.SampradayasTable
import com.sangita.grantha.backend.dal.tables.TagsTable
import com.sangita.grantha.backend.dal.tables.TalasTable
import com.sangita.grantha.backend.dal.tables.TemplesTable
import com.sangita.grantha.shared.domain.model.ComposerDto
import com.sangita.grantha.shared.domain.model.DeityDto
import com.sangita.grantha.shared.domain.model.AuditLogDto
import com.sangita.grantha.shared.domain.model.ImportSourceDto
import com.sangita.grantha.shared.domain.model.ImportStatusDto
import com.sangita.grantha.shared.domain.model.ImportedKrithiDto
import com.sangita.grantha.shared.domain.model.KrithiDto
import com.sangita.grantha.shared.domain.model.KrithiNotationRowDto
import com.sangita.grantha.shared.domain.model.KrithiNotationVariantDto
import com.sangita.grantha.shared.domain.model.LanguageCodeDto
import com.sangita.grantha.shared.domain.model.MusicalFormDto
import com.sangita.grantha.shared.domain.model.NotationTypeDto
import com.sangita.grantha.shared.domain.model.RagaDto
import com.sangita.grantha.shared.domain.model.SampradayaDto
import com.sangita.grantha.shared.domain.model.SampradayaTypeDto
import com.sangita.grantha.shared.domain.model.ScriptCodeDto
import com.sangita.grantha.shared.domain.model.TagCategoryDto
import com.sangita.grantha.shared.domain.model.TagDto
import com.sangita.grantha.shared.domain.model.TalaDto
import com.sangita.grantha.shared.domain.model.TempleDto
import com.sangita.grantha.shared.domain.model.WorkflowStateDto
import kotlin.uuid.ExperimentalUuidApi
import org.jetbrains.exposed.v1.core.ResultRow

@OptIn(ExperimentalUuidApi::class)
fun ResultRow.toComposerDto(): ComposerDto = ComposerDto(
    id = this[ComposersTable.id].value.toKotlinUuid(),
    name = this[ComposersTable.name],
    nameNormalized = this[ComposersTable.nameNormalized],
    birthYear = this[ComposersTable.birthYear],
    deathYear = this[ComposersTable.deathYear],
    place = this[ComposersTable.place],
    notes = this[ComposersTable.notes],
    createdAt = this.kotlinInstant(ComposersTable.createdAt),
    updatedAt = this.kotlinInstant(ComposersTable.updatedAt)
)

@OptIn(ExperimentalUuidApi::class)
fun ResultRow.toRagaDto(): RagaDto = RagaDto(
    id = this[RagasTable.id].value.toKotlinUuid(),
    name = this[RagasTable.name],
    nameNormalized = this[RagasTable.nameNormalized],
    melakartaNumber = this[RagasTable.melakartaNumber],
    parentRagaId = this[RagasTable.parentRagaId]?.toKotlinUuid(),
    arohanam = this[RagasTable.arohanam],
    avarohanam = this[RagasTable.avarohanam],
    notes = this[RagasTable.notes],
    createdAt = this.kotlinInstant(RagasTable.createdAt),
    updatedAt = this.kotlinInstant(RagasTable.updatedAt)
)

@OptIn(ExperimentalUuidApi::class)
fun ResultRow.toTalaDto(): TalaDto = TalaDto(
    id = this[TalasTable.id].value.toKotlinUuid(),
    name = this[TalasTable.name],
    nameNormalized = this[TalasTable.nameNormalized],
    angaStructure = this[TalasTable.angaStructure],
    beatCount = this[TalasTable.beatCount],
    notes = this[TalasTable.notes],
    createdAt = this.kotlinInstant(TalasTable.createdAt),
    updatedAt = this.kotlinInstant(TalasTable.updatedAt)
)

@OptIn(ExperimentalUuidApi::class)
fun ResultRow.toDeityDto(): DeityDto = DeityDto(
    id = this[DeitiesTable.id].value.toKotlinUuid(),
    name = this[DeitiesTable.name],
    nameNormalized = this[DeitiesTable.nameNormalized],
    description = this[DeitiesTable.description],
    createdAt = this.kotlinInstant(DeitiesTable.createdAt),
    updatedAt = this.kotlinInstant(DeitiesTable.updatedAt)
)

@OptIn(ExperimentalUuidApi::class)
fun ResultRow.toTempleDto(): TempleDto = TempleDto(
    id = this[TemplesTable.id].value.toKotlinUuid(),
    name = this[TemplesTable.name],
    nameNormalized = this[TemplesTable.nameNormalized],
    city = this[TemplesTable.city],
    state = this[TemplesTable.state],
    country = this[TemplesTable.country],
    primaryDeityId = this[TemplesTable.primaryDeityId]?.toKotlinUuid(),
    latitude = this[TemplesTable.latitude],
    longitude = this[TemplesTable.longitude],
    notes = this[TemplesTable.notes],
    createdAt = this.kotlinInstant(TemplesTable.createdAt),
    updatedAt = this.kotlinInstant(TemplesTable.updatedAt)
)

@OptIn(ExperimentalUuidApi::class)
fun ResultRow.toTagDto(): TagDto = TagDto(
    id = this[TagsTable.id].value.toKotlinUuid(),
    category = TagCategoryDto.valueOf(this[TagsTable.category]),
    slug = this[TagsTable.slug],
    displayNameEn = this[TagsTable.displayNameEn],
    descriptionEn = this[TagsTable.descriptionEn],
    createdAt = this.kotlinInstant(TagsTable.createdAt)
)

@OptIn(ExperimentalUuidApi::class)
fun ResultRow.toSampradayaDto(): SampradayaDto = SampradayaDto(
    id = this[SampradayasTable.id].value.toKotlinUuid(),
    name = this[SampradayasTable.name],
    type = SampradayaTypeDto.valueOf(this[SampradayasTable.type]),
    description = this[SampradayasTable.description],
    createdAt = this.kotlinInstant(SampradayasTable.createdAt)
)

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
fun ResultRow.toImportSourceDto(): ImportSourceDto = ImportSourceDto(
    id = this[ImportSourcesTable.id].value.toKotlinUuid(),
    name = this[ImportSourcesTable.name],
    baseUrl = this[ImportSourcesTable.baseUrl],
    description = this[ImportSourcesTable.description],
    contactInfo = this[ImportSourcesTable.contactInfo],
    createdAt = this.kotlinInstant(ImportSourcesTable.createdAt)
)

@OptIn(ExperimentalUuidApi::class)
fun ResultRow.toImportedKrithiDto(): ImportedKrithiDto = ImportedKrithiDto(
    id = this[ImportedKrithisTable.id].value.toKotlinUuid(),
    importSourceId = this[ImportedKrithisTable.importSourceId].toKotlinUuid(),
    sourceKey = this[ImportedKrithisTable.sourceKey],
    rawTitle = this[ImportedKrithisTable.rawTitle],
    rawLyrics = this[ImportedKrithisTable.rawLyrics],
    rawComposer = this[ImportedKrithisTable.rawComposer],
    rawRaga = this[ImportedKrithisTable.rawRaga],
    rawTala = this[ImportedKrithisTable.rawTala],
    rawDeity = this[ImportedKrithisTable.rawDeity],
    rawTemple = this[ImportedKrithisTable.rawTemple],
    rawLanguage = this[ImportedKrithisTable.rawLanguage],
    parsedPayload = this[ImportedKrithisTable.parsedPayload],
    importStatus = this[ImportedKrithisTable.importStatus].toDto(),
    mappedKrithiId = this[ImportedKrithisTable.mappedKrithiId]?.toKotlinUuid(),
    reviewerUserId = this[ImportedKrithisTable.reviewerUserId]?.toKotlinUuid(),
    reviewerNotes = this[ImportedKrithisTable.reviewerNotes],
    reviewedAt = this.kotlinInstantOrNull(ImportedKrithisTable.reviewedAt),
    createdAt = this.kotlinInstant(ImportedKrithisTable.createdAt)
)

@OptIn(ExperimentalUuidApi::class)
fun ResultRow.toAuditLogDto(): AuditLogDto = AuditLogDto(
    id = this[AuditLogTable.id].value.toKotlinUuid(),
    actorUserId = this[AuditLogTable.actorUserId]?.toKotlinUuid(),
    actorIp = this[AuditLogTable.actorIp],
    action = this[AuditLogTable.action],
    entityTable = this[AuditLogTable.entityTable],
    entityId = this[AuditLogTable.entityId]?.toKotlinUuid(),
    changedAt = this.kotlinInstant(AuditLogTable.changedAt),
    diff = this[AuditLogTable.diff],
    metadata = this[AuditLogTable.metadata]
)

fun WorkflowState.toDto(): WorkflowStateDto = WorkflowStateDto.valueOf(name)

fun LanguageCode.toDto(): LanguageCodeDto = LanguageCodeDto.valueOf(name)

fun ScriptCode.toDto(): ScriptCodeDto = ScriptCodeDto.valueOf(name)

fun ImportStatus.toDto(): ImportStatusDto = ImportStatusDto.valueOf(name)

fun MusicalForm.toDto(): MusicalFormDto = MusicalFormDto.valueOf(name)
