package com.rajatnagpure.pcmplayerconverter.ui.viewmodel
import com.rajatnagpure.pcmplayerconverter.ui.components.AppText as Text

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

import android.content.Context
import com.rajatnagpure.pcmplayerconverter.analytics.AnalyticsEvents
import com.rajatnagpure.pcmplayerconverter.analytics.AnalyticsTracker
import com.rajatnagpure.pcmplayerconverter.data.repository.ThemeRepository
import com.rajatnagpure.pcmplayerconverter.ui.theme.NeuTheme
import com.rajatnagpure.pcmplayerconverter.util.HapticsManager

@HiltViewModel
class MainViewModel @Inject constructor(
    private val pcmPlayer: PcmPlayer,
    private val themeRepository: ThemeRepository,
    private val analytics: AnalyticsTracker
) : ViewModel() {

    val currentTheme: StateFlow<NeuTheme> = themeRepository.currentTheme

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

    private val _analyticsEnabled = kotlinx.coroutines.flow.MutableStateFlow(analytics.isCollectionEnabled)
    val analyticsEnabled: StateFlow<Boolean> = _analyticsEnabled.asStateFlow()

    private var lastScreen: String? = null

    init {
        // Segment every report by these
        analytics.setUserProperty(AnalyticsEvents.UP_THEME, themeRepository.currentTheme.value.name.lowercase())
        analytics.setUserProperty(AnalyticsEvents.UP_HAPTICS, HapticsManager.isEnabled.toString())

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

    fun setTheme(theme: NeuTheme) {
        themeRepository.setTheme(theme)
        analytics.setUserProperty(AnalyticsEvents.UP_THEME, theme.name.lowercase())
        logSettingChanged("theme", theme.name.lowercase())
    }

    fun setHapticsEnabled(context: Context, enabled: Boolean) {
        HapticsManager.setEnabled(context, enabled)
        analytics.setUserProperty(AnalyticsEvents.UP_HAPTICS, enabled.toString())
        logSettingChanged("haptics", enabled.toString())
    }

    fun setAnalyticsEnabled(enabled: Boolean) {
        // Log the opt-out before collection stops so the opt-out rate is measurable
        logSettingChanged("analytics", enabled.toString())
        analytics.setCollectionEnabled(enabled)
        _analyticsEnabled.value = enabled
    }

    /** Manual screen tracking: Compose navigation is invisible to Firebase's automatic Activity tracking. */
    fun trackScreen(screenName: String) {
        if (screenName == lastScreen) return
        lastScreen = screenName
        analytics.logScreen(screenName)
    }

    /** For overlays (dialogs) that should count as a screen view every time they open. */
    fun trackOverlay(screenName: String) {
        analytics.logScreen(screenName)
    }

    fun logDrawerAction(item: String) {
        analytics.logEvent(AnalyticsEvents.DRAWER_ACTION, mapOf(AnalyticsEvents.P_ITEM to item))
        if (item == "share_app") {
            analytics.logEvent(
                AnalyticsEvents.SHARE,
                mapOf(AnalyticsEvents.P_METHOD to "system_chooser", AnalyticsEvents.P_CONTENT_TYPE to "app")
            )
        }
    }

    private fun logSettingChanged(setting: String, value: String) {
        analytics.logEvent(
            AnalyticsEvents.SETTINGS_CHANGED,
            mapOf(AnalyticsEvents.P_SETTING to setting, AnalyticsEvents.P_VALUE to value)
        )
    }
}
