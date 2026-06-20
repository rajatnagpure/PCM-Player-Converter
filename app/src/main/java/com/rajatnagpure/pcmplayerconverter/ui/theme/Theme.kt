package com.rajatnagpure.pcmplayerconverter.ui.theme
import com.rajatnagpure.pcmplayerconverter.ui.components.AppText as Text

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

// Enforce Dark Theme exclusively
private val DarkColorScheme = darkColorScheme(
    primary = NeuLightBlue,
    secondary = NeuLightBlue,
    tertiary = NeuLightBlue,
    background = NeuBackground,
    surface = NeuBackground,
    onPrimary = androidx.compose.ui.graphics.Color.White,
    onSecondary = androidx.compose.ui.graphics.Color.White,
    onTertiary = androidx.compose.ui.graphics.Color.White,
    onBackground = androidx.compose.ui.graphics.Color.White,
    onSurface = androidx.compose.ui.graphics.Color.White,
)

@Composable
fun PCMPlayerConverterTheme(
    darkTheme: Boolean = true, // Force Dark Theme
    // Dynamic color is available on Android 12+
    dynamicColor: Boolean = false, // Disable dynamic color to enforce our gradient branding
    content: @Composable () -> Unit
) {
    // ALWAYS use DarkColorScheme
    val colorScheme = DarkColorScheme

    val view = LocalView.current
    if (!view.isInEditMode) {
        // Edge-to-Edge is handled by MainActivity.kt
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
