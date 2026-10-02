package com.rajatnagpure.pcmplayerconverter.ui.viewmodel
import com.rajatnagpure.pcmplayerconverter.ui.components.AppText as Text

import android.net.Uri
import android.os.Environment
import android.os.SystemClock
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rajatnagpure.pcmplayerconverter.analytics.AnalyticsEvents
import com.rajatnagpure.pcmplayerconverter.analytics.AnalyticsTracker
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
    private val conversionEvents: ConversionEvents,
    private val analytics: AnalyticsTracker
) : ViewModel() {

    // Move UI state declaration before init blocks to ensure it's initialized when collectors run
    private val _uiState = MutableStateFlow(GeneratorUiState())
    val uiState: StateFlow<GeneratorUiState> = _uiState.asStateFlow()

    // Analytics bookkeeping (no PII)
    private var lastImportedUri: String? = null
    private var recordingStartedAt = 0L

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
                        
                        onFileSelectedForConversion(uri, AnalyticsEvents.SOURCE_EXTERNAL)
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
        conversionEvents.setBusy(ConversionEvents.TASK_RECORDING, false)
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

    fun onRecordPermissionResult(granted: Boolean) {
        analytics.logEvent(
            AnalyticsEvents.PERMISSION_RESULT,
            mapOf(AnalyticsEvents.P_PERMISSION to "record_audio", AnalyticsEvents.P_GRANTED to granted)
        )
        if (granted) toggleRecording()
    }

    fun onNotificationPermissionResult(granted: Boolean) {
        analytics.logEvent(
            AnalyticsEvents.PERMISSION_RESULT,
            mapOf(AnalyticsEvents.P_PERMISSION to "post_notifications", AnalyticsEvents.P_GRANTED to granted)
        )
    }

    fun updateConfig(config: AudioConfig) {
        _uiState.value = _uiState.value.copy(audioConfig = config)
    }

    fun toggleRecording() {
        val config = _uiState.value.audioConfig
        viewModelScope.launch {
            if (_uiState.value.isRecording) {
                recordAudioUseCase.stop()
                conversionEvents.setBusy(ConversionEvents.TASK_RECORDING, false)
                analytics.logEvent(
                    AnalyticsEvents.RECORDING_STOP,
                    recordingParams(config) + (AnalyticsEvents.P_DURATION_S to (SystemClock.elapsedRealtime() - recordingStartedAt) / 1000)
                )
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
                conversionEvents.setBusy(ConversionEvents.TASK_RECORDING, true)
                recordingStartedAt = SystemClock.elapsedRealtime()
                analytics.logEvent(AnalyticsEvents.RECORDING_START, recordingParams(config))
                try {
                    recordAudioUseCase.start(file, config)
                } catch (e: Exception) {
                    _uiState.value = _uiState.value.copy(isRecording = false, errorMessage = "Recording error: ${e.message}")
                    conversionEvents.setBusy(ConversionEvents.TASK_RECORDING, false)
                    analytics.logEvent(
                        AnalyticsEvents.RECORDING_FAILED,
                        recordingParams(config) + (AnalyticsEvents.P_ERROR_TYPE to AnalyticsEvents.errorType(e))
                    )
                    analytics.recordNonFatal(e, mapOf("feature" to "recording"))
                }
            }
        }
    }

    private fun recordingParams(config: AudioConfig): Map<String, Any?> = mapOf(
        AnalyticsEvents.P_SAMPLE_RATE to config.sampleRate,
        AnalyticsEvents.P_CHANNELS to config.channels,
        AnalyticsEvents.P_ENCODING to config.encoding.name.lowercase()
    )

    fun onFileSelectedForConversion(uri: Uri, source: String = AnalyticsEvents.SOURCE_PICKER) {
        android.util.Log.d("GeneratorViewModel", "onFileSelectedForConversion called with URI: $uri")
        viewModelScope.launch {
            val file = localFileDataSource.getFileFromUri(uri)
            if (file != null) {
                android.util.Log.d("GeneratorViewModel", "File loaded successfully: ${file.name}")
                _uiState.value = _uiState.value.copy(selectedFileToConvert = file, errorMessage = null)
                if (lastImportedUri != uri.toString()) {
                    lastImportedUri = uri.toString()
                    analytics.logEvent(
                        AnalyticsEvents.FILE_IMPORT,
                        mapOf(
                            AnalyticsEvents.P_SOURCE to source,
                            AnalyticsEvents.P_TARGET to AnalyticsEvents.SCREEN_GENERATOR,
                            AnalyticsEvents.P_FILE_EXT to AnalyticsEvents.extensionOf(file.name),
                            AnalyticsEvents.P_SIZE_BUCKET to AnalyticsEvents.sizeBucket(file.length())
                        )
                    )
                }
            } else {
                android.util.Log.e("GeneratorViewModel", "Failed to load file from URI")
                _uiState.value = _uiState.value.copy(errorMessage = "Failed to load file")
                analytics.logEvent(
                    AnalyticsEvents.FILE_IMPORT_FAILED,
                    mapOf(AnalyticsEvents.P_SOURCE to source, AnalyticsEvents.P_TARGET to AnalyticsEvents.SCREEN_GENERATOR)
                )
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
                    analytics.logEvent(
                        AnalyticsEvents.RECORDING_SAVED,
                        mapOf(AnalyticsEvents.P_SIZE_BUCKET to AnalyticsEvents.sizeBucket(tempFile.length()))
                    )
                    tempFile.delete()
                    _uiState.value = _uiState.value.copy(
                        isConverting = false, 
                        statusMessage = "Recording saved successfully",
                        lastRecordedFile = null, // Clear reference to deleted temp file
                        tempRecordedFile = null
                    )
                } catch (e: Exception) {
                    _uiState.value = _uiState.value.copy(isConverting = false, errorMessage = "Save failed: ${e.message}")
                    analytics.recordNonFatal(e, mapOf("feature" to "save_recording"))
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
        analytics.logEvent(
            AnalyticsEvents.CONVERSION_CANCELLED,
            mapOf(
                AnalyticsEvents.P_DIRECTION to if (_uiState.value.tempRecordedFile != null) AnalyticsEvents.SOURCE_RECORDING else AnalyticsEvents.DIRECTION_AUDIO_TO_PCM
            )
        )
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
