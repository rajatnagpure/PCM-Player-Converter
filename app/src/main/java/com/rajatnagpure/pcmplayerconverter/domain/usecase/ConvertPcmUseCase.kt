package com.rajatnagpure.pcmplayerconverter.domain.usecase

import com.rajatnagpure.pcmplayerconverter.domain.model.AudioConfig
import com.rajatnagpure.pcmplayerconverter.domain.repository.AudioRepository
import java.io.File
import javax.inject.Inject

class ConvertPcmUseCase @Inject constructor(
    private val repository: AudioRepository
) {
    suspend operator fun invoke(pcmFile: File, outputFile: File, config: AudioConfig): Result<File> {
        return repository.convertPcm(pcmFile, outputFile, config)
    }
}

class ConvertAudioToPcmUseCase @Inject constructor(
    private val repository: AudioRepository
) {
    suspend operator fun invoke(inputFile: File, pcmFile: File): Result<File> {
        return repository.convertAudioToPcm(inputFile, pcmFile)
    }
}

class RecordAudioUseCase @Inject constructor(
    private val repository: AudioRepository
) {
    suspend fun start(outputFile: File, config: AudioConfig) {
        repository.startRecording(outputFile, config)
    }
    
    suspend fun stop() {
        repository.stopRecording()
    }
    
    fun isRecording() = repository.isRecording()

    val amplitudeFlow = repository.amplitudeFlow
}

class PlayAudioUseCase @Inject constructor(
    private val repository: AudioRepository
) {
    suspend fun play(file: File, config: AudioConfig) {
        repository.playAudio(file, config)
    }
    
    suspend fun stop() {
        repository.stopAudio()
    }
    
    fun isPlaying() = repository.isPlaying()
}
