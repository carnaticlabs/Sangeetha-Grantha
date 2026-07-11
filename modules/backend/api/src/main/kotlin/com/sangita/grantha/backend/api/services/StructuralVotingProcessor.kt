package com.sangita.grantha.backend.api.services

import com.sangita.grantha.backend.api.services.scraping.StructuralVotingEngine
import com.sangita.grantha.backend.dal.SangitaDal
import com.sangita.grantha.shared.domain.model.KrithiEvidenceSourceDto
import com.sangita.grantha.shared.domain.model.SectionSummaryDto
import com.sangita.grantha.shared.domain.model.VotingParticipantDto
import com.sangita.grantha.shared.domain.model.import.CanonicalExtractionDto
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

    /** A source paired with its parsed proposed structure. */
    private data class SourceProposal(
        val source: KrithiEvidenceSourceDto,
        val sections: List<StructuralVotingEngine.VotedSection>,
    )

    /**
     * Run structural voting for a Krithi that has multiple source evidence records.
     */
    suspend fun runVotingForKrithi(krithiId: Uuid): Boolean {
        val evidence = dal.sourceEvidence.getKrithiEvidence(krithiId) ?: return false
        if (evidence.sources.size < 2) return false

        // Parse each source's proposed structure, keeping the source linkage so
        // we can attribute agreement and emit rich VotingParticipant records.
        val proposals = evidence.sources.mapNotNull { source ->
            val rawExtraction = source.rawExtraction ?: return@mapNotNull null
            try {
                val extraction = json.decodeFromString<CanonicalExtractionDto>(rawExtraction)
                val sections = extraction.sections.map { section ->
                    StructuralVotingEngine.VotedSection(
                        type = mapCanonicalSectionType(section.type),
                        label = section.label,
                    )
                }
                SourceProposal(source, sections)
            } catch (e: Exception) {
                logger.warn("Failed to parse extraction for voting: ${e.message}")
                null
            }
        }

        if (proposals.size < 2) return false

        val candidates = proposals.map { proposal ->
            StructuralVotingEngine.SectionCandidate(
                sections = proposal.sections,
                isAuthoritySource = proposal.source.sourceTier <= 2,
                label = proposal.source.sourceName,
            )
        }

        val bestStructure = votingEngine.pickBestStructure(candidates)
        if (bestStructure.isEmpty()) return false

        val bestTypes = bestStructure.map { it.type }

        // Consensus structure as typed, ordered sections.
        val consensusStructure = bestStructure.mapIndexed { idx, section ->
            SectionSummaryDto(
                sectionType = section.type.name,
                orderIndex = idx,
                label = section.label,
            )
        }

        // One participant per source, with agreement derived from whether its
        // section-type sequence matches the chosen consensus.
        val participants = proposals.map { proposal ->
            VotingParticipantDto(
                sourceId = proposal.source.importSourceId,
                sourceName = proposal.source.sourceName,
                sourceTier = proposal.source.sourceTier,
                agrees = proposal.sections.map { it.type } == bestTypes,
                proposedStructure = proposal.sections.mapIndexed { idx, section ->
                    SectionSummaryDto(section.type.name, idx, section.label)
                },
                sourceUrl = proposal.source.sourceUrl,
                extractionMethod = proposal.source.extractionMethod,
            )
        }

        val agreeingCount = participants.count { it.agrees }
        val allSame = agreeingCount == participants.size
        val consensusType = when {
            allSame -> "UNANIMOUS"
            proposals.any { it.source.sourceTier <= 2 } -> "AUTHORITY_OVERRIDE"
            else -> "MAJORITY"
        }
        val confidence = when {
            allSame -> "HIGH"
            agreeingCount > participants.size / 2 -> "MEDIUM"
            else -> "LOW"
        }

        dal.structuralVoting.createVotingRecord(
            krithiId = krithiId,
            participants = participants,
            consensusStructure = consensusStructure,
            consensusType = consensusType,
            confidence = confidence,
        )

        val votingMetadata = buildJsonObject {
            put("consensusType", consensusType)
            put("confidence", confidence)
            put("sourceCount", participants.size)
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
