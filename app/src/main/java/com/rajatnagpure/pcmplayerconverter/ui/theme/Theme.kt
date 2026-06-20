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

@Composable
fun PCMPlayerConverterTheme(
    neuThemeConfig: NeuThemeConfig = CyberpunkTheme,
    content: @Composable () -> Unit
) {
    val colorScheme = if (neuThemeConfig.isDark) {
        darkColorScheme(
            primary = neuThemeConfig.primary,
            secondary = neuThemeConfig.primary,
            tertiary = neuThemeConfig.primary,
            background = neuThemeConfig.background,
            surface = neuThemeConfig.background,
            onPrimary = androidx.compose.ui.graphics.Color.White,
            onSecondary = androidx.compose.ui.graphics.Color.White,
            onTertiary = androidx.compose.ui.graphics.Color.White,
            onBackground = neuThemeConfig.onSurface,
            onSurface = neuThemeConfig.onSurface,
        )
    } else {
        lightColorScheme(
            primary = neuThemeConfig.primary,
            secondary = neuThemeConfig.primary,
            tertiary = neuThemeConfig.primary,
            background = neuThemeConfig.background,
            surface = neuThemeConfig.background,
            onPrimary = androidx.compose.ui.graphics.Color.White,
            onSecondary = androidx.compose.ui.graphics.Color.White,
            onTertiary = androidx.compose.ui.graphics.Color.White,
            onBackground = neuThemeConfig.onSurface,
            onSurface = neuThemeConfig.onSurface,
        )
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        // Edge-to-Edge is handled by MainActivity.kt
    }

    androidx.compose.runtime.CompositionLocalProvider(LocalNeuTheme provides neuThemeConfig) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            content = content
        )
    }
}
