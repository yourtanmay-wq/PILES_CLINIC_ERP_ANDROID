package com.tkbiswas.pilesclinic.data.repository

import com.tkbiswas.pilesclinic.data.remote.RefreshRequest
import com.tkbiswas.pilesclinic.data.remote.RetrofitProvider
import com.tkbiswas.pilesclinic.data.remote.SignInRequest
import com.tkbiswas.pilesclinic.data.remote.SupabaseAuthApi
import com.tkbiswas.pilesclinic.data.remote.SupabaseConfig
import com.tkbiswas.pilesclinic.data.session.SessionManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

sealed class AuthResult {
    data class Success(val email: String?) : AuthResult()
    data class Failure(val message: String) : AuthResult()
}

class AuthRepository(private val sessionManager: SessionManager) {

    private val api: SupabaseAuthApi
        get() = RetrofitProvider.get(sessionManager).create(SupabaseAuthApi::class.java)

    val isSignedIn: Boolean get() = sessionManager.isSignedIn
    val currentUserEmail: String? get() = sessionManager.userEmail

    suspend fun signIn(email: String, password: String): AuthResult = withContext(Dispatchers.IO) {
        if (!SupabaseConfig.isConfigured) {
            return@withContext AuthResult.Failure(
                "Supabase not configured. Add SUPABASE_URL / SUPABASE_ANON_KEY to local.properties first."
            )
        }
        try {
            val response = api.signInWithPassword(body = SignInRequest(email, password))
            val token = response.access_token
            val refresh = response.refresh_token
            if (token.isNullOrBlank() || refresh.isNullOrBlank()) {
                val reason = response.error_description ?: response.msg ?: response.error
                ?: "Sign-in failed. Check email/password."
                return@withContext AuthResult.Failure(reason)
            }
            sessionManager.saveSession(
                accessToken = token,
                refreshToken = refresh,
                email = response.user?.email ?: email,
                expiresInSeconds = response.expires_in ?: 3600L
            )
            AuthResult.Success(sessionManager.userEmail)
        } catch (e: Exception) {
            AuthResult.Failure(e.message ?: "Network error while signing in.")
        }
    }

    /** Refreshes the access token using the stored refresh token, if the current one looks expired. */
    suspend fun refreshIfNeeded(): Boolean = withContext(Dispatchers.IO) {
        if (!sessionManager.isTokenLikelyExpired) return@withContext true
        val refresh = sessionManager.refreshToken ?: return@withContext false
        try {
            val response = api.refreshSession(body = RefreshRequest(refresh))
            val token = response.access_token
            val newRefresh = response.refresh_token
            if (token.isNullOrBlank() || newRefresh.isNullOrBlank()) return@withContext false
            sessionManager.saveSession(
                accessToken = token,
                refreshToken = newRefresh,
                email = sessionManager.userEmail,
                expiresInSeconds = response.expires_in ?: 3600L
            )
            true
        } catch (e: Exception) {
            false
        }
    }

    suspend fun signOut() = withContext(Dispatchers.IO) {
        try {
            api.signOut()
        } catch (e: Exception) {
            // Even if the network call fails, clear the local session below —
            // the user asked to sign out and offline sign-out must still work.
        }
        sessionManager.clear()
    }
}
