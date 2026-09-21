package com.sangita.grantha.rasika

import android.content.Intent
import android.view.KeyEvent
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.lifecycle.Lifecycle
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.sangita.grantha.mobile.MainActivity
import com.sangita.grantha.shared.mobile.harness.RasikaUiTestHarness
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * TRACK-140 R01 — Android **system** Back must pop the in-app stack, matching the
 * in-app header chevron. Connected journeys that only tap `content-desc=Back`
 * previously missed KEYCODE_BACK finishing [MainActivity].
 */
@RunWith(AndroidJUnit4::class)
class RasikaSystemBackJourneyTest {

    @get:Rule
    val compose = createEmptyComposeRule()

    private fun intent() = Intent(
        ApplicationProvider.getApplicationContext(),
        MainActivity::class.java,
    ).apply {
        putExtra(RasikaUiTestHarness.FIXTURES_FLAG, true)
        putExtra(RasikaUiTestHarness.RESET_STATE_FLAG, true)
    }

    private fun launchFresh(): ActivityScenario<MainActivity> =
        ActivityScenario.launch(intent())

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

    private fun pressSystemBack() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        instrumentation.waitForIdleSync()
        instrumentation.sendKeyDownUpSync(KeyEvent.KEYCODE_BACK)
        instrumentation.waitForIdleSync()
        compose.waitForIdle()
    }

    private fun assertActivityStillResumed(scenario: ActivityScenario<MainActivity>) {
        assertEquals(Lifecycle.State.RESUMED, scenario.state)
        scenario.onActivity { activity ->
            assertFalse(activity.isFinishing)
            assertFalse(activity.isDestroyed)
        }
    }

    private fun awaitReaderClosed() {
        compose.waitUntil(15_000) {
            compose.onAllNodes(hasContentDescription("Back"))
                .fetchSemanticsNodes().isEmpty()
        }
    }

    @Test
    fun systemBackFromHomeReaderReturnsToHomeWithoutFinishing() {
        launchFresh().use { scenario ->
            awaitText("Home")
            awaitText("Read composition")
            compose.onAllNodes(hasText("Read composition") and hasClickAction())
                .onFirst().performClick()
            compose.waitForIdle()
            awaitText("Pallavi")

            pressSystemBack()

            assertActivityStillResumed(scenario)
            awaitReaderClosed()
            awaitText("Read composition")
            awaitText("Home")
        }
    }

    @Test
    fun systemBackFromExploreReaderReturnsToExploreWithoutFinishing() {
        launchFresh().use { scenario ->
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

            pressSystemBack()

            assertActivityStillResumed(scenario)
            awaitReaderClosed()
            awaitText("Vatapi Ganapatim")
            tab("Explore").assertIsDisplayed()
        }
    }

    @Test
    fun systemBackFromRagaDetailReturnsToExploreWithoutFinishing() {
        launchFresh().use { scenario ->
            awaitText("Home")
            tab("Explore").performClick()
            compose.waitForIdle()
            compose.onAllNodes(hasText("Ragas") and hasClickAction()).onFirst().performClick()
            compose.waitForIdle()
            compose.onAllNodes(hasText("Hamsadhvani", substring = true) and hasClickAction())
                .onFirst().performClick()
            compose.waitForIdle()
            awaitText("Hamsadhvani")
            compose.waitUntil(15_000) {
                compose.onAllNodes(hasContentDescription("Back"))
                    .fetchSemanticsNodes().isNotEmpty()
            }

            pressSystemBack()

            assertActivityStillResumed(scenario)
            awaitReaderClosed()
            awaitText("Hamsadhvani")
            tab("Explore").assertIsDisplayed()
        }
    }
}
