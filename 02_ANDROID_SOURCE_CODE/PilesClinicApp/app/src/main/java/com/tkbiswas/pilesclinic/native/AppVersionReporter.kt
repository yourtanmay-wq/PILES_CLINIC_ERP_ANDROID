package com.tkbiswas.pilesclinic.native

import android.content.Context
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit

/**
 * 📱🔒 V771 (২৮.০৮.২০২৬, TK-নির্দেশ) — **এই ফোনে কোন ভার্সন চলছে, মেঘকে জানানো।**
 *
 * TK-এর কথা: *"আমি কি করে জানবো — App থেকে দেখার ব্যবস্থা রাখুন।"*
 *
 * ═════════════════════════════════════════════════════════════════════
 * **কেন দরকার (প্রমাণসহ, আন্দাজে নয়)**
 *   Supabase-এর লগে দেখা গেছে একটা পড়া বারবার **400 ভুল** দিচ্ছে —
 *   `deleted_records?select=*&order=updatedAt…`। এই ডাকটা **আজকের কোডে নেই**
 *   (V451-এ ঠিক করা হয়েছিল)। ⇒ কোনো ফোনে এখনো **পুরনো ভার্সন** চলছে, আর
 *   সেটাই অকারণে Egress খরচ করছে। কোন ফোন, সেটা না জানলে ঠিক করা যায় না।
 * ═════════════════════════════════════════════════════════════════════
 *
 * ⚡ **খরচ কত (হিসাব করে)** — দিনে **একবার**, একটাই ছোট্ট RPC (~200 বাইট)।
 *    ১০টা ফোনে মাসে ~৬০ KB। Egress-এ প্রভাব কার্যত শূন্য।
 * ⛔ ফোন **শুধু নিজের ভার্সন ও সময়** লিখতে পারে — সার্ভারই তা নিশ্চিত করে
 *    (`public.report_app_version`)। নাম · মোবাইল · বেতন কিছুই ছোঁয়া যায় না,
 *    আর নতুন কোনো সারি তৈরি হয় না।
 * ⛔ ব্যর্থ হলে চুপচাপ ছেড়ে দেয় — অ্যাপের কিছুই আটকায় না।
 * ⛔ 🧵 **আলাদা থ্রেড থেকে ডাকুন** (নেটে যায়)।
 */
object AppVersionReporter {

    private const val PREFS = "app_version_report"
    private const val KEY_AT = "at"
    private const val KEY_V = "v"
    private const val ONE_DAY = 24L * 60L * 60L * 1000L

    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .callTimeout(20, TimeUnit.SECONDS)   // 🔴 V803 — নিয়ম ৬.২: এখানেও সময়সীমা ছিল না
        .build()

    /**
     * 🧵 আলাদা থ্রেড। দিনে একবারই সত্যিই পাঠায়।
     * ⛔ ভার্সন বদলালে (নতুন বিল্ড) **সঙ্গে সঙ্গে** পাঠায় — তখন অপেক্ষা নয়,
     *    কারণ TK-এর ঠিক ওই খবরটাই দরকার।
     */
    fun reportIfDue(ctx: Context, mobile: String) {
        try {
            val mob = StaffDirectory.normalizeMobile(mobile)
            if (mob.length != 10) return
            val v = com.tkbiswas.pilesclinic.BuildConfig.VERSION_CODE
            if (v <= 0) return
            val p = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            val lastV = p.getInt(KEY_V, 0)
            val lastAt = p.getLong(KEY_AT, 0L)
            val now = System.currentTimeMillis()
            if (lastV == v && now - lastAt < ONE_DAY) return
            val body = "{\"p_mobile\":\"" + mob + "\",\"p_version\":" + v + "}"
            val req = Request.Builder()
                .url(SupabaseClient.URL + "/rest/v1/rpc/report_app_version")
                .addHeader("apikey", SupabaseClient.KEY)
                .addHeader("Authorization", "Bearer " + SupabaseClient.KEY)
                .addHeader("Content-Type", "application/json")
                .post(body.toRequestBody("application/json".toMediaType()))
                .build()
            client.newCall(req).execute().use { resp ->
                // ⛔ সফল হলে তবেই সময় লেখা হয় — নইলে পরের বার আবার চেষ্টা হবে।
                if (resp.isSuccessful) {
                    p.edit().putInt(KEY_V, v).putLong(KEY_AT, now).apply()
                }
            }
        } catch (_: Throwable) { /* চুপচাপ — অ্যাপের কিছুই আটকাবে না */ }
    }
}
