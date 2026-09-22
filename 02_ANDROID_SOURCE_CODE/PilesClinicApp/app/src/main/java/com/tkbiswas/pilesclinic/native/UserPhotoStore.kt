package com.tkbiswas.pilesclinic.native

import android.content.Context

/**
 * Native rebuild -- staff/doctor avatar photo store.
 *
 * The WebView keeps user photos in localStorage (rk_userPhotos), device-local
 * and cosmetic. This mirrors that with SharedPreferences keyed by the user's
 * 10-digit mobile, so the behaviour matches the original (per-device avatar,
 * not synced). Stored value is the same data-URL string format used elsewhere.
 */
object UserPhotoStore {

    private const val PREFS = "piles_clinic_user_photos"

    private fun mob(raw: String): String = raw.filter { it.isDigit() }.takeLast(10)

    fun get(context: Context, mobile: String): String? {
        val p = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val v = p.getString(mob(mobile), null)
        return if (v.isNullOrBlank()) null else v
    }

    fun set(context: Context, mobile: String, dataUrl: String) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putString(mob(mobile), dataUrl).apply()
    }

    fun clear(context: Context, mobile: String) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .remove(mob(mobile)).apply()
    }
}
