package com.rajatnagpure.pcmplayerconverter.data.repository

import com.rajatnagpure.pcmplayerconverter.data.audio.PcmPlayer
import com.rajatnagpure.pcmplayerconverter.data.audio.PcmRecorder
import com.rajatnagpure.pcmplayerconverter.data.converter.AacEncoder
import com.rajatnagpure.pcmplayerconverter.data.converter.AudioDecoder
import com.rajatnagpure.pcmplayerconverter.data.converter.PcmConverter
import com.rajatnagpure.pcmplayerconverter.domain.model.AudioConfig
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import java.io.File
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class AudioRepositoryTest {

    private lateinit var repository: AudioRepositoryImpl
    private val pcmConverter: PcmConverter = mockk()
    private val aacEncoder: AacEncoder = mockk()
    private val audioDecoder: AudioDecoder = mockk()
    private val pcmPlayer: PcmPlayer = mockk(relaxed = true)
    private val pcmRecorder: PcmRecorder = mockk(relaxed = true)

    @Before
    fun setup() {
        repository = AudioRepositoryImpl(pcmConverter, aacEncoder, audioDecoder, pcmPlayer, pcmRecorder)
    }

    @Test
    fun `convertPcm uses WavConverter when format is WAV`() = runTest {
        // Arrange
        val pcmFile = mockk<File>()
        val wavFile = mockk<File>()
        val config = AudioConfig(outputFormat = com.rajatnagpure.pcmplayerconverter.domain.model.AudioOutputFormat.WAV)
        
        every { pcmFile.exists() } returns true
        every { pcmConverter.rawToWave(config, pcmFile, wavFile) } returns wavFile

        // Act
        val result = repository.convertPcm(pcmFile, wavFile, config)

        // Assert
        assertTrue(result.isSuccess)
        verify { pcmConverter.rawToWave(config, pcmFile, wavFile) }
        verify(exactly = 0) { aacEncoder.encodeToM4a(any(), any(), any()) }
    }

    @Test
    fun `convertPcm uses AacEncoder when format is M4A`() = runTest {
        // Arrange
        val pcmFile = mockk<File>()
        val m4aFile = mockk<File>()
        val config = AudioConfig(outputFormat = com.rajatnagpure.pcmplayerconverter.domain.model.AudioOutputFormat.M4A)
        
        every { pcmFile.exists() } returns true
        every { aacEncoder.encodeToM4a(config, pcmFile, m4aFile) } returns m4aFile

        // Act
        val result = repository.convertPcm(pcmFile, m4aFile, config)

        // Assert
        assertTrue(result.isSuccess)
        verify { aacEncoder.encodeToM4a(config, pcmFile, m4aFile) }
        verify(exactly = 0) { pcmConverter.rawToWave(any(), any(), any()) }
    }
    
    @Test
    fun `convertAudioToPcm calls audioDecoder`() = runTest {
        // Arrange
        val inputFile = mockk<File>()
        val pcmFile = mockk<File>()
        
        every { inputFile.exists() } returns true
        every { audioDecoder.decodeToPcm(inputFile, pcmFile) } returns pcmFile
        
        // Act
        val result = repository.convertAudioToPcm(inputFile, pcmFile)
        
        // Assert
        assertTrue(result.isSuccess)
        verify { audioDecoder.decodeToPcm(inputFile, pcmFile) }
    }

    @Test
    fun `startRecording calls recorder`() = runTest {
        val file = mockk<File>()
        val config = AudioConfig()
        
        repository.startRecording(file, config)
        
        coVerify { pcmRecorder.startRecording(file, config) }
    }
}
