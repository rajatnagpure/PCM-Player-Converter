package com.example.pcmplayerconverter

import android.media.AudioFormat

object Constants {
    const val RECORDER_SAMPLERATE = 8000
    const val RECORDER_CHANNELS: Int = AudioFormat.CHANNEL_IN_MONO
    const val RECORDER_AUDIO_ENCODING: Int = AudioFormat.ENCODING_PCM_16BIT
    const val BufferElements2Rec = 1024 // want to play 2048 (2K) since 2 bytes we use only 1024
    const val BytesPerElement = 2 // 2 bytes in 16bit format
}