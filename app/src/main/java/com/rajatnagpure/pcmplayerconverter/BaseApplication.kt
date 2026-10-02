package com.rajatnagpure.pcmplayerconverter

import android.app.Application
import com.rajatnagpure.pcmplayerconverter.analytics.FirebaseAnalyticsTracker
import com.rajatnagpure.pcmplayerconverter.data.repository.PromoRepository
import com.rajatnagpure.pcmplayerconverter.data.repository.RemoteConfigRepository
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class BaseApplication : Application() {

    @Inject lateinit var analyticsTracker: FirebaseAnalyticsTracker
    @Inject lateinit var promoRepository: PromoRepository
    @Inject lateinit var remoteConfigRepository: RemoteConfigRepository

    override fun onCreate() {
        super.onCreate()
        // Respect the user's opt-out (and keep debug builds out of production data)
        analyticsTracker.applyCollectionState()
        promoRepository.incrementSessionCount()
        remoteConfigRepository.fetchAndActivate()
    }
}
