package com.sangita.grantha.shared.presentation.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.sangita.grantha.shared.domain.model.SemanticSearchResultItem
import com.sangita.grantha.shared.presentation.search.KrithiSearchMode
import com.sangita.grantha.shared.presentation.theme.RasikaTheme
import com.sangita.grantha.shared.presentation.theme.RasikaTokens
import kotlin.math.abs
import kotlin.math.floor

/**
 * Hybrid or Semantic result. It does not adapt [com.sangita.grantha.shared.domain.model.catalogue.CatalogueKrithiSummaryDto]:
 * the discovery payload has a primary raga name and indexed snippet, not the catalogue raga sequence.
 */
@Composable
fun DiscoveryKrithiCard(
    item: SemanticSearchResultItem,
    mode: KrithiSearchMode,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    favourited: Boolean = false,
    onToggleFavourite: (() -> Unit)? = null,
) {
    val colors = RasikaTheme.colors
    val rank = when (mode) {
        KrithiSearchMode.Hybrid -> hybridRankLabel(item.rrfScore)
        KrithiSearchMode.Semantic -> semanticRankLabel(item.similarityScore)
        KrithiSearchMode.Lexical -> null
    }
    val sub = buildString {
        append(item.composerName)
        item.talaName?.let { name ->
            append("  ·  ")
            append(name)
        }
    }
    RasikaPressable(onClick = onClick, modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(RasikaTokens.sm),
            horizontalArrangement = Arrangement.spacedBy(RasikaTokens.sm),
            verticalAlignment = Alignment.Top,
        ) {
            GlyphTile(item.title.trim().take(1).uppercase())
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.Top) {
                    Text(
                        item.title,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f),
                    )
                    if (rank != null) {
                        Text(
                            rank,
                            style = MaterialTheme.typography.labelSmall,
                            color = colors.inkMuted,
                            modifier = Modifier.padding(start = RasikaTokens.xs),
                        )
                    }
                }
                item.ragaName?.let { name ->
                    Text(
                        name,
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.padding(top = 3.dp),
                    )
                }
                Text(
                    sub,
                    style = MaterialTheme.typography.labelMedium,
                    color = colors.inkMuted,
                    modifier = Modifier.padding(top = 3.dp),
                )
                if (item.matchedContent.isNotBlank()) {
                    Text(
                        item.matchedContent,
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.inkMuted,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(top = RasikaTokens.xs),
                    )
                }
            }
            if (onToggleFavourite != null) {
                RasikaFavouriteButton(
                    favourited = favourited,
                    onClick = onToggleFavourite,
                )
            }
        }
    }
}

/** Unlabeled hybrid reciprocal-rank score, four digits after the decimal. Null draws nothing. */
internal fun hybridRankLabel(rrfScore: Double?): String? {
    rrfScore ?: return null
    if (rrfScore.isNaN()) return null
    return formatFourDecimals(rrfScore)
}

/** Nearest integer percent of cosine similarity, half up, with a % suffix and no other word. */
internal fun semanticRankLabel(similarityScore: Double): String {
    if (similarityScore.isNaN()) return "0%"
    val percent = halfUpToInt(similarityScore * 100.0)
    return "$percent%"
}

private fun formatFourDecimals(value: Double): String {
    val negative = value < 0.0
    val scaled = halfUpToLong(abs(value) * 10_000.0)
    val whole = scaled / 10_000
    val fraction = (scaled % 10_000).toString().padStart(4, '0')
    return buildString {
        if (negative && scaled != 0L) append('-')
        append(whole)
        append('.')
        append(fraction)
    }
}

private fun halfUpToInt(value: Double): Int = halfUpToLong(value).toInt()

/** Ties at .5 round away from zero for the magnitude, which is half up for non-negative scores. */
private fun halfUpToLong(value: Double): Long {
    if (value.isNaN()) return 0L
    val negative = value < 0.0
    val magnitude = abs(value)
    val whole = floor(magnitude)
    val fraction = magnitude - whole
    val rounded = if (fraction + 1e-9 >= 0.5) whole + 1.0 else whole
    val result = rounded.toLong()
    return if (negative) -result else result
}
