package com.sangita.grantha.rasika

import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.sangita.grantha.mobile.MainActivity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test
import org.junit.runner.RunWith

/**
 * TRACK-140 N01 baseline harness. Assertions evolve with U01–U03; GA owns full journeys.
 * This smoke proves the instrumentation runner launches the current host.
 */
@RunWith(AndroidJUnit4::class)
class RasikaJourneyTest {
    @Test
    fun launchesMainActivity() {
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                assertEquals("com.sangita.grantha.rasika", activity.packageName)
                assertFalse(activity.isFinishing)
            }
        }
    }
}
