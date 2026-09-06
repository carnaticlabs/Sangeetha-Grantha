package com.sangita.grantha.shared.mobile.config

data class MobileApiConfig(
    val baseUrl: String,
    val requestTimeoutMs: Long = 15_000,
    val connectTimeoutMs: Long = 10_000,
    val allowCleartext: Boolean = false,
) {
    init {
        require(baseUrl.isNotBlank()) { "API base URL is required" }
        require(baseUrl.startsWith("https://") || allowCleartext) {
            "Release transport requires HTTPS; debug may permit cleartext with allowCleartext=true"
        }
        require(!baseUrl.endsWith("/")) { "baseUrl must not end with /" }
    }

    companion object {
        const val ANDROID_EMULATOR_DEBUG_URL: String = "http://10.0.2.2:8080"
        const val IOS_SIMULATOR_DEBUG_URL: String = "http://127.0.0.1:8080"

        fun debugAndroidEmulator(): MobileApiConfig =
            MobileApiConfig(baseUrl = ANDROID_EMULATOR_DEBUG_URL, allowCleartext = true)

        fun debugIosSimulator(): MobileApiConfig =
            MobileApiConfig(baseUrl = IOS_SIMULATOR_DEBUG_URL, allowCleartext = true)

        fun release(httpsBaseUrl: String): MobileApiConfig =
            MobileApiConfig(baseUrl = httpsBaseUrl, allowCleartext = false)
    }
}
