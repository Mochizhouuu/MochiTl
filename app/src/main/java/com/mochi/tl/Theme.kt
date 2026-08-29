@file:OptIn(ExperimentalMaterial3Api::class)

package com.mochi.tl

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

/**
 * Tema visual aplikasi (Material 3, skema warna custom MochiTL).
 */
@Composable
fun MochiAppTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val lightColors = lightColorScheme(
        primary = Color(0xFF386A36),
        onPrimary = Color.White,
        primaryContainer = Color(0xFFD3EED0),
        onPrimaryContainer = Color(0xFF072107),
        secondary = Color(0xFF52634F),
        onSecondary = Color.White,
        secondaryContainer = Color(0xFFD5E8CE),
        onSecondaryContainer = Color(0xFF101F10),
        background = Color(0xFFF7FBF5),
        surface = Color(0xFFF7FBF5),
        surfaceVariant = Color(0xFFDEE5D9),
        onSurfaceVariant = Color(0xFF424940)
    )

    val darkColors = darkColorScheme(
        primary = Color(0xFFA5D698),
        onPrimary = Color(0xFF07390C),
        primaryContainer = Color(0xFF205120),
        onPrimaryContainer = Color(0xFFC1F3B3),
        secondary = Color(0xFFB9CCB3),
        onSecondary = Color(0xFF253423),
        secondaryContainer = Color(0xFF3B4B38),
        onSecondaryContainer = Color(0xFFD5E8CE),
        background = Color(0xFF101410),
        surface = Color(0xFF101410),
        surfaceVariant = Color(0xFF424940),
        onSurfaceVariant = Color(0xFFC2C9BD)
    )

    val colorScheme = if (darkTheme) darkColors else lightColors
    val view = LocalView.current

    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window
            window?.let { win ->
                win.statusBarColor = colorScheme.surface.toArgb()
                win.navigationBarColor = colorScheme.surface.toArgb()
                val controller = WindowCompat.getInsetsController(win, view)
                controller.isAppearanceLightStatusBars = !darkTheme
                controller.isAppearanceLightNavigationBars = !darkTheme
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        content = content
    )
}
