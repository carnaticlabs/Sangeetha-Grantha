package com.sangita.grantha.shared.presentation.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import com.sangita.grantha.shared.mobile.storage.AppearancePreference
import platform.UIKit.UIApplication
import platform.UIKit.UIUserInterfaceStyle
import platform.UIKit.UIWindow

@Suppress("UNUSED_PARAMETER")
@Composable
actual fun ApplySystemAppearance(appearance: AppearancePreference, dark: Boolean) {
    SideEffect {
        val style = when (appearance) {
            AppearancePreference.SYSTEM -> UIUserInterfaceStyle.UIUserInterfaceStyleUnspecified
            AppearancePreference.LIGHT -> UIUserInterfaceStyle.UIUserInterfaceStyleLight
            AppearancePreference.DARK -> UIUserInterfaceStyle.UIUserInterfaceStyleDark
        }
        UIApplication.sharedApplication.windows.mapNotNull { it as? UIWindow }.forEach { window ->
            window.overrideUserInterfaceStyle = style
        }
    }
}
