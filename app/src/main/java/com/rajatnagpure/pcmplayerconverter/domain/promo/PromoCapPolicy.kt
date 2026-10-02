package com.rajatnagpure.pcmplayerconverter.domain.promo

import java.util.concurrent.TimeUnit

/** Tunables for the cross-promo banner; overridable from Firebase Remote Config. */
data class PromoConfig(
    val enabled: Boolean = true,
    val minSessions: Int = 2,
    val cooldownDays: Int = 3,
    val dismissSnoozeDays: Int = 14,
    val maxImpressions: Int = 5,
    val maxDismissals: Int = 2
) {
    companion object {
        val DEFAULT = PromoConfig()
    }
}

/** Persisted promo history. Timestamps are epoch millis, 0 = never. */
data class PromoState(
    val sessionCount: Int = 0,
    val impressions: Int = 0,
    val lastShownAt: Long = 0L,
    val dismissCount: Int = 0,
    val snoozeUntil: Long = 0L,
    val clicked: Boolean = false
)

/**
 * Frequency-capping rules for the promo banner, kept pure so every rule is unit-testable:
 *  1. Remote kill-switch.
 *  2. Never if the promoted app is already installed.
 *  3. Never again after a click, after [PromoConfig.maxDismissals] dismissals, or after
 *     [PromoConfig.maxImpressions] impressions.
 *  4. Not during the user's first [PromoConfig.minSessions] - 1 sessions.
 *  5. After a dismissal, snooze for [PromoConfig.dismissSnoozeDays].
 *  6. At most one showing session every [PromoConfig.cooldownDays].
 * Per-session (once per app launch) and "not while busy" rules are applied by the caller.
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
        if (state.clicked) return false
        if (state.dismissCount >= config.maxDismissals) return false
        if (state.impressions >= config.maxImpressions) return false
        if (state.sessionCount < config.minSessions) return false
        if (nowMs < state.snoozeUntil) return false
        if (state.lastShownAt > 0 && nowMs - state.lastShownAt < TimeUnit.DAYS.toMillis(config.cooldownDays.toLong())) {
            return false
        }
        return true
    }
}
