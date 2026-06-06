package com.example.akillirobotkontrol.ui.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val CyberColorScheme = darkColorScheme(
    primary = CyberBlue,
    onPrimary = DarkBackground,
    primaryContainer = CyberBlue.copy(alpha = 0.2f),
    onPrimaryContainer = CyberBlue,
    secondary = NeonGreen,
    onSecondary = DarkBackground,
    secondaryContainer = NeonGreen.copy(alpha = 0.2f),
    onSecondaryContainer = NeonGreen,
    tertiary = CyberBlue,
    error = AlertRed,
    onError = DarkBackground,
    errorContainer = AlertRed.copy(alpha = 0.2f),
    onErrorContainer = AlertRed,
    background = DarkBackground,
    onBackground = TextPrimary,
    surface = SurfaceDark,
    onSurface = TextPrimary,
    surfaceVariant = SurfaceVariantDark,
    onSurfaceVariant = TextSecondary,
    outline = CyberBlue.copy(alpha = 0.5f)
)

@Composable
fun AkilliRobotKontrolTheme(
    content: @Composable () -> Unit
) {
    // Sabit karanlık tema
    val colorScheme = CyberColorScheme
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.background.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        content = content
    )
}
