package com.rajatnagpure.pcmplayerconverter.domain.promo

import java.util.concurrent.TimeUnit

/** Tunables for the cross-promo banner; overridable from Firebase Remote Config. */
data class PromoConfig(
    val enabled: Boolean = true,
    /** The banner never shows before this app launch (2 = not on the first launch). */
    val minSessions: Int = 2,
    /** After the user taps ✕, the banner comes back after this many days. */
    val dismissSnoozeDays: Int = 4,
    /** While this many dismissals fall inside the last [dismissWindowDays], the banner stays hidden. */
    val maxDismissals: Int = 2,
    val dismissWindowDays: Int = 14
) {
    companion object {
        val DEFAULT = PromoConfig()
    }
}

/** Persisted promo history. Timestamps are epoch millis. */
data class PromoState(
    val sessionCount: Int = 0,
    /** Dismissal times, oldest first. */
    val dismissals: List<Long> = emptyList()
) {
    val lastDismissAt: Long get() = dismissals.lastOrNull() ?: 0L
}

/**
 * The banner is capped on dismissals only — impressions and Play taps never hide it:
 *  1. Remote kill-switch.
 *  2. Never once the promoted app is installed (re-checked whenever the app resumes).
 *  3. Not during the user's first [PromoConfig.minSessions] - 1 app launches.
 *  4. Hidden for [PromoConfig.dismissSnoozeDays] after each dismissal.
 *  5. Hidden while [PromoConfig.maxDismissals] dismissals fall within the last
 *     [PromoConfig.dismissWindowDays]; it returns once the older one leaves that window.
 * Nothing is permanent apart from the install check.
 * Otherwise it stays visible on every launch. "Not while busy" is applied by the caller.
 */
object PromoCapPolicy {

    fun shouldShow(
        state: PromoState,
        config: PromoConfig,
        nowMs: Long,
        isTargetInstalled: Boolean
    ): Boolean {
        if (!config.enabled) return false
        if (isTargetInstalled) return false
        if (state.sessionCount < config.minSessions) return false
        if (state.lastDismissAt > 0 && nowMs - state.lastDismissAt < days(config.dismissSnoozeDays)) return false
        if (isDismissCapped(state.dismissals, config, nowMs)) return false
        return true
    }

    /** True while [PromoConfig.maxDismissals] or more dismissals fall within the rolling window. */
    fun isDismissCapped(dismissals: List<Long>, config: PromoConfig, nowMs: Long): Boolean =
        dismissals.count { nowMs - it < days(config.dismissWindowDays) } >= config.maxDismissals

    private fun days(n: Int) = TimeUnit.DAYS.toMillis(n.toLong())
}
