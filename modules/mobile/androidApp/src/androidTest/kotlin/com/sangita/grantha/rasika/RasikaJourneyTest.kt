package com.sangita.grantha.rasika

import android.content.Intent
import androidx.compose.ui.test.SemanticsNodeInteraction
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
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * TRACK-138 MVP journeys named in the Plan: search → reader → favourite → restart,
 * plus a connection-failure pass that must not drop local bookmarks.
 *
 * These drive [MainActivity] against fixtures (or the unavailable catalogue), not the
 * live corpus. Fixture records are synthetic contract examples. TRACK-140 R18 keeps
 * the broader native suite.
 */
@RunWith(AndroidJUnit4::class)
class RasikaJourneyTest {

    @get:Rule
    val compose = createEmptyComposeRule()

    private fun intent(
        reset: Boolean,
        fixtures: Boolean = true,
        offline: Boolean = false,
    ) = Intent(
        ApplicationProvider.getApplicationContext(),
        MainActivity::class.java,
    ).apply {
        if (fixtures) putExtra(RasikaUiTestHarness.FIXTURES_FLAG, true)
        if (offline) putExtra(RasikaUiTestHarness.OFFLINE_FLAG, true)
        if (reset) putExtra(RasikaUiTestHarness.RESET_STATE_FLAG, true)
    }

    private fun launch(
        reset: Boolean,
        fixtures: Boolean = true,
        offline: Boolean = false,
    ): ActivityScenario<MainActivity> =
        ActivityScenario.launch(intent(reset, fixtures, offline))

    private fun awaitText(text: String, timeoutMs: Long = 15_000) {
        compose.waitUntil(timeoutMs) {
            compose.onAllNodes(hasText(text, substring = true))
                .fetchSemanticsNodes().isNotEmpty()
        }
    }

    private fun awaitDescription(label: String, timeoutMs: Long = 15_000) {
        compose.waitUntil(timeoutMs) {
            compose.onAllNodes(hasContentDescription(label))
                .fetchSemanticsNodes().isNotEmpty()
        }
    }

    private fun tab(label: String): SemanticsNodeInteraction {
        val matches = compose.onAllNodes(hasContentDescription(label))
        val tops = matches.fetchSemanticsNodes().map { it.boundsInRoot.top }
        check(tops.isNotEmpty()) { "No node labelled '$label'" }
        return matches[tops.indexOf(tops.max())]
    }

    private fun openVatapiReader() {
        awaitText("Home")
        tab("Explore").performClick()
        compose.waitForIdle()
        compose.onAllNodes(hasSetTextAction()).onFirst().performTextInput("Vatapi")
        compose.onAllNodes(hasText("Search", substring = true)).onFirst().performClick()
        awaitText("Vatapi")
        compose.onAllNodes(hasText("Vatapi Ganapatim", substring = true) and hasClickAction())
            .onFirst().performClick()
        compose.waitForIdle()
        awaitText("Pallavi")
    }

    @Test
    fun launchesMainActivity() {
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                assertEquals("com.sangita.grantha.rasika", activity.packageName)
                assertFalse(activity.isFinishing)
            }
        }
    }

    @Test
    fun searchReaderFavouriteSurvivesRestart() {
        launch(reset = true).use {
            openVatapiReader()
            awaitDescription("Save favourite")
            compose.onAllNodes(hasContentDescription("Save favourite")).onFirst().performClick()
            compose.waitForIdle()
            tab("Library").performClick()
            compose.waitForIdle()
            awaitText("Vatapi Ganapatim")
        }

        launch(reset = false).use {
            awaitText("Home")
            tab("Library").performClick()
            compose.waitForIdle()
            awaitText("Vatapi Ganapatim")
        }
    }

    @Test
    fun connectionFailureKeepsFavouritesAndShowsRetry() {
        launch(reset = true).use {
            openVatapiReader()
            awaitDescription("Save favourite")
            compose.onAllNodes(hasContentDescription("Save favourite")).onFirst().performClick()
            compose.waitForIdle()
        }

        launch(reset = false, fixtures = false, offline = true).use {
            awaitText("Home")
            tab("Explore").performClick()
            compose.waitForIdle()
            compose.onAllNodes(hasText("Search", substring = true)).onFirst().performClick()
            awaitText("Could not reach the catalogue")
            awaitText("Your favourites are still here")
            tab("Library").performClick()
            compose.waitForIdle()
            awaitText("Vatapi Ganapatim")
        }
    }
}
