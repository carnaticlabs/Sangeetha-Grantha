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
    fun upsert(record: BookmarkRecord): LocalWriteResult
    fun remove(krithiId: Uuid): LocalWriteResult
}

interface PreferencesStore {
    fun read(): PreferencesRecord
    fun write(record: PreferencesRecord): LocalWriteResult
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

    fun decode(raw: String?): LocalSettingsDocument =
        when (val loaded = load(raw)) {
            is LocalDocumentLoad.Ok -> loaded.document
            is LocalDocumentLoad.Corrupt, is LocalDocumentLoad.UnsupportedVersion -> LocalSettingsDocument()
        }

    fun load(raw: String?): LocalDocumentLoad {
        if (raw.isNullOrBlank()) return LocalDocumentLoad.Ok(LocalSettingsDocument())
        val parsed = runCatching {
            com.sangita.grantha.shared.mobile.network.catalogueJson.decodeFromString(
                LocalSettingsDocument.serializer(),
                raw,
            )
        }
        val document = parsed.getOrNull() ?: return LocalDocumentLoad.Corrupt(raw)
        if (document.schemaVersion > SCHEMA_VERSION) {
            return LocalDocumentLoad.UnsupportedVersion(raw, document.schemaVersion)
        }
        return LocalDocumentLoad.Ok(document)
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
    private val documents: LocalDocumentStore = LocalDocumentStore(store, key),
) : BookmarkStore {
    override fun list(): List<BookmarkRecord> =
        when (val loaded = documents.load()) {
            is LocalDocumentLoad.Ok -> loaded.document.bookmarks.sortedByDescending { it.createdAtEpochMs }
            is LocalDocumentLoad.Corrupt, is LocalDocumentLoad.UnsupportedVersion -> emptyList()
        }

    override fun upsert(record: BookmarkRecord): LocalWriteResult {
        val clipped = record.copy(label = LocalSettingsCodec.clipLabel(record.label))
        return documents.mutate { current ->
            current.copy(bookmarks = current.bookmarks.filterNot { it.krithiId == clipped.krithiId } + clipped)
        }
    }

    override fun remove(krithiId: Uuid): LocalWriteResult =
        documents.mutate { current ->
            current.copy(bookmarks = current.bookmarks.filterNot { it.krithiId == krithiId })
        }
}

class CodecBackedPreferencesStore(
    private val store: KeyValueStore,
    private val key: String = LocalSettingsCodec.SETTINGS_KEY,
    private val documents: LocalDocumentStore = LocalDocumentStore(store, key),
) : PreferencesStore {
    override fun read(): PreferencesRecord =
        when (val loaded = documents.load()) {
            is LocalDocumentLoad.Ok -> loaded.document.preferences
            is LocalDocumentLoad.Corrupt, is LocalDocumentLoad.UnsupportedVersion -> PreferencesRecord()
        }

    override fun write(record: PreferencesRecord): LocalWriteResult =
        documents.mutate { current ->
            current.copy(preferences = record.copy(schemaVersion = LocalSettingsCodec.SCHEMA_VERSION))
        }
}
