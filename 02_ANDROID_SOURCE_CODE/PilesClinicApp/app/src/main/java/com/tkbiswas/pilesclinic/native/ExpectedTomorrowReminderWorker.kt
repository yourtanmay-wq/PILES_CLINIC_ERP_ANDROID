package com.tkbiswas.pilesclinic.native

import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.tkbiswas.pilesclinic.R
import com.tkbiswas.pilesclinic.print.BranchCatalog
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

/**
 * NEW (2026-08-07, TK-approved). Fires once each evening (5 PM). Reads the
 * "আসার কথা" (chamber_expected) rows dated TOMORROW for the logged-in staff's
 * OWN branch and, if any, posts one phone notification so the staff can call
 * those people a day ahead. Tapping it opens the in-app "কাল আসার কথা" list.
 *
 * Safety / cost:
 *   • ONE Supabase read per fire (filtered to tomorrow's date), so it is
 *     free-plan friendly and never touches money/attendance rows.
 *   • MASTER is deliberately excluded (TK: "মাস্টারকে ডিস্টার্ব করা যাবে না")।
 *   • A date-flag makes it ring at most ONCE per day even if WorkManager runs
 *     the worker twice (TK: "ঘন্টা যেন বারবার না আসে")।
 *   • Wrapped so it can never crash; it always re-schedules the next day.
 *
 * Best-effort reminder, not a guaranteed alarm (Android battery optimisation
 * can delay background work on some phones) — same caveat as CallReminder.
 */
class ExpectedTomorrowReminderWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        /* 🟢 V590 — ওয়ার্কার আলাদা করে চলে, তাই কে লগইন করা আছে সেটা এখানে
           একবার দেখে নেওয়া হয়; নইলে বাংলা-বন্ধ নিয়মটা এই নোটিফিকেশনে খাটত না।
           ⛔ শুধু ফোনে জমা সেশন পড়া — কোনো নেটওয়ার্ক-কল নেই। */
        try { NoBengali.refresh(applicationContext) } catch (_: Throwable) { }
        try {
            val ctx = applicationContext
            val user = NativeSession.current(ctx)
            // Only real branch staff get this. Master sees every branch and does
            // not make these calls, so master is never notified here.
            if (user != null && user.role != "master" && user.branch.isNotBlank()) {
                val todayKey = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Calendar.getInstance().time)
                val prefs = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                // Ring at most once per calendar day.
                if (prefs.getString(KEY_LAST_FIRED, "") != todayKey) {
                    val due = expectedTomorrow(user.branch)
                    if (due.isNotEmpty()) {
                        notify(ctx, due)
                        prefs.edit().putString(KEY_LAST_FIRED, todayKey).commit()
                    }
                }
            }
        } catch (_: Exception) {
            // never crash the worker
        } finally {
            ExpectedTomorrowReminderScheduler.scheduleNext(applicationContext)
        }
        return Result.success()
    }

    private data class Expected(val name: String, val mobile: String)

    /** Reads chamber_expected rows dated tomorrow, kept to this branch. */
    private fun expectedTomorrow(branch: String): List<Expected> {
        val tomorrow = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, 1) }
        val key = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(tomorrow.time)
        val rows = try {
            SupabaseClient.fetchList(
                "payments",
                "payType=eq.chamber_expected&date=eq.$key",
                200
            )
        } catch (_: Throwable) { return emptyList() }
        val myBranchId = BranchCatalog.byName(branch).id
        val out = ArrayList<Expected>()
        val seen = HashSet<String>()
        for (i in 0 until rows.length()) {
            val row = rows.optJSONObject(i) ?: continue
            if (row.optString("payType", "") != "chamber_expected") continue
            if (BranchCatalog.byName(row.optString("branch", "")).id != myBranchId) continue
            val mobile = row.optString("mobile", "")
            if (!seen.add(mobile.ifBlank { row.optString("id", "") })) continue
            out.add(Expected(row.optString("name", ""), mobile))
        }
        return out
    }

    private fun notify(ctx: Context, due: List<Expected>) {
        val nm = ctx.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channel = NoticeChannels.ensure(
            ctx, CHANNEL_ID, "Tomorrow's Chamber", "One-day call reminder for tomorrow's expected patients"
        )
        val open = Intent(ctx, ExpectedTomorrowActivity::class.java)
            .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        val flags = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M)
            android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE
        else android.app.PendingIntent.FLAG_UPDATE_CURRENT
        val pi = android.app.PendingIntent.getActivity(ctx, 0, open, flags)

        val count = due.size
        val shown = due.take(5).joinToString("\n") { "• ${it.name.ifBlank { it.mobile }}" }
        val extra = (count - 5).let { if (it > 0) "\n+$it more" else "" }

        // Lock screen: count only (no names, privacy) — same rule as CallReminder.
        val publicVersion = NotificationCompat.Builder(ctx, channel)
            .setSmallIcon(R.drawable.ic_notif_bell)
            .setColor(android.graphics.Color.parseColor("#0F766E"))
            /* 🟢🔒 V590 (TK-নির্দেশ) — নোটিফিকেশন কোনো পর্দার ভিতরে থাকে না,
               তাই বাংলা-বন্ধ স্টাফের স্বয়ংক্রিয় সুইপ এখানে পৌঁছায় না — শিরোনামে
               কাঁচা বাংলাই যেত। এখন `NoBengali.s()` দিয়ে (হিন্দি/ইংরেজি)। */
            .setContentTitle(NoBengali.s("📞 কাল আসার কথা"))
            .setContentText("$count patient(s) expected tomorrow — call to remind them.")
            .build()

        val n = NotificationCompat.Builder(ctx, channel)
            .setSmallIcon(R.drawable.ic_notif_bell)
            .setColor(android.graphics.Color.parseColor("#0F766E"))
            .setContentTitle(NoBengali.s("📞 কাল আসার কথা") + " ($count)")   // 🟢 V590
            .setContentText("Tap to view — " + due.take(2).joinToString(", ") { it.name.ifBlank { it.mobile } } + if (count > 2) "…" else "")
            .setStyle(NotificationCompat.BigTextStyle().bigText(shown + extra))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pi)
            .setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
            .setPublicVersion(publicVersion)
            .build()
        nm.notify(NOTIF_ID, n)
    }

    companion object {
        const val CHANNEL_ID = "expected_tomorrow"
        const val NOTIF_ID = 4208
        private const val PREFS = "piles_clinic_expected_tomorrow"
        private const val KEY_LAST_FIRED = "lastFired"
    }
}
