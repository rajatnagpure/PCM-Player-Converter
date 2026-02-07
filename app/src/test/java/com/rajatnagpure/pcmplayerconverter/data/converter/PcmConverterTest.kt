package com.rajatnagpure.pcmplayerconverter.data.converter

import com.rajatnagpure.pcmplayerconverter.domain.model.AudioConfig
import com.rajatnagpure.pcmplayerconverter.domain.model.PcmEncoding
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class PcmConverterTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private val converter = PcmConverter()

    @Test
    fun `rawToWave generates correct wav header and file content`() {
        // Arrange
        val rawFile = tempFolder.newFile("test.pcm")
        val content = ByteArray(100) { it.toByte() }
        rawFile.writeBytes(content)
        
        val wavFile = tempFolder.newFile("test.wav")
        val config = AudioConfig(sampleRate = 44100, channels = 2, encoding = PcmEncoding.BIT_16)

        // Act
        val result = converter.rawToWave(config, rawFile, wavFile)

        // Assert
        assertTrue(result.exists())
        assertTrue(result.length() > content.size) // Header + content
        
        val wavBytes = result.readBytes()
        // Header is 44 bytes
        assertEquals(44 + content.size, wavBytes.size)
        
        // Check RIFF header
        assertEquals('R'.code.toByte(), wavBytes[0])
        assertEquals('I'.code.toByte(), wavBytes[1])
        assertEquals('F'.code.toByte(), wavBytes[2])
        assertEquals('F'.code.toByte(), wavBytes[3])
        
        // Check WAVE fmt
        assertEquals('W'.code.toByte(), wavBytes[8])
        assertEquals('A'.code.toByte(), wavBytes[9])
        assertEquals('V'.code.toByte(), wavBytes[10])
        assertEquals('E'.code.toByte(), wavBytes[11])
    }
}
