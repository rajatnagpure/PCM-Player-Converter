package com.rajatnagpure.pcmplayerconverter.Integration

import android.content.Intent
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.rule.GrantPermissionRule
import com.rajatnagpure.pcmplayerconverter.service.ConversionService
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

@RunWith(AndroidJUnit4::class)
class ConversionServiceIntegrationTest {

    @get:Rule
    val permissionRule: GrantPermissionRule = GrantPermissionRule.grant(android.Manifest.permission.FOREGROUND_SERVICE)

    private val appContext = ApplicationProvider.getApplicationContext<android.app.Application>()

    private val latch = CountDownLatch(1)
    private var receivedSuccess = false

    @Before
    fun setup() {
        // register a broadcast receiver to listen for completion
        val filter = android.content.IntentFilter(ConversionService.ACTION_CONVERSION_COMPLETE)
        appContext.registerReceiver(object : android.content.BroadcastReceiver() {
            override fun onReceive(context: android.content.Context?, intent: Intent?) {
                if (intent == null) return
                receivedSuccess = intent.getBooleanExtra("success", false)
                latch.countDown()
            }
        }, filter)
    }

    @After
    fun teardown() {
        // Nothing to cleanup; system will handle
    }

    @Test
    fun `conversion service completes and broadcasts success`() {
        // create a small dummy input file
        val cacheDir = appContext.cacheDir
        val inFile = File(cacheDir, "test_input.wav")
        inFile.writeBytes(ByteArray(512) { 0x00 })
        val outFile = File(cacheDir, "test_output.pcm")

        val intent = Intent(appContext, ConversionService::class.java).apply {
            putExtra("inFile", inFile)
            putExtra("outFile", outFile)
            putExtra("task", "AUDIO_TO_PCM")
        }

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            appContext.startForegroundService(intent)
        } else {
            appContext.startService(intent)
        }

        // wait up to 10 seconds for service to broadcast
        val completed = latch.await(10, TimeUnit.SECONDS)
        assertTrue("Service did not broadcast completion", completed && receivedSuccess)
    }
}
