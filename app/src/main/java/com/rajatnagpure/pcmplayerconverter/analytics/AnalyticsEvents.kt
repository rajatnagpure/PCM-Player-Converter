package com.rajatnagpure.pcmplayerconverter.analytics

/**
 * Single source of truth for event/param names. GA4 limits: names <= 40 chars,
 * string values <= 100 chars, <= 25 params per event. See docs/FIREBASE_SETUP.md.
 */
object AnalyticsEvents {
    // Events
    const val FILE_IMPORT = "file_import"
    const val FILE_IMPORT_FAILED = "file_import_failed"
    const val EXTERNAL_FILE_OPEN = "external_file_open"
    const val CONVERSION_START = "conversion_start"
    const val CONVERSION_COMPLETE = "conversion_complete"
    const val CONVERSION_FAILED = "conversion_failed"
    const val CONVERSION_CANCELLED = "conversion_cancelled"
    const val PLAYBACK_START = "playback_start"
    const val PLAYBACK_STOP = "playback_stop"
    const val RECORDING_START = "recording_start"
    const val RECORDING_STOP = "recording_stop"
    const val RECORDING_FAILED = "recording_failed"
    const val RECORDING_SAVED = "recording_saved"
    const val PERMISSION_RESULT = "permission_result"
    const val SETTINGS_CHANGED = "settings_changed"
    const val DRAWER_ACTION = "drawer_action"
    const val SHARE = "share" // GA4 recommended event
    const val PROMO_IMPRESSION = "promo_impression"
    const val PROMO_CLICK = "promo_click"
    const val PROMO_DISMISS = "promo_dismiss"

    // Params
    const val P_SOURCE = "source"
    const val P_TARGET = "target"
    const val P_FILE_EXT = "file_ext"
    const val P_SIZE_BUCKET = "size_bucket"
    const val P_OUTPUT_SIZE_BUCKET = "output_size_bucket"
    const val P_DIRECTION = "direction"
    const val P_OUTPUT_FORMAT = "output_format"
    const val P_SAMPLE_RATE = "sample_rate"
    const val P_CHANNELS = "channels"
    const val P_ENCODING = "encoding"
    const val P_DURATION_MS = "duration_ms"
    const val P_DURATION_S = "duration_s"
    const val P_ERROR_TYPE = "error_type"
    const val P_STAGE = "stage"
    const val P_SCREEN = "screen"
    const val P_PERMISSION = "permission"
    const val P_GRANTED = "granted"
    const val P_SETTING = "setting"
    const val P_VALUE = "value"
    const val P_ITEM = "item"
    const val P_ACTION = "action"
    const val P_MIME_GROUP = "mime_group"
    const val P_PROMO_ID = "promo_id"
    const val P_IMPRESSION_N = "impression_n"
    const val P_METHOD = "method"
    const val P_CONTENT_TYPE = "content_type"

    // Values
    const val SOURCE_PICKER = "picker"
    const val SOURCE_EXTERNAL = "external_intent"
    const val SOURCE_RECORDING = "recording"
    const val DIRECTION_PCM_TO_AUDIO = "pcm_to_audio"
    const val DIRECTION_AUDIO_TO_PCM = "audio_to_pcm"
    const val SCREEN_CONVERTER = "converter"
    const val SCREEN_GENERATOR = "generator"
    const val SCREEN_HELP = "help"
    const val SCREEN_PLAYER = "player"
    const val SCREEN_SETTINGS = "settings"
    const val PROMO_FLOODFILL = "floodfill"

    // User properties (max 25 per project, names <= 24 chars)
    const val UP_THEME = "theme"
    const val UP_HAPTICS = "haptics_enabled"
    const val UP_HAS_CONVERTED = "has_converted"

    /** Coarse size buckets: enough to correlate failures/duration with size, without fingerprinting. */
    fun sizeBucket(bytes: Long): String = when {
        bytes < 0 -> "unknown"
        bytes < 100L * 1024 -> "<100KB"
        bytes < 1024L * 1024 -> "100KB-1MB"
        bytes < 10L * 1024 * 1024 -> "1-10MB"
        bytes < 50L * 1024 * 1024 -> "10-50MB"
        bytes < 200L * 1024 * 1024 -> "50-200MB"
        else -> ">200MB"
    }

    /** Lower-case extension without the dot, capped so odd names can't leak into reports. */
    fun extensionOf(fileName: String?): String {
        val ext = fileName?.substringAfterLast('.', "")?.lowercase().orEmpty()
        return if (ext.isNotEmpty() && ext.length <= 5 && ext.all { it.isLetterOrDigit() }) ext else "none"
    }

    fun errorType(t: Throwable?): String = t?.javaClass?.simpleName?.take(100) ?: "unknown"
}
