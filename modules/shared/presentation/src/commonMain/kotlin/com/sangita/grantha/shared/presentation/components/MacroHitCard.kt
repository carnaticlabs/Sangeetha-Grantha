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
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import com.sangita.grantha.shared.domain.model.catalogue.CatalogueMacroHitDto
import com.sangita.grantha.shared.domain.model.catalogue.CatalogueSearchHitKindDto
import com.sangita.grantha.shared.presentation.RasikaCopy
import com.sangita.grantha.shared.presentation.theme.RasikaTheme
import com.sangita.grantha.shared.presentation.theme.RasikaTokens

/**
 * A collection or kshetra search card (TRACK-152). The kind is stated as text, never by colour alone,
 * and a recorded-gap kshetra says so.
 */
@Composable
fun MacroHitCard(
    hit: CatalogueMacroHitDto,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = RasikaTheme.colors
    val kind = macroKindLabel(hit.kind)
    val description = RasikaCopy.macroCardDescription(kind, hit.title, hit.subtitle, hit.recordedGap)
    RasikaPressable(
        onClick = onClick,
        modifier = modifier.fillMaxWidth().semantics { contentDescription = description },
    ) {
        Column(
            Modifier.padding(RasikaTokens.sm),
            verticalArrangement = Arrangement.spacedBy(RasikaTokens.xxs),
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(RasikaTokens.xs), verticalAlignment = Alignment.CenterVertically) {
                Text(kind, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary)
                if (hit.recordedGap) {
                    Text(RasikaCopy.RECORDED_GAP, style = MaterialTheme.typography.labelSmall, color = colors.inkMuted)
                }
            }
            Text(hit.title, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
            hit.subtitle?.takeIf { it.isNotBlank() }?.let {
                Text(it, style = MaterialTheme.typography.labelMedium, color = colors.inkMuted)
            }
            if (hit.snippet.isNotBlank()) {
                Text(
                    hit.snippet,
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.inkMuted,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = RasikaTokens.xs),
                )
            }
        }
    }
}

internal fun macroKindLabel(kind: CatalogueSearchHitKindDto): String = when (kind) {
    CatalogueSearchHitKindDto.COLLECTION -> RasikaCopy.KIND_COLLECTION
    CatalogueSearchHitKindDto.KSHETRA -> RasikaCopy.KIND_KSHETRA
    CatalogueSearchHitKindDto.KRITHI -> ""
}
