package com.tkbiswas.pilesclinic.data.remote

import com.google.gson.FieldNamingPolicy
import com.google.gson.GsonBuilder
import com.tkbiswas.pilesclinic.BuildConfig
import com.tkbiswas.pilesclinic.data.session.SessionManager
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

/**
 * Every Supabase call (auth + REST) needs an `apikey` header, and an
 * `Authorization: Bearer <token>` header — the signed-in user's access token
 * if available, otherwise the anon key (so anonymous/public-read calls, if
 * any RLS policy allows them, still work).
 */
class SupabaseHeaderInterceptor(private val sessionManager: SessionManager) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): okhttp3.Response {
        val bearer = sessionManager.accessToken?.takeIf { it.isNotBlank() } ?: SupabaseConfig.anonKey
        val request = chain.request().newBuilder()
            .header("apikey", SupabaseConfig.anonKey)
            .header("Authorization", "Bearer $bearer")
            .build()
        return chain.proceed(request)
    }
}

object RetrofitProvider {

    private val gson = GsonBuilder()
        .setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
        .create()

    @Volatile
    private var retrofit: Retrofit? = null

    fun get(sessionManager: SessionManager): Retrofit {
        return retrofit ?: synchronized(this) {
            retrofit ?: build(sessionManager).also { retrofit = it }
        }
    }

    private fun build(sessionManager: SessionManager): Retrofit {
        SupabaseConfig.requireConfiguredOrThrow()

        val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = if (BuildConfig.DEBUG) {
                HttpLoggingInterceptor.Level.BASIC
            } else {
                HttpLoggingInterceptor.Level.NONE
            }
        }

        val client = OkHttpClient.Builder()
            .addInterceptor(SupabaseHeaderInterceptor(sessionManager))
            .addInterceptor(loggingInterceptor)
            .connectTimeout(20, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS)
            .writeTimeout(20, TimeUnit.SECONDS)
            .callTimeout(45, TimeUnit.SECONDS)   // 🔴 V803 — নিয়ম ৬.২
            .build()

        return Retrofit.Builder()
            .baseUrl(SupabaseConfig.baseUrl)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()
    }
}
