package com.sangita.grantha.backend.dal.integration

import com.sangita.grantha.backend.dal.SangitaDalImpl
import com.sangita.grantha.backend.dal.enums.LanguageCode
import com.sangita.grantha.backend.dal.enums.MusicalForm
import com.sangita.grantha.backend.dal.enums.WorkflowState
import com.sangita.grantha.backend.dal.repositories.KrithiCreateParams
import com.sangita.grantha.backend.dal.support.toJavaUuid
import com.sangita.grantha.backend.dal.support.toKotlinUuid
import com.sangita.grantha.backend.testsupport.IntegrationTestBase
import com.sangita.grantha.shared.domain.model.SectionSummaryDto
import com.sangita.grantha.shared.domain.model.VotingParticipantDto
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test

/**
 * Verifies the structural-voting JSONB contract: the `consensus_structure` and
 * `participating_sources` columns must round-trip as typed [SectionSummaryDto] /
 * [VotingParticipantDto] lists (not opaque strings), and dissent must be derived
 * from participant agreement. Guards the frontend⇄backend type reconciliation —
 * regressing to string payloads here would silently break the voting UI.
 */
class StructuralVotingRoundTripTest : IntegrationTestBase() {
    private val dal = SangitaDalImpl()

    @Test
    fun `vote round-trips typed consensus structure and participants`() = runTest {
        val composer = dal.composers.findOrCreate(name = "Tyagaraja")
        val krithi = dal.krithis.create(
            KrithiCreateParams(
                title = "Voting RoundTrip Probe",
                titleNormalized = "voting roundtrip probe",
                composerId = composer.id.toJavaUuid(),
                musicalForm = MusicalForm.KRITHI,
                primaryLanguage = LanguageCode.TE,
                isRagamalika = false,
                workflowState = WorkflowState.DRAFT,
            )
        )

        val consensus = listOf(
            SectionSummaryDto(sectionType = "PALLAVI", orderIndex = 0),
            SectionSummaryDto(sectionType = "ANUPALLAVI", orderIndex = 1, label = "Anupallavi"),
            SectionSummaryDto(sectionType = "CHARANAM", orderIndex = 2),
        )
        val agreeingSourceId = UUID.randomUUID().toKotlinUuid()
        val participants = listOf(
            VotingParticipantDto(
                sourceId = agreeingSourceId,
                sourceName = "Probe Source",
                sourceTier = 1,
                agrees = true,
                proposedStructure = consensus,
                sourceUrl = "https://example.test/probe",
                extractionMethod = "PDF",
            ),
            VotingParticipantDto(
                sourceId = UUID.randomUUID().toKotlinUuid(),
                sourceName = "Dissenting Source",
                sourceTier = 3,
                agrees = false,
                proposedStructure = listOf(SectionSummaryDto(sectionType = "PALLAVI", orderIndex = 0)),
                sourceUrl = "https://example.test/dissent",
                extractionMethod = null,
            ),
        )

        dal.structuralVoting.createVotingRecord(
            krithiId = krithi.id,
            participants = participants,
            consensusStructure = consensus,
            consensusType = "MAJORITY",
            confidence = "MEDIUM",
        )

        // List projection: structured consensus + counts derived from participants.
        val (items, _) = dal.structuralVoting.list(limit = 100)
        val decision = items.first { it.krithiId == krithi.id }
        assertEquals(3, decision.consensusStructure.size)
        assertEquals("ANUPALLAVI", decision.consensusStructure[1].sectionType)
        assertEquals("Anupallavi", decision.consensusStructure[1].label)
        assertEquals(2, decision.sourceCount)
        assertEquals(1, decision.dissentCount)

        // Detail projection: rich participants with agreement + proposed structure.
        val detail = dal.structuralVoting.findById(decision.id)
        assertNotNull(detail)
        assertEquals(2, detail.participants.size)
        assertEquals(3, detail.consensusStructure.size)
        val agreeing = detail.participants.first { it.agrees }
        assertEquals(agreeingSourceId, agreeing.sourceId)
        assertEquals("Probe Source", agreeing.sourceName)
        assertEquals(1, agreeing.sourceTier)
        assertEquals("PDF", agreeing.extractionMethod)
        assertEquals(3, agreeing.proposedStructure.size)
        assertEquals(1, detail.participants.count { !it.agrees })
    }
}
