package com.sangita.grantha.shared.presentation

import platform.Foundation.NSBundle

/**
 * Debug device builds may list several origins in Info.plist (`RasikaApiBaseUrls`),
 * separated by `|` or commas. The first LAN address is used so the phone works over
 * Wi-Fi; 127.0.0.1 stays available for the simulator when no LAN address is set.
 */
internal fun selectDebugApiBaseUrl(): String? {
    val raw = NSBundle.mainBundle.objectForInfoDictionaryKey("RasikaApiBaseUrls") as? String
    if (raw.isNullOrBlank() || raw.startsWith("$(")) return null
    val candidates = raw.split(',', '|').map { it.trim() }.filter { it.isNotEmpty() }
    val chosen = candidates.firstOrNull { !isLoopback(it) } ?: candidates.firstOrNull() ?: return null
    println("Catalogue API $chosen")
    return chosen
}

private fun isLoopback(baseUrl: String): Boolean {
    val host = baseUrl.removePrefix("http://").removePrefix("https://").substringBefore('/').substringBefore(':')
    return host == "127.0.0.1" || host == "localhost" || host == "[::1]"
}
