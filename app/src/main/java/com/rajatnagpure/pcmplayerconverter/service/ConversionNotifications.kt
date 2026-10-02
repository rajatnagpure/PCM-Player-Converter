package com.rajatnagpure.pcmplayerconverter.service

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.rajatnagpure.pcmplayerconverter.MainActivity
import com.rajatnagpure.pcmplayerconverter.R

object ConversionNotifications {
    const val PROGRESS_CHANNEL_ID = "conversion_channel"
    const val RESULT_CHANNEL_ID = "conversion_results"
    const val PROGRESS_NOTIFICATION_ID = 1
    const val RESULT_NOTIFICATION_ID = 2

    fun ensureChannels(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(
            NotificationChannel(PROGRESS_CHANNEL_ID, "Conversion progress", NotificationManager.IMPORTANCE_LOW)
        )
        manager.createNotificationChannel(
            NotificationChannel(RESULT_CHANNEL_ID, "Conversion results", NotificationManager.IMPORTANCE_DEFAULT).apply {
                description = "Tells you when a conversion has completed or failed"
            }
        )
    }

    fun progress(context: Context): Notification {
        ensureChannels(context)
        return NotificationCompat.Builder(context, PROGRESS_CHANNEL_ID)
            .setContentTitle("PCM Converter")
            .setContentText("Converting…")
            .setSmallIcon(R.mipmap.ic_launcher)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setProgress(0, 0, true)
            .setContentIntent(openAppIntent(context))
            .build()
    }

    fun completed(context: Context, outputName: String?, outUri: Uri?): Notification {
        ensureChannels(context)
        val text = if (outputName != null) "Saved as $outputName" else "Your file has been saved"
        return NotificationCompat.Builder(context, RESULT_CHANNEL_ID)
            .setContentTitle("Conversion completed")
            .setContentText(text)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setAutoCancel(true)
            .setCategory(NotificationCompat.CATEGORY_STATUS)
            .setContentIntent(outUri?.let { openFileIntent(context, it) } ?: openAppIntent(context))
            .build()
    }

    fun failed(context: Context, reason: String?): Notification {
        ensureChannels(context)
        return NotificationCompat.Builder(context, RESULT_CHANNEL_ID)
            .setContentTitle("Conversion failed")
            .setContentText(reason?.take(120) ?: "Something went wrong. Please try again.")
            .setSmallIcon(R.mipmap.ic_launcher)
            .setAutoCancel(true)
            .setCategory(NotificationCompat.CATEGORY_ERROR)
            .setContentIntent(openAppIntent(context))
            .build()
    }

    /** Posts silently-skips when the user has not granted POST_NOTIFICATIONS (Android 13+). */
    fun post(context: Context, id: Int, notification: Notification) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) return
        NotificationManagerCompat.from(context).notify(id, notification)
    }

    private fun openAppIntent(context: Context): PendingIntent {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        return PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
    }

    private fun openFileIntent(context: Context, uri: Uri): PendingIntent {
        val view = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, context.contentResolver.getType(uri) ?: "audio/*")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        // Chooser always resolves, even when no player app is installed.
        val chooser = Intent.createChooser(view, "Open with").apply {
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        return PendingIntent.getActivity(context, 1, chooser, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
    }
}
