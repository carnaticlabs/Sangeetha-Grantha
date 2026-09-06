package com.sangita.grantha.shared.mobile.storage

import com.sangita.grantha.shared.domain.model.ScriptCodeDto
import com.sangita.grantha.shared.domain.model.catalogue.CatalogueContract
import com.sangita.grantha.shared.domain.serialization.UuidSerializer
import kotlinx.serialization.Serializable
import kotlin.uuid.Uuid

@Serializable
enum class AppearancePreference {
    SYSTEM,
    LIGHT,
    DARK,
}

@Serializable
enum class TextSizePreference {
    SMALL,
    MEDIUM,
    LARGE,
    EXTRA_LARGE,
}

@Serializable
data class BookmarkRecord(
    @Serializable(with = UuidSerializer::class)
    val krithiId: Uuid,
    val label: String,
    val createdAtEpochMs: Long,
    val schemaVersion: Int = LocalSettingsCodec.SCHEMA_VERSION,
)

@Serializable
data class PreferencesRecord(
    val preferredScript: ScriptCodeDto? = null,
    val textSize: TextSizePreference = TextSizePreference.MEDIUM,
    val appearance: AppearancePreference = AppearancePreference.SYSTEM,
    val schemaVersion: Int = LocalSettingsCodec.SCHEMA_VERSION,
)

@Serializable
data class LocalSettingsDocument(
    val bookmarks: List<BookmarkRecord> = emptyList(),
    val preferences: PreferencesRecord = PreferencesRecord(),
    val schemaVersion: Int = LocalSettingsCodec.SCHEMA_VERSION,
)

interface KeyValueStore {
    fun read(key: String): String?
    fun write(key: String, value: String)
    fun remove(key: String)
}

interface BookmarkStore {
    fun list(): List<BookmarkRecord>
    fun upsert(record: BookmarkRecord)
    fun remove(krithiId: Uuid)
}

interface PreferencesStore {
    fun read(): PreferencesRecord
    fun write(record: PreferencesRecord)
}

class InMemoryKeyValueStore(
    initial: Map<String, String> = emptyMap(),
) : KeyValueStore {
    private val values = initial.toMutableMap()
    override fun read(key: String): String? = values[key]
    override fun write(key: String, value: String) {
        values[key] = value
    }
    override fun remove(key: String) {
        values.remove(key)
    }
}

object LocalSettingsCodec {
    const val SCHEMA_VERSION: Int = 1
    const val SETTINGS_KEY: String = "rasika.local.settings.v1"

    fun encode(document: LocalSettingsDocument): String =
        com.sangita.grantha.shared.mobile.network.catalogueJson.encodeToString(
            LocalSettingsDocument.serializer(),
            document.copy(schemaVersion = SCHEMA_VERSION),
        )

    fun decode(raw: String?): LocalSettingsDocument {
        if (raw.isNullOrBlank()) return LocalSettingsDocument()
        return runCatching {
            com.sangita.grantha.shared.mobile.network.catalogueJson.decodeFromString(
                LocalSettingsDocument.serializer(),
                raw,
            )
        }.getOrDefault(LocalSettingsDocument())
    }

    fun clipLabel(label: String): String {
        val trimmed = label.trim()
        if (trimmed.length <= CatalogueContract.BOOKMARK_LABEL_MAX_CODE_POINTS) return trimmed
        return trimmed.take(CatalogueContract.BOOKMARK_LABEL_MAX_CODE_POINTS)
    }
}

class CodecBackedBookmarkStore(
    private val store: KeyValueStore,
    private val key: String = LocalSettingsCodec.SETTINGS_KEY,
) : BookmarkStore {
    override fun list(): List<BookmarkRecord> = document().bookmarks.sortedByDescending { it.createdAtEpochMs }

    override fun upsert(record: BookmarkRecord) {
        val clipped = record.copy(label = LocalSettingsCodec.clipLabel(record.label))
        val current = document()
        val next = current.copy(
            bookmarks = current.bookmarks.filterNot { it.krithiId == clipped.krithiId } + clipped,
        )
        write(next)
    }

    override fun remove(krithiId: Uuid) {
        val current = document()
        write(current.copy(bookmarks = current.bookmarks.filterNot { it.krithiId == krithiId }))
    }

    private fun document(): LocalSettingsDocument = LocalSettingsCodec.decode(store.read(key))

    private fun write(document: LocalSettingsDocument) {
        store.write(key, LocalSettingsCodec.encode(document))
    }
}

class CodecBackedPreferencesStore(
    private val store: KeyValueStore,
    private val key: String = LocalSettingsCodec.SETTINGS_KEY,
) : PreferencesStore {
    override fun read(): PreferencesRecord = LocalSettingsCodec.decode(store.read(key)).preferences

    override fun write(record: PreferencesRecord) {
        val current = LocalSettingsCodec.decode(store.read(key))
        store.write(
            key,
            LocalSettingsCodec.encode(current.copy(preferences = record.copy(schemaVersion = LocalSettingsCodec.SCHEMA_VERSION))),
        )
    }
}
