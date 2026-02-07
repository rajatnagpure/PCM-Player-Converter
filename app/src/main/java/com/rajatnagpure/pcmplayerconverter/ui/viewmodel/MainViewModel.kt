package com.rajatnagpure.pcmplayerconverter.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rajatnagpure.pcmplayerconverter.data.audio.PcmPlayer
import dagger.hilt.android.lifecycle.HiltViewModel
import java.io.File
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.collect

@HiltViewModel
class MainViewModel @Inject constructor(
    private val pcmPlayer: PcmPlayer
) : ViewModel() {

    private val _isPlayerVisible = kotlinx.coroutines.flow.MutableStateFlow(false)
    val isPlayerVisible: StateFlow<Boolean> = _isPlayerVisible.asStateFlow()

    val isPlaying: StateFlow<Boolean> = pcmPlayer.isPlayingFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val isPaused: StateFlow<Boolean> = pcmPlayer.isPausedFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val currentFile: StateFlow<File?> = pcmPlayer.currentFile
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val progress: StateFlow<Float> = pcmPlayer.progressFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0f)

    init {
        // Automatically show player when a file starts playing
        viewModelScope.launch {
            pcmPlayer.isPlayingFlow.collect { playing ->
                if (playing) {
                    _isPlayerVisible.value = true
                }
            }
        }
    }

    fun togglePlayback() {
        val playing = isPlaying.value
        val paused = isPaused.value
        val file = currentFile.value
        val lastConfig = pcmPlayer.lastConfig.value

        if (paused) {
            pcmPlayer.resume()
        } else if (playing) {
            pcmPlayer.pause()
        } else if (file != null && lastConfig != null) {
            // Restart playback
            viewModelScope.launch {
                pcmPlayer.play(file, lastConfig)
            }
        }
    }

    fun dismissPlayer() {
        _isPlayerVisible.value = false
        stopPlayback()
    }

    fun stopPlayback() {
        pcmPlayer.stop()
    }

    fun seekTo(progress: Float) {
        pcmPlayer.seekTo(progress)
    }
}
