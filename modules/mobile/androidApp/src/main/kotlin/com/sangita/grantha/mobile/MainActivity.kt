package com.sangita.grantha.mobile

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.sangita.grantha.shared.mobile.harness.RasikaUiTestHarness
import com.sangita.grantha.shared.mobile.platform.AndroidKeyValueStore
import com.sangita.grantha.shared.mobile.platform.androidLiveCatalogue
import com.sangita.grantha.shared.mobile.repository.FavouritesRepository
import com.sangita.grantha.shared.mobile.repository.PreferencesRepository
import com.sangita.grantha.shared.mobile.storage.CodecBackedBookmarkStore
import com.sangita.grantha.shared.mobile.storage.CodecBackedPreferencesStore
import com.sangita.grantha.shared.mobile.usage.MobileSession
import com.sangita.grantha.shared.presentation.RasikaApp
import com.sangita.grantha.shared.presentation.di.MobileAppContainer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel

class MainActivity : ComponentActivity() {
    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val kv = AndroidKeyValueStore(applicationContext)

        // TRACK-140 R18. Only an instrumentation launch can put these extras on the
        // intent, so a normally launched app always takes the live client below.
        val useFixtures = intent?.getBooleanExtra(RasikaUiTestHarness.FIXTURES_FLAG, false) == true
        if (intent?.getBooleanExtra(RasikaUiTestHarness.RESET_STATE_FLAG, false) == true) {
            RasikaUiTestHarness.resetLocalState(kv)
        }

        val container = MobileAppContainer(
            // Live Ktor client. For offline UI review, inject CatalogueRepository(FixtureCatalogueApi()).
            catalogue = if (useFixtures) RasikaUiTestHarness.fixtureCatalogue() else androidLiveCatalogue(),
            favourites = FavouritesRepository(CodecBackedBookmarkStore(kv)),
            preferences = PreferencesRepository(CodecBackedPreferencesStore(kv)),
            session = MobileSession(),
            appScope = appScope,
        )
        setContent { RasikaApp(container) }
    }

    override fun onDestroy() {
        super.onDestroy()
        appScope.cancel()
    }
}
