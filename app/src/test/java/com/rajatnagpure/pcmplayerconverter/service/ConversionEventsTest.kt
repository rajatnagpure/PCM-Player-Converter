package com.rajatnagpure.pcmplayerconverter.service

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withTimeoutOrNull
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ConversionEventsTest {

    @Test
    fun `result is delivered only to its origin`() = runTest {
        val events = ConversionEvents()
        val result = ConversionResult(1L, ConversionOrigin.CONVERTER, success = true, outputName = "a.wav")
        events.publish(result)

        assertEquals(result, events.results(ConversionOrigin.CONVERTER).first())
        assertNull(withTimeoutOrNull(50) { events.results(ConversionOrigin.GENERATOR).firstOrNull() })
    }

    @Test
    fun `result survives until acknowledged`() = runTest {
        val events = ConversionEvents()
        val result = ConversionResult(2L, ConversionOrigin.GENERATOR, success = false, errorMessage = "boom")
        events.publish(result)
        // a late subscriber (e.g. recreated ViewModel) still gets it
        assertEquals(result, events.results(ConversionOrigin.GENERATOR).first())

        events.acknowledge(result)
        assertNull(withTimeoutOrNull(50) { events.results(ConversionOrigin.GENERATOR).firstOrNull() })
    }

    @Test
    fun `acknowledging a stale job keeps the newer result`() = runTest {
        val events = ConversionEvents()
        val old = ConversionResult(1L, ConversionOrigin.CONVERTER, success = true)
        val new = ConversionResult(2L, ConversionOrigin.CONVERTER, success = true)
        events.publish(old)
        events.publish(new)
        events.acknowledge(old)
        assertEquals(new, events.results(ConversionOrigin.CONVERTER).first())
    }

    @Test
    fun `busy tracks running tasks`() = runTest {
        val events = ConversionEvents()
        assertFalse(events.isBusy.first())
        events.setBusy(ConversionEvents.TASK_CONVERSION, true)
        events.setBusy(ConversionEvents.TASK_RECORDING, true)
        events.setBusy(ConversionEvents.TASK_CONVERSION, false)
        assertTrue(events.isBusy.first())
        events.setBusy(ConversionEvents.TASK_RECORDING, false)
        assertFalse(events.isBusy.first())
    }
}
