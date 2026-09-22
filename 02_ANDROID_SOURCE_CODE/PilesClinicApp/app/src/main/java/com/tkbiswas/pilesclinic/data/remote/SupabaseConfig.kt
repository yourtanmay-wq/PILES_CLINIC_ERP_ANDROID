package com.tkbiswas.pilesclinic.data.remote

import com.tkbiswas.pilesclinic.BuildConfig

/**
 * Central place that reads the Supabase URL/Anon Key injected at build time
 * from local.properties or environment variables (see app/build.gradle.kts).
 * Nothing is ever hardcoded here.
 */
object SupabaseConfig {

    val url: String = BuildConfig.SUPABASE_URL.trim().trimEnd('/')
    val anonKey: String = BuildConfig.SUPABASE_ANON_KEY.trim()

    /** True once a real project URL + anon key have been configured locally. */
    val isConfigured: Boolean
        get() = url.isNotBlank() && anonKey.isNotBlank() && url.startsWith("http")

    /** Base URL Retrofit needs (must end with a single trailing slash). */
    val baseUrl: String
        get() = "$url/"

    fun requireConfiguredOrThrow() {
        if (!isConfigured) {
            throw IllegalStateException(
                "Supabase is not configured yet. Add SUPABASE_URL and SUPABASE_ANON_KEY " +
                    "to local.properties (see local.properties.example / SUPABASE_SETUP.md)."
            )
        }
    }
}
