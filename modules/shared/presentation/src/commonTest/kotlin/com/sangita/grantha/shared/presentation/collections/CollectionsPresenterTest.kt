package com.sangita.grantha.shared.presentation.collections

import com.sangita.grantha.shared.domain.model.catalogue.CatalogueCollectionRefDto
import com.sangita.grantha.shared.domain.model.catalogue.CatalogueCollectionRoleDto
import com.sangita.grantha.shared.domain.model.catalogue.CatalogueOrderBasisDto
import com.sangita.grantha.shared.mobile.fixture.CollectionFixtures
import com.sangita.grantha.shared.mobile.fixture.FixtureCatalogueApi
import com.sangita.grantha.shared.mobile.network.CatalogueApi
import com.sangita.grantha.shared.mobile.network.CatalogueFailure
import com.sangita.grantha.shared.mobile.repository.CatalogueRepository
import com.sangita.grantha.shared.mobile.usage.InteractionContext
import com.sangita.grantha.shared.mobile.usage.MobileSession
import com.sangita.grantha.shared.presentation.components.LoadState
import com.sangita.grantha.shared.presentation.navigation.RasikaDestination
import com.sangita.grantha.shared.presentation.navigation.RasikaNavigator
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull

@OptIn(ExperimentalCoroutinesApi::class)
class CollectionsPresenterTest {
    private fun repo(api: CatalogueApi = FixtureCatalogueApi()) = CatalogueRepository(api)
    private val session get() = MobileSession(clockMs = { 0L })

    private fun ref(
        basis: CatalogueOrderBasisDto,
        position: Int? = 6,
        role: CatalogueCollectionRoleDto = CatalogueCollectionRoleDto.CORE,
    ) = CatalogueCollectionRefDto(
        collectionId = CollectionFixtures.list.items.first().id,
        slug = "x",
        name = "Kamalamba Navavarana",
        role = role,
        sequenceOrder = 6,
        position = position,
        coreCount = 9,
        orderBasis = basis,
    )

    @Test
    fun listLoadsOnceAndGroupsByComposer() = runTest {
        val presenter = CollectionsPresenter(repo(), session, this)
        presenter.loadIfNeeded()
        advanceUntilIdle()
        assertEquals(LoadState.Idle, presenter.state.value.load)
        assertEquals(3, presenter.state.value.items.size)
        presenter.loadIfNeeded()
        advanceUntilIdle()
        assertEquals(3, presenter.state.value.items.size)
    }

    @Test
    fun emptyListIsEmptyNotError() = runTest {
        val empty = object : CatalogueApi by FixtureCatalogueApi() {
            override suspend fun listCollections(interaction: InteractionContext) =
                com.sangita.grantha.shared.domain.model.catalogue.CatalogueCollectionListDto(emptyList())
        }
        val presenter = CollectionsPresenter(repo(empty), session, this)
        presenter.loadIfNeeded()
        advanceUntilIdle()
        assertEquals(LoadState.Empty, presenter.state.value.load)
    }

    @Test
    fun failedListIsRetryableAndRecovers() = runTest {
        var fail = true
        val flaky = object : CatalogueApi by FixtureCatalogueApi() {
            override suspend fun listCollections(interaction: InteractionContext) =
                if (fail) throw CatalogueFailure.Unavailable() else CollectionFixtures.list
        }
        val presenter = CollectionsPresenter(repo(flaky), session, this)
        presenter.loadIfNeeded()
        advanceUntilIdle()
        val error = assertIs<LoadState.Error>(presenter.state.value.load)
        assertEquals(true, error.retryable)
        fail = false
        presenter.retry()
        advanceUntilIdle()
        assertEquals(3, presenter.state.value.items.size)
    }

    @Test
    fun detailOpensBySlugAndUnknownSlugIsNotRetryable() = runTest {
        val presenter = CollectionDetailPresenter(repo(), session, this)
        presenter.open(CollectionFixtures.navavaranaSlug)
        advanceUntilIdle()
        assertEquals(11, presenter.state.value.detail?.members?.size)
        presenter.open("missing")
        advanceUntilIdle()
        val error = assertIs<LoadState.Error>(presenter.state.value.load)
        assertEquals(false, error.retryable)
        assertNull(presenter.state.value.detail)
    }

    @Test
    fun ordinalsAppearOnlyWhenTheCycleAttestsPosition() {
        assertEquals("6 of 9", CollectionCopy.positionLabel(ref(CatalogueOrderBasisDto.SOURCE)))
        assertNull(CollectionCopy.positionLabel(ref(CatalogueOrderBasisDto.PERFORMANCE_CONVENTION)))
        assertNull(CollectionCopy.positionLabel(ref(CatalogueOrderBasisDto.UNSPECIFIED)))
        assertNull(CollectionCopy.positionLabel(ref(CatalogueOrderBasisDto.SOURCE, position = null)))
    }

    @Test
    fun roleAndOrderWordingIsExplicit() {
        assertEquals("Attribution disputed", CollectionCopy.roleLabel(CatalogueCollectionRoleDto.DISPUTED_CONJECTURE))
        assertNull(CollectionCopy.roleLabel(CatalogueCollectionRoleDto.CORE))
        assertEquals(
            "Traditional performance order · Aradhana",
            CollectionCopy.orderNote(CatalogueOrderBasisDto.PERFORMANCE_CONVENTION, "Aradhana"),
        )
        assertNull(CollectionCopy.orderNote(CatalogueOrderBasisDto.SOURCE, null))
        assertEquals("Sambodhana · vocative", CollectionCopy.vibhakti("SAMBODHANA"))
    }

    @Test
    fun replaceSwapsTopWithoutGrowingTheStack() {
        val nav = RasikaNavigator()
        val first = RasikaDestination.KrithiReader(CollectionFixtures.navavaranaSixthId)
        nav.open(first)
        val before = nav.stackSnapshot().size
        repeat(20) { nav.replace(RasikaDestination.CollectionDetail("s$it")) }
        assertEquals(before, nav.stackSnapshot().size)
        assertEquals(false, nav.stackLimitReached)
        // On a tab root, replace pushes rather than discarding the root.
        val root = RasikaNavigator()
        root.replace(first)
        assertEquals(2, root.stackSnapshot().size)
    }
}
