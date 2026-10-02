package com.rajatnagpure.pcmplayerconverter.data.repository

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import com.rajatnagpure.pcmplayerconverter.domain.promo.PromoCapPolicy
import com.rajatnagpure.pcmplayerconverter.domain.promo.PromoConfig
import com.rajatnagpure.pcmplayerconverter.domain.promo.PromoState
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Persists promo-banner history in the shared "pcm_settings" prefs.
 * A "session" is one app process (cold start), counted from BaseApplication.onCreate.
 */
@Singleton
class PromoRepository @Inject constructor(
    @ApplicationContext context: Context
) {
    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    /** In-memory: an impression was already logged during this process (analytics only). */
    var impressionLoggedThisSession: Boolean = false
        private set

    fun state(): PromoState = PromoState(
        sessionCount = prefs.getInt(KEY_SESSION_COUNT, 0),
        dismissals = prefs.getString(KEY_DISMISSALS, null)
            ?.split(',')?.mapNotNull { it.toLongOrNull() }.orEmpty()
    )

    fun incrementSessionCount() {
        prefs.edit { putInt(KEY_SESSION_COUNT, prefs.getInt(KEY_SESSION_COUNT, 0) + 1) }
    }

    /** Lifetime impression counter, used only to number promo_impression events. */
    fun recordImpression(): Int {
        val count = prefs.getInt(KEY_IMPRESSIONS, 0) + 1
        prefs.edit { putInt(KEY_IMPRESSIONS, count) }
        impressionLoggedThisSession = true
        return count
    }

    fun impressions(): Int = prefs.getInt(KEY_IMPRESSIONS, 0)

    /**
     * Records a ✕ tap. Returns true when this tap reaches the dismissal cap, i.e. the banner now
     * stays hidden until the oldest dismissal in the window is more than the window old.
     */
    fun recordDismiss(nowMs: Long, config: PromoConfig): Boolean {
        // Keep only dismissals still inside the window; older ones no longer count.
        val window = TimeUnit.DAYS.toMillis(config.dismissWindowDays.toLong())
        val dismissals = (state().dismissals + nowMs).filter { nowMs - it < window }
        prefs.edit { putString(KEY_DISMISSALS, dismissals.joinToString(",")) }
        return PromoCapPolicy.isDismissCapped(dismissals, config, nowMs)
    }

    companion object {
        private const val PREFS_NAME = "pcm_settings"
        private const val KEY_SESSION_COUNT = "session_count"
        private const val KEY_IMPRESSIONS = "promo_floodfill_impressions"
        private const val KEY_DISMISSALS = "promo_floodfill_dismissals"
    }
}
