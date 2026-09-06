package com.sangita.grantha.shared.presentation.reader

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.BiasAlignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import com.sangita.grantha.shared.domain.model.MusicalFormDto
import com.sangita.grantha.shared.domain.model.catalogue.CatalogueCompletenessDto
import com.sangita.grantha.shared.presentation.RasikaCopy
import com.sangita.grantha.shared.presentation.Res
import com.sangita.grantha.shared.presentation.components.LoadStateContent
import com.sangita.grantha.shared.presentation.components.PaintedCornice
import com.sangita.grantha.shared.presentation.components.RasikaChip
import com.sangita.grantha.shared.presentation.components.RasikaIconAction
import com.sangita.grantha.shared.presentation.theme.RasikaTheme
import com.sangita.grantha.shared.presentation.theme.RasikaTokens
import com.sangita.grantha.shared.presentation.trinity
import org.jetbrains.compose.resources.painterResource
import kotlin.uuid.Uuid

private enum class ReaderTab { Lyrics, Details }

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun KrithiReaderScreen(
    krithiId: Uuid,
    presenter: KrithiReaderPresenter,
    favourited: Boolean,
    onToggleFavourite: (label: String) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val state by presenter.state.collectAsState()
    LaunchedEffect(krithiId) { presenter.open(krithiId) }
    var tab by remember { mutableStateOf(ReaderTab.Lyrics) }

    Column(modifier.fillMaxSize().background(MaterialTheme.colorScheme.surface)) {
        ReaderHeader(
            title = state.reader?.title ?: RasikaCopy.APP_NAME,
            meta = state.reader?.let { readerMeta(it.composer.name, it.ragas.sortedBy { r -> r.orderIndex }.joinToString(" · ") { r -> r.name }) }.orEmpty(),
            favourited = favourited,
            onBack = onBack,
            onToggleFavourite = { onToggleFavourite(state.reader?.title ?: "") },
        )
        PaintedCornice()
        ReaderTabStrip(tab = tab, onSelect = { tab = it })

        LoadStateContent(
            state = state.load,
            emptyTitle = RasikaCopy.UNAVAILABLE,
            emptyBody = RasikaCopy.EMPTY_SEARCH_BODY,
            onRetry = presenter::retry,
            modifier = Modifier.fillMaxSize(),
        ) {
            val reader = state.reader ?: return@LoadStateContent
            when (tab) {
                ReaderTab.Lyrics -> LyricsPane(state, presenter)
                ReaderTab.Details -> DetailsPane(reader)
            }
        }
    }
}

@Composable
private fun ReaderHeader(
    title: String,
    meta: String,
    favourited: Boolean,
    onBack: () -> Unit,
    onToggleFavourite: () -> Unit,
) {
    Box(
        Modifier
            .fillMaxWidth()
            .height(206.dp)
            .background(MaterialTheme.colorScheme.error),
    ) {
        Image(
            painter = painterResource(Res.drawable.trinity),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop,
            alignment = BiasAlignment(-0.28f, -0.48f),
        )
        Box(
            Modifier.fillMaxSize().background(
                Brush.verticalGradient(
                    0.0f to Color(0xFF781908).copy(alpha = 0.66f),
                    0.45f to Color(0xFF601406).copy(alpha = 0.40f),
                    1.0f to Color(0xFF601406).copy(alpha = 0.93f),
                ),
            ),
        )
        Row(
            Modifier.fillMaxWidth().padding(horizontal = RasikaTokens.xs, vertical = RasikaTokens.xs),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            RasikaIconAction(
                icon = Icons.AutoMirrored.Outlined.ArrowBack,
                label = RasikaCopy.BACK,
                onClick = onBack,
                tint = RasikaTokens.onDark,
            )
            RasikaIconAction(
                icon = if (favourited) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                label = if (favourited) RasikaCopy.REMOVE_FAVOURITE else RasikaCopy.ADD_FAVOURITE,
                onClick = onToggleFavourite,
                tint = RasikaTokens.onDark,
            )
        }
        Column(
            Modifier.align(Alignment.BottomStart).padding(horizontal = RasikaTokens.lg, vertical = RasikaTokens.md),
        ) {
            Text(
                title,
                style = MaterialTheme.typography.displayLarge,
                color = RasikaTokens.onDark,
            )
            if (meta.isNotBlank()) {
                Text(
                    meta,
                    style = MaterialTheme.typography.labelMedium,
                    color = RasikaTokens.onDarkSoft,
                    modifier = Modifier.padding(top = RasikaTokens.xs),
                )
            }
        }
    }
}

@Composable
private fun ReaderTabStrip(tab: ReaderTab, onSelect: (ReaderTab) -> Unit) {
    Row(Modifier.fillMaxWidth().background(RasikaTheme.colors.creamDeep)) {
        ReaderTabCell(RasikaCopy.LYRICS.uppercase(), tab == ReaderTab.Lyrics, Modifier.weight(1f)) { onSelect(ReaderTab.Lyrics) }
        ReaderTabCell(RasikaCopy.DETAILS.uppercase(), tab == ReaderTab.Details, Modifier.weight(1f)) { onSelect(ReaderTab.Details) }
    }
}

@Composable
private fun ReaderTabCell(label: String, selected: Boolean, modifier: Modifier, onClick: () -> Unit) {
    Box(
        modifier.clickable(onClick = onClick).padding(vertical = RasikaTokens.sm),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            label,
            style = MaterialTheme.typography.labelLarge,
            color = if (selected) MaterialTheme.colorScheme.error else RasikaTheme.colors.inkMuted,
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun LyricsPane(state: ReaderUiState, presenter: KrithiReaderPresenter) {
    val reader = state.reader ?: return
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = RasikaTokens.screen, vertical = RasikaTokens.md),
    ) {
        if (reader.variants.size > 1) {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(RasikaTokens.xs)) {
                reader.variants.forEach { variant ->
                    val label = variant.label ?: scriptLabel(variant.script.name)
                    RasikaChip(
                        selected = variant.id == state.selectedVariantId,
                        onClick = { presenter.selectVariant(variant.id) },
                        label = label,
                    )
                }
            }
        }
        // R5 — variant identity line: names the selected reading and whether primary.
        presenter.selectedVariant()?.let { v ->
            val identity = buildString {
                append(if (v.isPrimary) "Primary reading" else "Secondary reading")
                append(" · ")
                append(scriptLabel(v.script.name))
                append(" · ")
                append(v.language.name.lowercase().replaceFirstChar { it.uppercase() })
            }
            Text(
                identity,
                style = MaterialTheme.typography.labelMedium,
                color = RasikaTheme.colors.inkMuted,
                modifier = Modifier.padding(top = RasikaTokens.sm),
            )
        }

        val lyrics = state.lyrics
        if (lyrics != null) {
            val unsegmented = lyrics.unsegmentedText
            if (lyrics.sections.isNotEmpty()) {
                lyrics.sections.sortedBy { it.orderIndex }.forEach { section ->
                    SectionHeading(section.label ?: section.sectionType)
                    Text(
                        section.text,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(top = RasikaTokens.sm),
                    )
                }
            } else if (!unsegmented.isNullOrBlank()) {
                Text(
                    unsegmented,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(top = RasikaTokens.md),
                )
            }
        }
    }
}

@Composable
private fun SectionHeading(label: String) {
    Row(
        Modifier.fillMaxWidth().padding(top = RasikaTokens.lg),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(RasikaTokens.sm),
    ) {
        Text(
            label.uppercase(),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.error,
        )
        // Dashed gold anga rule.
        Box(
            Modifier.weight(1f).height(2.dp).background(RasikaTheme.colors.goldLine.copy(alpha = 0.6f)),
        )
    }
}

@Composable
private fun DetailsPane(reader: com.sangita.grantha.shared.domain.model.catalogue.CatalogueKrithiReaderDto) {
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = RasikaTokens.screen, vertical = RasikaTokens.xs),
    ) {
        val ragaLine = reader.ragas.sortedBy { it.orderIndex }.joinToString(" · ") { it.name }
        DetailRow("RĀGA", ragaLine, if (reader.isRagamalika) RasikaCopy.RAGAMALIKA else null)
        reader.tala?.let { DetailRow("TĀLA", it.name, null) }
        DetailRow("FORM", reader.musicalForm.displayName(), null)
        DetailRow("LANGUAGE", reader.originalLanguage.name.lowercase().replaceFirstChar { it.uppercase() }, null)

        if (reader.isRagamalika && reader.ragas.size > 1) {
            Text(
                RasikaCopy.MEMBERSHIP.uppercase(),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(top = RasikaTokens.lg, bottom = RasikaTokens.xs),
            )
            reader.ragas.sortedBy { it.orderIndex }.forEachIndexed { index, r ->
                Text(
                    "${index + 1}.  ${r.name}",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(vertical = 2.dp),
                )
            }
        }

        CompletenessNote(reader.completeness)
    }
}

@Composable
private fun DetailRow(key: String, value: String, sub: String?) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(vertical = RasikaTokens.sm),
    ) {
        Text(
            key,
            style = MaterialTheme.typography.labelSmall,
            color = RasikaTheme.colors.inkMuted,
            modifier = Modifier.width(96.dp).padding(top = 4.dp),
        )
        Column(Modifier.weight(1f)) {
            Text(value, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
            if (sub != null) {
                Text(sub, style = MaterialTheme.typography.labelMedium, color = RasikaTheme.colors.inkMuted)
            }
        }
    }
    androidx.compose.material3.HorizontalDivider(thickness = 1.dp, color = RasikaTheme.colors.hairline)
}

@Composable
private fun CompletenessNote(completeness: CatalogueCompletenessDto) {
    val note = when (completeness) {
        CatalogueCompletenessDto.COMPLETE ->
            "This reading is stored as complete for the selected script."
        CatalogueCompletenessDto.PARTIAL ->
            "Some sections are not present in the stored source reading, so completeness is partial."
        CatalogueCompletenessDto.UNKNOWN ->
            RasikaCopy.COMPLETENESS_FALLBACK
    }
    Row(
        Modifier
            .fillMaxWidth()
            .padding(top = RasikaTokens.md)
            .background(RasikaTheme.colors.creamDeep, androidx.compose.foundation.shape.RoundedCornerShape(RasikaTokens.xs))
            .padding(RasikaTokens.sm),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(RasikaTokens.xs),
    ) {
        Icon(
            Icons.Outlined.Info,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.error,
            modifier = Modifier.width(18.dp).height(18.dp),
        )
        Text(note, style = MaterialTheme.typography.bodyLarge, color = RasikaTheme.colors.inkMuted)
    }
}

private fun readerMeta(composer: String, ragaLine: String): String = buildString {
    append(composer)
    if (ragaLine.isNotBlank()) {
        append("   ·   ")
        append(ragaLine)
    }
}

private fun scriptLabel(raw: String): String =
    raw.lowercase().replaceFirstChar { it.uppercase() }

private fun MusicalFormDto.displayName(): String =
    name.lowercase().replaceFirstChar { it.uppercase() }
