package com.tkbiswas.pilesclinic.native

import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.tkbiswas.pilesclinic.R
import com.tkbiswas.pilesclinic.clinical.NextVisitPlan
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * 🔔🔒 V839 (২৯.০৮.২০২৬) — **NEXT VISIT PLAN-এর নোটিফিকেশন**
 *
 * TK-নির্দেশ: *"প্লান থাকলে যেন Notification আসে, Sound হবে"* — এবং
 * *"দুটোই রাখব"* (আগের দিন সন্ধ্যায় + রোগী এলে)।
 *
 * ### ⛔ নতুন কোনো ব্যাকগ্রাউন্ড কাজ বানানো হয়নি
 * `DoctorReminderWorker` (V671) **আগে থেকেই** প্রতি ১৫ মিনিটে চলে, শুধু
 * ডাক্তারের ফোনে। এই দুটো কাজ ওরই ভিতরে জুড়ে দেওয়া হলো ⇒ **ব্যাটারিতে
 * বাড়তি চাপ শূন্য**, নতুন কোনো WorkManager কাজ নেই।
 *
 * ### 🔊 আওয়াজ
 * `NoticeChannels.ensure()` — প্রকল্পের নিজের প্রমাণিত চ্যানেল
 * (IMPORTANCE_HIGH · আওয়াজ · কাঁপুনি · আলো)। নতুন কিছু বানানো হয়নি।
 *
 * ### 🚨 Egress — মেপে বসানো
 * · **ক (আগের দিন):** সন্ধ্যার নির্দিষ্ট জানালায় **দিনে একবারই** একটামাত্র
 *   সরু পড়া, আর ছাঁকনি "যাঁদের প্ল্যান আছে" — প্ল্যানহীন সারি একটাও নামে না।
 * · **খ (রোগী এলে):** শুধু **গত ২০ মিনিটে বদলানো** সারি
 *   (`updatedAt=gt.…`) — সাধারণত ০–৩টা সারি। এটাই সবচেয়ে সস্তা পথ,
 *   কারণ `NextVisitQueue` রোগী ফেরানোর সময় `updatedAt` আজকের করে দেয়।
 * · দুটোই ব্যর্থ হলে **নীরবে কিছুই হয় না**।
 */
object NextVisitPlanNotifier {

    private const val PREFS = "nvp_notify"
    const val NOTIF_ID_TOMORROW = 4231
    const val NOTIF_ID_ARRIVED = 4232
    private const val CHANNEL_ID = "next_visit_plan"

    /** সন্ধ্যার জানালা — এই ঘণ্টায় "কাল কী আছে" একবার জানানো হয়। */
    private const val EVENING_HOUR = 20

    private fun ymd(d: Date): String = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(d)

    private fun prefs(ctx: Context) = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    /** ডাক্তারের ফোনে, তাঁর ব্রাঞ্চের সারি — এই দুটো শর্ত সব জায়গায় এক। */
    private fun branchFilter(branch: String): String {
        if (branch.isBlank() || branch.equals("All", true)) return ""
        return "&branch=eq." + java.net.URLEncoder.encode(branch, "UTF-8").replace("+", "%20")
    }

    /** ⛔ শুধু ডাক্তার — TK-এর স্পষ্ট নির্দেশ। */
    fun run(ctx: Context) {
        try {
            val user = NativeSession.current(ctx) ?: return
            if (user.role != "doctor") return
            tomorrowDigest(ctx, user.branch)
            arrivedNow(ctx, user.branch)
        } catch (_: Throwable) { /* worker কখনো ভাঙবে না */ }
    }

    // ── ক) আগের দিন সন্ধ্যায় — "কাল এতজনের প্ল্যান আছে" ──────────────
    private fun tomorrowDigest(ctx: Context, branch: String) {
        try {
            val now = Calendar.getInstance()
            if (now.get(Calendar.HOUR_OF_DAY) != EVENING_HOUR) return
            val todayKey = ymd(now.time)
            val sp = prefs(ctx)
            if (sp.getString("digest_day", "") == todayKey) return   // দিনে একবার

            val tomorrow = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, 1) }
            val tKey = ymd(tomorrow.time)

            /* 🔴🔒 V839 — **নিজের অডিটে ধরা পড়া ভুল** (TK আলাদা করে বলেননি):
               আগে এখানে `doctorComplete=is.false` ছাঁকনি ছিল। কিন্তু ডাক্তার
               প্ল্যান লিখে Save করলেই `doctorComplete=true` বসে যায় ⇒ ঠিক
               যাঁদের জন্য এই নোটিফিকেশন, তাঁরাই বাদ পড়ে যেতেন — **কোনোদিন
               বাজতই না**। এখন ছাঁকনি "যাঁদের প্ল্যান আছে" — এটাই সঠিক, আর
               সস্তাও (প্ল্যানহীন সারি একটাও নামে না)। */
            val rows = SupabaseClient.fetchListSlimOrNull(
                "patients", NextVisitPlan.FIELD + "=not.is.null" + branchFilter(branch), 300,
                "id,name,registrationDate," + NextVisitPlan.FIELD
            ) ?: return

            val lines = ArrayList<String>()
            for (i in 0 until rows.length()) {
                val r = rows.optJSONObject(i) ?: continue
                val e = NextVisitPlan.latest(r) ?: continue
                if (e.date.take(10) != tKey) continue
                val nm = r.optString("name", "").trim().ifBlank { "Patient" }
                val badge = NextVisitPlan.oldOrNew(r.optString("registrationDate", ""))
                lines.add("• $nm" + (if (badge.isNotBlank()) " ($badge)" else "") + ": " + e.shortLine())
            }
            // ⛔ কেউ না থাকলে কোনো নোটিফিকেশন নয় — অকারণে বাজবে না।
            if (lines.isEmpty()) { sp.edit().putString("digest_day", todayKey).apply(); return }

            notify(
                ctx, NOTIF_ID_TOMORROW,
                "🔵 Tomorrow: ${lines.size} patient(s) have a plan",
                lines.first().removePrefix("• "),
                lines.take(6).joinToString("\n") +
                    (if (lines.size > 6) "\n+${lines.size - 6} more" else "")
            )
            sp.edit().putString("digest_day", todayKey).apply()
        } catch (_: Throwable) { }
    }

    // ── খ) রোগী এলে — আওয়াজ সহ ──────────────────────────────────────
    private fun arrivedNow(ctx: Context, branch: String) {
        try {
            val since = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US)
                .format(Date(System.currentTimeMillis() - 20 * 60 * 1000L))
            val sinceEnc = java.net.URLEncoder.encode(since, "UTF-8")
            val rows = SupabaseClient.fetchListSlimOrNull(
                "patients", "updatedAt=gt.$sinceEnc&doctorComplete=is.false" + branchFilter(branch), 50,
                "id,name,registrationDate," + NextVisitPlan.FIELD
            ) ?: return

            val todayKey = ymd(Date())
            val sp = prefs(ctx)
            val key = "arrived_$todayKey"
            val seen = sp.getStringSet(key, emptySet()) ?: emptySet()
            val fresh = HashSet(seen)

            for (i in 0 until rows.length()) {
                val r = rows.optJSONObject(i) ?: continue
                val id = r.optString("id", "")
                if (id.isBlank() || seen.contains(id)) continue      // দিনে একবার/রোগী
                val e = NextVisitPlan.latest(r) ?: continue
                val nm = r.optString("name", "").trim().ifBlank { "Patient" }
                val badge = NextVisitPlan.oldOrNew(r.optString("registrationDate", ""))
                val big = StringBuilder(e.shortLine())
                if (e.medicine.isNotBlank()) big.append("\nMedicine: ").append(e.medicine)
                if (e.note.isNotBlank()) big.append("\n").append(e.note)
                if (e.byName.isNotBlank()) big.append("\n(").append(e.byName).append(")")
                notify(
                    ctx, NOTIF_ID_ARRIVED,
                    "🔔 $nm" + (if (badge.isNotBlank()) " ($badge)" else "") + " — " + "has a plan",
                    e.shortLine(), big.toString()
                )
                fresh.add(id)
            }
            if (fresh.size != seen.size) {
                // পুরনো দিনের চাবি জমতে দেওয়া হয় না — digest_day রেখে বাকি নতুন করে।
                val digest = sp.getString("digest_day", "") ?: ""
                sp.edit().clear().putString("digest_day", digest).putStringSet(key, fresh).apply()
            }
        } catch (_: Throwable) { }
    }

    private fun notify(ctx: Context, id: Int, title: String, text: String, big: String) {
        try {
            val nm = ctx.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val channel = NoticeChannels.ensure(
                ctx, CHANNEL_ID, "Next Visit Plan",
                "What the doctor planned for this patient's next visit"
            )
            val open = Intent(ctx, DoctorQueueActivity::class.java)
                .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            val flags = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M)
                android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE
            else android.app.PendingIntent.FLAG_UPDATE_CURRENT
            val pi = android.app.PendingIntent.getActivity(ctx, id, open, flags)

            /* লক-স্ক্রিনে রোগীর নাম দেখানো হয় না (গোপনীয়তা) — প্রকল্পের
               `DoctorReminderWorker`-এর হুবহু একই নিয়ম। */
            val publicVersion = NotificationCompat.Builder(ctx, channel)
                .setSmallIcon(R.drawable.ic_notif_bell)
                .setColor(android.graphics.Color.parseColor("#0B3D91"))
                .setContentTitle("🔵 Patient plan")
                .setContentText("Open the app to see.")
                .build()

            val n = NotificationCompat.Builder(ctx, channel)
                .setSmallIcon(R.drawable.ic_notif_bell)
                .setColor(android.graphics.Color.parseColor("#0B3D91"))
                .setContentTitle(title)
                .setContentText(text)
                .setStyle(NotificationCompat.BigTextStyle().bigText(big))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .setContentIntent(pi)
                .setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
                .setPublicVersion(publicVersion)
                .build()
            nm.notify(id, n)
        } catch (_: Throwable) { }
    }
}
