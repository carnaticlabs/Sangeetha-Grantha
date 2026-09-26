package com.sangita.grantha.mobile

import android.util.Log
import java.net.HttpURLConnection
import java.net.URI
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

/**
 * Debug builds may list several origins. Prefer a reachable LAN address so the
 * phone keeps working after the USB cable is removed, and keep 127.0.0.1 for
 * `adb reverse` when no LAN address answers.
 */
internal fun selectDebugApiBaseUrl(raw: String): String {
    val candidates = raw.split(',').map { it.trim() }.filter { it.isNotEmpty() }
    if (candidates.size <= 1) return candidates.firstOrNull() ?: raw
    val pool = Executors.newFixedThreadPool(candidates.size)
    val reachable = try {
        val checks = candidates.map { url -> url to pool.submit<Boolean> { healthOk(url) } }
        checks.mapNotNull { (url, check) ->
            val ok = try {
                check.get(2, TimeUnit.SECONDS)
            } catch (_: Exception) {
                false
            }
            if (ok) url else null
        }
    } finally {
        pool.shutdownNow()
    }
    val chosen = reachable.firstOrNull { !isLoopback(it) } ?: reachable.firstOrNull() ?: candidates.first()
    Log.i(TAG, "Catalogue API $chosen")
    return chosen
}

private fun healthOk(baseUrl: String): Boolean = try {
    val connection = (URI(baseUrl).resolve("/health").toURL().openConnection() as HttpURLConnection).apply {
        connectTimeout = 1_500
        readTimeout = 1_500
        requestMethod = "GET"
        instanceFollowRedirects = false
    }
    try {
        connection.responseCode in 200..299
    } finally {
        connection.disconnect()
    }
} catch (_: Exception) {
    false
}

private fun isLoopback(baseUrl: String): Boolean {
    val host = runCatching { URI(baseUrl).host }.getOrNull() ?: return false
    return host == "127.0.0.1" || host == "localhost" || host == "::1"
}

private const val TAG = "RasikaApi"
