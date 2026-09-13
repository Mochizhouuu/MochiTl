@file:OptIn(ExperimentalMaterial3Api::class)

package com.mochi.tl

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat

/**
 * Scale bentuk (Shape) MochiTL dengan standar radius 6dp, 8dp, 12dp, 16dp, 24dp.
 */
val MochiShapes = Shapes(
    extraSmall = RoundedCornerShape(6.dp),
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(12.dp),
    large = RoundedCornerShape(16.dp),
    extraLarge = RoundedCornerShape(24.dp)
)

/**
 * Skala tipografi yang sengaja disesuaikan dengan bobot (FontWeight) dan line-height hierarkis.
 */
val MochiTypography = Typography(
    headlineLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Bold,
        fontSize = 28.sp,
        lineHeight = 34.sp
    ),
    headlineMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Bold,
        fontSize = 22.sp,
        lineHeight = 28.sp
    ),
    headlineSmall = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.SemiBold,
        fontSize = 18.sp,
        lineHeight = 24.sp
    ),
    titleLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Bold,
        fontSize = 18.sp,
        lineHeight = 24.sp
    ),
    titleMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.SemiBold,
        fontSize = 15.sp,
        lineHeight = 20.sp
    ),
    titleSmall = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize = 13.sp,
        lineHeight = 18.sp
    ),
    bodyLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 15.sp,
        lineHeight = 22.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 13.sp,
        lineHeight = 18.sp
    ),
    bodySmall = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        lineHeight = 16.sp
    ),
    labelLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.SemiBold,
        fontSize = 13.sp,
        lineHeight = 18.sp
    ),
    labelMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        lineHeight = 16.sp
    ),
    labelSmall = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize = 10.sp,
        lineHeight = 14.sp
    )
)

/**
 * Skema warna Mode Terang — Hijau khas MochiTL dengan kontainer desaturasi.
 */
private val LightColors = lightColorScheme(
    primary = Color(0xFF386A36),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD3EED0),
    onPrimaryContainer = Color(0xFF072107),
    secondary = Color(0xFF52634F),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFD5E8CE),
    onSecondaryContainer = Color(0xFF101F10),
    tertiary = Color(0xFF3B6470),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFBEEAF7),
    onTertiaryContainer = Color(0xFF001F26),
    background = Color(0xFFF7FBF5),
    onBackground = Color(0xFF1A1C1A),
    surface = Color(0xFFF7FBF5),
    onSurface = Color(0xFF1A1C1A),
    surfaceVariant = Color(0xFFDEE5D9),
    onSurfaceVariant = Color(0xFF424940),
    surfaceContainer = Color(0xFFEEF3EB),
    surfaceContainerHigh = Color(0xFFE8EEE5),
    surfaceContainerLow = Color(0xFFF2F7F0),
    outline = Color(0xFF72796F),
    outlineVariant = Color(0xFFC2C9BD)
)

/**
 * Skema warna Mode Gelap Standar.
 */
private val DarkColors = darkColorScheme(
    primary = Color(0xFFA5D698),
    onPrimary = Color(0xFF07390C),
    primaryContainer = Color(0xFF205120),
    onPrimaryContainer = Color(0xFFC1F3B3),
    secondary = Color(0xFFB9CCB3),
    onSecondary = Color(0xFF253423),
    secondaryContainer = Color(0xFF3B4B38),
    onSecondaryContainer = Color(0xFFD5E8CE),
    tertiary = Color(0xFFA3CDDC),
    onTertiary = Color(0xFF023640),
    tertiaryContainer = Color(0xFF214C58),
    onTertiaryContainer = Color(0xFFBEEAF7),
    background = Color(0xFF101410),
    onBackground = Color(0xFFE2E8DC),
    surface = Color(0xFF101410),
    onSurface = Color(0xFFE2E8DC),
    surfaceVariant = Color(0xFF424940),
    onSurfaceVariant = Color(0xFFC2C9BD),
    surfaceContainer = Color(0xFF1C211C),
    surfaceContainerHigh = Color(0xFF242A24),
    surfaceContainerLow = Color(0xFF161B16),
    outline = Color(0xFF8C9389),
    outlineVariant = Color(0xFF424940)
)

/**
 * Skema warna Mode Gelap OLED / True Black hemat daya.
 */
private val OledColors = darkColorScheme(
    primary = Color(0xFFA5D698),
    onPrimary = Color(0xFF07390C),
    primaryContainer = Color(0xFF183018),
    onPrimaryContainer = Color(0xFFC1F3B3),
    secondary = Color(0xFFB9CCB3),
    onSecondary = Color(0xFF253423),
    secondaryContainer = Color(0xFF2A3828),
    onSecondaryContainer = Color(0xFFD5E8CE),
    tertiary = Color(0xFFA3CDDC),
    onTertiary = Color(0xFF023640),
    tertiaryContainer = Color(0xFF183842),
    onTertiaryContainer = Color(0xFFBEEAF7),
    background = Color(0xFF000000),
    onBackground = Color(0xFFE2E8DC),
    surface = Color(0xFF000000),
    onSurface = Color(0xFFE2E8DC),
    surfaceVariant = Color(0xFF2C332B),
    onSurfaceVariant = Color(0xFFC2C9BD),
    surfaceContainer = Color(0xFF121212),
    surfaceContainerHigh = Color(0xFF1A1A1A),
    surfaceContainerLow = Color(0xFF0A0A0A),
    outline = Color(0xFF636A61),
    outlineVariant = Color(0xFF323A31)
)

/**
 * Tema visual aplikasi (Material 3, skema warna custom MochiTL).
 */
@Composable
fun MochiAppTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    isOledMode: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        darkTheme && isOledMode -> OledColors
        darkTheme -> DarkColors
        else -> LightColors
    }

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
        shapes = MochiShapes,
        typography = MochiTypography,
        content = content
    )
}
