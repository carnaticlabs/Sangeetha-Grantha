package com.sangita.grantha.rasika

import android.os.Build
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTextInput
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import android.content.Intent
import com.sangita.grantha.mobile.MainActivity
import com.sangita.grantha.shared.mobile.fixture.CatalogueFixtures
import com.sangita.grantha.shared.mobile.harness.RasikaUiTestHarness
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.FileInputStream

/**
 * TRACK-140 R18 journeys that were listed as residual after the first native pass:
 * multi-page Explore, bookmark toggle, script/source switching, large text and
 * reduced-motion. TalkBack itself remains a manual journey; these tests assert the
 * labelled semantics tree and run under device font-scale / animator-scale settings.
 */
@RunWith(AndroidJUnit4::class)
class RasikaR18ExtendedJourneyTest {

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

    private fun relaunchKeepingState(): ActivityScenario<MainActivity> =
        ActivityScenario.launch(intent(reset = false))

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

    private fun tab(label: String) = run {
        val matches = compose.onAllNodes(hasContentDescription(label))
        val tops = matches.fetchSemanticsNodes().map { it.boundsInRoot.top }
        check(tops.isNotEmpty()) { "No node labelled '$label'" }
        matches[tops.indexOf(tops.max())]
    }

    private fun openExplore() {
        awaitText("Home")
        tab("Explore").performClick()
        compose.waitForIdle()
    }

    private fun submitSearch(term: String) {
        compose.onAllNodes(hasSetTextAction()).onFirst().performTextInput(term)
        compose.onAllNodes(hasText("Search", substring = true)).onFirst().performClick()
    }

    private fun openVatapiReader() {
        openExplore()
        submitSearch("Vatapi")
        awaitText("Vatapi")
        compose.onAllNodes(hasText("Vatapi Ganapatim", substring = true) and hasClickAction())
            .onFirst().performClick()
        compose.waitForIdle()
        awaitText("Pallavi")
    }

    private fun adbShell(command: String) {
        val pfd = InstrumentationRegistry.getInstrumentation().uiAutomation.executeShellCommand(command)
        FileInputStream(pfd.fileDescriptor).use { it.readBytes() }
        pfd.close()
    }

    @Test
    fun exploreEmptySearchLoadsTheNextPage() {
        launchFresh().use {
            openExplore()
            compose.onAllNodes(hasText("Search", substring = true)).onFirst().performClick()
            awaitText("in this library")
            compose.onNode(hasScrollAction()).performScrollToNode(hasText("Load more"))
            compose.onAllNodes(hasText("Load more")).onFirst().performClick()
            compose.waitForIdle()
            compose.onNode(hasScrollAction())
                .performScrollToNode(hasText(CatalogueFixtures.pagingLastTitle, substring = true))
            awaitText(CatalogueFixtures.pagingLastTitle)
        }
    }

    @Test
    fun bookmarkToggleAppearsInLibraryAndSurvivesRestart() {
        launchFresh().use {
            openVatapiReader()
            awaitDescription("Save favourite")
            compose.onAllNodes(hasContentDescription("Save favourite")).onFirst().performClick()
            compose.waitForIdle()
            tab("Library").performClick()
            compose.waitForIdle()
            awaitText("Vatapi Ganapatim")
        }

        relaunchKeepingState().use {
            awaitText("Home")
            tab("Library").performClick()
            compose.waitForIdle()
            awaitText("Vatapi Ganapatim")
            compose.onAllNodes(hasContentDescription("Remove favourite")).onFirst().performClick()
            compose.waitForIdle()
            awaitText("No favourites yet")
        }
    }

    @Test
    fun readerSwitchesToTheDevanagariSource() {
        launchFresh().use {
            openVatapiReader()
            awaitText("Latin")
            compose.onAllNodes(hasText("Devanagari", substring = true)).onFirst().performClick()
            compose.waitForIdle()
            awaitText("वातापि")
        }
    }

    @Test
    fun largeTextKeepsTabsAndSearchReachable() {
        adbShell("settings put system font_scale 2.0")
        try {
            launchFresh().use {
                awaitText("Home")
                listOf("Home", "Explore", "Library", "Settings").forEach {
                    tab(it).assertIsDisplayed()
                }
                awaitText("Title, incipit or a line of sahitya")
            }
        } finally {
            adbShell("settings put system font_scale 1.0")
        }
    }

    @Test
    fun reducedMotionKeepsTabsReachable() {
        adbShell("settings put global transition_animation_scale 0")
        adbShell("settings put global animator_duration_scale 0")
        adbShell("settings put global window_animation_scale 0")
        try {
            launchFresh().use {
                awaitText("Home")
                listOf("Explore", "Library", "Settings", "Home").forEach { label ->
                    tab(label).performClick()
                    compose.waitForIdle()
                    tab(label).assertIsDisplayed()
                }
            }
        } finally {
            adbShell("settings put global transition_animation_scale 1")
            adbShell("settings put global animator_duration_scale 1")
            adbShell("settings put global window_animation_scale 1")
        }
    }

    @Test
    fun labelledControlsCoverSearchBookmarkAndAppearance() {
        launchFresh().use {
            awaitText("Home")
            awaitDescription("Title, incipit or a line of sahitya")
            tab("Explore").performClick()
            compose.waitForIdle()
            awaitDescription("Title, incipit or a line of sahitya")
            tab("Settings").performClick()
            compose.waitForIdle()
            awaitText("Appearance")
            listOf("System", "Light", "Dark").forEach { awaitText(it) }
        }
    }

    @Test
    fun recordsExtendedJourneyDeviceIdentity() {
        println(
            "TRACK-140 R18 Android extended journey device: " +
                "${Build.MANUFACTURER} ${Build.MODEL} API ${Build.VERSION.SDK_INT}",
        )
    }
}
