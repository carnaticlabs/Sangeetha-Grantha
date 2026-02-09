package com.sangita.grantha.backend.api.services.scraping

import com.sangita.grantha.backend.api.services.ScrapedSectionDto
import com.sangita.grantha.shared.domain.model.RagaSectionDto

class StructuralVotingEngine {
    data class SectionCandidate(
        val sections: List<ScrapedSectionDto>,
        val isAuthoritySource: Boolean = false,
        val label: String? = null
    )

    fun pickBestStructure(candidates: List<SectionCandidate>): List<ScrapedSectionDto> {
        if (candidates.isEmpty()) return emptyList()

        return candidates
            .filter { it.sections.isNotEmpty() }
            .maxWithOrNull(compareBy<SectionCandidate> { score(it) }
                .thenBy { it.sections.size }
                .thenBy { if (it.isAuthoritySource) 1 else 0 })
            ?.sections
            ?: candidates.first().sections
    }

    private fun score(candidate: SectionCandidate): Double {
        val types = candidate.sections.map { it.type }
        val primaryCount = types.count { it in PRIMARY_SECTIONS }
        val technicalCount = types.count { it in TECHNICAL_SECTIONS }
        val authorityBonus = if (candidate.isAuthoritySource) 0.5 else 0.0

        // Prefer complete structures (primary sections), then technical markers.
        return (primaryCount * 2.0) + (technicalCount * 0.5) + (types.size * 0.1) + authorityBonus
    }

    companion object {
        private val PRIMARY_SECTIONS = setOf(
            RagaSectionDto.PALLAVI,
            RagaSectionDto.ANUPALLAVI,
            RagaSectionDto.CHARANAM,
            RagaSectionDto.SAMASHTI_CHARANAM
        )

        private val TECHNICAL_SECTIONS = setOf(
            RagaSectionDto.MADHYAMA_KALA,
            RagaSectionDto.CHITTASWARAM,
            RagaSectionDto.MUKTAYI_SWARA,
            RagaSectionDto.ETTUGADA_SWARA,
            RagaSectionDto.ETTUGADA_SAHITYA,
            RagaSectionDto.SWARA_SAHITYA,
            RagaSectionDto.VILOMA_CHITTASWARAM,
            RagaSectionDto.SOLKATTU_SWARA,
            RagaSectionDto.ANUBANDHA
        )
    }
}
