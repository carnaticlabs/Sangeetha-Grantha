package com.sangita.grantha.shared.presentation.browse

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
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sangita.grantha.shared.presentation.RasikaCopy
import com.sangita.grantha.shared.presentation.components.KrithiCard
import com.sangita.grantha.shared.presentation.components.LoadState
import com.sangita.grantha.shared.presentation.components.LoadStateContent
import com.sangita.grantha.shared.presentation.components.RasikaChip
import com.sangita.grantha.shared.presentation.components.RasikaPressable
import com.sangita.grantha.shared.presentation.components.RasikaPrimaryButton
import com.sangita.grantha.shared.presentation.components.RasikaScreenHeader
import com.sangita.grantha.shared.presentation.components.RasikaSearchField
import com.sangita.grantha.shared.presentation.explore.ragaRelationshipCaption
import com.sangita.grantha.shared.presentation.theme.RasikaTheme
import com.sangita.grantha.shared.presentation.theme.RasikaTokens
import kotlin.uuid.Uuid

@Composable
fun BrowseScreen(
    presenter: BrowsePresenter,
    onOpenKrithi: (Uuid) -> Unit,
    onOpenRaga: (Uuid) -> Unit,
    onOpenComposer: (Uuid) -> Unit,
    isFavourite: (Uuid) -> Boolean,
    onToggleFavourite: (Uuid, String) -> Unit,
    modifier: Modifier = Modifier,
    onBack: (() -> Unit)? = null,
) {
    val state by presenter.state.collectAsStateWithLifecycle()
    LaunchedEffect(Unit) {
        if (state.ragas.isEmpty() && state.load is LoadState.Idle) presenter.loadDirectory()
    }
    val showingAssociated = state.selectedRagaId != null || state.selectedComposerId != null
    val contextTitle = when {
        state.selectedRagaId != null ->
            state.ragas.firstOrNull { it.id == state.selectedRagaId }?.name ?: RasikaCopy.BROWSE_RAGAS
        state.selectedComposerId != null ->
            state.composers.firstOrNull { it.id == state.selectedComposerId }?.name ?: RasikaCopy.BROWSE_COMPOSERS
        else -> RasikaCopy.BROWSE_TITLE
    }
    Column(modifier.fillMaxSize()) {
        RasikaScreenHeader(
            title = contextTitle,
            onBack = when {
                showingAssociated -> {
                    { presenter.onDirectory(state.directory) }
                }
                onBack != null -> onBack
                else -> null
            },
        )
        Column(Modifier.fillMaxSize().padding(horizontal = RasikaTokens.screen)) {
            if (!showingAssociated) {
                Row(
                    modifier = Modifier.padding(top = RasikaTokens.md),
                    horizontalArrangement = Arrangement.spacedBy(RasikaTokens.xs),
                ) {
                    RasikaChip(
                        selected = state.directory == BrowseDirectory.Ragas,
                        onClick = { presenter.onDirectory(BrowseDirectory.Ragas) },
                        label = RasikaCopy.BROWSE_RAGAS,
                    )
                    RasikaChip(
                        selected = state.directory == BrowseDirectory.Composers,
                        onClick = { presenter.onDirectory(BrowseDirectory.Composers) },
                        label = RasikaCopy.BROWSE_COMPOSERS,
                    )
                }
                RasikaSearchField(
                    value = state.query,
                    onValueChange = presenter::onQueryChange,
                    placeholder = RasikaCopy.BROWSE_FILTER,
                    onSearch = presenter::loadDirectory,
                    modifier = Modifier.padding(top = RasikaTokens.sm),
                )
                RasikaPrimaryButton(
                    label = RasikaCopy.SEARCH_ACTION,
                    onClick = presenter::loadDirectory,
                    modifier = Modifier.padding(top = RasikaTokens.sm, bottom = RasikaTokens.md),
                )
            } else {
                androidx.compose.foundation.layout.Spacer(
                    Modifier.padding(top = RasikaTokens.md),
                )
            }
            LoadStateContent(
                state = state.load,
                emptyTitle = RasikaCopy.EMPTY_SEARCH,
                emptyBody = RasikaCopy.EMPTY_SEARCH_BODY,
                onRetry = presenter::retry,
                modifier = Modifier.fillMaxSize(),
            ) {
                LazyColumn(
                    contentPadding = PaddingValues(bottom = RasikaTokens.xl),
                    verticalArrangement = Arrangement.spacedBy(RasikaTokens.sm),
                ) {
                    if (state.directory == BrowseDirectory.Ragas && state.selectedRagaId == null) {
                        items(state.ragas, key = { it.id.toString() }) { raga ->
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
                    } else if (state.directory == BrowseDirectory.Composers && state.selectedComposerId == null) {
                        items(state.composers, key = { it.id.toString() }) { composer ->
                            DirectoryRow(
                                title = composer.name,
                                meta = RasikaCopy.inThisLibrary(composer.publishedCompositionCount),
                                onClick = { onOpenComposer(composer.id) },
                            )
                        }
                    } else {
                        items(state.associated, key = { it.id.toString() }) { summary ->
                            KrithiCard(
                                summary = summary,
                                onClick = { onOpenKrithi(summary.id) },
                                favourited = isFavourite(summary.id),
                                onToggleFavourite = { onToggleFavourite(summary.id, summary.title) },
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun directoryMeta(relationship: String?, count: Long): String = buildString {
    if (!relationship.isNullOrBlank()) append(relationship)
    if (isNotEmpty()) append(" · ")
    append(RasikaCopy.inThisLibrary(count))
}

@Composable
private fun DirectoryRow(title: String, meta: String, onClick: () -> Unit) {
    RasikaPressable(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .semantics { contentDescription = title },
    ) {
        Column(Modifier.padding(RasikaTokens.md), verticalArrangement = Arrangement.spacedBy(RasikaTokens.xxs)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text(meta, style = MaterialTheme.typography.labelMedium, color = RasikaTheme.colors.inkMuted)
        }
    }
}
