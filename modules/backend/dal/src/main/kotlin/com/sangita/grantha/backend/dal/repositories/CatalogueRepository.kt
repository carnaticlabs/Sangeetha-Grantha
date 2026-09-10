package com.sangita.grantha.backend.dal.repositories

import com.sangita.grantha.backend.dal.DatabaseFactory
import com.sangita.grantha.backend.dal.enums.MusicalForm
import com.sangita.grantha.backend.dal.enums.WorkflowState
import com.sangita.grantha.backend.dal.models.CatalogueLike
import com.sangita.grantha.backend.dal.models.CatalogueReadingDefaults
import com.sangita.grantha.backend.dal.models.CatalogueV2DtoMappers
import com.sangita.grantha.backend.dal.models.CatalogueVisibility
import com.sangita.grantha.backend.dal.models.toDto
import com.sangita.grantha.backend.dal.support.toJavaUuid
import com.sangita.grantha.backend.dal.support.toKotlinUuid
import com.sangita.grantha.backend.dal.tables.ComposerAliasesTable
import com.sangita.grantha.backend.dal.tables.ComposersTable
import com.sangita.grantha.backend.dal.tables.DeitiesTable
import com.sangita.grantha.backend.dal.tables.KrithiLyricSectionsTable
import com.sangita.grantha.backend.dal.tables.KrithiLyricVariantsTable
import com.sangita.grantha.backend.dal.tables.KrithiRagasTable
import com.sangita.grantha.backend.dal.tables.KrithiSectionsTable
import com.sangita.grantha.backend.dal.tables.KrithisTable
import com.sangita.grantha.backend.dal.tables.RagaAliasesTable
import com.sangita.grantha.backend.dal.tables.RagaRelationsTable
import com.sangita.grantha.backend.dal.tables.RagasTable
import com.sangita.grantha.backend.dal.tables.TalasTable
import com.sangita.grantha.backend.dal.tables.TemplesTable
import com.sangita.grantha.shared.domain.model.RagaSectionDto
import com.sangita.grantha.shared.domain.model.catalogue.CatalogueComposerDetailDto
import com.sangita.grantha.shared.domain.model.catalogue.CatalogueComposerRefDto
import com.sangita.grantha.shared.domain.model.catalogue.CatalogueComposerSummaryDto
import com.sangita.grantha.shared.domain.model.catalogue.CatalogueCompletenessDto
import com.sangita.grantha.shared.domain.model.catalogue.CatalogueDiscoveryDto
import com.sangita.grantha.shared.domain.model.catalogue.CatalogueKrithiReaderDto
import com.sangita.grantha.shared.domain.model.catalogue.CatalogueKrithiSummaryDto
import com.sangita.grantha.shared.domain.model.catalogue.CatalogueLyricSectionDto
import com.sangita.grantha.shared.domain.model.catalogue.CatalogueLyricsDto
import com.sangita.grantha.shared.domain.model.catalogue.CatalogueNomenclatureLinkDto
import com.sangita.grantha.shared.domain.model.catalogue.CataloguePagedResponse
import com.sangita.grantha.shared.domain.model.catalogue.CatalogueRagaDetailDto
import com.sangita.grantha.shared.domain.model.catalogue.CatalogueRagaRefDto
import com.sangita.grantha.shared.domain.model.catalogue.CatalogueRagaSummaryDto
import com.sangita.grantha.shared.domain.model.catalogue.CatalogueReferenceDto
import com.sangita.grantha.shared.domain.model.catalogue.CatalogueTalaRefDto
import com.sangita.grantha.shared.domain.model.catalogue.CatalogueVariantRefDto
import java.util.UUID
import kotlin.uuid.Uuid
import org.jetbrains.exposed.v1.core.JoinType
import org.jetbrains.exposed.v1.core.Op
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.inList
import org.jetbrains.exposed.v1.core.inSubQuery
import org.jetbrains.exposed.v1.core.like
import org.jetbrains.exposed.v1.core.LikePattern
import org.jetbrains.exposed.v1.core.lowerCase
import org.jetbrains.exposed.v1.core.neq
import org.jetbrains.exposed.v1.core.or
import org.jetbrains.exposed.v1.jdbc.select
import org.jetbrains.exposed.v1.jdbc.selectAll

/**
 * Published-only Rasika catalogue reads (TRACK-138). Raga membership is always
 * the `krithi_ragas` junction, including repeated ragamalika rows.
 */
class CatalogueRepository {

    suspend fun searchKrithis(
        query: String?,
        composerId: UUID?,
        ragaId: UUID?,
        page: Int,
        pageSize: Int,
        visibility: CatalogueVisibility = CatalogueVisibility.V1,
    ): CataloguePagedResponse<CatalogueKrithiSummaryDto> = DatabaseFactory.dbQuery {
        val condition = krithiFilter(query, composerId, ragaId, visibility)
        val total = KrithisTable.select(KrithisTable.id).where { condition }.count()
        val offset = page.toLong() * pageSize.toLong()
        val ids = KrithisTable
            .select(KrithisTable.id)
            .where { condition }
            .orderBy(KrithisTable.titleNormalized to SortOrder.ASC, KrithisTable.id to SortOrder.ASC)
            .limit(pageSize)
            .offset(offset)
            .map { it[KrithisTable.id].value }
        CataloguePagedResponse(
            items = hydrateSummaries(ids),
            total = total,
            page = page,
            pageSize = pageSize,
        )
    }

    suspend fun findPublishedReader(
        id: Uuid,
        visibility: CatalogueVisibility = CatalogueVisibility.V1,
    ): CatalogueKrithiReaderDto? = DatabaseFactory.dbQuery {
        val javaId = id.toJavaUuid()
        val row = KrithisTable
            .selectAll()
            .where { (KrithisTable.id eq javaId) and publishedComposition(visibility) }
            .singleOrNull()
            ?: return@dbQuery null
        val summaries = hydrateSummaries(listOf(javaId))
        val summary = summaries.singleOrNull() ?: return@dbQuery null
        val variants = loadVariants(javaId)
        CatalogueKrithiReaderDto(
            id = summary.id,
            title = summary.title,
            incipit = summary.incipit,
            composer = summary.composer,
            ragas = summary.ragas,
            tala = summary.tala,
            musicalForm = summary.musicalForm,
            originalLanguage = row[KrithisTable.primaryLanguage].toDto(),
            isRagamalika = summary.isRagamalika,
            defaultVariantId = CatalogueReadingDefaults.selectDefaultVariantId(variants),
            variants = variants,
            completeness = CatalogueCompletenessDto.UNKNOWN,
            deity = deityReference(row[KrithisTable.deityId]),
            temple = templeReference(row[KrithisTable.templeId]),
        )
    }

    suspend fun findPublishedLyrics(
        krithiId: Uuid,
        variantId: Uuid,
        visibility: CatalogueVisibility = CatalogueVisibility.V1,
    ): CatalogueLyricsDto? = DatabaseFactory.dbQuery {
        val javaKrithiId = krithiId.toJavaUuid()
        val published = KrithisTable
            .select(KrithisTable.id)
            .where { (KrithisTable.id eq javaKrithiId) and publishedComposition(visibility) }
            .count() > 0
        if (!published) return@dbQuery null

        val variantRow = KrithiLyricVariantsTable
            .selectAll()
            .where { KrithiLyricVariantsTable.id eq variantId.toJavaUuid() }
            .singleOrNull()
            ?: return@dbQuery null
        if (variantRow[KrithiLyricVariantsTable.krithiId] != javaKrithiId) {
            return@dbQuery null
        }

        val sections = KrithiLyricSectionsTable
            .join(
                KrithiSectionsTable,
                JoinType.INNER,
                onColumn = KrithiLyricSectionsTable.sectionId,
                otherColumn = KrithiSectionsTable.id,
            )
            .selectAll()
            .where { KrithiLyricSectionsTable.lyricVariantId eq variantId.toJavaUuid() }
            .orderBy(KrithiSectionsTable.orderIndex to SortOrder.ASC)
            .map { sectionRow ->
                CatalogueLyricSectionDto(
                    sectionId = sectionRow[KrithiSectionsTable.id].value.toKotlinUuid(),
                    sectionType = sectionRow[KrithiSectionsTable.sectionType],
                    label = sectionRow[KrithiSectionsTable.label],
                    orderIndex = sectionRow[KrithiSectionsTable.orderIndex],
                    text = sectionRow[KrithiLyricSectionsTable.text],
                )
            }

        val storedLyrics = variantRow[KrithiLyricVariantsTable.lyrics]
        CatalogueLyricsDto(
            variantId = variantRow[KrithiLyricVariantsTable.id].value.toKotlinUuid(),
            krithiId = variantRow[KrithiLyricVariantsTable.krithiId].toKotlinUuid(),
            language = variantRow[KrithiLyricVariantsTable.language].toDto(),
            script = variantRow[KrithiLyricVariantsTable.script].toDto(),
            transliterationScheme = variantRow[KrithiLyricVariantsTable.transliterationScheme],
            isPrimary = variantRow[KrithiLyricVariantsTable.isPrimary],
            label = variantRow[KrithiLyricVariantsTable.variantLabel],
            sourceReference = variantRow[KrithiLyricVariantsTable.sourceReference],
            unsegmentedText = if (sections.isEmpty()) storedLyrics.takeIf { it.isNotBlank() } else null,
            sections = sections,
        )
    }

    suspend fun searchRagas(
        query: String?,
        page: Int,
        pageSize: Int,
        visibility: CatalogueVisibility = CatalogueVisibility.V1,
    ): CataloguePagedResponse<CatalogueRagaSummaryDto> = DatabaseFactory.dbQuery {
        val condition = ragaFilter(query)
        val total = RagasTable.select(RagasTable.id).where { condition }.count()
        val offset = page.toLong() * pageSize.toLong()
        val rows = RagasTable
            .selectAll()
            .where { condition }
            .orderBy(RagasTable.nameNormalized to SortOrder.ASC, RagasTable.id to SortOrder.ASC)
            .limit(pageSize)
            .offset(offset)
            .toList()
        val ids = rows.map { it[RagasTable.id].value }
        val counts = publishedCountsByRaga(ids, visibility)
        val parentIds = rows.mapNotNull { it[RagasTable.parentRagaId] }
        val parents = parentNames(parentIds)
        val parentMelas = parentMelakartaNumbers(parentIds)
        val matching = matchingRagaAliases(ids, query)
        CataloguePagedResponse(
            items = rows.map { row ->
                val id = row[RagasTable.id].value
                val parentId = row[RagasTable.parentRagaId]
                CatalogueRagaSummaryDto(
                    id = id.toKotlinUuid(),
                    name = row[RagasTable.name],
                    matchingAliases = matching[id].orEmpty(),
                    publishedCompositionCount = counts[id] ?: 0L,
                    melakartaNumber = row[RagasTable.melakartaNumber],
                    parentRagaName = parentId?.let { parents[it] },
                    parentMelakartaNumber = parentId?.let { parentMelas[it] },
                )
            },
            total = total,
            page = page,
            pageSize = pageSize,
        )
    }

    suspend fun findRaga(
        id: Uuid,
        visibility: CatalogueVisibility = CatalogueVisibility.V1,
    ): CatalogueRagaDetailDto? = DatabaseFactory.dbQuery {
        val javaId = id.toJavaUuid()
        val row = RagasTable.selectAll().where { RagasTable.id eq javaId }.singleOrNull()
            ?: return@dbQuery null
        val aliases = RagaAliasesTable
            .selectAll()
            .where { RagaAliasesTable.ragaId eq javaId }
            .orderBy(RagaAliasesTable.alias to SortOrder.ASC)
            .map { it[RagaAliasesTable.alias] }
        val parentId = row[RagasTable.parentRagaId]
        val parentName = parentId?.let { parentNames(listOf(it))[it] }
        val parentMelakartaNumber = parentId?.let { parentMelakartaNumbers(listOf(it))[it] }
        CatalogueRagaDetailDto(
            id = javaId.toKotlinUuid(),
            name = row[RagasTable.name],
            aliases = aliases,
            publishedCompositionCount = publishedCountsByRaga(listOf(javaId), visibility)[javaId] ?: 0L,
            melakartaNumber = row[RagasTable.melakartaNumber],
            parentRagaId = parentId?.toKotlinUuid(),
            parentRagaName = parentName,
            arohanam = row[RagasTable.arohanam],
            avarohanam = row[RagasTable.avarohanam],
            nomenclatureLinks = nomenclatureLinks(javaId, parentId, parentName),
            parentMelakartaNumber = parentMelakartaNumber,
        )
    }

    suspend fun searchComposers(
        query: String?,
        page: Int,
        pageSize: Int,
        visibility: CatalogueVisibility = CatalogueVisibility.V1,
    ): CataloguePagedResponse<CatalogueComposerSummaryDto> = DatabaseFactory.dbQuery {
        val condition = composerFilter(query)
        val total = ComposersTable.select(ComposersTable.id).where { condition }.count()
        val offset = page.toLong() * pageSize.toLong()
        val rows = ComposersTable
            .selectAll()
            .where { condition }
            .orderBy(ComposersTable.nameNormalized to SortOrder.ASC, ComposersTable.id to SortOrder.ASC)
            .limit(pageSize)
            .offset(offset)
            .toList()
        val ids = rows.map { it[ComposersTable.id].value }
        val counts = publishedCountsByComposer(ids, visibility)
        val matching = matchingComposerAliases(ids, query)
        CataloguePagedResponse(
            items = rows.map { row ->
                val id = row[ComposersTable.id].value
                CatalogueComposerSummaryDto(
                    id = id.toKotlinUuid(),
                    name = row[ComposersTable.name],
                    matchingAliases = matching[id].orEmpty(),
                    publishedCompositionCount = counts[id] ?: 0L,
                )
            },
            total = total,
            page = page,
            pageSize = pageSize,
        )
    }

    suspend fun findComposer(
        id: Uuid,
        visibility: CatalogueVisibility = CatalogueVisibility.V1,
    ): CatalogueComposerDetailDto? = DatabaseFactory.dbQuery {
        val javaId = id.toJavaUuid()
        val row = ComposersTable.selectAll().where { ComposersTable.id eq javaId }.singleOrNull()
            ?: return@dbQuery null
        val aliases = ComposerAliasesTable
            .selectAll()
            .where { ComposerAliasesTable.composerId eq javaId }
            .orderBy(ComposerAliasesTable.aliasNormalized to SortOrder.ASC)
            .map { it[ComposerAliasesTable.aliasNormalized] }
        CatalogueComposerDetailDto(
            id = javaId.toKotlinUuid(),
            name = row[ComposersTable.name],
            aliases = aliases,
            publishedCompositionCount = publishedCountsByComposer(listOf(javaId), visibility)[javaId] ?: 0L,
            birthYear = row[ComposersTable.birthYear],
            deathYear = row[ComposersTable.deathYear],
            place = row[ComposersTable.place],
        )
    }

    suspend fun findDiscovery(
        visibility: CatalogueVisibility = CatalogueVisibility.V2,
    ): CatalogueDiscoveryDto = DatabaseFactory.dbQuery {
        val firstId = KrithisTable
            .select(KrithisTable.id)
            .where { publishedComposition(visibility) }
            .orderBy(KrithisTable.titleNormalized to SortOrder.ASC, KrithisTable.id to SortOrder.ASC)
            .limit(1)
            .map { it[KrithisTable.id].value }
            .singleOrNull()
        val feature = firstId?.let { hydrateSummaries(listOf(it)).singleOrNull() }
            ?.let(CatalogueV2DtoMappers::catalogueOrderFeature)
        CatalogueDiscoveryDto(feature = feature, editorialRevision = null)
    }

    private fun publishedComposition(visibility: CatalogueVisibility): Op<Boolean> {
        val published = KrithisTable.workflowState eq WorkflowState.PUBLISHED
        return if (visibility == CatalogueVisibility.V1) {
            published and (KrithisTable.musicalForm neq MusicalForm.UNESTABLISHED)
        } else {
            published
        }
    }

    private fun krithiFilter(
        query: String?,
        composerId: UUID?,
        ragaId: UUID?,
        visibility: CatalogueVisibility,
    ): Op<Boolean> {
        var condition: Op<Boolean> = publishedComposition(visibility)
        query?.trim()?.takeIf { it.isNotEmpty() }?.let { raw ->
            val pattern = literalContains(raw)
            val composerByName = ComposersTable
                .select(ComposersTable.id)
                .where { ComposersTable.nameNormalized like pattern }
            val composerByAlias = ComposerAliasesTable
                .select(ComposerAliasesTable.composerId)
                .where { ComposerAliasesTable.aliasNormalized like pattern }
            val ragaByName = RagasTable
                .select(RagasTable.id)
                .where { RagasTable.nameNormalized like pattern }
            val ragaByAlias = RagaAliasesTable
                .select(RagaAliasesTable.ragaId)
                .where { RagaAliasesTable.alias.lowerCase() like pattern }
            val krithiByRaga = KrithiRagasTable
                .select(KrithiRagasTable.krithiId)
                .where {
                    (KrithiRagasTable.ragaId inSubQuery ragaByName) or
                        (KrithiRagasTable.ragaId inSubQuery ragaByAlias)
                }
            condition = condition and (
                (KrithisTable.titleNormalized like pattern) or
                    (KrithisTable.incipitNormalized like pattern) or
                    (KrithisTable.composerId inSubQuery composerByName) or
                    (KrithisTable.composerId inSubQuery composerByAlias) or
                    (KrithisTable.id inSubQuery krithiByRaga)
                )
        }
        composerId?.let { condition = condition and (KrithisTable.composerId eq it) }
        ragaId?.let { filterId ->
            val membership = KrithiRagasTable
                .select(KrithiRagasTable.krithiId)
                .where { KrithiRagasTable.ragaId eq filterId }
            condition = condition and (KrithisTable.id inSubQuery membership)
        }
        return condition
    }

    private fun ragaFilter(query: String?): Op<Boolean> {
        val raw = query?.trim().orEmpty()
        if (raw.isEmpty()) return Op.TRUE
        val pattern = literalContains(raw)
        val aliasHits = RagaAliasesTable
            .select(RagaAliasesTable.ragaId)
            .where { RagaAliasesTable.alias.lowerCase() like pattern }
        return (RagasTable.nameNormalized like pattern) or (RagasTable.id inSubQuery aliasHits)
    }

    private fun composerFilter(query: String?): Op<Boolean> {
        val raw = query?.trim().orEmpty()
        if (raw.isEmpty()) return Op.TRUE
        val pattern = literalContains(raw)
        val aliasHits = ComposerAliasesTable
            .select(ComposerAliasesTable.composerId)
            .where { ComposerAliasesTable.aliasNormalized like pattern }
        return (ComposersTable.nameNormalized like pattern) or (ComposersTable.id inSubQuery aliasHits)
    }

    private fun hydrateSummaries(ids: List<UUID>): List<CatalogueKrithiSummaryDto> {
        if (ids.isEmpty()) return emptyList()
        val krithis = KrithisTable
            .selectAll()
            .where { KrithisTable.id inList ids }
            .associateBy { it[KrithisTable.id].value }
        val composerIds = krithis.values.map { it[KrithisTable.composerId] }.distinct()
        val composers = ComposersTable
            .selectAll()
            .where { ComposersTable.id inList composerIds }
            .associate { it[ComposersTable.id].value to it[ComposersTable.name] }
        val talaIds = krithis.values.mapNotNull { it[KrithisTable.talaId] }.distinct()
        val talas = if (talaIds.isEmpty()) {
            emptyMap()
        } else {
            TalasTable
                .selectAll()
                .where { TalasTable.id inList talaIds }
                .associate { it[TalasTable.id].value to it[TalasTable.name] }
        }
        val ragasByKrithi = linkedMapOf<UUID, MutableList<CatalogueRagaRefDto>>()
        KrithiRagasTable
            .join(RagasTable, JoinType.INNER, KrithiRagasTable.ragaId, RagasTable.id)
            .select(
                KrithiRagasTable.krithiId,
                KrithiRagasTable.ragaId,
                KrithiRagasTable.orderIndex,
                KrithiRagasTable.section,
                RagasTable.name,
            )
            .where { KrithiRagasTable.krithiId inList ids }
            .orderBy(KrithiRagasTable.orderIndex to SortOrder.ASC)
            .forEach { row ->
                val krithiId = row[KrithiRagasTable.krithiId]
                ragasByKrithi.getOrPut(krithiId) { mutableListOf() }.add(
                    CatalogueRagaRefDto(
                        id = row[KrithiRagasTable.ragaId].toKotlinUuid(),
                        name = row[RagasTable.name],
                        orderIndex = row[KrithiRagasTable.orderIndex],
                        section = row[KrithiRagasTable.section]?.let { RagaSectionDto.valueOf(it.name) },
                    ),
                )
            }
        return ids.mapNotNull { id ->
            val row = krithis[id] ?: return@mapNotNull null
            val composerId = row[KrithisTable.composerId]
            val composerName = composers[composerId] ?: return@mapNotNull null
            val talaId = row[KrithisTable.talaId]
            CatalogueKrithiSummaryDto(
                id = id.toKotlinUuid(),
                title = row[KrithisTable.title],
                incipit = row[KrithisTable.incipit],
                composer = CatalogueComposerRefDto(composerId.toKotlinUuid(), composerName),
                ragas = ragasByKrithi[id].orEmpty(),
                tala = talaId?.let { tid ->
                    talas[tid]?.let { CatalogueTalaRefDto(tid.toKotlinUuid(), it) }
                },
                musicalForm = row[KrithisTable.musicalForm].toDto(),
                isRagamalika = row[KrithisTable.isRagamalika],
            )
        }
    }

    private fun loadVariants(krithiId: UUID): List<CatalogueVariantRefDto> =
        KrithiLyricVariantsTable
            .selectAll()
            .where { KrithiLyricVariantsTable.krithiId eq krithiId }
            .map { row ->
                CatalogueVariantRefDto(
                    id = row[KrithiLyricVariantsTable.id].value.toKotlinUuid(),
                    language = row[KrithiLyricVariantsTable.language].toDto(),
                    script = row[KrithiLyricVariantsTable.script].toDto(),
                    transliterationScheme = row[KrithiLyricVariantsTable.transliterationScheme],
                    isPrimary = row[KrithiLyricVariantsTable.isPrimary],
                    label = row[KrithiLyricVariantsTable.variantLabel],
                    sourceReference = row[KrithiLyricVariantsTable.sourceReference],
                )
            }

    private fun publishedCountsByRaga(
        ragaIds: List<UUID>,
        visibility: CatalogueVisibility,
    ): Map<UUID, Long> {
        if (ragaIds.isEmpty()) return emptyMap()
        val counts = mutableMapOf<UUID, MutableSet<UUID>>()
        KrithiRagasTable
            .join(KrithisTable, JoinType.INNER, KrithiRagasTable.krithiId, KrithisTable.id)
            .select(KrithiRagasTable.ragaId, KrithisTable.id)
            .where {
                (KrithiRagasTable.ragaId inList ragaIds) and publishedComposition(visibility)
            }
            .forEach { row ->
                counts.getOrPut(row[KrithiRagasTable.ragaId]) { mutableSetOf() }
                    .add(row[KrithisTable.id].value)
            }
        return counts.mapValues { it.value.size.toLong() }
    }

    private fun publishedCountsByComposer(
        composerIds: List<UUID>,
        visibility: CatalogueVisibility,
    ): Map<UUID, Long> {
        if (composerIds.isEmpty()) return emptyMap()
        val counts = mutableMapOf<UUID, Long>()
        KrithisTable
            .select(KrithisTable.composerId, KrithisTable.id)
            .where {
                (KrithisTable.composerId inList composerIds) and publishedComposition(visibility)
            }
            .forEach { row ->
                val composerId = row[KrithisTable.composerId]
                counts[composerId] = (counts[composerId] ?: 0L) + 1L
            }
        return counts
    }

    private fun parentMelakartaNumbers(parentIds: List<UUID>): Map<UUID, Int> {
        if (parentIds.isEmpty()) return emptyMap()
        return RagasTable
            .select(RagasTable.id, RagasTable.melakartaNumber)
            .where { RagasTable.id inList parentIds }
            .mapNotNull { row ->
                val number = row[RagasTable.melakartaNumber] ?: return@mapNotNull null
                row[RagasTable.id].value to number
            }
            .toMap()
    }

    private fun deityReference(id: UUID?): CatalogueReferenceDto? {
        if (id == null) return null
        return DeitiesTable
            .select(DeitiesTable.id, DeitiesTable.name)
            .where { DeitiesTable.id eq id }
            .singleOrNull()
            ?.let { CatalogueReferenceDto(it[DeitiesTable.id].value.toKotlinUuid(), it[DeitiesTable.name]) }
    }

    private fun templeReference(id: UUID?): CatalogueReferenceDto? {
        if (id == null) return null
        return TemplesTable
            .select(TemplesTable.id, TemplesTable.name)
            .where { TemplesTable.id eq id }
            .singleOrNull()
            ?.let { CatalogueReferenceDto(it[TemplesTable.id].value.toKotlinUuid(), it[TemplesTable.name]) }
    }

    private fun parentNames(parentIds: List<UUID>): Map<UUID, String> {
        if (parentIds.isEmpty()) return emptyMap()
        return RagasTable
            .selectAll()
            .where { RagasTable.id inList parentIds }
            .associate { it[RagasTable.id].value to it[RagasTable.name] }
    }

    private fun matchingRagaAliases(ragaIds: List<UUID>, query: String?): Map<UUID, List<String>> {
        if (ragaIds.isEmpty()) return emptyMap()
        val needle = query?.trim()?.lowercase().orEmpty()
        val grouped = mutableMapOf<UUID, MutableList<String>>()
        RagaAliasesTable
            .selectAll()
            .where { RagaAliasesTable.ragaId inList ragaIds }
            .orderBy(RagaAliasesTable.alias to SortOrder.ASC)
            .forEach { row ->
                val alias = row[RagaAliasesTable.alias]
                if (needle.isEmpty() || alias.lowercase().contains(needle)) {
                    grouped.getOrPut(row[RagaAliasesTable.ragaId]) { mutableListOf() }.add(alias)
                }
            }
        return if (needle.isEmpty()) emptyMap() else grouped
    }

    private fun matchingComposerAliases(composerIds: List<UUID>, query: String?): Map<UUID, List<String>> {
        if (composerIds.isEmpty()) return emptyMap()
        val needle = query?.trim()?.lowercase().orEmpty()
        if (needle.isEmpty()) return emptyMap()
        val grouped = mutableMapOf<UUID, MutableList<String>>()
        ComposerAliasesTable
            .selectAll()
            .where { ComposerAliasesTable.composerId inList composerIds }
            .orderBy(ComposerAliasesTable.aliasNormalized to SortOrder.ASC)
            .forEach { row ->
                val alias = row[ComposerAliasesTable.aliasNormalized]
                if (alias.contains(needle)) {
                    grouped.getOrPut(row[ComposerAliasesTable.composerId]) { mutableListOf() }.add(alias)
                }
            }
        return grouped
    }

    private fun nomenclatureLinks(
        ragaId: UUID,
        parentId: UUID?,
        parentName: String?,
    ): List<CatalogueNomenclatureLinkDto> {
        val links = mutableListOf<CatalogueNomenclatureLinkDto>()
        if (parentId != null && parentName != null) {
            links.add(
                CatalogueNomenclatureLinkDto(
                    relatedRagaId = parentId.toKotlinUuid(),
                    relatedRagaName = parentName,
                    relationLabel = "parent",
                ),
            )
        }
        val relations = RagaRelationsTable
            .selectAll()
            .where { (RagaRelationsTable.fromRagaId eq ragaId) or (RagaRelationsTable.toRagaId eq ragaId) }
            .toList()
        val relatedIds = relations.map { row ->
            if (row[RagaRelationsTable.fromRagaId] == ragaId) {
                row[RagaRelationsTable.toRagaId]
            } else {
                row[RagaRelationsTable.fromRagaId]
            }
        }.distinct()
        val names = parentNames(relatedIds)
        relations.forEach { row ->
            val relatedId = if (row[RagaRelationsTable.fromRagaId] == ragaId) {
                row[RagaRelationsTable.toRagaId]
            } else {
                row[RagaRelationsTable.fromRagaId]
            }
            val name = names[relatedId] ?: return@forEach
            links.add(
                CatalogueNomenclatureLinkDto(
                    relatedRagaId = relatedId.toKotlinUuid(),
                    relatedRagaName = name,
                    relationLabel = row[RagaRelationsTable.relation],
                ),
            )
        }
        return links
    }

    private fun literalContains(raw: String): LikePattern =
        LikePattern(CatalogueLike.containsPattern(raw), CatalogueLike.ESCAPE_CHAR)
}
