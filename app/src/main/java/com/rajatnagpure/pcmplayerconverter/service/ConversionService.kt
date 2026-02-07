package com.rajatnagpure.pcmplayerconverter.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.rajatnagpure.pcmplayerconverter.R
import com.rajatnagpure.pcmplayerconverter.domain.usecase.ConvertPcmUseCase
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import java.io.File

@AndroidEntryPoint
class ConversionService : Service() {

    @Inject
    lateinit var convertPcmUseCase: ConvertPcmUseCase

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val inFile = intent?.getSerializableExtra("inFile") as? File
        val outFile = intent?.getSerializableExtra("outFile") as? File
        val config = intent?.getParcelableExtra<com.rajatnagpure.pcmplayerconverter.domain.model.AudioConfig>("config")

        if (inFile != null && outFile != null && config != null) {
            startForeground(1, createNotification("Converting..."))
            serviceScope.launch {
                try {
                    convertPcmUseCase(inFile, outFile, config)
                    stopForeground(STOP_FOREGROUND_REMOVE)
                    stopSelf()
                } catch (e: Exception) {
                    stopForeground(STOP_FOREGROUND_REMOVE)
                    stopSelf()
                }
            }
        } else {
            stopSelf()
        }
        return START_NOT_STICKY
    }

    private fun createNotification(content: String): Notification {
        val channelId = "conversion_channel"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channelId, "Conversions", NotificationManager.IMPORTANCE_LOW)
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
        return NotificationCompat.Builder(this, channelId)
            .setContentTitle("PCM Converter")
            .setContentText(content)
            .setSmallIcon(R.mipmap.ic_launcher)
            .build()
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
    }
}
