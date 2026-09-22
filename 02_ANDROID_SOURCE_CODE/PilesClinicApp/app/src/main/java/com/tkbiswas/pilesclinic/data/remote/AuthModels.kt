package com.tkbiswas.pilesclinic.data.remote

data class SignInRequest(
    val email: String,
    val password: String
)

data class RefreshRequest(
    val refresh_token: String
)

data class SupabaseUserDto(
    val id: String,
    val email: String?
)

data class AuthResponse(
    val access_token: String?,
    val token_type: String?,
    val expires_in: Long?,
    val refresh_token: String?,
    val user: SupabaseUserDto?,
    val error: String? = null,
    val error_description: String? = null,
    val msg: String? = null
)
