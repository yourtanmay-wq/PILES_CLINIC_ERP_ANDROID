package com.tkbiswas.pilesclinic.native

import android.content.Context
import com.tkbiswas.pilesclinic.BuildConfig
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.util.concurrent.TimeUnit

/**
 * 🔴🆕🔒 V436 (TK-রিপোর্ট ১৮.০৮.২০২৬, ছবিসহ — *"আপনি যে লাস্ট ফাইল পাঠালেন সেটা
 * Dr. K.H mandal আমার সামনেই আপডেট করলো, কিন্তু তাকে তার পুরনো ভার্সনই শো করছে
 * V259"*)।
 *
 * **কী হয়েছিল:** ড্যাশবোর্ডের `☁️ Synced · V###` লেখাটা সরাসরি
 * `BuildConfig.VERSION_CODE` থেকে আসে — অর্থাৎ **যে APK সত্যিই চলছে তার নিজের
 * ভার্সন**। তাই "V259" দেখানোর একটাই মানে: নতুন APK ওই ফোনে বসেইনি (পুরনো
 * অ্যাপটা অন্য চাবিতে বানানো থাকলে অ্যান্ড্রয়েড চুপচাপ বসাতে দেয় না)। কেউ
 * খেয়াল না করলে মাসের পর মাস পুরনো অ্যাপ চলতেই থাকত।
 *
 * **এই ফাইলটা যা করে:** ওয়েবসাইটে রাখা একটা **ছোট্ট** ফাইল
 * (`/version.json`, কয়েকশো বাইট) পড়ে দেখে সর্বশেষ ভার্সন কত। নিজের ভার্সন তার
 * চেয়ে কম হলে ড্যাশবোর্ডে লাল সতর্কবার্তা দেখায়।
 *
 * ⛔ **কিছুই ভাঙতে পারে না — কারণ:**
 *  ১) পুরো কাজটা পিছনের থ্রেডে, `try/catch`-এ মোড়া। নেট না থাকলে · ফাইল না
 *     থাকলে · লেখা ভুল থাকলে — নিঃশব্দে **কিছুই হয় না**, পটিটা লুকানোই থাকে।
 *  ২) কোনো ডেটা পড়ে না, লেখে না — Supabase-এ একটাও অনুরোধ যায় না, তাই
 *     কোটার উপর প্রভাব শূন্য।
 *  ৩) দিনে **একবারের বেশি** দেখা হয় না (SharedPreferences-এ সময় জমা থাকে)।
 *  ৪) নতুন ভার্সন **বেশি** হলে তবেই দেখায়; সমান/কম হলে কখনো নয়। তাই ভুল করে
 *     সতর্কবার্তা ওঠার পথ নেই।
 *  ৫) সংখ্যাটা পড়তে না পারলে (0 বা কম) কিছুই দেখানো হয় না।
 */
object AppVersionCheck {

    /** ওয়েবসাইটে রাখা ছোট ফাইলটা — `03_NETLIFY_READY/version.json`. */
    private const val URL = "https://maaayurvedpilesclinic.netlify.app/version.json"

    private const val PREFS = "app_version_check"
    private const val KEY_AT = "last_checked_at"
    private const val KEY_LATEST = "latest_seen"
    private const val ONE_DAY_MS = 24L * 60L * 60L * 1000L

    private val http: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(6, TimeUnit.SECONDS)
            .readTimeout(6, TimeUnit.SECONDS)
            .callTimeout(10, TimeUnit.SECONDS)
            .build()
    }

    /**
     * সর্বশেষ **জানা** ভার্সন ফেরত দেয় (আগে যা দেখা হয়েছিল), আর দরকার হলে
     * পিছনে গিয়ে আবার দেখে নেয়।
     * @return নিজের চেয়ে নতুন ভার্সন থাকলে সেই সংখ্যা, নইলে 0।
     */
    @JvmStatic
    fun newerVersionOrZero(ctx: Context): Int = try {
        val p = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val latest = p.getInt(KEY_LATEST, 0)
        if (latest > BuildConfig.VERSION_CODE) latest else 0
    } catch (_: Throwable) { 0 }

    /**
     * পিছনে গিয়ে ওয়েবসাইট থেকে সর্বশেষ ভার্সন জেনে নেয় (দিনে একবার)।
     * শেষ হলে [then] ডাকা হয় — নিজের চেয়ে নতুন হলে সেই সংখ্যা, নইলে 0।
     * ⛔ যেকোনো গোলমালে 0 — কিছুই দেখানো হয় না।
     */
    @JvmStatic
    fun refresh(ctx: Context, force: Boolean, then: (Int) -> Unit) {
        try {
            val p = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            val last = p.getLong(KEY_AT, 0L)
            val now = System.currentTimeMillis()
            if (!force && last > 0L && (now - last) < ONE_DAY_MS) {
                then(newerVersionOrZero(ctx)); return
            }
            Thread {
                var found = 0
                try {
                    val req = Request.Builder().url(URL).get().build()
                    http.newCall(req).execute().use { resp ->
                        if (resp.isSuccessful) {
                            val body = resp.body?.string().orEmpty()
                            val v = JSONObject(body).optInt("versionCode", 0)
                            if (v > 0) {
                                found = v
                                p.edit().putInt(KEY_LATEST, v).putLong(KEY_AT, now).apply()
                            }
                        }
                    }
                } catch (_: Throwable) {
                    // নেট নেই / ফাইল নেই / লেখা ভুল — কিছুই হয় না
                }
                val newer = if (found > BuildConfig.VERSION_CODE) found else 0
                try { then(newer) } catch (_: Throwable) { }
            }.start()
        } catch (_: Throwable) {
            try { then(0) } catch (_: Throwable) { }
        }
    }
}
