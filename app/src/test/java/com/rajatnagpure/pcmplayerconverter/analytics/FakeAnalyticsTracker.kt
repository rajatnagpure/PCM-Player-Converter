package com.rajatnagpure.pcmplayerconverter.analytics

/** Records calls so tests can assert on analytics without Firebase. */
class FakeAnalyticsTracker : AnalyticsTracker {
    data class Event(val name: String, val params: Map<String, Any?>)

    val events = mutableListOf<Event>()
    val userProperties = mutableMapOf<String, String?>()
    val nonFatals = mutableListOf<Throwable>()
    override var isCollectionEnabled: Boolean = true
        private set

    override fun logEvent(name: String, params: Map<String, Any?>) {
        events += Event(name, params)
    }

    override fun logScreen(screenName: String) {
        events += Event("screen_view", mapOf("screen_name" to screenName))
    }

    override fun setUserProperty(name: String, value: String?) {
        userProperties[name] = value
    }

    override fun recordNonFatal(throwable: Throwable, context: Map<String, String>) {
        nonFatals += throwable
    }

    override fun setCollectionEnabled(enabled: Boolean) {
        isCollectionEnabled = enabled
    }

    fun named(name: String) = events.filter { it.name == name }
}
