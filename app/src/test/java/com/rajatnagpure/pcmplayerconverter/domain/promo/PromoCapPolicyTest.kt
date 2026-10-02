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

    @Test fun `keeps showing on every launch with no impression limit`() =
        assertTrue(show(eligible.copy(sessionCount = 500)))

    @Test fun `hidden on first launch`() = assertFalse(show(eligible.copy(sessionCount = 1)))

    @Test fun `shown on first launch when min sessions is 1`() =
        assertTrue(show(eligible.copy(sessionCount = 1), cfg = config.copy(minSessions = 1)))

    @Test fun `hidden when remotely disabled`() = assertFalse(show(cfg = config.copy(enabled = false)))

    @Test fun `hidden once the game is installed`() = assertFalse(show(installed = true))

    @Test fun `comes back exactly 4 days after a dismissal`() {
        val snooze = config.dismissSnoozeDays * day
        assertFalse(show(eligible.copy(dismissals = listOf(now - snooze + 1))))
        assertTrue(show(eligible.copy(dismissals = listOf(now - snooze))))
    }

    @Test fun `two dismissals in the last 14 days keep it hidden`() {
        // dismissed on day -10 and day -6: snooze is over, but 2 taps are inside the window
        assertFalse(show(eligible.copy(dismissals = listOf(now - 10 * day, now - 6 * day))))
    }

    @Test fun `comes back once the older dismissal leaves the 14-day window`() {
        val window = config.dismissWindowDays * day
        assertFalse(show(eligible.copy(dismissals = listOf(now - window + 1, now - 6 * day))))
        assertTrue(show(eligible.copy(dismissals = listOf(now - window, now - 6 * day))))
    }

    @Test fun `isDismissCapped counts only the rolling window`() {
        val window = config.dismissWindowDays * day
        assertTrue(PromoCapPolicy.isDismissCapped(listOf(now - 4 * day, now), config, now))
        assertFalse(PromoCapPolicy.isDismissCapped(listOf(now - window, now), config, now))
        assertFalse(PromoCapPolicy.isDismissCapped(listOf(now), config, now))
    }
}
