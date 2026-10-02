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
import com.rajatnagpure.pcmplayerconverter.domain.usecase.ConvertAudioToPcmUseCase
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

    @Inject
    lateinit var convertAudioToPcmUseCase: ConvertAudioToPcmUseCase

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val inFile = intent?.getSerializableExtra("inFile") as? File
        val outUriString = intent?.getStringExtra("outUri")
        val outFile = intent?.getSerializableExtra("outFile") as? File // Legacy/Internal support
        val config = intent?.getParcelableExtra<com.rajatnagpure.pcmplayerconverter.domain.model.AudioConfig>("config")
        val task = intent?.getStringExtra("task") ?: "PCM_TO_FORMAT"

        if (inFile != null && (outUriString != null || outFile != null)) {
            startForeground(1, createNotification("Converting..."))
            serviceScope.launch {
                var tempFile: File? = null
                try {
                    // unexpected but possible: use provided outFile or create a temp one
                    val workingFile = if (outFile != null) {
                        outFile
                    } else {
                        // Create a temp file in cache for the conversion process
                        tempFile = File(cacheDir, "temp_conversion_${System.currentTimeMillis()}")
                        tempFile
                    }

                    val result = if (task == "AUDIO_TO_PCM") {
                        // convert from input audio to PCM
                        convertAudioToPcmUseCase(inFile, workingFile)
                    } else {
                        // default: convert pcm to requested format
                        if (config != null) {
                            convertPcmUseCase(inFile, workingFile, config)
                        } else {
                            throw IllegalArgumentException("Missing config for PCM conversion")
                        }
                    }
                    result.getOrThrow()

                    // If we used a temp file and have a target URI, copy the result there
                    if (outUriString != null && tempFile != null && workingFile.exists()) {
                         val outUri = android.net.Uri.parse(outUriString)
                         contentResolver.openOutputStream(outUri)?.use { outputStream ->
                             java.io.FileInputStream(workingFile).use { inputStream ->
                                 inputStream.copyTo(outputStream)
                             }
                         }
                    }

                    // notify success to UI
                    val successIntent = Intent(ACTION_CONVERSION_COMPLETE).apply {
                        putExtra("success", true)
                        if (outUriString != null) {
                             putExtra("outUri", outUriString)
                        }
                        if (outFile != null) {
                             putExtra("outFile", outFile.absolutePath)
                        }
                    }
                    sendBroadcast(successIntent)

                    stopForeground(STOP_FOREGROUND_REMOVE)
                    stopSelf()
                } catch (e: Exception) {
                    // notify failure to UI
                    val failIntent = Intent(ACTION_CONVERSION_COMPLETE).apply {
                        putExtra("success", false)
                        putExtra("message", e.message)
                    }
                    sendBroadcast(failIntent)

                    stopForeground(STOP_FOREGROUND_REMOVE)
                    stopSelf()
                } finally {
                    // Clean up temp file
                    tempFile?.delete()
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

    companion object {
        const val ACTION_CONVERSION_COMPLETE = "com.rajatnagpure.pcmplayerconverter.CONVERSION_COMPLETE"
    }
}
