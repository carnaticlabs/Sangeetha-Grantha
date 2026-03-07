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
import com.sangita.grantha.backend.dal.tables.ComposersTable
import com.sangita.grantha.backend.dal.tables.DeitiesTable
import com.sangita.grantha.backend.dal.tables.RagasTable
import com.sangita.grantha.backend.dal.tables.RoleAssignmentsTable
import com.sangita.grantha.backend.dal.tables.SampradayasTable
import com.sangita.grantha.backend.dal.tables.TagsTable
import com.sangita.grantha.backend.dal.tables.TalasTable
import com.sangita.grantha.backend.dal.tables.TemplesTable
import com.sangita.grantha.backend.dal.tables.UsersTable
import com.sangita.grantha.shared.domain.model.BatchStatusDto
import com.sangita.grantha.shared.domain.model.ComposerDto
import com.sangita.grantha.shared.domain.model.DeityDto
import com.sangita.grantha.shared.domain.model.ImportStatusDto
import com.sangita.grantha.shared.domain.model.JobTypeDto
import com.sangita.grantha.shared.domain.model.LanguageCodeDto
import com.sangita.grantha.shared.domain.model.MusicalFormDto
import com.sangita.grantha.shared.domain.model.RagaDto
import com.sangita.grantha.shared.domain.model.RoleAssignmentDto
import com.sangita.grantha.shared.domain.model.SampradayaDto
import com.sangita.grantha.shared.domain.model.SampradayaTypeDto
import com.sangita.grantha.shared.domain.model.ScriptCodeDto
import com.sangita.grantha.shared.domain.model.TagCategoryDto
import com.sangita.grantha.shared.domain.model.TagDto
import com.sangita.grantha.shared.domain.model.TalaDto
import com.sangita.grantha.shared.domain.model.TaskStatusDto
import com.sangita.grantha.shared.domain.model.TempleDto
import com.sangita.grantha.shared.domain.model.UserDto
import com.sangita.grantha.shared.domain.model.WorkflowStateDto
import kotlin.uuid.ExperimentalUuidApi
import org.jetbrains.exposed.v1.core.ResultRow

// =============================================================================
// Core Entity Mappers: Composer, Raga, Tala, Deity, Temple, Tag, Sampradaya,
// User, Role + enum converters
// (Extracted from DtoMappers.kt as part of TRACK-073)
// =============================================================================

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

// --- Enum converters ---

fun WorkflowState.toDto(): WorkflowStateDto = WorkflowStateDto.valueOf(name)

fun LanguageCode.toDto(): LanguageCodeDto = LanguageCodeDto.valueOf(name)

fun ScriptCode.toDto(): ScriptCodeDto = ScriptCodeDto.valueOf(name)

fun ImportStatus.toDto(): ImportStatusDto = ImportStatusDto.valueOf(name)

fun MusicalForm.toDto(): MusicalFormDto = MusicalFormDto.valueOf(name)

fun BatchStatus.toDto(): BatchStatusDto = BatchStatusDto.valueOf(name)

fun JobType.toDto(): JobTypeDto = JobTypeDto.valueOf(name)

fun TaskStatus.toDto(): TaskStatusDto = TaskStatusDto.valueOf(name)
