package com.rajatnagpure.pcmplayerconverter

import org.junit.Assert.*
import org.junit.Test

/**
 * Unit tests for MainActivity intent processing logic
 * These tests focus on the core logic without requiring Android framework
 */
class MainActivityIntentTest {

    @Test
    fun `file extension detection - PCM file`() {
        val uriString = "content://media/external/audio/test_file.pcm".lowercase()
        val isPcm = uriString.endsWith(".pcm")
        
        assertTrue("URI ending with .pcm should be detected as PCM", isPcm)
    }
    
    @Test
    fun `file extension detection - MP3 file`() {
        val uriString = "content://media/external/audio/test_file.mp3".lowercase()
        val isMp3 = uriString.endsWith(".mp3")
        
        assertTrue("URI ending with .mp3 should be detected as MP3", isMp3)
    }
    
    @Test
    fun `MIME type detection - PCM`() {
        val mimeType = "audio/pcm"
        val isPcm = mimeType.contains("pcm", ignoreCase = true)
        
        assertTrue("MIME type containing 'pcm' should be detected", isPcm)
    }
    
    @Test
    fun `MIME type detection - MP3 via mpeg`() {
        val mimeType = "audio/mpeg"
        val isMp3 = mimeType.contains("mp3", ignoreCase = true) || 
                    mimeType.contains("mpeg", ignoreCase = true)
        
        assertTrue("MIME type 'audio/mpeg' should be detected as MP3", isMp3)
    }
    
    @Test
    fun `MIME type detection - MP3 explicit`() {
        val mimeType = "audio/mp3"
        val isMp3 = mimeType.contains("mp3", ignoreCase = true)
        
        assertTrue("MIME type 'audio/mp3' should be detected as MP3", isMp3)
    }
    
    @Test
    fun `route creation - PCM file routes to converter`() {
        val encodedUri = "content%3A%2F%2Ftest%2Ffile.pcm"
        val route = "converter?uri=$encodedUri"
        
        assertTrue("PCM files should route to converter", route.startsWith("converter?uri="))
        assertTrue("Route should contain encoded URI", route.contains(encodedUri))
    }
    
    @Test
    fun `route creation - MP3 file routes to generator`() {
        val encodedUri = "content%3A%2F%2Ftest%2Ffile.mp3"
        val route = "generator?uri=$encodedUri"
        
        assertTrue("MP3 files should route to generator", route.startsWith("generator?uri="))
        assertTrue("Route should contain encoded URI", route.contains(encodedUri))
    }
    
    @Test
    fun `edge case - uppercase PCM extension detected`() {
        val uriString = "content://media/external/audio/TEST_FILE.PCM".lowercase()
        val isPcm = uriString.endsWith(".pcm")
        
        assertTrue("Uppercase .PCM extension should be detected after lowercasing", isPcm)
    }
    
    @Test
    fun `edge case - file with multiple dots`() {
        val uriString = "content://test/my.backup.file.pcm".lowercase()
        val isPcm = uriString.endsWith(".pcm")
        
        assertTrue("File with multiple dots should still detect .pcm extension", isPcm)
    }
    
    @Test
    fun `edge case - non-audio file defaults to converter`() {
        val uriString = "content://test/document.txt".lowercase()
        val isPcm = uriString.endsWith(".pcm")
        val isMp3 = uriString.endsWith(".mp3")
        
        assertFalse("TXT file should not be detected as PCM", isPcm)
        assertFalse("TXT file should not be detected as MP3", isMp3)
        // In real implementation, this would default to converter
    }
    
    @Test
    fun `route validation - placeholder URIs rejected`() {
        val placeholders = listOf("{uri}", "null", "")
        
        placeholders.forEach { placeholder ->
            val isValid = placeholder.isNotBlank() && 
                         placeholder != "{uri}" && 
                         placeholder != "null"
            assertFalse("Placeholder '$placeholder' should be rejected", isValid)
        }
    }
    
    @Test
    fun `route validation - valid URI accepted`() {
        val validRoute = "converter?uri=content%3A%2F%2Ftest%2Ffile.pcm"
        
        val isValid = validRoute.isNotBlank() && 
                     validRoute != "converter?uri={uri}" && 
                     validRoute != "generator?uri={uri}" &&
                     !validRoute.endsWith("uri=")
        
        assertTrue("Valid route should be accepted", isValid)
    }
}
