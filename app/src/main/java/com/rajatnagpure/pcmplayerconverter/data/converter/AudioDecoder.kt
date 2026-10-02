package com.rajatnagpure.pcmplayerconverter.data.converter

import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.nio.ByteBuffer
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AudioDecoder @Inject constructor() {

    fun decodeToPcm(inputFile: File, outputFile: File): File {
        // Quick path for WAV files: WAV often already contains PCM data in a "data" chunk.
        if (inputFile.name.lowercase().endsWith(".wav") || looksLikeWav(inputFile)) {
            return extractPcmFromWav(inputFile, outputFile)
        }

        // Fallback for compressed formats: use MediaExtractor + MediaCodec pipeline
        val extractor = MediaExtractor()
        try {
            extractor.setDataSource(inputFile.absolutePath)
        } catch (e: Exception) {
            throw Exception("Failed to set data source for MediaExtractor: ${e.message}", e)
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
            extractor.release()
            throw Exception("No audio track found in file")
        }

        extractor.selectTrack(trackIndex)
        val mime = format.getString(MediaFormat.KEY_MIME) ?: run {
            extractor.release()
            throw Exception("Mime type definition failed")
        }

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
            throw Exception("Decoding failed: ${e.message}", e)
        } finally {
            try {
                codec.stop()
                codec.release()
            } catch (ignore: Exception) {}
            extractor.release()
            try { outputStream.close() } catch (ignore: Exception) {}
        }
        return outputFile
    }

    private fun looksLikeWav(file: File): Boolean {
        return try {
            val fis = FileInputStream(file)
            val header = ByteArray(12)
            val read = fis.read(header)
            fis.close()
            if (read < 12) return false
            val riff = String(header, 0, 4, Charsets.US_ASCII)
            val wave = String(header, 8, 4, Charsets.US_ASCII)
            riff == "RIFF" && wave == "WAVE"
        } catch (e: Exception) {
            false
        }
    }

    private fun extractPcmFromWav(inputFile: File, outputFile: File): File {
        var fis: FileInputStream? = null
        var fos: FileOutputStream? = null
        try {
            fis = FileInputStream(inputFile)
            fos = FileOutputStream(outputFile)

            // Read RIFF header (12 bytes)
            val header12 = ByteArray(12)
            val r = fis.read(header12)
            if (r < 12) throw Exception("Invalid WAV file: header too short")
            val riff = String(header12, 0, 4, Charsets.US_ASCII)
            val wave = String(header12, 8, 4, Charsets.US_ASCII)
            if (riff != "RIFF" || wave != "WAVE") throw Exception("Not a WAV file")

            // Iterate chunks until we find "data"
            val buf8 = ByteArray(8)
            var dataSize: Long = -1
            while (true) {
                val read = fis.read(buf8)
                if (read < 8) break
                val chunkId = String(buf8, 0, 4, Charsets.US_ASCII)
                val size = littleEndianInt(buf8, 4)
                if (chunkId == "data") {
                    dataSize = size.toLong()
                    break
                } else {
                    // skip this chunk (size may be large)
                    var toSkip = size.toLong()
                    // some chunks have odd sizes: pad byte
                    if (toSkip > 0) {
                        var skipped = 0L
                        while (skipped < toSkip) {
                            val s = fis.skip(toSkip - skipped)
                            if (s <= 0) break
                            skipped += s
                        }
                    }
                    // If chunk size was odd, skip pad byte
                    if (size % 2 == 1) fis.skip(1)
                }
            }

            if (dataSize < 0) {
                throw Exception("WAV data chunk not found")
            }

            // Copy raw PCM bytes from current position up to dataSize
            val buffer = ByteArray(16 * 1024)
            var remaining = dataSize
            while (remaining > 0) {
                val toRead = if (remaining > buffer.size) buffer.size else remaining.toInt()
                val readBytes = fis.read(buffer, 0, toRead)
                if (readBytes <= 0) break
                fos.write(buffer, 0, readBytes)
                remaining -= readBytes
            }

            return outputFile
        } catch (e: Exception) {
            throw Exception("Failed to extract PCM from WAV: ${e.message}", e)
        } finally {
            try { fis?.close() } catch (ignore: Exception) {}
            try { fos?.close() } catch (ignore: Exception) {}
        }
    }

    private fun littleEndianInt(bytes: ByteArray, offset: Int): Int {
        return (bytes[offset].toInt() and 0xff) or
            ((bytes[offset + 1].toInt() and 0xff) shl 8) or
            ((bytes[offset + 2].toInt() and 0xff) shl 16) or
            ((bytes[offset + 3].toInt() and 0xff) shl 24)
    }
}
