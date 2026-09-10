package com.sangita.grantha.shared.presentation

import androidx.compose.ui.window.ComposeUIViewController
import com.sangita.grantha.shared.mobile.harness.RasikaUiTestHarness
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
import platform.Foundation.NSProcessInfo
import platform.UIKit.UIViewController

/**
 * TRACK-140 R18. Read from the process arguments rather than a Kotlin parameter so the
 * Swift entry point keeps its no-argument selector. XCUIApplication sets these; a user
 * launching the app normally cannot, so the live client below stays the default.
 */
private fun launchArguments(): List<String> =
    NSProcessInfo.processInfo.arguments.mapNotNull { it as? String }

fun MainViewController(): UIViewController {
    val arguments = launchArguments()
    val useFixtures = RasikaUiTestHarness.FIXTURES_ARGUMENT in arguments
    val kv = IosKeyValueStore()
    if (RasikaUiTestHarness.RESET_STATE_ARGUMENT in arguments) {
        RasikaUiTestHarness.resetLocalState(kv)
    }
    val container = MobileAppContainer(
        // Live Ktor client. For offline UI review, inject CatalogueRepository(FixtureCatalogueApi()).
        catalogue = if (useFixtures) RasikaUiTestHarness.fixtureCatalogue() else iosLiveCatalogue(),
        favourites = FavouritesRepository(CodecBackedBookmarkStore(kv)),
        preferences = PreferencesRepository(CodecBackedPreferencesStore(kv)),
        session = MobileSession(),
        appScope = CoroutineScope(SupervisorJob() + Dispatchers.Main),
    )
    return ComposeUIViewController { RasikaApp(container) }
}
