package com.sangita.grantha.shared.presentation.entities

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.Modifier
import com.sangita.grantha.shared.domain.model.catalogue.CatalogueNomenclatureLinkDto
import com.sangita.grantha.shared.presentation.RasikaCopy
import com.sangita.grantha.shared.presentation.components.KrithiCard
import com.sangita.grantha.shared.presentation.components.LoadStateContent
import com.sangita.grantha.shared.presentation.components.RasikaPressable
import com.sangita.grantha.shared.presentation.components.RasikaScreenHeader
import com.sangita.grantha.shared.presentation.explore.ragaRelationshipCaption
import com.sangita.grantha.shared.presentation.theme.RasikaTheme
import com.sangita.grantha.shared.presentation.theme.RasikaTokens
import kotlin.uuid.Uuid

@Composable
fun RagaDetailScreen(
    ragaId: Uuid,
    presenter: EntityDetailPresenter,
    onOpenKrithi: (Uuid) -> Unit,
    onOpenRelatedRaga: (Uuid) -> Unit,
    onBack: () -> Unit,
    isFavourite: (Uuid) -> Boolean,
    onToggleFavourite: (Uuid, String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val state by presenter.state.collectAsStateWithLifecycle()
    LaunchedEffect(ragaId) { presenter.openRaga(ragaId) }
    Column(modifier.fillMaxSize()) {
        RasikaScreenHeader(
            title = state.raga?.name ?: RasikaCopy.BROWSE_RAGAS,
            onBack = onBack,
        )
        LoadStateContent(
            state = state.load,
            emptyTitle = RasikaCopy.UNAVAILABLE,
            emptyBody = RasikaCopy.EMPTY_SEARCH_BODY,
            onRetry = presenter::retry,
            modifier = Modifier.fillMaxSize(),
        ) {
            val raga = state.raga ?: return@LoadStateContent
            Column(
                Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = RasikaTokens.screen, vertical = RasikaTokens.md),
                verticalArrangement = Arrangement.spacedBy(RasikaTokens.sm),
            ) {
                ragaRelationshipCaption(
                    raga.melakartaNumber,
                    raga.parentRagaName,
                    raga.parentMelakartaNumber,
                )?.let { caption ->
                    Text(caption, style = MaterialTheme.typography.bodyLarge)
                }
                Text(RasikaCopy.inThisLibrary(raga.publishedCompositionCount), color = RasikaTheme.colors.inkMuted)
                if (raga.aliases.isNotEmpty()) {
                    Text(
                        raga.aliases.joinToString(" · "),
                        style = MaterialTheme.typography.labelMedium,
                        color = RasikaTheme.colors.inkMuted,
                    )
                }
                ScaleBlock(RasikaCopy.AROHANAM, raga.arohanam)
                ScaleBlock(RasikaCopy.AVAROHANAM, raga.avarohanam)
                raga.nomenclatureLinks.forEach { link ->
                    RelatedRagaRow(link, onOpenRelatedRaga)
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

@Composable
private fun ScaleBlock(label: String, scale: String?) {
    if (scale.isNullOrBlank()) return
    Text(label, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
    SelectionContainer {
        Text(
            scale,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun RelatedRagaRow(link: CatalogueNomenclatureLinkDto, onOpen: (Uuid) -> Unit) {
    RasikaPressable(onClick = { onOpen(link.relatedRagaId) }, modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(vertical = RasikaTokens.xs)) {
            Text(link.relatedRagaName, style = MaterialTheme.typography.titleMedium)
            Text(link.relationLabel, style = MaterialTheme.typography.labelMedium, color = RasikaTheme.colors.inkMuted)
        }
    }
}
