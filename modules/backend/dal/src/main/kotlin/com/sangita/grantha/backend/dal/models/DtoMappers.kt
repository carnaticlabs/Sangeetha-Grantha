package com.sangita.grantha.backend.dal.models

import com.sangita.grantha.backend.dal.enums.BatchStatus
import com.sangita.grantha.backend.dal.enums.ImportStatus
import com.sangita.grantha.backend.dal.enums.JobType
import com.sangita.grantha.backend.dal.enums.LanguageCode
import com.sangita.grantha.backend.dal.enums.MusicalForm
import com.sangita.grantha.backend.dal.enums.ScriptCode
import com.sangita.grantha.backend.dal.enums.TaskStatus
import com.sangita.grantha.backend.dal.enums.WorkflowState
import com.sangita.grantha.backend.dal.support.toKotlinUuid
import com.sangita.grantha.backend.dal.tables.AuditLogTable
import com.sangita.grantha.backend.dal.tables.ComposersTable
import com.sangita.grantha.backend.dal.tables.DeitiesTable
import com.sangita.grantha.backend.dal.tables.ImportBatchTable
import com.sangita.grantha.backend.dal.tables.ImportEventTable
import com.sangita.grantha.backend.dal.tables.ImportJobTable
import com.sangita.grantha.backend.dal.tables.ImportSourcesTable
import com.sangita.grantha.backend.dal.tables.ImportTaskRunTable
import com.sangita.grantha.backend.dal.tables.ImportedKrithisTable
import com.sangita.grantha.backend.dal.tables.KrithisTable
import com.sangita.grantha.backend.dal.tables.KrithiNotationRowsTable
import com.sangita.grantha.backend.dal.tables.KrithiNotationVariantsTable
import com.sangita.grantha.backend.dal.tables.KrithiSectionsTable
import com.sangita.grantha.backend.dal.tables.KrithiLyricVariantsTable
import com.sangita.grantha.backend.dal.tables.KrithiLyricSectionsTable
import com.sangita.grantha.backend.dal.tables.RagasTable
import com.sangita.grantha.backend.dal.tables.SampradayasTable
import com.sangita.grantha.backend.dal.tables.TagsTable
import com.sangita.grantha.backend.dal.tables.TalasTable
import com.sangita.grantha.backend.dal.tables.TemplesTable
import com.sangita.grantha.backend.dal.tables.UsersTable
import com.sangita.grantha.backend.dal.tables.RoleAssignmentsTable
import com.sangita.grantha.shared.domain.model.ComposerDto
import com.sangita.grantha.shared.domain.model.DeityDto
import com.sangita.grantha.shared.domain.model.AuditLogDto
import com.sangita.grantha.shared.domain.model.BatchStatusDto
import com.sangita.grantha.shared.domain.model.ImportBatchDto
import com.sangita.grantha.shared.domain.model.ImportEventDto
import com.sangita.grantha.shared.domain.model.ImportJobDto
import com.sangita.grantha.shared.domain.model.ImportSourceDto
import com.sangita.grantha.shared.domain.model.ImportStatusDto
import com.sangita.grantha.shared.domain.model.ImportTaskRunDto
import com.sangita.grantha.shared.domain.model.ImportedKrithiDto
import com.sangita.grantha.shared.domain.model.JobTypeDto
import com.sangita.grantha.shared.domain.model.TaskStatusDto
import com.sangita.grantha.shared.domain.model.KrithiDto
import com.sangita.grantha.shared.domain.model.KrithiNotationRowDto
import com.sangita.grantha.shared.domain.model.KrithiNotationVariantDto
import com.sangita.grantha.shared.domain.model.KrithiSectionDto
import com.sangita.grantha.shared.domain.model.KrithiLyricVariantDto
import com.sangita.grantha.shared.domain.model.KrithiLyricSectionDto
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
import com.sangita.grantha.shared.domain.model.UserDto
import com.sangita.grantha.shared.domain.model.RoleAssignmentDto
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

fun BatchStatus.toDto(): BatchStatusDto = BatchStatusDto.valueOf(name)

fun JobType.toDto(): JobTypeDto = JobTypeDto.valueOf(name)

fun TaskStatus.toDto(): TaskStatusDto = TaskStatusDto.valueOf(name)

@OptIn(ExperimentalUuidApi::class)
fun ResultRow.toUserDto(): UserDto = UserDto(
    id = this[UsersTable.id].value.toKotlinUuid(),
    email = this[UsersTable.email],
    fullName = this[UsersTable.fullName],
    displayName = this[UsersTable.displayName],
    isActive = this[UsersTable.isActive],
    createdAt = this.kotlinInstant(UsersTable.createdAt),
    updatedAt = this.kotlinInstant(UsersTable.updatedAt)
)

@OptIn(ExperimentalUuidApi::class)
fun ResultRow.toRoleAssignmentDto(): RoleAssignmentDto = RoleAssignmentDto(
    userId = this[RoleAssignmentsTable.userId].toKotlinUuid(),
    roleCode = this[RoleAssignmentsTable.roleCode],
    assignedAt = this.kotlinInstant(RoleAssignmentsTable.assignedAt)
)

// Bulk Import Orchestration Mappers
@OptIn(ExperimentalUuidApi::class)
fun ResultRow.toImportBatchDto(): ImportBatchDto = ImportBatchDto(
    id = this[ImportBatchTable.id].value.toKotlinUuid(),
    sourceManifest = this[ImportBatchTable.sourceManifest],
    createdByUserId = this[ImportBatchTable.createdByUserId]?.toKotlinUuid(),
    status = this[ImportBatchTable.status].toDto(),
    totalTasks = this[ImportBatchTable.totalTasks],
    processedTasks = this[ImportBatchTable.processedTasks],
    succeededTasks = this[ImportBatchTable.succeededTasks],
    failedTasks = this[ImportBatchTable.failedTasks],
    blockedTasks = this[ImportBatchTable.blockedTasks],
    startedAt = this.kotlinInstantOrNull(ImportBatchTable.startedAt),
    completedAt = this.kotlinInstantOrNull(ImportBatchTable.completedAt),
    createdAt = this.kotlinInstant(ImportBatchTable.createdAt),
    updatedAt = this.kotlinInstant(ImportBatchTable.updatedAt)
)

@OptIn(ExperimentalUuidApi::class)
fun ResultRow.toImportJobDto(): ImportJobDto = ImportJobDto(
    id = this[ImportJobTable.id].value.toKotlinUuid(),
    batchId = this[ImportJobTable.batchId].toKotlinUuid(),
    jobType = this[ImportJobTable.jobType].toDto(),
    status = this[ImportJobTable.status].toDto(),
    retryCount = this[ImportJobTable.retryCount],
    payload = this[ImportJobTable.payload],
    result = this[ImportJobTable.result],
    startedAt = this.kotlinInstantOrNull(ImportJobTable.startedAt),
    completedAt = this.kotlinInstantOrNull(ImportJobTable.completedAt),
    createdAt = this.kotlinInstant(ImportJobTable.createdAt),
    updatedAt = this.kotlinInstant(ImportJobTable.updatedAt)
)

@OptIn(ExperimentalUuidApi::class)
fun ResultRow.toImportTaskRunDto(): ImportTaskRunDto = ImportTaskRunDto(
    id = this[ImportTaskRunTable.id].value.toKotlinUuid(),
    jobId = this[ImportTaskRunTable.jobId].toKotlinUuid(),
    krithiKey = this[ImportTaskRunTable.krithiKey],
    status = this[ImportTaskRunTable.status].toDto(),
    attempt = this[ImportTaskRunTable.attempt],
    sourceUrl = this[ImportTaskRunTable.sourceUrl],
    error = this[ImportTaskRunTable.error],
    durationMs = this[ImportTaskRunTable.durationMs],
    checksum = this[ImportTaskRunTable.checksum],
    evidencePath = this[ImportTaskRunTable.evidencePath],
    startedAt = this.kotlinInstantOrNull(ImportTaskRunTable.startedAt),
    completedAt = this.kotlinInstantOrNull(ImportTaskRunTable.completedAt),
    createdAt = this.kotlinInstant(ImportTaskRunTable.createdAt),
    updatedAt = this.kotlinInstant(ImportTaskRunTable.updatedAt)
)

@OptIn(ExperimentalUuidApi::class)
fun ResultRow.toImportEventDto(): ImportEventDto = ImportEventDto(
    id = this[ImportEventTable.id].value.toKotlinUuid(),
    refType = this[ImportEventTable.refType],
    refId = this[ImportEventTable.refId].toKotlinUuid(),
    eventType = this[ImportEventTable.eventType],
    data = this[ImportEventTable.data],
    createdAt = this.kotlinInstant(ImportEventTable.createdAt)
)
