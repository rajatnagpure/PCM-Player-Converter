package com.rajatnagpure.pcmplayerconverter.di

import android.content.Context
import com.rajatnagpure.pcmplayerconverter.BaseApplication
import com.rajatnagpure.pcmplayerconverter.data.audio.PcmPlayer
import com.rajatnagpure.pcmplayerconverter.data.audio.PcmRecorder
import com.rajatnagpure.pcmplayerconverter.data.converter.AudioEncoder
import com.rajatnagpure.pcmplayerconverter.data.converter.AudioDecoder
import com.rajatnagpure.pcmplayerconverter.data.converter.PcmConverter
import com.rajatnagpure.pcmplayerconverter.data.repository.AudioRepositoryImpl
import com.rajatnagpure.pcmplayerconverter.domain.repository.AudioRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideApplication(@ApplicationContext app: Context): BaseApplication {
        return app as BaseApplication
    }

    @Provides
    @Singleton
    fun provideAudioRepository(
        pcmConverter: PcmConverter,
        audioEncoder: AudioEncoder,
        audioDecoder: AudioDecoder,
        pcmPlayer: PcmPlayer,
        pcmRecorder: PcmRecorder
    ): AudioRepository {
        return AudioRepositoryImpl(pcmConverter, audioEncoder, audioDecoder, pcmPlayer, pcmRecorder)
    }
}
