package com.sangita.grantha.rasika

import android.app.UiModeManager
import android.content.Context
import android.content.Intent
import android.os.Build
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
 * TRACK-140 U01 / R09 — native appearance matrix that was still open after the
 * first R18 pass. TalkBack itself remains a manual journey.
 */
@RunWith(AndroidJUnit4::class)
class RasikaAppearanceJourneyTest {

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

    private fun awaitText(text: String, timeoutMs: Long = 15_000) {
        compose.waitUntil(timeoutMs) {
            compose.onAllNodes(hasText(text, substring = true))
                .fetchSemanticsNodes().isNotEmpty()
        }
    }

    private fun tab(label: String) = run {
        val matches = compose.onAllNodes(hasContentDescription(label))
        val tops = matches.fetchSemanticsNodes().map { it.boundsInRoot.top }
        check(tops.isNotEmpty()) { "No node labelled '$label'" }
        matches[tops.indexOf(tops.max())]
    }

    private fun submitSearch(term: String) {
        compose.onAllNodes(hasSetTextAction()).onFirst().performTextInput(term)
        compose.onAllNodes(hasText("Search", substring = true))
            .onFirst().performClick()
    }

    @Test
    fun appearanceChoiceAppliesAcrossHomeExploreReaderAndLibrary() {
        launchFresh().use {
            awaitText("Home")
            tab("Settings").performClick()
            compose.waitForIdle()
            awaitText("Appearance")
            compose.onAllNodes(hasText("Light", substring = true)).onFirst().performClick()
            compose.waitForIdle()
            tab("Home").performClick()
            compose.waitForIdle()
            awaitText("Home")
            tab("Explore").performClick()
            compose.waitForIdle()
            submitSearch("Vatapi")
            awaitText("Vatapi")
            compose.onAllNodes(hasText("Vatapi Ganapatim", substring = true) and hasClickAction())
                .onFirst().performClick()
            compose.waitForIdle()
            awaitText("Pallavi")
            tab("Library").performClick()
            compose.waitForIdle()
            awaitText("No favourites yet")
            tab("Settings").performClick()
            compose.waitForIdle()
            awaitText("Appearance")
            awaitText("Selected")
        }
    }

    @Test
    fun systemAppearanceFollowsDeviceNightMode() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val uiMode = context.getSystemService(Context.UI_MODE_SERVICE) as UiModeManager
        try {
            launchFresh().use {
                awaitText("Home")
                tab("Settings").performClick()
                compose.waitForIdle()
                awaitText("Appearance")
                compose.onAllNodes(hasText("System", substring = true)).onFirst().performClick()
                compose.waitForIdle()
                if (Build.VERSION.SDK_INT >= 31) {
                    uiMode.setApplicationNightMode(UiModeManager.MODE_NIGHT_YES)
                    compose.waitForIdle()
                    tab("Home").performClick()
                    compose.waitForIdle()
                    awaitText("Home")
                    uiMode.setApplicationNightMode(UiModeManager.MODE_NIGHT_NO)
                    compose.waitForIdle()
                    awaitText("Home")
                }
            }
        } finally {
            if (Build.VERSION.SDK_INT >= 31) {
                uiMode.setApplicationNightMode(UiModeManager.MODE_NIGHT_AUTO)
            }
        }
    }
}
