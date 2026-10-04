package com.rajatnagpure.pcmplayerconverter.config

object AppConfig {
    const val APP_PACKAGE = "com.rajatnagpure.pcmplayerconverter"
    const val DEVELOPER_PLAYSTORE_SEARCH_URL = "https://play.google.com/store/apps/developer?id=Rajat+Nagpure&hl=en_IN"
    const val APP_PLAYSTORE_DETAILS_URL = "market://details?id=$APP_PACKAGE"
    const val APP_SHARE_TEXT_PREFIX = "Working with PCM Raw audio data? Check out this app: https://play.google.com/store/apps/details?id=$APP_PACKAGE"
    const val FEATURE_REQUEST_FORM_URL = "https://forms.gle/Gz8mufBgiUBjV19s6"
    const val GITHUB_REPO_URL = "https://github.com/rajatnagpure/PCM-Player-Converter"
    const val APP_NAME = "PCM CONVERTER"

    // Cross-promotion: Color Shift puzzle game (package id kept from its Flood Fill days; also in <queries>)
    const val FLOODFILL_PACKAGE = "com.rajatnagpure.floodfill"
    /**
     * Exact listing URL opened by the banner. The `referrer` carries the UTM tags
     * (utm_source=pcm_player_converter, utm_medium=cross_promo, utm_campaign=in_app_banner) so installs
     * from the banner are attributed in Play Console and in Color Shift's own Firebase/GA data.
     */
    const val COLOR_SHIFT_PLAY_URL =
        "https://play.google.com/store/apps/details?id=com.rajatnagpure.floodfill" +
            "&referrer=utm_source%3Dpcm_player_converter%26utm_medium%3Dcross_promo%26utm_campaign%3Din_app_banner"
}
