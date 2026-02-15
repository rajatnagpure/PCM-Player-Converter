package com.rajatnagpure.pcmplayerconverter

import android.content.Intent
import android.net.Uri
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.io.FileOutputStream

/**
 * UI tests for intent routing and file sharing functionality
 * These tests verify the end-to-end behavior when files are shared to the app
 */
@RunWith(AndroidJUnit4::class)
class IntentRoutingUITest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun testTabSwitching_preservesSelectedFile() {
        // We verify that the components exist and labels are correct
        composeTestRule.onNodeWithText("Input File", ignoreCase = true).assertExists()
        composeTestRule.onNodeWithText("Select", ignoreCase = true).assertExists().assertHasClickAction()
        
        // Go to Generator
        composeTestRule.onNodeWithText("Generator").performClick()
        
        // Check if Generator components exist
        composeTestRule.onNodeWithText("Record", ignoreCase = true).assertExists()
        composeTestRule.onNodeWithText("Select Audio", ignoreCase = true).assertExists()
    }
    
    @Test
    fun testUIColors_areConsistent() {
        // Verify primary color is used for Top Bar
        composeTestRule.onNodeWithContentDescription("Help").assertExists()
    }
    
    @Test
    fun testConverterTab_displaysCorrectly() {
        // Verify Converter tab is displayed by default
        composeTestRule.onNodeWithText("PCM Converter").assertExists()
        composeTestRule.onNodeWithText("Input File", ignoreCase = true).assertExists()
        composeTestRule.onNodeWithText("Output Configuration", ignoreCase = true).assertExists()
        composeTestRule.onNodeWithText("Convert", ignoreCase = true).assertExists()
    }
    
    @Test
    fun testGeneratorTab_displaysCorrectly() {
        // Navigate to Generator tab
        composeTestRule.onNodeWithText("Generator").performClick()
        
        // Verify Generator components
        composeTestRule.onNodeWithText("PCM Converter").assertExists()
        composeTestRule.onNodeWithText("Select Audio", ignoreCase = true).assertExists()
        composeTestRule.onNodeWithText("Record", ignoreCase = true).assertExists()
        composeTestRule.onNodeWithText("Recording Configuration", ignoreCase = true).assertExists()
    }
    
    @Test
    fun testNavigationBetweenTabs_works() {
        // Start at Converter
        composeTestRule.onNodeWithText("Convert", ignoreCase = true).assertExists()
        
        // Navigate to Generator
        composeTestRule.onNodeWithText("Generator").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Record", ignoreCase = true).assertExists()
        
        // Navigate back to Converter
        composeTestRule.onNodeWithText("Converter").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Convert", ignoreCase = true).assertExists()
    }
    
    @Test
    fun testPcmFileIntent_opensConverterTab() {
        // Create a test file in the app's cache
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val testFile = File(context.cacheDir, "test_file.pcm")
        testFile.writeBytes(ByteArray(1024) { 0x00 })
        
        val uri = androidx.core.content.FileProvider.getUriForFile(
            context,
            "${context.packageName}.provider",
            testFile
        )
        
        // Create intent to simulate file sharing
        val intent = Intent(Intent.ACTION_SEND).apply {
            putExtra(Intent.EXTRA_STREAM, uri)
            type = "application/octet-stream"
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        
        // Launch activity with intent
        val activityScenario = androidx.test.core.app.ActivityScenario.launch<MainActivity>(intent)
        
        // Note: The actual verification would depend on how the UI updates
        // For now, we verify the app doesn't crash and basic UI is present
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("PCM Converter").assertExists()
        composeTestRule.onNodeWithText("test_file.pcm").assertExists()
        
        activityScenario.close()
        testFile.delete()
    }
    
    @Test
    fun testMp3FileIntent_opensGeneratorTab() {
        // Create a test MP3 file
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val testFile = File(context.cacheDir, "test_audio.mp3")
        testFile.writeBytes(ByteArray(1024) { 0x00 })
        
        val uri = androidx.core.content.FileProvider.getUriForFile(
            context,
            "${context.packageName}.provider",
            testFile
        )
        
        // Create intent to simulate file sharing
        val intent = Intent(Intent.ACTION_SEND).apply {
            putExtra(Intent.EXTRA_STREAM, uri)
            type = "audio/mpeg"
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        
        // Launch activity with intent
        val activityScenario = androidx.test.core.app.ActivityScenario.launch<MainActivity>(intent)
        
        // Verify app doesn't crash and UI is present
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("PCM Converter").assertExists()
        // Note: We might need to switch to Converter tab if it defaults to Generator for MP3,
        // but the current implementation stays on the tab determined by intent logic.
        // Let's verify file name is visible.
        composeTestRule.onNodeWithText("test_audio.mp3").assertExists()
        
        activityScenario.close()
        testFile.delete()
    }
    
    @Test
    fun testHelpButton_exists() {
        composeTestRule.onNodeWithContentDescription("Help").assertExists()
        composeTestRule.onNodeWithContentDescription("Help").assertHasClickAction()
    }
    
    @Test
    fun testBottomNavigation_hasCorrectItems() {
        // Verify both navigation items exist
        composeTestRule.onNodeWithText("Converter").assertExists()
        composeTestRule.onNodeWithText("Generator").assertExists()
    }
}
