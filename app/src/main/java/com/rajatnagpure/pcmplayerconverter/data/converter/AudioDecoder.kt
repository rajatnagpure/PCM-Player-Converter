package com.rajatnagpure.pcmplayerconverter.data.converter

import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import java.io.File
import java.io.FileOutputStream
import java.nio.ByteBuffer
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AudioDecoder @Inject constructor() {

    fun decodeToPcm(inputFile: File, outputFile: File): File {
        val extractor = MediaExtractor()
        try {
            extractor.setDataSource(inputFile.absolutePath)
        } catch (e: Exception) {
            throw Exception("Failed to set data source", e)
        }

        var trackIndex = -1
        var format: MediaFormat? = null
        for (i in 0 until extractor.trackCount) {
            val f = extractor.getTrackFormat(i)
            val mime = f.getString(MediaFormat.KEY_MIME)
            if (mime?.startsWith("audio/") == true) {
                trackIndex = i
                format = f
                break
            }
        }

        if (trackIndex < 0 || format == null) {
            throw Exception("No audio track found")
        }

        extractor.selectTrack(trackIndex)
        val mime = format.getString(MediaFormat.KEY_MIME) ?: throw Exception("Mime type definition failed")
        val codec = MediaCodec.createDecoderByType(mime)
        codec.configure(format, null, null, 0)
        codec.start()

        val bufferInfo = MediaCodec.BufferInfo()
        val outputStream = FileOutputStream(outputFile)
        var sawInputEOS = false
        var sawOutputEOS = false
        val timeoutUs = 5000L

        try {
            while (!sawOutputEOS) {
                if (!sawInputEOS) {
                    val inputBufferId = codec.dequeueInputBuffer(timeoutUs)
                    if (inputBufferId >= 0) {
                        val inputBuffer = codec.getInputBuffer(inputBufferId)
                        val sampleSize = inputBuffer?.let { extractor.readSampleData(it, 0) } ?: -1
                        if (sampleSize < 0) {
                            codec.queueInputBuffer(inputBufferId, 0, 0, 0L, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
                            sawInputEOS = true
                        } else {
                            val presentationTimeUs = extractor.sampleTime
                            codec.queueInputBuffer(inputBufferId, 0, sampleSize, presentationTimeUs, 0)
                            extractor.advance()
                        }
                    }
                }

                val outputBufferId = codec.dequeueOutputBuffer(bufferInfo, timeoutUs)
                if (outputBufferId >= 0) {
                    if ((bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                        sawOutputEOS = true
                    }
                    if (bufferInfo.size > 0) {
                        val outputBuffer = codec.getOutputBuffer(outputBufferId)
                        if (outputBuffer != null) {
                            val chunk = ByteArray(bufferInfo.size)
                            outputBuffer.get(chunk)
                            outputBuffer.clear()
                            outputStream.write(chunk)
                        }
                    }
                    codec.releaseOutputBuffer(outputBufferId, false)
                }
            }
        } catch (e: Exception) {
            throw e
        } finally {
            codec.stop()
            codec.release()
            extractor.release()
            outputStream.close()
        }
        return outputFile
    }
}
