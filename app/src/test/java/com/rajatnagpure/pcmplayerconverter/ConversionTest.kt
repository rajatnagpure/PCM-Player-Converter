package com.rajatnagpure.pcmplayerconverter

import org.junit.Test
import org.junit.Assert.*
import java.io.File
import java.io.FileOutputStream

class ConversionTest {

    @Test
    fun testRawToWaveLogic() {
        val conversionFunctions = ConversionFunctions()
        val tempPcmFile = File.createTempFile("test", ".pcm")
        val tempWavFile = File.createTempFile("test", ".wav")

        // Write some dummy data to PCM file
        FileOutputStream(tempPcmFile).use { fos ->
            fos.write(ByteArray(1024))
        }

        try {
            conversionFunctions.rawToWave(8000, 16, 1, tempPcmFile, tempWavFile)
            assertTrue(tempWavFile.exists())
            assertTrue(tempWavFile.length() > 1024) // Header + Data
        } finally {
            tempPcmFile.delete()
            tempWavFile.delete()
        }
    }
}
