package com.tkbiswas.pilesclinic.data.session

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

/**
 * Persists the Supabase Auth session (tokens only — never a raw password) in
 * an EncryptedSharedPreferences file, so a signed-in user stays signed in
 * across app restarts, consistent with offline-first behaviour.
 */
class SessionManager(context: Context) {

    private val prefs: SharedPreferences by lazy {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()

        EncryptedSharedPreferences.create(
            context,
            "piles_clinic_session",
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    var accessToken: String?
        get() = prefs.getString(KEY_ACCESS_TOKEN, null)
        set(value) = prefs.edit().putString(KEY_ACCESS_TOKEN, value).apply()

    var refreshToken: String?
        get() = prefs.getString(KEY_REFRESH_TOKEN, null)
        set(value) = prefs.edit().putString(KEY_REFRESH_TOKEN, value).apply()

    var userEmail: String?
        get() = prefs.getString(KEY_USER_EMAIL, null)
        set(value) = prefs.edit().putString(KEY_USER_EMAIL, value).apply()

    var expiresAtMillis: Long
        get() = prefs.getLong(KEY_EXPIRES_AT, 0L)
        set(value) = prefs.edit().putLong(KEY_EXPIRES_AT, value).apply()

    val isSignedIn: Boolean
        get() = !accessToken.isNullOrBlank()

    val isTokenLikelyExpired: Boolean
        get() = expiresAtMillis in 1 until System.currentTimeMillis()

    fun saveSession(accessToken: String, refreshToken: String, email: String?, expiresInSeconds: Long) {
        this.accessToken = accessToken
        this.refreshToken = refreshToken
        this.userEmail = email
        this.expiresAtMillis = System.currentTimeMillis() + (expiresInSeconds * 1000L)
    }

    fun clear() {
        prefs.edit().clear().apply()
    }

    companion object {
        private const val KEY_ACCESS_TOKEN = "access_token"
        private const val KEY_REFRESH_TOKEN = "refresh_token"
        private const val KEY_USER_EMAIL = "user_email"
        private const val KEY_EXPIRES_AT = "expires_at"
    }
}
