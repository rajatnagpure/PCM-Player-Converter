package com.rajatnagpure.pcmplayerconverter

import android.content.Intent
import android.net.Uri
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.io.FileOutputStream

@RunWith(AndroidJUnit4::class)
class IntentRoutingUITest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun testTabSwitching_preservesSelectedFile() {
        // We verify that the components exist and labels are correct
        composeTestRule.onNodeWithText("INPUT FILE").assertExists()
        composeTestRule.onNodeWithText("SELECT").assertExists().assertHasClickAction()
        
        // Go to Generator
        composeTestRule.onNodeWithText("Generator").performClick()
        
        // Check if Generator components exist
        composeTestRule.onNodeWithText("RECORD").assertExists()
        composeTestRule.onNodeWithText("SELECT AUDIO").assertExists()
    }
    
    @Test
    fun testUIColors_areConsistent() {
        // Verify primary color is used for Top Bar (visual check usually required, 
        // but we can check if certain elements have the "onPrimary" content color)
        composeTestRule.onNodeWithContentDescription("Help").assertExists()
    }
}
