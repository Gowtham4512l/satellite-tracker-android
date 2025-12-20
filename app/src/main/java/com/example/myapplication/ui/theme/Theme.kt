package com.example.myapplication.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val SpaceColorScheme = darkColorScheme(
    primary = NebulaPurple,
    onPrimary = StarWhite,
    primaryContainer = SpacePurple,
    onPrimaryContainer = StarWhite,
    
    secondary = CosmicBlue,
    onSecondary = DeepSpaceBlack,
    secondaryContainer = DeepSpaceBlue,
    onSecondaryContainer = StarWhite,
    
    tertiary = NebulaPink,
    onTertiary = StarWhite,
    tertiaryContainer = DarkNebula,
    onTertiaryContainer = StarWhite,
    
    background = DeepSpaceBlack,
    onBackground = TextPrimary,
    
    surface = DeepSpaceBlue,
    onSurface = TextPrimary,
    surfaceVariant = CardBackground,
    onSurfaceVariant = TextSecondary,
    
    error = ErrorRed,
    onError = StarWhite,
    errorContainer = Color(0x33FF5370),
    onErrorContainer = ErrorRed,
    
    outline = GlassBorder,
    outlineVariant = Color(0x22FFFFFF),
    
    scrim = OverlayDark
)

@Composable
fun MyApplicationTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = SpaceColorScheme,
        typography = Typography,
        content = content
    )
}