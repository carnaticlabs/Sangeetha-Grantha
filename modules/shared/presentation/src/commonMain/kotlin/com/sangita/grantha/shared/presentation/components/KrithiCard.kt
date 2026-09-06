package com.sangita.grantha.shared.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.sangita.grantha.shared.domain.model.catalogue.CatalogueKrithiSummaryDto
import com.sangita.grantha.shared.presentation.RasikaCopy
import com.sangita.grantha.shared.presentation.theme.RasikaTheme
import com.sangita.grantha.shared.presentation.theme.RasikaTokens

/**
 * Result / list row (visual-design §5). A nāsika-topped glyph tile, the Fraunces title
 * with a right-aligned rāga label, a composer · tāla · language sub-line, an optional
 * matched-sahitya snippet, and a saffron heart when the krithi is saved.
 */
@Composable
fun KrithiCard(
    summary: CatalogueKrithiSummaryDto,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    favourited: Boolean = false,
    snippet: String? = null,
    onToggleFavourite: (() -> Unit)? = null,
) {
    val colors = RasikaTheme.colors
    val ragas = summary.ragas.sortedBy { it.orderIndex }
    val ragaShort = ragas.firstOrNull()?.name?.uppercase()
    val sub = buildString {
        append(summary.composer.name)
        summary.tala?.let { append("  ·  "); append(it.name) }
    }
    RasikaPressable(onClick = onClick, modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(RasikaTokens.sm),
            horizontalArrangement = Arrangement.spacedBy(RasikaTokens.sm),
            verticalAlignment = Alignment.Top,
        ) {
            GlyphTile(summary.title.trim().take(1).uppercase())
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.Top) {
                    Text(
                        summary.title,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f),
                    )
                    if (ragaShort != null) {
                        Text(
                            ragaShort,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.padding(start = RasikaTokens.xs, top = 2.dp),
                        )
                    }
                }
                Text(
                    sub,
                    style = MaterialTheme.typography.labelMedium,
                    color = colors.inkMuted,
                    modifier = Modifier.padding(top = 3.dp),
                )
                if (summary.isRagamalika) {
                    Text(
                        RasikaCopy.RAGAMALIKA.uppercase(),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.padding(top = 3.dp),
                    )
                }
                if (!snippet.isNullOrBlank()) {
                    Text(
                        snippet,
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.inkMuted,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(top = RasikaTokens.xs),
                    )
                }
            }
            if (onToggleFavourite != null && favourited) {
                Icon(
                    Icons.Filled.Favorite,
                    contentDescription = RasikaCopy.REMOVE_FAVOURITE,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(16.dp).padding(top = 2.dp),
                )
            }
        }
    }
}

/** 36×40 nāsika-crowned tile carrying the title's first glyph. */
@Composable
private fun GlyphTile(glyph: String) {
    val colors = RasikaTheme.colors
    Box(
        modifier = Modifier
            .size(width = 36.dp, height = 40.dp)
            .background(colors.creamDeep, RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp, bottomStart = 4.dp, bottomEnd = 4.dp))
            .border(
                RasikaTokens.hairlineWidth,
                MaterialTheme.colorScheme.primary.copy(alpha = 0.25f),
                RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp, bottomStart = 4.dp, bottomEnd = 4.dp),
            ),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            glyph,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.error,
        )
    }
}
