package com.sangita.grantha.shared.presentation

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.backhandler.BackHandler
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sangita.grantha.shared.presentation.browse.BrowsePresenter
import com.sangita.grantha.shared.presentation.browse.BrowseScreen
import com.sangita.grantha.shared.presentation.components.RasikaTabBar
import com.sangita.grantha.shared.presentation.di.MobileAppContainer
import com.sangita.grantha.shared.presentation.favourites.FavouritesPresenter
import com.sangita.grantha.shared.presentation.favourites.FavouritesScreen
import com.sangita.grantha.shared.presentation.navigation.RasikaDestination
import com.sangita.grantha.shared.presentation.navigation.RasikaNavigator
import com.sangita.grantha.shared.presentation.preferences.PreferencesPresenter
import com.sangita.grantha.shared.presentation.preferences.PreferencesScreen
import com.sangita.grantha.shared.presentation.reader.KrithiReaderPresenter
import com.sangita.grantha.shared.presentation.reader.KrithiReaderScreen
import com.sangita.grantha.shared.presentation.search.SearchPresenter
import com.sangita.grantha.shared.presentation.search.SearchScreen
import com.sangita.grantha.shared.presentation.theme.RasikaMotion
import com.sangita.grantha.shared.presentation.theme.RasikaTheme

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun RasikaApp(container: MobileAppContainer) {
    val navigator = remember { RasikaNavigator() }
    var current by remember { mutableStateOf(navigator.current) }
    var selectedTab by remember { mutableStateOf(navigator.selectedTab) }
    val prefsPresenter = remember { PreferencesPresenter(container.preferences) }
    val prefs by prefsPresenter.state.collectAsStateWithLifecycle()
    val searchPresenter = remember {
        SearchPresenter(container.catalogue, container.session, container.appScope)
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
        favouritesPresenter.refresh()
    }

    RasikaTheme(appearance = prefs.appearance, textSize = prefs.textSize) {
        BackHandler(enabled = navigator.stackSnapshot().size > 1) {
            navigator.back()
            sync()
        }
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
            val bodyModifier = Modifier.padding(padding)
            AnimatedContent(
                targetState = current,
                modifier = bodyModifier,
                transitionSpec = {
                    fadeIn(
                        tween(RasikaMotion.tabCrossfadeMs, easing = RasikaMotion.emphasizedDecelerate),
                    ) togetherWith fadeOut(
                        tween(RasikaMotion.tabCrossfadeMs, easing = RasikaMotion.emphasizedDecelerate),
                    )
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
                    RasikaDestination.Search -> SearchScreen(
                        presenter = searchPresenter,
                        onOpenKrithi = {
                            navigator.open(RasikaDestination.KrithiReader(it))
                            sync()
                        },
                        onOpenPreferences = {
                            navigator.open(RasikaDestination.Preferences)
                            sync()
                        },
                        isFavourite = favouritesPresenter::isFavourite,
                        onToggleFavourite = { id, label ->
                            favouritesPresenter.toggle(id, label)
                            sync()
                        },
                    )
                    RasikaDestination.Browse,
                    is RasikaDestination.RagaDetail,
                    is RasikaDestination.ComposerDetail,
                    -> BrowseScreen(
                        presenter = browsePresenter,
                        onOpenKrithi = {
                            navigator.open(RasikaDestination.KrithiReader(it))
                            sync()
                        },
                        isFavourite = favouritesPresenter::isFavourite,
                        onToggleFavourite = { id, label ->
                            favouritesPresenter.toggle(id, label)
                            sync()
                        },
                    )
                    RasikaDestination.Favourites -> FavouritesScreen(
                        presenter = favouritesPresenter,
                        onOpenKrithi = {
                            navigator.open(RasikaDestination.KrithiReader(it))
                            sync()
                        },
                        onOpenPreferences = {
                            navigator.open(RasikaDestination.Preferences)
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
                    RasikaDestination.Preferences -> PreferencesScreen(
                        presenter = prefsPresenter,
                        onBack = {
                            navigator.back()
                            sync()
                        },
                    )
                }
            }
        }
    }
}
