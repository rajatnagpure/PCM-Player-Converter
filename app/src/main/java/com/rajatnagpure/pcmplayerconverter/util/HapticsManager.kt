package com.rajatnagpure.pcmplayerconverter.util

import android.content.Context
import android.view.HapticFeedbackConstants
import android.view.View
import androidx.compose.runtime.compositionLocalOf
import androidx.core.content.edit

private const val PREFS_NAME = "pcm_settings"
private const val KEY_HAPTICS_ENABLED = "haptics_enabled"

object HapticsManager {

    private var _enabled: Boolean = true
    val isEnabled: Boolean get() = _enabled

    fun load(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        _enabled = prefs.getBoolean(KEY_HAPTICS_ENABLED, true)
    }

    fun setEnabled(context: Context, enabled: Boolean) {
        _enabled = enabled
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit {
            putBoolean(KEY_HAPTICS_ENABLED, enabled)
        }
    }

    fun perform(view: View, constant: Int = HapticFeedbackConstants.KEYBOARD_TAP) {
        if (_enabled) view.performHapticFeedback(constant)
    }
}

/** CompositionLocal so any composable can call LocalHapticsManager.current.perform(view) */
val LocalHapticsManager = compositionLocalOf { HapticsManager }
