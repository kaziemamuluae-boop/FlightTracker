package com.example.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import com.example.model.AppThemeMode

private val DarkAviationColorScheme = darkColorScheme(
    primary = AviationSkyBlue,
    onPrimary = AviationDeepNavy,
    primaryContainer = AviationSurfaceHighest,
    onPrimaryContainer = AviationTextPrimary,
    secondary = AviationCyanHighlight,
    onSecondary = AviationDeepNavy,
    secondaryContainer = Color(0xFF0E3A4D),
    onSecondaryContainer = AviationCyanHighlight,
    tertiary = AviationMintGreen,
    onTertiary = AviationDeepNavy,
    tertiaryContainer = Color(0xFF0F3D2E),
    onTertiaryContainer = AviationMintGreen,
    background = AviationDeepNavy,
    onBackground = AviationTextPrimary,
    surface = AviationSurfaceNavy,
    onSurface = AviationTextPrimary,
    surfaceVariant = AviationSurfaceElevated,
    onSurfaceVariant = AviationTextSecondary,
    surfaceContainerLowest = AviationDeepNavy,
    surfaceContainerLow = AviationSurfaceNavy,
    surfaceContainer = AviationSurfaceElevated,
    surfaceContainerHigh = AviationSurfaceHighest,
    outline = AviationOutlineDark,
    outlineVariant = Color(0xFF1E3054),
    error = AviationCoralRed,
    onError = AviationDeepNavy,
    errorContainer = Color(0xFF4C1D24),
    onErrorContainer = AviationCoralRed
)

private val LightAviationColorScheme = lightColorScheme(
    primary = DaylightPrimary,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD6EEFF),
    onPrimaryContainer = Color(0xFF073452),
    secondary = DaylightSecondary,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFCFFAFE),
    onSecondaryContainer = Color(0xFF083344),
    tertiary = DaylightTertiary,
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFD1FAE5),
    onTertiaryContainer = Color(0xFF064E3B),
    background = DaylightBackground,
    onBackground = DaylightTextPrimary,
    surface = DaylightSurface,
    onSurface = DaylightTextPrimary,
    surfaceVariant = DaylightSurfaceVariant,
    onSurfaceVariant = DaylightTextSecondary,
    surfaceContainerLowest = Color.White,
    surfaceContainerLow = Color(0xFFF8FAFC),
    surfaceContainer = DaylightSurfaceVariant,
    surfaceContainerHigh = Color(0xFFD8E6F6),
    outline = DaylightOutline,
    outlineVariant = Color(0xFFE2E8F0),
    error = Color(0xFFDC2626),
    onError = Color.White,
    errorContainer = Color(0xFFFEE2E2),
    onErrorContainer = Color(0xFF7F1D1D)
)

val AviationShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(22.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

@Composable
fun FlightTrackerTheme(
    themeMode: AppThemeMode = AppThemeMode.DARK,
    content: @Composable () -> Unit
) {
    val darkTheme = when (themeMode) {
        AppThemeMode.DARK -> true
        AppThemeMode.LIGHT -> false
        AppThemeMode.SYSTEM -> isSystemInDarkTheme()
    }
    val colorScheme = if (darkTheme) DarkAviationColorScheme else LightAviationColorScheme

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window
            if (window != null) {
                WindowCompat.getInsetsController(window, view).apply {
                    isAppearanceLightStatusBars = !darkTheme
                    isAppearanceLightNavigationBars = !darkTheme
                }
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = AppTypography,
        shapes = AviationShapes,
        content = content
    )
}
