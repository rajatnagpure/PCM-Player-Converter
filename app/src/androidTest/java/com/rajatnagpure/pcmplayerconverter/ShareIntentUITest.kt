package com.rajatnagpure.pcmplayerconverter

import android.content.Intent
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.core.content.FileProvider
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

/**
 * Files shared into the app are routed to the right tab and loaded.
 * Each test launches MainActivity with its own SEND intent (no activity pre-launched by a rule).
 */
@RunWith(AndroidJUnit4::class)
class ShareIntentUITest {

    @get:Rule
    val composeTestRule = createEmptyComposeRule()

    private fun shareIntent(fileName: String, mimeType: String): Pair<Intent, File> {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val testFile = File(context.cacheDir, fileName).apply { writeBytes(ByteArray(1024)) }
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.provider", testFile)
        val intent = Intent(Intent.ACTION_SEND).apply {
            setClass(context, MainActivity::class.java)
            putExtra(Intent.EXTRA_STREAM, uri)
            type = mimeType
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        return intent to testFile
    }

    private fun assertTextEventuallyShown(text: String) {
        composeTestRule.waitUntil(5_000) {
            composeTestRule.onAllNodesWithText(text, substring = true, ignoreCase = true)
                .fetchSemanticsNodes().isNotEmpty()
        }
    }

    @Test
    fun pcmShare_opensConverterWithFile() {
        val (intent, file) = shareIntent("test_file.pcm", "application/octet-stream")
        ActivityScenario.launch<MainActivity>(intent).use {
            assertTextEventuallyShown("Input File")
            assertTextEventuallyShown("test_file")
        }
        file.delete()
    }

    @Test
    fun mp3Share_opensGeneratorWithFile() {
        val (intent, file) = shareIntent("test_audio.mp3", "audio/mpeg")
        ActivityScenario.launch<MainActivity>(intent).use {
            assertTextEventuallyShown("Recording Configuration")
            assertTextEventuallyShown("test_audio")
        }
        file.delete()
    }
}
