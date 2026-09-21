package com.sangita.grantha.shared.presentation.theme

import android.app.Activity
import android.os.Build
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import com.sangita.grantha.shared.mobile.storage.AppearancePreference

@Suppress("UNUSED_PARAMETER")
@Composable
actual fun ApplySystemAppearance(appearance: AppearancePreference, dark: Boolean) {
    val view = LocalView.current
    if (view.isInEditMode) return
    val background = if (dark) RasikaTokens.paperDark else RasikaTokens.cream
    SideEffect {
        val window = (view.context as? Activity)?.window ?: return@SideEffect
        WindowCompat.getInsetsController(window, view).apply {
            isAppearanceLightStatusBars = !dark
            isAppearanceLightNavigationBars = !dark
        }
        if (Build.VERSION.SDK_INT < 35) {
            @Suppress("DEPRECATION")
            window.statusBarColor = background.toArgb()
            // API 24–25 cannot flip navigation-button contrast
            // (LIGHT_NAVIGATION_BAR is API 26+). Keep a dark bar so
            // the default light Back/Home/Overview glyphs stay legible.
            @Suppress("DEPRECATION")
            window.navigationBarColor = if (Build.VERSION.SDK_INT < 26) {
                RasikaTokens.paperDark.toArgb()
            } else {
                background.toArgb()
            }
        }
    }
}
