package com.rajatnagpure.pcmplayerconverter.di

import com.rajatnagpure.pcmplayerconverter.analytics.AnalyticsTracker
import com.rajatnagpure.pcmplayerconverter.analytics.FirebaseAnalyticsTracker
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
abstract class AnalyticsModule {

    @Binds
    abstract fun bindAnalyticsTracker(impl: FirebaseAnalyticsTracker): AnalyticsTracker
}
