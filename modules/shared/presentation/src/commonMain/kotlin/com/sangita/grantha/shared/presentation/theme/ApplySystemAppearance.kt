package com.sangita.grantha.shared.presentation.theme

import androidx.compose.runtime.Composable
import com.sangita.grantha.shared.mobile.storage.AppearancePreference

/**
 * Pins native system-bar / interface style to the in-app appearance choice.
 * SYSTEM follows the device; Light and Dark stay pinned even when the OS changes.
 */
@Composable
expect fun ApplySystemAppearance(appearance: AppearancePreference, dark: Boolean)
