package com.rajatnagpure.pcmplayerconverter.util

import android.app.Application
import android.content.Intent
import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import com.rajatnagpure.pcmplayerconverter.config.AppConfig
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf

@RunWith(RobolectricTestRunner::class)
class PlayStoreTest {

    private val app: Application = ApplicationProvider.getApplicationContext()

    @Test
    fun `banner link opens the exact UTM-tagged URL in the Play Store app`() {
        PlayStore.openUrl(app, AppConfig.COLOR_SHIFT_PLAY_URL)

        val intent = shadowOf(app).nextStartedActivity
        assertEquals(Intent.ACTION_VIEW, intent.action)
        assertEquals(PlayStore.PLAY_STORE_PACKAGE, intent.`package`)
        assertEquals(AppConfig.COLOR_SHIFT_PLAY_URL, intent.dataString)

        // The referrer decodes to the UTM tags the Play Store passes on to Color Shift
        val uri = Uri.parse(intent.dataString)
        assertEquals("com.rajatnagpure.floodfill", uri.getQueryParameter("id"))
        assertEquals(
            "utm_source=pcm_player_converter&utm_medium=cross_promo&utm_campaign=in_app_banner",
            uri.getQueryParameter("referrer")
        )
    }
}
