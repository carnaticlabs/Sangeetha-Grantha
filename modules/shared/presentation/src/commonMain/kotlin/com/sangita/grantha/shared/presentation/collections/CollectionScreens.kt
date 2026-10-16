package com.sangita.grantha.shared.presentation.collections

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
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
import com.sangita.grantha.shared.domain.model.catalogue.CatalogueCollectionMemberDto
import com.sangita.grantha.shared.domain.model.catalogue.CatalogueCollectionRoleDto
import com.sangita.grantha.shared.domain.model.catalogue.CatalogueCollectionSummaryDto
import com.sangita.grantha.shared.domain.model.catalogue.CatalogueOrderBasisDto
import com.sangita.grantha.shared.presentation.components.LoadState
import com.sangita.grantha.shared.presentation.components.LoadStateContent
import com.sangita.grantha.shared.presentation.components.RagaSequence
import com.sangita.grantha.shared.presentation.components.RasikaChip
import com.sangita.grantha.shared.presentation.components.RasikaPressable
import com.sangita.grantha.shared.presentation.components.RasikaScreenHeader
import com.sangita.grantha.shared.presentation.theme.RasikaTheme
import com.sangita.grantha.shared.presentation.theme.RasikaTokens
import kotlin.uuid.Uuid

private const val SHELF_LIMIT = 3

/**
 * Home shelf. It is supplementary, so a failed or empty load shows nothing rather than an error
 * that would sit beside the main feature.
 */
@Composable
fun CollectionsShelf(
    presenter: CollectionsPresenter,
    onOpenCollection: (String) -> Unit,
    onOpenAll: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val state by presenter.state.collectAsStateWithLifecycle()
    LaunchedEffect(Unit) { presenter.loadIfNeeded() }
    if (state.load != LoadState.Idle || state.items.isEmpty()) return
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(RasikaTokens.xs)) {
        Text(
            CollectionCopy.SECTION,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(top = RasikaTokens.lg),
        )
        state.items.take(SHELF_LIMIT).forEach { item ->
            CollectionRow(item, onClick = { onOpenCollection(item.slug) })
        }
        if (state.items.size > SHELF_LIMIT) {
            RasikaChip(selected = false, onClick = onOpenAll, label = CollectionCopy.ALL)
        }
    }
}

@Composable
fun CollectionsScreen(
    presenter: CollectionsPresenter,
    onOpenCollection: (String) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val state by presenter.state.collectAsStateWithLifecycle()
    LaunchedEffect(Unit) { presenter.loadIfNeeded() }
    Column(modifier.fillMaxSize()) {
        RasikaScreenHeader(
            title = CollectionCopy.SECTION,
            label = CollectionCopy.SCREEN_LABEL,
            onBack = onBack,
        )
        LoadStateContent(
            state = state.load,
            emptyTitle = CollectionCopy.EMPTY_TITLE,
            emptyBody = CollectionCopy.EMPTY_BODY,
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
                state.items.groupBy { it.composer?.name.orEmpty() }.forEach { (composer, items) ->
                    if (composer.isNotBlank()) {
                        Text(
                            composer.uppercase(),
                            style = MaterialTheme.typography.labelSmall,
                            color = RasikaTheme.colors.inkMuted,
                            modifier = Modifier.padding(top = RasikaTokens.sm),
                        )
                    }
                    items.forEach { item -> CollectionRow(item, onClick = { onOpenCollection(item.slug) }) }
                }
            }
        }
    }
}

@Composable
private fun CollectionRow(item: CatalogueCollectionSummaryDto, onClick: () -> Unit) {
    val sub = listOfNotNull(item.composer?.name, CollectionCopy.memberCount(item.memberCount)).joinToString(" · ")
    RasikaPressable(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .semantics { contentDescription = "${item.name}, $sub" },
    ) {
        Column(Modifier.fillMaxWidth().padding(RasikaTokens.sm)) {
            Text(item.name, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
            Text(sub, style = MaterialTheme.typography.labelMedium, color = RasikaTheme.colors.inkMuted)
        }
    }
}

@Composable
fun CollectionDetailScreen(
    slug: String,
    presenter: CollectionDetailPresenter,
    onOpenKrithi: (Uuid) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val state by presenter.state.collectAsStateWithLifecycle()
    LaunchedEffect(slug) { presenter.open(slug) }
    Column(modifier.fillMaxSize()) {
        RasikaScreenHeader(
            title = state.detail?.name ?: CollectionCopy.SECTION,
            subtitle = state.detail?.composer?.name,
            onBack = onBack,
        )
        LoadStateContent(
            state = state.load,
            emptyTitle = CollectionCopy.EMPTY_TITLE,
            emptyBody = CollectionCopy.EMPTY_BODY,
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
                detail.overview?.takeIf { it.isNotBlank() }?.let {
                    Text(it, style = MaterialTheme.typography.bodyLarge)
                }
                CollectionCopy.orderNote(detail.orderBasis, detail.orderConvention)?.let {
                    Text(it, style = MaterialTheme.typography.labelMedium, color = RasikaTheme.colors.inkMuted)
                }
                if (!detail.isComplete) {
                    Text(
                        CollectionCopy.OPEN_SET,
                        style = MaterialTheme.typography.labelMedium,
                        color = RasikaTheme.colors.inkMuted,
                    )
                }
                Text(
                    CollectionCopy.memberCount(detail.members.size).uppercase(),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(top = RasikaTokens.sm),
                )
                detail.members.forEach { member ->
                    MemberRow(
                        member = member,
                        showOrdinal = detail.orderBasis == CatalogueOrderBasisDto.SOURCE,
                        onClick = { onOpenKrithi(member.krithi.id) },
                    )
                }
            }
        }
    }
}

@Composable
private fun MemberRow(member: CatalogueCollectionMemberDto, showOrdinal: Boolean, onClick: () -> Unit) {
    val ordinal = member.position?.takeIf { showOrdinal }
    val detailLine = listOfNotNull(
        member.axisValue,
        member.attributes["yogini"]?.let { "Yogini $it" },
        CollectionCopy.roleLabel(member.role),
    ).joinToString(" · ")
    RasikaPressable(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .semantics {
                contentDescription = listOfNotNull(
                    ordinal?.let { "Number $it" },
                    member.krithi.title,
                    detailLine.takeIf { it.isNotBlank() },
                ).joinToString(", ")
            },
    ) {
        Row(
            Modifier.fillMaxWidth().padding(RasikaTokens.sm),
            horizontalArrangement = Arrangement.spacedBy(RasikaTokens.sm),
        ) {
            if (showOrdinal) {
                Text(
                    ordinal?.toString().orEmpty(),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.width(24.dp),
                )
            }
            Column(Modifier.weight(1f)) {
                Text(
                    member.krithi.title,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                if (member.krithi.ragas.isNotEmpty()) {
                    RagaSequence(member.krithi.ragas, modifier = Modifier.padding(top = 3.dp))
                }
                if (detailLine.isNotBlank()) {
                    Text(
                        detailLine,
                        style = MaterialTheme.typography.labelMedium,
                        color = if (member.role == CatalogueCollectionRoleDto.DISPUTED_CONJECTURE) {
                            MaterialTheme.colorScheme.error
                        } else {
                            RasikaTheme.colors.inkMuted
                        },
                        modifier = Modifier.padding(top = 3.dp),
                    )
                }
            }
        }
    }
}
