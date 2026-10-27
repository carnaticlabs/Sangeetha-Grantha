package com.sangita.grantha.shared.presentation.browse

import com.sangita.grantha.shared.domain.model.catalogue.CatalogueFacetKeyDto
import com.sangita.grantha.shared.domain.model.catalogue.CatalogueFacetListDto
import com.sangita.grantha.shared.mobile.fixture.CatalogueFixtures
import com.sangita.grantha.shared.mobile.fixture.FixtureCatalogueApi
import com.sangita.grantha.shared.mobile.network.CatalogueApi
import com.sangita.grantha.shared.mobile.network.LakshanaFacetSelection
import com.sangita.grantha.shared.mobile.repository.CatalogueRepository
import com.sangita.grantha.shared.mobile.usage.InteractionContext
import com.sangita.grantha.shared.mobile.usage.MobileSession
import com.sangita.grantha.shared.presentation.components.LoadState
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.uuid.Uuid

@OptIn(ExperimentalCoroutinesApi::class)
class BrowseFacetsPresenterTest {
    private val countRequests = mutableListOf<Boolean>()

    private val recording = object : CatalogueApi by FixtureCatalogueApi() {
        override suspend fun listFacets(
            query: String?,
            composerId: Uuid?,
            ragaId: Uuid?,
            facets: LakshanaFacetSelection,
            counts: Boolean,
            interaction: InteractionContext,
        ): CatalogueFacetListDto {
            countRequests += counts
            return CatalogueFixtures.facetsFor(composerId, counts)
        }
    }

    @Test
    fun facetListFollowsComposer() = runTest {
        val dikshitar = presenter()
        dikshitar.openComposer(CatalogueFixtures.dikshitarId)
        advanceUntilIdle()
        assertEquals(
            listOf("vibhakti", "collection", "tune", "mandalam", "element"),
            dikshitar.state.value.facets.map { it.key.wireKey },
        )

        val tyagaraja = presenter()
        tyagaraja.openComposer(CatalogueFixtures.tyagarajaId)
        advanceUntilIdle()
        assertEquals(listOf("collection"), tyagaraja.state.value.facets.map { it.key.wireKey })
    }

    @Test
    fun selectedValueNoLongerOfferedIsDropped() = runTest {
        val presenter = presenter()
        presenter.openComposer(CatalogueFixtures.dikshitarId)
        advanceUntilIdle()
        presenter.onFacetValue(CatalogueFacetKeyDto.VIBHAKTI, "TRITIYA")
        advanceUntilIdle()
        assertEquals("TRITIYA", presenter.state.value.facetSelection[CatalogueFacetKeyDto.VIBHAKTI])
        assertEquals(listOf(CatalogueFixtures.vatapiId), presenter.state.value.associated.map { it.id })

        // Tyagaraja does not offer vibhakti, so the stale selection is cleared rather than sent.
        presenter.openFacetScope(CatalogueFixtures.tyagarajaId, presenter.state.value.facetSelection)
        advanceUntilIdle()
        assertTrue(presenter.state.value.facetSelection.isEmpty)
    }

    @Test
    fun countsAreRequestedOnlyWhenPickerOpens() = runTest {
        val presenter = presenter()
        presenter.openComposer(CatalogueFixtures.dikshitarId)
        advanceUntilIdle()
        assertFalse(countRequests.any { it })
        assertTrue(presenter.state.value.facets.flatMap { it.values }.all { it.count == null })

        presenter.onOpenFacetPicker(CatalogueFacetKeyDto.VIBHAKTI)
        advanceUntilIdle()
        assertEquals(true, countRequests.last())
        assertEquals(CatalogueFacetKeyDto.VIBHAKTI, presenter.state.value.openFacet)
        assertEquals(1L, presenter.state.value.facets.first().values.first().count)
    }

    @Test
    fun chipTapOpensBrowseWithOneFacet() = runTest {
        val presenter = presenter()
        presenter.openFacet(CatalogueFacetKeyDto.VIBHAKTI, "TRITIYA", "Tritiya")
        assertEquals("Tritiya", presenter.state.value.facetTitle)
        advanceUntilIdle()
        val state = presenter.state.value
        assertNull(state.selectedComposerId)
        assertNull(state.selectedRagaId)
        assertEquals(mapOf(CatalogueFacetKeyDto.VIBHAKTI to "TRITIYA"), state.facetSelection.values)
        assertEquals(listOf(CatalogueFixtures.vatapiId), state.associated.map { it.id })
        assertEquals(LoadState.Idle, state.load)
    }

    @Test
    fun clearingTheFacetReturnsTheUnfilteredScope() = runTest {
        val presenter = presenter()
        presenter.openComposer(CatalogueFixtures.dikshitarId)
        advanceUntilIdle()
        presenter.onFacetValue(CatalogueFacetKeyDto.VIBHAKTI, "TRITIYA")
        advanceUntilIdle()
        presenter.onClearFacet(CatalogueFacetKeyDto.VIBHAKTI)
        advanceUntilIdle()
        assertTrue(presenter.state.value.facetSelection.isEmpty)
        assertEquals(1, presenter.state.value.associated.size)
    }

    /** Re-scopes to another composer keeping a selection, as a stale picker state would. */
    private fun BrowsePresenter.openFacetScope(composerId: Uuid, selection: LakshanaFacetSelection) =
        openComposerWith(composerId, selection)

    private fun TestScope.presenter() = BrowsePresenter(
        catalogue = CatalogueRepository(recording),
        session = MobileSession(clockMs = { 0L }),
        scope = this,
    )
}
