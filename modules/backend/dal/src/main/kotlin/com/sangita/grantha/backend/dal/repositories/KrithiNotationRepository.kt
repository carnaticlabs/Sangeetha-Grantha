package com.sangita.grantha.backend.dal.repositories

import com.sangita.grantha.backend.dal.DatabaseFactory
import com.sangita.grantha.backend.dal.models.toKrithiNotationRowDto
import com.sangita.grantha.backend.dal.models.toKrithiNotationVariantDto
import com.sangita.grantha.backend.dal.support.toJavaUuid
import com.sangita.grantha.backend.dal.tables.KrithiNotationRowsTable
import com.sangita.grantha.backend.dal.tables.KrithiNotationVariantsTable
import com.sangita.grantha.backend.dal.tables.KrithiSectionsTable
import com.sangita.grantha.shared.domain.model.KrithiNotationRowDto
import com.sangita.grantha.shared.domain.model.KrithiNotationVariantDto
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.util.UUID
import kotlin.uuid.Uuid
import org.jetbrains.exposed.v1.core.*
import org.jetbrains.exposed.v1.jdbc.*

data class NotationRowWithSectionOrder(
    val row: KrithiNotationRowDto,
    val sectionOrderIndex: Int,
)

class KrithiNotationRepository {
    suspend fun findVariantById(variantId: Uuid): KrithiNotationVariantDto? =
        DatabaseFactory.dbQuery {
            KrithiNotationVariantsTable
                .selectAll()
                .where { KrithiNotationVariantsTable.id eq variantId.toJavaUuid() }
                .map { it.toKrithiNotationVariantDto() }
                .singleOrNull()
        }

    suspend fun listVariantsByKrithiId(krithiId: Uuid): List<KrithiNotationVariantDto> =
        DatabaseFactory.dbQuery {
            KrithiNotationVariantsTable
                .selectAll()
                .where { KrithiNotationVariantsTable.krithiId eq krithiId.toJavaUuid() }
                .orderBy(
                    KrithiNotationVariantsTable.isPrimary to SortOrder.DESC,
                    KrithiNotationVariantsTable.createdAt to SortOrder.ASC
                )
                .map { it.toKrithiNotationVariantDto() }
        }

    suspend fun listRowsByVariantIds(variantIds: List<Uuid>): List<NotationRowWithSectionOrder> =
        DatabaseFactory.dbQuery {
            if (variantIds.isEmpty()) return@dbQuery emptyList()

            KrithiNotationRowsTable
                .join(
                    KrithiSectionsTable,
                    joinType = JoinType.INNER,
                    onColumn = KrithiNotationRowsTable.sectionId,
                    otherColumn = KrithiSectionsTable.id,
                )
                .selectAll()
                .where { KrithiNotationRowsTable.notationVariantId inList variantIds.map { it.toJavaUuid() } }
                .orderBy(
                    KrithiSectionsTable.orderIndex to SortOrder.ASC,
                    KrithiNotationRowsTable.orderIndex to SortOrder.ASC
                )
                .map {
                    NotationRowWithSectionOrder(
                        row = it.toKrithiNotationRowDto(),
                        sectionOrderIndex = it[KrithiSectionsTable.orderIndex]
                    )
                }
        }

    suspend fun createVariant(
        krithiId: UUID,
        notationType: String,
        talaId: UUID?,
        kalai: Int,
        eduppuOffsetBeats: Int?,
        variantLabel: String?,
        sourceReference: String?,
        isPrimary: Boolean,
        createdByUserId: UUID?,
        updatedByUserId: UUID?,
    ): KrithiNotationVariantDto = DatabaseFactory.dbQuery {
        val now = OffsetDateTime.now(ZoneOffset.UTC)
        val variantId = UUID.randomUUID()

        if (isPrimary) {
            KrithiNotationVariantsTable.update({
                (KrithiNotationVariantsTable.krithiId eq krithiId) and
                    (KrithiNotationVariantsTable.notationType eq notationType)
            }) {
                it[KrithiNotationVariantsTable.isPrimary] = false
                it[KrithiNotationVariantsTable.updatedAt] = now
            }
        }

        KrithiNotationVariantsTable.insert {
            it[id] = variantId
            it[KrithiNotationVariantsTable.krithiId] = krithiId
            it[KrithiNotationVariantsTable.notationType] = notationType
            it[KrithiNotationVariantsTable.talaId] = talaId
            it[KrithiNotationVariantsTable.kalai] = kalai
            it[KrithiNotationVariantsTable.eduppuOffsetBeats] = eduppuOffsetBeats
            it[KrithiNotationVariantsTable.variantLabel] = variantLabel
            it[KrithiNotationVariantsTable.sourceReference] = sourceReference
            it[KrithiNotationVariantsTable.isPrimary] = isPrimary
            it[KrithiNotationVariantsTable.createdByUserId] = createdByUserId
            it[KrithiNotationVariantsTable.updatedByUserId] = updatedByUserId
            it[KrithiNotationVariantsTable.createdAt] = now
            it[KrithiNotationVariantsTable.updatedAt] = now
        }
            .resultedValues
            ?.single()
            ?.toKrithiNotationVariantDto()
            ?: error("Failed to insert notation variant")
    }

    suspend fun updateVariant(
        variantId: Uuid,
        notationType: String? = null,
        talaId: UUID? = null,
        kalai: Int? = null,
        eduppuOffsetBeats: Int? = null,
        variantLabel: String? = null,
        sourceReference: String? = null,
        isPrimary: Boolean? = null,
        updatedByUserId: UUID? = null,
    ): KrithiNotationVariantDto? = DatabaseFactory.dbQuery {
        val existing = KrithiNotationVariantsTable
            .selectAll()
            .where { KrithiNotationVariantsTable.id eq variantId.toJavaUuid() }
            .singleOrNull() ?: return@dbQuery null

        val now = OffsetDateTime.now(ZoneOffset.UTC)
        val effectiveNotationType = notationType ?: existing[KrithiNotationVariantsTable.notationType]
        val effectiveIsPrimary = isPrimary ?: existing[KrithiNotationVariantsTable.isPrimary]

        if (effectiveIsPrimary) {
            KrithiNotationVariantsTable.update({
                (KrithiNotationVariantsTable.krithiId eq existing[KrithiNotationVariantsTable.krithiId]) and
                    (KrithiNotationVariantsTable.notationType eq effectiveNotationType)
            }) {
                it[KrithiNotationVariantsTable.isPrimary] = false
                it[KrithiNotationVariantsTable.updatedAt] = now
            }
        }

        // Use Exposed 1.0.0-rc-4 updateReturning to update and fetch the row in one round-trip
        KrithiNotationVariantsTable
            .updateReturning(
                where = { KrithiNotationVariantsTable.id eq variantId.toJavaUuid() }
            ) { updateStmt ->
                notationType?.let { value -> updateStmt[KrithiNotationVariantsTable.notationType] = value }
                updateStmt[KrithiNotationVariantsTable.talaId] = talaId ?: existing[KrithiNotationVariantsTable.talaId]
                kalai?.let { value -> updateStmt[KrithiNotationVariantsTable.kalai] = value }
                updateStmt[KrithiNotationVariantsTable.eduppuOffsetBeats] =
                    eduppuOffsetBeats ?: existing[KrithiNotationVariantsTable.eduppuOffsetBeats]
                updateStmt[KrithiNotationVariantsTable.variantLabel] = variantLabel ?: existing[KrithiNotationVariantsTable.variantLabel]
                updateStmt[KrithiNotationVariantsTable.sourceReference] =
                    sourceReference ?: existing[KrithiNotationVariantsTable.sourceReference]
                updateStmt[KrithiNotationVariantsTable.isPrimary] = effectiveIsPrimary
                updateStmt[KrithiNotationVariantsTable.updatedByUserId] = updatedByUserId
                updateStmt[KrithiNotationVariantsTable.updatedAt] = now
            }
            .singleOrNull()
            ?.toKrithiNotationVariantDto()
            ?: error("Failed to update notation variant")
    }

    suspend fun deleteVariant(variantId: Uuid): Boolean = DatabaseFactory.dbQuery {
        KrithiNotationVariantsTable.deleteWhere { KrithiNotationVariantsTable.id eq variantId.toJavaUuid() } > 0
    }

    suspend fun createRow(
        notationVariantId: UUID,
        sectionId: UUID,
        orderIndex: Int,
        swaraText: String,
        sahityaText: String?,
        talaMarkers: String?,
    ): KrithiNotationRowDto = DatabaseFactory.dbQuery {
        val now = OffsetDateTime.now(ZoneOffset.UTC)
        val rowId = UUID.randomUUID()

        KrithiNotationRowsTable.insert {
            it[id] = rowId
            it[KrithiNotationRowsTable.notationVariantId] = notationVariantId
            it[KrithiNotationRowsTable.sectionId] = sectionId
            it[KrithiNotationRowsTable.orderIndex] = orderIndex
            it[KrithiNotationRowsTable.swaraText] = swaraText
            it[KrithiNotationRowsTable.sahityaText] = sahityaText
            it[KrithiNotationRowsTable.talaMarkers] = talaMarkers
            it[KrithiNotationRowsTable.createdAt] = now
            it[KrithiNotationRowsTable.updatedAt] = now
        }
            .resultedValues
            ?.single()
            ?.toKrithiNotationRowDto()
            ?: error("Failed to insert notation row")
    }

    suspend fun updateRow(
        rowId: Uuid,
        sectionId: UUID? = null,
        orderIndex: Int? = null,
        swaraText: String? = null,
        sahityaText: String? = null,
        talaMarkers: String? = null,
    ): KrithiNotationRowDto? = DatabaseFactory.dbQuery {
        val existing = KrithiNotationRowsTable
            .selectAll()
            .where { KrithiNotationRowsTable.id eq rowId.toJavaUuid() }
            .singleOrNull() ?: return@dbQuery null

        val now = OffsetDateTime.now(ZoneOffset.UTC)
        val javaRowId = rowId.toJavaUuid()
        
        // Use Exposed 1.0.0-rc-4 updateReturning to update and fetch the row in one round-trip
        KrithiNotationRowsTable
            .updateReturning(
                where = { KrithiNotationRowsTable.id eq javaRowId }
            ) { updateStmt ->
                updateStmt[KrithiNotationRowsTable.sectionId] = sectionId ?: existing[KrithiNotationRowsTable.sectionId]
                updateStmt[KrithiNotationRowsTable.orderIndex] = orderIndex ?: existing[KrithiNotationRowsTable.orderIndex]
                updateStmt[KrithiNotationRowsTable.swaraText] = swaraText ?: existing[KrithiNotationRowsTable.swaraText]
                updateStmt[KrithiNotationRowsTable.sahityaText] = sahityaText ?: existing[KrithiNotationRowsTable.sahityaText]
                updateStmt[KrithiNotationRowsTable.talaMarkers] = talaMarkers ?: existing[KrithiNotationRowsTable.talaMarkers]
                updateStmt[KrithiNotationRowsTable.updatedAt] = now
            }
            .singleOrNull()
            ?.toKrithiNotationRowDto()
            ?: error("Failed to update notation row")
    }

    suspend fun deleteRow(rowId: Uuid): Boolean = DatabaseFactory.dbQuery {
        KrithiNotationRowsTable.deleteWhere { KrithiNotationRowsTable.id eq rowId.toJavaUuid() } > 0
    }
}
