package com.rajatnagpure.pcmplayerconverter.ui.viewmodel

import android.app.Application
import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import com.rajatnagpure.pcmplayerconverter.analytics.AnalyticsEvents
import com.rajatnagpure.pcmplayerconverter.analytics.FakeAnalyticsTracker
import com.rajatnagpure.pcmplayerconverter.data.local.LocalFileDataSource
import com.rajatnagpure.pcmplayerconverter.service.ConversionEvents
import com.rajatnagpure.pcmplayerconverter.service.ConversionOrigin
import com.rajatnagpure.pcmplayerconverter.service.ConversionResult
import com.rajatnagpure.pcmplayerconverter.domain.usecase.ConvertPcmUseCase
import com.rajatnagpure.pcmplayerconverter.domain.usecase.PlayAudioUseCase
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.File

@ExperimentalCoroutinesApi
@RunWith(RobolectricTestRunner::class)
class ConverterViewModelTest {

    private lateinit var testDispatcher: kotlinx.coroutines.test.TestDispatcher
    private lateinit var convertPcmUseCase: ConvertPcmUseCase
    private lateinit var playAudioUseCase: PlayAudioUseCase
    private lateinit var localFileDataSource: LocalFileDataSource
    private lateinit var application: Application
    private lateinit var savedStateHandle: SavedStateHandle
    private lateinit var viewModel: ConverterViewModel
    private lateinit var conversionEvents: ConversionEvents
    private lateinit var analytics: FakeAnalyticsTracker

    @Before
    fun setup() {
        testDispatcher = StandardTestDispatcher()
        Dispatchers.setMain(testDispatcher)

        convertPcmUseCase = mockk(relaxed = true)
        playAudioUseCase = mockk(relaxed = true)
        localFileDataSource = mockk(relaxed = true)
        application = mockk(relaxed = true)
        savedStateHandle = SavedStateHandle()
        conversionEvents = ConversionEvents()
        analytics = FakeAnalyticsTracker()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `onFileSelected updates uiState with selected file`() = runTest {
        viewModel = ConverterViewModel(convertPcmUseCase, playAudioUseCase, localFileDataSource, savedStateHandle, application, conversionEvents, analytics)
        
        val mockUri = mockk<Uri>(relaxed = true)
        val fakeFile = File("test.pcm")
        coEvery { localFileDataSource.getFileFromUri(mockUri) } returns fakeFile
        every { mockUri.scheme } returns "file"

        viewModel.onFileSelected(mockUri)
        
        // Allow coroutines to run
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(fakeFile, viewModel.uiState.value.selectedFile)
    }

    @Test
    fun `saveFileToUri triggers conversion service and sets isConverting`() = runTest {
        viewModel = ConverterViewModel(convertPcmUseCase, playAudioUseCase, localFileDataSource, savedStateHandle, application, conversionEvents, analytics)
        
        // Setup initial state with a file
        val mockUri = mockk<Uri>(relaxed = true)
        every { mockUri.toString() } returns "content://out/file.wav"
        val fakeFile = File("test.pcm")
        coEvery { localFileDataSource.getFileFromUri(any()) } returns fakeFile
        every { mockUri.scheme } returns "file"
        
        viewModel.onFileSelected(mockUri)
        testDispatcher.scheduler.advanceUntilIdle() // Wait for file selection

        // Act
        val outputUri = mockk<Uri>(relaxed = true)
        every { outputUri.toString() } returns "content://out/result.wav"
        viewModel.saveFileToUri(outputUri)

        // Assert
        assertTrue(viewModel.uiState.value.isConverting)
    }

    @Test
    fun `savedStateHandle triggers onFileSelected`() = runTest {
        savedStateHandle["uri"] = "content://com.example/test.pcm"
        
        val fakeFile = File("test.pcm")
        coEvery { localFileDataSource.copyUriToCache(any()) } returns fakeFile
        
        viewModel = ConverterViewModel(convertPcmUseCase, playAudioUseCase, localFileDataSource, savedStateHandle, application, conversionEvents, analytics)
        
        testDispatcher.scheduler.advanceUntilIdle()
        
        assertNotNull(viewModel.uiState.value.selectedFile)
    }

    @Test
    fun `successful conversion shows conversion completed`() = runTest {
        viewModel = ConverterViewModel(convertPcmUseCase, playAudioUseCase, localFileDataSource, savedStateHandle, application, conversionEvents, analytics)
        testDispatcher.scheduler.advanceUntilIdle()

        val result = ConversionResult(42L, ConversionOrigin.CONVERTER, success = true, outputName = "song.wav")
        conversionEvents.publish(result)
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals("Conversion completed — saved as song.wav", state.conversionMessage)
        assertEquals("Conversion completed — saved as song.wav", state.toastMessage)
        assertEquals(true, state.conversionSucceeded)
        assertEquals(false, state.isConverting)
    }

    @Test
    fun `generator results are ignored by the converter`() = runTest {
        viewModel = ConverterViewModel(convertPcmUseCase, playAudioUseCase, localFileDataSource, savedStateHandle, application, conversionEvents, analytics)
        testDispatcher.scheduler.advanceUntilIdle()

        conversionEvents.publish(ConversionResult(1L, ConversionOrigin.GENERATOR, success = true, outputName = "x.pcm"))
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(null, viewModel.uiState.value.conversionMessage)
    }

    @Test
    fun `failed conversion shows failure reason`() = runTest {
        viewModel = ConverterViewModel(convertPcmUseCase, playAudioUseCase, localFileDataSource, savedStateHandle, application, conversionEvents, analytics)
        conversionEvents.publish(ConversionResult(7L, ConversionOrigin.CONVERTER, success = false, errorMessage = "Disk full"))
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals("Conversion failed: Disk full", viewModel.uiState.value.conversionMessage)
        assertEquals(false, viewModel.uiState.value.conversionSucceeded)
    }

    @Test
    fun `file import is logged once without the file name`() = runTest {
        viewModel = ConverterViewModel(convertPcmUseCase, playAudioUseCase, localFileDataSource, savedStateHandle, application, conversionEvents, analytics)
        val uri = mockk<Uri>(relaxed = true)
        every { uri.scheme } returns "file"
        every { uri.toString() } returns "file:///sdcard/private name.pcm"
        coEvery { localFileDataSource.getFileFromUri(uri) } returns File("private name.pcm")

        viewModel.onFileSelected(uri)
        viewModel.onFileSelected(uri) // e.g. screen + ViewModel both reacting to the same nav arg
        testDispatcher.scheduler.advanceUntilIdle()

        val imports = analytics.named(AnalyticsEvents.FILE_IMPORT)
        assertEquals(1, imports.size)
        assertEquals("pcm", imports.single().params[AnalyticsEvents.P_FILE_EXT])
        assertTrue(imports.single().params.values.none { it.toString().contains("private") })
    }

    @Test
    fun `cancelling the save picker is logged`() = runTest {
        viewModel = ConverterViewModel(convertPcmUseCase, playAudioUseCase, localFileDataSource, savedStateHandle, application, conversionEvents, analytics)
        viewModel.cancelSave()
        assertEquals(1, analytics.named(AnalyticsEvents.CONVERSION_CANCELLED).size)
    }
}
