package com.sangita.grantha.backend.api.services

import com.sangita.grantha.backend.dal.SangitaDal
import com.sangita.grantha.shared.domain.model.ComposerDto
import com.sangita.grantha.shared.domain.model.ImportedKrithiDto
import com.sangita.grantha.shared.domain.model.RagaDto
import com.sangita.grantha.shared.domain.model.TalaDto
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.Serializable
import org.slf4j.LoggerFactory
import java.time.Instant
import kotlin.math.max
import kotlin.uuid.toKotlinUuid

@Serializable
data class ResolutionResult(
    val composerCandidates: List<Candidate<ComposerDto>>,
    val ragaCandidates: List<Candidate<RagaDto>>,
    val talaCandidates: List<Candidate<TalaDto>>,
    val resolved: Boolean = false
)

@Serializable
data class Candidate<T>(
    val entity: T,
    val score: Int,
    val confidence: String // HIGH, MEDIUM, LOW
)

interface IEntityResolver {
    /**
     * Resolve imported krithi metadata against reference data.
     */
    suspend fun resolve(importedKrithi: ImportedKrithiDto): ResolutionResult
}

class EntityResolutionServiceImpl(
    private val dal: SangitaDal,
    private val normalizer: NameNormalizationService = NameNormalizationService()
) : IEntityResolver {
    private val logger = LoggerFactory.getLogger(javaClass)

    companion object {
        private const val CACHE_TTL_MINUTES = 15L
    }

    private val cacheMutex = Mutex()
    private var lastCacheUpdate: Instant = Instant.MIN
    private val cacheTtl = java.time.Duration.ofMinutes(CACHE_TTL_MINUTES)

    private var cachedComposers: List<ComposerDto> = emptyList()
    private var cachedRagas: List<RagaDto> = emptyList()
    private var cachedTalas: List<TalaDto> = emptyList()
    
    // Normalized maps for O(1) lookup: NormalizedName -> Entity
    private var composerMap: Map<String, ComposerDto> = emptyMap()
    private var ragaMap: Map<String, RagaDto> = emptyMap()
    private var talaMap: Map<String, TalaDto> = emptyMap()

    private suspend fun ensureCache() {
        if (Instant.now().isBefore(lastCacheUpdate.plus(cacheTtl)) && cachedComposers.isNotEmpty()) return

        cacheMutex.withLock {
            if (Instant.now().isBefore(lastCacheUpdate.plus(cacheTtl)) && cachedComposers.isNotEmpty()) return
            
            cachedComposers = dal.composers.listAll()
            cachedRagas = dal.ragas.listAll()
            cachedTalas = dal.talas.listAll()
            
            // Re-normalize reference data to ensure matching rules are consistent with incoming data
            // Fix: Use groupBy to handle collisions (multiple entities may normalize to same key)
            composerMap = cachedComposers.groupBy { normalizer.normalizeComposer(it.name) ?: it.name.lowercase() }
                .mapValues { (_, group) ->
                    if (group.size > 1) {
                        logger.warn("Normalization collision for composers: ${group.map { it.name }}")
                    }
                    group.first() // Use first entity if collision
                }
            ragaMap = cachedRagas.groupBy { normalizer.normalizeRaga(it.name) ?: it.name.lowercase() }
                .mapValues { (_, group) ->
                    if (group.size > 1) {
                        logger.warn("Normalization collision for ragas: ${group.map { it.name }}")
                    }
                    group.first()
                }
            talaMap = cachedTalas.groupBy { normalizer.normalizeTala(it.name) ?: it.name.lowercase() }
                .mapValues { (_, group) ->
                    if (group.size > 1) {
                        logger.warn("Normalization collision for talas: ${group.map { it.name }}")
                    }
                    group.first()
                }
            
            lastCacheUpdate = Instant.now()
        }
    }

    override suspend fun resolve(importedKrithi: ImportedKrithiDto): ResolutionResult {
        ensureCache()

        val normComposer = normalizer.normalizeComposer(importedKrithi.rawComposer)
        val normRaga = normalizer.normalizeRaga(importedKrithi.rawRaga)
        val normTala = normalizer.normalizeTala(importedKrithi.rawTala)

        // TRACK-013: Two-tier caching strategy
        // 1. Database cache check (persistent across restarts)
        // 2. In-memory exact match (O(1))
        // 3. Fuzzy match fallback (O(N) * L)
        val composerCandidates = resolveWithCache(
            entityType = "composer",
            rawName = importedKrithi.rawComposer,
            normalized = normComposer,
            exactMatch = normComposer?.let { composerMap[it] },
            allEntities = cachedComposers
        ) { normalizer.normalizeComposer(it.name) ?: it.name.lowercase() }

        val ragaCandidates = resolveWithCache(
            entityType = "raga",
            rawName = importedKrithi.rawRaga,
            normalized = normRaga,
            exactMatch = normRaga?.let { ragaMap[it] },
            allEntities = cachedRagas
        ) { normalizer.normalizeRaga(it.name) ?: it.name.lowercase() }

        val talaCandidates = resolveWithCache(
            entityType = "tala",
            rawName = importedKrithi.rawTala,
            normalized = normTala,
            exactMatch = normTala?.let { talaMap[it] },
            allEntities = cachedTalas
        ) { normalizer.normalizeTala(it.name) ?: it.name.lowercase() }

        val fullyResolved = composerCandidates.isNotEmpty() &&
                            ragaCandidates.isNotEmpty() &&
                            (importedKrithi.rawTala == null || talaCandidates.isNotEmpty())

        return ResolutionResult(
            composerCandidates = composerCandidates,
            ragaCandidates = ragaCandidates,
            talaCandidates = talaCandidates,
            resolved = fullyResolved
        )
    }

    // TRACK-013: Two-tier caching with database persistence
    private suspend fun <T : Any> resolveWithCache(
        entityType: String,
        rawName: String?,
        normalized: String?,
        exactMatch: T?,
        allEntities: List<T>,
        normalizedNameSelector: (T) -> String
    ): List<Candidate<T>> {
        if (normalized.isNullOrBlank()) return emptyList()

        // 1. Check database cache first (persistent across restarts)
        val cached = dal.entityResolutionCache.findByNormalizedName(entityType, normalized)
        if (cached != null && cached.confidence >= 90) {
            // HIGH confidence cache hit - use it directly
            val entity = allEntities.find { getEntityId(it) == cached.resolvedEntityId }
            if (entity != null) {
                logger.debug("Database cache hit for {} '{}' -> {}", entityType, normalized, getEntityName(entity))
                return listOf(Candidate(entity, cached.confidence, "HIGH"))
            } else {
                // Cache entry exists but entity not found - invalidate cache
                logger.warn("Database cache entry for {} '{}' references non-existent entity {}",
                    entityType, normalized, cached.resolvedEntityId)
                dal.entityResolutionCache.deleteByEntityId(entityType, cached.resolvedEntityId)
            }
        }

        // 2. In-memory exact match (O(1))
        if (exactMatch != null) {
            val entityId = getEntityId(exactMatch)
            // Cache the exact match in database
            dal.entityResolutionCache.save(
                entityType = entityType,
                rawName = rawName ?: normalized,
                normalizedName = normalized,
                resolvedEntityId = entityId,
                confidence = 100
            )
            logger.debug("Exact match for {} '{}' -> {}, cached to DB", entityType, normalized, getEntityName(exactMatch))
            return listOf(Candidate(exactMatch, 100, "HIGH"))
        }

        // 3. Fuzzy match fallback (O(N) * L)
        val fuzzyResults = match(normalized, allEntities, normalizedNameSelector)

        // Cache high-confidence fuzzy matches
        if (fuzzyResults.isNotEmpty()) {
            val topCandidate = fuzzyResults.first()
            if (topCandidate.score >= 90) {
                val entityId = getEntityId(topCandidate.entity)
                dal.entityResolutionCache.save(
                    entityType = entityType,
                    rawName = rawName ?: normalized,
                    normalizedName = normalized,
                    resolvedEntityId = entityId,
                    confidence = topCandidate.score
                )
                logger.debug("Fuzzy match (score={}) for {} '{}' -> {}, cached to DB",
                    topCandidate.score, entityType, normalized, getEntityName(topCandidate.entity))
            }
        }

        return fuzzyResults
    }

    // Helper to extract entity ID (uses reflection-free approach)
    private fun <T : Any> getEntityId(entity: T): kotlin.uuid.Uuid {
        return when (entity) {
            is ComposerDto -> entity.id
            is RagaDto -> entity.id
            is TalaDto -> entity.id
            else -> throw IllegalArgumentException("Unknown entity type: ${entity::class.simpleName}")
        }
    }

    // Helper to extract entity name
    private fun <T : Any> getEntityName(entity: T): String {
        return when (entity) {
            is ComposerDto -> entity.name
            is RagaDto -> entity.name
            is TalaDto -> entity.name
            else -> "Unknown"
        }
    }

    /**
     * Invalidate cache when entities are created/updated/deleted
     * Call this from entity CRUD operations
     */
    suspend fun invalidateCache(entityType: String, entityId: kotlin.uuid.Uuid) {
        dal.entityResolutionCache.deleteByEntityId(entityType, entityId)
        // Also clear in-memory cache for this entity type
        cacheMutex.withLock {
            when (entityType) {
                "composer" -> cachedComposers = emptyList()
                "raga" -> cachedRagas = emptyList()
                "tala" -> cachedTalas = emptyList()
            }
            lastCacheUpdate = Instant.MIN // Force refresh
        }
        logger.info("Invalidated cache for {} with ID {}", entityType, entityId)
    }

    private fun <T> match(targetNormalized: String?, candidates: List<T>, normalizedNameSelector: (T) -> String): List<Candidate<T>> {
        if (targetNormalized.isNullOrBlank()) return emptyList()

        return candidates.map { candidate ->
            val candidateNormalized = normalizedNameSelector(candidate)
            val score = ratio(targetNormalized, candidateNormalized)
            val confidence = when {
                score >= 90 -> "HIGH"
                score >= 70 -> "MEDIUM"
                else -> "LOW"
            }
            Candidate(candidate, score, confidence)
        }
        .filter { it.score > 50 }
        .sortedByDescending { it.score }
        .take(5)
    }

    // Simple Levenshtein ratio implementation (0-100)
    private fun ratio(s1: String, s2: String): Int {
        val rows = s1.length + 1
        val cols = s2.length + 1
        val distance = Array(rows) { IntArray(cols) }

        for (i in 0 until rows) distance[i][0] = i
        for (j in 0 until cols) distance[0][j] = j

        for (i in 1 until rows) {
            for (j in 1 until cols) {
                val cost = if (s1[i - 1] == s2[j - 1]) 0 else 1
                distance[i][j] = minOf(
                    distance[i - 1][j] + 1,      // deletion
                    distance[i][j - 1] + 1,      // insertion
                    distance[i - 1][j - 1] + cost // substitution
                )
            }
        }

        val maxLen = max(s1.length, s2.length)
        if (maxLen == 0) return 100
        val dist = distance[s1.length][s2.length]
        return ((1.0 - dist.toDouble() / maxLen) * 100).toInt()
    }
}
