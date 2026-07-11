package com.sangita.grantha.backend.dal.tables

import com.sangita.grantha.backend.dal.enums.LanguageCode
import com.sangita.grantha.backend.dal.enums.ScriptCode
import com.sangita.grantha.backend.dal.support.pgEnum
import org.jetbrains.exposed.v1.core.dao.id.java.UUIDTable
import org.jetbrains.exposed.v1.core.java.javaUUID
import org.jetbrains.exposed.v1.javatime.timestampWithTimeZone

/**
 * Versioned canon & provenance graph (ADR-014 / TRACK-117, migration V44).
 *
 * `krithi_revisions` + `krithi_section_revisions` are APPEND-ONLY: never
 * update rows here — a change is a new revision. The current-state tables
 * (`krithi_sections`, `krithi_lyric_sections`) remain the serving projection.
 */

/** Physical source artifact: registry (import_sources) 1—N documents 1—N extraction runs. */
object SourceDocumentsTable : UUIDTable("source_documents") {
    val importSourceId = javaUUID("import_source_id")
    val sourceUrl = text("source_url")
    val sourceFormat = text("source_format")
    val pageRange = text("page_range").nullable()
    val checksum = text("checksum").nullable()
    val retrievedAt = timestampWithTimeZone("retrieved_at")
}

/** Append-only revision envelope — one row per accepted change-set to a krithi. */
object KrithiRevisionsTable : UUIDTable("krithi_revisions") {
    val krithiId = javaUUID("krithi_id")
    val revisionNo = integer("revision_no")
    val changeKind = text("change_kind") // IMPORT / CURATOR_EDIT / MERGE / CORRECTION
    val changeReason = text("change_reason").nullable()
    val extractionId = javaUUID("extraction_id").nullable()
    val createdByUserId = javaUUID("created_by_user_id").nullable()
    val validFrom = timestampWithTimeZone("valid_from")
    val recordedAt = timestampWithTimeZone("recorded_at")
}

/** Append-only per-section content + per-section provenance under an envelope. */
object KrithiSectionRevisionsTable : UUIDTable("krithi_section_revisions") {
    val revisionId = javaUUID("revision_id")
    val krithiId = javaUUID("krithi_id")
    val sectionType = text("section_type")
    val orderIndex = integer("order_index")
    val label = text("label").nullable()
    val language = pgEnum<LanguageCode>("language", LanguageCode.DB_TYPE).nullable()
    val script = pgEnum<ScriptCode>("script", ScriptCode.DB_TYPE).nullable()
    val text = text("text")
    val normalizedText = text("normalized_text").nullable()
    val extractionId = javaUUID("extraction_id").nullable()
    val sourceDocumentId = javaUUID("source_document_id").nullable()
    val validFrom = timestampWithTimeZone("valid_from")
}
