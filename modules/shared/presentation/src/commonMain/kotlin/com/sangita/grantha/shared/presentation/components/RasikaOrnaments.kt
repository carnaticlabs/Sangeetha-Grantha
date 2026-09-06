package com.sangita.grantha.shared.presentation.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.unit.dp
import com.sangita.grantha.shared.presentation.theme.RasikaTokens

/**
 * The four ornament primitives from the visual-design reference §4. Each is flat
 * geometry drawn with Compose [Canvas] — no image assets, no SVG runtime, nothing to
 * license. These carry the app's temple-interior identity more than colour alone.
 */

/**
 * Painted cornice — the banded moulding that closes every screen header. Stacked bands
 * top to bottom: green, gold, a teal band with a repeating cream scallop, coral base.
 * This band, more than any colour, is what makes the app read as a painted temple.
 */
@Composable
fun PaintedCornice(modifier: Modifier = Modifier) {
    Canvas(
        modifier
            .fillMaxWidth()
            .height(24.dp),
    ) {
        val w = size.width
        val green = 5.dp.toPx()
        val gold = 4.dp.toPx()
        val coral = 4.dp.toPx()
        val teal = size.height - green - gold - coral

        var y = 0f
        drawRect(RasikaTokens.corniceGreen, topLeft = Offset(0f, y), size = androidSize(w, green))
        y += green
        drawRect(RasikaTokens.corniceGold, topLeft = Offset(0f, y), size = androidSize(w, gold))
        y += gold
        drawRect(RasikaTokens.tealLight, topLeft = Offset(0f, y), size = androidSize(w, teal))
        drawScallopRow(
            top = y,
            bandHeight = teal,
            width = w,
            period = 15.dp.toPx(),
            fill = RasikaTokens.corniceScallop,
            pointingDown = true,
        )
        y += teal
        drawRect(RasikaTokens.cornicePink, topLeft = Offset(0f, y), size = androidSize(w, coral))
    }
}

/**
 * Scalloped plinth edge — a cream band with teal arcs cut downward, forming the top
 * edge of the bottom navigation so the nav reads as a temple plinth.
 */
@Composable
fun ScallopPlinthEdge(
    modifier: Modifier = Modifier,
    band: Color = RasikaTokens.plinthScallop,
    arc: Color = RasikaTokens.teal,
) {
    Canvas(
        modifier
            .fillMaxWidth()
            .height(10.dp),
    ) {
        drawRect(band, size = size)
        // Arcs cut downward from the band into the plinth below.
        val period = 15.dp.toPx()
        val path = Path()
        var x = 0f
        while (x < size.width) {
            path.moveTo(x, 0f)
            path.quadraticTo(x + period / 2f, size.height * 1.6f, x + period, 0f)
            path.close()
            x += period
        }
        drawPath(path, arc)
    }
}

/**
 * Nāsika cap — a teal bar with a centred cream horseshoe and a coral base rule. Caps
 * cards and list thumbnails; also used as the small glyph-tile crown on result rows.
 */
@Composable
fun NasikaCap(modifier: Modifier = Modifier) {
    Canvas(
        modifier
            .fillMaxWidth()
            .height(20.dp),
    ) {
        drawNasika(width = size.width, height = size.height)
    }
}

/** Draws a downward (into the band) scallop row of half-circles across [width]. */
private fun DrawScope.drawScallopRow(
    top: Float,
    bandHeight: Float,
    width: Float,
    period: Float,
    fill: Color,
    pointingDown: Boolean,
) {
    val path = Path()
    var x = 0f
    val baseY = if (pointingDown) top + bandHeight else top
    val tipY = if (pointingDown) top + bandHeight * 0.15f else top + bandHeight * 0.85f
    while (x < width) {
        path.moveTo(x, baseY)
        path.quadraticTo(x + period / 2f, tipY, x + period, baseY)
        path.close()
        x += period
    }
    drawPath(path, fill)
}

/** Draws the nāsika motif (teal bar, cream horseshoe, coral base) filling the box. */
private fun DrawScope.drawNasika(width: Float, height: Float) {
    val base = 4.dp.toPx().coerceAtMost(height * 0.25f)
    drawRect(RasikaTokens.tealLight, size = androidSize(width, height - base))
    // Cream horseshoe: a centred half-ellipse rising from the base of the teal band.
    val horseshoeW = (width * 0.55f).coerceAtMost(60.dp.toPx())
    val left = (width - horseshoeW) / 2f
    val path = Path().apply {
        moveTo(left, height - base)
        arcTo(
            rect = Rect(left, base * 0.5f, left + horseshoeW, (height - base) * 2f),
            startAngleDegrees = 180f,
            sweepAngleDegrees = 180f,
            forceMoveTo = false,
        )
        close()
    }
    drawPath(path, RasikaTokens.corniceScallop)
    drawRect(RasikaTokens.coral, topLeft = Offset(0f, height - base), size = androidSize(width, base))
}

private fun androidSize(w: Float, h: Float) = androidx.compose.ui.geometry.Size(w, h)
