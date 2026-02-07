package com.rajatnagpure.pcmplayerconverter.ui.viewmodel

import android.app.Application
import android.content.Intent
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rajatnagpure.pcmplayerconverter.data.local.LocalFileDataSource
import com.rajatnagpure.pcmplayerconverter.domain.model.AudioConfig
import com.rajatnagpure.pcmplayerconverter.domain.model.PcmEncoding
import com.rajatnagpure.pcmplayerconverter.domain.usecase.ConvertPcmUseCase
import com.rajatnagpure.pcmplayerconverter.domain.usecase.PlayAudioUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import java.io.File
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

@HiltViewModel
class ConverterViewModel @Inject constructor(
    private val convertPcmUseCase: ConvertPcmUseCase,
    private val playAudioUseCase: PlayAudioUseCase,
    private val localFileDataSource: LocalFileDataSource,
    savedStateHandle: androidx.lifecycle.SavedStateHandle,
    private val application: Application
) : ViewModel() {

    private val _uiState = MutableStateFlow(ConverterUiState())
    val uiState: StateFlow<ConverterUiState> = _uiState.asStateFlow()

    init {
        savedStateHandle.get<String>("uri")?.let { uriString ->
            if (uriString.isNotEmpty()) {
                onFileSelected(Uri.parse(uriString))
            }
        }
    }

    fun onFileSelected(uri: Uri) {
        viewModelScope.launch {
            val file = localFileDataSource.getFileFromUri(uri)
            if (file != null) {
                _uiState.value = _uiState.value.copy(selectedFile = file, errorMessage = null)
            } else {
                _uiState.value = _uiState.value.copy(errorMessage = "Failed to load file")
            }
        }
    }

    fun updateConfig(config: AudioConfig) {
        _uiState.value = _uiState.value.copy(audioConfig = config)
    }

    fun convertToWav() {
        val inFile = _uiState.value.selectedFile ?: return
        val config = _uiState.value.audioConfig
        val extension = config.outputFormat.extension
        val outFile = File(inFile.parent, "${inFile.nameWithoutExtension}.$extension")

        val intent = Intent(application, com.rajatnagpure.pcmplayerconverter.service.ConversionService::class.java).apply {
            putExtra("inFile", inFile)
            putExtra("outFile", outFile)
            putExtra("config", config)
        }
        
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            application.startForegroundService(intent)
        } else {
            application.startService(intent)
        }
        
        _uiState.value = _uiState.value.copy(conversionMessage = "Conversion started in background...")
    }

    fun togglePlay() {
        val file = _uiState.value.selectedFile ?: return
        val config = _uiState.value.audioConfig
        viewModelScope.launch {
            if (_uiState.value.isPlaying) {
                playAudioUseCase.stop()
                _uiState.value = _uiState.value.copy(isPlaying = false)
            } else {
                _uiState.value = _uiState.value.copy(isPlaying = true)
                try {
                    playAudioUseCase.play(file, config)
                } catch (e: Exception) {
                    _uiState.value = _uiState.value.copy(errorMessage = "Playback error: ${e.message}")
                } finally {
                    _uiState.value = _uiState.value.copy(isPlaying = false)
                }
            }
        }
    }
}

data class ConverterUiState(
    val selectedFile: File? = null,
    val audioConfig: AudioConfig = AudioConfig(),
    val isConverting: Boolean = false,
    val isPlaying: Boolean = false,
    val errorMessage: String? = null,
    val conversionMessage: String? = null
)
