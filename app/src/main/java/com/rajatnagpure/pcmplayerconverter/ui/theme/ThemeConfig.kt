package com.rajatnagpure.pcmplayerconverter.ui.theme

import androidx.compose.ui.graphics.Color

enum class NeuTheme(val title: String) {
    CYBERPUNK("Cyberpunk"),
    MIDNIGHT("Midnight"),
    DAYLIGHT("Daylight"),
    SUNRISE("Sunrise")
}

data class NeuThemeConfig(
    val type: NeuTheme,
    val isDark: Boolean,
    val background: Color,
    val lightShadow: Color,
    val darkShadow: Color,
    val primary: Color,
    val onSurface: Color
)

val CyberpunkTheme = NeuThemeConfig(
    type = NeuTheme.CYBERPUNK,
    isDark = true,
    background = NeuDarkBg,
    lightShadow = NeuDarkShadowLight,
    darkShadow = NeuDarkShadowDark,
    primary = NeuAzureBlue,
    onSurface = Color.White
)

val MidnightTheme = NeuThemeConfig(
    type = NeuTheme.MIDNIGHT,
    isDark = true,
    background = NeuDarkBg,
    lightShadow = NeuDarkShadowLight,
    darkShadow = NeuDarkShadowDark,
    primary = NeuViolet,
    onSurface = Color.White
)

val DaylightTheme = NeuThemeConfig(
    type = NeuTheme.DAYLIGHT,
    isDark = false,
    background = NeuLightBg,
    lightShadow = NeuLightShadowLight,
    darkShadow = NeuLightShadowDark,
    primary = NeuAzureBlue,
    onSurface = Color(0xFF22262E)
)

val SunriseTheme = NeuThemeConfig(
    type = NeuTheme.SUNRISE,
    isDark = false,
    background = NeuLightBg,
    lightShadow = NeuLightShadowLight,
    darkShadow = NeuLightShadowDark,
    primary = NeuSunriseOrange,
    onSurface = Color(0xFF22262E)
)

val LocalNeuTheme = androidx.compose.runtime.staticCompositionLocalOf { CyberpunkTheme }
