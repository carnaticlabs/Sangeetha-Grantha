package com.sangita.grantha.shared.presentation.search

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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.sangita.grantha.shared.domain.model.catalogue.CatalogueKrithiSummaryDto
import com.sangita.grantha.shared.presentation.RasikaCopy
import com.sangita.grantha.shared.presentation.components.KrithiCard
import com.sangita.grantha.shared.presentation.components.LoadState
import com.sangita.grantha.shared.presentation.components.LoadStateContent
import com.sangita.grantha.shared.presentation.components.RasikaPrimaryButton
import com.sangita.grantha.shared.presentation.components.RasikaScreenHeader
import com.sangita.grantha.shared.presentation.components.RasikaSearchField
import com.sangita.grantha.shared.presentation.theme.RasikaTheme
import com.sangita.grantha.shared.presentation.theme.RasikaTokens
import kotlin.uuid.Uuid

@Composable
fun SearchScreen(
    presenter: SearchPresenter,
    onOpenKrithi: (Uuid) -> Unit,
    onOpenPreferences: () -> Unit,
    isFavourite: (Uuid) -> Boolean,
    onToggleFavourite: (Uuid, String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val state by presenter.state.collectAsState()
    Column(modifier.fillMaxSize()) {
        RasikaScreenHeader(
            title = RasikaCopy.SEARCH_TITLE,
            label = RasikaCopy.SEARCH_LABEL,
            onPreferences = onOpenPreferences,
        )
        Column(Modifier.fillMaxSize().padding(horizontal = RasikaTokens.screen)) {
            RasikaSearchField(
                value = state.query,
                onValueChange = presenter::onQueryChange,
                placeholder = RasikaCopy.SEARCH_PLACEHOLDER,
                onSearch = presenter::submit,
                modifier = Modifier.padding(top = RasikaTokens.md),
            )
            RasikaPrimaryButton(
                label = RasikaCopy.SEARCH_ACTION,
                onClick = presenter::submit,
                modifier = Modifier.padding(top = RasikaTokens.sm, bottom = RasikaTokens.md),
            )
            LoadStateContent(
                state = state.load,
                emptyTitle = RasikaCopy.EMPTY_SEARCH,
                emptyBody = RasikaCopy.EMPTY_SEARCH_BODY,
                onRetry = presenter::retry,
                modifier = Modifier.fillMaxSize(),
            ) {
                if (state.items.isEmpty() && state.load is LoadState.Idle) {
                    Text(
                        RasikaCopy.SEARCH_IDLE,
                        style = MaterialTheme.typography.bodyLarge,
                        color = RasikaTheme.colors.inkMuted,
                        modifier = Modifier.padding(top = RasikaTokens.lg),
                    )
                } else {
                    ResultList(state.items, onOpenKrithi, isFavourite, onToggleFavourite)
                }
            }
        }
    }
}

@Composable
private fun ResultList(
    items: List<CatalogueKrithiSummaryDto>,
    onOpenKrithi: (Uuid) -> Unit,
    isFavourite: (Uuid) -> Boolean,
    onToggleFavourite: (Uuid, String) -> Unit,
) {
    LazyColumn(
        contentPadding = PaddingValues(top = RasikaTokens.sm, bottom = RasikaTokens.xl),
        verticalArrangement = Arrangement.spacedBy(RasikaTokens.sm),
    ) {
        item(key = "count") {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    RasikaCopy.resultCount(items.size),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.error,
                )
                Text(
                    RasikaCopy.TITLE_ORDER,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.secondary,
                )
            }
        }
        items(items, key = { it.id.toString() }) { summary ->
            KrithiCard(
                summary = summary,
                onClick = { onOpenKrithi(summary.id) },
                favourited = isFavourite(summary.id),
                snippet = summary.incipit,
                onToggleFavourite = { onToggleFavourite(summary.id, summary.title) },
            )
        }
    }
}
