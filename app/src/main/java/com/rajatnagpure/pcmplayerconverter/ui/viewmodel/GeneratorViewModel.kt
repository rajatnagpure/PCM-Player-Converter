package com.rajatnagpure.pcmplayerconverter.ui.viewmodel
import com.rajatnagpure.pcmplayerconverter.ui.components.AppText as Text

import android.net.Uri
import android.os.Environment
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rajatnagpure.pcmplayerconverter.data.local.LocalFileDataSource
import com.rajatnagpure.pcmplayerconverter.domain.model.AudioConfig
import com.rajatnagpure.pcmplayerconverter.domain.usecase.ConvertAudioToPcmUseCase
import com.rajatnagpure.pcmplayerconverter.domain.usecase.RecordAudioUseCase
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
class GeneratorViewModel @Inject constructor(
    private val recordAudioUseCase: RecordAudioUseCase,
    private val convertAudioToPcmUseCase: ConvertAudioToPcmUseCase,
    private val localFileDataSource: LocalFileDataSource,
    savedStateHandle: androidx.lifecycle.SavedStateHandle,
    private val application: android.app.Application,
    private val conversionEvents: ConversionEvents
) : ViewModel() {

    // Move UI state declaration before init blocks to ensure it's initialized when collectors run
    private val _uiState = MutableStateFlow(GeneratorUiState())
    val uiState: StateFlow<GeneratorUiState> = _uiState.asStateFlow()

    init {
        // Only results of jobs started from this screen
        viewModelScope.launch {
            conversionEvents.results(ConversionOrigin.GENERATOR).collect { onConversionResult(it) }
        }

        viewModelScope.launch {
            savedStateHandle.getStateFlow("uri", "{uri}").collect { uriStr ->
                android.util.Log.d("GeneratorViewModel", "Received URI from SavedStateHandle: $uriStr")
                
                if (uriStr.isNotBlank() && uriStr != "{uri}" && uriStr != "null") {
                    try {
                        // Navigation system already decodes parameters in the route pattern.
                        val uri = android.net.Uri.parse(uriStr)
                        android.util.Log.d("GeneratorViewModel", "Parsed URI: $uri")
                        
                        onFileSelectedForConversion(uri)
                    } catch (e: Exception) {
                        android.util.Log.e("GeneratorViewModel", "Error parsing URI: $uriStr", e)
                        _uiState.value = _uiState.value.copy(errorMessage = "Error opening shared file")
                    }
                } else {
                    android.util.Log.d("GeneratorViewModel", "Skipping placeholder/null/blank URI")
                }
            }
        }
    }

    init {
        viewModelScope.launch {
            recordAudioUseCase.amplitudeFlow.collect { amp ->
                _uiState.value = _uiState.value.copy(currentAmplitude = amp)
            }
        }
    }

    private fun onConversionResult(result: ConversionResult) {
        _uiState.value = if (result.success) {
            val msg = if (result.outputName != null) "Conversion completed — saved as ${result.outputName}" else "Conversion completed"
            _uiState.value.copy(isConverting = false, statusMessage = msg, errorMessage = null)
        } else {
            _uiState.value.copy(isConverting = false, statusMessage = null, errorMessage = "Conversion failed" + (result.errorMessage?.let { ": $it" } ?: ""))
        }
        conversionEvents.acknowledge(result)
    }

    fun updateConfig(config: AudioConfig) {
        _uiState.value = _uiState.value.copy(audioConfig = config)
    }

    fun toggleRecording() {
        val config = _uiState.value.audioConfig
        viewModelScope.launch {
            if (_uiState.value.isRecording) {
                recordAudioUseCase.stop()
                val tempFile = _uiState.value.lastRecordedFile
                _uiState.value = _uiState.value.copy(
                    isRecording = false, 
                    showSaveDialog = true,
                    statusMessage = null, // Clear the "Recording..." message
                    tempRecordedFile = tempFile,
                    suggestedFileName = "recorded_audio_${System.currentTimeMillis()}.pcm"
                )
            } else {
                val cacheDir = application.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS) ?: application.cacheDir
                val file = File(cacheDir, "temp_recording.pcm")
                _uiState.value = _uiState.value.copy(isRecording = true, statusMessage = "Recording...", lastRecordedFile = file, errorMessage = null)
                try {
                    recordAudioUseCase.start(file, config)
                } catch (e: Exception) {
                    _uiState.value = _uiState.value.copy(isRecording = false, errorMessage = "Recording error: ${e.message}")
                }
            }
        }
    }

    fun onFileSelectedForConversion(uri: Uri) {
        android.util.Log.d("GeneratorViewModel", "onFileSelectedForConversion called with URI: $uri")
        viewModelScope.launch {
            val file = localFileDataSource.getFileFromUri(uri)
            if (file != null) {
                android.util.Log.d("GeneratorViewModel", "File loaded successfully: ${file.name}")
                _uiState.value = _uiState.value.copy(selectedFileToConvert = file, errorMessage = null)
            } else {
                android.util.Log.e("GeneratorViewModel", "Failed to load file from URI")
                _uiState.value = _uiState.value.copy(errorMessage = "Failed to load file")
            }
        }
    }

    fun requestPcmSaveFileName() {
        val file = _uiState.value.selectedFileToConvert ?: return
        val suggested = "${file.nameWithoutExtension}.pcm"
        _uiState.value = _uiState.value.copy(showSaveDialog = true, suggestedFileName = suggested, tempRecordedFile = null)
    }

    fun saveFileToUri(outUri: Uri) {
        val tempFile = _uiState.value.tempRecordedFile
        if (tempFile != null) {
            // Saving a recording: direct copy
            viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
                _uiState.value = _uiState.value.copy(showSaveDialog = false, isConverting = true, statusMessage = "Saving...")
                try {
                    application.contentResolver.openOutputStream(outUri)?.use { output ->
                        java.io.FileInputStream(tempFile).use { input ->
                            input.copyTo(output)
                        }
                    }
                    tempFile.delete()
                    _uiState.value = _uiState.value.copy(
                        isConverting = false, 
                        statusMessage = "Recording saved successfully",
                        lastRecordedFile = null, // Clear reference to deleted temp file
                        tempRecordedFile = null
                    )
                } catch (e: Exception) {
                    _uiState.value = _uiState.value.copy(isConverting = false, errorMessage = "Save failed: ${e.message}")
                }
            }
        } else {
            // Saving a conversion - delegate to ConversionService
            val inFile = _uiState.value.selectedFileToConvert ?: return
            _uiState.value = _uiState.value.copy(isConverting = true, showSaveDialog = false, errorMessage = null)

            // start background ConversionService with task AUDIO_TO_PCM
            val intent = android.content.Intent(application, ConversionService::class.java).apply {
                putExtra(ConversionService.EXTRA_IN_FILE, inFile)
                putExtra(ConversionService.EXTRA_OUT_URI, outUri.toString())
                putExtra(ConversionService.EXTRA_TASK, ConversionService.TASK_AUDIO_TO_PCM)
                putExtra(ConversionService.EXTRA_ORIGIN, ConversionOrigin.GENERATOR.name)
                putExtra(ConversionService.EXTRA_JOB_ID, System.currentTimeMillis())
            }

            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                application.startForegroundService(intent)
            } else {
                application.startService(intent)
            }
        }
    }

    fun cancelSave() {
        _uiState.value.tempRecordedFile?.delete()
        _uiState.value = _uiState.value.copy(showSaveDialog = false, tempRecordedFile = null)
    }

    // New helper to let UI clear status messages after showing toast once
    fun clearStatusMessage() {
        _uiState.value = _uiState.value.copy(statusMessage = null)
    }
}

data class GeneratorUiState(
    val audioConfig: AudioConfig = AudioConfig(),
    val isRecording: Boolean = false,
    val isConverting: Boolean = false,
    val lastRecordedFile: File? = null,
    val selectedFileToConvert: File? = null,
    val statusMessage: String? = null,
    val errorMessage: String? = null,
    val currentAmplitude: Float = 0f,
    val showSaveDialog: Boolean = false,
    val tempRecordedFile: File? = null,
    val suggestedFileName: String = ""
)
