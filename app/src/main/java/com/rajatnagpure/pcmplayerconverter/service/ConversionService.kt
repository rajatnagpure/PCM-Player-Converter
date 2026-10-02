package com.rajatnagpure.pcmplayerconverter.service

import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.net.Uri
import android.os.Build
import android.os.IBinder
import android.os.SystemClock
import android.provider.OpenableColumns
import androidx.core.app.ServiceCompat
import androidx.core.content.IntentCompat
import com.rajatnagpure.pcmplayerconverter.analytics.AnalyticsEvents
import com.rajatnagpure.pcmplayerconverter.analytics.AnalyticsTracker
import com.rajatnagpure.pcmplayerconverter.domain.model.AudioConfig
import com.rajatnagpure.pcmplayerconverter.domain.usecase.ConvertAudioToPcmUseCase
import com.rajatnagpure.pcmplayerconverter.domain.usecase.ConvertPcmUseCase
import dagger.hilt.android.AndroidEntryPoint
import java.io.File
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

@AndroidEntryPoint
class ConversionService : Service() {

    @Inject
    lateinit var convertPcmUseCase: ConvertPcmUseCase

    @Inject
    lateinit var convertAudioToPcmUseCase: ConvertAudioToPcmUseCase

    @Inject
    lateinit var conversionEvents: ConversionEvents

    @Inject
    lateinit var analytics: AnalyticsTracker

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val inFile = intent?.let { IntentCompat.getSerializableExtra(it, EXTRA_IN_FILE, File::class.java) }
        val outUriString = intent?.getStringExtra(EXTRA_OUT_URI)
        val outFile = intent?.let { IntentCompat.getSerializableExtra(it, EXTRA_OUT_FILE, File::class.java) } // Legacy/Internal support
        val config = intent?.let { IntentCompat.getParcelableExtra(it, EXTRA_CONFIG, AudioConfig::class.java) }
        val task = intent?.getStringExtra(EXTRA_TASK) ?: TASK_PCM_TO_FORMAT
        val origin = intent?.getStringExtra(EXTRA_ORIGIN)
            ?.let { runCatching { ConversionOrigin.valueOf(it) }.getOrNull() }
            ?: if (task == TASK_AUDIO_TO_PCM) ConversionOrigin.GENERATOR else ConversionOrigin.CONVERTER
        val jobId = intent?.getLongExtra(EXTRA_JOB_ID, 0L)?.takeIf { it != 0L } ?: System.currentTimeMillis()

        if (inFile != null && (outUriString != null || outFile != null)) {
            ServiceCompat.startForeground(
                this,
                ConversionNotifications.PROGRESS_NOTIFICATION_ID,
                ConversionNotifications.progress(this),
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC else 0
            )

            val eventParams = conversionParams(task, inFile, config)
            analytics.logEvent(AnalyticsEvents.CONVERSION_START, eventParams)
            val startedAt = SystemClock.elapsedRealtime()

            serviceScope.launch {
                var tempFile: File? = null
                var stage = "convert"
                try {
                    // unexpected but possible: use provided outFile or create a temp one
                    val workingFile = if (outFile != null) {
                        outFile
                    } else {
                        // Create a temp file in cache for the conversion process
                        tempFile = File(cacheDir, "temp_conversion_${System.currentTimeMillis()}")
                        tempFile
                    }

                    val result = if (task == TASK_AUDIO_TO_PCM) {
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
                    val outputBytes = workingFile.length()

                    // If we used a temp file and have a target URI, copy the result there
                    stage = "copy_out"
                    val outUri = outUriString?.let { Uri.parse(it) }
                    if (outUri != null && tempFile != null && workingFile.exists()) {
                        val stream = contentResolver.openOutputStream(outUri)
                            ?: throw IllegalStateException("Could not open the selected file for writing")
                        stream.use { outputStream ->
                            java.io.FileInputStream(workingFile).use { inputStream ->
                                inputStream.copyTo(outputStream)
                            }
                        }
                    }

                    val outputName = outUri?.let { displayNameOf(it) } ?: outFile?.name
                    analytics.logEvent(
                        AnalyticsEvents.CONVERSION_COMPLETE,
                        eventParams + mapOf(
                            AnalyticsEvents.P_DURATION_MS to SystemClock.elapsedRealtime() - startedAt,
                            AnalyticsEvents.P_OUTPUT_SIZE_BUCKET to AnalyticsEvents.sizeBucket(outputBytes)
                        )
                    )
                    analytics.setUserProperty(AnalyticsEvents.UP_HAS_CONVERTED, "true")

                    conversionEvents.publish(ConversionResult(jobId, origin, success = true, outputName = outputName))
                    ConversionNotifications.post(
                        this@ConversionService,
                        ConversionNotifications.RESULT_NOTIFICATION_ID,
                        ConversionNotifications.completed(this@ConversionService, outputName, outUri)
                    )
                } catch (e: Exception) {
                    analytics.logEvent(
                        AnalyticsEvents.CONVERSION_FAILED,
                        eventParams + mapOf(
                            AnalyticsEvents.P_ERROR_TYPE to AnalyticsEvents.errorType(e),
                            AnalyticsEvents.P_STAGE to stage,
                            AnalyticsEvents.P_DURATION_MS to SystemClock.elapsedRealtime() - startedAt
                        )
                    )
                    analytics.recordNonFatal(e, mapOf("task" to task, "stage" to stage))

                    conversionEvents.publish(ConversionResult(jobId, origin, success = false, errorMessage = e.message))
                    ConversionNotifications.post(
                        this@ConversionService,
                        ConversionNotifications.RESULT_NOTIFICATION_ID,
                        ConversionNotifications.failed(this@ConversionService, e.message)
                    )
                } finally {
                    // Clean up temp file
                    tempFile?.delete()
                    // Only the progress notification is removed; the result notification stays.
                    ServiceCompat.stopForeground(this@ConversionService, ServiceCompat.STOP_FOREGROUND_REMOVE)
                    stopSelf(startId)
                }
            }
        } else {
            stopSelf(startId)
        }
        return START_NOT_STICKY
    }

    private fun conversionParams(task: String, inFile: File, config: AudioConfig?): Map<String, Any?> {
        val base = mapOf(
            AnalyticsEvents.P_FILE_EXT to AnalyticsEvents.extensionOf(inFile.name),
            AnalyticsEvents.P_SIZE_BUCKET to AnalyticsEvents.sizeBucket(inFile.length())
        )
        return if (task == TASK_AUDIO_TO_PCM) {
            base + (AnalyticsEvents.P_DIRECTION to AnalyticsEvents.DIRECTION_AUDIO_TO_PCM)
        } else {
            base + mapOf(
                AnalyticsEvents.P_DIRECTION to AnalyticsEvents.DIRECTION_PCM_TO_AUDIO,
                AnalyticsEvents.P_OUTPUT_FORMAT to config?.outputFormat?.extension,
                AnalyticsEvents.P_SAMPLE_RATE to config?.sampleRate,
                AnalyticsEvents.P_CHANNELS to config?.channels,
                AnalyticsEvents.P_ENCODING to config?.encoding?.name?.lowercase()
            )
        }
    }

    private fun displayNameOf(uri: Uri): String? = runCatching {
        contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) cursor.getString(0) else null
        }
    }.getOrNull() ?: uri.lastPathSegment?.substringAfterLast('/')

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
    }

    companion object {
        const val EXTRA_IN_FILE = "inFile"
        const val EXTRA_OUT_URI = "outUri"
        const val EXTRA_OUT_FILE = "outFile"
        const val EXTRA_CONFIG = "config"
        const val EXTRA_TASK = "task"
        const val EXTRA_ORIGIN = "origin"
        const val EXTRA_JOB_ID = "jobId"
        const val TASK_PCM_TO_FORMAT = "PCM_TO_FORMAT"
        const val TASK_AUDIO_TO_PCM = "AUDIO_TO_PCM"
    }
}
