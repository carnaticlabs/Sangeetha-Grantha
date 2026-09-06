package com.sangita.grantha.shared.mobile.config

import kotlin.test.Test
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class MobileApiConfigTest {
    @Test
    fun debugCleartextIsExplicit() {
        assertTrue(MobileApiConfig.debugAndroidEmulator().allowCleartext)
        assertTrue(MobileApiConfig.debugIosSimulator().allowCleartext)
        assertFalse(MobileApiConfig.release("https://api.sangitagrantha.org").allowCleartext)
    }

    @Test
    fun releaseRejectsHttpAndBlank() {
        assertFailsWith<IllegalArgumentException> {
            MobileApiConfig.release("http://example.com")
        }
        assertFailsWith<IllegalArgumentException> {
            MobileApiConfig(baseUrl = "")
        }
        assertFailsWith<IllegalArgumentException> {
            MobileApiConfig.release("https://api.sangitagrantha.org/")
        }
    }
}
