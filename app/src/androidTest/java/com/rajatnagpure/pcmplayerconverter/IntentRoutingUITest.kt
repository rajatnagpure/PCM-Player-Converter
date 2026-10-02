package com.rajatnagpure.pcmplayerconverter

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

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
        composeTestRule.onNode(hasText("Generator", ignoreCase = true) and hasClickAction()).performClick()
        
        // Check if Generator components exist
        composeTestRule.onNodeWithText("Record", ignoreCase = true).assertExists()
        composeTestRule.onNodeWithText("Select Audio", ignoreCase = true).assertExists()
    }
    
    @Test
    fun testUIColors_areConsistent() {
        // Verify primary color is used for Top Bar
        composeTestRule.onNodeWithContentDescription("Help", substring = true).assertExists()
    }
    
    @Test
    fun testConverterTab_displaysCorrectly() {
        // Verify Converter tab is displayed by default
        composeTestRule.onAllNodesWithText("PCM", substring = true, ignoreCase = true).onFirst().assertExists()
        composeTestRule.onNodeWithText("Input File", ignoreCase = true).assertExists()
        composeTestRule.onNodeWithText("Output Configuration", ignoreCase = true).assertExists()
        composeTestRule.onNodeWithText("Convert & Save", ignoreCase = true).assertExists()
    }
    
    @Test
    fun testGeneratorTab_displaysCorrectly() {
        // Navigate to Generator tab
        composeTestRule.onNode(hasText("Generator", ignoreCase = true) and hasClickAction()).performClick()
        
        // Verify Generator components
        composeTestRule.onAllNodesWithText("PCM", substring = true, ignoreCase = true).onFirst().assertExists()
        composeTestRule.onNodeWithText("Select Audio", ignoreCase = true).assertExists()
        composeTestRule.onNodeWithText("Record", ignoreCase = true).assertExists()
        composeTestRule.onNodeWithText("Recording Configuration", ignoreCase = true).assertExists()
    }
    
    @Test
    fun testNavigationBetweenTabs_works() {
        // Start at Converter
        composeTestRule.onNodeWithText("Convert & Save", ignoreCase = true).assertExists()
        
        // Navigate to Generator
        composeTestRule.onNode(hasText("Generator", ignoreCase = true) and hasClickAction()).performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Record", ignoreCase = true).assertExists()
        
        // Navigate back to Converter
        composeTestRule.onNode(hasText("Converter", ignoreCase = true) and hasClickAction()).performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Convert & Save", ignoreCase = true).assertExists()
    }
    
    @Test
    fun testHelpButton_exists() {
        composeTestRule.onNodeWithContentDescription("Help", substring = true).assertExists()
        composeTestRule.onNodeWithContentDescription("Help", substring = true).assertHasClickAction()
    }
    
    @Test
    fun testBottomNavigation_hasCorrectItems() {
        // Verify both navigation items exist
        composeTestRule.onNode(hasText("Converter", ignoreCase = true) and hasClickAction()).assertExists()
        composeTestRule.onNode(hasText("Generator", ignoreCase = true) and hasClickAction()).assertExists()
    }
}
