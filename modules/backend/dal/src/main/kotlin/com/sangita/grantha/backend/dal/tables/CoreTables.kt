package com.sangita.grantha.backend.dal.tables

import com.sangita.grantha.backend.dal.enums.ImportStatus
import com.sangita.grantha.backend.dal.enums.LanguageCode
import com.sangita.grantha.backend.dal.enums.RagaSection
import com.sangita.grantha.backend.dal.enums.ScriptCode
import com.sangita.grantha.backend.dal.enums.WorkflowState
import com.sangita.grantha.backend.dal.support.jsonbText
import com.sangita.grantha.backend.dal.support.pgEnum
import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.core.dao.id.UUIDTable
import org.jetbrains.exposed.v1.javatime.timestampWithTimeZone

object RolesTable : Table("roles") {
    val code = text("code")
    val name = text("name")
    val capabilities = jsonbText("capabilities")

    override val primaryKey = PrimaryKey(code)
}

object UsersTable : UUIDTable("users") {
    val email = text("email").nullable()
    val fullName = text("full_name")
    val displayName = text("display_name").nullable()
    val passwordHash = text("password_hash").nullable()
    val isActive = bool("is_active")
    val createdAt = timestampWithTimeZone("created_at")
    val updatedAt = timestampWithTimeZone("updated_at")
}

object RoleAssignmentsTable : Table("role_assignments") {
    val userId = uuid("user_id")
    val roleCode = text("role_code")
    val assignedAt = timestampWithTimeZone("assigned_at")

    override val primaryKey = PrimaryKey(userId, roleCode)
}

object ComposersTable : UUIDTable("composers") {
    val name = text("name")
    val nameNormalized = text("name_normalized")
    val birthYear = integer("birth_year").nullable()
    val deathYear = integer("death_year").nullable()
    val place = text("place").nullable()
    val notes = text("notes").nullable()
    val createdAt = timestampWithTimeZone("created_at")
    val updatedAt = timestampWithTimeZone("updated_at")
}

object RagasTable : UUIDTable("ragas") {
    val name = text("name")
    val nameNormalized = text("name_normalized")
    val melakartaNumber = integer("melakarta_number").nullable()
    val parentRagaId = uuid("parent_raga_id").nullable()
    val arohanam = text("arohanam").nullable()
    val avarohanam = text("avarohanam").nullable()
    val notes = text("notes").nullable()
    val createdAt = timestampWithTimeZone("created_at")
    val updatedAt = timestampWithTimeZone("updated_at")
}

object TalasTable : UUIDTable("talas") {
    val name = text("name")
    val nameNormalized = text("name_normalized")
    val angaStructure = text("anga_structure").nullable()
    val beatCount = integer("beat_count").nullable()
    val notes = text("notes").nullable()
    val createdAt = timestampWithTimeZone("created_at")
    val updatedAt = timestampWithTimeZone("updated_at")
}

object DeitiesTable : UUIDTable("deities") {
    val name = text("name")
    val nameNormalized = text("name_normalized")
    val description = text("description").nullable()
    val createdAt = timestampWithTimeZone("created_at")
    val updatedAt = timestampWithTimeZone("updated_at")
}

object TemplesTable : UUIDTable("temples") {
    val name = text("name")
    val nameNormalized = text("name_normalized")
    val city = text("city").nullable()
    val state = text("state").nullable()
    val country = text("country").nullable()
    val primaryDeityId = uuid("primary_deity_id").nullable()
    val latitude = double("latitude").nullable()
    val longitude = double("longitude").nullable()
    val notes = text("notes").nullable()
    val createdAt = timestampWithTimeZone("created_at")
    val updatedAt = timestampWithTimeZone("updated_at")
}

object TempleNamesTable : UUIDTable("temple_names") {
    val templeId = uuid("temple_id")
    val languageCode = pgEnum<LanguageCode>("language_code", LanguageCode.DB_TYPE)
    val scriptCode = pgEnum<ScriptCode>("script_code", ScriptCode.DB_TYPE)
    val name = text("name")
    val normalizedName = text("normalized_name")
    val isPrimary = bool("is_primary")
    val sourceInfo = text("source")
    val createdAt = timestampWithTimeZone("created_at")
}

object TagsTable : UUIDTable("tags") {
    val category = text("category")
    val slug = text("slug")
    val displayNameEn = text("display_name_en")
    val descriptionEn = text("description_en").nullable()
    val createdAt = timestampWithTimeZone("created_at")
}

object SampradayasTable : UUIDTable("sampradayas") {
    val name = text("name")
    val type = text("type")
    val description = text("description").nullable()
    val createdAt = timestampWithTimeZone("created_at")
}

object KrithisTable : UUIDTable("krithis") {
    val title = text("title")
    val incipit = text("incipit").nullable()
    val titleNormalized = text("title_normalized")
    val incipitNormalized = text("incipit_normalized").nullable()
    val composerId = uuid("composer_id")
    val primaryRagaId = uuid("primary_raga_id").nullable()
    val talaId = uuid("tala_id").nullable()
    val deityId = uuid("deity_id").nullable()
    val templeId = uuid("temple_id").nullable()
    val primaryLanguage = pgEnum<LanguageCode>("primary_language", LanguageCode.DB_TYPE)
    val isRagamalika = bool("is_ragamalika").default(false)
    val workflowState = pgEnum<WorkflowState>("workflow_state", WorkflowState.DB_TYPE)
    val sahityaSummary = text("sahitya_summary").nullable()
    val notes = text("notes").nullable()
    val createdByUserId = uuid("created_by_user_id").nullable()
    val updatedByUserId = uuid("updated_by_user_id").nullable()
    val createdAt = timestampWithTimeZone("created_at")
    val updatedAt = timestampWithTimeZone("updated_at")
}

object KrithiRagasTable : Table("krithi_ragas") {
    val krithiId = uuid("krithi_id")
    val ragaId = uuid("raga_id")
    val orderIndex = integer("order_index").default(0)
    val section = pgEnum<RagaSection>("section", RagaSection.DB_TYPE).nullable()
    val notes = text("notes").nullable()

    override val primaryKey = PrimaryKey(krithiId, ragaId, orderIndex)
}

object KrithiLyricVariantsTable : UUIDTable("krithi_lyric_variants") {
    val krithiId = uuid("krithi_id")
    val language = pgEnum<LanguageCode>("language", LanguageCode.DB_TYPE)
    val script = pgEnum<ScriptCode>("script", ScriptCode.DB_TYPE)
    val transliterationScheme = text("transliteration_scheme").nullable()
    val isPrimary = bool("is_primary").default(false)
    val variantLabel = text("variant_label").nullable()
    val sourceReference = text("source_reference").nullable()
    val lyrics = text("lyrics")
    val sampradayaId = uuid("sampradaya_id").nullable()
    val createdByUserId = uuid("created_by_user_id").nullable()
    val updatedByUserId = uuid("updated_by_user_id").nullable()
    val createdAt = timestampWithTimeZone("created_at")
    val updatedAt = timestampWithTimeZone("updated_at")
}

object KrithiSectionsTable : UUIDTable("krithi_sections") {
    val krithiId = uuid("krithi_id")
    val sectionType = text("section_type")
    val orderIndex = integer("order_index")
    val label = text("label").nullable()
    val notes = text("notes").nullable()
    val createdAt = timestampWithTimeZone("created_at")
    val updatedAt = timestampWithTimeZone("updated_at")
}

object KrithiLyricSectionsTable : UUIDTable("krithi_lyric_sections") {
    val lyricVariantId = uuid("lyric_variant_id")
    val sectionId = uuid("section_id")
    val text = text("text")
    val normalizedText = text("normalized_text").nullable()
    val createdAt = timestampWithTimeZone("created_at")
    val updatedAt = timestampWithTimeZone("updated_at")
}

object KrithiTagsTable : Table("krithi_tags") {
    val krithiId = uuid("krithi_id")
    val tagId = uuid("tag_id")
    val sourceInfo = text("source").default("manual")
    val confidence = integer("confidence").nullable()

    override val primaryKey = PrimaryKey(krithiId, tagId)
}

object ImportSourcesTable : UUIDTable("import_sources") {
    val name = text("name")
    val baseUrl = text("base_url").nullable()
    val description = text("description").nullable()
    val contactInfo = text("contact_info").nullable()
    val createdAt = timestampWithTimeZone("created_at")
}

object ImportedKrithisTable : UUIDTable("imported_krithis") {
    val importSourceId = uuid("import_source_id")
    val sourceKey = text("source_key").nullable()
    val rawTitle = text("raw_title").nullable()
    val rawLyrics = text("raw_lyrics").nullable()
    val rawComposer = text("raw_composer").nullable()
    val rawRaga = text("raw_raga").nullable()
    val rawTala = text("raw_tala").nullable()
    val rawDeity = text("raw_deity").nullable()
    val rawTemple = text("raw_temple").nullable()
    val rawLanguage = text("raw_language").nullable()
    val parsedPayload = jsonbText("parsed_payload").nullable()
    val importStatus = pgEnum<ImportStatus>("import_status", ImportStatus.DB_TYPE)
    val mappedKrithiId = uuid("mapped_krithi_id").nullable()
    val reviewerUserId = uuid("reviewer_user_id").nullable()
    val reviewerNotes = text("reviewer_notes").nullable()
    val reviewedAt = timestampWithTimeZone("reviewed_at").nullable()
    val createdAt = timestampWithTimeZone("created_at")
}

object AuditLogTable : UUIDTable("audit_log") {
    val actorUserId = uuid("actor_user_id").nullable()
    val actorIp = text("actor_ip").nullable()
    val action = text("action")
    val entityTable = text("entity_table")
    val entityId = uuid("entity_id").nullable()
    val changedAt = timestampWithTimeZone("changed_at")
    val diff = jsonbText("diff").nullable()
    val metadata = jsonbText("metadata").nullable()
}
