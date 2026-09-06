package com.sangita.grantha.shared.presentation

import androidx.compose.ui.window.ComposeUIViewController
import com.sangita.grantha.shared.mobile.platform.IosKeyValueStore
import com.sangita.grantha.shared.mobile.platform.iosLiveCatalogue
import com.sangita.grantha.shared.mobile.repository.FavouritesRepository
import com.sangita.grantha.shared.mobile.repository.PreferencesRepository
import com.sangita.grantha.shared.mobile.storage.CodecBackedBookmarkStore
import com.sangita.grantha.shared.mobile.storage.CodecBackedPreferencesStore
import com.sangita.grantha.shared.mobile.usage.MobileSession
import com.sangita.grantha.shared.presentation.di.MobileAppContainer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import platform.UIKit.UIViewController

fun MainViewController(): UIViewController {
    val kv = IosKeyValueStore()
    val container = MobileAppContainer(
        // Live Ktor client. For offline UI review, inject CatalogueRepository(FixtureCatalogueApi()).
        catalogue = iosLiveCatalogue(),
        favourites = FavouritesRepository(CodecBackedBookmarkStore(kv)),
        preferences = PreferencesRepository(CodecBackedPreferencesStore(kv)),
        session = MobileSession(),
        appScope = CoroutineScope(SupervisorJob() + Dispatchers.Main),
    )
    return ComposeUIViewController { RasikaApp(container) }
}
