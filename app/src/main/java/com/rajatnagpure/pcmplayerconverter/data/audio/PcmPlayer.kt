package com.rajatnagpure.pcmplayerconverter.data.audio

import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import android.util.Log
import com.rajatnagpure.pcmplayerconverter.domain.model.AudioConfig
import com.rajatnagpure.pcmplayerconverter.domain.model.PcmEncoding
import java.io.File
import java.io.FileInputStream
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Singleton
class PcmPlayer @Inject constructor() {

    private var audioTrack: AudioTrack? = null
    
    private val _isPlaying = kotlinx.coroutines.flow.MutableStateFlow(false)
    val isPlayingFlow: kotlinx.coroutines.flow.StateFlow<Boolean> = _isPlaying

    private val _currentFile = kotlinx.coroutines.flow.MutableStateFlow<File?>(null)
    val currentFile: kotlinx.coroutines.flow.StateFlow<File?> = _currentFile

    private val _progress = kotlinx.coroutines.flow.MutableStateFlow(0f)
    val progressFlow: kotlinx.coroutines.flow.StateFlow<Float> = _progress

    private var seekRequested: Float? = null
    
    // Backwards compatibility for now (though we should migrate usages)
    private var isPlayingVar = false

    suspend fun play(file: File, config: AudioConfig) {
        withContext(Dispatchers.IO) {
            if (_isPlaying.value) stop()

            val channelConfig = if (config.channels == 1) AudioFormat.CHANNEL_OUT_MONO else AudioFormat.CHANNEL_OUT_STEREO
            val encoding = when (config.encoding) {
                PcmEncoding.BIT_8 -> AudioFormat.ENCODING_PCM_8BIT
                PcmEncoding.BIT_16 -> AudioFormat.ENCODING_PCM_16BIT
                PcmEncoding.FLOAT_32 -> AudioFormat.ENCODING_PCM_FLOAT
            }

            val bufferSize = AudioTrack.getMinBufferSize(config.sampleRate, channelConfig, encoding)
            
            // Ensure buffer size is valid
            val safeBufferSize = if (bufferSize > 0) bufferSize else config.sampleRate * 2

            audioTrack = AudioTrack(
                AudioManager.STREAM_MUSIC,
                config.sampleRate,
                channelConfig,
                encoding,
                safeBufferSize,
                AudioTrack.MODE_STREAM
            )

            audioTrack?.play()
            _isPlaying.value = true
            _currentFile.value = file
            isPlayingVar = true

            val totalBytes = file.length()
            val inputStream = FileInputStream(file)
            val buffer = ByteArray(safeBufferSize)
            var bytesReadTotal = 0L
            var readMsg: Int

            try {
                while (_isPlaying.value) {
                    val currentSeek = seekRequested
                    if (currentSeek != null) {
                        val seekPos = (currentSeek * totalBytes).toLong()
                        // Ensure alignment for PCM (e.g. 2 bytes per sample for 16-bit mono, 4 for stereo)
                        val bytesPerFrame = config.channels * (config.encoding.bitDepth / 8).coerceAtLeast(1)
                        val alignedSeekPos = (seekPos / bytesPerFrame) * bytesPerFrame
                        
                        try {
                            inputStream.channel.position(alignedSeekPos)
                            bytesReadTotal = alignedSeekPos
                            audioTrack?.flush()
                        } catch (e: Exception) {
                            Log.e("PcmPlayer", "Seek failed", e)
                        }
                        seekRequested = null
                    }

                    readMsg = inputStream.read(buffer)
                    if (readMsg == -1) break
                    
                    audioTrack?.write(buffer, 0, readMsg)
                    bytesReadTotal += readMsg
                    _progress.value = (bytesReadTotal.toFloat() / totalBytes).coerceIn(0f, 1f)
                }
            } catch (e: Exception) {
                Log.e("PcmPlayer", "Error playing", e)
            } finally {
                _progress.value = 0f
                inputStream.close()
                stop()
            }
        }
    }

    fun stop() {
        _isPlaying.value = false
        isPlayingVar = false
        try {
            audioTrack?.stop()
            audioTrack?.release()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        audioTrack = null
    }
    
    fun isPlaying() = isPlayingVar

    fun seekTo(progress: Float) {
        seekRequested = progress.coerceIn(0f, 1f)
    }
}
