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

/** Drives the dismissable "Try Flood Fill" banner. Capping rules live in [PromoCapPolicy]. */
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

    private suspend fun evaluate() {
        if (promoRepository.closedThisSession) return
        if (promoRepository.shownThisSession) {
            // Same launch, e.g. activity recreated: keep showing without counting a new impression
            _visible.value = true
            return
        }
        val eligible = PromoCapPolicy.shouldShow(
            state = promoRepository.state(),
            config = remoteConfigRepository.promoConfig(),
            nowMs = clock(),
            isTargetInstalled = isInstalled(AppConfig.FLOODFILL_PACKAGE)
        )
        if (!eligible) return
        // Never interrupt an active conversion/recording
        conversionEvents.isBusy.first { !it }
        val impression = promoRepository.recordImpression(clock())
        _visible.value = true
        analytics.logEvent(
            AnalyticsEvents.PROMO_IMPRESSION,
            mapOf(AnalyticsEvents.P_PROMO_ID to AnalyticsEvents.PROMO_FLOODFILL, AnalyticsEvents.P_IMPRESSION_N to impression)
        )
    }

    fun onClick() {
        promoRepository.recordClick()
        _visible.value = false
        analytics.logEvent(
            AnalyticsEvents.PROMO_CLICK,
            mapOf(AnalyticsEvents.P_PROMO_ID to AnalyticsEvents.PROMO_FLOODFILL, AnalyticsEvents.P_IMPRESSION_N to promoRepository.state().impressions)
        )
    }

    fun onDismiss() {
        promoRepository.recordDismiss(clock(), remoteConfigRepository.promoConfig().dismissSnoozeDays)
        _visible.value = false
        analytics.logEvent(
            AnalyticsEvents.PROMO_DISMISS,
            mapOf(AnalyticsEvents.P_PROMO_ID to AnalyticsEvents.PROMO_FLOODFILL, AnalyticsEvents.P_IMPRESSION_N to promoRepository.state().impressions)
        )
    }

    private fun isInstalled(packageName: String): Boolean = try {
        context.packageManager.getPackageInfo(packageName, 0)
        true
    } catch (e: PackageManager.NameNotFoundException) {
        false
    }
}
