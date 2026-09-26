package com.sangita.grantha.shared.presentation.favourites

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import com.sangita.grantha.shared.presentation.RasikaCopy
import com.sangita.grantha.shared.presentation.components.LoadState
import com.sangita.grantha.shared.presentation.components.LoadStateContent
import com.sangita.grantha.shared.presentation.components.RasikaFavouriteButton
import com.sangita.grantha.shared.presentation.components.RasikaPressable
import com.sangita.grantha.shared.presentation.components.RasikaScreenHeader
import com.sangita.grantha.shared.presentation.theme.RasikaTheme
import com.sangita.grantha.shared.mobile.storage.BookmarkRecord
import com.sangita.grantha.shared.presentation.theme.RasikaTokens
import kotlin.uuid.Uuid

@Composable
fun FavouritesScreen(
    presenter: FavouritesPresenter,
    bookmarks: List<BookmarkRecord>,
    onOpenKrithi: (Uuid) -> Unit,
    onOpenPreferences: () -> Unit,
    modifier: Modifier = Modifier,
) {
    LaunchedEffect(bookmarks) { presenter.refresh() }
    Column(modifier.fillMaxSize()) {
        RasikaScreenHeader(
            title = RasikaCopy.FAVOURITES_TITLE,
            subtitle = RasikaCopy.favStorageLine(bookmarks.size),
            onPreferences = onOpenPreferences,
        )
        val load = if (bookmarks.isEmpty()) LoadState.Empty else LoadState.Idle
        LoadStateContent(
            state = load,
            emptyTitle = RasikaCopy.EMPTY_FAVOURITES,
            emptyBody = RasikaCopy.EMPTY_FAVOURITES_BODY,
            onRetry = presenter::refresh,
            modifier = Modifier.fillMaxSize().padding(horizontal = RasikaTokens.screen),
        ) {
            LazyColumn(
                contentPadding = PaddingValues(
                    horizontal = RasikaTokens.screen,
                    vertical = RasikaTokens.md,
                ),
                verticalArrangement = Arrangement.spacedBy(RasikaTokens.sm),
            ) {
                items(bookmarks, key = { it.krithiId.toString() }) { bookmark ->
                    RasikaPressable(
                        onClick = { onOpenKrithi(bookmark.krithiId) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .semantics { contentDescription = bookmark.label },
                    ) {
                        Row(
                            modifier = Modifier.padding(RasikaTokens.md),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                bookmark.label,
                                style = MaterialTheme.typography.titleMedium,
                                modifier = Modifier.weight(1f),
                            )
                            RasikaFavouriteButton(
                                favourited = true,
                                onClick = { presenter.remove(bookmark.krithiId) },
                            )
                        }
                    }
                }
            }
        }
    }
}
