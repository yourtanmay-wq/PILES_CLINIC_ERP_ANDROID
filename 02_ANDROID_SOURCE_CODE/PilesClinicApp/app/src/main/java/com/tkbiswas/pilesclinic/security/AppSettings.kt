package com.tkbiswas.pilesclinic.security

import android.content.Context

/**
 * Simple app-wide settings store, native SharedPreferences (no cloud dependency —
 * these are device-local behaviour toggles, not clinical data).
 */
object AppSettings {

    private const val PREFS_NAME = "app_settings"
    private const val KEY_SESSION_TIMEOUT_MIN = "session_timeout_minutes"
    private const val KEY_AUTO_SYNC_ENABLED = "auto_sync_enabled"
    private const val KEY_CRASH_LOGGING_ENABLED = "crash_logging_enabled"
    private const val KEY_LAST_CRASH_LOG_PATH = "last_crash_log_path"

    private fun prefs(context: Context) =
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun getSessionTimeoutMinutes(context: Context): Int =
        prefs(context).getInt(KEY_SESSION_TIMEOUT_MIN, 30)

    fun setSessionTimeoutMinutes(context: Context, minutes: Int) {
        prefs(context).edit().putInt(KEY_SESSION_TIMEOUT_MIN, minutes.coerceIn(1, 240)).apply()
    }

    fun isAutoSyncEnabled(context: Context): Boolean =
        prefs(context).getBoolean(KEY_AUTO_SYNC_ENABLED, true)

    fun setAutoSyncEnabled(context: Context, enabled: Boolean) {
        prefs(context).edit().putBoolean(KEY_AUTO_SYNC_ENABLED, enabled).apply()
    }

    fun isCrashLoggingEnabled(context: Context): Boolean =
        prefs(context).getBoolean(KEY_CRASH_LOGGING_ENABLED, true)

    fun setCrashLoggingEnabled(context: Context, enabled: Boolean) {
        prefs(context).edit().putBoolean(KEY_CRASH_LOGGING_ENABLED, enabled).apply()
    }

    fun getLastCrashLogPath(context: Context): String? =
        prefs(context).getString(KEY_LAST_CRASH_LOG_PATH, null)

    fun setLastCrashLogPath(context: Context, path: String) {
        prefs(context).edit().putString(KEY_LAST_CRASH_LOG_PATH, path).apply()
    }
}
