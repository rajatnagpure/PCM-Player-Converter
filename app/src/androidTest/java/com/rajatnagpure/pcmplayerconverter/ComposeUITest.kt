package com.rajatnagpure.pcmplayerconverter

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import com.rajatnagpure.pcmplayerconverter.ui.MainActivityScreen
import org.junit.Rule
import org.junit.Test

class ComposeUITest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun testMainScreenElements() {
        // Check if header is displayed
        composeTestRule.onNodeWithText("PCM Player & Converter").assertIsDisplayed()

        // Check if Browse button exists
        composeTestRule.onNodeWithText("Browse").assertIsDisplayed()

        // Check if Play button exists
        composeTestRule.onNodeWithText("Play").assertIsDisplayed()
        
        // Check if Convert buttons exist
        composeTestRule.onNodeWithText("Convert to MP3").assertIsDisplayed()
        composeTestRule.onNodeWithText("Convert to Wav").assertIsDisplayed()
    }
}
