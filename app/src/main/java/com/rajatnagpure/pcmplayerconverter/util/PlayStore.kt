package com.rajatnagpure.pcmplayerconverter.util

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri

object PlayStore {

    const val PLAY_STORE_PACKAGE = "com.android.vending"

    /** Opens the Play Store listing; falls back to the web listing when the Store app is missing. */
    fun openListing(context: Context, packageName: String, referrer: String? = null) {
        val query = buildString {
            append("id=").append(packageName)
            if (referrer != null) append("&referrer=").append(Uri.encode(referrer))
        }
        val market = Intent(Intent.ACTION_VIEW, Uri.parse("market://details?$query"))
        val web = Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?$query"))
        startFirst(context, market, web)
    }

    /**
     * Opens an exact play.google.com URL (e.g. one carrying a `referrer` with UTM tags) in the Play Store
     * app, keeping every query parameter; falls back to the browser when the Play Store is missing.
     */
    fun openUrl(context: Context, url: String) {
        val inPlayStore = Intent(Intent.ACTION_VIEW, Uri.parse(url)).setPackage(PLAY_STORE_PACKAGE)
        val inBrowser = Intent(Intent.ACTION_VIEW, Uri.parse(url))
        startFirst(context, inPlayStore, inBrowser)
    }

    private fun startFirst(context: Context, vararg intents: Intent) {
        for (intent in intents) {
            try {
                context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                return
            } catch (_: ActivityNotFoundException) {
                // try next
            }
        }
    }

    /** startActivity that never crashes when no app can handle the intent (e.g. no browser). */
    fun safeStart(context: Context, intent: Intent): Boolean = try {
        context.startActivity(intent)
        true
    } catch (_: ActivityNotFoundException) {
        false
    }
}
