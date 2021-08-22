package com.rajatnagpure.pcmplayerconverter;

import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.util.Log;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import static android.content.ContentValues.TAG;
import static com.rajatnagpure.pcmplayerconverter.Constants.RECORDER_AUDIO_ENCODING;

public class ConversionFunctions {

    ConversionFunctions(){}

    public void rawToWave(int samplingRate, short bitsPerSample, short channels, final File rawFile, final File waveFile) throws IOException {

        byte[] rawData = new byte[(int) rawFile.length()];
        DataInputStream input = null;
        try {
            input = new DataInputStream(new FileInputStream(rawFile));
            input.read(rawData);
        } finally {
            if (input != null) {
                input.close();
            }
        }

        DataOutputStream output = null;
        try {
            output = new DataOutputStream(new FileOutputStream(waveFile));
            // WAVE header
            // see http://ccrma.stanford.edu/courses/422/projects/WaveFormat/
            writeString(output, "RIFF"); // chunk id
            writeInt(output, 36 + rawData.length); // chunk size
            writeString(output, "WAVE"); // format
            writeString(output, "fmt "); // subchunk 1 id
            writeInt(output, 16); // subchunk 1 size
            writeShort(output, (short) 1); // audio format (1 = PCM)
            writeShort(output, (short) channels); // number of channels
            writeInt(output, samplingRate); // sample rate
            writeInt(output, samplingRate * channels * bitsPerSample / 8); // byte rate
            writeShort(output, (short) ((short) channels * bitsPerSample / 8)); // block align
            writeShort(output, (short) bitsPerSample); // bits per sample
            writeString(output, "data"); // subchunk 2 id
            writeInt(output, rawData.length); // subchunk 2 size
            // Audio data (conversion big endian -> little endian)
            short[] shorts = new short[rawData.length / 2];
            ByteBuffer.wrap(rawData).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer().get(shorts);
            ByteBuffer bytes = ByteBuffer.allocate(shorts.length * 2);
            for (short s : shorts) {
                bytes.putShort(s);
            }

            output.write(fullyReadFileToBytes(rawFile));
        } finally {
            if (output != null) {
                output.close();
            }
        }
    }
    byte[] fullyReadFileToBytes(File f) throws IOException {
        int size = (int) f.length();
        byte bytes[] = new byte[size];
        byte tmpBuff[] = new byte[size];
        FileInputStream fis= new FileInputStream(f);
        try {

            int read = fis.read(bytes, 0, size);
            if (read < size) {
                int remain = size - read;
                while (remain > 0) {
                    read = fis.read(tmpBuff, 0, remain);
                    System.arraycopy(tmpBuff, 0, bytes, size - remain, read);
                    remain -= read;
                }
            }
        }  catch (IOException e){
            throw e;
        } finally {
            fis.close();
        }
        return bytes;
    }
    private void writeInt(final DataOutputStream output, final int value) throws IOException {
        output.write(value);
        output.write(value >> 8);
        output.write(value >> 16);
        output.write(value >> 24);
    }

    private void writeShort(final DataOutputStream output, final short value) throws IOException {
        output.write(value);
        output.write(value >> 8);
    }

    private void writeString(final DataOutputStream output, final String value) throws IOException {
        for (int i = 0; i < value.length(); i++) {
            output.write(value.charAt(i));
        }
    }

    public void startPlaying(String mFileName) {

        new Thread(new Runnable() {
            public void run() {

                try {

                    File file = new File(mFileName);

                    byte[] audioData = null;

                    InputStream inputStream = new FileInputStream(mFileName);
                    audioData = new byte[Constants.BufferElements2Rec];

                    AudioTrack mPlayer = new AudioTrack(AudioManager.STREAM_MUSIC, Constants.RECORDER_SAMPLERATE,
                            AudioFormat.CHANNEL_OUT_MONO, RECORDER_AUDIO_ENCODING,
                            Constants.BufferElements2Rec * Constants.BytesPerElement, AudioTrack.MODE_STREAM);


                    final float duration = (float) file.length() / Constants.RECORDER_SAMPLERATE / 2;

                    Log.i(TAG, "PLAYBACK AUDIO");
                    Log.i(TAG, String.valueOf(duration));


                    mPlayer.setPositionNotificationPeriod(Constants.RECORDER_SAMPLERATE / 10);
                    mPlayer.setNotificationMarkerPosition(Math.round(duration * Constants.RECORDER_SAMPLERATE));

                    mPlayer.play();

                    int i = 0;
                    while ((i = inputStream.read(audioData)) != -1) {
                        try {
                            mPlayer.write(audioData, 0, i);
                        } catch (Exception e) {
                            Log.e(TAG, "Exception: " + e.getLocalizedMessage());
                        }
                    }

                } catch (FileNotFoundException fe) {
                    Log.e(TAG, "File not found: " + fe.getLocalizedMessage());
                } catch (IOException io) {
                    Log.e(TAG, "IO Exception: " + io.getLocalizedMessage());
                }

            }

        }).start();
    }
}
