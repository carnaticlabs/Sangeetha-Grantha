package com.sangita.grantha.shared.presentation.entities

import com.sangita.grantha.shared.mobile.fixture.CatalogueFixtures
import com.sangita.grantha.shared.mobile.fixture.FixtureCatalogueApi
import com.sangita.grantha.shared.mobile.repository.CatalogueRepository
import com.sangita.grantha.shared.mobile.usage.MobileSession
import com.sangita.grantha.shared.presentation.components.LoadState
import com.sangita.grantha.shared.presentation.explore.ragaRelationshipCaption
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.uuid.Uuid

@OptIn(ExperimentalCoroutinesApi::class)
class EntityDetailPresenterTest {
    @Test
    fun ragaDetailUsesParentMelakartaAndExactWorksFilter() = runTest {
        val presenter = presenter()
        presenter.openRaga(CatalogueFixtures.hamsadhvaniId)
        advanceUntilIdle()
        val raga = presenter.state.value.raga
        requireNotNull(raga)
        assertEquals("Hamsadhvani", raga.name)
        assertEquals(
            "Janya of Dheerasankarabharanam (Mela 29)",
            ragaRelationshipCaption(raga.melakartaNumber, raga.parentRagaName, raga.parentMelakartaNumber),
        )
        assertEquals(CatalogueFixtures.vatapiId, presenter.state.value.works.single().id)
        assertEquals(LoadState.Idle, presenter.state.value.load)
    }

    @Test
    fun composerDetailLoadsExactComposerWorks() = runTest {
        val presenter = presenter()
        presenter.openComposer(CatalogueFixtures.tyagarajaId)
        advanceUntilIdle()
        assertEquals("Tyagaraja", presenter.state.value.composer?.name)
        assertEquals(CatalogueFixtures.endaroId, presenter.state.value.works.single().id)
    }

    @Test
    fun missingEntityIsNotFound() = runTest {
        val presenter = presenter()
        presenter.openRaga(Uuid.parse("00000000-0000-4000-8000-000000000000"))
        advanceUntilIdle()
        val load = presenter.state.value.load
        assertEquals(true, load is LoadState.Error && !load.retryable)
    }

    private fun kotlinx.coroutines.test.TestScope.presenter() = EntityDetailPresenter(
        catalogue = CatalogueRepository(FixtureCatalogueApi()),
        session = MobileSession(clockMs = { 0L }),
        scope = this,
    )
}
