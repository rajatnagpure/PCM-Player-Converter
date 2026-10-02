package com.rajatnagpure.pcmplayerconverter.ui.viewmodel

import android.content.Context
import android.content.pm.PackageManager
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rajatnagpure.pcmplayerconverter.analytics.AnalyticsEvents
import com.rajatnagpure.pcmplayerconverter.analytics.AnalyticsTracker
import com.rajatnagpure.pcmplayerconverter.config.AppConfig
import com.rajatnagpure.pcmplayerconverter.data.repository.PromoRepository
import com.rajatnagpure.pcmplayerconverter.data.repository.RemoteConfigRepository
import com.rajatnagpure.pcmplayerconverter.domain.promo.PromoCapPolicy
import com.rajatnagpure.pcmplayerconverter.service.ConversionEvents
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/** Drives the dismissable "Try Color Shift" banner. Capping rules live in [PromoCapPolicy]. */
@HiltViewModel
class PromoViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val promoRepository: PromoRepository,
    private val remoteConfigRepository: RemoteConfigRepository,
    private val conversionEvents: ConversionEvents,
    private val analytics: AnalyticsTracker
) : ViewModel() {

    private val _visible = MutableStateFlow(false)
    val visible: StateFlow<Boolean> = _visible.asStateFlow()

    private val clock: () -> Long = System::currentTimeMillis

    init {
        viewModelScope.launch { evaluate() }
    }

    /**
     * Shows the banner whenever the policy allows. Called at launch and again on every resume, so
     * the banner disappears as soon as the user comes back from the Play Store with the game installed.
     */
    fun refresh() {
        viewModelScope.launch { evaluate() }
    }

    private suspend fun evaluate() {
        val eligible = PromoCapPolicy.shouldShow(
            state = promoRepository.state(),
            config = remoteConfigRepository.promoConfig(),
            nowMs = clock(),
            isTargetInstalled = isInstalled(AppConfig.FLOODFILL_PACKAGE)
        )
        if (!eligible) {
            _visible.value = false
            return
        }
        if (_visible.value) return
        // Never pop in during an active conversion/recording
        conversionEvents.isBusy.first { !it }
        _visible.value = true
        if (!promoRepository.impressionLoggedThisSession) {
            analytics.logEvent(
                AnalyticsEvents.PROMO_IMPRESSION,
                mapOf(AnalyticsEvents.P_PROMO_ID to AnalyticsEvents.PROMO_FLOODFILL, AnalyticsEvents.P_IMPRESSION_N to promoRepository.recordImpression())
            )
        }
    }

    /** Opens the store only: the banner stays until the game is installed or the user dismisses it. */
    fun onClick() {
        analytics.logEvent(
            AnalyticsEvents.PROMO_CLICK,
            mapOf(AnalyticsEvents.P_PROMO_ID to AnalyticsEvents.PROMO_FLOODFILL, AnalyticsEvents.P_IMPRESSION_N to promoRepository.impressions())
        )
    }

    fun onDismiss() {
        val capped = promoRepository.recordDismiss(clock(), remoteConfigRepository.promoConfig())
        _visible.value = false
        analytics.logEvent(
            AnalyticsEvents.PROMO_DISMISS,
            mapOf(
                AnalyticsEvents.P_PROMO_ID to AnalyticsEvents.PROMO_FLOODFILL,
                AnalyticsEvents.P_IMPRESSION_N to promoRepository.impressions(),
                AnalyticsEvents.P_VALUE to if (capped) "capped_14d" else "snoozed_4d"
            )
        )
    }

    private fun isInstalled(packageName: String): Boolean = try {
        context.packageManager.getPackageInfo(packageName, 0)
        true
    } catch (e: PackageManager.NameNotFoundException) {
        false
    }
}
