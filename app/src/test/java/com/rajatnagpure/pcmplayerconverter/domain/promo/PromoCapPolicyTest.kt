package com.rajatnagpure.pcmplayerconverter.domain.promo

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.concurrent.TimeUnit

class PromoCapPolicyTest {

    private val day = TimeUnit.DAYS.toMillis(1)
    private val now = 1_000L * day
    private val config = PromoConfig.DEFAULT
    private val eligible = PromoState(sessionCount = 2)

    private fun show(state: PromoState = eligible, cfg: PromoConfig = config, installed: Boolean = false) =
        PromoCapPolicy.shouldShow(state, cfg, now, installed)

    @Test fun `shows for an eligible returning user`() = assertTrue(show())

    @Test fun `hidden on first session`() = assertFalse(show(eligible.copy(sessionCount = 1)))

    @Test fun `hidden when remotely disabled`() = assertFalse(show(cfg = config.copy(enabled = false)))

    @Test fun `hidden when game already installed`() = assertFalse(show(installed = true))

    @Test fun `hidden forever after a click`() = assertFalse(show(eligible.copy(clicked = true)))

    @Test fun `hidden after max impressions`() {
        assertTrue(show(eligible.copy(impressions = config.maxImpressions - 1)))
        assertFalse(show(eligible.copy(impressions = config.maxImpressions)))
    }

    @Test fun `hidden after max dismissals`() {
        assertFalse(show(eligible.copy(dismissCount = config.maxDismissals)))
    }

    @Test fun `respects snooze after dismiss`() {
        assertFalse(show(eligible.copy(dismissCount = 1, snoozeUntil = now + 1)))
        assertTrue(show(eligible.copy(dismissCount = 1, snoozeUntil = now)))
    }

    @Test fun `respects cooldown between showing sessions`() {
        val cooldown = config.cooldownDays * day
        assertFalse(show(eligible.copy(impressions = 1, lastShownAt = now - cooldown + 1)))
        assertTrue(show(eligible.copy(impressions = 1, lastShownAt = now - cooldown)))
    }
}
