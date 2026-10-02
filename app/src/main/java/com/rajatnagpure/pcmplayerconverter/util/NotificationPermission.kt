package com.rajatnagpure.pcmplayerconverter.util

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.core.content.edit

private const val PREFS_NAME = "pcm_settings"
private const val KEY_ASKED = "notification_permission_asked"

/**
 * Returns a function that runs [action], first asking once (Android 13+) for POST_NOTIFICATIONS so
 * the "Conversion completed" notification can be shown. [action] runs whether or not it's granted;
 * the in-app message still covers users who decline.
 */
@Composable
fun rememberNotificationPermissionGate(
    onResult: (granted: Boolean) -> Unit = {},
    action: () -> Unit
): () -> Unit {
    val context = LocalContext.current
    val currentAction by rememberUpdatedState(action)
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        onResult(granted)
        currentAction()
    }
    return remember(context, launcher) {
        {
            if (shouldAsk(context)) {
                context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit { putBoolean(KEY_ASKED, true) }
                launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
            } else {
                currentAction()
            }
        }
    }
}

private fun shouldAsk(context: Context): Boolean {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return false
    if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) return false
    return !context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).getBoolean(KEY_ASKED, false)
}
