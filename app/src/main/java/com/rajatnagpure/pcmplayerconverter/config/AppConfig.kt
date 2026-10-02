package com.rajatnagpure.pcmplayerconverter.config

object AppConfig {
    const val APP_PACKAGE = "com.rajatnagpure.pcmplayerconverter"
    const val DEVELOPER_PLAYSTORE_SEARCH_URL = "https://play.google.com/store/apps/developer?id=Rajat+Nagpure&hl=en_IN"
    const val APP_PLAYSTORE_DETAILS_URL = "market://details?id=$APP_PACKAGE"
    const val APP_SHARE_TEXT_PREFIX = "Working with PCM Raw audio data? Check out this app: https://play.google.com/store/apps/details?id=$APP_PACKAGE"
    const val FEATURE_REQUEST_FORM_URL = "https://forms.gle/Gz8mufBgiUBjV19s6"
    const val GITHUB_REPO_URL = "https://github.com/rajatnagpure/PCM-Player-Converter"
    const val APP_NAME = "PCM CONVERTER"

    // Cross-promotion: Flood Fill puzzle game (also declared in <queries> in the manifest)
    const val FLOODFILL_PACKAGE = "com.rajatnagpure.floodfill"
    /** Play install referrer so installs from this banner are attributable in Play Console / GA. */
    const val FLOODFILL_REFERRER = "utm_source=pcm_converter&utm_medium=in_app_banner&utm_campaign=cross_promo"
}
