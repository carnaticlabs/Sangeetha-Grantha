package com.sangita.grantha.shared.presentation.kshetra

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
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
import kotlinx.coroutines.launch

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
    scrollState: ScrollState = rememberScrollState(),
) {
    val state by presenter.state.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    LaunchedEffect(Unit) { presenter.loadIfNeeded() }
    // Changing the filter changes the content, so the list starts again from the top.
    fun toTop() {
        scope.launch { scrollState.scrollTo(0) }
    }
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
                    .verticalScroll(scrollState)
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
                            onClick = {
                                presenter.clearElement()
                                toTop()
                            },
                            label = KshetraCopy.ELEMENT_ALL,
                        )
                        state.availableElements.forEach { element ->
                            RasikaChip(
                                selected = state.element == element,
                                onClick = {
                                    presenter.selectElement(element)
                                    toTop()
                                },
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
                    RasikaChip(
                        selected = false,
                        onClick = {
                            presenter.clearElement()
                            toTop()
                        },
                        label = KshetraCopy.CLEAR_FILTER,
                    )
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
    listState: LazyListState = rememberLazyListState(),
) {
    val state by presenter.state.collectAsStateWithLifecycle()
    val uriHandler = LocalUriHandler.current
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
            // One lazy list, so only the rows on screen are composed: Tiruvarur sings 114 kritis.
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                state = listState,
                contentPadding = PaddingValues(horizontal = RasikaTokens.screen, vertical = RasikaTokens.md),
                verticalArrangement = Arrangement.spacedBy(RasikaTokens.xs),
            ) {
                if (detail.recordedGap) {
                    item(key = "gap-label") { SectionLabel(KshetraCopy.RECORDED_GAP) }
                    item(key = "gap-note") {
                        Text(KshetraCopy.GAP_NOTE, style = MaterialTheme.typography.labelMedium, color = RasikaTheme.colors.inkMuted)
                    }
                } else if (!detail.overview.isNullOrBlank()) {
                    item(key = "about-label") { SectionLabel(KshetraCopy.ABOUT) }
                }
                detail.overview?.takeIf { it.isNotBlank() }?.let {
                    item(key = "overview") { Text(it, style = MaterialTheme.typography.bodyLarge) }
                }
                state.mapLink?.let { link ->
                    item(key = "map-link") {
                        RasikaPressable(
                            onClick = { uriHandler.openUri(link.url) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = RasikaTokens.sm)
                                .semantics { contentDescription = KshetraCopy.mapLinkDescription(link) },
                        ) {
                            Column(Modifier.fillMaxWidth().padding(RasikaTokens.sm)) {
                                Text(
                                    KshetraCopy.OPEN_IN_MAPS,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.primary,
                                )
                                if (link.opensParentComplex) {
                                    Text(
                                        KshetraCopy.mapParentNote(link.targetName),
                                        style = MaterialTheme.typography.labelMedium,
                                        color = RasikaTheme.colors.inkMuted,
                                    )
                                }
                            }
                        }
                    }
                }
                detail.parent?.let { parent ->
                    item(key = "part-of-label") { SectionLabel(KshetraCopy.PART_OF) }
                    item(key = "part-of") {
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
                    item(key = "facts") {
                        Column(Modifier.padding(top = RasikaTokens.sm)) {
                            facts.forEach { (key, value) -> FactRow(key, value) }
                        }
                    }
                }
                if (detail.children.isNotEmpty()) {
                    item(key = "shrines-label") { SectionLabel(KshetraCopy.SHRINES_HEADING) }
                    items(detail.children, key = { "shrine:${it.id}" }) { child ->
                        KshetraRow(child, onClick = { onOpenKshetra(child.id) })
                    }
                }
                if (detail.krithis.isNotEmpty() && !detail.recordedGap) {
                    item(key = "kritis-label") { SectionLabel(KshetraCopy.KRITIS_HEADING) }
                    items(detail.krithis, key = { "kriti:${it.id}" }) { krithi ->
                        KritiRow(krithi, onClick = { onOpenKrithi(krithi.id) })
                    }
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
