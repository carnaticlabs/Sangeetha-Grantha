package com.sangita.grantha.shared.presentation.kshetra

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sangita.grantha.shared.domain.model.catalogue.CatalogueKrithiSummaryDto
import com.sangita.grantha.shared.domain.model.catalogue.CatalogueTempleSummaryDto
import com.sangita.grantha.shared.presentation.components.LoadState
import com.sangita.grantha.shared.presentation.components.LoadStateContent
import com.sangita.grantha.shared.presentation.components.RagaSequence
import com.sangita.grantha.shared.presentation.components.RasikaChip
import com.sangita.grantha.shared.presentation.components.RasikaPressable
import com.sangita.grantha.shared.presentation.components.RasikaScreenHeader
import com.sangita.grantha.shared.presentation.theme.RasikaTheme
import com.sangita.grantha.shared.presentation.theme.RasikaTokens
import kotlin.uuid.Uuid

/**
 * Home shelf. It is supplementary, so a failed or empty load shows nothing rather than an error
 * that would sit beside the main feature.
 */
@Composable
fun KshetraShelf(
    presenter: KshetraListPresenter,
    onOpenAll: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val state by presenter.state.collectAsStateWithLifecycle()
    LaunchedEffect(Unit) { presenter.loadIfNeeded() }
    if (state.load != LoadState.Idle || state.items.isEmpty()) return
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(RasikaTokens.xs)) {
        Text(
            KshetraCopy.SECTION,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(top = RasikaTokens.lg),
        )
        Text(KshetraCopy.SHELF_BODY, style = MaterialTheme.typography.bodyLarge, color = RasikaTheme.colors.inkMuted)
        RasikaChip(selected = false, onClick = onOpenAll, label = KshetraCopy.ALL)
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun KshetrasScreen(
    presenter: KshetraListPresenter,
    onOpenKshetra: (Uuid) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val state by presenter.state.collectAsStateWithLifecycle()
    LaunchedEffect(Unit) { presenter.loadIfNeeded() }
    Column(modifier.fillMaxSize()) {
        RasikaScreenHeader(
            title = KshetraCopy.SECTION,
            label = KshetraCopy.SCREEN_LABEL,
            onBack = onBack,
        )
        LoadStateContent(
            state = state.load,
            emptyTitle = KshetraCopy.EMPTY_TITLE,
            emptyBody = KshetraCopy.EMPTY_BODY,
            onRetry = presenter::retry,
            modifier = Modifier.fillMaxSize(),
        ) {
            Column(
                Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = RasikaTokens.screen, vertical = RasikaTokens.md),
                verticalArrangement = Arrangement.spacedBy(RasikaTokens.xs),
            ) {
                Text(KshetraCopy.LIST_INTRO, style = MaterialTheme.typography.bodyLarge, color = RasikaTheme.colors.inkMuted)
                if (state.availableElements.isNotEmpty()) {
                    Text(
                        KshetraCopy.ELEMENT_FILTER,
                        style = MaterialTheme.typography.labelSmall,
                        color = RasikaTheme.colors.inkMuted,
                        modifier = Modifier.padding(top = RasikaTokens.sm),
                    )
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(RasikaTokens.xs),
                        verticalArrangement = Arrangement.spacedBy(RasikaTokens.xs),
                    ) {
                        RasikaChip(
                            selected = state.element == null,
                            onClick = presenter::clearElement,
                            label = KshetraCopy.ELEMENT_ALL,
                        )
                        state.availableElements.forEach { element ->
                            RasikaChip(
                                selected = state.element == element,
                                onClick = { presenter.selectElement(element) },
                                label = KshetraCopy.element(element),
                            )
                        }
                    }
                }
                if (state.filterMatchesNothing) {
                    Text(
                        KshetraCopy.FILTER_EMPTY_TITLE,
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(top = RasikaTokens.md),
                    )
                    Text(KshetraCopy.FILTER_EMPTY_BODY, style = MaterialTheme.typography.bodyLarge, color = RasikaTheme.colors.inkMuted)
                    RasikaChip(selected = false, onClick = presenter::clearElement, label = KshetraCopy.CLEAR_FILTER)
                }
                state.groups.forEach { group ->
                    Text(
                        KshetraCopy.mandalam(group.mandalam).uppercase(),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(top = RasikaTokens.sm).semantics { heading() },
                    )
                    group.items.forEach { item -> KshetraRow(item, onClick = { onOpenKshetra(item.id) }) }
                }
            }
        }
    }
}

@Composable
private fun KshetraRow(item: CatalogueTempleSummaryDto, onClick: () -> Unit) {
    val sub = KshetraCopy.rowSubtitle(item)
    RasikaPressable(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .semantics { contentDescription = KshetraCopy.rowDescription(item) },
    ) {
        Column(Modifier.fillMaxWidth().padding(RasikaTokens.sm)) {
            Text(item.name, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
            if (sub.isNotBlank()) {
                Text(sub, style = MaterialTheme.typography.labelMedium, color = RasikaTheme.colors.inkMuted)
            }
        }
    }
}

@Composable
fun KshetraDetailScreen(
    templeId: Uuid,
    presenter: KshetraDetailPresenter,
    onOpenKrithi: (Uuid) -> Unit,
    onOpenKshetra: (Uuid) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val state by presenter.state.collectAsStateWithLifecycle()
    LaunchedEffect(templeId) { presenter.open(templeId) }
    Column(modifier.fillMaxSize()) {
        RasikaScreenHeader(
            title = state.detail?.name ?: KshetraCopy.SECTION,
            subtitle = state.detail?.let { KshetraCopy.place(it.city, it.state) },
            onBack = onBack,
        )
        LoadStateContent(
            state = state.load,
            emptyTitle = KshetraCopy.EMPTY_TITLE,
            emptyBody = KshetraCopy.EMPTY_BODY,
            onRetry = presenter::retry,
            modifier = Modifier.fillMaxSize(),
        ) {
            val detail = state.detail ?: return@LoadStateContent
            Column(
                Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = RasikaTokens.screen, vertical = RasikaTokens.md),
                verticalArrangement = Arrangement.spacedBy(RasikaTokens.xs),
            ) {
                if (detail.recordedGap) {
                    SectionLabel(KshetraCopy.RECORDED_GAP)
                    Text(KshetraCopy.GAP_NOTE, style = MaterialTheme.typography.labelMedium, color = RasikaTheme.colors.inkMuted)
                } else if (!detail.overview.isNullOrBlank()) {
                    SectionLabel(KshetraCopy.ABOUT)
                }
                detail.overview?.takeIf { it.isNotBlank() }?.let {
                    Text(it, style = MaterialTheme.typography.bodyLarge)
                }
                detail.parent?.let { parent ->
                    SectionLabel(KshetraCopy.PART_OF)
                    RasikaPressable(
                        onClick = { onOpenKshetra(parent.id) },
                        modifier = Modifier.fillMaxWidth().semantics { contentDescription = "${KshetraCopy.PART_OF} ${parent.name}" },
                    ) {
                        Text(
                            parent.name,
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(RasikaTokens.sm),
                        )
                    }
                }
                val facts = listOfNotNull(
                    detail.mandalam?.let { KshetraCopy.REGION to KshetraCopy.mandalam(it) },
                    detail.element?.let { KshetraCopy.ELEMENT to KshetraCopy.element(it) },
                    detail.sthalaVriksha?.takeIf { it.isNotBlank() }?.let { KshetraCopy.STHALA_VRIKSHA to it },
                    detail.sthalaTirtha?.takeIf { it.isNotBlank() }?.let { KshetraCopy.STHALA_TIRTHA to it },
                    detail.nadiTirtha?.takeIf { it.isNotBlank() }?.let { KshetraCopy.NADI_TIRTHA to it },
                    detail.deityPosture?.let { KshetraCopy.POSTURE to KshetraCopy.posture(it) },
                )
                if (facts.isNotEmpty()) {
                    Column(Modifier.padding(top = RasikaTokens.sm)) {
                        facts.forEach { (key, value) -> FactRow(key, value) }
                    }
                }
                if (detail.children.isNotEmpty()) {
                    SectionLabel(KshetraCopy.SHRINES_HEADING)
                    detail.children.forEach { child -> KshetraRow(child, onClick = { onOpenKshetra(child.id) }) }
                }
                if (detail.krithis.isNotEmpty() && !detail.recordedGap) {
                    SectionLabel(KshetraCopy.KRITIS_HEADING)
                    detail.krithis.forEach { krithi -> KritiRow(krithi, onClick = { onOpenKrithi(krithi.id) }) }
                }
            }
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text.uppercase(),
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(top = RasikaTokens.sm).semantics { heading() },
    )
}

@Composable
private fun FactRow(key: String, value: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = RasikaTokens.xs).semantics(mergeDescendants = true) {
        contentDescription = "$key, $value"
    }) {
        Text(
            key,
            style = MaterialTheme.typography.labelSmall,
            color = RasikaTheme.colors.inkMuted,
            modifier = Modifier.width(112.dp).padding(top = 4.dp),
        )
        Text(value, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
    }
}

@Composable
private fun KritiRow(krithi: CatalogueKrithiSummaryDto, onClick: () -> Unit) {
    RasikaPressable(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .semantics { contentDescription = "${krithi.title}, ${krithi.composer.name}" },
    ) {
        Column(Modifier.fillMaxWidth().padding(RasikaTokens.sm)) {
            Text(krithi.title, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
            Text(krithi.composer.name, style = MaterialTheme.typography.labelMedium, color = RasikaTheme.colors.inkMuted)
            if (krithi.ragas.isNotEmpty()) {
                RagaSequence(krithi.ragas, modifier = Modifier.padding(top = 3.dp))
            }
        }
    }
}
