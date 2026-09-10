package com.sangita.grantha.shared.presentation.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.sangita.grantha.shared.presentation.RasikaCopy
import com.sangita.grantha.shared.presentation.theme.RasikaMotion
import com.sangita.grantha.shared.presentation.theme.RasikaTheme
import com.sangita.grantha.shared.presentation.theme.RasikaTokens
import com.sangita.grantha.shared.presentation.theme.rememberReduceMotion

@Composable
fun LoadStateContent(
    state: LoadState,
    emptyTitle: String,
    emptyBody: String,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    when (state) {
        LoadState.Idle -> content()
        LoadState.Loading -> SkeletonCards(modifier, reduceMotion = rememberReduceMotion())
        LoadState.Empty -> MessagePane(emptyTitle, emptyBody, retry = null, modifier = modifier)
        is LoadState.Error -> MessagePane(
            title = RasikaCopy.ERROR_TITLE,
            body = RasikaCopy.ERROR_BODY,
            retry = if (state.retryable) onRetry else null,
            modifier = modifier,
        )
    }
}

/** Three shimmering placeholder cards, matching the prototype's loading pass. */
@Composable
private fun SkeletonCards(modifier: Modifier = Modifier, reduceMotion: Boolean = false) {
    if (reduceMotion) {
        SkeletonBars(modifier, alpha = 0.65f)
    } else {
        val transition = rememberInfiniteTransition(label = "skeleton")
        val alpha by transition.animateFloat(
            initialValue = 0.4f,
            targetValue = 0.9f,
            animationSpec = infiniteRepeatable(tween(900), RepeatMode.Reverse),
            label = "skeletonAlpha",
        )
        SkeletonBars(modifier, alpha)
    }
}

@Composable
private fun SkeletonBars(modifier: Modifier, alpha: Float) {
    Column(
        modifier
            .fillMaxWidth()
            .padding(top = RasikaTokens.md),
        verticalArrangement = Arrangement.spacedBy(RasikaTokens.sm),
    ) {
        listOf(0.62f, 0.52f, 0.7f).forEach { titleWidth ->
            Column(
                Modifier
                    .fillMaxWidth()
                    .background(RasikaTheme.colors.card, RoundedCornerShape(RasikaTokens.cardRadius))
                    .border(
                        RasikaTokens.hairlineWidth,
                        RasikaTheme.colors.hairline,
                        RoundedCornerShape(RasikaTokens.cardRadius),
                    )
                    .padding(RasikaTokens.md),
            ) {
                Bar(fraction = titleWidth, height = 15.dp, alpha = alpha)
                Box(Modifier.height(RasikaTokens.xs))
                Bar(fraction = titleWidth * 0.6f, height = 10.dp, alpha = alpha)
            }
        }
    }
}

@Composable
private fun Bar(fraction: Float, height: androidx.compose.ui.unit.Dp, alpha: Float) {
    Box(
        Modifier
            .fillMaxWidth(fraction)
            .height(height)
            .background(
                RasikaTheme.colors.creamDeep.copy(alpha = alpha),
                RoundedCornerShape(4.dp),
            ),
    )
}

@Composable
private fun MessagePane(
    title: String,
    body: String,
    retry: (() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        AnimatedVisibility(
            visible = true,
            enter = fadeIn(tween(if (rememberReduceMotion()) 0 else RasikaMotion.messageAppearMs)),
        ) {
            Column(
                modifier = Modifier.padding(RasikaTokens.lg),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(RasikaTokens.sm),
            ) {
                Text(
                    title,
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    body,
                    style = MaterialTheme.typography.bodyLarge,
                    color = RasikaTheme.colors.inkMuted,
                )
                if (retry != null) {
                    RasikaPrimaryButton(label = RasikaCopy.RETRY, onClick = retry)
                }
            }
        }
    }
}
