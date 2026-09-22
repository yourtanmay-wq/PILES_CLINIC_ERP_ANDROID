package com.tkbiswas.pilesclinic.native

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.tkbiswas.pilesclinic.R
import java.util.Calendar

/**
 * TK'S RULE (2026-07-27, locked, agreed point by point):
 *   1. starts at 7 PM, repeats every 10 minutes, stops at midnight
 *   2. STAFF ONLY (never Master, never Doctor)
 *   3. keeps reminding until that branch's chamber is closed
 *   4. stays completely SILENT if nobody arrived and no money was taken
 *
 * Tapping the reminder opens Chamber Date straight away.
 *
 * Cloud cost is kept as small as possible: first one tiny check of the
 * chamber_close table; the fuller board is read only when the chamber really
 * is still open.
 */
class ChamberCloseReminderWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        try {
            val ctx = applicationContext
            val user = NativeSession.current(ctx)
            val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
            if (user != null && user.role == "staff" && hour >= 19) {
                val branch = user.branch.trim()
                if (branch.isNotBlank() && !branch.equals("All", ignoreCase = true)) {
                    val today = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US)
                        .format(java.util.Date())
                    // a mark that could not reach the cloud earlier is sent
                    // first, so the reminder can actually stop
                    ChamberCloseRepository.flushPending(ctx)
                    if (!ChamberCloseRepository.isClosed(ctx, branch, today)) {
                        // 🟢🔒 B663 (15.08.2026, TK-অনুমোদিত · Egress-৫): এই মনে-করানোটা
                        //   সন্ধে ৭টা থেকে রাত ১২টা পর্যন্ত **প্রতি ১০ মিনিটে** চলে (TK-এর
                        //   লক করা নিয়ম — বদলানো হয়নি), আর প্রতিবার **পুরো বোর্ড** নামাত:
                        //   payments + enquiries + patients + followups, প্রতিটা 5000 পর্যন্ত।
                        //   অথচ এখান থেকে দরকার মাত্র দুটো জিনিস — কতজন এসেছেন, টাকা উঠেছে
                        //   কি না। চেম্বার বন্ধ না করা থাকলে ৫ ঘণ্টায় ৩০ বার, প্রতিটা ফোনে।
                        //   এখন **আগে একটা ছোট প্রশ্ন**: গতবারের পরে ওই চারটে টেবিলের
                        //   কোনোটাতে কিছু বদলেছে কি? (LiveRefresh-এর প্রমাণিত HEAD count-only —
                        //   **একটাও সারি নামে না**)। কিছু না বদলালে ফোনে **জমানো বোর্ডটাই**
                        //   ব্যবহার হয় — সন্ধের পরে ক্লিনিক থেমে গেলে বাকি টিকগুলো প্রায় বিনা খরচে।
                        //   ⛔ প্রথম টিকে সবসময় সত্যিকারের বোর্ড নামে (`primed`) — বাসি হিসাব
                        //     নিয়ে কখনো নোটিফিকেশন যাবে না।
                        //   ⛔ জমানো বোর্ড না থাকলেও সত্যিকারের বোর্ড নামে।
                        //   ⛔ প্রশ্নটা ব্যর্থ হলে ধরে নেওয়া হয় "বদলেছে" → সত্যিকারের বোর্ড নামে
                        //     (অর্থাৎ ঠিক আগের ব্যবহার), তাই কোনো মনে-করানো হারাবে না।
                        //   ⛔ নোটিফিকেশনের নিয়ম · সংখ্যা · চুপ থাকার শর্ত — কিচ্ছু বদলায়নি।
                        val firstTick = !primed
                        primed = true
                        val changed = try {
                            boardWatch.changed("chamberclose|$branch|$today", branch)
                        } catch (_: Throwable) { true }
                        val board = (
                            if (!firstTick && !changed)
                                ChamberAttendanceRepository.loadCachedBoard(ctx, today, branch)
                            else null
                        ) ?: ChamberAttendanceRepository.loadBoard(today, branch, ctx)
                        val arrived = board.rows.count { it.arrived }
                        val money = board.rows.any {
                            it.paymentCash > 0 || it.paymentOnline > 0 ||
                                it.feesCash > 0 || it.feesOnline > 0
                        }
                        // TK: no patient and no money -> no sound at all.
                        if (arrived > 0 || money) notify(ctx, arrived)
                    }
                }
            }
        } catch (_: Exception) {
            // a reminder must never crash anything
        } finally {
            ChamberCloseReminderScheduler.scheduleNext(applicationContext)
        }
        return Result.success()
    }

    private fun notify(ctx: Context, arrived: Int) {
        try {
            val nm = ctx.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            // 🔊 খাতার সারি B28 — একই কারণ, একই সমাধান (NoticeChannels দেখুন)।
            val channel = NoticeChannels.ensure(
                ctx, CHANNEL_ID, "Chamber Not Closed", "Reminder to close today's chamber"
            )
            val open = Intent(ctx, ChamberAttendanceActivity::class.java)
                .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            val flags = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M)
                android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE
            else android.app.PendingIntent.FLAG_UPDATE_CURRENT
            val pi = android.app.PendingIntent.getActivity(ctx, 2, open, flags)

            val n = NotificationCompat.Builder(ctx, channel)
                // 🎨 TK-APPROVED (2026-08-06): clean bell icon + brand accent + BigText.
                .setSmallIcon(R.drawable.ic_notif_bell)
                .setColor(android.graphics.Color.parseColor("#0B3B73"))
                .setContentTitle("📋 Chamber not closed yet")
                .setContentText("$arrived arrived today — tap to open Chamber Date and close it.")
                .setStyle(NotificationCompat.BigTextStyle().bigText("$arrived arrived today — tap to open Chamber Date and close it."))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setDefaults(NotificationCompat.DEFAULT_SOUND or NotificationCompat.DEFAULT_VIBRATE)
                .setAutoCancel(true)
                .setContentIntent(pi)
                .build()
            nm.notify(NOTIF_ID, n)
        } catch (_: Exception) {}
    }

    companion object {
        const val CHANNEL_ID = "chamber_not_closed"
        const val NOTIF_ID = 4203

        /**
         * 🟢🔒 B663 (15.08.2026, TK-অনুমোদিত · Egress-৫)
         * বোর্ডের চারটে টেবিলের পাহারাদার — প্রতি টিকে শুধু একটা ছোট সংখ্যা আনে
         * (HEAD count-only, একটাও সারি নামে না)। ⛔ কিছু লেখে না, বদলায় না।
         */
        private val boardWatch = LiveRefresh.Watch("payments", "enquiries", "patients", "followups")

        /** এই প্রক্রিয়ায় অন্তত একবার সত্যিকারের বোর্ড নামানো হয়েছে কি না। */
        private var primed = false
    }
}
