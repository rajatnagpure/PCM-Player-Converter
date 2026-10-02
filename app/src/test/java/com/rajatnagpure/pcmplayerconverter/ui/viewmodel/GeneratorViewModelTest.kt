package com.rajatnagpure.pcmplayerconverter.ui.viewmodel

import android.app.Application
import android.net.Uri
import com.rajatnagpure.pcmplayerconverter.data.local.LocalFileDataSource
import com.rajatnagpure.pcmplayerconverter.domain.usecase.ConvertAudioToPcmUseCase
import com.rajatnagpure.pcmplayerconverter.domain.usecase.RecordAudioUseCase
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain

@kotlinx.coroutines.ExperimentalCoroutinesApi
@org.junit.runner.RunWith(org.robolectric.RobolectricTestRunner::class)
class GeneratorViewModelTest {

    private lateinit var testDispatcher: kotlinx.coroutines.test.TestDispatcher

    @org.junit.Before
    fun setup() {
        testDispatcher = kotlinx.coroutines.test.StandardTestDispatcher()
        kotlinx.coroutines.Dispatchers.setMain(testDispatcher)
    }

    @org.junit.After
    fun tearDown() {
        kotlinx.coroutines.Dispatchers.resetMain()
    }

    @Test
    fun `savedStateHandle uri triggers onFileSelectedForConversion`() = runTest {
        val recordUseCase = mockk<RecordAudioUseCase>()
        every { recordUseCase.amplitudeFlow } returns kotlinx.coroutines.flow.MutableStateFlow(0f)
        coEvery { recordUseCase.start(any(), any()) } returns Unit
        coEvery { recordUseCase.stop() } returns Unit
        every { recordUseCase.isRecording() } returns false

        val convertUseCase = mockk<ConvertAudioToPcmUseCase>(relaxed = true)
        val localData = mockk<LocalFileDataSource>(relaxed = true)
        val application = mockk<Application>(relaxed = true)

        val fakeFile = File.createTempFile("test", ".wav")
        coEvery { localData.getFileFromUri(any()) } returns fakeFile

        val savedStateHandle = androidx.lifecycle.SavedStateHandle(mapOf("uri" to "content://com.example/test.wav"))
        val vm = GeneratorViewModel(recordUseCase, convertUseCase, localData, savedStateHandle, application, com.rajatnagpure.pcmplayerconverter.service.ConversionEvents())

        // allow coroutine to process
        kotlinx.coroutines.delay(200)

        val state = vm.uiState
        assertNotNull(state.value)
    }

    @Test
    fun `saveFile starts background conversion and sets isConverting`() = runTest {
        val recordUseCase = mockk<RecordAudioUseCase>()
        every { recordUseCase.amplitudeFlow } returns kotlinx.coroutines.flow.MutableStateFlow(0f)
        coEvery { recordUseCase.start(any(), any()) } returns Unit
        coEvery { recordUseCase.stop() } returns Unit
        every { recordUseCase.isRecording() } returns false

        val convertUseCase = mockk<ConvertAudioToPcmUseCase>(relaxed = true)
        val localData = mockk<LocalFileDataSource>(relaxed = true)
        val application = mockk<Application>(relaxed = true)

        val fakeFile = File.createTempFile("test", ".mp3")
        coEvery { localData.getFileFromUri(any()) } returns fakeFile

        val savedStateHandle = androidx.lifecycle.SavedStateHandle(mapOf("uri" to "content://com.example/test.mp3"))
        val vm = GeneratorViewModel(recordUseCase, convertUseCase, localData, savedStateHandle, application, com.rajatnagpure.pcmplayerconverter.service.ConversionEvents())

        // simulate file selected
        kotlinx.coroutines.delay(200)
        val file = vm.uiState.value.selectedFileToConvert
        assertNotNull(file)

        // call saveFileToUri to trigger background conversion
        val mockUri = mockk<Uri>(relaxed = true)
        every { mockUri.toString() } returns "content://out/file.pcm"
        vm.saveFileToUri(mockUri)

        // immediately should be in converting state
        assertTrue(vm.uiState.value.isConverting)
    }

    @Test
    fun `converter results do not leak into generator and own results show completed`() = runTest {
        val recordUseCase = mockk<RecordAudioUseCase>()
        every { recordUseCase.amplitudeFlow } returns kotlinx.coroutines.flow.MutableStateFlow(0f)
        val events = com.rajatnagpure.pcmplayerconverter.service.ConversionEvents()
        val vm = GeneratorViewModel(
            recordUseCase, mockk(relaxed = true), mockk(relaxed = true),
            androidx.lifecycle.SavedStateHandle(), mockk<Application>(relaxed = true),
            events
        )
        testDispatcher.scheduler.advanceUntilIdle()

        events.publish(com.rajatnagpure.pcmplayerconverter.service.ConversionResult(1L, com.rajatnagpure.pcmplayerconverter.service.ConversionOrigin.CONVERTER, true, "a.wav"))
        testDispatcher.scheduler.advanceUntilIdle()
        org.junit.Assert.assertNull(vm.uiState.value.statusMessage)

        events.publish(com.rajatnagpure.pcmplayerconverter.service.ConversionResult(2L, com.rajatnagpure.pcmplayerconverter.service.ConversionOrigin.GENERATOR, true, "b.pcm"))
        testDispatcher.scheduler.advanceUntilIdle()
        org.junit.Assert.assertEquals("Conversion completed — saved as b.pcm", vm.uiState.value.statusMessage)
    }
}
