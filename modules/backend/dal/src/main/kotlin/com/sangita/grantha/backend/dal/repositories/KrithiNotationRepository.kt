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
import org.jetbrains.exposed.v1.core.JoinType
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.inList
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.update
import org.jetbrains.exposed.v1.jdbc.deleteWhere

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

        KrithiNotationVariantsTable
            .selectAll()
            .where { KrithiNotationVariantsTable.id eq variantId }
            .single()
            .toKrithiNotationVariantDto()
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

        KrithiNotationVariantsTable.update({ KrithiNotationVariantsTable.id eq variantId.toJavaUuid() }) {
            notationType?.let { value -> it[KrithiNotationVariantsTable.notationType] = value }
            it[KrithiNotationVariantsTable.talaId] = talaId ?: existing[KrithiNotationVariantsTable.talaId]
            kalai?.let { value -> it[KrithiNotationVariantsTable.kalai] = value }
            it[KrithiNotationVariantsTable.eduppuOffsetBeats] =
                eduppuOffsetBeats ?: existing[KrithiNotationVariantsTable.eduppuOffsetBeats]
            it[KrithiNotationVariantsTable.variantLabel] = variantLabel ?: existing[KrithiNotationVariantsTable.variantLabel]
            it[KrithiNotationVariantsTable.sourceReference] =
                sourceReference ?: existing[KrithiNotationVariantsTable.sourceReference]
            it[KrithiNotationVariantsTable.isPrimary] = effectiveIsPrimary
            it[KrithiNotationVariantsTable.updatedByUserId] = updatedByUserId
            it[KrithiNotationVariantsTable.updatedAt] = now
        }

        KrithiNotationVariantsTable
            .selectAll()
            .where { KrithiNotationVariantsTable.id eq variantId.toJavaUuid() }
            .single()
            .toKrithiNotationVariantDto()
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

        KrithiNotationRowsTable
            .selectAll()
            .where { KrithiNotationRowsTable.id eq rowId }
            .single()
            .toKrithiNotationRowDto()
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
        KrithiNotationRowsTable.update({ KrithiNotationRowsTable.id eq rowId.toJavaUuid() }) {
            it[KrithiNotationRowsTable.sectionId] = sectionId ?: existing[KrithiNotationRowsTable.sectionId]
            it[KrithiNotationRowsTable.orderIndex] = orderIndex ?: existing[KrithiNotationRowsTable.orderIndex]
            it[KrithiNotationRowsTable.swaraText] = swaraText ?: existing[KrithiNotationRowsTable.swaraText]
            it[KrithiNotationRowsTable.sahityaText] = sahityaText ?: existing[KrithiNotationRowsTable.sahityaText]
            it[KrithiNotationRowsTable.talaMarkers] = talaMarkers ?: existing[KrithiNotationRowsTable.talaMarkers]
            it[KrithiNotationRowsTable.updatedAt] = now
        }

        KrithiNotationRowsTable
            .selectAll()
            .where { KrithiNotationRowsTable.id eq rowId.toJavaUuid() }
            .single()
            .toKrithiNotationRowDto()
    }

    suspend fun deleteRow(rowId: Uuid): Boolean = DatabaseFactory.dbQuery {
        KrithiNotationRowsTable.deleteWhere { KrithiNotationRowsTable.id eq rowId.toJavaUuid() } > 0
    }
}
