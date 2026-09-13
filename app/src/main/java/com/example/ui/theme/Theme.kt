package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val RashedDarkColorScheme = darkColorScheme(
    primary = CyanNeon,
    onPrimary = Color(0xFF030712),
    primaryContainer = Color(0xFF0E3854),
    onPrimaryContainer = CyanNeon,
    secondary = VioletElectric,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFF2E1A47),
    onSecondaryContainer = Color(0xFFDDD6FE),
    tertiary = PinkCyber,
    onTertiary = Color.White,
    background = DarkCanvas,
    onBackground = TextPrimary,
    surface = DarkSurface,
    onSurface = TextPrimary,
    surfaceVariant = DarkSurfaceElevated,
    onSurfaceVariant = TextSecondary,
    error = CrimsonStop,
    onError = Color.White
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = true, // Futuristic Voice UI is dark-first
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = RashedDarkColorScheme,
        typography = Typography,
        content = content
    )
}

