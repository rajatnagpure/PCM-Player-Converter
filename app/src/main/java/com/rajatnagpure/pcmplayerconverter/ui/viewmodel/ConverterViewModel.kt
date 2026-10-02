package com.rajatnagpure.pcmplayerconverter.ui.viewmodel
import com.rajatnagpure.pcmplayerconverter.ui.components.AppText as Text

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
import com.rajatnagpure.pcmplayerconverter.service.ConversionEvents
import com.rajatnagpure.pcmplayerconverter.service.ConversionOrigin
import com.rajatnagpure.pcmplayerconverter.service.ConversionResult
import com.rajatnagpure.pcmplayerconverter.service.ConversionService
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
    private val application: Application,
    private val conversionEvents: ConversionEvents
) : ViewModel() {

    private val _uiState = MutableStateFlow(ConverterUiState())
    val uiState: StateFlow<ConverterUiState> = _uiState.asStateFlow()

    init {
        // Results are delivered through the in-process ConversionEvents bus and held until shown,
        // so the "completed" message survives ViewModel recreation and never leaks to Generator.
        viewModelScope.launch {
            conversionEvents.results(ConversionOrigin.CONVERTER).collect { onConversionResult(it) }
        }

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

    private fun onConversionResult(result: ConversionResult) {
        val msg = if (result.success) {
            if (result.outputName != null) "Conversion completed — saved as ${result.outputName}" else "Conversion completed"
        } else {
            "Conversion failed" + (result.errorMessage?.let { ": $it" } ?: "")
        }
        _uiState.value = _uiState.value.copy(
            isConverting = false,
            conversionMessage = msg,
            conversionSucceeded = result.success,
            toastMessage = msg
        )
        conversionEvents.acknowledge(result)
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
        // Set flag to true to trigger SideEffect in UI
        _uiState.value = _uiState.value.copy(showSaveDialog = true, suggestedFileName = defaultName)
    }

    fun cancelSave() {
        _uiState.value = _uiState.value.copy(showSaveDialog = false)
    }

    fun saveFileToUri(outUri: Uri) {
        val inFile = _uiState.value.selectedFile ?: return
        val config = _uiState.value.audioConfig
        _uiState.value = _uiState.value.copy(showSaveDialog = false, isConverting = true, conversionSucceeded = null)

        val intent = Intent(application, ConversionService::class.java).apply {
            putExtra(ConversionService.EXTRA_IN_FILE, inFile)
            putExtra(ConversionService.EXTRA_OUT_URI, outUri.toString())
            putExtra(ConversionService.EXTRA_CONFIG, config)
            putExtra(ConversionService.EXTRA_ORIGIN, ConversionOrigin.CONVERTER.name)
            putExtra(ConversionService.EXTRA_JOB_ID, System.currentTimeMillis())
        }
        
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            application.startForegroundService(intent)
        } else {
            application.startService(intent)
        }
        
        // conversionMessage is replaced when the result arrives via ConversionEvents
        _uiState.value = _uiState.value.copy(conversionMessage = "Converting…")
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

    // allow external clearing of conversion message
    fun clearConversionMessage() {
        _uiState.value = _uiState.value.copy(conversionMessage = null, conversionSucceeded = null)
    }

    // one-shot toast shown by the screen
    fun clearToastMessage() {
        _uiState.value = _uiState.value.copy(toastMessage = null)
    }
}

data class ConverterUiState(
    val selectedFile: File? = null,
    val audioConfig: AudioConfig = AudioConfig(),
    val isConverting: Boolean = false,
    val isPlaying: Boolean = false,
    val errorMessage: String? = null,
    val conversionMessage: String? = null,
    val conversionSucceeded: Boolean? = null,
    val toastMessage: String? = null,
    val showSaveDialog: Boolean = false,
    val suggestedFileName: String = ""
)
