package com.sangita.grantha.shared.presentation.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import com.sangita.grantha.shared.domain.model.catalogue.CatalogueRagaRefDto
import com.sangita.grantha.shared.presentation.RasikaCopy
import com.sangita.grantha.shared.presentation.theme.RasikaTokens

/**
 * Ordered raga membership. Never collapses to the first raga alone (M9).
 * "+n more" is an expansion control, not a raga name.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun RagaSequence(
    ragas: List<CatalogueRagaRefDto>,
    modifier: Modifier = Modifier,
    compactLimit: Int = 2,
) {
    val ordered = ragas.sortedBy { it.orderIndex }
    if (ordered.isEmpty()) return
    var expanded by remember(ordered.map { it.id to it.orderIndex }) { mutableStateOf(false) }
    val extra = (ordered.size - compactLimit).coerceAtLeast(0)
    val shown = if (expanded || extra == 0) ordered else ordered.take(compactLimit)
    FlowRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(RasikaTokens.xs),
        verticalArrangement = Arrangement.spacedBy(RasikaTokens.xxs),
    ) {
        shown.forEach { raga ->
            Text(
                raga.name,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.secondary,
            )
        }
        if (extra > 0 && !expanded) {
            val label = RasikaCopy.moreRagas(extra)
            Text(
                label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .clickable { expanded = true }
                    .semantics { contentDescription = label },
            )
        }
    }
}
