package com.tkbiswas.pilesclinic.data.remote

import retrofit2.http.Body
import retrofit2.http.Headers
import retrofit2.http.POST
import retrofit2.http.Query

interface SupabaseAuthApi {

    @Headers("Content-Type: application/json")
    @POST("auth/v1/token")
    suspend fun signInWithPassword(
        @Query("grant_type") grantType: String = "password",
        @Body body: SignInRequest
    ): AuthResponse

    @Headers("Content-Type: application/json")
    @POST("auth/v1/token")
    suspend fun refreshSession(
        @Query("grant_type") grantType: String = "refresh_token",
        @Body body: RefreshRequest
    ): AuthResponse

    @POST("auth/v1/logout")
    suspend fun signOut()
}
