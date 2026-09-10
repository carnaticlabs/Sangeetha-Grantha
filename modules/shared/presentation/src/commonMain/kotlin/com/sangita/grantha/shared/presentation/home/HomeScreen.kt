package com.sangita.grantha.shared.presentation.home

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sangita.grantha.shared.domain.model.catalogue.CatalogueDiscoveryFeatureDto
import com.sangita.grantha.shared.presentation.RasikaCopy
import com.sangita.grantha.shared.presentation.components.LoadState
import com.sangita.grantha.shared.presentation.components.RagaSequence
import com.sangita.grantha.shared.presentation.components.RasikaChip
import com.sangita.grantha.shared.presentation.components.RasikaPrimaryButton
import com.sangita.grantha.shared.presentation.components.RasikaScreenHeader
import com.sangita.grantha.shared.presentation.components.RasikaSearchField
import com.sangita.grantha.shared.presentation.theme.RasikaTheme
import com.sangita.grantha.shared.presentation.theme.RasikaTokens
import kotlin.uuid.Uuid

@Composable
fun HomeScreen(
    presenter: HomePresenter,
    onSearch: (String) -> Unit,
    onOpenKrithi: (Uuid) -> Unit,
    onOpenRagas: () -> Unit,
    onOpenComposers: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val state by presenter.state.collectAsStateWithLifecycle()
    LaunchedEffect(Unit) { presenter.loadIfNeeded() }
    Column(modifier.fillMaxSize()) {
        RasikaScreenHeader(
            title = RasikaCopy.HOME_TITLE,
            label = RasikaCopy.HOME_LABEL,
            subtitle = RasikaCopy.CATALOGUE_NAME,
        )
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = RasikaTokens.screen),
        ) {
            RasikaSearchField(
                value = state.query,
                onValueChange = presenter::onQueryChange,
                placeholder = RasikaCopy.SEARCH_PLACEHOLDER,
                onSearch = { onSearch(state.query) },
                modifier = Modifier.padding(top = RasikaTokens.md),
            )
            RasikaPrimaryButton(
                label = RasikaCopy.SEARCH_ACTION,
                onClick = { onSearch(state.query) },
                modifier = Modifier.padding(top = RasikaTokens.sm, bottom = RasikaTokens.md),
            )
            FeatureSection(
                feature = state.feature,
                load = state.featureLoad,
                onRetry = presenter::retryFeature,
                onOpen = onOpenKrithi,
            )
            Text(
                RasikaCopy.EXPLORE_SHORTCUTS,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(top = RasikaTokens.lg, bottom = RasikaTokens.xs),
            )
            Row(horizontalArrangement = Arrangement.spacedBy(RasikaTokens.xs)) {
                RasikaChip(selected = false, onClick = onOpenRagas, label = RasikaCopy.BROWSE_RAGAS)
                RasikaChip(selected = false, onClick = onOpenComposers, label = RasikaCopy.BROWSE_COMPOSERS)
            }
            Text(
                RasikaCopy.HOME_INVITE,
                style = MaterialTheme.typography.bodyLarge,
                color = RasikaTheme.colors.inkMuted,
                modifier = Modifier.padding(top = RasikaTokens.lg, bottom = RasikaTokens.xl),
            )
        }
    }
}

@Composable
private fun FeatureSection(
    feature: CatalogueDiscoveryFeatureDto?,
    load: LoadState,
    onRetry: () -> Unit,
    onOpen: (Uuid) -> Unit,
) {
    when (load) {
        LoadState.Loading -> Text(
            RasikaCopy.LOADING,
            style = MaterialTheme.typography.bodyLarge,
            color = RasikaTheme.colors.inkMuted,
            modifier = Modifier.padding(top = RasikaTokens.sm),
        )
        is LoadState.Error -> Column(Modifier.padding(top = RasikaTokens.sm)) {
            Text(
                RasikaCopy.FEATURE_UNAVAILABLE,
                style = MaterialTheme.typography.bodyLarge,
                color = RasikaTheme.colors.inkMuted,
            )
            if (load.retryable) {
                RasikaPrimaryButton(
                    label = RasikaCopy.RETRY,
                    onClick = onRetry,
                    modifier = Modifier.padding(top = RasikaTokens.sm),
                )
            }
        }
        LoadState.Empty, LoadState.Idle -> {
            if (feature == null) return
            val colors = RasikaTheme.colors
            Column(
                Modifier
                    .fillMaxWidth()
                    .padding(top = RasikaTokens.sm)
                    .background(colors.creamDeep, RoundedCornerShape(RasikaTokens.cardRadius))
                    .border(RasikaTokens.hairlineWidth, colors.hairline, RoundedCornerShape(RasikaTokens.cardRadius))
                    .padding(RasikaTokens.md)
                    .semantics { contentDescription = RasikaCopy.FEATURE_PANEL },
            ) {
                Text(
                    feature.heading.uppercase(),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                )
                Text(
                    feature.krithi.title,
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(top = RasikaTokens.xs),
                )
                Text(
                    feature.krithi.composer.name,
                    style = MaterialTheme.typography.bodyLarge,
                    color = colors.inkMuted,
                    modifier = Modifier.padding(top = 2.dp),
                )
                RagaSequence(feature.krithi.ragas, modifier = Modifier.padding(top = RasikaTokens.xs))
                Text(
                    feature.summary,
                    style = MaterialTheme.typography.bodyLarge,
                    color = colors.inkMuted,
                    modifier = Modifier.padding(top = RasikaTokens.sm),
                )
                RasikaPrimaryButton(
                    label = RasikaCopy.READ_COMPOSITION,
                    onClick = { onOpen(feature.krithi.id) },
                    modifier = Modifier.padding(top = RasikaTokens.md),
                )
            }
        }
    }
}
