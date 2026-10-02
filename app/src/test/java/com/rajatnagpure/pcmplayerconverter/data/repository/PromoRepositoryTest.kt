package com.rajatnagpure.pcmplayerconverter.data.repository

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.rajatnagpure.pcmplayerconverter.domain.promo.PromoConfig
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.util.concurrent.TimeUnit

@RunWith(RobolectricTestRunner::class)
class PromoRepositoryTest {

    private val context: Context = ApplicationProvider.getApplicationContext()
    private val day = TimeUnit.DAYS.toMillis(1)
    private val config = PromoConfig.DEFAULT

    @Before
    fun clear() {
        context.getSharedPreferences("pcm_settings", Context.MODE_PRIVATE).edit().clear().commit()
    }

    @Test
    fun `persists sessions and impressions`() {
        val repo = PromoRepository(context)
        repo.incrementSessionCount()
        repo.incrementSessionCount()
        assertEquals(1, repo.recordImpression())
        assertTrue(repo.impressionLoggedThisSession)
        assertEquals(2, PromoRepository(context).state().sessionCount)
        assertEquals(1, PromoRepository(context).impressions())
    }

    @Test
    fun `second dismissal within 14 days reaches the cap`() {
        val repo = PromoRepository(context)
        assertFalse(repo.recordDismiss(nowMs = 100 * day, config = config))
        assertEquals(listOf(100 * day), PromoRepository(context).state().dismissals)
        assertTrue(repo.recordDismiss(nowMs = 104 * day, config = config))
        assertEquals(listOf(100 * day, 104 * day), PromoRepository(context).state().dismissals)
    }

    @Test
    fun `dismissals far apart never cap and old ones are pruned`() {
        val repo = PromoRepository(context)
        assertFalse(repo.recordDismiss(nowMs = 100 * day, config = config))
        assertFalse(repo.recordDismiss(nowMs = 120 * day, config = config))
        assertEquals(listOf(120 * day), PromoRepository(context).state().dismissals)
    }
}
