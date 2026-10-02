package com.rajatnagpure.pcmplayerconverter.util

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri

object PlayStore {

    /** Opens the Play Store listing; falls back to the web listing when the Store app is missing. */
    fun openListing(context: Context, packageName: String, referrer: String? = null) {
        val query = buildString {
            append("id=").append(packageName)
            if (referrer != null) append("&referrer=").append(Uri.encode(referrer))
        }
        val market = Intent(Intent.ACTION_VIEW, Uri.parse("market://details?$query"))
        val web = Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?$query"))
        listOf(market, web).forEach { intent ->
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
