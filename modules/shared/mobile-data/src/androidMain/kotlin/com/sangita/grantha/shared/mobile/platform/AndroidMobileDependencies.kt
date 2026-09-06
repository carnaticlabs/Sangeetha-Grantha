package com.sangita.grantha.shared.mobile.platform

import android.content.Context
import com.sangita.grantha.shared.mobile.config.MobileApiConfig
import com.sangita.grantha.shared.mobile.network.KtorCatalogueApi
import com.sangita.grantha.shared.mobile.network.createMobileHttpClient
import com.sangita.grantha.shared.mobile.repository.CatalogueRepository
import com.sangita.grantha.shared.mobile.storage.KeyValueStore
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp

class AndroidKeyValueStore(
    context: Context,
    name: String = "rasika_preferences",
) : KeyValueStore {
    private val prefs = context.applicationContext.getSharedPreferences(name, Context.MODE_PRIVATE)

    override fun read(key: String): String? = prefs.getString(key, null)

    override fun write(key: String, value: String) {
        prefs.edit().putString(key, value).apply()
    }

    override fun remove(key: String) {
        prefs.edit().remove(key).apply()
    }
}

fun androidDebugHttpClient(config: MobileApiConfig = MobileApiConfig.debugAndroidEmulator()): HttpClient =
    createMobileHttpClient(
        engine = OkHttp.create {
            config {
                cache(null)
                followRedirects(false)
            }
        },
        config = config,
    )

/** Live catalogue for the Android host. Tests keep [com.sangita.grantha.shared.mobile.fixture.FixtureCatalogueApi]. */
fun androidLiveCatalogue(config: MobileApiConfig = MobileApiConfig.debugAndroidEmulator()): CatalogueRepository =
    CatalogueRepository(KtorCatalogueApi(androidDebugHttpClient(config)))
