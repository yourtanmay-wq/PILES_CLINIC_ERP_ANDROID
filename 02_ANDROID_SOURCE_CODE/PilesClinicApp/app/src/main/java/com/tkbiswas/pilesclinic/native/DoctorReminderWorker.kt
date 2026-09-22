package com.tkbiswas.pilesclinic.native

import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.tkbiswas.pilesclinic.R
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

/**
 * 🟢🔒🔒 V656 (২৫.০৮.২০২৬, TK-নির্দেশ, তিনটে প্রশ্ন করে নিশ্চিত হয়ে) →
 * 🔴🔒 V671 (২৫.০৮.২০২৬, TK-নির্দেশ) — "শুধুমাত্র তারিখ বাঁচলে হবে না,
 * সময়টাও তো বাঁচতে হবে তবেই তো নোটিফিকেশন সাউন্ড হবে"।
 *
 * `BriefingReminderScheduler`-এর হুবহু একই প্রমাণিত প্রতি-১৫-মিনিট
 * WorkManager one-time-chain (exact-alarm অনুমতি লাগে না)। প্রতিবার
 * চালু হয়ে দেখে — ডাক্তারের বাছা **সময়টা পেরিয়ে গেছে কিনা**; পেরিয়ে
 * থাকলে সঙ্গে সঙ্গে বাজে, দিনে একবার। সময় না বাছা থাকলে (ঐচ্ছিক — TK:
 * "কোনোটাই বাধ্যতামূলক নয়") পুরনো ডিফল্ট সন্ধ্যা ৫টা ধরা হয়।
 *
 * 🔔🔒 V1185 (০৭.০৯.২০২৬, Dr. K H MANDAL-এর কপ্লেন) — আগে শর্ত ছিল
 * "±৭ মিনিটের জানালা"; WorkManager দেরিতে চললে জানালাটা মিস হয়ে যেত ও
 * ওই রিমাইন্ডার **সেদিন আর কখনোই** বাজত না। নিচে বিস্তারিত লেখা আছে।
 */
class DoctorReminderWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        try { NoBengali.refresh(applicationContext) } catch (_: Throwable) { }
        try {
            val ctx = applicationContext
            val user = NativeSession.current(ctx)
            /* 🔔🔒 V1186 (০৭.০৯.২০২৬, TK-নির্দেশ) — নতুন `doctor_reminders`
               টেবিলের রিমাইন্ডারও এই একই কাজের ভিতরেই বাজে, তাই বাড়তি কোনো
               WorkManager কাজ বা ব্যাটারি-খরচ নেই।
               ⛔ **স্টাফও পান** — কারণ এখন যে কেউ পাঠাতে পারেন, আর যাঁকে
                  পাঠানো হয়েছে তিনি স্টাফ-ডাক্তার যেই হোন, তাঁর ফোনেই বাজা উচিত।
               ⛔ নিজের try/catch-এ — ব্যর্থ হলেও নিচের পুরনো (patients-ভিত্তিক)
                  রিমাইন্ডার এক অক্ষরও প্রভাবিত হয় না। */
            try { notifyNewTable(ctx, user) } catch (_: Throwable) { }
            // ⛔ শুধু ডাক্তার — TK-এর স্পষ্ট নির্দেশ ("শুধু ডাক্তার")।
            if (user != null && user.role == "doctor") {
                /* 🔔🔒 V839 (TK-নির্দেশ) — NEXT VISIT PLAN-এর দুটো নোটিফিকেশন
                   (কাল কী আছে · রোগী এসেছেন) **এই চলতি কাজের ভিতরেই** —
                   নতুন কোনো WorkManager কাজ নয়, তাই ব্যাটারিতে বাড়তি চাপ নেই।
                   ⛔ নিজের try/catch-এ; ব্যর্থ হলেও নিচের পুরনো রিমাইন্ডার
                      এক অক্ষরও প্রভাবিত হয় না। */
                try { NextVisitPlanNotifier.run(ctx) } catch (_: Throwable) { }
                val now = Calendar.getInstance()
                val todayKey = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(now.time)
                val prefs = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                val alreadyFiredIds = prefs.getStringSet(firedKeyFor(todayKey), emptySet()) ?: emptySet()
                val nowHM = now.get(Calendar.HOUR_OF_DAY) * 60 + now.get(Calendar.MINUTE)
                val defaultSlotHM = DEFAULT_SLOT_HOUR * 60
                /* 🩺🔒 V1109 (০৫.০৯.২০২৬, TK-নির্দেশ: *"কোন ডাক্তারকে মনে করিয়ে
                   দিতে হবে সেই ডাক্তারের নাম যেন চুস করা যায়, এবং এটা যেন
                   কার্যকরী হয়"*)।
                   🔴 আগে কী হত (মেপে দেখা): উপরের `role == "doctor"` ছাড়া আর
                      কোনো ছাঁকনি ছিল না ⇒ ওই দিনের **প্রতিটা** রিমাইন্ডার
                      **সব ডাক্তারের** ফোনে বাজত, যদিও পর্দার লেখায় বলা ছিল
                      *"শুধু আপনাকেই"*।
                   ⇒ এখন সারিতে ডাক্তার বাছা থাকলে **শুধু তাঁর ফোনেই** বাজে।
                   ⛔ পুরনো সারিতে ঘরটা ফাঁকা ⇒ আচরণ হুবহু আগের মতোই (সবার কাছে),
                      তাই আগের কোনো রিমাইন্ডার হারায় না।
                   ⛔ নম্বর মেলানো হয় **শেষ ১০ অঙ্ক** ধরে (+91 থাকুক বা না থাকুক) —
                      প্রকল্পের সব জায়গার একই নিয়ম। */
                val meDigits = (user.mobile).filter { it.isDigit() }.takeLast(10)
                val due = dueTomorrow().filter { d ->
                    val want = d.forMobile.filter { it.isDigit() }.takeLast(10)
                    if (want.isNotEmpty() && want != meDigits) return@filter false
                    true
                }.filter { d ->
                    if (d.id in alreadyFiredIds) return@filter false
                    /* 🔔🔒 V1185 (০৭.০৯.২০২৬, TK-নির্দেশ — Dr. K H MANDAL-এর কপ্লেন:
                       *"বারংবার রিমাইন্ডার সেট করা সত্ত্বেও আমাকে কেন কোনো
                       নোটিফিকেশন … মনে করিয়ে দেওয়া হচ্ছে না?"*)।

                       🔴 **আসল কারণ (কোডে মেপে বের করা, আন্দাজ নয়):** আগে শর্ত ছিল
                          `|বাছা সময় − এখন| ≤ ৭ মিনিট` — অর্থাৎ ব্যাকগ্রাউন্ডের কাজটা
                          ঠিক ওই ১৫ মিনিটের জানালার ভিতরে চলতেই হত। WorkManager-এর
                          সময় **নিশ্চিত নয়** (Doze · ব্যাটারি সাশ্রয়ে দেরি হয়),
                          তাই জানালাটা একবার পেরিয়ে গেলে ওই রিমাইন্ডার **সেদিন আর
                          কখনোই** বাজত না — চিরতরে হারিয়ে যেত।

                       ⇒ এখন নিয়মটা "জানালা" নয়, **"সময় হয়ে গেছে কিনা"**:
                         বাছা সময় পেরিয়ে গেলেই পরের বার কাজটা চালু হওয়ামাত্র বাজে।
                       ⛔ দিনে একবারের বেশি নয় — `alreadyFiredIds` (উপরে) আগের
                          মতোই পাহারা দেয়, তাই বারবার বাজার সুযোগ নেই।
                       ⛔ সময়ের **আগে** কখনো বাজে না, আর দিনটাও আগের মতোই
                          "তারিখের আগের দিন" (`dueTomorrow()` অপরিবর্তিত)।
                       ⛔ সময় না বাছা থাকলে আগের মতোই ডিফল্ট সন্ধ্যা ৫টা — শুধু
                          এখন ৫টা পেরিয়ে গেলেও বাজে, মিস হয় না। */
                    val wantHM = d.timeHM ?: defaultSlotHM
                    nowHM >= wantHM
                }
                if (due.isNotEmpty()) {
                    notify(ctx, due)
                    val updated = (alreadyFiredIds + due.map { it.id }).toMutableSet()
                    prefs.edit().putStringSet(firedKeyFor(todayKey), updated).commit()
                }
            }
        } catch (_: Exception) {
            // worker কখনো ক্র্যাশ করবে না
        } finally {
            DoctorReminderScheduler.scheduleNext(applicationContext)
        }
        return Result.success()
    }

    /**
     * 🔔🔒 V1186 — নতুন টেবিলের (`doctor_reminders`) রিমাইন্ডার।
     * পুরনো পথের হুবহু একই তিনটে নিয়ম: **তারিখের আগের দিন** · বাছা **সময়
     * পেরিয়ে গেলে** · **দিনে একবার**।
     * ⛔ যাঁকে পাঠানো হয়েছে কেবল তাঁর ফোনেই (নাম না দেওয়া থাকলে ওই ব্রাঞ্চের
     *    সবার)। ⛔ যিনি পাঠিয়েছেন তাঁর নিজের ফোনে বাজে না।
     * ⛔ একটাই সরু পড়া — শুধু আগামীকালের সারিগুলো।
     */
    private fun notifyNewTable(ctx: Context, user: NativeUser?) {
        if (user == null) return
        val me = user.mobile.filter { it.isDigit() }.takeLast(10)
        if (me.length != 10) return
        /* 🔔🔒 V1241 (০৮.০৯.২০২৬, TK-রিপোর্ট ছবিসহ — Dr. K.H MANDAL-এর
           তিন-চারবারের অভিযোগ, খাতার সারি ৩৬১): *"alarm / notification কেন আসে না"*।

           🔴 **আসল কারণ (কোডে মেপে, আন্দাজ নয় — এবং দোষটা আমারই):**
              এই পড়াটা **শুধু আগামীকালের** সারিগুলো আনত (`remindDate = কাল`),
              কারণ ব্যবস্থাটা বানানো হয়েছিল "আগের দিন মনে করিয়ে দেওয়া"র জন্য।
              কিন্তু পর্দার তারিখ-বাছাই **আজকের তারিখও বাছতে দেয়** (minDate = আজ),
              আর কার্ডে লেখা থাকে *"Remind on 08/09/2026 · 11.12 PM"* —
              ⇒ কেউ **আজকের** দিনের রিমাইন্ডার দিলে "আগের দিন" মুহূর্তটা
                (গতকাল) আগেই পেরিয়ে গেছে, তাই সেটা **কোনোদিনই বাজত না** —
                চুপচাপ হারিয়ে যেত। TK-এর ছবিটা ঠিক সেই সারিটাই।

           ⇒ এখন **আজ ও আগামীকাল দুটো দিনই** পড়া হয়। নিচের সময়ের নিয়ম
             (`nowHM >= wantHM`) দুটোতেই এক — তাই আগামীকালের সারি আগের মতোই
             **আগের দিন** বাজে (এক অক্ষরও বদলায়নি), আর আজকের সারি **আজ,
             বাছা সময় হয়ে গেলেই** বাজে।
           ⛔ দিনে একবারের বেশি নয় — নিচের `fired_new_<আজকের তারিখ>` আগের মতোই
              পাহারা দেয় (আগের দিনের ঘণ্টা আর আজকের ঘণ্টা আলাদা দিনের তালিকায়,
              তাই দুটোই বাজে — যা রিমাইন্ডারের জন্য ঠিক)।
           ⛔ ক্লাউডে **একটাই পড়া, আগের মতোই** — শুধু দুটো তারিখ চাওয়া হয়,
              তাই খরচ বাড়ে না (নিয়ম ১৩)। */
        val tomorrow = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, 1) }
        val keyTomorrow = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(tomorrow.time)
        val keyToday = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Calendar.getInstance().time)
        val rows = try {
            SupabaseClient.fetchList(
                DoctorReminderRepository.TABLE,
                "active=eq.true&remindDate=in.($keyToday,$keyTomorrow)", 200, order = "createdAt.desc"
            )
        } catch (_: Throwable) { return }
        if (rows.length() == 0) return

        val now = Calendar.getInstance()
        val nowHM = now.get(Calendar.HOUR_OF_DAY) * 60 + now.get(Calendar.MINUTE)
        val todayKey = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(now.time)
        val prefs = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val firedKey = "fired_new_$todayKey"
        val fired = prefs.getStringSet(firedKey, emptySet()) ?: emptySet()

        val mine = ArrayList<Due>()
        var nextAlarmAt = 0L                     // ⏰ V1241
        for (i in 0 until rows.length()) {
            val r = rows.optJSONObject(i) ?: continue
            val id = r.optString("id", "")
            if (id.isBlank() || id in fired) continue
            val note = r.optString("note", "").trim()
            if (note.isBlank()) continue
            val forM = r.optString("forMobile", "").filter { it.isDigit() }.takeLast(10)
            val byM = r.optString("byMobile", "").filter { it.isDigit() }.takeLast(10)
            if (byM == me && forM != me) continue        // নিজের পাঠানো নিজেকে নয়
            val hit = if (forM.isNotEmpty()) forM == me
                      else r.optString("branch", "").trim().equals(user.branch.trim(), true)
            if (!hit) continue
            val ts = r.optString("remindTime", "").trim()
            val hm = if (ts.isNotBlank()) try {
                val p = ts.split(":").map { it.toInt() }; p[0] * 60 + p[1]
            } catch (_: Throwable) { null } else null
            /* ⏰🔒 V1241 (TK: *"যে টাইম সেট করবে সেই টাইমে জোর করে নোটিফিকেশন
               দিতে হবে"*) — আজ যে সময়গুলো **এখনো আসেনি**, তার মধ্যে সবচেয়ে
               কাছেরটার জন্য ফোনের **অ্যালার্ম ঘড়ি** বসানো হয় (নিচে)।
               ⛔ দিনটা সবসময় **আজ** — কারণ বাজার নিয়মটাই "আজকের ঘড়িতে সময়
                  হয়েছে কিনা": আগামীকালের সারি আজ বাজে (আগের দিনের মনে করানো,
                  অপরিবর্তিত), আর আজকের সারি আজ বাজে (V1241-এ নতুন)।
               ⛔ ক্লাউডে বাড়তি একটাও পড়া নয় — এই একই সারিগুলো থেকেই। */
            val hmWant = hm ?: (DEFAULT_SLOT_HOUR * 60)
            if (nowHM < hmWant) {
                val at = DoctorReminderAlarm.millisOf(
                    todayKey, String.format(Locale.US, "%02d:%02d", hmWant / 60, hmWant % 60)
                )
                if (at > System.currentTimeMillis() && (nextAlarmAt == 0L || at < nextAlarmAt)) nextAlarmAt = at
                continue
            }
            mine.add(Due(id, r.optString("patientName", "").trim(), note, hm, forM))
        }
        // ⏰ V1241 — এখনো বাকি থাকা সবচেয়ে কাছের সময়ের জন্য অ্যালার্ম বসাই।
        if (nextAlarmAt > 0L) DoctorReminderAlarm.scheduleAt(ctx, nextAlarmAt)
        if (mine.isEmpty()) return
        /* 🔔 V1241 — নতুন টেবিলের ঘণ্টার নিজের নম্বর। আগে দুটো পথই একই
           নম্বর ব্যবহার করত, তাই একই দফায় দুটোই বাজলে **দ্বিতীয়টা প্রথমটাকে
           মুছে দিত** — ডাক্তার একটা রিমাইন্ডার দেখতেই পেতেন না।
           ⛔ শুধু নম্বর আলাদা; লেখা · শব্দ · চ্যানেল কিছুই বদলায়নি। */
        notify(ctx, mine, NOTIF_ID_NEW)
        prefs.edit().putStringSet(firedKey, (fired + mine.map { it.id }).toMutableSet()).commit()
    }

    private data class Due(
        val id: String, val name: String, val note: String, val timeHM: Int?,
        /* 🩺 V1109 — কোন ডাক্তারের জন্য। ফাঁকা = সব ডাক্তার (পুরনো আচরণ)। */
        val forMobile: String = ""
    )

    /** patients টেবিলে যাদের doctorReminderDate = আগামীকাল। */
    private fun dueTomorrow(): List<Due> {
        /* 🔔 V1241 (নিয়ম ৭ — একই দোষ সব জায়গায়) — পুরনো (patients-ভিত্তিক)
           পথেও হুবহু একই দোষ ছিল: শুধু আগামীকালের তারিখ পড়া হত, তাই **আজকের
           তারিখে** দেওয়া রিমাইন্ডার কোনোদিনই বাজত না। এখন আজ ও আগামীকাল
           দুটোই। ⛔ আগামীকালের সারির আচরণ এক অক্ষরও বদলায়নি। */
        val tomorrow = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, 1) }
        val keyTomorrow = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(tomorrow.time)
        val keyToday = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Calendar.getInstance().time)
        val rows = try {
            SupabaseClient.fetchListSlimOrNull(
                "patients", "doctorReminderDate=in.($keyToday,$keyTomorrow)", 200,
                // 🔴🔒 V671 — doctorReminderTime-ও এখন পড়া হয়।
                // 🩺 V1109 — `doctorReminderFor`-ও পড়া হয় (কোন ডাক্তারের জন্য)।
                "id,name,doctorReminderNote,doctorReminderDate,doctorReminderTime,doctorReminderFor"
            )
        } catch (_: Throwable) { null } ?: return emptyList()
        val out = ArrayList<Due>()
        for (i in 0 until rows.length()) {
            val row = rows.optJSONObject(i) ?: continue
            val note = row.optString("doctorReminderNote", "").trim()
            if (note.isBlank()) continue
            val timeStr = row.optString("doctorReminderTime", "").trim()
            val timeHM = if (timeStr.isNotBlank()) try {
                val parts = timeStr.split(":").map { it.toInt() }
                parts[0] * 60 + parts[1]
            } catch (_: Throwable) { null } else null
            out.add(Due(
                row.optString("id", "").trim(), row.optString("name", "").trim(), note, timeHM,
                row.optString("doctorReminderFor", "").trim()   // 🩺 V1109
            ))
        }
        return out
    }

    private fun notify(ctx: Context, due: List<Due>, notifId: Int = NOTIF_ID) {
        val nm = ctx.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channel = NoticeChannels.ensure(
            ctx, CHANNEL_ID, "Doctor Reminder", "One-day-ahead reminder for patient notes the doctor wrote"
        )
        val open = Intent(ctx, DashboardActivity::class.java)
            .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        val flags = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M)
            android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE
        else android.app.PendingIntent.FLAG_UPDATE_CURRENT
        val pi = android.app.PendingIntent.getActivity(ctx, 0, open, flags)

        val count = due.size
        val shown = due.take(5).joinToString("\n") { "• ${it.name.ifBlank { "Patient" }}: ${it.note}" }
        val extra = (count - 5).let { if (it > 0) "\n+$it more" else "" }

        // লক-স্ক্রিনে শুধু সংখ্যা (গোপনীয়তা) — বাকি সব রিমাইন্ডারের একই নিয়ম।
        val publicVersion = NotificationCompat.Builder(ctx, channel)
            .setSmallIcon(R.drawable.ic_notif_bell)
            .setColor(android.graphics.Color.parseColor("#0F766E"))
            .setContentTitle(NoBengali.s("🔔 আগামীকালের রোগী-নোট"))
            .setContentText("$count reminder(s) for tomorrow.")
            .build()

        val n = NotificationCompat.Builder(ctx, channel)
            .setSmallIcon(R.drawable.ic_notif_bell)
            .setColor(android.graphics.Color.parseColor("#0F766E"))
            .setContentTitle(NoBengali.s("🔔 আগামীকালের রোগী-নোট") + " ($count)")
            .setContentText(due.first().name.ifBlank { "Patient" } + ": " + due.first().note)
            .setStyle(NotificationCompat.BigTextStyle().bigText(shown + extra))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pi)
            .setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
            .setPublicVersion(publicVersion)
            .build()
        nm.notify(notifId, n)
    }

    companion object {
        const val CHANNEL_ID = "doctor_reminder"
        const val NOTIF_ID = 4209
        const val NOTIF_ID_NEW = 4210   // 🔔 V1241
        private const val PREFS = "piles_clinic_doctor_reminder"
        // 🔴🔒 V671 — এখন প্রতি ১৫ মিনিটে চেক (আগে দিনে একবার, ৫টায়)।
        const val REPEAT_GAP_MINUTES = 15
        const val DEFAULT_SLOT_HOUR = 17
        // 🔴🔒 V671 — একই রোগীর জন্য একই দিনে দুবার না বাজে, তাই প্রতিদিনের
        // "কাদের বাজানো হয়েছে" তালিকা (আগে শুধু একটা date-flag ছিল)।
        private fun firedKeyFor(dateKey: String) = "fired_$dateKey"
    }
}
