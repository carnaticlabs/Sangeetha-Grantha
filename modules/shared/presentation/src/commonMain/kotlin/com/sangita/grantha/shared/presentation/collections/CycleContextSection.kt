package com.sangita.grantha.shared.presentation.collections

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import com.sangita.grantha.shared.domain.model.catalogue.CatalogueCollectionNeighbourDto
import com.sangita.grantha.shared.domain.model.catalogue.CatalogueCollectionRefDto
import com.sangita.grantha.shared.presentation.components.RasikaPressable
import com.sangita.grantha.shared.presentation.theme.RasikaTheme
import com.sangita.grantha.shared.presentation.theme.RasikaTokens
import kotlin.uuid.Uuid

/** "Part of" cards for the reader's Details tab: collection, position, previous and next. */
@Composable
fun CycleContextSection(
    collections: List<CatalogueCollectionRefDto>,
    onOpenCollection: (String) -> Unit,
    onOpenNeighbour: (Uuid) -> Unit,
    modifier: Modifier = Modifier,
) {
    collections.forEach { ref ->
        Column(modifier.fillMaxWidth().padding(vertical = RasikaTokens.sm), verticalArrangement = Arrangement.spacedBy(RasikaTokens.xs)) {
            Text(
                CollectionCopy.PART_OF,
                style = MaterialTheme.typography.labelSmall,
                color = RasikaTheme.colors.inkMuted,
            )
            RasikaPressable(
                onClick = { onOpenCollection(ref.slug) },
                modifier = Modifier.fillMaxWidth().semantics { contentDescription = CollectionCopy.chipLabel(ref) },
            ) {
                Column(Modifier.fillMaxWidth().padding(RasikaTokens.sm)) {
                    Text(ref.name, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
                    val line = listOfNotNull(
                        CollectionCopy.positionLabel(ref),
                        CollectionCopy.roleLabel(ref.role),
                        ref.axisValue,
                    ).joinToString(" · ")
                    if (line.isNotBlank()) {
                        Text(line, style = MaterialTheme.typography.labelMedium, color = RasikaTheme.colors.inkMuted)
                    }
                    CollectionCopy.orderNote(ref.orderBasis, ref.orderConvention)
                        ?.takeIf { ref.orderBasis != com.sangita.grantha.shared.domain.model.catalogue.CatalogueOrderBasisDto.UNSPECIFIED }
                        ?.let { Text(it, style = MaterialTheme.typography.labelSmall, color = RasikaTheme.colors.inkMuted) }
                }
            }
            ref.previous?.let { NeighbourRow(CollectionCopy.PREVIOUS, it, onOpenNeighbour) }
            ref.next?.let { NeighbourRow(CollectionCopy.NEXT, it, onOpenNeighbour) }
        }
    }
}

@Composable
private fun NeighbourRow(direction: String, neighbour: CatalogueCollectionNeighbourDto, onOpen: (Uuid) -> Unit) {
    RasikaPressable(
        onClick = { onOpen(neighbour.krithiId) },
        modifier = Modifier.fillMaxWidth().semantics { contentDescription = "$direction: ${neighbour.title}" },
    ) {
        Column(Modifier.fillMaxWidth().padding(RasikaTokens.sm)) {
            Text(direction.uppercase(), style = MaterialTheme.typography.labelSmall, color = RasikaTheme.colors.inkMuted)
            Text(neighbour.title, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurface)
            neighbour.ragaName?.let {
                Text(it, style = MaterialTheme.typography.labelMedium, color = RasikaTheme.colors.inkMuted)
            }
        }
    }
}
