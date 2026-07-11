package com.sangita.grantha.backend.dal.repositories

import com.sangita.grantha.backend.dal.DatabaseFactory
import com.sangita.grantha.backend.dal.enums.LanguageCode
import com.sangita.grantha.backend.dal.enums.ScriptCode
import com.sangita.grantha.backend.dal.tables.ExtractionQueueTable
import com.sangita.grantha.backend.dal.tables.ImportSourcesEnhancedTable
import com.sangita.grantha.backend.dal.tables.KrithiRevisionsTable
import com.sangita.grantha.backend.dal.tables.KrithiSectionRevisionsTable
import com.sangita.grantha.backend.dal.tables.SourceDocumentsTable
import com.sangita.grantha.shared.domain.model.KrithiRevisionDto
import com.sangita.grantha.shared.domain.model.KrithiSectionRevisionDto
import com.sangita.grantha.shared.domain.model.SectionProvenanceDto
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.util.UUID
import kotlin.uuid.toKotlinUuid
import org.jetbrains.exposed.v1.core.*
import org.jetbrains.exposed.v1.jdbc.*

/** Input for one section body inside a new revision (ADR-014 write contract). */
data class SectionRevisionWrite(
    val sectionType: String,
    val orderIndex: Int,
    val label: String? = null,
    val language: LanguageCode? = null,
    val script: ScriptCode? = null,
    val text: String,
    val normalizedText: String? = null,
    val extractionId: UUID? = null,
    val sourceDocumentId: UUID? = null,
)

/** Input for a new append-only revision envelope. */
data class RevisionWrite(
    val krithiId: UUID,
    val changeKind: String, // IMPORT / CURATOR_EDIT / MERGE / CORRECTION
    val changeReason: String? = null,
    val extractionId: UUID? = null,
    val createdByUserId: UUID? = null,
    val sections: List<SectionRevisionWrite> = emptyList(),
)

/**
 * Versioned canon repository (ADR-014 / TRACK-117).
 *
 * The revision tables are append-only: this repository exposes no update or
 * delete. "Current" is the latest revision; the legacy current-state tables
 * remain the serving projection, written by the existing creation/edit paths.
 */
class RevisionRepository(
    private val sourceEvidence: SourceEvidenceRepository,
) {

    private val R = KrithiRevisionsTable
    private val S = KrithiSectionRevisionsTable
    private val D = SourceDocumentsTable
    private val E = ExtractionQueueTable
    private val REG = ImportSourcesEnhancedTable

    private fun toInstant(odt: OffsetDateTime): kotlin.time.Instant =
        odt.toInstant().let { kotlin.time.Instant.fromEpochSeconds(it.epochSecond, it.nano) }

    /**
     * Find-or-create the physical artifact node for (registry, url, checksum).
     * Dedup key matches the `source_documents_dedup_uq` constraint.
     */
    suspend fun ensureSourceDocument(
        importSourceId: UUID,
        sourceUrl: String,
        sourceFormat: String,
        checksum: String? = null,
        pageRange: String? = null,
    ): UUID = DatabaseFactory.dbQuery {
        val existing = D.select(D.id)
            .where { (D.importSourceId eq importSourceId) and (D.sourceUrl eq sourceUrl) }
            .andWhere { if (checksum == null) D.checksum.isNull() else D.checksum eq checksum }
            .limit(1)
            .firstOrNull()
        if (existing != null) {
            existing[D.id].value
        } else {
            (D.insert {
                it[D.importSourceId] = importSourceId
                it[D.sourceUrl] = sourceUrl
                it[D.sourceFormat] = sourceFormat
                it[D.checksum] = checksum
                it[D.pageRange] = pageRange
                it[D.retrievedAt] = OffsetDateTime.now(ZoneOffset.UTC)
            } get D.id).value
        }
    }

    /**
     * [ensureSourceDocument] with the registry entry resolved from the source
     * name/URL the same way `krithi_source_evidence` does (name → domain →
     * auto-create).
     */
    suspend fun ensureSourceDocumentForSource(
        sourceName: String,
        sourceUrl: String,
        sourceTier: Int,
        sourceFormat: String,
        checksum: String? = null,
        pageRange: String? = null,
    ): UUID = DatabaseFactory.dbQuery {
        val importSourceId = sourceEvidence.resolveImportSourceId(
            sourceName, sourceUrl, sourceTier, OffsetDateTime.now(ZoneOffset.UTC),
        )
        val existing = D.select(D.id)
            .where { (D.importSourceId eq importSourceId) and (D.sourceUrl eq sourceUrl) }
            .andWhere { if (checksum == null) D.checksum.isNull() else D.checksum eq checksum }
            .limit(1)
            .firstOrNull()
        if (existing != null) {
            existing[D.id].value
        } else {
            (D.insert {
                it[D.importSourceId] = importSourceId
                it[D.sourceUrl] = sourceUrl
                it[D.sourceFormat] = sourceFormat
                it[D.checksum] = checksum
                it[D.pageRange] = pageRange
                it[D.retrievedAt] = OffsetDateTime.now(ZoneOffset.UTC)
            } get D.id).value
        }
    }

    /**
     * Append a new revision envelope + its per-section rows. Never updates in
     * place; `revision_no` is allocated as max+1 per krithi.
     */
    suspend fun appendRevision(write: RevisionWrite): KrithiRevisionDto = DatabaseFactory.dbQuery {
        require(write.extractionId != null || write.createdByUserId != null) {
            "Revision must be attributed to an extraction or a user (ADR-014)"
        }
        val now = OffsetDateTime.now(ZoneOffset.UTC)
        val maxNo = R.select(R.revisionNo.max())
            .where { R.krithiId eq write.krithiId }
            .firstOrNull()?.get(R.revisionNo.max()) ?: 0
        val revisionNo = maxNo + 1

        val revisionId = (R.insert {
            it[R.krithiId] = write.krithiId
            it[R.revisionNo] = revisionNo
            it[R.changeKind] = write.changeKind
            it[R.changeReason] = write.changeReason
            it[R.extractionId] = write.extractionId
            it[R.createdByUserId] = write.createdByUserId
            it[R.validFrom] = now
            it[R.recordedAt] = now
        } get R.id).value

        write.sections.forEach { s ->
            S.insert {
                it[S.revisionId] = revisionId
                it[S.krithiId] = write.krithiId
                it[S.sectionType] = s.sectionType
                it[S.orderIndex] = s.orderIndex
                it[S.label] = s.label
                it[S.language] = s.language
                it[S.script] = s.script
                it[S.text] = s.text
                it[S.normalizedText] = s.normalizedText
                it[S.extractionId] = s.extractionId
                it[S.sourceDocumentId] = s.sourceDocumentId
                it[S.validFrom] = now
            }
        }

        loadRevision(revisionId)
            ?: error("Revision $revisionId vanished within its own transaction")
    }

    /**
     * Append a revision whose section bodies are a snapshot of the krithi's
     * CURRENT serving state (`krithi_sections` × lyric variants). The
     * reusable primitive for curator-driven changes (approve/edit/merge),
     * where content was just written to the current tables by the legacy
     * paths and the revision records the accepted result.
     */
    suspend fun snapshotCurrentState(
        krithiId: UUID,
        changeKind: String,
        changeReason: String? = null,
        extractionId: UUID? = null,
        createdByUserId: UUID? = null,
        sourceDocumentId: UUID? = null,
    ): KrithiRevisionDto {
        val sections = DatabaseFactory.dbQuery {
            val K = com.sangita.grantha.backend.dal.tables.KrithiSectionsTable
            val LV = com.sangita.grantha.backend.dal.tables.KrithiLyricVariantsTable
            val LS = com.sangita.grantha.backend.dal.tables.KrithiLyricSectionsTable

            val skeleton = K.selectAll().where { K.krithiId eq krithiId }
                .orderBy(K.orderIndex to SortOrder.ASC).toList()

            val lyricRows = LS.join(LV, JoinType.INNER, LS.lyricVariantId, LV.id)
                .join(K, JoinType.INNER, LS.sectionId, K.id)
                .select(K.sectionType, K.orderIndex, K.label, LV.language, LV.script, LS.text, LS.normalizedText)
                .where { LV.krithiId eq krithiId }
                .map { row ->
                    SectionRevisionWrite(
                        sectionType = row[K.sectionType],
                        orderIndex = row[K.orderIndex],
                        label = row[K.label],
                        language = row[LV.language],
                        script = row[LV.script],
                        text = row[LS.text],
                        normalizedText = row[LS.normalizedText],
                        extractionId = extractionId,
                        sourceDocumentId = sourceDocumentId,
                    )
                }

            val coveredOrders = lyricRows.map { it.orderIndex }.toSet()
            val skeletonRows = skeleton
                .filter { it[K.orderIndex] !in coveredOrders }
                .map { row ->
                    SectionRevisionWrite(
                        sectionType = row[K.sectionType],
                        orderIndex = row[K.orderIndex],
                        label = row[K.label],
                        text = "",
                        extractionId = extractionId,
                        sourceDocumentId = sourceDocumentId,
                    )
                }
            (lyricRows + skeletonRows).sortedBy { it.orderIndex }
        }
        return appendRevision(
            RevisionWrite(
                krithiId = krithiId,
                changeKind = changeKind,
                changeReason = changeReason,
                extractionId = extractionId,
                createdByUserId = createdByUserId,
                sections = sections,
            ),
        )
    }

    /** Latest revision (envelope + sections) for a krithi, or null if none. */
    suspend fun latestRevision(krithiId: UUID): KrithiRevisionDto? = DatabaseFactory.dbQuery {
        R.selectAll()
            .where { R.krithiId eq krithiId }
            .orderBy(R.validFrom to SortOrder.DESC, R.revisionNo to SortOrder.DESC)
            .limit(1)
            .firstOrNull()
            ?.let { loadRevision(it[R.id].value) }
    }

    /** Full revision history for a krithi, oldest first. */
    suspend fun listRevisions(krithiId: UUID): List<KrithiRevisionDto> = DatabaseFactory.dbQuery {
        R.selectAll()
            .where { R.krithiId eq krithiId }
            .orderBy(R.revisionNo to SortOrder.ASC)
            .map { it[R.id].value }
            .mapNotNull { loadRevision(it) }
    }

    /**
     * The N5 answer in one query: sections of the revision current at [asOf]
     * (default: now), each joined to its extraction run, source document, and
     * registry entry.
     */
    suspend fun sectionProvenance(
        krithiId: UUID,
        asOf: OffsetDateTime = OffsetDateTime.now(ZoneOffset.UTC),
    ): List<SectionProvenanceDto> = DatabaseFactory.dbQuery {
        val target = R.selectAll()
            .where { (R.krithiId eq krithiId) and (R.validFrom lessEq asOf) }
            .orderBy(R.validFrom to SortOrder.DESC, R.revisionNo to SortOrder.DESC)
            .limit(1)
            .firstOrNull() ?: return@dbQuery emptyList()

        val revisionId = target[R.id].value
        S.join(E, org.jetbrains.exposed.v1.core.JoinType.LEFT, S.extractionId, E.id)
            .join(D, org.jetbrains.exposed.v1.core.JoinType.LEFT, S.sourceDocumentId, D.id)
            .join(REG, org.jetbrains.exposed.v1.core.JoinType.LEFT, D.importSourceId, REG.id)
            .select(
                S.sectionType, S.orderIndex, S.text,
                E.extractorVersion, E.confidence,
                D.sourceUrl, D.checksum, REG.name,
            )
            .where { S.revisionId eq revisionId }
            .orderBy(S.orderIndex to SortOrder.ASC)
            .map { row ->
                SectionProvenanceDto(
                    sectionType = row[S.sectionType],
                    orderIndex = row[S.orderIndex],
                    text = row[S.text],
                    revisionNo = target[R.revisionNo],
                    changeKind = target[R.changeKind],
                    extractorVersion = row.getOrNull(E.extractorVersion),
                    extractionConfidence = row.getOrNull(E.confidence)?.toDouble(),
                    sourceUrl = row.getOrNull(D.sourceUrl),
                    sourceChecksum = row.getOrNull(D.checksum),
                    sourceRegistryName = row.getOrNull(REG.name),
                    createdByUserId = target[R.createdByUserId]?.toString(),
                )
            }
    }

    // ── internals ───────────────────────────────────────────────────────────

    /** Must run inside an open transaction. */
    private fun loadRevision(revisionId: UUID): KrithiRevisionDto? {
        val row = R.selectAll().where { R.id eq revisionId }.firstOrNull() ?: return null
        val sections = S.selectAll()
            .where { S.revisionId eq revisionId }
            .orderBy(S.orderIndex to SortOrder.ASC)
            .map { it.toSectionDto() }
        return row.toRevisionDto(sections)
    }

    private fun ResultRow.toRevisionDto(sections: List<KrithiSectionRevisionDto>) = KrithiRevisionDto(
        id = this[R.id].value.toKotlinUuid(),
        krithiId = this[R.krithiId].toKotlinUuid(),
        revisionNo = this[R.revisionNo],
        changeKind = this[R.changeKind],
        changeReason = this[R.changeReason],
        extractionId = this[R.extractionId]?.toKotlinUuid(),
        createdByUserId = this[R.createdByUserId]?.toKotlinUuid(),
        validFrom = toInstant(this[R.validFrom]),
        recordedAt = toInstant(this[R.recordedAt]),
        sections = sections,
    )

    private fun ResultRow.toSectionDto() = KrithiSectionRevisionDto(
        id = this[S.id].value.toKotlinUuid(),
        revisionId = this[S.revisionId].toKotlinUuid(),
        sectionType = this[S.sectionType],
        orderIndex = this[S.orderIndex],
        label = this[S.label],
        language = this[S.language]?.name,
        script = this[S.script]?.name,
        text = this[S.text],
        normalizedText = this[S.normalizedText],
        extractionId = this[S.extractionId]?.toKotlinUuid(),
        sourceDocumentId = this[S.sourceDocumentId]?.toKotlinUuid(),
        validFrom = toInstant(this[S.validFrom]),
    )
}
