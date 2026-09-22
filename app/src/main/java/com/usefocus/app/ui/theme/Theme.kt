package com.usefocus.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp

private val Light =
    lightColorScheme(
        primary = Color(0xFF176B52),
        onPrimary = Color.White,
        primaryContainer = Color(0xFFDDF5E7),
        onPrimaryContainer = Color(0xFF0A3E30),
        secondary = Color(0xFF68598C),
        secondaryContainer = Color(0xFFEDE5FF),
        onSecondaryContainer = Color(0xFF332654),
        background = Color(0xFFFFFBF4),
        surface = Color(0xFFFFFBF4),
        surfaceVariant = Color(0xFFF7F1E7),
        onSurface = Color(0xFF10231D),
        onSurfaceVariant = Color(0xFF66716D),
        outlineVariant = Color(0xFFE3E8E2),
    )
private val Dark =
    darkColorScheme(
        primary = Color(0xFF8BE0B8),
        onPrimary = Color(0xFF123B2E),
        primaryContainer = Color(0xFF164B3B),
        onPrimaryContainer = Color(0xFFD7EEDF),
        secondary = Color(0xFFB6CCBF),
        secondaryContainer = Color(0xFF3A3153),
        background = Color(0xFF0B1713),
        surface = Color(0xFF0B1713),
        surfaceVariant = Color(0xFF15251F),
        onSurface = Color(0xFFF5FBF7),
        onSurfaceVariant = Color(0xFFB7C6BE),
    )

private val BentoShapes =
    Shapes(
        extraSmall = RoundedCornerShape(10.dp),
        small = RoundedCornerShape(14.dp),
        medium = RoundedCornerShape(22.dp),
        large = RoundedCornerShape(28.dp),
        extraLarge = RoundedCornerShape(32.dp),
    )

@Composable
fun WebsiteBlockerTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (isSystemInDarkTheme()) Dark else Light,
        shapes = BentoShapes,
        content = content,
    )
}
