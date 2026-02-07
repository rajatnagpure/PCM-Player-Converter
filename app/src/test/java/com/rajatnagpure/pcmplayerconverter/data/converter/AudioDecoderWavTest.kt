package com.rajatnagpure.pcmplayerconverter.data.converter

import org.junit.Assert.*
import org.junit.Test
import java.io.File
import java.io.FileOutputStream

class AudioDecoderWavTest {

    // Helper to create a minimal PCM WAV file (mono, 16-bit, 44.1kHz) with provided sample bytes
    private fun createMinimalWav(file: File, pcmData: ByteArray) {
        FileOutputStream(file).use { out ->
            // RIFF header
            out.write("RIFF".toByteArray(Charsets.US_ASCII))
            val fileSizeMinus8 = 36 + pcmData.size // 36 + data
            out.write(intToLittleEndian(fileSizeMinus8))
            out.write("WAVE".toByteArray(Charsets.US_ASCII))

            // fmt chunk (PCM)
            out.write("fmt ".toByteArray(Charsets.US_ASCII))
            out.write(intToLittleEndian(16)) // chunk size
            out.write(shortToLittleEndian(1)) // audio format = 1 (PCM)
            out.write(shortToLittleEndian(1)) // channels = 1 (mono)
            out.write(intToLittleEndian(44100)) // sample rate
            val byteRate = 44100 * 1 * 16 / 8
            out.write(intToLittleEndian(byteRate)) // byte rate
            val blockAlign = (1 * 16 / 8)
            out.write(shortToLittleEndian(blockAlign)) // block align
            out.write(shortToLittleEndian(16)) // bits per sample

            // data chunk
            out.write("data".toByteArray(Charsets.US_ASCII))
            out.write(intToLittleEndian(pcmData.size))
            out.write(pcmData)
        }
    }

    private fun intToLittleEndian(v: Int): ByteArray {
        return byteArrayOf((v and 0xff).toByte(), ((v shr 8) and 0xff).toByte(), ((v shr 16) and 0xff).toByte(), ((v shr 24) and 0xff).toByte())
    }

    private fun shortToLittleEndian(v: Int): ByteArray {
        return byteArrayOf((v and 0xff).toByte(), ((v shr 8) and 0xff).toByte())
    }

    @Test
    fun `decodeToPcm extracts raw PCM bytes from WAV`() {
        val tempDir = System.getProperty("java.io.tmpdir")
        val inFile = File.createTempFile("test-wav", ".wav", File(tempDir))
        val outFile = File.createTempFile("out-pcm", ".pcm", File(tempDir))

        // create sample PCM bytes (simple ramp)
        val pcm = ByteArray(1024) { i -> ((i and 0xff).toByte()) }
        createMinimalWav(inFile, pcm)

        val decoder = AudioDecoder()
        val result = decoder.decodeToPcm(inFile, outFile)
        assertTrue(result.exists())
        assertEquals(pcm.size, result.length().toInt())

        val read = result.readBytes()
        assertArrayEquals(pcm, read)

        // cleanup
        inFile.delete()
        outFile.delete()
    }
}
