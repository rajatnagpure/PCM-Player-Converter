package com.rajatnagpure.pcmplayerconverter.data.audio

import android.annotation.SuppressLint
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import com.rajatnagpure.pcmplayerconverter.domain.model.AudioConfig
import com.rajatnagpure.pcmplayerconverter.domain.model.PcmEncoding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PcmRecorder @Inject constructor() {

    private var recorder: AudioRecord? = null
    private var isRecording = false
    
    private val _amplitude = kotlinx.coroutines.flow.MutableStateFlow(0f)
    val amplitudeFlow: kotlinx.coroutines.flow.StateFlow<Float> = _amplitude

    @SuppressLint("MissingPermission")
    suspend fun startRecording(outputFile: File, config: AudioConfig) {
        withContext(Dispatchers.IO) {
            if (isRecording) stopRecording()

            val channelConfig = if (config.channels == 1) AudioFormat.CHANNEL_IN_MONO else AudioFormat.CHANNEL_IN_STEREO
            val encoding = when (config.encoding) {
                PcmEncoding.BIT_8 -> AudioFormat.ENCODING_PCM_8BIT
                PcmEncoding.BIT_16 -> AudioFormat.ENCODING_PCM_16BIT
                PcmEncoding.FLOAT_32 -> AudioFormat.ENCODING_PCM_FLOAT
            }

            val minBufferSize = AudioRecord.getMinBufferSize(config.sampleRate, channelConfig, encoding)
            val bufferSize = if (minBufferSize > 0) minBufferSize * 2 else config.sampleRate * 2

            recorder = AudioRecord(
                MediaRecorder.AudioSource.MIC,
                config.sampleRate,
                channelConfig,
                encoding,
                bufferSize
            )

            if (recorder?.state != AudioRecord.STATE_INITIALIZED) {
                throw Exception("AudioRecord initialization failed. Ensure your device microphone is available and supports sample rate ${config.sampleRate}Hz.")
            }

            recorder?.startRecording()
            if (recorder?.recordingState != AudioRecord.RECORDSTATE_RECORDING) {
                throw Exception("Failed to start recording. The microphone might be in use by another application.")
            }
            isRecording = true

            val outputStream = FileOutputStream(outputFile)
            val buffer = ByteArray(bufferSize)

            try {
                while (isRecording) {
                    val read = recorder?.read(buffer, 0, buffer.size) ?: 0
                    if (read > 0) {
                        outputStream.write(buffer, 0, read)
                        
                        // Calculate amplitude for visualization (16-bit PCM assumption for now)
                        var max = 0
                        for (i in 0 until read step 2) {
                            if (i + 1 < read) {
                                val sample = (buffer[i+1].toInt() shl 8) or (buffer[i].toInt() and 0xFF)
                                val absSample = Math.abs(sample)
                                if (absSample > max) max = absSample
                            }
                        }
                        _amplitude.value = max.toFloat() / 32768f
                    } else if (read < 0) {
                        android.util.Log.e("PcmRecorder", "AudioRecord read error: $read")
                        if (read == AudioRecord.ERROR_INVALID_OPERATION || read == AudioRecord.ERROR_BAD_VALUE) {
                            break
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                outputStream.close()
            }
        }
    }

    fun stopRecording() {
        isRecording = false
        try {
            recorder?.stop()
            recorder?.release()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        recorder = null
        _amplitude.value = 0f
    }

    fun isRecording() = isRecording
}
