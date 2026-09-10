package com.sangita.grantha.shared.presentation.theme

import androidx.compose.runtime.Composable

/** True when the host has Reduce Motion / animator duration scale 0. */
@Composable
expect fun rememberReduceMotion(): Boolean
