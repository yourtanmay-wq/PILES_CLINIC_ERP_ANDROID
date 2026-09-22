package com.tkbiswas.pilesclinic.native

import android.content.Context
import org.json.JSONObject

/**
 * 🔔 V1298 — এই ফোনের push-ঠিকানা (Firebase token) ক্লাউডের `device_tokens` টেবিলে রাখা,
 * লগইন করা স্টাফের নাম/ব্রাঞ্চ/role সহ — সার্ভার এটা দেখেই ঠিক করে কোন নোটিশ কার ফোনে যাবে
 * (BriefingModel.targetsHit-এর নিয়মে)। লগআউট/অন্য কেউ লগইন করলে একই token নতুন লোকের নামে বসে।
 * ⛔ Firebase না থাকলে (google-services.json ছাড়া বিল্ড) চুপচাপ কিছু করে না।
 */
object PushTokenSync {
    private const val PREF = "push_token_sync"

    /** Dashboard খোলার সময় ডাকা হয় — token চেয়ে নিয়ে (দরকার হলে) ক্লাউডে লেখে। */
    fun syncOnLogin(ctx: Context) {
        try {
            val user = NativeSession.current(ctx) ?: return
            com.google.firebase.messaging.FirebaseMessaging.getInstance().token
                .addOnSuccessListener { token -> try { register(ctx, token) } catch (_: Throwable) { } }
        } catch (_: Throwable) { }
    }

    fun register(ctx: Context, token: String) {
        if (token.isBlank()) return
        val user = NativeSession.current(ctx) ?: return
        val sp = ctx.getSharedPreferences(PREF, Context.MODE_PRIVATE)
        val sig = token + "|" + user.mobile + "|" + user.role + "|" + user.branch
        val last = sp.getString("last", "") ?: ""
        val lastAt = sp.getLong("at", 0L)
        // একই token+লোক আজই লেখা হয়ে থাকলে আবার নয় (দিনে একবার যথেষ্ট)
        if (sig == last && System.currentTimeMillis() - lastAt < 24L * 60L * 60L * 1000L) return
        Thread {
            try {
                val row = JSONObject()
                    .put("token", token)
                    .put("mobile", user.mobile)
                    .put("name", user.name)
                    .put("role", user.role)
                    .put("branch", user.branch)
                    .put("platform", "android")
                    .put("updatedAt", java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", java.util.Locale.US)
                        .apply { timeZone = java.util.TimeZone.getTimeZone("UTC") }.format(java.util.Date()))
                if (SupabaseClient.upsert("device_tokens", row)) {
                    sp.edit().putString("last", sig).putLong("at", System.currentTimeMillis()).apply()
                }
            } catch (_: Throwable) { }
        }.start()
    }
}
