package com.rajatnagpure.pcmplayerconverter.analytics

import android.content.Context
import android.os.Bundle
import android.util.Log
import androidx.core.content.edit
import com.google.firebase.FirebaseApp
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.rajatnagpure.pcmplayerconverter.BuildConfig
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

private const val PREFS_NAME = "pcm_settings"
private const val KEY_ANALYTICS_OPT_IN = "analytics_opt_in"
private const val TAG = "Analytics"

@Singleton
class FirebaseAnalyticsTracker @Inject constructor(
    @ApplicationContext private val context: Context
) : AnalyticsTracker {

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    /** False when the app was built without google-services.json; every call becomes a no-op. */
    private val firebaseReady: Boolean by lazy { FirebaseApp.getApps(context).isNotEmpty() }
    private val analytics: FirebaseAnalytics by lazy { FirebaseAnalytics.getInstance(context) }

    override val isCollectionEnabled: Boolean
        get() = prefs.getBoolean(KEY_ANALYTICS_OPT_IN, true)

    override fun logEvent(name: String, params: Map<String, Any?>) {
        if (BuildConfig.DEBUG) Log.d(TAG, "event $name $params")
        if (!firebaseReady) return
        analytics.logEvent(name, params.toBundle())
    }

    override fun logScreen(screenName: String) {
        logEvent(
            FirebaseAnalytics.Event.SCREEN_VIEW,
            mapOf(
                FirebaseAnalytics.Param.SCREEN_NAME to screenName,
                FirebaseAnalytics.Param.SCREEN_CLASS to screenName
            )
        )
    }

    override fun setUserProperty(name: String, value: String?) {
        if (!firebaseReady) return
        analytics.setUserProperty(name, value?.take(36))
    }

    override fun recordNonFatal(throwable: Throwable, context: Map<String, String>) {
        if (BuildConfig.DEBUG) Log.w(TAG, "non-fatal $context", throwable)
        if (!firebaseReady) return
        val crashlytics = FirebaseCrashlytics.getInstance()
        for ((k, v) in context) crashlytics.setCustomKey(k, v)
        crashlytics.recordException(throwable)
    }

    override fun setCollectionEnabled(enabled: Boolean) {
        prefs.edit { putBoolean(KEY_ANALYTICS_OPT_IN, enabled) }
        applyCollectionState()
    }

    /** Call once at startup. Debug builds stay off unless built with -PanalyticsInDebug=true. */
    fun applyCollectionState() {
        if (!firebaseReady) return
        val effective = isCollectionEnabled && BuildConfig.ANALYTICS_ENABLED
        analytics.setAnalyticsCollectionEnabled(effective)
        FirebaseCrashlytics.getInstance().isCrashlyticsCollectionEnabled = effective
    }

    private fun Map<String, Any?>.toBundle(): Bundle = Bundle().apply {
        for ((key, value) in this@toBundle) {
            when (value) {
                null -> Unit
                is String -> putString(key, value.take(100))
                is Int -> putLong(key, value.toLong())
                is Long -> putLong(key, value)
                is Float -> putDouble(key, value.toDouble())
                is Double -> putDouble(key, value)
                is Boolean -> putString(key, value.toString())
                is Enum<*> -> putString(key, value.name.lowercase())
                else -> putString(key, value.toString().take(100))
            }
        }
    }
}
