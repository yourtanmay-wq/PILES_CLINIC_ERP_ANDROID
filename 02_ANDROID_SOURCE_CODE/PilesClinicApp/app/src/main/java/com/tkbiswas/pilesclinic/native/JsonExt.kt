package com.tkbiswas.pilesclinic.native

import org.json.JSONObject

/**
 * Null-safe string read for Supabase JSON rows.
 *
 * org.json's optString() returns the literal string "null" when a key's value
 * is JSON null (which is how Supabase sends empty text columns). That caused
 * "null" to show on screen (e.g. "Last Remark: null"). This helper returns an
 * empty string for both missing keys and JSON-null values, so the UI shows
 * blanks/fallbacks exactly like the original web app.
 */
fun JSONObject.s(key: String): String =
    if (isNull(key)) "" else optString(key, "")
