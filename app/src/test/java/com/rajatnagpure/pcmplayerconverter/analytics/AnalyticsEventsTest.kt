package com.rajatnagpure.pcmplayerconverter.analytics

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AnalyticsEventsTest {

    @Test
    fun `sizeBucket boundaries`() {
        assertEquals("unknown", AnalyticsEvents.sizeBucket(-1))
        assertEquals("<100KB", AnalyticsEvents.sizeBucket(0))
        assertEquals("<100KB", AnalyticsEvents.sizeBucket(100L * 1024 - 1))
        assertEquals("100KB-1MB", AnalyticsEvents.sizeBucket(100L * 1024))
        assertEquals("1-10MB", AnalyticsEvents.sizeBucket(1024L * 1024))
        assertEquals("10-50MB", AnalyticsEvents.sizeBucket(10L * 1024 * 1024))
        assertEquals("50-200MB", AnalyticsEvents.sizeBucket(50L * 1024 * 1024))
        assertEquals(">200MB", AnalyticsEvents.sizeBucket(200L * 1024 * 1024))
    }

    @Test
    fun `extensionOf never leaks file names`() {
        assertEquals("pcm", AnalyticsEvents.extensionOf("My Secret Recording.PCM"))
        assertEquals("none", AnalyticsEvents.extensionOf("noextension"))
        assertEquals("none", AnalyticsEvents.extensionOf("weird.name with spaces"))
        assertEquals("none", AnalyticsEvents.extensionOf(null))
    }

    @Test
    fun `event and param names respect GA4 limits`() {
        val names = AnalyticsEvents::class.java.declaredFields
            .filter { it.type == String::class.java && java.lang.reflect.Modifier.isStatic(it.modifiers) }
            .map { it.isAccessible = true; it.get(null) as String }
        assertTrue(names.isNotEmpty())
        names.forEach { name ->
            assertTrue("$name too long", name.length <= 40)
        }
        assertEquals("error_type", AnalyticsEvents.P_ERROR_TYPE)
        assertEquals("IllegalStateException", AnalyticsEvents.errorType(IllegalStateException("x")))
    }
}
