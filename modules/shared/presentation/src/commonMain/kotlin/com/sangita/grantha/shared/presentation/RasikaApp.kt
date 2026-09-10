package com.sangita.grantha.shared.presentation

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigationevent.NavigationEventInfo
import androidx.navigationevent.compose.NavigationEventHandler
import androidx.navigationevent.compose.rememberNavigationEventState
import com.sangita.grantha.shared.presentation.browse.BrowsePresenter
import com.sangita.grantha.shared.presentation.browse.BrowseScreen
import com.sangita.grantha.shared.presentation.components.RasikaPrimaryButton
import com.sangita.grantha.shared.presentation.components.RasikaTabBar
import com.sangita.grantha.shared.presentation.di.MobileAppContainer
import com.sangita.grantha.shared.presentation.entities.ComposerDetailScreen
import com.sangita.grantha.shared.presentation.entities.EntityDetailPresenter
import com.sangita.grantha.shared.presentation.entities.RagaDetailScreen
import com.sangita.grantha.shared.presentation.explore.ExploreCategory
import com.sangita.grantha.shared.presentation.favourites.FavouritesPresenter
import com.sangita.grantha.shared.presentation.favourites.FavouritesScreen
import com.sangita.grantha.shared.presentation.home.HomePresenter
import com.sangita.grantha.shared.presentation.home.HomeScreen
import com.sangita.grantha.shared.presentation.navigation.RasikaDestination
import com.sangita.grantha.shared.presentation.navigation.RasikaNavigator
import com.sangita.grantha.shared.presentation.navigation.RasikaTab
import com.sangita.grantha.shared.presentation.preferences.PreferencesPresenter
import com.sangita.grantha.shared.presentation.preferences.PreferencesScreen
import com.sangita.grantha.shared.presentation.reader.KrithiReaderPresenter
import com.sangita.grantha.shared.presentation.reader.KrithiReaderScreen
import com.sangita.grantha.shared.presentation.search.SearchPresenter
import com.sangita.grantha.shared.presentation.search.SearchScreen
import com.sangita.grantha.shared.presentation.theme.RasikaMotion
import com.sangita.grantha.shared.presentation.theme.RasikaTheme
import com.sangita.grantha.shared.presentation.theme.RasikaTokens
import com.sangita.grantha.shared.presentation.theme.rememberReduceMotion

@Composable
fun RasikaApp(container: MobileAppContainer) {
    val navigator = remember { RasikaNavigator() }
    var current by remember { mutableStateOf(navigator.current) }
    var selectedTab by remember { mutableStateOf(navigator.selectedTab) }
    var stackLimitReached by remember { mutableStateOf(false) }
    val prefsPresenter = remember { PreferencesPresenter(container.preferences) }
    val prefs by prefsPresenter.state.collectAsStateWithLifecycle()
    val homePresenter = remember {
        HomePresenter(container.catalogue, container.session, container.appScope)
    }
    val searchPresenter = remember {
        SearchPresenter(container.catalogue, container.session, container.appScope)
    }
    val entityPresenter = remember {
        EntityDetailPresenter(container.catalogue, container.session, container.appScope)
    }
    val browsePresenter = remember {
        BrowsePresenter(container.catalogue, container.session, container.appScope)
    }
    val favouritesPresenter = remember { FavouritesPresenter(container.favourites) }
    val readerPresenter = remember {
        KrithiReaderPresenter(container.catalogue, container.preferences, container.session, container.appScope)
    }

    fun sync() {
        current = navigator.current
        selectedTab = navigator.selectedTab
        stackLimitReached = navigator.stackLimitReached
        favouritesPresenter.refresh()
    }

    fun open(destination: RasikaDestination) {
        navigator.open(destination)
        sync()
    }

    RasikaTheme(appearance = prefs.appearance, textSize = prefs.textSize) {
        val reduceMotion = rememberReduceMotion()
        val backEventState = rememberNavigationEventState(currentInfo = NavigationEventInfo.None)
        NavigationEventHandler(
            state = backEventState,
            isForwardEnabled = false,
            isBackEnabled = navigator.stackSnapshot().size > 1,
            onBackCompleted = {
                navigator.back()
                sync()
            },
        )
        Scaffold(
            containerColor = MaterialTheme.colorScheme.background,
            bottomBar = {
                RasikaTabBar(
                    selected = selectedTab,
                    onSelect = { tab ->
                        navigator.selectTab(tab)
                        sync()
                    },
                )
            },
        ) { padding ->
            Column(Modifier.padding(padding)) {
                if (stackLimitReached) {
                    Column(
                        Modifier
                            .fillMaxWidth()
                            .padding(horizontal = RasikaTokens.screen, vertical = RasikaTokens.sm),
                    ) {
                        Text(RasikaCopy.STACK_LIMIT, style = MaterialTheme.typography.bodyLarge)
                        RasikaPrimaryButton(
                            label = RasikaCopy.RETURN_TO_ROOT,
                            onClick = {
                                navigator.returnToRoot()
                                sync()
                            },
                            modifier = Modifier.padding(top = RasikaTokens.xs),
                        )
                    }
                }
                AnimatedContent(
                    targetState = current,
                    modifier = Modifier.weight(1f),
                    transitionSpec = {
                        if (reduceMotion) {
                            EnterTransition.None togetherWith ExitTransition.None
                        } else {
                            fadeIn(
                                tween(RasikaMotion.tabCrossfadeMs, easing = RasikaMotion.emphasizedDecelerate),
                            ) togetherWith fadeOut(
                                tween(RasikaMotion.tabCrossfadeMs, easing = RasikaMotion.emphasizedDecelerate),
                            )
                        }
                    },
                    contentKey = { dest ->
                        when (dest) {
                            is RasikaDestination.KrithiReader -> "reader:${dest.krithiId}"
                            is RasikaDestination.RagaDetail -> "raga:${dest.ragaId}"
                            is RasikaDestination.ComposerDetail -> "composer:${dest.composerId}"
                            else -> dest::class.simpleName.orEmpty()
                        }
                    },
                    label = "rasikaDestination",
                ) { dest ->
                    when (dest) {
                        RasikaDestination.Home -> HomeScreen(
                            presenter = homePresenter,
                            onSearch = { query ->
                                searchPresenter.applyCommittedQuery(query)
                                navigator.selectTab(RasikaTab.Explore)
                                navigator.returnToRoot()
                                sync()
                            },
                            onOpenKrithi = { open(RasikaDestination.KrithiReader(it)) },
                            onOpenRagas = {
                                searchPresenter.openDirectory(ExploreCategory.Ragas)
                                navigator.selectTab(RasikaTab.Explore)
                                navigator.returnToRoot()
                                sync()
                            },
                            onOpenComposers = {
                                searchPresenter.openDirectory(ExploreCategory.Composers)
                                navigator.selectTab(RasikaTab.Explore)
                                navigator.returnToRoot()
                                sync()
                            },
                        )
                        RasikaDestination.Explore -> SearchScreen(
                            presenter = searchPresenter,
                            onOpenKrithi = { open(RasikaDestination.KrithiReader(it)) },
                            onOpenRaga = { open(RasikaDestination.RagaDetail(it)) },
                            onOpenComposer = { open(RasikaDestination.ComposerDetail(it)) },
                            onOpenPreferences = {
                                navigator.selectTab(RasikaTab.Settings)
                                sync()
                            },
                            isFavourite = favouritesPresenter::isFavourite,
                            onToggleFavourite = { id, label ->
                                favouritesPresenter.toggle(id, label)
                                sync()
                            },
                        )
                        RasikaDestination.Browse -> BrowseScreen(
                            presenter = browsePresenter,
                            onOpenKrithi = { open(RasikaDestination.KrithiReader(it)) },
                            onOpenRaga = { open(RasikaDestination.RagaDetail(it)) },
                            onOpenComposer = { open(RasikaDestination.ComposerDetail(it)) },
                            isFavourite = favouritesPresenter::isFavourite,
                            onToggleFavourite = { id, label ->
                                favouritesPresenter.toggle(id, label)
                                sync()
                            },
                            onBack = {
                                navigator.back()
                                sync()
                            },
                        )
                        is RasikaDestination.RagaDetail -> RagaDetailScreen(
                            ragaId = dest.ragaId,
                            presenter = entityPresenter,
                            onOpenKrithi = { open(RasikaDestination.KrithiReader(it)) },
                            onOpenRelatedRaga = { open(RasikaDestination.RagaDetail(it)) },
                            onBack = {
                                navigator.back()
                                sync()
                            },
                            isFavourite = favouritesPresenter::isFavourite,
                            onToggleFavourite = { id, label ->
                                favouritesPresenter.toggle(id, label)
                                sync()
                            },
                        )
                        is RasikaDestination.ComposerDetail -> ComposerDetailScreen(
                            composerId = dest.composerId,
                            presenter = entityPresenter,
                            onOpenKrithi = { open(RasikaDestination.KrithiReader(it)) },
                            onBack = {
                                navigator.back()
                                sync()
                            },
                            isFavourite = favouritesPresenter::isFavourite,
                            onToggleFavourite = { id, label ->
                                favouritesPresenter.toggle(id, label)
                                sync()
                            },
                        )
                        RasikaDestination.Library -> FavouritesScreen(
                            presenter = favouritesPresenter,
                            onOpenKrithi = { open(RasikaDestination.KrithiReader(it)) },
                            onOpenPreferences = {
                                navigator.selectTab(RasikaTab.Settings)
                                sync()
                            },
                        )
                        is RasikaDestination.KrithiReader -> KrithiReaderScreen(
                            krithiId = dest.krithiId,
                            presenter = readerPresenter,
                            favourited = favouritesPresenter.isFavourite(dest.krithiId),
                            onToggleFavourite = { label ->
                                favouritesPresenter.toggle(dest.krithiId, label)
                                sync()
                            },
                            onBack = {
                                navigator.back()
                                sync()
                            },
                        )
                        RasikaDestination.Settings -> PreferencesScreen(
                            presenter = prefsPresenter,
                        )
                    }
                }
            }
        }
    }
}
