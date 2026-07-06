package com.example.data.session

import android.content.Context
import android.content.SharedPreferences

class SessionManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("piles_clinic_erp_session", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_ROLE = "user_role"
        private const val KEY_PHONE = "user_phone"
        private const val KEY_LOGGED_IN = "is_logged_in"
    }

    fun saveSession(role: String, phone: String) {
        prefs.edit().apply {
            putString(KEY_ROLE, role)
            putString(KEY_PHONE, phone)
            putBoolean(KEY_LOGGED_IN, true)
            apply()
        }
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

    fun getUserPhone(): String? {
        return prefs.getString(KEY_PHONE, null)
    }

    fun clearSession() {
        prefs.edit().apply {
            remove(KEY_ROLE)
            remove(KEY_PHONE)
            putBoolean(KEY_LOGGED_IN, false)
            apply()
        }
    }
}

