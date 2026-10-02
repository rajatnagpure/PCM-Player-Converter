package com.rajatnagpure.pcmplayerconverter.service

import android.app.NotificationManager
import android.content.Context
import android.net.Uri
import androidx.core.app.NotificationCompat
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ConversionNotificationsTest {

    private val context: Context = ApplicationProvider.getApplicationContext()

    @Test
    fun `completed notification says conversion completed on the results channel`() {
        val n = ConversionNotifications.completed(context, "song.wav", Uri.parse("content://x/song.wav"))
        assertEquals(ConversionNotifications.RESULT_CHANNEL_ID, n.channelId)
        assertEquals("Conversion completed", n.extras.getCharSequence(NotificationCompat.EXTRA_TITLE).toString())
        assertEquals("Saved as song.wav", n.extras.getCharSequence(NotificationCompat.EXTRA_TEXT).toString())
        assertNotNull(n.contentIntent)
    }

    @Test
    fun `results channel is audible and progress channel is silent`() {
        ConversionNotifications.ensureChannels(context)
        val nm = context.getSystemService(NotificationManager::class.java)
        assertEquals(NotificationManager.IMPORTANCE_DEFAULT, nm.getNotificationChannel(ConversionNotifications.RESULT_CHANNEL_ID).importance)
        assertEquals(NotificationManager.IMPORTANCE_LOW, nm.getNotificationChannel(ConversionNotifications.PROGRESS_CHANNEL_ID).importance)
    }

    @Test
    fun `failed notification has a title`() {
        val n = ConversionNotifications.failed(context, null)
        assertEquals("Conversion failed", n.extras.getCharSequence(NotificationCompat.EXTRA_TITLE).toString())
    }
}
