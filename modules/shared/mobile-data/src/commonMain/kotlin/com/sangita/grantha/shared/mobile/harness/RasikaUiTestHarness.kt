package com.sangita.grantha.shared.mobile.harness

import com.sangita.grantha.shared.mobile.fixture.FixtureCatalogueApi
import com.sangita.grantha.shared.mobile.repository.CatalogueRepository
import com.sangita.grantha.shared.mobile.storage.KeyValueStore
import com.sangita.grantha.shared.mobile.storage.LocalSettingsCodec

/**
 * TRACK-140 R18: deterministic native journeys.
 *
 * Journeys must prove navigation, persistence and appearance behaviour, none of which
 * a live catalogue makes reproducible: the corpus changes, the network can fail, and a
 * device carries state from the previous run. Both hosts therefore accept two launch
 * flags that a UI test — and only a UI test — sets.
 *
 * The fixture catalogue is the same [FixtureCatalogueApi] the shared presenter tests use,
 * so a journey and its unit-test counterpart assert against identical records. Fixture
 * records are synthetic contract examples, never live catalogue evidence, and the Spec
 * requires them to be labelled as such wherever they appear in proof.
 *
 * Neither flag has any effect on a normally launched app: the hosts read them from the
 * launch intent (Android) or the process arguments (iOS), which a user cannot set.
 */
object RasikaUiTestHarness {
    /** Serve the catalogue from [FixtureCatalogueApi] instead of the live Ktor client. */
    const val FIXTURES_FLAG: String = "rasika.uiTest.fixtures"

    /** Clear bookmarks and preferences before the first frame, for a fresh-install journey. */
    const val RESET_STATE_FLAG: String = "rasika.uiTest.resetState"

    /** iOS passes flags as process arguments; these are the argument spellings. */
    const val FIXTURES_ARGUMENT: String = "-$FIXTURES_FLAG"
    const val RESET_STATE_ARGUMENT: String = "-$RESET_STATE_FLAG"

    /** A catalogue backed by the shared fixtures, with no network dependency. */
    fun fixtureCatalogue(): CatalogueRepository = CatalogueRepository(FixtureCatalogueApi())

    /**
     * Remove every locally persisted Rasika document. Only the single settings key is
     * written by [LocalSettingsCodec], so this leaves no partial document behind — which
     * matters, because a half-cleared document is exactly the corrupt-input case the
     * store is required to surface rather than silently replace.
     */
    fun resetLocalState(store: KeyValueStore) {
        store.remove(LocalSettingsCodec.SETTINGS_KEY)
    }
}
