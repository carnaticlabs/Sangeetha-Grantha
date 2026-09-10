package com.sangita.grantha.shared.presentation.reader

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.sangita.grantha.shared.domain.model.ScriptCodeDto
import com.sangita.grantha.shared.domain.model.catalogue.CatalogueCompletenessDto
import com.sangita.grantha.shared.domain.model.catalogue.CatalogueKrithiReaderDto
import com.sangita.grantha.shared.domain.model.catalogue.CatalogueLyricSectionDto
import com.sangita.grantha.shared.domain.model.catalogue.CatalogueVariantRefDto
import com.sangita.grantha.shared.presentation.RasikaCopy
import com.sangita.grantha.shared.presentation.components.LoadState
import com.sangita.grantha.shared.presentation.components.LoadStateContent
import com.sangita.grantha.shared.presentation.components.RagaSequence
import com.sangita.grantha.shared.presentation.components.RasikaChip
import com.sangita.grantha.shared.presentation.components.RasikaFavouriteButton
import com.sangita.grantha.shared.presentation.components.RasikaPrimaryButton
import com.sangita.grantha.shared.presentation.components.RasikaScreenHeader
import com.sangita.grantha.shared.presentation.theme.RasikaTheme
import com.sangita.grantha.shared.presentation.theme.RasikaTokens
import kotlinx.coroutines.launch
import kotlin.uuid.Uuid

private enum class ReaderTab { Lyrics, Details }

@OptIn(ExperimentalLayoutApi::class, ExperimentalFoundationApi::class)
@Composable
fun KrithiReaderScreen(
    krithiId: Uuid,
    presenter: KrithiReaderPresenter,
    favourited: Boolean,
    onToggleFavourite: (label: String) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val state by presenter.state.collectAsStateWithLifecycle()
    LaunchedEffect(krithiId) { presenter.open(krithiId) }
    var tab by remember { mutableStateOf(ReaderTab.Lyrics) }

    Column(modifier.fillMaxSize().background(MaterialTheme.colorScheme.surface)) {
        RasikaScreenHeader(
            title = state.reader?.title ?: RasikaCopy.APP_NAME,
            subtitle = state.reader?.composer?.name,
            onBack = onBack,
            action = {
                RasikaFavouriteButton(
                    favourited = favourited,
                    onClick = { onToggleFavourite(state.reader?.title ?: "") },
                )
            },
        )
        state.reader?.let { reader ->
            Column(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = RasikaTokens.screen, vertical = RasikaTokens.xs),
                verticalArrangement = Arrangement.spacedBy(RasikaTokens.xs),
            ) {
                RagaSequence(reader.ragas)
                RasikaCopy.musicalFormLabel(reader.musicalForm)?.let { form ->
                    Text(form, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.secondary)
                }
            }
        }
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
                ReaderTab.Lyrics -> LyricsPane(state, presenter, reader)
                ReaderTab.Details -> DetailsPane(reader, state)
            }
        }
    }
}

@Composable
private fun ReaderTabStrip(tab: ReaderTab, onSelect: (ReaderTab) -> Unit) {
    Row(Modifier.fillMaxWidth().background(RasikaTheme.colors.creamDeep)) {
        ReaderTabCell(RasikaCopy.LYRICS.uppercase(), tab == ReaderTab.Lyrics, Modifier.weight(1f)) {
            onSelect(ReaderTab.Lyrics)
        }
        ReaderTabCell(RasikaCopy.DETAILS.uppercase(), tab == ReaderTab.Details, Modifier.weight(1f)) {
            onSelect(ReaderTab.Details)
        }
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

@OptIn(ExperimentalLayoutApi::class, ExperimentalFoundationApi::class)
@Composable
private fun LyricsPane(
    state: ReaderUiState,
    presenter: KrithiReaderPresenter,
    reader: CatalogueKrithiReaderDto,
) {
    val selected = presenter.selectedVariant()
    val jumpScope = rememberCoroutineScope()
    val sections = state.lyrics?.sections.orEmpty().sortedBy { it.orderIndex }
    val requesters = remember(sections.map { it.sectionId }) {
        sections.associate { it.sectionId to BringIntoViewRequester() }
    }
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = RasikaTokens.screen, vertical = RasikaTokens.md),
    ) {
        if (reader.variants.isNotEmpty()) {
            val scripts = reader.variants.map { it.script }.distinct()
            if (scripts.size > 1) {
                Text(
                    RasikaCopy.SCRIPT_CHOICE,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                )
                FlowRow(
                    modifier = Modifier.padding(top = RasikaTokens.xs, bottom = RasikaTokens.sm),
                    horizontalArrangement = Arrangement.spacedBy(RasikaTokens.xs),
                    verticalArrangement = Arrangement.spacedBy(RasikaTokens.xs),
                ) {
                    scripts.forEach { script ->
                        RasikaChip(
                            selected = selected?.script == script,
                            onClick = {
                                val target = reader.variants
                                    .filter { it.script == script }
                                    .minByOrNull { it.id.toString() }
                                if (target != null) presenter.selectVariant(target.id)
                            },
                            label = scriptLabel(script),
                        )
                    }
                }
            }
            val readings = if (selected != null) {
                reader.variants.filter { it.script == selected.script }
            } else {
                reader.variants
            }
            if (readings.size > 1 || scripts.size == 1) {
                Text(
                    RasikaCopy.SOURCE_CHOICE,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                )
                FlowRow(
                    modifier = Modifier.padding(top = RasikaTokens.xs),
                    horizontalArrangement = Arrangement.spacedBy(RasikaTokens.xs),
                    verticalArrangement = Arrangement.spacedBy(RasikaTokens.xs),
                ) {
                    readings.forEach { variant ->
                        RasikaChip(
                            selected = variant.id == state.selectedVariantId,
                            onClick = { presenter.selectVariant(variant.id) },
                            label = sourceLabel(variant),
                        )
                    }
                }
            }
        }

        selected?.let { variant ->
            Text(
                readingIdentity(variant),
                style = MaterialTheme.typography.labelMedium,
                color = RasikaTheme.colors.inkMuted,
                modifier = Modifier.padding(top = RasikaTokens.sm),
            )
        }

        when (val lyricsLoad = state.lyricsLoad) {
            LoadState.Loading -> Text(
                RasikaCopy.LOADING_READING,
                style = MaterialTheme.typography.bodyLarge,
                color = RasikaTheme.colors.inkMuted,
                modifier = Modifier.padding(top = RasikaTokens.sm),
            )
            is LoadState.Error -> {
                Text(
                    RasikaCopy.READING_FAILED,
                    style = MaterialTheme.typography.bodyLarge,
                    color = RasikaTheme.colors.inkMuted,
                    modifier = Modifier.padding(top = RasikaTokens.sm),
                )
                if (lyricsLoad.retryable) {
                    RasikaPrimaryButton(
                        label = RasikaCopy.RETRY,
                        onClick = presenter::retry,
                        modifier = Modifier.padding(top = RasikaTokens.xs),
                    )
                }
            }
            else -> Unit
        }

        if (sections.size > 1) {
            Text(
                RasikaCopy.JUMP_TO_SECTION,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(top = RasikaTokens.md),
            )
            FlowRow(
                modifier = Modifier.padding(top = RasikaTokens.xs),
                horizontalArrangement = Arrangement.spacedBy(RasikaTokens.xs),
                verticalArrangement = Arrangement.spacedBy(RasikaTokens.xs),
            ) {
                sections.forEach { section ->
                    RasikaChip(
                        selected = false,
                        onClick = {
                            jumpScope.launch { requesters[section.sectionId]?.bringIntoView() }
                        },
                        label = SectionLabels.heading(section.sectionType, section.label),
                    )
                }
            }
        }

        val lyrics = state.lyrics
        if (lyrics != null) {
            val unsegmented = lyrics.unsegmentedText
            if (sections.isNotEmpty()) {
                sections.forEach { section ->
                    key(section.sectionId) {
                        val requester = requesters[section.sectionId]
                        Column(
                            Modifier
                                .fillMaxWidth()
                                .then(if (requester != null) Modifier.bringIntoViewRequester(requester) else Modifier),
                        ) {
                            LyricSection(
                                section = section,
                                musicalForm = reader.musicalForm,
                                ragaLabel = SectionLabels.ragaNamesForSection(reader.ragas, section.sectionId),
                            )
                        }
                    }
                }
            } else if (!unsegmented.isNullOrBlank()) {
                SelectionContainer {
                    Text(
                        unsegmented,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(top = RasikaTokens.md),
                    )
                }
            }
            ReadingCompleteness(lyrics.completeness)
        }
    }
}

@Composable
private fun LyricSection(
    section: CatalogueLyricSectionDto,
    musicalForm: com.sangita.grantha.shared.domain.model.MusicalFormDto,
    ragaLabel: String?,
) {
    Row(
        Modifier.fillMaxWidth().padding(top = RasikaTokens.lg),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(RasikaTokens.sm),
    ) {
        Text(
            SectionLabels.heading(section.sectionType, section.label),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.error,
        )
        Box(Modifier.weight(1f).height(2.dp).background(RasikaTheme.colors.goldLine.copy(alpha = 0.6f)))
    }
    SectionLabels.caption(section.sectionType, musicalForm)?.let { caption ->
        Text(
            caption,
            style = MaterialTheme.typography.labelMedium,
            color = RasikaTheme.colors.inkMuted,
            modifier = Modifier.padding(top = RasikaTokens.xxs),
        )
    }
    if (!ragaLabel.isNullOrBlank()) {
        Text(
            ragaLabel,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.secondary,
            modifier = Modifier.padding(top = RasikaTokens.xxs),
        )
    }
    SelectionContainer {
        if (SectionLabels.isPreformattedSwara(section.sectionType)) {
            Text(
                section.text,
                style = MaterialTheme.typography.bodyMedium,
                softWrap = false,
                modifier = Modifier
                    .padding(top = RasikaTokens.sm)
                    .horizontalScroll(rememberScrollState()),
            )
        } else {
            Text(
                section.text,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(top = RasikaTokens.sm),
            )
        }
    }
}

@Composable
private fun DetailsPane(reader: CatalogueKrithiReaderDto, state: ReaderUiState) {
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = RasikaTokens.screen, vertical = RasikaTokens.xs),
    ) {
        DetailRow("RĀGA", reader.ragas.sortedBy { it.orderIndex }.joinToString(" · ") { it.name }, if (reader.isRagamalika) RasikaCopy.RAGAMALIKA else null)
        reader.tala?.let { DetailRow("TĀLA", it.name, null) }
        RasikaCopy.musicalFormLabel(reader.musicalForm)?.let { form ->
            DetailRow("FORM", form, null)
        }
        DetailRow(
            "LANGUAGE",
            reader.originalLanguage.name.lowercase().replaceFirstChar { it.uppercase() },
            null,
        )
        presenterSelectedSource(state)?.let { source ->
            DetailRow("SOURCE", source, null)
        }

        if (reader.isRagamalika && reader.ragas.isNotEmpty()) {
            Text(
                RasikaCopy.MEMBERSHIP.uppercase(),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(top = RasikaTokens.lg, bottom = RasikaTokens.xs),
            )
            RagaSequence(reader.ragas, compactLimit = reader.ragas.size)
        }

        state.lyrics?.let { ReadingCompleteness(it.completeness) }
            ?: Text(
                RasikaCopy.COMPLETENESS_NOT_ESTABLISHED,
                style = MaterialTheme.typography.bodyLarge,
                color = RasikaTheme.colors.inkMuted,
                modifier = Modifier.padding(top = RasikaTokens.md),
            )
    }
}

private fun presenterSelectedSource(state: ReaderUiState): String? {
    val id = state.selectedVariantId ?: return null
    val variant = state.reader?.variants?.find { it.id == id } ?: return null
    return sourceLabel(variant)
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
private fun ReadingCompleteness(completeness: CatalogueCompletenessDto) {
    val note = when (completeness) {
        CatalogueCompletenessDto.COMPLETE -> RasikaCopy.COMPLETE_READING
        CatalogueCompletenessDto.PARTIAL -> RasikaCopy.PARTIAL_READING
        CatalogueCompletenessDto.UNKNOWN -> RasikaCopy.COMPLETENESS_NOT_ESTABLISHED
    }
    if (completeness == CatalogueCompletenessDto.UNKNOWN) return
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

private fun sourceLabel(variant: CatalogueVariantRefDto): String =
    variant.label?.takeIf { it.isNotBlank() }
        ?: variant.sourceReference?.takeIf { it.isNotBlank() }
        ?: scriptLabel(variant.script)

private fun readingIdentity(variant: CatalogueVariantRefDto): String = buildString {
    append(scriptLabel(variant.script))
    variant.sourceReference?.takeIf { it.isNotBlank() }?.let {
        append(" · ")
        append(it)
    }
}

private fun scriptLabel(script: ScriptCodeDto): String =
    script.name.lowercase().replaceFirstChar { it.uppercase() }
