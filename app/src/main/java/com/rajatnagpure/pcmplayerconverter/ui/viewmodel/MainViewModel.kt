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

@HiltViewModel
class MainViewModel @Inject constructor(
    private val pcmPlayer: PcmPlayer
) : ViewModel() {

    val isPlaying: StateFlow<Boolean> = pcmPlayer.isPlayingFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val currentFile: StateFlow<File?> = pcmPlayer.currentFile
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val progress: StateFlow<Float> = pcmPlayer.progressFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0f)

    fun stopPlayback() {
        pcmPlayer.stop()
    }

    fun seekTo(progress: Float) {
        pcmPlayer.seekTo(progress)
    }
}
