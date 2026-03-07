package com.sangita.grantha.backend.api.services

import com.sangita.grantha.backend.api.services.scraping.StructuralVotingEngine
import com.sangita.grantha.backend.dal.SangitaDal
import com.sangita.grantha.shared.domain.model.import.CanonicalExtractionDto
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import org.slf4j.LoggerFactory
import kotlin.uuid.Uuid

/**
 * Runs structural voting for Krithis with multiple source evidence records.
 *
 * Extracted from ExtractionResultProcessor (TRACK-075).
 */
class StructuralVotingProcessor(
    private val dal: SangitaDal,
    private val votingEngine: StructuralVotingEngine,
) {
    private val logger = LoggerFactory.getLogger(javaClass)
    private val json = Json { ignoreUnknownKeys = true }

    /**
     * Run structural voting for a Krithi that has multiple source evidence records.
     */
    suspend fun runVotingForKrithi(krithiId: Uuid): Boolean {
        val evidence = dal.sourceEvidence.getKrithiEvidence(krithiId) ?: return false
        if (evidence.sources.size < 2) return false

        // Build voting candidates from source evidence
        val candidates = evidence.sources.mapNotNull { source ->
            val rawExtraction = source.rawExtraction ?: return@mapNotNull null
            try {
                val extraction = json.decodeFromString<CanonicalExtractionDto>(rawExtraction)
                StructuralVotingEngine.SectionCandidate(
                    sections = extraction.sections.map { section ->
                        ScrapedSectionDto(
                            type = mapCanonicalSectionType(section.type),
                            text = section.label ?: section.type.name,
                            label = section.label,
                        )
                    },
                    isAuthoritySource = source.sourceTier <= 2,
                    label = source.sourceName,
                )
            } catch (e: Exception) {
                logger.warn("Failed to parse extraction for voting: ${e.message}")
                null
            }
        }

        if (candidates.size < 2) return false

        val bestStructure = votingEngine.pickBestStructure(candidates)
        if (bestStructure.isEmpty()) return false

        // Determine consensus type
        val allSame = candidates.all { candidate ->
            candidate.sections.map { it.type } == bestStructure.map { it.type }
        }
        val consensusType = when {
            allSame -> "UNANIMOUS"
            candidates.any { it.isAuthoritySource } -> "AUTHORITY_OVERRIDE"
            else -> "MAJORITY"
        }

        // Build participating sources JSON
        val participatingJson = json.encodeToString(
            ListSerializer(String.serializer()),
            evidence.sources.map { it.sourceName },
        )

        // Build consensus structure JSON
        val consensusJson = json.encodeToString(
            ListSerializer(String.serializer()),
            bestStructure.map { "${it.type.name}:${it.label ?: ""}" },
        )

        // Build dissenting sources
        val dissentingSources = candidates.filter { candidate ->
            candidate.sections.map { it.type } != bestStructure.map { it.type }
        }
        val dissentingJson = json.encodeToString(
            ListSerializer(String.serializer()),
            dissentingSources.mapNotNull { it.label },
        )

        // Determine confidence
        val confidence = when {
            allSame -> "HIGH"
            candidates.count { c -> c.sections.map { it.type } == bestStructure.map { it.type } } > candidates.size / 2 -> "MEDIUM"
            else -> "LOW"
        }

        dal.structuralVoting.createVotingRecord(
            krithiId = krithiId,
            participatingSources = participatingJson,
            consensusStructure = consensusJson,
            consensusType = consensusType,
            confidence = confidence,
            dissentingSources = dissentingJson,
        )

        val votingMetadata = buildJsonObject {
            put("consensusType", consensusType)
            put("confidence", confidence)
            put("sourceCount", evidence.sources.size)
        }
        dal.auditLogs.append(
            action = "STRUCTURAL_VOTING_DECISION",
            entityTable = "structural_vote_log",
            entityId = krithiId,
            metadata = votingMetadata.toString(),
        )

        return true
    }

    private fun mapCanonicalSectionType(type: com.sangita.grantha.shared.domain.model.import.CanonicalSectionType): com.sangita.grantha.shared.domain.model.RagaSectionDto = when (type) {
        com.sangita.grantha.shared.domain.model.import.CanonicalSectionType.PALLAVI -> com.sangita.grantha.shared.domain.model.RagaSectionDto.PALLAVI
        com.sangita.grantha.shared.domain.model.import.CanonicalSectionType.ANUPALLAVI -> com.sangita.grantha.shared.domain.model.RagaSectionDto.ANUPALLAVI
        com.sangita.grantha.shared.domain.model.import.CanonicalSectionType.CHARANAM -> com.sangita.grantha.shared.domain.model.RagaSectionDto.CHARANAM
        com.sangita.grantha.shared.domain.model.import.CanonicalSectionType.SAMASHTI_CHARANAM -> com.sangita.grantha.shared.domain.model.RagaSectionDto.SAMASHTI_CHARANAM
        com.sangita.grantha.shared.domain.model.import.CanonicalSectionType.CHITTASWARAM -> com.sangita.grantha.shared.domain.model.RagaSectionDto.CHITTASWARAM
        com.sangita.grantha.shared.domain.model.import.CanonicalSectionType.SWARA_SAHITYA -> com.sangita.grantha.shared.domain.model.RagaSectionDto.SWARA_SAHITYA
        com.sangita.grantha.shared.domain.model.import.CanonicalSectionType.MADHYAMA_KALA -> com.sangita.grantha.shared.domain.model.RagaSectionDto.MADHYAMA_KALA
        com.sangita.grantha.shared.domain.model.import.CanonicalSectionType.OTHER -> com.sangita.grantha.shared.domain.model.RagaSectionDto.OTHER
    }
}
