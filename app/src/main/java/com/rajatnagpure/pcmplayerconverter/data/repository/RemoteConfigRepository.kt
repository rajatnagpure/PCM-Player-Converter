package com.rajatnagpure.pcmplayerconverter.data.repository

import android.content.Context
import android.util.Log
import com.google.firebase.FirebaseApp
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.firebase.remoteconfig.remoteConfigSettings
import com.rajatnagpure.pcmplayerconverter.BuildConfig
import com.rajatnagpure.pcmplayerconverter.R
import com.rajatnagpure.pcmplayerconverter.domain.promo.PromoConfig
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Thin wrapper over Firebase Remote Config (free tier). Values fetched now are activated
 * immediately but the promo decision for the current launch may already have been made, so
 * changes reliably apply from the next app start. Falls back to [PromoConfig.DEFAULT] when
 * Firebase is not configured.
 */
@Singleton
class RemoteConfigRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val remoteConfig: FirebaseRemoteConfig? by lazy {
        if (FirebaseApp.getApps(context).isEmpty()) {
            null
        } else {
            FirebaseRemoteConfig.getInstance().apply {
                setConfigSettingsAsync(remoteConfigSettings {
                    minimumFetchIntervalInSeconds = if (BuildConfig.DEBUG) 60 else FETCH_INTERVAL_SECONDS
                })
                setDefaultsAsync(R.xml.remote_config_defaults)
            }
        }
    }

    fun fetchAndActivate() {
        remoteConfig?.fetchAndActivate()?.addOnFailureListener { Log.w(TAG, "Remote config fetch failed", it) }
    }

    fun promoConfig(): PromoConfig {
        val rc = remoteConfig ?: return PromoConfig.DEFAULT
        return PromoConfig(
            enabled = rc.getBoolean(KEY_PROMO_ENABLED),
            minSessions = rc.getLong(KEY_MIN_SESSIONS).toInt(),
            cooldownDays = rc.getLong(KEY_COOLDOWN_DAYS).toInt(),
            dismissSnoozeDays = rc.getLong(KEY_SNOOZE_DAYS).toInt(),
            clickSnoozeDays = rc.getLong(KEY_CLICK_SNOOZE_DAYS).toInt(),
            maxImpressions = rc.getLong(KEY_MAX_IMPRESSIONS).toInt(),
            maxDismissals = rc.getLong(KEY_MAX_DISMISSALS).toInt(),
            maxClicks = rc.getLong(KEY_MAX_CLICKS).toInt()
        )
    }

    companion object {
        private const val TAG = "RemoteConfig"
        private const val FETCH_INTERVAL_SECONDS = 12L * 60 * 60
        const val KEY_PROMO_ENABLED = "promo_floodfill_enabled"
        const val KEY_MIN_SESSIONS = "promo_min_sessions"
        const val KEY_COOLDOWN_DAYS = "promo_cooldown_days"
        const val KEY_SNOOZE_DAYS = "promo_dismiss_snooze_days"
        const val KEY_MAX_IMPRESSIONS = "promo_max_impressions"
        const val KEY_MAX_DISMISSALS = "promo_max_dismissals"
        const val KEY_CLICK_SNOOZE_DAYS = "promo_click_snooze_days"
        const val KEY_MAX_CLICKS = "promo_max_clicks"
    }
}
