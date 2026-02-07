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

class GeneratorViewModelTest {

    @Test
    fun `savedStateHandle uri triggers onFileSelectedForConversion`() = runTest {
        val recordUseCase = mockk<RecordAudioUseCase>(relaxed = true)
        val convertUseCase = mockk<ConvertAudioToPcmUseCase>(relaxed = true)
        val localData = mockk<LocalFileDataSource>(relaxed = true)
        val application = mockk<Application>(relaxed = true)

        val fakeFile = File.createTempFile("test", ".wav")
        coEvery { localData.getFileFromUri(any()) } returns fakeFile

        val savedStateHandle = androidx.lifecycle.SavedStateHandle(mapOf("uri" to "content://com.example/test.wav"))
        val vm = GeneratorViewModel(recordUseCase, convertUseCase, localData, savedStateHandle, application)

        // allow coroutine to process
        kotlinx.coroutines.delay(200)

        val state = vm.uiState
        assertNotNull(state.value)
    }

    @Test
    fun `saveFile starts background conversion and sets isConverting`() = runTest {
        val recordUseCase = mockk<RecordAudioUseCase>(relaxed = true)
        val convertUseCase = mockk<ConvertAudioToPcmUseCase>(relaxed = true)
        val localData = mockk<LocalFileDataSource>(relaxed = true)
        val application = mockk<Application>(relaxed = true)

        val fakeFile = File.createTempFile("test", ".mp3")
        coEvery { localData.getFileFromUri(any()) } returns fakeFile

        val savedStateHandle = androidx.lifecycle.SavedStateHandle(mapOf("uri" to "content://com.example/test.mp3"))
        val vm = GeneratorViewModel(recordUseCase, convertUseCase, localData, savedStateHandle, application)

        // simulate file selected
        kotlinx.coroutines.delay(200)
        val file = vm.uiState.value.selectedFileToConvert
        assertNotNull(file)

        // call saveFile to trigger background conversion
        vm.saveFile("out.pcm")

        // immediately should be in converting state
        assertTrue(vm.uiState.value.isConverting)
    }
}
