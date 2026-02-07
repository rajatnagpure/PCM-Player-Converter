package com.rajatnagpure.pcmplayerconverter.data.converter

import com.rajatnagpure.pcmplayerconverter.domain.model.AudioConfig
import java.io.DataOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.nio.ByteBuffer
import java.nio.ByteOrder
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PcmConverter @Inject constructor() {

    fun rawToWave(config: AudioConfig, rawFile: File, waveFile: File): File {
        val rawData = rawFile.readBytes()
        var output: DataOutputStream? = null
        try {
            output = DataOutputStream(FileOutputStream(waveFile))
            // WAVE header
            // see http://ccrma.stanford.edu/courses/422/projects/WaveFormat/
            writeString(output, "RIFF") // chunk id
            writeInt(output, 36 + rawData.size) // chunk size
            writeString(output, "WAVE") // format
            writeString(output, "fmt ") // subchunk 1 id
            writeInt(output, 16) // subchunk 1 size
            writeShort(output, 1.toShort()) // audio format (1 = PCM)
            writeShort(output, config.channels.toShort()) // number of channels
            writeInt(output, config.sampleRate) // sample rate
            
            val bitsPerSample = config.encoding.bitDepth
            val byteRate = config.sampleRate * config.channels * bitsPerSample / 8
            
            writeInt(output, byteRate) // byte rate
            writeShort(output, (config.channels * bitsPerSample / 8).toShort()) // block align
            writeShort(output, bitsPerSample.toShort()) // bits per sample
            writeString(output, "data") // subchunk 2 id
            writeInt(output, rawData.size) // subchunk 2 size

            // Audio data (conversion big endian -> little endian) if needed?
            // Original code did this:
            // short[] shorts = new short[rawData.length / 2];
            // ByteBuffer.wrap(rawData).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer().get(shorts);
            // ByteBuffer bytes = ByteBuffer.allocate(shorts.length * 2);
            // for (short s : shorts) { bytes.putShort(s); }
            // output.write(fullyReadFileToBytes(rawFile));
            
            // The original logic seems partially redundant or confusing. 
            // It reads bytes, converts to Little Endian shorts, then puts back to bytes, then writes the file content DIRECTLY.
            // If the input PCM is already Little Endian (standard), we just write it.
            // Android AudioRecord usually records in Little Endian (ENCODING_PCM_16BIT).
            // Example original code: output.write(fullyReadFileToBytes(rawFile));
            // It essentially ignored the ByteBuffer transformation it did above it?
            // Yes, `shorts` and `bytes` were created but `output.write` used `fullyReadFileToBytes(rawFile)`.
            // So I will just write the raw bytes.
            
            output.write(rawData)
        } finally {
            output?.close()
        }
        return waveFile
    }

    private fun writeInt(output: DataOutputStream, value: Int) {
        output.write(value)
        output.write(value shr 8)
        output.write(value shr 16)
        output.write(value shr 24)
    }

    private fun writeShort(output: DataOutputStream, value: Short) {
        output.write(value.toInt())
        output.write(value.toInt() shr 8)
    }

    private fun writeString(output: DataOutputStream, value: String) {
        for (i in 0 until value.length) {
            output.write(value[i].code)
        }
    }
}
