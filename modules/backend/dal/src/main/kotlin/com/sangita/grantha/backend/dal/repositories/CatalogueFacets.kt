package com.sangita.grantha.backend.dal.repositories

import com.sangita.grantha.backend.dal.enums.Bhuta
import com.sangita.grantha.backend.dal.enums.Mandalam
import com.sangita.grantha.backend.dal.enums.VibhaktiCase
import com.sangita.grantha.backend.dal.tables.KrithiCycleMembershipsTable
import com.sangita.grantha.backend.dal.tables.KrithiTagsTable
import com.sangita.grantha.backend.dal.tables.KrithisTable
import com.sangita.grantha.backend.dal.tables.TagsTable
import com.sangita.grantha.backend.dal.tables.TemplesTable
import com.sangita.grantha.shared.domain.model.catalogue.CatalogueFacetChipDto
import com.sangita.grantha.shared.domain.model.catalogue.CatalogueFacetDto
import com.sangita.grantha.shared.domain.model.catalogue.CatalogueFacetKeyDto
import com.sangita.grantha.shared.domain.model.catalogue.CatalogueFacetListDto
import com.sangita.grantha.shared.domain.model.catalogue.CatalogueFacetValueDto
import com.sangita.grantha.shared.domain.model.catalogue.CatalogueMandalamDto
import com.sangita.grantha.shared.domain.model.catalogue.CatalogueTempleElementDto
import java.util.UUID
import org.jetbrains.exposed.v1.core.JoinType
import org.jetbrains.exposed.v1.core.Join
import org.jetbrains.exposed.v1.core.Op
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.countDistinct
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.inList
import org.jetbrains.exposed.v1.core.inSubQuery
import org.jetbrains.exposed.v1.core.isNotNull
import org.jetbrains.exposed.v1.jdbc.select
import org.jetbrains.exposed.v1.jdbc.selectAll

/** `krithi_tags.source` written only by the reviewed cycle seeds; the single guard constant. */
internal const val REVIEWED_CYCLE_SOURCE = "reviewed_cycle"

/**
 * The reviewed guard: a membership is public only when the same (kriti, tag) pair carries a
 * `krithi_tags` row written by a reviewed seed. `krithi_cycle_memberships` has no review column,
 * so row existence alone is not enough. Shared by collections and the collection/tune facets so
 * they cannot drift.
 */
internal fun reviewedMembershipJoin(): Join =
    KrithiCycleMembershipsTable.join(
        KrithiTagsTable,
        JoinType.INNER,
        onColumn = KrithiCycleMembershipsTable.krithiId,
        otherColumn = KrithiTagsTable.krithiId,
        additionalConstraint = { KrithiCycleMembershipsTable.tagId eq KrithiTagsTable.tagId },
    )

internal val reviewedMembershipPredicate: Op<Boolean>
    get() = KrithiTagsTable.sourceInfo eq REVIEWED_CYCLE_SOURCE

/** Reviewed memberships with their tag, for facets (no `krithis` join: used inside sub-queries). */
private fun reviewedTaggedJoin(): Join =
    reviewedMembershipJoin().join(TagsTable, JoinType.INNER, KrithiCycleMembershipsTable.tagId, TagsTable.id)

/** Reviewed memberships with tag and kriti, for grouped listing and counts. */
private fun reviewedTaggedKrithiJoin(): Join =
    reviewedTaggedJoin().join(KrithisTable, JoinType.INNER, KrithiCycleMembershipsTable.krithiId, KrithisTable.id)

/** One selected value per facet. Unset means the facet is not filtering. */
data class FacetSelection(
    val vibhakti: String? = null,
    val collection: String? = null,
    val tune: String? = null,
    val mandalam: String? = null,
    val element: String? = null,
) {
    operator fun get(key: CatalogueFacetKeyDto): String? = when (key) {
        CatalogueFacetKeyDto.VIBHAKTI -> vibhakti
        CatalogueFacetKeyDto.COLLECTION -> collection
        CatalogueFacetKeyDto.TUNE -> tune
        CatalogueFacetKeyDto.MANDALAM -> mandalam
        CatalogueFacetKeyDto.ELEMENT -> element
    }

    fun without(key: CatalogueFacetKeyDto): FacetSelection = when (key) {
        CatalogueFacetKeyDto.VIBHAKTI -> copy(vibhakti = null)
        CatalogueFacetKeyDto.COLLECTION -> copy(collection = null)
        CatalogueFacetKeyDto.TUNE -> copy(tune = null)
        CatalogueFacetKeyDto.MANDALAM -> copy(mandalam = null)
        CatalogueFacetKeyDto.ELEMENT -> copy(element = null)
    }

    val isEmpty: Boolean get() = CatalogueFacetKeyDto.entries.all { this[it] == null }

    companion object {
        val NONE = FacetSelection()
    }
}

/** Lazily computed eligible temple ids, shared by the temple-derived facets of one request. */
internal class FacetContext {
    val eligibleTempleIds: Set<UUID> by lazy { TempleEligibility.eligibleIds() }
}

/**
 * One entry per facet. Adding a facet is adding an entry here and a member of
 * [CatalogueFacetKeyDto]; no endpoint, parser or response shape changes (TRACK-153 Spec A.5).
 * All functions must run inside `DatabaseFactory.dbQuery`.
 */
internal abstract class FacetDefinition(val key: CatalogueFacetKeyDto) {
    /** Filter predicate over `krithis` for one already validated [value]. */
    abstract fun predicate(value: String, ctx: FacetContext): Op<Boolean>

    /** Values (ordered) with at least one krithi matching [scope]. */
    abstract fun values(scope: Op<Boolean>, ctx: FacetContext): List<CatalogueFacetValueDto>

    /** Distinct krithi per value matching [scope]; one grouped query. */
    abstract fun counts(scope: Op<Boolean>, ctx: FacetContext): Map<String, Long>

    /** The chip for one kriti, or null (not a chip facet, or the kriti has no value). */
    open fun chip(krithi: ResultRow, ctx: FacetContext): CatalogueFacetChipDto? = null
}

private object VibhaktiFacet : FacetDefinition(CatalogueFacetKeyDto.VIBHAKTI) {
    override fun predicate(value: String, ctx: FacetContext): Op<Boolean> =
        KrithisTable.vibhaktiCase eq VibhaktiCase.valueOf(value)

    override fun values(scope: Op<Boolean>, ctx: FacetContext): List<CatalogueFacetValueDto> {
        val present = KrithisTable.select(KrithisTable.vibhaktiCase)
            .where { scope and KrithisTable.vibhaktiCase.isNotNull() }
            .withDistinct()
            .mapNotNull { it[KrithisTable.vibhaktiCase]?.name }
            .toSet()
        return CatalogueFacetValueDtoOrder.enumOrder(VibhaktiCase.entries.map { it.name }, present)
    }

    override fun counts(scope: Op<Boolean>, ctx: FacetContext): Map<String, Long> {
        val count = KrithisTable.id.countDistinct()
        return KrithisTable.select(KrithisTable.vibhaktiCase, count)
            .where { scope and KrithisTable.vibhaktiCase.isNotNull() }
            .groupBy(KrithisTable.vibhaktiCase)
            .mapNotNull { row -> row[KrithisTable.vibhaktiCase]?.let { it.name to row[count] } }
            .toMap()
    }

    override fun chip(krithi: ResultRow, ctx: FacetContext): CatalogueFacetChipDto? =
        krithi[KrithisTable.vibhaktiCase]?.let { CatalogueFacetChipDto(key, it.name) }
}

private object CollectionFacet : FacetDefinition(CatalogueFacetKeyDto.COLLECTION) {
    override fun predicate(value: String, ctx: FacetContext): Op<Boolean> {
        val members = reviewedTaggedJoin()
            .select(KrithiCycleMembershipsTable.krithiId)
            .where { reviewedMembershipPredicate and (TagsTable.slug eq value) }
        return KrithisTable.id inSubQuery members
    }

    override fun values(scope: Op<Boolean>, ctx: FacetContext): List<CatalogueFacetValueDto> =
        reviewedTaggedKrithiJoin()
            .select(TagsTable.slug, TagsTable.displayNameEn)
            .where { reviewedMembershipPredicate and scope }
            .withDistinct()
            .map { CatalogueFacetValueDto(it[TagsTable.slug], it[TagsTable.displayNameEn]) }
            .sortedWith(compareBy({ it.label?.lowercase() }, { it.value }))

    override fun counts(scope: Op<Boolean>, ctx: FacetContext): Map<String, Long> {
        val count = KrithisTable.id.countDistinct()
        return reviewedTaggedKrithiJoin()
            .select(TagsTable.slug, count)
            .where { reviewedMembershipPredicate and scope }
            .groupBy(TagsTable.slug)
            .associate { it[TagsTable.slug] to it[count] }
    }
}

/**
 * The Nottusvara cycle is the only cycle whose `axis_value` is a tune (others hold avaranas, lingas,
 * ragas). The schema has no axis-kind on a cycle, so the facet is pinned to this slug; a test fails
 * if the slug stops resolving.
 */
internal const val TUNE_CYCLE_SLUG = "nottusvara-sahitya"

private object TuneFacet : FacetDefinition(CatalogueFacetKeyDto.TUNE) {
    private val inTuneCycle: Op<Boolean>
        get() = reviewedMembershipPredicate and
            (TagsTable.slug eq TUNE_CYCLE_SLUG) and
            KrithiCycleMembershipsTable.axisValue.isNotNull()

    override fun predicate(value: String, ctx: FacetContext): Op<Boolean> {
        val members = reviewedTaggedJoin()
            .select(KrithiCycleMembershipsTable.krithiId)
            .where { inTuneCycle and (KrithiCycleMembershipsTable.axisValue eq value) }
        return KrithisTable.id inSubQuery members
    }

    override fun values(scope: Op<Boolean>, ctx: FacetContext): List<CatalogueFacetValueDto> =
        reviewedTaggedKrithiJoin()
            .select(KrithiCycleMembershipsTable.axisValue)
            .where { inTuneCycle and scope }
            .withDistinct()
            .mapNotNull { it[KrithiCycleMembershipsTable.axisValue]?.takeIf(String::isNotBlank) }
            .sortedWith(compareBy({ it.lowercase() }, { it }))
            .map { CatalogueFacetValueDto(it, it) }

    override fun counts(scope: Op<Boolean>, ctx: FacetContext): Map<String, Long> {
        val count = KrithisTable.id.countDistinct()
        return reviewedTaggedKrithiJoin()
            .select(KrithiCycleMembershipsTable.axisValue, count)
            .where { inTuneCycle and scope }
            .groupBy(KrithiCycleMembershipsTable.axisValue)
            .mapNotNull { row -> row[KrithiCycleMembershipsTable.axisValue]?.let { it to row[count] } }
            .toMap()
    }

    override fun chip(krithi: ResultRow, ctx: FacetContext): CatalogueFacetChipDto? =
        reviewedTaggedJoin()
            .select(KrithiCycleMembershipsTable.axisValue)
            .where { inTuneCycle and (KrithiCycleMembershipsTable.krithiId eq krithi[KrithisTable.id].value) }
            .mapNotNull { it[KrithiCycleMembershipsTable.axisValue]?.takeIf(String::isNotBlank) }
            .minOrNull()
            ?.let { CatalogueFacetChipDto(key, it, it) }
}

/** `mandalam` / `element`: the kriti's own temple column, only when that temple is eligible. */
private abstract class TempleColumnFacet(
    key: CatalogueFacetKeyDto,
    private val order: List<String>,
) : FacetDefinition(key) {
    protected abstract fun valueOf(temple: ResultRow): String?
    protected abstract fun templeIdsWith(value: String, eligible: Set<UUID>): List<UUID>

    override fun predicate(value: String, ctx: FacetContext): Op<Boolean> =
        KrithisTable.templeId inList templeIdsWith(value, ctx.eligibleTempleIds)

    /** Published-matching kriti count per eligible temple, then rolled up to the facet value. */
    private fun perValue(scope: Op<Boolean>, ctx: FacetContext): Map<String, Long> {
        val count = KrithisTable.id.countDistinct()
        val byTemple = KrithisTable.select(KrithisTable.templeId, count)
            .where { scope and (KrithisTable.templeId inList ctx.eligibleTempleIds.toList()) }
            .groupBy(KrithisTable.templeId)
            .mapNotNull { row -> row[KrithisTable.templeId]?.let { it to row[count] } }
            .toMap()
        if (byTemple.isEmpty()) return emptyMap()
        val temples = TemplesTable.selectAll().where { TemplesTable.id inList byTemple.keys.toList() }
        return temples.mapNotNull { t -> valueOf(t)?.let { it to (byTemple[t[TemplesTable.id].value] ?: 0L) } }
            .groupBy({ it.first }, { it.second })
            .mapValues { it.value.sum() }
    }

    override fun values(scope: Op<Boolean>, ctx: FacetContext): List<CatalogueFacetValueDto> =
        CatalogueFacetValueDtoOrder.enumOrder(order, perValue(scope, ctx).keys)

    override fun counts(scope: Op<Boolean>, ctx: FacetContext): Map<String, Long> = perValue(scope, ctx)
}

private object MandalamFacet : TempleColumnFacet(
    CatalogueFacetKeyDto.MANDALAM,
    CatalogueMandalamDto.entries.map { it.name },
) {
    override fun valueOf(temple: ResultRow): String? = temple[TemplesTable.mandalam]?.name

    override fun templeIdsWith(value: String, eligible: Set<UUID>): List<UUID> =
        TemplesTable.select(TemplesTable.id)
            .where { (TemplesTable.mandalam eq Mandalam.valueOf(value)) and (TemplesTable.id inList eligible.toList()) }
            .map { it[TemplesTable.id].value }

    override fun chip(krithi: ResultRow, ctx: FacetContext): CatalogueFacetChipDto? {
        val templeId = krithi[KrithisTable.templeId]?.takeIf { it in ctx.eligibleTempleIds } ?: return null
        return TemplesTable.select(TemplesTable.mandalam)
            .where { TemplesTable.id eq templeId }
            .singleOrNull()?.get(TemplesTable.mandalam)
            ?.let { CatalogueFacetChipDto(key, it.name) }
    }
}

private object ElementFacet : TempleColumnFacet(
    CatalogueFacetKeyDto.ELEMENT,
    CatalogueTempleElementDto.entries.map { it.name },
) {
    override fun valueOf(temple: ResultRow): String? = temple[TemplesTable.bhuta]?.name

    override fun templeIdsWith(value: String, eligible: Set<UUID>): List<UUID> =
        TemplesTable.select(TemplesTable.id)
            .where { (TemplesTable.bhuta eq Bhuta.valueOf(value)) and (TemplesTable.id inList eligible.toList()) }
            .map { it[TemplesTable.id].value }
}

private object CatalogueFacetValueDtoOrder {
    fun enumOrder(declared: List<String>, present: Set<String>): List<CatalogueFacetValueDto> =
        declared.filter { it in present }.map { CatalogueFacetValueDto(it) }
}

/** Facet order is the order of the registry (Spec A.1). */
internal object CatalogueFacets {
    val definitions: List<FacetDefinition> =
        listOf(VibhaktiFacet, CollectionFacet, TuneFacet, MandalamFacet, ElementFacet)

    private val byKey = definitions.associateBy { it.key }

    /** AND of every selected facet's predicate. */
    fun predicate(selection: FacetSelection, ctx: FacetContext): Op<Boolean> {
        var condition: Op<Boolean> = Op.TRUE
        for (definition in definitions) {
            selection[definition.key]?.let { condition = condition and definition.predicate(it, ctx) }
        }
        return condition
    }

    /** Listed values over [scope] only (facet filters ignored); empty facets are omitted. */
    fun list(scope: Op<Boolean>, selection: FacetSelection, withCounts: Boolean, ctx: FacetContext): CatalogueFacetListDto =
        CatalogueFacetListDto(
            definitions.mapNotNull { definition ->
                val values = definition.values(scope, ctx)
                if (values.isEmpty()) return@mapNotNull null
                val counts = if (withCounts) {
                    // Drill-down: every other facet's filter applies, this facet's own does not.
                    definition.counts(scope and predicate(selection.without(definition.key), ctx), ctx)
                } else {
                    null
                }
                CatalogueFacetDto(
                    key = definition.key,
                    values = if (counts == null) values else values.map { it.copy(count = counts[it.value] ?: 0L) },
                )
            },
        )

    /** Valid filter values per facet over [scope]: the validation source for the service. */
    fun valueSets(scope: Op<Boolean>, ctx: FacetContext): Map<CatalogueFacetKeyDto, Set<String>> =
        definitions.associate { it.key to it.values(scope, ctx).map { v -> v.value }.toSet() }

    /** Chips for the reader, in registry order, only for facets that expose one. */
    fun chips(krithi: ResultRow, ctx: FacetContext): List<CatalogueFacetChipDto> =
        definitions.mapNotNull { it.chip(krithi, ctx) }

    fun definition(key: CatalogueFacetKeyDto): FacetDefinition = byKey.getValue(key)
}
