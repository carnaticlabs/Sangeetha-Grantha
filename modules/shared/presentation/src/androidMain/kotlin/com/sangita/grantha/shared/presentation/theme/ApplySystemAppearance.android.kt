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
            @Suppress("DEPRECATION")
            window.navigationBarColor = background.toArgb()
        }
    }
}
