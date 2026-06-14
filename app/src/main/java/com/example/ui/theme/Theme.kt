package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val AppDarkColorScheme = darkColorScheme(
    primary = Color(0xFF4FA0F6), // Light Azure Blue
    onPrimary = Color.White,
    primaryContainer = Color(0xFF282C34), // Dark Charcoal Grey surface
    onPrimaryContainer = Color.White,
    secondaryContainer = Color(0xFF4FA0F6),
    onSecondaryContainer = Color.White,
    tertiary = Color(0xFF4FA0F6), 
    onTertiary = Color.White,
    background = Color(0xFF21252B), // Dark Background
    onBackground = Color(0xFFE5E7EB),
    surface = Color(0xFF282C34), // Dark Secondary
    onSurface = Color(0xFFE5E7EB),
    surfaceVariant = Color(0xFF2E333C),
    onSurfaceVariant = Color(0xFFCAC4D0),
    error = Color(0xFFFF8582), // Coral Pink
    onError = Color.White,
    outline = Color(0xFF49454F),
    outlineVariant = Color(0xFF5E646E)
)

private val AppLightColorScheme = lightColorScheme(
    primary = Color(0xFF1B81F5), // Azure Blue
    onPrimary = Color.White,
    primaryContainer = Color(0xFF1B81F5), 
    onPrimaryContainer = Color.White,
    secondaryContainer = Color(0xFFE8F2FF), // Sky Blue Tint
    onSecondaryContainer = Color(0xFF1B81F5),
    tertiary = Color(0xFF1B81F5),
    onTertiary = Color.White,
    background = Color(0xFFF4F5F7), // Off-White
    onBackground = Color(0xFF2D323E), // Charcoal Grey
    surface = Color.White,
    onSurface = Color(0xFF2D323E),
    surfaceVariant = Color.White, 
    onSurfaceVariant = Color(0xFF5F656A),
    error = Color(0xFFFF6D6A), // Coral Pink
    onError = Color.White,
    outline = Color(0xFF79747E),
    outlineVariant = Color(0xFFCAC4D0)
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = true,
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) AppDarkColorScheme else AppLightColorScheme,
        typography = Typography,
        content = content
    )
}

