package com.rajatnagpure.pcmplayerconverter.ui.viewmodel

import android.net.Uri
import android.os.Environment
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
    savedStateHandle: androidx.lifecycle.SavedStateHandle
) : ViewModel() {

    init {
        savedStateHandle.get<String>("uri")?.let { uriStr ->
            try {
                val uri = Uri.parse(java.net.URLDecoder.decode(uriStr, "UTF-8"))
                val file = localFileDataSource.getFileFromUri(uri)
                if (file != null) {
                    _uiState.value = _uiState.value.copy(selectedFileToConvert = file)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private val _uiState = MutableStateFlow(GeneratorUiState())
    val uiState: StateFlow<GeneratorUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            recordAudioUseCase.amplitudeFlow.collect { amp ->
                _uiState.value = _uiState.value.copy(currentAmplitude = amp)
            }
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
                _uiState.value = _uiState.value.copy(
                    isRecording = false, 
                    showSaveDialog = true,
                    tempRecordedFile = _uiState.value.lastRecordedFile
                )
            } else {
                val cacheDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                val file = File(cacheDir, "temp_recording.pcm") // Use temporary name, rename later
                _uiState.value = _uiState.value.copy(isRecording = true, statusMessage = "Recording...", lastRecordedFile = file)
                try {
                    recordAudioUseCase.start(file, config)
                } catch (e: Exception) {
                    _uiState.value = _uiState.value.copy(isRecording = false, errorMessage = "Recording error: ${e.message}")
                }
            }
        }
    }

    fun onFileSelectedForConversion(uri: Uri) {
         viewModelScope.launch {
            val file = localFileDataSource.getFileFromUri(uri)
            if (file != null) {
                _uiState.value = _uiState.value.copy(selectedFileToConvert = file, errorMessage = null)
            } else {
                _uiState.value = _uiState.value.copy(errorMessage = "Failed to load file")
            }
        }
    }

    fun convertToPcm() {
        val inFile = _uiState.value.selectedFileToConvert ?: return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isConverting = true, errorMessage = null)
            val outFile = File(inFile.parent, "${inFile.nameWithoutExtension}.pcm")
            val result = convertAudioToPcmUseCase(inFile, outFile)
            if (result.isSuccess) {
                 _uiState.value = _uiState.value.copy(isConverting = false, statusMessage = "Converted to ${outFile.absolutePath}")
            } else {
                 _uiState.value = _uiState.value.copy(isConverting = false, errorMessage = result.exceptionOrNull()?.message)
            }
        }
    }
    fun saveRecording(fileName: String) {
        val tempFile = _uiState.value.tempRecordedFile ?: return
        val finalFile = File(tempFile.parent, if (fileName.endsWith(".pcm")) fileName else "$fileName.pcm")
        tempFile.renameTo(finalFile)
        _uiState.value = _uiState.value.copy(
            showSaveDialog = false, 
            statusMessage = "Saved to ${finalFile.name}",
            lastRecordedFile = finalFile
        )
    }

    fun cancelSave() {
        _uiState.value.tempRecordedFile?.delete()
        _uiState.value = _uiState.value.copy(showSaveDialog = false, tempRecordedFile = null)
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
    val tempRecordedFile: File? = null
)
