package com.sangita.grantha.shared.presentation.kshetra

import com.sangita.grantha.shared.domain.model.catalogue.CatalogueMandalamDto
import com.sangita.grantha.shared.domain.model.catalogue.CataloguePlaceKindDto
import com.sangita.grantha.shared.domain.model.catalogue.CatalogueTempleElementDto
import com.sangita.grantha.shared.domain.model.catalogue.CatalogueTempleListDto
import com.sangita.grantha.shared.domain.model.catalogue.CatalogueTempleSummaryDto
import com.sangita.grantha.shared.mobile.fixture.FixtureCatalogueApi
import com.sangita.grantha.shared.mobile.fixture.KshetraFixtures
import com.sangita.grantha.shared.mobile.network.CatalogueApi
import com.sangita.grantha.shared.mobile.network.CatalogueFailure
import com.sangita.grantha.shared.mobile.repository.CatalogueRepository
import com.sangita.grantha.shared.mobile.usage.InteractionContext
import com.sangita.grantha.shared.mobile.usage.MobileSession
import com.sangita.grantha.shared.presentation.components.LoadState
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.uuid.Uuid

@OptIn(ExperimentalCoroutinesApi::class)
class KshetraPresentersTest {
    private fun repo(api: CatalogueApi = FixtureCatalogueApi()) = CatalogueRepository(api)
    private val session get() = MobileSession(clockMs = { 0L })

    private fun summary(name: String, mandalam: CatalogueMandalamDto?, element: CatalogueTempleElementDto? = null) =
        CatalogueTempleSummaryDto(
            id = Uuid.random(),
            name = name,
            placeKind = CataloguePlaceKindDto.LOCALITY,
            mandalam = mandalam,
            element = element,
        )

    @Test
    fun groupsFollowMandalamOrderWithUnassignedLastAndNoEmptyGroups() {
        val groups = groupKshetras(
            listOf(
                summary("Z", null),
                summary("B", CatalogueMandalamDto.PANDYA),
                summary("A", CatalogueMandalamDto.CHOLA),
                summary("C", CatalogueMandalamDto.CHOLA),
            ),
        )
        assertEquals(listOf(CatalogueMandalamDto.CHOLA, CatalogueMandalamDto.PANDYA, null), groups.map { it.mandalam })
        assertEquals(listOf("A", "C"), groups.first().items.map { it.name })
        assertTrue(groups.none { it.items.isEmpty() })
    }

    @Test
    fun elementFilterKeepsOnlyMatchingTemplesAndDropsEmptiedGroups() {
        val items = KshetraFixtures.list.items
        val groups = groupKshetras(items, CatalogueTempleElementDto.AGNI)
        assertEquals(listOf(CatalogueMandalamDto.TONDAI), groups.map { it.mandalam })
        assertEquals(listOf("Arunachaleswarar Temple, Tiruvannamalai"), groups.single().items.map { it.name })
    }

    @Test
    fun listLoadsOnceAndGroupsRootsOnly() = runTest {
        var calls = 0
        val api = object : CatalogueApi by FixtureCatalogueApi() {
            override suspend fun listTemples(interaction: InteractionContext): CatalogueTempleListDto {
                calls++
                return KshetraFixtures.list
            }
        }
        val presenter = KshetraListPresenter(repo(api), session, this)
        presenter.loadIfNeeded()
        advanceUntilIdle()
        presenter.loadIfNeeded()
        advanceUntilIdle()
        assertEquals(1, calls)
        val state = presenter.state.value
        assertEquals(LoadState.Idle, state.load)
        assertEquals(
            listOf(CatalogueMandalamDto.CHOLA, CatalogueMandalamDto.PANDYA, CatalogueMandalamDto.TONDAI, null),
            state.groups.map { it.mandalam },
        )
        // Child shrines are nested under the parent, never repeated in the flat list.
        assertTrue(state.items.none { it.id == KshetraFixtures.kamalambaShrineId })
        assertEquals(3, state.items.first { it.id == KshetraFixtures.tiruvarurId }.childCount)
    }

    @Test
    fun elementChipsOfferOnlyElementsPresentAndToggle() = runTest {
        val presenter = KshetraListPresenter(repo(), session, this)
        presenter.loadIfNeeded()
        advanceUntilIdle()
        assertEquals(
            listOf(
                CatalogueTempleElementDto.AKASHA,
                CatalogueTempleElementDto.AGNI,
                CatalogueTempleElementDto.PRITHVI,
            ),
            presenter.state.value.availableElements,
        )
        presenter.selectElement(CatalogueTempleElementDto.AKASHA)
        assertEquals(1, presenter.state.value.groups.sumOf { it.items.size })
        presenter.selectElement(CatalogueTempleElementDto.AKASHA)
        assertNull(presenter.state.value.element)
        assertEquals(5, presenter.state.value.groups.sumOf { it.items.size })
    }

    @Test
    fun anElementFilterSurvivesRefreshOnlyIfStillPresent() = runTest {
        val presenter = KshetraListPresenter(repo(), session, this)
        presenter.loadIfNeeded()
        advanceUntilIdle()
        presenter.selectElement(CatalogueTempleElementDto.VAYU)
        assertTrue(presenter.state.value.filterMatchesNothing)
        presenter.retry()
        advanceUntilIdle()
        assertNull(presenter.state.value.element)
        assertFalse(presenter.state.value.filterMatchesNothing)
    }

    @Test
    fun emptyListIsEmptyNotError() = runTest {
        val empty = object : CatalogueApi by FixtureCatalogueApi() {
            override suspend fun listTemples(interaction: InteractionContext) = CatalogueTempleListDto(emptyList())
        }
        val presenter = KshetraListPresenter(repo(empty), session, this)
        presenter.loadIfNeeded()
        advanceUntilIdle()
        assertEquals(LoadState.Empty, presenter.state.value.load)
    }

    @Test
    fun listFailureIsRetryable() = runTest {
        val presenter = KshetraListPresenter(CatalogueRepository(com.sangita.grantha.shared.mobile.fixture.UnavailableCatalogueApi()), session, this)
        presenter.loadIfNeeded()
        advanceUntilIdle()
        val load = assertIs<LoadState.Error>(presenter.state.value.load)
        assertTrue(load.retryable)
    }

    @Test
    fun tiruvarurDetailNestsChildrenAndKritis() = runTest {
        val presenter = KshetraDetailPresenter(repo(), session, this)
        presenter.open(KshetraFixtures.tiruvarurId)
        advanceUntilIdle()
        val detail = presenter.state.value.detail!!
        assertEquals(3, detail.children.size)
        assertEquals(1, detail.krithis.size)
        assertFalse(detail.recordedGap)
        assertEquals(LoadState.Idle, presenter.state.value.load)
    }

    @Test
    fun gapDetailIsLoadedNotAnError() = runTest {
        val presenter = KshetraDetailPresenter(repo(), session, this)
        presenter.open(KshetraFixtures.kanyakumariId)
        advanceUntilIdle()
        val detail = presenter.state.value.detail!!
        assertTrue(detail.recordedGap)
        assertTrue(detail.krithis.isEmpty())
        assertEquals(LoadState.Idle, presenter.state.value.load)
    }

    @Test
    fun unknownTempleIsNotFoundAndNotRetryable() = runTest {
        val presenter = KshetraDetailPresenter(repo(), session, this)
        presenter.open(KshetraFixtures.unknownId)
        advanceUntilIdle()
        val load = assertIs<LoadState.Error>(presenter.state.value.load)
        assertFalse(load.retryable)
    }

    @Test
    fun staleDetailResponseIsDiscarded() = runTest {
        val presenter = KshetraDetailPresenter(repo(), session, this)
        presenter.open(KshetraFixtures.tiruvarurId)
        presenter.open(KshetraFixtures.chidambaramId)
        advanceUntilIdle()
        assertEquals(KshetraFixtures.chidambaramId, presenter.state.value.detail?.id)
    }

    @Test
    fun templeChipIsALinkOnlyWhenTheDetailIsServed() = runTest {
        val presenter = TempleLinkPresenter(repo(), session, this)
        presenter.resolve(KshetraFixtures.tiruvarurId)
        assertFalse(presenter.state.value.linkable)
        advanceUntilIdle()
        assertTrue(presenter.state.value.linkable)

        presenter.resolve(KshetraFixtures.unknownId)
        advanceUntilIdle()
        assertFalse(presenter.state.value.linkable)

        presenter.resolve(null)
        assertNull(presenter.state.value.templeId)
    }

    @Test
    fun templeChipStaysAPlainLabelWhenTheCheckFails() = runTest {
        val presenter = TempleLinkPresenter(
            CatalogueRepository(com.sangita.grantha.shared.mobile.fixture.UnavailableCatalogueApi()),
            session,
            this,
        )
        presenter.resolve(KshetraFixtures.tiruvarurId)
        advanceUntilIdle()
        assertFalse(presenter.state.value.linkable)
    }

    @Test
    fun missingTempleSurfacesAsCatalogueNotFound() = runTest {
        val failure = runCatching { repo().getTemple(KshetraFixtures.unknownId, session.beginInteraction()) }
            .exceptionOrNull()
        assertIs<CatalogueFailure.NotFound>(failure)
    }
}
