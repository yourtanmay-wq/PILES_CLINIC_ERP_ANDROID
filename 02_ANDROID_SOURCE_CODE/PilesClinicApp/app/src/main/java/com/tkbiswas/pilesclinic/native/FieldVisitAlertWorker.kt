package com.tkbiswas.pilesclinic.native

import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.tkbiswas.pilesclinic.R

/**
 * 🛰️🔒 V1345 (১১.০৯.২০২৬, TK-নির্দেশ) — **মাস্টারকে জানানো: কোন স্টাফের
 * লোকেশন আজ কাজ করছে না।**
 *
 * প্রেক্ষাপট: RUPAM-এর কিমি/লোকেশন বারবার ০.০ থাকার আসল কারণ (V1344-এ
 * সারানো অনুমতির ফাঁক) কোডেই ধরা পড়েছিল, কিন্তু TK প্রশ্ন করেছিলেন — এর
 * বাইরে ব্যাটারি-সেভার GPS বন্ধ করে দিলে সেটা তো স্টাফের ফোনের নিজস্ব লাল
 * সতর্কতা ছাড়া TK কখনো জানতেই পারতেন না, যতক্ষণ না নিজে গিয়ে Field Visit
 * Tracking পর্দাটা খুলে দেখতেন। TK নিজেই বেছেছেন এই মাস্টার-অ্যালার্ট।
 *
 * 🏍️🔒 V1471 (১৪.০৯.২০২৬) — আগে দিনে **একবারই** (দুপুর ৩টা) চেক হতো, তাই
 * সেদিনই RUPAM-এর ২.৫ ঘন্টার ফাঁক TK সন্ধ্যায় নিজে পর্দা খুলে দেখার আগে
 * কখনো জানতেই পারেননি। এখন `FieldVisitAlertScheduler` কাজের সময় (সকাল
 * ৯টা–রাত ৯টা) **প্রতি ৪৫ মিনিটে** আবার বসায়।
 *
 * 🟢🔒 V1472 (১৪.০৯.২০২৬, TK-নির্দেশ: "শুধু ওই ২ staff নয়, সবার ক্ষেত্রেই
 * একই নিয়ম রাখুন") — আগে শুধু RUPAM/ARMAN (`FieldVisit.isFieldStaff`)। এখন
 * হাজিরা-ব্যবহারকারী **সব staff/branch** অ্যাকাউন্ট (role=="staff")।
 *
 * ─── যা করে ───────────────────────────────────────────────────────────────
 * প্রতি ৪৫ মিনিটে আজকের সব হাজিরা-স্টাফের `wn.field_visit_days` সারি দেখে —
 * যাঁদের IN TIME/শুরু হয়েছে (`started_at`), এখনো OUT হননি (`ended_at` ফাঁকা),
 * আজ **অন্তত একবার** লোকেশন এসেছিল (`last_seen_at` আছে — নইলে হয়তো তিনি
 * লোকেশন-অনুমতিই দেননি, যেটা staff/branch-এর জন্য ঐচ্ছিক, দোষ নয়), কিন্তু
 * গত ৪৫ মিনিটে নতুন কিছু আসেনি — তাঁদের নাম নিয়ে মাস্টারের ফোনে নোটিফিকেশন।
 * কেউ বাকি না থাকলে/সবার লোকেশন ঠিক থাকলে **একদম চুপ**।
 *
 * ─── ⛔ নিরাপত্তা ও খরচ ───────────────────────────────────────────────────
 *  • শুধু মাস্টারের ফোনে চলে, আর শুধু আজ IN TIME করা স্টাফদের সারি পড়ে
 *    (সাধারণত হাতে-গোনা কয়েকজন, একটা ছোট ডাক) — Egress নগণ্য।
 *  • `MasterOutTimeWorker`/`MasterOutTimeScheduler`-এ এতটুকুও হাত পড়েনি —
 *    সম্পূর্ণ আলাদা, স্বাধীন WorkManager-চেইন।
 *  • কিছু ভুল হলে চুপচাপ ফিরে যায় — কোনো কিছু ভাঙে না, কোনো তথ্য লেখা হয় না।
 *
 * ⚠️ সৎ সীমা (প্রজেক্টের বাকি সব ব্যাকগ্রাউন্ড রিমাইন্ডারের মতোই): কিছু
 *    ফোনের battery-optimization এই কাজটা নিজেই দেরি/বাদ দিতে পারে — এটা
 *    best-effort, ১০০% গ্যারান্টিড alarm নয়।
 */
class FieldVisitAlertWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val ctx = applicationContext
        try {
            val user = NativeSession.current(ctx)
            if (user != null && user.role == "master") {
                val stale = staleFieldStaff(ctx)
                if (stale.isNotEmpty()) notify(ctx, stale)
            }
        } catch (_: Throwable) {
            // কখনো ক্র্যাশ করবে না
        }
        try { FieldVisitAlertScheduler.scheduleNext(ctx) } catch (_: Throwable) { }
        return Result.success()
    }

    /** আজ IN হয়েছে, এখনো OUT হয়নি, কিন্তু ৪৫ মিনিটেও লোকেশন আসেনি — এমন ফিল্ড-স্টাফের নাম। */
    private suspend fun staleFieldStaff(ctx: Context): List<String> {
        return try {
            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                val ma = com.tkbiswas.pilesclinic.modules.ModuleAuth
                if (!ma.isSignedIn) { try { ma.signInCurrentSession(ctx) } catch (_: Throwable) { } }
                if (!ma.isSignedIn) return@withContext emptyList<String>()
                /* 🟢🔒 V1472 (১৪.০৯.২০২৬, TK-নির্দেশ: "শুধু ওই ২ staff নয়, সবার
                   ক্ষেত্রেই একই নিয়ম রাখুন") — আগে শুধু RUPAM/ARMAN (isFieldStaff)।
                   এখন হাজিরা-ব্যবহারকারী সব staff/branch অ্যাকাউন্ট (role=="staff",
                   RoleRules.usesAttendance()-এর হুবহু একই সংজ্ঞা — Doctor/Field-role/
                   Master বাদ)। */
                val roster = StaffDirectory.allAccounts().filter { it.role == "staff" }
                if (roster.isEmpty()) return@withContext emptyList<String>()
                val today = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US)
                    .apply { timeZone = java.util.TimeZone.getTimeZone("Asia/Kolkata") }
                    .format(java.util.Date())
                val res = ma.getRowsChecked(
                    "wn", "field_visit_days",
                    "select=staff_code,started_at,ended_at,last_seen_at&work_date=eq.$today&limit=50"
                )
                if (!res.ok) return@withContext emptyList<String>()
                val cutoffMs = System.currentTimeMillis() - 45L * 60L * 1000L
                val out = mutableListOf<String>()
                for (acc in roster) {
                    var row: org.json.JSONObject? = null
                    for (i in 0 until res.rows.length()) {
                        val r = res.rows.optJSONObject(i) ?: continue
                        if (r.optString("staff_code", "").equals(acc.name, ignoreCase = true)) { row = r; break }
                    }
                    if (row == null) continue   // আজ IN-ই করেননি — এই অ্যালার্টের বিষয় নয়
                    val started = row.optString("started_at", "")
                    val ended = row.optString("ended_at", "")
                    if (started.isBlank() || ended.isNotBlank()) continue
                    /* 🟢🔒 V1472 — সাধারণ staff/branch-এর জন্য লোকেশন-অনুমতি
                       **ঐচ্ছিক** (V1346-এর নিজের নিয়ম, "না দিলেও IN TIME আটকাবে
                       না")। তাই যাঁর আজ একটাও লোকেশন-ফিক্স কখনোই আসেনি (হয়তো
                       অনুমতিই দেননি — এটা তাঁর অধিকার, দোষ নয়), তাঁকে "থেমে
                       গেছে" বলে বারবার সতর্ক করা ভুল — RUPAM-এর মতো সত্যিকারের
                       বিচ্ছিন্নতা (আগে ঠিকই চলছিল, তারপর থেমে গেছে) থেকে আলাদা
                       করতে হবে। শুধু last_seen_at-এ **আসল কোনো মান আছে** (মানে
                       ট্র্যাকিং সত্যিই একবার চলেছিল) অথচ এখন পুরনো — তবেই সতর্ক। */
                    val lastSeenStr = row.optString("last_seen_at", "")
                    if (lastSeenStr.isBlank()) continue
                    val lastSeenMs = parseIsoMs(lastSeenStr)
                    if (lastSeenMs in 1 until cutoffMs) out.add(acc.name)
                }
                out.distinct()
            }
        } catch (_: Throwable) { emptyList() }
    }

    /** ⛔ FieldVisitActivity.kt-এর `parseIso()`-এর হুবহু একই ধাঁচ (একাধিক
     * সম্ভাব্য ফরম্যাট চেষ্টা) — নতুন কোনো java.time/desugaring নির্ভরতা
     * তৈরি না করে প্রকল্পের প্রমাণিত পদ্ধতিই পুনর্ব্যবহার করা হলো। */
    private fun parseIsoMs(iso: String): Long {
        if (iso.isBlank()) return 0L
        val cleaned = iso.trim().replace(" ", "T")
        val patterns = listOf(
            "yyyy-MM-dd'T'HH:mm:ss.SSSXXX", "yyyy-MM-dd'T'HH:mm:ssXXX",
            "yyyy-MM-dd'T'HH:mm:ss.SSS", "yyyy-MM-dd'T'HH:mm:ss"
        )
        for ((i, p) in patterns.withIndex()) {
            try {
                val f = java.text.SimpleDateFormat(p, java.util.Locale.US)
                if (i >= 2) f.timeZone = java.util.TimeZone.getTimeZone("UTC")
                return f.parse(cleaned)?.time ?: continue
            } catch (_: Throwable) { }
        }
        return 0L
    }

    private fun notify(ctx: Context, codes: List<String>) {
        try {
            val nm = ctx.getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
            val channel = NoticeChannels.ensure(
                ctx, CHANNEL_ID, "Staff location not working",
                "Tells the Master which staff's location has stopped updating today"
            )
            val names = codes.joinToString(", ")
            val intent = Intent(ctx, com.tkbiswas.pilesclinic.modules.WorkNotebookActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            }
            val pi = android.app.PendingIntent.getActivity(
                ctx, 9112, intent,
                android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE
            )
            val n = NotificationCompat.Builder(ctx, channel)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle("📍 Staff location not updating (${codes.size})")
                .setContentText(names)
                .setStyle(NotificationCompat.BigTextStyle().bigText(names))
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setAutoCancel(true)
                .setContentIntent(pi)
                .build()
            nm.notify(9112, n)
        } catch (_: Throwable) { }
    }

    companion object {
        private const val CHANNEL_ID = "field_visit_location_alert"
    }
}
