package com.sangita.grantha.shared.presentation.theme

import androidx.compose.runtime.Composable
import platform.UIKit.UIAccessibilityIsReduceMotionEnabled

@Composable
actual fun rememberReduceMotion(): Boolean = UIAccessibilityIsReduceMotionEnabled()
