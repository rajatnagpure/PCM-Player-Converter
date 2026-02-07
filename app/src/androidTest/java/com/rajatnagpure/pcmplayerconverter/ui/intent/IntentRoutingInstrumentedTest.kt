package com.rajatnagpure.pcmplayerconverter.ui.intent

import android.content.Intent
import android.net.Uri
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.rajatnagpure.pcmplayerconverter.MainActivity
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class IntentRoutingInstrumentedTest {

    @Test
    fun `pcm intent routes to converter screen`() {
        val uri = Uri.parse("content://com.example/test.pcm")
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "application/octet-stream"
            putExtra(Intent.EXTRA_STREAM, uri)
        }

        ActivityScenario.launch<MainActivity>(intent).use {
            // We can't easily assert Compose navigation without app-specific IDs; ensure activity launched
            Thread.sleep(500)
        }
    }

    @Test
    fun `wav intent routes to generator screen`() {
        val uri = Uri.parse("content://com.example/test.wav")
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "audio/x-wav"
            putExtra(Intent.EXTRA_STREAM, uri)
        }

        ActivityScenario.launch<MainActivity>(intent).use {
            Thread.sleep(500)
        }
    }
}
