package com.tkbiswas.pilesclinic.security

import android.content.Context
import com.tkbiswas.pilesclinic.data.session.SessionManager

/**
 * Session Management (Phase 9): if the app sits untouched longer than the
 * configured timeout (default 30 min), the next resume clears the Supabase
 * session (data/session/SessionManager from Phase 5) so a shared clinic
 * tablet doesn't stay signed in indefinitely. Local/offline data is never
 * touched — only the signed-in auth session is cleared.
 */
object SessionTimeoutManager {

    private const val PREFS_NAME = "session_timeout"
    private const val KEY_LAST_ACTIVE_AT = "last_active_at"

    private fun prefs(context: Context) =
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun recordActivityNow(context: Context) {
        prefs(context).edit().putLong(KEY_LAST_ACTIVE_AT, System.currentTimeMillis()).apply()
    }

    private fun lastActiveAt(context: Context): Long =
        prefs(context).getLong(KEY_LAST_ACTIVE_AT, System.currentTimeMillis())

    fun isTimedOut(context: Context): Boolean {
        val timeoutMillis = AppSettings.getSessionTimeoutMinutes(context) * 60_000L
        val elapsed = System.currentTimeMillis() - lastActiveAt(context)
        return elapsed > timeoutMillis
    }

    /** Called on every app resume. Auto-logout/session-timeout has been DISABLED
     * per requirement: the login must stay active until the user logs out
     * manually. This now only records activity time and never clears the session. */
    fun enforceTimeoutIfNeeded(context: Context) {
        // Auto-logout removed intentionally. No session is cleared here.
        recordActivityNow(context)
    }
}
