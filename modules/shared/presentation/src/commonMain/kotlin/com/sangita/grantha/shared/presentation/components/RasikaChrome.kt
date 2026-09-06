package com.sangita.grantha.shared.presentation.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.sangita.grantha.shared.presentation.RasikaCopy
import com.sangita.grantha.shared.presentation.navigation.RasikaTab
import com.sangita.grantha.shared.presentation.theme.RasikaMotion
import com.sangita.grantha.shared.presentation.theme.RasikaTheme
import com.sangita.grantha.shared.presentation.theme.RasikaTokens

/** Thin gold rule used inside cards and between rows. */
@Composable
fun RasikaHairline(modifier: Modifier = Modifier) {
    androidx.compose.material3.HorizontalDivider(
        modifier = modifier,
        thickness = RasikaTokens.hairlineWidth,
        color = RasikaTheme.colors.hairline,
    )
}

/**
 * Screen header: a small tracked section label / Fraunces title row, closed by the
 * [PaintedCornice] band so every screen reads as a painted temple interior.
 */
@Composable
fun RasikaScreenHeader(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    label: String? = null,
    onBack: (() -> Unit)? = null,
    onPreferences: (() -> Unit)? = null,
    action: @Composable (() -> Unit)? = null,
) {
    Column(modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surface)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    start = if (onBack != null) RasikaTokens.xs else RasikaTokens.screen,
                    end = RasikaTokens.xs,
                    top = RasikaTokens.xs,
                    bottom = RasikaTokens.sm,
                ),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (onBack != null) {
                RasikaIconAction(
                    icon = Icons.AutoMirrored.Outlined.ArrowBack,
                    label = RasikaCopy.BACK,
                    onClick = onBack,
                    tint = MaterialTheme.colorScheme.primary,
                )
            }
            Column(Modifier.weight(1f).padding(end = RasikaTokens.xs)) {
                if (!label.isNullOrBlank()) {
                    Text(
                        label,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
                Text(title, style = MaterialTheme.typography.titleLarge)
                if (!subtitle.isNullOrBlank()) {
                    Text(
                        subtitle,
                        style = MaterialTheme.typography.labelMedium,
                        color = RasikaTheme.colors.inkMuted,
                    )
                }
            }
            action?.invoke()
            if (onPreferences != null) {
                RasikaIconAction(
                    icon = Icons.Outlined.Settings,
                    label = RasikaCopy.PREFERENCES,
                    onClick = onPreferences,
                    tint = MaterialTheme.colorScheme.secondary,
                )
            }
        }
        PaintedCornice()
    }
}

@Composable
fun RasikaIconAction(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    tint: Color = MaterialTheme.colorScheme.onSurface,
) {
    Box(
        modifier = modifier
            .size(RasikaTokens.tapTarget)
            .clickable(onClick = onClick)
            .semantics { contentDescription = label },
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = label, tint = tint)
    }
}

/** Favourite affordance — a heart, filled saffron when saved (visual-design §5). */
@Composable
fun RasikaFavouriteButton(
    favourited: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val label = if (favourited) RasikaCopy.REMOVE_FAVOURITE else RasikaCopy.ADD_FAVOURITE
    RasikaIconAction(
        icon = if (favourited) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
        label = label,
        onClick = onClick,
        modifier = modifier,
        tint = if (favourited) MaterialTheme.colorScheme.primary else RasikaTheme.colors.inkMuted,
    )
}

@Composable
fun RasikaSearchField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    onSearch: () -> Unit,
    modifier: Modifier = Modifier,
    contentDescription: String = placeholder,
) {
    val colors = RasikaTheme.colors
    val scheme = MaterialTheme.colorScheme
    val shape = RoundedCornerShape(RasikaTokens.fieldRadius)
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = RasikaTokens.tapTarget)
            .semantics { this.contentDescription = contentDescription },
        singleLine = true,
        cursorBrush = SolidColor(scheme.primary),
        textStyle = MaterialTheme.typography.bodyLarge.copy(color = scheme.onSurface),
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        keyboardActions = KeyboardActions(onSearch = { onSearch() }),
        decorationBox = { inner ->
            Row(
                Modifier
                    .fillMaxWidth()
                    .heightIn(min = RasikaTokens.tapTarget)
                    .background(colors.card, shape)
                    .border(RasikaTokens.hairlineWidth, colors.hairline, shape)
                    .padding(horizontal = RasikaTokens.md),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    Icons.Outlined.Search,
                    contentDescription = null,
                    tint = scheme.primary,
                    modifier = Modifier.size(18.dp),
                )
                Box(Modifier.weight(1f).padding(start = RasikaTokens.sm)) {
                    if (value.isEmpty()) {
                        Text(placeholder, style = MaterialTheme.typography.bodyLarge, color = colors.inkMuted)
                    }
                    inner()
                }
            }
        },
    )
}

@Composable
fun RasikaPrimaryButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val scheme = MaterialTheme.colorScheme
    Surface(
        onClick = onClick,
        modifier = modifier
            .heightIn(min = RasikaTokens.tapTarget)
            .semantics { contentDescription = label },
        shape = RoundedCornerShape(RasikaTokens.fieldRadius),
        color = scheme.primary,
        contentColor = scheme.onPrimary,
        shadowElevation = 0.dp,
        tonalElevation = 0.dp,
    ) {
        Box(
            Modifier.padding(horizontal = RasikaTokens.lg),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                label,
                style = MaterialTheme.typography.labelLarge,
                color = scheme.onPrimary,
            )
        }
    }
}

/**
 * Pill chip. Selected reads as a solid saffron pill with cream ink; unselected is a
 * white pill with a teal hairline and teal ink (visual-design filters / switchers).
 */
@Composable
fun RasikaChip(
    selected: Boolean,
    onClick: () -> Unit,
    label: String,
    modifier: Modifier = Modifier,
) {
    val colors = RasikaTheme.colors
    val scheme = MaterialTheme.colorScheme
    Surface(
        onClick = onClick,
        modifier = modifier
            .heightIn(min = 40.dp)
            .semantics { contentDescription = label },
        shape = RoundedCornerShape(RasikaTokens.fieldRadius),
        color = if (selected) scheme.primary else colors.card,
        border = if (selected) null else BorderStroke(RasikaTokens.hairlineWidth, scheme.secondary.copy(alpha = 0.35f)),
        shadowElevation = 0.dp,
        tonalElevation = 0.dp,
    ) {
        Box(
            Modifier.padding(horizontal = RasikaTokens.md),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                label,
                style = MaterialTheme.typography.labelLarge,
                color = if (selected) scheme.onPrimary else scheme.secondary,
            )
        }
    }
}

/** Removable filter chip — solid saffron with a trailing clear affordance. */
@Composable
fun RasikaFilterChip(
    label: String,
    onClear: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val scheme = MaterialTheme.colorScheme
    Surface(
        onClick = onClear,
        modifier = modifier
            .heightIn(min = 36.dp)
            .semantics { contentDescription = label },
        shape = RoundedCornerShape(RasikaTokens.fieldRadius),
        color = scheme.primary,
        shadowElevation = 0.dp,
        tonalElevation = 0.dp,
    ) {
        Row(
            Modifier.padding(start = RasikaTokens.sm, end = RasikaTokens.xs),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(RasikaTokens.xs),
        ) {
            Text(label, style = MaterialTheme.typography.labelLarge, color = scheme.onPrimary)
            Icon(
                Icons.Outlined.Close,
                contentDescription = RasikaCopy.CLEAR,
                tint = scheme.onPrimary,
                modifier = Modifier.size(14.dp),
            )
        }
    }
}

@Composable
fun RasikaPressable(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.98f else 1f,
        animationSpec = tween(RasikaMotion.cardPressMs),
        label = "rasikaCardPress",
    )
    Surface(
        onClick = onClick,
        modifier = modifier.graphicsLayer {
            scaleX = scale
            scaleY = scale
        },
        interactionSource = interaction,
        shape = RoundedCornerShape(RasikaTokens.cardRadius),
        color = RasikaTheme.colors.card,
        border = BorderStroke(RasikaTokens.hairlineWidth, RasikaTheme.colors.hairline),
        shadowElevation = 0.dp,
        tonalElevation = 0.dp,
    ) {
        content()
    }
}

/**
 * Bottom navigation — three tabs on a teal plinth with a scalloped cream top edge, per
 * R7 and visual-design §5. Preferences is not a tab; it sits behind the header gear.
 */
@Composable
fun RasikaTabBar(
    selected: RasikaTab,
    onSelect: (RasikaTab) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier.fillMaxWidth()) {
        ScallopPlinthEdge()
        Row(
            Modifier
                .fillMaxWidth()
                .background(RasikaTokens.teal)
                .height(64.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            RasikaTabSpec.entries.forEach { spec ->
                val isSelected = selected == spec.tab
                val tint = if (isSelected) RasikaTokens.onDark else RasikaTokens.onDarkSoft.copy(alpha = 0.7f)
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clickable { onSelect(spec.tab) }
                        .semantics { contentDescription = spec.label },
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Icon(spec.icon, contentDescription = spec.label, tint = tint)
                    Text(
                        spec.label.uppercase(),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                        color = tint,
                        modifier = Modifier.padding(top = RasikaTokens.xxs),
                    )
                }
            }
        }
    }
}

private enum class RasikaTabSpec(
    val tab: RasikaTab,
    val label: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
) {
    Search(RasikaTab.Search, RasikaCopy.TAB_SEARCH, Icons.Outlined.Search),
    Browse(RasikaTab.Browse, RasikaCopy.TAB_BROWSE, Icons.AutoMirrored.Outlined.MenuBook),
    Favourites(RasikaTab.Favourites, RasikaCopy.TAB_FAVOURITES, Icons.Outlined.FavoriteBorder),
}
