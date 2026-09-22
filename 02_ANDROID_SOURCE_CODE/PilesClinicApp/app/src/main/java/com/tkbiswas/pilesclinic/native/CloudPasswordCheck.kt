package com.tkbiswas.pilesclinic.native

import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import java.util.concurrent.TimeUnit

/**
 * Checks the "usercredentials" Supabase table for a per-user password override,
 * mirroring app.js's cloudPasswordForMobile(). If this fails (no internet, table
 * empty, etc.) it returns null and the caller falls back to the bundled
 * StaffDirectory role default password -- exactly the same fallback order the
 * WebView login() already uses, so behavior stays identical between the native
 * and WebView login screens.
 */
object CloudPasswordCheck {

    private val client = OkHttpClient.Builder()
        .connectTimeout(6, TimeUnit.SECONDS)
        .readTimeout(6, TimeUnit.SECONDS)
        // TK-REQUESTED FIX (2026-07-24, "slow internet" audit): this client
        // was missing the same callTimeout SupabaseClient.kt already has
        // (TK-reported 2026-07-20) -- on a very slow but not-dead
        // connection, data can keep trickling in just fast enough to keep
        // resetting readTimeout, so the call never actually times out and
        // could hang far longer than 6s. This matters most right here
        // because it's LOGIN -- and the 2026-07-23 password security fix
        // specifically depends on this call reaching a definite
        // success/failure (PasswordState.Failed blocks the old default
        // password on purpose) rather than hanging forever. Caps the
        // TOTAL time so login always resolves one way or the other.
        .callTimeout(15, TimeUnit.SECONDS)
        .build()

    // Same Supabase project the website's app.js talks to (03_NETLIFY_READY/
    // config.js) -- B271, 02.08.2026: the unused duplicate phone-copy at
    // assets/www/config.js was removed.
    // Single source of truth for the native layer: reuse SupabaseClient's
    // URL/KEY instead of a second hardcoded copy (keeps native config in one place).
    private val SUPABASE_URL = SupabaseClient.URL
    private val SUPABASE_KEY = SupabaseClient.KEY


    /** MASTER AUDIT 22.09.2026 — additive V2 verification.
     * Returns only OK/WRONG/NO_CUSTOM; the stored credential never leaves the DB.
     * ERROR means caller should use the old proven path so no working login is broken. */
    enum class VerifyV2 { OK, WRONG, NO_CUSTOM, ERROR }

    fun verifyPasswordV2(mobileDigitsOnly: String, typed: String): VerifyV2 {
        if (mobileDigitsOnly.length != 10 || typed.isBlank()) return VerifyV2.ERROR
        return try {
            val url = "$SUPABASE_URL/rest/v1/rpc/tk_credential_verify_v2"
            val bodyJson = org.json.JSONObject()
                .put("p_mobile", mobileDigitsOnly)
                .put("p_password", typed)
                .toString()
            val request = Request.Builder()
                .url(url)
                .addHeader("apikey", SUPABASE_KEY)
                .addHeader("Authorization", "Bearer $SUPABASE_KEY")
                .addHeader("Content-Type", "application/json")
                .post(bodyJson.toRequestBody("application/json".toMediaType()))
                .build()
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return VerifyV2.ERROR
                val raw = response.body?.string() ?: return VerifyV2.ERROR
                val obj = org.json.JSONObject(raw)
                when (obj.optString("status", "").uppercase()) {
                    "OK" -> VerifyV2.OK
                    "WRONG" -> VerifyV2.WRONG
                    "NO_CUSTOM" -> VerifyV2.NO_CUSTOM
                    else -> VerifyV2.ERROR
                }
            }
        } catch (_: Throwable) { VerifyV2.ERROR }
    }

    /** Returns the saved password for this mobile if one exists in the cloud
     * table, or null if not found / on any error (network, parsing, etc.) --
     * callers must treat null as "no override, use the role default." */
    fun fetchOverridePassword(mobileDigitsOnly: String): String? {
        return try {
            val url = "$SUPABASE_URL/rest/v1/usercredentials?mobile=eq.$mobileDigitsOnly&select=password&limit=1"
            val request = Request.Builder()
                .url(url)
                .addHeader("apikey", SUPABASE_KEY)
                .addHeader("Authorization", "Bearer $SUPABASE_KEY")
                .get()
                .build()
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return null
                val body = response.body?.string() ?: return null
                val arr = JSONArray(body)
                if (arr.length() == 0) return null
                val pw = arr.getJSONObject(0).s("password")
                pw.ifBlank { null }
            }
        } catch (e: Exception) {
            null
        }
    }

    /** 🔵🔒 B618 (11.08.2026, TK-নির্দেশ): লগইনের আগেই (anon) দেখি এই মোবাইল
     *  সাসপেন্ড কিনা — public RPC `suspended_until_for` (V311, security-definer,
     *  শুধু ওই মোবাইলের suspended_until ফেরায়)। ফেরত: "yyyy-MM-dd" বা null।
     *  ⛔ যেকোনো ব্যর্থতায় (নেট/parsing) null — fail-open, নেট-সমস্যায় কখনো
     *  লগইন আটকাবে না (শুধু স্পষ্ট তারিখ থাকলেই কলার আটকাবে)। */
    fun fetchSuspendedUntil(mobileDigitsOnly: String): String? {
        return try {
            val url = "$SUPABASE_URL/rest/v1/rpc/suspended_until_for"
            val bodyJson = "{\"p_mobile\":\"$mobileDigitsOnly\"}"
            val request = Request.Builder()
                .url(url)
                .addHeader("apikey", SUPABASE_KEY)
                .addHeader("Authorization", "Bearer $SUPABASE_KEY")
                .addHeader("Content-Type", "application/json")
                .post(bodyJson.toRequestBody("application/json".toMediaType()))
                .build()
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return null
                val b = (response.body?.string() ?: "").trim()
                val cleaned = b.trim('"', ' ', '\n', '\r')
                if (cleaned.isBlank() || cleaned.equals("null", true)) null else cleaned
            }
        } catch (e: Exception) {
            null
        }
    }

    /** 🔴🔒 V1649 (২১.০৯.২০২৬, TK-নির্দেশ) — সাসপেন্ডের কারণ (থাকলে) আলাদা
     *  করে পড়ে — `suspended_until_for` একটুও বদলায়নি (পুরনো অ্যাপ/ওয়েব
     *  আগের মতোই চলবে)। ফাঁকা/না-লেখা হলে null — কল-সাইট নিজেই
     *  "কিছু লেখা হয়নি" দেখাবে। */
    fun fetchSuspendReason(mobileDigitsOnly: String): String? {
        return try {
            val url = "$SUPABASE_URL/rest/v1/rpc/suspend_reason_for"
            val bodyJson = "{\"p_mobile\":\"$mobileDigitsOnly\"}"
            val request = Request.Builder()
                .url(url)
                .addHeader("apikey", SUPABASE_KEY)
                .addHeader("Authorization", "Bearer $SUPABASE_KEY")
                .addHeader("Content-Type", "application/json")
                .post(bodyJson.toRequestBody("application/json".toMediaType()))
                .build()
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return null
                val b = (response.body?.string() ?: "").trim()
                val cleaned = b.trim('"', ' ', '\n', '\r')
                if (cleaned.isBlank() || cleaned.equals("null", true)) null else cleaned
            }
        } catch (e: Exception) {
            null
        }
    }

    // TK-REQUESTED SECURITY FIX (2026-07-23): the old fetchOverridePassword()
    // above returns null for THREE different situations that must NOT be
    // treated the same at login: (a) the user has no custom password, (b) the
    // server answered and confirmed there's no custom password, (c) the
    // request FAILED (no internet / timeout / bad response). Treating (c) the
    // same as (a)/(b) meant a network glitch let the OLD default password log
    // in even for a user who had set a custom one -- a real security hole.
    // This new result type lets the caller tell those apart. The old function
    // is left exactly as-is so nothing else that calls it changes.
    sealed class PasswordState {
        // Server answered: this user HAS a custom password.
        // V216 (§4): passwordHash-ও বহন করা হয় (থাকলে)। hash থাকলে সেটা দিয়েই
        // যাচাই; না থাকলে plaintext `password` দিয়ে (backward-compatible)।
        data class HasCustom(val password: String, val passwordHash: String = "") : PasswordState()
        // Server answered clearly: this user has NO custom password.
        object NoCustom : PasswordState()
        // Could not reach/parse the server -- unknown, must NOT fall back.
        object Failed : PasswordState()
    }

    fun fetchOverridePasswordState(mobileDigitsOnly: String): PasswordState {
        return try {
            // V216 (§4): password_hash-ও আনা হয় (থাকলে hash দিয়ে যাচাই হবে)।
            val url = "$SUPABASE_URL/rest/v1/usercredentials?mobile=eq.$mobileDigitsOnly&select=password,password_hash&limit=1"
            val request = Request.Builder()
                .url(url)
                .addHeader("apikey", SUPABASE_KEY)
                .addHeader("Authorization", "Bearer $SUPABASE_KEY")
                .get()
                .build()
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return PasswordState.Failed
                val body = response.body?.string() ?: return PasswordState.Failed
                val arr = JSONArray(body)
                if (arr.length() == 0) return PasswordState.NoCustom
                val obj = arr.getJSONObject(0)
                val pw = obj.s("password")
                val hash = obj.s("password_hash")
                // hash বা plaintext — যেকোনো একটা থাকলেই এই user-এর custom password আছে।
                if (pw.isBlank() && hash.isBlank()) PasswordState.NoCustom
                else PasswordState.HasCustom(pw, hash)
            }
        } catch (e: Exception) {
            PasswordState.Failed
        }
    }

    /** V216 (§4): lazy migration — সঠিক plaintext দিয়ে login সফল হলে, hash এখনো না
     *  থাকলে, এই user-এর row-তে password_hash বসিয়ে দেওয়া হয় (background-এ)।
     *  ⛔ plaintext `password` কলাম এখনই মোছা হয় না — সব ফোন hash-ready না হওয়া
     *  পর্যন্ত নিরাপদ। ব্যর্থ হলে চুপচাপ ফিরে আসে, login-এ কোনো প্রভাব নেই। */
    fun storePasswordHash(mobileDigitsOnly: String, passwordHash: String): Boolean {
        return try {
            val url = "$SUPABASE_URL/rest/v1/usercredentials?mobile=eq.$mobileDigitsOnly"
            val bodyJson = org.json.JSONObject().put("password_hash", passwordHash).toString()
            val request = Request.Builder()
                .url(url)
                .addHeader("apikey", SUPABASE_KEY)
                .addHeader("Authorization", "Bearer $SUPABASE_KEY")
                .addHeader("Prefer", "return=minimal")
                .patch(bodyJson.toRequestBody("application/json".toMediaType()))
                .build()
            client.newCall(request).execute().use { response -> response.isSuccessful }
        } catch (_: Exception) { false }
    }
}
