package com.sangita.grantha.shared.presentation.entities

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.Modifier
import com.sangita.grantha.shared.domain.model.catalogue.CatalogueComposerDetailDto
import com.sangita.grantha.shared.presentation.RasikaCopy
import com.sangita.grantha.shared.presentation.components.KrithiCard
import com.sangita.grantha.shared.presentation.components.LoadStateContent
import com.sangita.grantha.shared.presentation.components.RasikaScreenHeader
import com.sangita.grantha.shared.presentation.theme.RasikaTheme
import com.sangita.grantha.shared.presentation.theme.RasikaTokens
import kotlin.uuid.Uuid

@Composable
fun ComposerDetailScreen(
    composerId: Uuid,
    presenter: EntityDetailPresenter,
    onOpenKrithi: (Uuid) -> Unit,
    onBack: () -> Unit,
    isFavourite: (Uuid) -> Boolean,
    onToggleFavourite: (Uuid, String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val state by presenter.state.collectAsStateWithLifecycle()
    LaunchedEffect(composerId) { presenter.openComposer(composerId) }
    Column(modifier.fillMaxSize()) {
        RasikaScreenHeader(
            title = state.composer?.name ?: RasikaCopy.BROWSE_COMPOSERS,
            onBack = onBack,
        )
        LoadStateContent(
            state = state.load,
            emptyTitle = RasikaCopy.UNAVAILABLE,
            emptyBody = RasikaCopy.EMPTY_SEARCH_BODY,
            onRetry = presenter::retry,
            modifier = Modifier.fillMaxSize(),
        ) {
            val composer = state.composer ?: return@LoadStateContent
            Column(
                Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = RasikaTokens.screen, vertical = RasikaTokens.md),
                verticalArrangement = Arrangement.spacedBy(RasikaTokens.sm),
            ) {
                composerLifespan(composer)?.let { Text(it, style = MaterialTheme.typography.bodyLarge) }
                composer.place?.takeIf { it.isNotBlank() }?.let { place ->
                    Text(place, style = MaterialTheme.typography.bodyLarge, color = RasikaTheme.colors.inkMuted)
                }
                Text(RasikaCopy.inThisLibrary(composer.publishedCompositionCount), color = RasikaTheme.colors.inkMuted)
                if (composer.aliases.isNotEmpty()) {
                    Text(
                        composer.aliases.joinToString(" · "),
                        style = MaterialTheme.typography.labelMedium,
                        color = RasikaTheme.colors.inkMuted,
                    )
                }
                Text(
                    RasikaCopy.WORKS_IN_LIBRARY,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(top = RasikaTokens.md),
                )
                state.works.forEach { summary ->
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

private fun composerLifespan(composer: CatalogueComposerDetailDto): String? {
    val birth = composer.birthYear
    val death = composer.deathYear
    return when {
        birth != null && death != null -> "$birth–$death"
        birth != null -> birth.toString()
        death != null -> death.toString()
        else -> null
    }
}
