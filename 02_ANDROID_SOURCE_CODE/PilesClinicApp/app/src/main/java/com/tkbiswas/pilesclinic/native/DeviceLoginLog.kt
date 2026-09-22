package com.tkbiswas.pilesclinic.native

import android.content.Context
import org.json.JSONObject

/**
 * 📱🔒🔒 V889 (৩০.০৮.২০২৬, TK-নির্দেশ) — *"কোন ফোনে কে লগইন জমা রাখুন"*
 *
 * **কেন দরকার হলো:** SHAMOL ROY-এর রেজিস্ট্রেশনে মাস্টারের নাম দেখে TK
 * প্রশ্ন করেন — *"আমি তো কোনো রেজিস্ট্রেশন করিনি"*। যাচাইয়ে দেখা গেল ওই
 * মুহূর্তে ওই ফোনে মাস্টার আইডিতে লগইন ছিল। কিন্তু **কোন ফোনে কে লগইন**
 * সেটা অ্যাপ কোথাও জমা রাখত না, তাই ধরার উপায়ও ছিল না।
 *
 * **এখন:** প্রতিটা ফোনের নিজস্ব একটা স্থায়ী নম্বর (`deviceId`) থাকে, আর
 * লগইন করলেই ক্লাউডের `device_logins` টেবিলে একটাই সারি বসে/হালনাগাদ হয় —
 * কোন ফোন · কে · কোন ব্রাঞ্চ · কখন।
 *
 * ⛔ **Egress প্রায় শূন্য** — লেখা হয় শুধু **লগইনের সময়** (দিনে বড়জোর
 *    কয়েকবার), আর সারি **একটাই** (deviceId-ই আইডি), তাই জমে না।
 * ⛔ কোনো পড়া নেই — অ্যাপ এই টেবিল কখনো পড়ে না, TK শুধু Supabase-এ দেখেন।
 * ⛔ ব্যর্থ হলে চুপচাপ ছেড়ে দেয় — লগইন কখনো আটকাবে না।
 */
object DeviceLoginLog {

    private const val PREFS = "piles_clinic_device_id"
    private const val KEY = "id"

    /** এই ফোনের স্থায়ী নম্বর — একবার তৈরি হয়, অ্যাপ মোছা না হলে বদলায় না। */
    fun deviceId(context: Context): String {
        val p = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val old = p.getString(KEY, "").orEmpty()
        if (old.isNotBlank()) return old
        val fresh = "dev_" + java.util.UUID.randomUUID().toString().replace("-", "")
        p.edit().putString(KEY, fresh).apply()
        return fresh
    }

    /** লগইনের সময় ডাকা হয় — পিছনে চলে, কখনো অপেক্ষা করায় না। */
    fun record(context: Context, mobile: String, name: String, branch: String, role: String) {
        val app = context.applicationContext
        Thread {
            try {
                val row = JSONObject()
                    .put("id", deviceId(app))
                    .put("staffMobile", mobile.filter { it.isDigit() }.takeLast(10))
                    .put("staffName", name)
                    .put("branch", branch)
                    .put("role", role)
                    .put("phoneModel", (android.os.Build.MANUFACTURER + " " + android.os.Build.MODEL).trim())
                    .put("appVersion", com.tkbiswas.pilesclinic.BuildConfig.VERSION_NAME)
                    .put("loggedInAt", isoNow())
                SupabaseClient.upsert("device_logins", row)
            } catch (_: Throwable) { }
        }.start()
    }

    private fun isoNow(): String =
        java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", java.util.Locale.US)
            .apply { timeZone = java.util.TimeZone.getTimeZone("UTC") }
            .format(java.util.Date())
}
