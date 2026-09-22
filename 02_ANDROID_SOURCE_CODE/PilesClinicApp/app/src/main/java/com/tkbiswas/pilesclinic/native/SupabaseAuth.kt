package com.tkbiswas.pilesclinic.native

import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.util.concurrent.TimeUnit

/**
 * V216 (§5, 31.07.2026) — Supabase Auth (GoTrue) preparation helper.
 *
 * ⛔ সততা: এই ক্লাসটা **এখনো login-এ ব্যবহার হচ্ছে না** — এটা Auth-এ সরার
 *  প্রস্তুতি (prepared code)। এখন login চলে StaffDirectory/usercredentials দিয়ে
 *  (LoginActivity)। RLS নিরাপদে চালু করতে হলে আগে প্রতিটা staff/doctor/master-কে
 *  Supabase Auth user বানাতে হবে (email/phone + password), যাতে প্রতিটা request-এর
 *  JWT-তে role/branch claim থাকে — তখন DB নিজেই role/branch যাচাই করতে পারবে।
 *  সেই কাজের জন্য এই helper প্রস্তুত: sign-in করে access_token (JWT) আনে।
 *
 *  পুরো ধাপ V216_MANUAL_SETUP_IF_REQUIRED.md-তে। ⛔ এই ফাইল compile হয় কিন্তু
 *  কোথাও ডাকা হয় না, তাই বর্তমান কোনো আচরণ বদলায় না।
 *
 *  ব্যবহার (Auth চালু হলে):
 *    val jwt = SupabaseAuth.signInWithPassword(email, password)   // null হলে ব্যর্থ
 *    // তারপর SupabaseClient-এ Authorization: Bearer <jwt> পাঠালে RLS policy JWT
 *    // claim (role/branch) ধরে কাজ করবে।
 */
object SupabaseAuth {

    private val client = OkHttpClient.Builder()
        .connectTimeout(8, TimeUnit.SECONDS)
        .readTimeout(8, TimeUnit.SECONDS)
        .callTimeout(15, TimeUnit.SECONDS)
        .build()

    private val URL = SupabaseClient.URL
    private val KEY = SupabaseClient.KEY
    private val jsonMedia = "application/json".toMediaType()

    data class AuthResult(
        val accessToken: String,
        val refreshToken: String,
        val userId: String,
        val expiresIn: Int
    )

    /** GoTrue password grant. সফল হলে AuthResult, নয়তো null (কোনো exception throw করে না)। */
    fun signInWithPassword(email: String, password: String): AuthResult? {
        return try {
            val body = JSONObject().put("email", email).put("password", password).toString()
            val req = Request.Builder()
                .url("$URL/auth/v1/token?grant_type=password")
                .addHeader("apikey", KEY)
                .addHeader("Content-Type", "application/json")
                .post(body.toRequestBody(jsonMedia))
                .build()
            client.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) return null
                val json = JSONObject(resp.body?.string() ?: return null)
                val token = json.optString("access_token", "")
                if (token.isBlank()) return null
                AuthResult(
                    accessToken = token,
                    refreshToken = json.optString("refresh_token", ""),
                    userId = json.optJSONObject("user")?.optString("id", "") ?: "",
                    expiresIn = json.optInt("expires_in", 3600)
                )
            }
        } catch (_: Throwable) { null }
    }

    /** নতুন Auth user তৈরি (admin/migration কাজে)। সফল হলে userId, নয়তো null। */
    fun signUp(email: String, password: String, metadata: JSONObject? = null): String? {
        return try {
            val body = JSONObject().put("email", email).put("password", password)
            if (metadata != null) body.put("data", metadata)   // role/branch claim ইত্যাদি
            val req = Request.Builder()
                .url("$URL/auth/v1/signup")
                .addHeader("apikey", KEY)
                .addHeader("Content-Type", "application/json")
                .post(body.toString().toRequestBody(jsonMedia))
                .build()
            client.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) return null
                val json = JSONObject(resp.body?.string() ?: return null)
                (json.optJSONObject("user")?.optString("id", "") ?: json.optString("id", "")).ifBlank { null }
            }
        } catch (_: Throwable) { null }
    }
}
