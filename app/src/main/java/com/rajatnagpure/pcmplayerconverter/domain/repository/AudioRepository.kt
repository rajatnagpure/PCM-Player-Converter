package com.rajatnagpure.pcmplayerconverter.domain.repository

import com.rajatnagpure.pcmplayerconverter.domain.model.AudioConfig
import java.io.File

interface AudioRepository {
    suspend fun convertPcm(pcmFile: File, outputFile: File, config: AudioConfig): Result<File>
    suspend fun convertAudioToPcm(inputFile: File, pcmFile: File): Result<File>
    suspend fun startRecording(outputFile: File, config: AudioConfig)
    suspend fun stopRecording()
    suspend fun playAudio(file: File, config: AudioConfig) // For raw PCM
    suspend fun stopAudio()
    fun isRecording(): Boolean
    fun isPlaying(): Boolean
    val amplitudeFlow: kotlinx.coroutines.flow.StateFlow<Float>
}
