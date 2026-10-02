package com.rajatnagpure.pcmplayerconverter

import android.app.Application
import com.rajatnagpure.pcmplayerconverter.analytics.FirebaseAnalyticsTracker
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class BaseApplication : Application() {

    @Inject lateinit var analyticsTracker: FirebaseAnalyticsTracker

    override fun onCreate() {
        super.onCreate()
        // Respect the user's opt-out (and keep debug builds out of production data)
        analyticsTracker.applyCollectionState()
    }
}
