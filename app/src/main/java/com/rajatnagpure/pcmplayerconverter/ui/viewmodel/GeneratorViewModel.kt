package com.rajatnagpure.pcmplayerconverter.ui.viewmodel

import android.net.Uri
import android.os.Environment
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rajatnagpure.pcmplayerconverter.data.local.LocalFileDataSource
import com.rajatnagpure.pcmplayerconverter.domain.model.AudioConfig
import com.rajatnagpure.pcmplayerconverter.domain.usecase.ConvertAudioToPcmUseCase
import com.rajatnagpure.pcmplayerconverter.domain.usecase.RecordAudioUseCase
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
    private val application: android.app.Application
) : ViewModel() {

    // Move UI state declaration before init blocks to ensure it's initialized when collectors run
    private val _uiState = MutableStateFlow(GeneratorUiState())
    val uiState: StateFlow<GeneratorUiState> = _uiState.asStateFlow()

    private val conversionReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent == null) return
            if (intent.action == com.rajatnagpure.pcmplayerconverter.service.ConversionService.ACTION_CONVERSION_COMPLETE) {
                val success = intent.getBooleanExtra("success", false)
                val outPath = intent.getStringExtra("outFile")
                viewModelScope.launch {
                    if (success) {
                        _uiState.value = _uiState.value.copy(isConverting = false, statusMessage = "Converted to ${outPath?.substringAfterLast('/')}" )
                    } else {
                        _uiState.value = _uiState.value.copy(isConverting = false, errorMessage = intent.getStringExtra("message"))
                    }
                }
            }
        }
    }

    init {
        // register receiver
        val filter = IntentFilter().apply {
            addAction(com.rajatnagpure.pcmplayerconverter.service.ConversionService.ACTION_CONVERSION_COMPLETE)
        }
        application.registerReceiver(conversionReceiver, filter)

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

    override fun onCleared() {
        super.onCleared()
        try {
            application.unregisterReceiver(conversionReceiver)
        } catch (e: Exception) {
            // ignore
        }
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

    fun saveFile(fileName: String) {
        val tempFile = _uiState.value.tempRecordedFile
        if (tempFile != null) {
            // Saving a recording
            val finalFile = File(tempFile.parent, if (fileName.endsWith(".pcm")) fileName else "$fileName.pcm")
            tempFile.renameTo(finalFile)
            _uiState.value = _uiState.value.copy(
                showSaveDialog = false, 
                statusMessage = "Saved recording as ${finalFile.name}",
                lastRecordedFile = finalFile
            )
            // Toast removed: UI will display statusMessage once and clear it
        } else {
            // Saving a conversion - now delegate to ConversionService for background processing
            val inFile = _uiState.value.selectedFileToConvert ?: return
            val outFile = File(inFile.parent, fileName)
            _uiState.value = _uiState.value.copy(isConverting = true, showSaveDialog = false, errorMessage = null)

            // start background ConversionService with task AUDIO_TO_PCM
            val intent = android.content.Intent(application, com.rajatnagpure.pcmplayerconverter.service.ConversionService::class.java).apply {
                putExtra("inFile", inFile)
                putExtra("outFile", outFile)
                putExtra("task", "AUDIO_TO_PCM")
            }

            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                application.startForegroundService(intent)
            } else {
                application.startService(intent)
            }
        }
    }

    private fun performPcmConversion(fileName: String) {
        val inFile = _uiState.value.selectedFileToConvert ?: return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isConverting = true, showSaveDialog = false, errorMessage = null)
            val outFile = File(inFile.parent, fileName)
            val result = convertAudioToPcmUseCase(inFile, outFile)
            if (result.isSuccess) {
                 _uiState.value = _uiState.value.copy(isConverting = false, statusMessage = "Converted to ${outFile.name}")
                 // Toast removed: UI will display statusMessage once and clear it
            } else {
                 _uiState.value = _uiState.value.copy(isConverting = false, errorMessage = result.exceptionOrNull()?.message)
                 // Error toast removed; UI shows errorMessage
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
