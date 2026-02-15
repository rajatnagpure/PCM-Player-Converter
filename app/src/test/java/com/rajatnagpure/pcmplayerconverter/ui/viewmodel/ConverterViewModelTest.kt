package com.rajatnagpure.pcmplayerconverter.ui.viewmodel

import android.app.Application
import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import com.rajatnagpure.pcmplayerconverter.data.local.LocalFileDataSource
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

    @Before
    fun setup() {
        testDispatcher = StandardTestDispatcher()
        Dispatchers.setMain(testDispatcher)

        convertPcmUseCase = mockk(relaxed = true)
        playAudioUseCase = mockk(relaxed = true)
        localFileDataSource = mockk(relaxed = true)
        application = mockk(relaxed = true)
        savedStateHandle = SavedStateHandle()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `onFileSelected updates uiState with selected file`() = runTest {
        viewModel = ConverterViewModel(convertPcmUseCase, playAudioUseCase, localFileDataSource, savedStateHandle, application)
        
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
        viewModel = ConverterViewModel(convertPcmUseCase, playAudioUseCase, localFileDataSource, savedStateHandle, application)
        
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
        
        viewModel = ConverterViewModel(convertPcmUseCase, playAudioUseCase, localFileDataSource, savedStateHandle, application)
        
        testDispatcher.scheduler.advanceUntilIdle()
        
        assertNotNull(viewModel.uiState.value.selectedFile)
    }
}
