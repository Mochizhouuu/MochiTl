package com.mochi.tl.designsystem.color

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

val Emerald700 = Color(0xFF047857)
val Emerald500 = Color(0xFF10B981)
val Emerald100 = Color(0xFFD1FAE5)
val Emerald900 = Color(0xFF064E3B)

val Slate900 = Color(0xFF0B1220)
val Slate800 = Color(0xFF131C2E)
val Slate700 = Color(0xFF1E293B)
val Slate600 = Color(0xFF334155)
val Slate500 = Color(0xFF64748B)
val Slate400 = Color(0xFF94A3B8)
val Slate200 = Color(0xFFE2E8F0)
val Slate100 = Color(0xFFF1F5F9)
val Slate50  = Color(0xFFF8FAFC)

val Red600 = Color(0xFFDC2626)
val Red100 = Color(0xFFFEE2E2)
val Amber600 = Color(0xFFD97706)
val Amber100 = Color(0xFFFEF3C7)

val MochiLightColorScheme: ColorScheme = lightColorScheme(
    primary = Emerald700,
    onPrimary = Color.White,
    primaryContainer = Emerald100,
    onPrimaryContainer = Emerald900,
    secondary = Color(0xFF0F766E),
    onSecondary = Color.White,
    secondaryContainer = Slate100,
    onSecondaryContainer = Slate900,
    tertiary = Color(0xFF0F766E),
    onTertiary = Color.White,
    background = Slate50,
    onBackground = Slate900,
    surface = Color.White,
    onSurface = Slate900,
    surfaceVariant = Slate100,
    onSurfaceVariant = Slate500,
    surfaceContainer = Color.White,
    surfaceContainerHigh = Slate100,
    surfaceContainerLow = Slate50,
    outline = Color(0xFFCBD5E1),
    outlineVariant = Color(0xFFE2E8F0),
    error = Red600,
    onError = Color.White,
    errorContainer = Red100,
    onErrorContainer = Color(0xFF991B1B)
)

val MochiDarkColorScheme: ColorScheme = darkColorScheme(
    primary = Emerald500,
    onPrimary = Color(0xFF022C22),
    primaryContainer = Emerald900,
    onPrimaryContainer = Emerald100,
    secondary = Color(0xFF14B8A6),
    onSecondary = Color(0xFF022C22),
    secondaryContainer = Slate700,
    onSecondaryContainer = Slate100,
    tertiary = Color(0xFF14B8A6),
    onTertiary = Color(0xFF022C22),
    background = Slate900,
    onBackground = Slate100,
    surface = Slate800,
    onSurface = Slate100,
    surfaceVariant = Slate700,
    onSurfaceVariant = Slate400,
    surfaceContainer = Slate800,
    surfaceContainerHigh = Slate700,
    surfaceContainerLow = Slate900,
    outline = Slate600,
    outlineVariant = Color(0xFF1E293B),
    error = Red600,
    onError = Color.White,
    errorContainer = Color(0xFF7F1D1D),
    onErrorContainer = Red100
)
