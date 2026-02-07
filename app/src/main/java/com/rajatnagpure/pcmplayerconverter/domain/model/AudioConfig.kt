package com.rajatnagpure.pcmplayerconverter.domain.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class AudioConfig(
    val sampleRate: Int = 44100,
    val channels: Int = 2, // 1 for Mono, 2 for Stereo
    val encoding: PcmEncoding = PcmEncoding.BIT_16,
    val outputFormat: AudioOutputFormat = AudioOutputFormat.WAV,
    val enableVbr: Boolean = true // Variable Bitrate for AAC
) : Parcelable

@Parcelize
enum class PcmEncoding(val bitDepth: Int, val description: String) : Parcelable {
    BIT_8(8, "8-bit PCM"),
    BIT_16(16, "16-bit PCM"),
    FLOAT_32(32, "32-bit Float")
}

@Parcelize
enum class AudioOutputFormat(val extension: String, val description: String) : Parcelable {
    WAV("wav", "WAV (Lossless)"),
    M4A("m4a", "M4A (AAC)"),
    FLAC("flac", "FLAC (Lossless)")
}

val SUPPORTED_SAMPLE_RATES = listOf(8000, 11025, 16000, 22050, 32000, 44100, 48000, 88200, 96000, 176400, 192000)
