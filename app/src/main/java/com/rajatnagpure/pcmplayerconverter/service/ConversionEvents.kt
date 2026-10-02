package com.rajatnagpure.pcmplayerconverter.service

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import javax.inject.Inject
import javax.inject.Singleton

enum class ConversionOrigin { CONVERTER, GENERATOR }

data class ConversionResult(
    val jobId: Long,
    val origin: ConversionOrigin,
    val success: Boolean,
    val outputName: String? = null,
    val errorMessage: String? = null
)

/**
 * In-process channel between [ConversionService] and the screens that started a job.
 * Results are held until the owning screen acknowledges them, so a result that lands while the
 * ViewModel is being recreated is still shown, and a Converter job never surfaces on Generator.
 */
@Singleton
class ConversionEvents @Inject constructor() {

    private val pending = MutableStateFlow<Map<ConversionOrigin, ConversionResult>>(emptyMap())
    private val running = MutableStateFlow<Set<String>>(emptySet())

    /** True while a conversion or recording is in progress (used to hold back the promo banner). */
    val isBusy: Flow<Boolean> = running.map { it.isNotEmpty() }.distinctUntilChanged()
    val runningTasks: StateFlow<Set<String>> = running.asStateFlow()

    fun results(origin: ConversionOrigin): Flow<ConversionResult> =
        pending.map { it[origin] }.filterNotNull().distinctUntilChanged()

    fun publish(result: ConversionResult) {
        pending.update { it + (result.origin to result) }
    }

    fun acknowledge(result: ConversionResult) {
        pending.update { if (it[result.origin]?.jobId == result.jobId) it - result.origin else it }
    }

    fun setBusy(task: String, busy: Boolean) {
        running.update { if (busy) it + task else it - task }
    }

    companion object {
        const val TASK_CONVERSION = "conversion"
        const val TASK_RECORDING = "recording"
    }
}
