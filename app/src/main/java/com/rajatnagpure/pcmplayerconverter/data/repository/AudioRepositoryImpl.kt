package com.rajatnagpure.pcmplayerconverter.data.repository

import com.rajatnagpure.pcmplayerconverter.data.audio.PcmPlayer
import com.rajatnagpure.pcmplayerconverter.data.audio.PcmRecorder
import com.rajatnagpure.pcmplayerconverter.data.converter.AudioEncoder
import com.rajatnagpure.pcmplayerconverter.data.converter.AudioDecoder
import com.rajatnagpure.pcmplayerconverter.data.converter.PcmConverter
import com.rajatnagpure.pcmplayerconverter.domain.model.AudioConfig
import com.rajatnagpure.pcmplayerconverter.domain.repository.AudioRepository
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Singleton
class AudioRepositoryImpl @Inject constructor(
    private val pcmConverter: PcmConverter,
    private val audioEncoder: AudioEncoder,
    private val audioDecoder: AudioDecoder,
    private val pcmPlayer: PcmPlayer,
    private val pcmRecorder: PcmRecorder
) : AudioRepository {

    override suspend fun convertPcm(pcmFile: File, outputFile: File, config: AudioConfig): Result<File> {
        return withContext(Dispatchers.IO) {
            try {
                if (!pcmFile.exists()) {
                    return@withContext Result.failure(Exception("PCM File not found"))
                }
                
                val resultFile = when (config.outputFormat) {
                    com.rajatnagpure.pcmplayerconverter.domain.model.AudioOutputFormat.WAV -> 
                        pcmConverter.rawToWave(config, pcmFile, outputFile)
                    com.rajatnagpure.pcmplayerconverter.domain.model.AudioOutputFormat.M4A,
                    com.rajatnagpure.pcmplayerconverter.domain.model.AudioOutputFormat.FLAC ->
                        audioEncoder.encode(config, pcmFile, outputFile)
                }
                Result.success(resultFile)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    override suspend fun convertAudioToPcm(inputFile: File, pcmFile: File): Result<File> {
        return withContext(Dispatchers.IO) {
            try {
                if (!inputFile.exists()) {
                    return@withContext Result.failure(Exception("Input File not found"))
                }
                val resultFile = audioDecoder.decodeToPcm(inputFile, pcmFile)
                Result.success(resultFile)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    override suspend fun startRecording(outputFile: File, config: AudioConfig) {
        pcmRecorder.startRecording(outputFile, config)
    }

    override suspend fun stopRecording() {
        pcmRecorder.stopRecording()
    }

    override suspend fun playAudio(file: File, config: AudioConfig) {
        pcmPlayer.play(file, config)
    }

    override suspend fun stopAudio() {
        pcmPlayer.stop()
    }

    override fun isRecording(): Boolean = pcmRecorder.isRecording()

    override fun isPlaying(): Boolean = pcmPlayer.isPlaying()

    override val amplitudeFlow: kotlinx.coroutines.flow.StateFlow<Float> = pcmRecorder.amplitudeFlow
}
