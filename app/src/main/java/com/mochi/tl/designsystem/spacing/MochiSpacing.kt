package com.mochi.tl.designsystem.spacing

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

data class MochiSpacing(
    val extraSmall: Dp = 4.dp,
    val small: Dp = 8.dp,
    val medium: Dp = 12.dp,
    val large: Dp = 16.dp,
    val extraLarge: Dp = 24.dp,
    val huge: Dp = 32.dp
)

val LocalMochiSpacing = compositionLocalOf { MochiSpacing() }

val MochiSpacing.Companion: MochiSpacing
    @Composable
    @ReadOnlyComposable
    get() = LocalMochiSpacing.current
