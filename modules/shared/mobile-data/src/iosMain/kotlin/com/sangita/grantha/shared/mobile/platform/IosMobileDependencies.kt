package com.sangita.grantha.shared.mobile.platform

import com.sangita.grantha.shared.mobile.config.MobileApiConfig
import com.sangita.grantha.shared.mobile.network.KtorCatalogueApi
import com.sangita.grantha.shared.mobile.network.createMobileHttpClient
import com.sangita.grantha.shared.mobile.repository.CatalogueRepository
import com.sangita.grantha.shared.mobile.storage.KeyValueStore
import io.ktor.client.HttpClient
import io.ktor.client.engine.darwin.Darwin
import platform.Foundation.NSUserDefaults

class IosKeyValueStore(
    private val defaults: NSUserDefaults = NSUserDefaults.standardUserDefaults,
) : KeyValueStore {
    override fun read(key: String): String? = defaults.stringForKey(key)

    override fun write(key: String, value: String) {
        defaults.setObject(value, key)
    }

    override fun remove(key: String) {
        defaults.removeObjectForKey(key)
    }
}

fun iosDebugHttpClient(config: MobileApiConfig = MobileApiConfig.debugIosSimulator()): HttpClient =
    createMobileHttpClient(
        engine = Darwin.create {
            configureSession {
                URLCache = null
            }
        },
        config = config,
    )

/** Live catalogue for the iOS host. Tests keep [com.sangita.grantha.shared.mobile.fixture.FixtureCatalogueApi]. */
fun iosLiveCatalogue(config: MobileApiConfig = MobileApiConfig.debugIosSimulator()): CatalogueRepository =
    CatalogueRepository(KtorCatalogueApi(iosDebugHttpClient(config)))
