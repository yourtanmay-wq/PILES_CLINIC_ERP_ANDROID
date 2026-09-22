package com.tkbiswas.pilesclinic.native

import android.content.Context
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 🔁🔒 V839 (২৯.০৮.২০২৬) — **রোগী আবার CHECK-UP তালিকায় ফিরবেন**
 *
 * TK-নির্দেশ (হুবহু): *"Arrived নয় পেমেন্ট করলেও যেন কাজ হয়। অর্থাৎ চেম্বার
 * তারিখ ডেট — সেই পর্দায় যদি পেশেন্টের নাম আসে, তাহলেও যেন চেকআপে চলে আসে।"*
 *
 * ### 🔬 আসল ফাঁক (কোড ধরে প্রমাণিত)
 * রোগী CHECK-UP তালিকায় ঢোকেন **শুধু রেজিস্ট্রেশনের সময়**
 * (`PatientModel.kt` — `queue=true` · `stage="Doctor Queue"` · `doctorComplete=false`)।
 * ডাক্তার চেকআপ Save করলে `doctorComplete=true` বসে ⇒ নাম তালিকা থেকে চলে যায়।
 * ⇒ পরের সপ্তাহে সেই রোগী এসে টাকা দিলেও বা চেম্বারের তালিকায় নাম উঠলেও
 *   **ফেরেন না — ডাক্তার তাঁকে দেখতেই পান না।**
 *
 * ### ✅ সমাধান — মূল নিয়মে হাত না দিয়ে
 * ⛔ `DoctorQueueModel.isInQueue()`-এর নিয়ম **এক অক্ষরও বদলানো হয়নি**।
 * বদলে ওয়েবের প্রমাণিত `forceVisitQueueEntry()` (`app.js`) যা করে, ঠিক সেটাই —
 * ওই তিনটে ঘর ফিরিয়ে বসানো হয়।
 *
 * ### 🛡️ পাহারা — TK-নির্দেশ
 * **একই রোগী দিনে একবারই** ফিরবেন। নইলে ডাক্তার চেকআপ শেষ করার পরে রোগী
 * দ্বিতীয়বার টাকা দিলে (যেমন ওষুধের টাকা) **আবার তালিকায় ফিরে আসতেন**।
 *
 * ### ⛔ যা কখনো হবে না
 * · রোগীর কোনো তথ্য · টাকা · চেকআপের লেখা — কিচ্ছু ছোঁয়া হয় না।
 * · সারি খুঁজে না পেলে বা নেট না থাকলে **নীরবে কিছুই হয় না** — কল বা সেভ
 *   কখনো আটকায় না (পুরোটা try/catch-এ, ব্যাকগ্রাউন্ড থ্রেডে)।
 * · যিনি ইতিমধ্যেই তালিকায় আছেন (`doctorComplete=false`), তাঁকে ছোঁয়া হয় না।
 */
object NextVisitQueue {

    private const val PREFS = "nvp_queue_reopen"

    private fun today(): String =
        SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())

    private fun isoNow(): String =
        SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US).format(Date())

    /** আজ এই নম্বরের জন্য আগে একবার করা হয়েছে কি না (দিনে একবারের পাহারা)। */
    private fun alreadyToday(context: Context?, tenDigits: String): Boolean {
        val ctx = context ?: return false
        return try {
            val sp = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            val key = "done_" + today()
            val seen = sp.getStringSet(key, emptySet()) ?: emptySet()
            if (seen.contains(tenDigits)) return true
            val updated = HashSet(seen); updated.add(tenDigits)
            // পুরনো দিনের চাবি জমতে দেওয়া হয় না — আজকেরটা রেখে বাকি মোছা।
            sp.edit().clear().putStringSet(key, updated).apply()
            false
        } catch (_: Throwable) { false }
    }

    /**
     * চেম্বারের তালিকায় নাম উঠলে **অথবা** পেমেন্ট হলে ডাকা হয়।
     * ⛔ সবসময় **ব্যাকগ্রাউন্ড থ্রেডে** — ডাকার জায়গা কখনো অপেক্ষা করে না।
     */
    fun reopenForToday(context: Context?, mobile: String) {
        val digits = mobile.filter { it.isDigit() }.takeLast(10)
        if (digits.length != 10) return
        if (alreadyToday(context, digits)) return
        val appCtx = context?.applicationContext
        BackgroundWork.run {
            try {
                /* সরু পড়া — শুধু যে দুটো ঘর সিদ্ধান্তে লাগে।
                   ⛔ `findByMobileOrNull`: নেট ব্যর্থ হলে `null` ফেরে, তখন
                      **কিছুই লেখা হয় না** (V434-এর প্রমাণিত নিয়ম)। */
                val rows = SupabaseClient.findByMobileOrNull(
                    "patients", digits, "id,doctorComplete,queue,stage,queuedAt", 20
                ) ?: return@run
                for (i in 0 until rows.length()) {
                    val row = rows.optJSONObject(i) ?: continue
                    val id = row.optString("id")
                    if (id.isBlank()) continue
                    // ইতিমধ্যেই তালিকায় আছেন — ছোঁয়ার দরকার নেই।
                    if (!row.optBoolean("doctorComplete", false)) {
                        /* 🔴🔒 V1300 (১০.০৯.২০২৬, তালিকা ৪১৬ — MD RIYAZ, TK: *"হ্যাঁ, ঠিক
                           করুন সাবধানে"*): আগে এখানে "doctorComplete নয় = ইতিমধ্যেই
                           তালিকায় আছেন" ধরে ছেড়ে দেওয়া হত। কিন্তু V1013-এর নিয়মে
                           তালিকায় থাকেন **শুধু আজ লাইনে ওঠা** রোগী (`queuedAt`, নইলে
                           visitDate/registrationDate) — যাঁর চেকআপ কখনো শেষ-চিহ্ন
                           পায়নি অথচ দিন পেরিয়ে গেছে (queuedAt ফাঁকা/পুরনো), তিনি
                           আজ Arrived/পেমেন্ট করলেও অদৃশ্য থাকতেন। এখন তাঁর শুধু
                           লাইনে-ওঠার দিনটা আজকের করা হয় — stage · queue ·
                           doctorComplete · অন্য কিছু ছোঁয়া হয় না।
                           ⛔ যিনি আজই লাইনে আছেন (queuedAt আজ) — আগের মতোই অছোঁয়া।
                           ⛔ queue=false ও stage-ও লাইনের নয় (যেমন Treatment Running,
                              doctorComplete কখনো বসেনি) — আগের মতোই কিছু হয় না;
                              সেটা এই ফাঁকের অংশ নয়, আলাদা সিদ্ধান্ত। */
                        val stage = row.optString("stage", "")
                        val inQueueRow = row.optBoolean("queue", false) || stage == "Doctor Queue" || stage == "Visit"
                        if (!inQueueRow) continue
                        val qAt = row.optString("queuedAt", "").take(10)
                        if (qAt == today()) continue
                        val stamp = JSONObject()
                            .put("queuedAt", today())
                            .put("updatedAt", isoNow())
                        val okStamp = SupabaseClient.updateById("patients", id, stamp)
                        if (!okStamp && appCtx != null) {
                            try { GenericUpdateQueue.queue(appCtx, "patients", id, stamp) } catch (_: Throwable) { }
                        }
                        try { MyPhoneWrites.remember(appCtx, "patients", id, stamp) } catch (_: Throwable) { }
                        continue
                    }
                    val fields = JSONObject()
                        .put("queue", true)
                        .put("stage", "Doctor Queue")
                        .put("doctorComplete", false)
                        /* 📅 V1013 — আজ তালিকায় উঠলেন, সেই দিনটাই এখানে বসে।
                           আগে এই দিনটা আলাদা করে জানার উপায় ছিল না, তাই
                           `updatedAt` দেখতে হত — আর তাতেই পুরনো রোগীরা
                           ফিরে আসতেন। */
                        .put("queuedAt", java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US)
                            .format(java.util.Date()))
                        .put("updatedAt", isoNow())
                    val ok = SupabaseClient.updateById("patients", id, fields)
                    if (!ok && appCtx != null) {
                        try { GenericUpdateQueue.queue(appCtx, "patients", id, fields) } catch (_: Throwable) { }
                    }
                    try { MyPhoneWrites.remember(appCtx, "patients", id, fields) } catch (_: Throwable) { }
                }
            } catch (_: Throwable) { /* কখনো কল/সেভ আটকাবে না */ }
        }
    }
}
