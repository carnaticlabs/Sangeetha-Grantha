package com.sangita.grantha.shared.presentation.search

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sangita.grantha.shared.presentation.RasikaCopy
import com.sangita.grantha.shared.presentation.components.KrithiCard
import com.sangita.grantha.shared.presentation.components.LoadState
import com.sangita.grantha.shared.presentation.components.LoadStateContent
import com.sangita.grantha.shared.presentation.components.RasikaChip
import com.sangita.grantha.shared.presentation.components.RasikaFilterChip
import com.sangita.grantha.shared.presentation.components.RasikaPressable
import com.sangita.grantha.shared.presentation.components.RasikaPrimaryButton
import com.sangita.grantha.shared.presentation.components.RasikaScreenHeader
import com.sangita.grantha.shared.presentation.components.RasikaSearchField
import com.sangita.grantha.shared.presentation.explore.ExploreCategory
import com.sangita.grantha.shared.presentation.explore.ragaRelationshipCaption
import com.sangita.grantha.shared.presentation.theme.RasikaTheme
import com.sangita.grantha.shared.presentation.theme.RasikaTokens
import kotlin.uuid.Uuid

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SearchScreen(
    presenter: SearchPresenter,
    onOpenKrithi: (Uuid) -> Unit,
    onOpenRaga: (Uuid) -> Unit,
    onOpenComposer: (Uuid) -> Unit,
    onOpenPreferences: () -> Unit,
    isFavourite: (Uuid) -> Boolean,
    onToggleFavourite: (Uuid, String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val state by presenter.state.collectAsStateWithLifecycle()
    Column(modifier.fillMaxSize()) {
        RasikaScreenHeader(
            title = RasikaCopy.SEARCH_TITLE,
            label = RasikaCopy.SEARCH_LABEL,
            onPreferences = onOpenPreferences,
        )
        if (state.filterSheetOpen) {
            FilterSheet(state, presenter)
            return
        }
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
                modifier = Modifier.padding(top = RasikaTokens.sm),
            )
            FlowRow(
                modifier = Modifier.padding(top = RasikaTokens.md),
                horizontalArrangement = Arrangement.spacedBy(RasikaTokens.xs),
                verticalArrangement = Arrangement.spacedBy(RasikaTokens.xs),
            ) {
                ExploreCategory.entries.forEach { category ->
                    RasikaChip(
                        selected = state.category == category,
                        onClick = { presenter.selectCategory(category) },
                        label = category.label(),
                    )
                }
            }
            if (state.hasUnappliedFilters) {
                Text(
                    RasikaCopy.FILTERS_PENDING,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(top = RasikaTokens.xs),
                )
            }
            if (state.category == ExploreCategory.Krithis) {
                Row(
                    Modifier.padding(top = RasikaTokens.sm),
                    horizontalArrangement = Arrangement.spacedBy(RasikaTokens.xs),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    RasikaChip(
                        selected = !state.appliedFacets.isEmpty,
                        onClick = presenter::openFilters,
                        label = RasikaCopy.FILTERS,
                    )
                    state.appliedFacets.ragaLabel?.let { label ->
                        RasikaFilterChip(label = label, onClear = presenter::removeAppliedRaga)
                    }
                    state.appliedFacets.composerLabel?.let { label ->
                        RasikaFilterChip(label = label, onClear = presenter::removeAppliedComposer)
                    }
                }
            }
            when (val load = state.load) {
                LoadState.Empty -> EmptyExplore(state, presenter)
                LoadState.Loading, is LoadState.Error -> LoadStateContent(
                    state = load,
                    emptyTitle = RasikaCopy.EMPTY_SEARCH,
                    emptyBody = RasikaCopy.EMPTY_SEARCH_BODY,
                    onRetry = presenter::retry,
                    modifier = Modifier.fillMaxSize(),
                ) {}
                LoadState.Idle -> if (
                    currentCount(state) == 0 &&
                    state.total == 0L &&
                    state.committedQuery.isEmpty() &&
                    state.appliedFacets.isEmpty
                ) {
                    Text(
                        RasikaCopy.SEARCH_IDLE,
                        style = MaterialTheme.typography.bodyLarge,
                        color = RasikaTheme.colors.inkMuted,
                        modifier = Modifier.padding(top = RasikaTokens.lg),
                    )
                } else {
                    ResultList(
                        state,
                        presenter,
                        onOpenKrithi,
                        onOpenRaga,
                        onOpenComposer,
                        isFavourite,
                        onToggleFavourite,
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun FilterSheet(state: SearchUiState, presenter: SearchPresenter) {
    Column(
        Modifier
            .fillMaxSize()
            .padding(horizontal = RasikaTokens.screen, vertical = RasikaTokens.md),
    ) {
        Text(RasikaCopy.FILTERS, style = MaterialTheme.typography.titleLarge)
        Text(
            RasikaCopy.FILTERS_HINT,
            style = MaterialTheme.typography.bodyLarge,
            color = RasikaTheme.colors.inkMuted,
            modifier = Modifier.padding(top = RasikaTokens.xs, bottom = RasikaTokens.md),
        )
        Text(RasikaCopy.BROWSE_RAGAS, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
        FlowRow(
            modifier = Modifier.padding(top = RasikaTokens.xs, bottom = RasikaTokens.md),
            horizontalArrangement = Arrangement.spacedBy(RasikaTokens.xs),
            verticalArrangement = Arrangement.spacedBy(RasikaTokens.xs),
        ) {
            RasikaChip(
                selected = state.draftFacets.ragaId == null,
                onClick = { presenter.draftRaga(null, null) },
                label = RasikaCopy.ALL_RAGAS,
            )
            state.filterRagas.forEach { raga ->
                RasikaChip(
                    selected = state.draftFacets.ragaId == raga.id,
                    onClick = { presenter.draftRaga(raga.id, raga.name) },
                    label = raga.name,
                )
            }
        }
        Text(RasikaCopy.BROWSE_COMPOSERS, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
        FlowRow(
            modifier = Modifier.padding(top = RasikaTokens.xs, bottom = RasikaTokens.lg),
            horizontalArrangement = Arrangement.spacedBy(RasikaTokens.xs),
            verticalArrangement = Arrangement.spacedBy(RasikaTokens.xs),
        ) {
            RasikaChip(
                selected = state.draftFacets.composerId == null,
                onClick = { presenter.draftComposer(null, null) },
                label = RasikaCopy.ALL_COMPOSERS,
            )
            state.filterComposers.forEach { composer ->
                RasikaChip(
                    selected = state.draftFacets.composerId == composer.id,
                    onClick = { presenter.draftComposer(composer.id, composer.name) },
                    label = composer.name,
                )
            }
        }
        RasikaPrimaryButton(label = RasikaCopy.APPLY_FILTERS, onClick = presenter::applyFilters)
        RasikaPrimaryButton(
            label = RasikaCopy.CANCEL,
            onClick = presenter::cancelFilters,
            modifier = Modifier.padding(top = RasikaTokens.sm),
        )
        RasikaPrimaryButton(
            label = RasikaCopy.RESET_FILTERS,
            onClick = presenter::resetDraftFilters,
            modifier = Modifier.padding(top = RasikaTokens.sm),
        )
    }
}

@Composable
private fun ResultList(
    state: SearchUiState,
    presenter: SearchPresenter,
    onOpenKrithi: (Uuid) -> Unit,
    onOpenRaga: (Uuid) -> Unit,
    onOpenComposer: (Uuid) -> Unit,
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
                    RasikaCopy.resultCount(state.total.toInt()),
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
        when (state.category) {
            ExploreCategory.Krithis -> items(state.items, key = { it.id.toString() }) { summary ->
                KrithiCard(
                    summary = summary,
                    onClick = { onOpenKrithi(summary.id) },
                    favourited = isFavourite(summary.id),
                    snippet = summary.incipit,
                    onToggleFavourite = { onToggleFavourite(summary.id, summary.title) },
                )
            }
            ExploreCategory.Ragas -> items(state.ragaItems, key = { it.id.toString() }) { raga ->
                DirectoryRow(
                    title = raga.name,
                    meta = directoryMeta(
                        ragaRelationshipCaption(
                            raga.melakartaNumber,
                            raga.parentRagaName,
                            raga.parentMelakartaNumber,
                        ),
                        raga.publishedCompositionCount,
                    ),
                    onClick = { onOpenRaga(raga.id) },
                )
            }
            ExploreCategory.Composers -> items(state.composerItems, key = { it.id.toString() }) { composer ->
                DirectoryRow(
                    title = composer.name,
                    meta = RasikaCopy.inThisLibrary(composer.publishedCompositionCount),
                    onClick = { onOpenComposer(composer.id) },
                )
            }
        }
        item(key = "paging") {
            when (val next = state.nextPageLoad) {
                is LoadState.Error -> {
                    Text(next.message, color = RasikaTheme.colors.inkMuted)
                    if (next.retryable) {
                        RasikaPrimaryButton(
                            label = RasikaCopy.RETRY,
                            onClick = presenter::retry,
                            modifier = Modifier.padding(top = RasikaTokens.xs),
                        )
                    }
                }
                LoadState.Loading -> Text(RasikaCopy.LOADING, color = RasikaTheme.colors.inkMuted)
                else -> if (state.hasMore) {
                    RasikaPrimaryButton(label = RasikaCopy.LOAD_MORE, onClick = presenter::loadNextPage)
                }
            }
        }
    }
}

@Composable
private fun EmptyExplore(state: SearchUiState, presenter: SearchPresenter) {
    Column(Modifier.padding(top = RasikaTokens.lg)) {
        Text(RasikaCopy.EMPTY_SEARCH, style = MaterialTheme.typography.titleLarge)
        Text(
            RasikaCopy.EMPTY_SEARCH_BODY,
            style = MaterialTheme.typography.bodyLarge,
            color = RasikaTheme.colors.inkMuted,
            modifier = Modifier.padding(top = RasikaTokens.xs),
        )
        if (state.category == ExploreCategory.Krithis) {
            RasikaPrimaryButton(
                label = RasikaCopy.BROWSE_RAGAS_ACTION,
                onClick = { presenter.selectCategory(ExploreCategory.Ragas) },
                modifier = Modifier.padding(top = RasikaTokens.md),
            )
        }
    }
}

@Composable
private fun DirectoryRow(title: String, meta: String, onClick: () -> Unit) {
    RasikaPressable(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(RasikaTokens.md), verticalArrangement = Arrangement.spacedBy(RasikaTokens.xxs)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text(meta, style = MaterialTheme.typography.labelMedium, color = RasikaTheme.colors.inkMuted)
        }
    }
}

private fun currentCount(state: SearchUiState): Int = when (state.category) {
    ExploreCategory.Krithis -> state.items.size
    ExploreCategory.Ragas -> state.ragaItems.size
    ExploreCategory.Composers -> state.composerItems.size
}

private fun ExploreCategory.label(): String = when (this) {
    ExploreCategory.Krithis -> RasikaCopy.CATEGORY_KRITHIS
    ExploreCategory.Ragas -> RasikaCopy.BROWSE_RAGAS
    ExploreCategory.Composers -> RasikaCopy.BROWSE_COMPOSERS
}

private fun directoryMeta(relationship: String?, count: Long): String = buildString {
    if (!relationship.isNullOrBlank()) append(relationship)
    if (isNotEmpty()) append(" · ")
    append(RasikaCopy.inThisLibrary(count))
}
