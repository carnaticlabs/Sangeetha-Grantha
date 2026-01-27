package com.sangita.grantha.backend.api.support

import com.sangita.grantha.backend.dal.SangitaDal
import com.sangita.grantha.shared.domain.model.ComposerDto
import com.sangita.grantha.shared.domain.model.RagaDto
import com.sangita.grantha.shared.domain.model.TalaDto

object TestFixtures {
    data class ReferenceDataSeed(
        val composer: ComposerDto,
        val raga: RagaDto,
        val tala: TalaDto
    )

    suspend fun seedReferenceData(dal: SangitaDal): ReferenceDataSeed {
        val composer = dal.composers.create(name = "Tyagaraja")
        val raga = dal.ragas.create(name = "Kalyani")
        val tala = dal.talas.create(name = "Adi")
        return ReferenceDataSeed(composer = composer, raga = raga, tala = tala)
    }
}
