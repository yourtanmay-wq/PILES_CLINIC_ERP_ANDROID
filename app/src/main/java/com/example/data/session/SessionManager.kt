package com.example.data.session

import android.content.Context
import android.content.SharedPreferences

class SessionManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("piles_clinic_erp_session", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_ROLE = "user_role"
        private const val KEY_LOGGED_IN = "is_logged_in"
    }

    fun saveSession(role: String) {
        prefs.edit().apply {
            putString(KEY_ROLE, role)
            putBoolean(KEY_LOGGED_IN, true)
            apply()
        }
    }

    fun getSessionRole(): String? {
        return if (prefs.getBoolean(KEY_LOGGED_IN, false)) {
            prefs.getString(KEY_ROLE, null)
        } else {
            null
        }
    }

    fun clearSession() {
        prefs.edit().apply {
            remove(KEY_ROLE)
            putBoolean(KEY_LOGGED_IN, false)
            apply()
        }
    }
}
