package com.rajatnagpure.pcmplayerconverter.data.converter

import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaFormat
import android.media.MediaMuxer
import com.rajatnagpure.pcmplayerconverter.domain.model.AudioConfig
import java.io.File
import java.io.FileInputStream
import java.nio.ByteBuffer
import java.util.concurrent.TimeoutException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AudioEncoder @Inject constructor() {

    fun encode(config: AudioConfig, pcmFile: File, outputFile: File): File {
        if (!pcmFile.exists()) {
            throw Exception("PCM file not found")
        }

        val mimeType = when (config.outputFormat) {
            com.rajatnagpure.pcmplayerconverter.domain.model.AudioOutputFormat.M4A -> "audio/mp4a-latm"
            com.rajatnagpure.pcmplayerconverter.domain.model.AudioOutputFormat.FLAC -> "audio/flac"
            else -> throw IllegalArgumentException("Unsupported format")
        }

        val codec = MediaCodec.createEncoderByType(mimeType)
        val format = MediaFormat.createAudioFormat(mimeType, config.sampleRate, config.channels)
        
        if (config.outputFormat == com.rajatnagpure.pcmplayerconverter.domain.model.AudioOutputFormat.M4A) {
            if (config.enableVbr) {
                format.setInteger(MediaFormat.KEY_BIT_RATE, 192000)
                format.setInteger(MediaFormat.KEY_AAC_PROFILE, MediaCodecInfo.CodecProfileLevel.AACObjectLC)
                format.setInteger("bitrate-mode", MediaCodecInfo.EncoderCapabilities.BITRATE_MODE_VBR) 
            } else {
                 format.setInteger(MediaFormat.KEY_BIT_RATE, 128000)
                 format.setInteger(MediaFormat.KEY_AAC_PROFILE, MediaCodecInfo.CodecProfileLevel.AACObjectLC)
            }
        } else if (config.outputFormat == com.rajatnagpure.pcmplayerconverter.domain.model.AudioOutputFormat.FLAC) {
            // FLAC doesn't need bitrate, it's lossless
            format.setInteger(MediaFormat.KEY_BIT_RATE, 128000) // ignored usually
            // Some devices need complexity
            format.setInteger(MediaFormat.KEY_FLAC_COMPRESSION_LEVEL, 5)
        }
        
        format.setInteger(MediaFormat.KEY_MAX_INPUT_SIZE, 16384)
        
        codec.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
        codec.start()

        // Muxer output format depends on container
        val muxerFormat = if (config.outputFormat == com.rajatnagpure.pcmplayerconverter.domain.model.AudioOutputFormat.FLAC) {
             // For .flac files, we might need a different muxer or just write frames.
             // On Android 10+, MediaMuxer supports OGG/WEBM. 
             // Let's use MPEG_4 which often supports FLAC too on modern devices.
             MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4 
        } else {
             MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4
        }

        val muxer = MediaMuxer(outputFile.absolutePath, muxerFormat)
        var trackIndex = -1
        var muxerStarted = false

        val bufferInfo = MediaCodec.BufferInfo()
        val fis = FileInputStream(pcmFile)
        val buffer = ByteArray(8192) // Process in chunks
        var inputDone = false
        var outputDone = false
        val timeoutUs = 10000L

        try {
            while (!outputDone) {
                if (!inputDone) {
                    val inputBufferId = codec.dequeueInputBuffer(timeoutUs)
                    if (inputBufferId >= 0) {
                        val inputBuffer = codec.getInputBuffer(inputBufferId)
                        val read = fis.read(buffer)
                        if (read == -1) {
                            codec.queueInputBuffer(inputBufferId, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
                            inputDone = true
                        } else {
                            inputBuffer?.clear()
                            inputBuffer?.put(buffer, 0, read)
                            codec.queueInputBuffer(inputBufferId, 0, read, System.nanoTime() / 1000, 0)
                        }
                    }
                }

                var outputBufferId = codec.dequeueOutputBuffer(bufferInfo, timeoutUs)
                while (outputBufferId >= 0) {
                    if ((bufferInfo.flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG) != 0) {
                        bufferInfo.size = 0
                    }

                    if (bufferInfo.size != 0) {
                        if (!muxerStarted) {
                            throw Exception("Muxer not started")
                        }
                        val outputBuffer = codec.getOutputBuffer(outputBufferId)
                        outputBuffer?.position(bufferInfo.offset)
                        outputBuffer?.limit(bufferInfo.offset + bufferInfo.size)
                        muxer.writeSampleData(trackIndex, outputBuffer!!, bufferInfo)
                    }

                    codec.releaseOutputBuffer(outputBufferId, false)
                    
                    if ((bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                        outputDone = true
                        break
                    }
                    outputBufferId = codec.dequeueOutputBuffer(bufferInfo, timeoutUs)
                }

                if (outputBufferId == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                    if (muxerStarted) {
                        throw Exception("format changed twice")
                    }
                    val newFormat = codec.outputFormat
                    trackIndex = muxer.addTrack(newFormat)
                    muxer.start()
                    muxerStarted = true
                }
            }
        } finally {
            fis.close()
            try {
                codec.stop()
                codec.release()
            } catch (e: Exception) { e.printStackTrace() }
            try {
                if (muxerStarted) {
                    muxer.stop()
                }
                muxer.release()
            } catch (e: Exception) { e.printStackTrace() }
        }
        
        return outputFile
    }
}
