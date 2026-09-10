package com.sangita.grantha.shared.mobile.repository

import com.sangita.grantha.shared.mobile.storage.BookmarkRecord
import com.sangita.grantha.shared.mobile.storage.BookmarkStore
import com.sangita.grantha.shared.mobile.storage.LocalWriteResult
import kotlin.uuid.Uuid

class FavouritesRepository(
    private val store: BookmarkStore,
    private val clockMs: () -> Long = { kotlin.time.Clock.System.now().toEpochMilliseconds() },
) {
    fun list(): List<BookmarkRecord> = store.list()

    fun isFavourite(krithiId: Uuid): Boolean = store.list().any { it.krithiId == krithiId }

    fun save(krithiId: Uuid, label: String): LocalWriteResult =
        store.upsert(
            BookmarkRecord(
                krithiId = krithiId,
                label = label,
                createdAtEpochMs = clockMs(),
            ),
        )

    fun remove(krithiId: Uuid): LocalWriteResult = store.remove(krithiId)
}
