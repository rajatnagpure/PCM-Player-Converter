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
        viewModelScope.launch {
            savedStateHandle.getStateFlow("uri", "{uri}").collect { uriStr ->
                android.util.Log.d("ConverterViewModel", "Received URI from SavedStateHandle: $uriStr")
                
                if (uriStr.isNotBlank() && uriStr != "{uri}" && uriStr != "null") {
                    try {
                        // Navigation system already decodes parameters in the route pattern.
                        // uriStr contains the original URI string.
                        val uri = Uri.parse(uriStr)
                        android.util.Log.d("ConverterViewModel", "Parsed URI: $uri")
                        
                        onFileSelected(uri)
                    } catch (e: Exception) {
                        android.util.Log.e("ConverterViewModel", "Error parsing URI: $uriStr", e)
                        _uiState.value = _uiState.value.copy(errorMessage = "Error opening shared file")
                    }
                } else {
                    android.util.Log.d("ConverterViewModel", "Skipping placeholder/null/blank URI")
                }
            }
        }
    }

    fun onFileSelected(uri: Uri) {
        android.util.Log.d("ConverterViewModel", "onFileSelected called with URI: $uri")
        viewModelScope.launch {
            var file: File? = null
            try {
                if ("content".equals(uri.scheme, ignoreCase = true)) {
                    // prefer a cached copy for content URIs to avoid permission issues
                    try {
                        file = localFileDataSource.copyUriToCache(uri)
                        android.util.Log.d("ConverterViewModel", "Copied content URI to cache: ${file?.absolutePath}")
                    } catch (e: Exception) {
                        android.util.Log.w("ConverterViewModel", "copyUriToCache failed, will fallback to getFileFromUri", e)
                    }
                }
            } catch (e: Exception) {
                android.util.Log.w("ConverterViewModel", "Error while attempting to copy URI to cache", e)
            }

            if (file == null) {
                file = localFileDataSource.getFileFromUri(uri)
            }

            if (file != null) {
                android.util.Log.d("ConverterViewModel", "File loaded successfully: ${file.name}")
                _uiState.value = _uiState.value.copy(selectedFile = file, errorMessage = null)
            } else {
                android.util.Log.e("ConverterViewModel", "Failed to load file from URI")
                _uiState.value = _uiState.value.copy(errorMessage = "Failed to load file")
            }
        }
    }

    fun updateConfig(config: AudioConfig) {
        _uiState.value = _uiState.value.copy(audioConfig = config)
    }

    fun requestSaveFileName() {
        val file = _uiState.value.selectedFile ?: return
        val config = _uiState.value.audioConfig
        val defaultName = "${file.nameWithoutExtension}.${config.outputFormat.extension}"
        _uiState.value = _uiState.value.copy(showSaveDialog = true, suggestedFileName = defaultName)
    }

    fun cancelSave() {
        _uiState.value = _uiState.value.copy(showSaveDialog = false)
    }

    fun convertToFormat(fileName: String) {
        val inFile = _uiState.value.selectedFile ?: return
        val config = _uiState.value.audioConfig
        _uiState.value = _uiState.value.copy(showSaveDialog = false, isConverting = true)

        val outFile = File(inFile.parent, fileName)
        
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
        android.widget.Toast.makeText(application, "Saving to ${outFile.name}...", android.widget.Toast.LENGTH_SHORT).show()
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
    val conversionMessage: String? = null,
    val showSaveDialog: Boolean = false,
    val suggestedFileName: String = ""
)
