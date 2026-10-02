package com.rajatnagpure.pcmplayerconverter.data.repository

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
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

    /** In-memory: whether the banner has already been shown during this process. */
    var shownThisSession: Boolean = false
        private set

    /** In-memory: user closed or clicked the banner during this process. */
    var closedThisSession: Boolean = false
        private set

    fun state(): PromoState = PromoState(
        sessionCount = prefs.getInt(KEY_SESSION_COUNT, 0),
        impressions = prefs.getInt(KEY_IMPRESSIONS, 0),
        lastShownAt = prefs.getLong(KEY_LAST_SHOWN_AT, 0L),
        dismissCount = prefs.getInt(KEY_DISMISS_COUNT, 0),
        snoozeUntil = prefs.getLong(KEY_SNOOZE_UNTIL, 0L),
        clicked = prefs.getBoolean(KEY_CLICKED, false)
    )

    fun incrementSessionCount() {
        prefs.edit { putInt(KEY_SESSION_COUNT, prefs.getInt(KEY_SESSION_COUNT, 0) + 1) }
    }

    /** Returns the new lifetime impression count. */
    fun recordImpression(nowMs: Long): Int {
        val count = prefs.getInt(KEY_IMPRESSIONS, 0) + 1
        prefs.edit {
            putInt(KEY_IMPRESSIONS, count)
            putLong(KEY_LAST_SHOWN_AT, nowMs)
        }
        shownThisSession = true
        return count
    }

    fun recordDismiss(nowMs: Long, snoozeDays: Int) {
        prefs.edit {
            putInt(KEY_DISMISS_COUNT, prefs.getInt(KEY_DISMISS_COUNT, 0) + 1)
            putLong(KEY_SNOOZE_UNTIL, nowMs + TimeUnit.DAYS.toMillis(snoozeDays.toLong()))
        }
        closedThisSession = true
    }

    fun recordClick() {
        prefs.edit { putBoolean(KEY_CLICKED, true) }
        closedThisSession = true
    }

    companion object {
        private const val PREFS_NAME = "pcm_settings"
        private const val KEY_SESSION_COUNT = "session_count"
        private const val KEY_IMPRESSIONS = "promo_floodfill_impressions"
        private const val KEY_LAST_SHOWN_AT = "promo_floodfill_last_shown_at"
        private const val KEY_DISMISS_COUNT = "promo_floodfill_dismiss_count"
        private const val KEY_SNOOZE_UNTIL = "promo_floodfill_snooze_until"
        private const val KEY_CLICKED = "promo_floodfill_clicked"
    }
}
