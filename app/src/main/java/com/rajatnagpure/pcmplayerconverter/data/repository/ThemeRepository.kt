package com.rajatnagpure.pcmplayerconverter.data.repository

import android.content.Context
import android.content.SharedPreferences
import com.rajatnagpure.pcmplayerconverter.ui.theme.NeuTheme
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ThemeRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val prefs: SharedPreferences = context.getSharedPreferences("theme_prefs", Context.MODE_PRIVATE)
    
    private val _currentTheme = MutableStateFlow(getSavedTheme())
    val currentTheme: StateFlow<NeuTheme> = _currentTheme.asStateFlow()

    private fun getSavedTheme(): NeuTheme {
        val themeName = prefs.getString("selected_theme", NeuTheme.CYBERPUNK.name) ?: NeuTheme.CYBERPUNK.name
        return try {
            NeuTheme.valueOf(themeName)
        } catch (e: IllegalArgumentException) {
            NeuTheme.CYBERPUNK
        }
    }

    fun setTheme(theme: NeuTheme) {
        prefs.edit().putString("selected_theme", theme.name).apply()
        _currentTheme.value = theme
    }
}
