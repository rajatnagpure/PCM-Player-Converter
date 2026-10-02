package com.rajatnagpure.pcmplayerconverter.analytics

/**
 * App-wide analytics facade. Inject this instead of touching Firebase directly so that
 * ViewModels/services stay unit-testable and a build without google-services.json still runs.
 *
 * Privacy: never pass file names, paths, URIs or free text typed by the user. Only enums,
 * extensions, buckets and exception class names.
 */
interface AnalyticsTracker {
    val isCollectionEnabled: Boolean

    fun logEvent(name: String, params: Map<String, Any?> = emptyMap())
    fun logScreen(screenName: String)
    fun setUserProperty(name: String, value: String?)
    fun recordNonFatal(throwable: Throwable, context: Map<String, String> = emptyMap())

    /** Persists the user's opt-in choice and applies it to Analytics + Crashlytics. */
    fun setCollectionEnabled(enabled: Boolean)
}
