package com.sangita.grantha.shared.presentation.theme

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Easing
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.sangita.grantha.shared.mobile.storage.AppearancePreference
import com.sangita.grantha.shared.mobile.storage.TextSizePreference
import com.sangita.grantha.shared.presentation.Res
import com.sangita.grantha.shared.presentation.fraunces_light
import com.sangita.grantha.shared.presentation.fraunces_medium
import com.sangita.grantha.shared.presentation.fraunces_regular
import com.sangita.grantha.shared.presentation.worksans_medium
import com.sangita.grantha.shared.presentation.worksans_regular
import com.sangita.grantha.shared.presentation.worksans_semibold
import org.jetbrains.compose.resources.Font

/**
 * Palette and type drawn from
 * application_documentation/05-frontend/mobile/track-138-visual-design.md.
 *
 * The palette is Tamil Nadu gopuram polychromy — coral-red cell-work banded against
 * teal on cream, with gold finials — and keeps the admin console's saffron accent so
 * web and mobile read as one product.
 *
 * Contrast rule (§2): gold and teal-light are ornament fills only, never text colours.
 * Any label under 14sp uses [saffronDeep], [inkSoft], or [teal].
 */
object RasikaTokens {
    // -- Core palette (light) -------------------------------------------------
    val saffronDeep = Color(0xFFA92815) // small text, section labels, active tab ink
    val saffron = Color(0xFFC1350F) // primary action, active chip, selection, progress
    val coral = Color(0xFFE0553F) // ornament fills, cornice base band
    val gold = Color(0xFFE9A838) // rules, anga boundaries, kalaśam
    val teal = Color(0xFF1F6B70) // tab plinth, secondary action, counts
    val tealLight = Color(0xFF2D8B90) // ornament vault, nāsika inner
    val cream = Color(0xFFFDF3E3) // screen ground
    val creamDeep = Color(0xFFFBE8CF) // tab strip, notices, thumbnail fill
    val paper = Color(0xFFFFFFFF) // cards
    val ink = Color(0xFF2A1A12) // primary text
    val inkSoft = Color(0xFF7A5C4A) // secondary text (5.5:1 on cream)
    val hairline = Color(0x33A92815) // card borders — rgba(169,40,21,.20)
    val onDark = Color(0xFFFFF6E8) // text over artwork and teal
    val onDarkSoft = Color(0xFFF7E9D2) // meta over artwork
    val saffronSoftLight = Color(0xFFFBE8CF)

    // -- Painted-cornice bands (§4) ------------------------------------------
    val corniceGreen = Color(0xFF7BA23F)
    val corniceGold = Color(0xFFF2C53D)
    val cornicePink = Color(0xFFE07A8A)
    val corniceScallop = Color(0xFFFDF3E3)
    val plinthScallop = Color(0xFFF7E9D2)

    // -- Dark (provisional — R7 dark theme still needs a dedicated pass) -------
    val paperDark = Color(0xFF161210)
    val cardDark = Color(0xFF231C18)
    val inkDark = Color(0xFFF3EBE0)
    val inkSoftDark = Color(0xFFB6A598)
    val saffronDark = Color(0xFFE08A4C)
    val saffronSoftDark = Color(0xFF3A2418)
    val tealDark = Color(0xFF7FB3C3)
    val goldDark = Color(0xFF8A7340)
    val errorDark = Color(0xFFE08A84)
    val hairlineDark = Color(0x33E08A4C)

    val xxs: Dp = 4.dp
    val xs: Dp = 8.dp
    val sm: Dp = 12.dp
    val md: Dp = 16.dp
    val lg: Dp = 24.dp
    val xl: Dp = 32.dp
    val screen: Dp = 20.dp
    val cardRadius: Dp = 14.dp
    val fieldRadius: Dp = 10.dp
    val tapTarget: Dp = 48.dp
    val hairlineWidth: Dp = 1.dp
}

object RasikaMotion {
    const val tabCrossfadeMs: Int = 180
    const val cardPressMs: Int = 80
    const val messageAppearMs: Int = 160
    val emphasizedDecelerate: Easing = CubicBezierEasing(0.05f, 0.7f, 0.1f, 1f)
}

private val RasikaShapes = Shapes(
    extraSmall = RoundedCornerShape(RasikaTokens.fieldRadius),
    small = RoundedCornerShape(RasikaTokens.fieldRadius),
    medium = RoundedCornerShape(RasikaTokens.cardRadius),
    large = RoundedCornerShape(RasikaTokens.cardRadius),
    extraLarge = RoundedCornerShape(RasikaTokens.cardRadius),
)

/**
 * Tokens Material 3 does not carry. [card] is the raised card ground (white in light),
 * distinct from [MaterialTheme.colorScheme.surface] which is the cream screen ground.
 */
@Immutable
data class RasikaExtendedColors(
    val card: Color,
    val creamDeep: Color,
    val inkMuted: Color,
    val hairline: Color,
    val goldLine: Color,
    val onDark: Color,
    val onDarkSoft: Color,
)

val LocalRasikaExtendedColors = staticCompositionLocalOf {
    RasikaExtendedColors(
        card = RasikaTokens.paper,
        creamDeep = RasikaTokens.creamDeep,
        inkMuted = RasikaTokens.inkSoft,
        hairline = RasikaTokens.hairline,
        goldLine = RasikaTokens.gold,
        onDark = RasikaTokens.onDark,
        onDarkSoft = RasikaTokens.onDarkSoft,
    )
}

val LocalLyricScale = staticCompositionLocalOf { 1f }

private val LightScheme = lightColorScheme(
    primary = RasikaTokens.saffron,
    onPrimary = RasikaTokens.cream,
    primaryContainer = RasikaTokens.saffronSoftLight,
    onPrimaryContainer = RasikaTokens.saffronDeep,
    secondary = RasikaTokens.teal,
    onSecondary = RasikaTokens.onDark,
    secondaryContainer = RasikaTokens.creamDeep,
    onSecondaryContainer = RasikaTokens.teal,
    background = RasikaTokens.cream,
    onBackground = RasikaTokens.ink,
    surface = RasikaTokens.cream,
    onSurface = RasikaTokens.ink,
    surfaceVariant = RasikaTokens.creamDeep,
    onSurfaceVariant = RasikaTokens.inkSoft,
    surfaceContainerLowest = RasikaTokens.paper,
    surfaceContainerLow = RasikaTokens.paper,
    surfaceContainer = RasikaTokens.paper,
    surfaceContainerHigh = RasikaTokens.paper,
    surfaceContainerHighest = RasikaTokens.paper,
    outline = RasikaTokens.hairline,
    outlineVariant = RasikaTokens.hairline,
    error = RasikaTokens.saffronDeep,
    onError = RasikaTokens.cream,
    inverseSurface = RasikaTokens.ink,
    inverseOnSurface = RasikaTokens.cream,
    inversePrimary = RasikaTokens.saffronDark,
)

private val DarkScheme = darkColorScheme(
    primary = RasikaTokens.saffronDark,
    onPrimary = RasikaTokens.ink,
    primaryContainer = RasikaTokens.saffronSoftDark,
    onPrimaryContainer = RasikaTokens.saffronDark,
    secondary = RasikaTokens.tealDark,
    onSecondary = RasikaTokens.ink,
    secondaryContainer = RasikaTokens.cardDark,
    onSecondaryContainer = RasikaTokens.tealDark,
    background = RasikaTokens.paperDark,
    onBackground = RasikaTokens.inkDark,
    surface = RasikaTokens.paperDark,
    onSurface = RasikaTokens.inkDark,
    surfaceVariant = RasikaTokens.cardDark,
    onSurfaceVariant = RasikaTokens.inkSoftDark,
    surfaceContainerLowest = RasikaTokens.paperDark,
    surfaceContainerLow = RasikaTokens.cardDark,
    surfaceContainer = RasikaTokens.cardDark,
    surfaceContainerHigh = RasikaTokens.cardDark,
    surfaceContainerHighest = RasikaTokens.cardDark,
    outline = RasikaTokens.goldDark,
    outlineVariant = RasikaTokens.goldDark,
    error = RasikaTokens.errorDark,
    inverseSurface = RasikaTokens.inkDark,
    inverseOnSurface = RasikaTokens.paperDark,
    inversePrimary = RasikaTokens.saffron,
)

@Composable
private fun frauncesFamily(): FontFamily = FontFamily(
    Font(Res.font.fraunces_light, FontWeight.Light),
    Font(Res.font.fraunces_regular, FontWeight.Normal),
    Font(Res.font.fraunces_medium, FontWeight.Medium),
)

@Composable
private fun workSansFamily(): FontFamily = FontFamily(
    Font(Res.font.worksans_regular, FontWeight.Normal),
    Font(Res.font.worksans_medium, FontWeight.Medium),
    Font(Res.font.worksans_semibold, FontWeight.SemiBold),
)

/**
 * Type roles from §3. Fraunces carries titles and lyrics; Work Sans carries labels
 * and body. Indic scripts (Devanagari/Tamil/Telugu/Kannada) fall back to the platform
 * font automatically — Fraunces is Latin-only and used for the transliterations.
 *
 * Lyric line-height stays at or above 1.9 — Indic stacked mātras clip at Material's 1.4.
 */
private fun rasikaTypography(
    fraunces: FontFamily,
    workSans: FontFamily,
    lyricScale: Float,
): Typography = Typography(
    // Krithi title / reader title
    displayLarge = TextStyle(
        fontFamily = fraunces,
        fontWeight = FontWeight.Medium,
        fontSize = 28.sp,
        lineHeight = 32.sp,
    ),
    // Screen titles (Browse, Favourites, Preferences)
    titleLarge = TextStyle(
        fontFamily = fraunces,
        fontWeight = FontWeight.Medium,
        fontSize = 25.sp,
        lineHeight = 29.sp,
    ),
    // Item / result / setting titles
    titleMedium = TextStyle(
        fontFamily = fraunces,
        fontWeight = FontWeight.Medium,
        fontSize = 16.sp,
        lineHeight = 21.sp,
    ),
    // General body, search input, helper text
    bodyLarge = TextStyle(
        fontFamily = workSans,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp,
    ),
    // Lyric text — the three sizes required by R7 come from lyricScale
    bodyMedium = TextStyle(
        fontFamily = fraunces,
        fontWeight = FontWeight.Normal,
        fontSize = (17.5f * lyricScale).sp,
        lineHeight = (33f * lyricScale).sp,
    ),
    // Sub-lines, snippets
    bodySmall = TextStyle(
        fontFamily = fraunces,
        fontWeight = FontWeight.Light,
        fontSize = 12.5.sp,
        lineHeight = 19.sp,
    ),
    // Section labels, tab labels, meta lines — uppercase, tracked
    labelLarge = TextStyle(
        fontFamily = workSans,
        fontWeight = FontWeight.SemiBold,
        fontSize = 10.5.sp,
        lineHeight = 15.sp,
        letterSpacing = 0.16.em,
    ),
    labelMedium = TextStyle(
        fontFamily = workSans,
        fontWeight = FontWeight.Normal,
        fontSize = 10.5.sp,
        lineHeight = 15.sp,
        letterSpacing = 0.04.em,
    ),
    labelSmall = TextStyle(
        fontFamily = workSans,
        fontWeight = FontWeight.SemiBold,
        fontSize = 9.sp,
        lineHeight = 13.sp,
        letterSpacing = 0.2.em,
    ),
)

fun TextSizePreference.toLyricScale(): Float = when (this) {
    TextSizePreference.SMALL -> 0.86f // ~15sp
    TextSizePreference.MEDIUM -> 1.0f // 17.5sp
    TextSizePreference.LARGE -> 1.14f // ~20sp
    TextSizePreference.EXTRA_LARGE -> 1.32f
}

@Composable
fun RasikaTheme(
    appearance: AppearancePreference = AppearancePreference.SYSTEM,
    textSize: TextSizePreference = TextSizePreference.MEDIUM,
    content: @Composable () -> Unit,
) {
    val dark = when (appearance) {
        AppearancePreference.SYSTEM -> isSystemInDarkTheme()
        AppearancePreference.LIGHT -> false
        AppearancePreference.DARK -> true
    }
    val extended = if (dark) {
        RasikaExtendedColors(
            card = RasikaTokens.cardDark,
            creamDeep = RasikaTokens.cardDark,
            inkMuted = RasikaTokens.inkSoftDark,
            hairline = RasikaTokens.hairlineDark,
            goldLine = RasikaTokens.goldDark,
            onDark = RasikaTokens.onDark,
            onDarkSoft = RasikaTokens.onDarkSoft,
        )
    } else {
        RasikaExtendedColors(
            card = RasikaTokens.paper,
            creamDeep = RasikaTokens.creamDeep,
            inkMuted = RasikaTokens.inkSoft,
            hairline = RasikaTokens.hairline,
            goldLine = RasikaTokens.gold,
            onDark = RasikaTokens.onDark,
            onDarkSoft = RasikaTokens.onDarkSoft,
        )
    }
    val scale = textSize.toLyricScale()
    val fraunces = frauncesFamily()
    val workSans = workSansFamily()
    CompositionLocalProvider(
        LocalRasikaExtendedColors provides extended,
        LocalLyricScale provides scale,
    ) {
        MaterialTheme(
            colorScheme = if (dark) DarkScheme else LightScheme,
            typography = rasikaTypography(fraunces, workSans, scale),
            shapes = RasikaShapes,
            content = content,
        )
    }
}

object RasikaTheme {
    val colors: RasikaExtendedColors
        @Composable get() = LocalRasikaExtendedColors.current
}
