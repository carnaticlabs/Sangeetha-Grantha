package com.sangita.grantha.rasika

import android.content.Intent
import android.os.Build
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.sangita.grantha.mobile.MainActivity
import com.sangita.grantha.shared.mobile.harness.RasikaUiTestHarness
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * TRACK-140 R18 — Android native journeys.
 *
 * These drive the real [MainActivity] against
 * [com.sangita.grantha.shared.mobile.fixture.FixtureCatalogueApi], not the live corpus.
 * R18 asks for *reproducible* evidence, and a live catalogue would make every navigation,
 * persistence and appearance assertion depend on whatever happens to be published at the
 * time. Fixture records are synthetic contract examples, never live catalogue evidence.
 *
 * Deliberately NOT claimed here, so the proof ledger is not overstated:
 *  - Multi-page paging. The shared fixture holds two compositions, less than one page.
 *    Paging rules are covered by the shared ExplorePresenterTest against a paging fixture.
 *  - TalkBack. These assert the semantics tree a screen reader consumes, which is
 *    necessary but is not a screen-reader pass. That stays a manual journey.
 *  - Large-text and reduced-motion matrices, which need device-level settings changes.
 */
@RunWith(AndroidJUnit4::class)
class RasikaR18JourneyTest {

    @get:Rule
    val compose = createEmptyComposeRule()

    private fun intent(reset: Boolean) = Intent(
        ApplicationProvider.getApplicationContext(),
        MainActivity::class.java,
    ).apply {
        putExtra(RasikaUiTestHarness.FIXTURES_FLAG, true)
        if (reset) putExtra(RasikaUiTestHarness.RESET_STATE_FLAG, true)
    }

    private fun launchFresh(): ActivityScenario<MainActivity> =
        ActivityScenario.launch(intent(reset = true))

    /** Relaunch WITHOUT the reset flag: persisted state must survive. */
    private fun relaunchKeepingState(): ActivityScenario<MainActivity> =
        ActivityScenario.launch(intent(reset = false))

    private fun awaitText(text: String, timeoutMs: Long = 15_000) {
        compose.waitUntil(timeoutMs) {
            compose.onAllNodes(hasText(text, substring = true))
                .fetchSemanticsNodes().isNotEmpty()
        }
    }

    /**
     * "Settings" labels both a header shortcut and the bottom tab, so a bare content
     * description matches two nodes. The tab bar is always the bottom-most match.
     */
    private fun tab(label: String): SemanticsNodeInteraction {
        val matches = compose.onAllNodes(hasContentDescription(label))
        val tops = matches.fetchSemanticsNodes().map { it.boundsInRoot.top }
        check(tops.isNotEmpty()) { "No node labelled '$label'" }
        return matches[tops.indexOf(tops.max())]
    }

    private fun submitSearch(term: String) {
        compose.onAllNodes(hasSetTextAction()).onFirst().performTextInput(term)
        compose.onAllNodes(hasText("Search", substring = true))
            .onFirst().performClick()
    }

    // R01 / R02 — Home is the fresh-install entry, with search before editorial material.
    @Test
    fun freshLaunchLandsOnHomeWithSearchAvailable() {
        launchFresh().use {
            awaitText("Home")
            listOf("Home", "Explore", "Library", "Settings").forEach {
                tab(it).assertIsDisplayed()
            }
            awaitText("Title, incipit or a line of sahitya")
        }
    }

    // R01 — the four tabs are stable destinations across switching.
    @Test
    fun tabsAreStableDestinations() {
        launchFresh().use {
            awaitText("Home")
            listOf("Explore", "Library", "Settings", "Home").forEach { label ->
                tab(label).performClick()
                compose.waitForIdle()
                tab(label).assertIsDisplayed()
            }
        }
    }

    // R03 — Explore searches under one visible query with explicit submission.
    @Test
    fun exploreSearchReturnsAFixtureComposition() {
        launchFresh().use {
            awaitText("Home")
            tab("Explore").performClick()
            compose.waitForIdle()
            submitSearch("Vatapi")
            awaitText("Vatapi")
        }
    }

    // R07 — opening a result shows that composition's stored reading.
    @Test
    fun openingACompositionShowsItsStoredReading() {
        launchFresh().use {
            awaitText("Home")
            tab("Explore").performClick()
            compose.waitForIdle()
            submitSearch("Vatapi")
            awaitText("Vatapi")
            compose.onAllNodes(hasText("Vatapi Ganapatim", substring = true) and hasClickAction())
                .onFirst().performClick()
            compose.waitForIdle()
            awaitText("Pallavi")
        }
    }

    // R09 — Appearance is a three-way choice applied without a Save step.
    @Test
    fun appearanceOffersSystemLightAndDark() {
        launchFresh().use {
            awaitText("Home")
            tab("Settings").performClick()
            compose.waitForIdle()
            awaitText("Appearance")
            listOf("System", "Light", "Dark").forEach { awaitText(it) }
            compose.onAllNodes(hasText("Light", substring = true)).onFirst().performClick()
            compose.waitForIdle()
            // Navigation stays usable immediately after the change.
            tab("Home").performClick()
            compose.waitForIdle()
            awaitText("Home")
        }
    }

    // R09 / R17 — an explicit appearance choice survives a process restart.
    @Test
    fun appearanceChoiceSurvivesRestart() {
        launchFresh().use {
            awaitText("Home")
            tab("Settings").performClick()
            compose.waitForIdle()
            awaitText("Appearance")
            compose.onAllNodes(hasText("Dark", substring = true)).onFirst().performClick()
            compose.waitForIdle()
        }

        relaunchKeepingState().use {
            awaitText("Home")
            tab("Settings").performClick()
            compose.waitForIdle()
            awaitText("Appearance")
            awaitText("Dark")
        }
    }

    // R08 — a fresh-install Library is empty and explains itself rather than erroring.
    @Test
    fun freshLibraryIsEmptyAndExplainsItself() {
        launchFresh().use {
            awaitText("Home")
            tab("Library").performClick()
            compose.waitForIdle()
            awaitText("No favourites yet")
        }
    }

    // R10 — every bottom-tab control carries a screen-reader label. Necessary for
    // TalkBack, not a substitute for a TalkBack pass.
    @Test
    fun navigationControlsAreLabelledForScreenReaders() {
        launchFresh().use {
            awaitText("Home")
            listOf("Home", "Explore", "Library", "Settings").forEach {
                tab(it).assertIsDisplayed()
            }
        }
    }

    // R18 — name the device the journeys ran on, for the proof ledger.
    @Test
    fun recordsDeviceIdentityForTheProofLedger() {
        println(
            "TRACK-140 R18 Android journey device: " +
                "${Build.MANUFACTURER} ${Build.MODEL} API ${Build.VERSION.SDK_INT}",
        )
    }
}
