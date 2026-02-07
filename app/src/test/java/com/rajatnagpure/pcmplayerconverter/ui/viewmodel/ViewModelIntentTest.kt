package com.rajatnagpure.pcmplayerconverter.ui.viewmodel

import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import com.rajatnagpure.pcmplayerconverter.data.local.LocalFileDataSource
import com.rajatnagpure.pcmplayerconverter.domain.usecase.ConvertPcmUseCase
import com.rajatnagpure.pcmplayerconverter.domain.usecase.PlayAudioUseCase
import io.mockk.mockk
import io.mockk.verify
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class ViewModelIntentTest {

    private val convertPcmUseCase = mockk<ConvertPcmUseCase>(relaxed = true)
    private val playAudioUseCase = mockk<PlayAudioUseCase>(relaxed = true)
    private val localFileDataSource = mockk<LocalFileDataSource>(relaxed = true)

    @Test
    fun `ConverterViewModel handles encoded uri from SavedStateHandle`() {
        val uri = Uri.parse("content://media/external/audio/test.pcm")
        val encodedUri = Uri.encode(uri.toString())
        val savedStateHandle = SavedStateHandle(mapOf("uri" to encodedUri))
        
        val viewModel = ConverterViewModel(
            convertPcmUseCase,
            playAudioUseCase,
            localFileDataSource,
            savedStateHandle,
            mockk(relaxed = true)
        )
        
        verify { localFileDataSource.getFileFromUri(uri) }
    }
}
