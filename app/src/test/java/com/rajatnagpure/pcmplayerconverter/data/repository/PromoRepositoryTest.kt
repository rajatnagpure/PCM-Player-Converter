package com.rajatnagpure.pcmplayerconverter.data.repository

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.util.concurrent.TimeUnit

@RunWith(RobolectricTestRunner::class)
class PromoRepositoryTest {

    private val context: Context = ApplicationProvider.getApplicationContext()

    @Before
    fun clear() {
        context.getSharedPreferences("pcm_settings", Context.MODE_PRIVATE).edit().clear().commit()
    }

    @Test
    fun `persists sessions, impressions, dismissals and clicks`() {
        val repo = PromoRepository(context)
        repo.incrementSessionCount()
        repo.incrementSessionCount()
        assertEquals(1, repo.recordImpression(nowMs = 100L))
        assertTrue(repo.shownThisSession)
        repo.recordDismiss(nowMs = 200L, snoozeDays = 14)
        assertTrue(repo.closedThisSession)

        val reloaded = PromoRepository(context).state()
        assertEquals(2, reloaded.sessionCount)
        assertEquals(1, reloaded.impressions)
        assertEquals(100L, reloaded.lastShownAt)
        assertEquals(1, reloaded.dismissCount)
        assertEquals(200L + TimeUnit.DAYS.toMillis(14), reloaded.snoozeUntil)

        repo.recordClick()
        assertTrue(PromoRepository(context).state().clicked)
    }
}
