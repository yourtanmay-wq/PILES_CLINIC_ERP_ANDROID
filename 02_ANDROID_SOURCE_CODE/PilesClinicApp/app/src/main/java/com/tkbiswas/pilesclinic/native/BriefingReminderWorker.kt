package com.tkbiswas.pilesclinic.native

import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.tkbiswas.pilesclinic.R

/**
 * 🆕 B467 (05.08.2026, TK-নির্দেশ) — `BriefingReminderScheduler.kt`-এর
 * দলিল দেখুন। ⚠️ সৎ সীমাবদ্ধতা (প্রজেক্টের বাকি সব ব্যাকগ্রাউন্ড
 * রিমাইন্ডারের মতোই): কিছু ফোনের battery-optimization ব্যাকগ্রাউন্ড কাজ
 * দেরি/বাদ দিতে পারে — এটা best-effort, ১০০% গ্যারান্টিড alarm না।
 */
class BriefingReminderWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val ctx = applicationContext
        try {
            // 🔵 V405 (16.08.2026, TK-অনুমোদিত — Egress): অপঠিত নোটিশ থাকলে এই
            //    কাজটা **প্রতি ১০ মিনিটে** পুরো `briefings` টেবিল নামাত — রাতেও।
            //    রাত ১০টা–সকাল ৬টায় কেউ নোটিশ পড়েন না, তবু ৮ ঘণ্টায় ~৪৮ বার
            //    প্রতিটা ফোনে ডাউনলোড হত।
            //    এখন ওই সময়টা ঘুমায় — প্রজেক্টের অন্য ব্যাকগ্রাউন্ড কাজের
            //    (B662) হুবহু একই "রাত ১০–৬" নিয়ম।
            //    ⛔ চেইন ভাঙে না — পরের দফা আগের মতোই বসানো হয়, তাই সকালের
            //       প্রথম দফাতেই আবার গোনা ও নোটিশ হবে; কোনো নোটিশ হারায় না।
            val hourIst = java.util.Calendar.getInstance(
                java.util.TimeZone.getTimeZone("Asia/Kolkata")
            ).get(java.util.Calendar.HOUR_OF_DAY)
            if (hourIst >= 22 || hourIst < 6) {
                BriefingReminderScheduler.scheduleRepeat(ctx)
                return Result.success()
            }
            val user = NativeSession.current(ctx)
            if (user != null) {
                val repo = BriefingRepository()
                // 🔴 V433 (TK, ১৮.০৮.২০২৬) — "Staff IN TIME" ধরনের শুধু-তথ্য
                // নোটিশে ১০ মিনিট পর পর অ্যালার্ম বাজত। এখন সেগুলো বাদ দিয়ে
                // গোনা হয়। ⛔ ঘন্টার সংখ্যা (unseenCount) আগের মতোই অপরিবর্তিত।
                val count = repo.unseenCountForReminder(repo.fetchRawForCount(ctx), user)   // 🔵 V405: শুধু গোনা ⇒ সরু পড়া
                if (count > 0) notify(ctx, count)
            }
        } catch (_: Throwable) {
            // worker কখনো crash করবে না
        }
        /* 🔴🔒 V1510 (TK-রিপোর্ট, ১৬.০৯.২০২৬ — "সাউন্ড কেন হলো না") — আগে
           `count > 0` হলে তবেই পরের ধাপ বসানো হত। ফলে একবার সব notice "Seen"
           হয়ে count শূন্যে নামলে **পুরো চেইনটাই চিরকালের জন্য থেমে যেত** —
           অ্যাপ আবার পুরোপুরি বন্ধ করে খোলা (Application.onCreate) না হওয়া
           পর্যন্ত কোনো নতুন Leave request/Refund/ইত্যাদি নোটিসে সাউন্ড আসত না,
           চুপচাপ শুধু Notice Board-এ বসে থাকত। TK-র ফোনে ঠিক এটাই ঘটেছিল।
           MasterOutTimeWorker-এর প্রমাণিত নিয়মের মতোই — ফল যাই হোক, পরের
           দফা সবসময় বসানো হচ্ছে, যাতে চেইন কখনো নিজে থেকে না থামে। */
        try { BriefingReminderScheduler.scheduleRepeat(ctx) } catch (_: Throwable) { }
        return Result.success()
    }

    private fun notify(ctx: Context, count: Int) {
        val nm = ctx.getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
        val channel = NoticeChannels.ensure(
            ctx, CHANNEL_ID, "Briefing Reminder", "Reminds you about unread Briefing/Notice messages"
        )
        val open = Intent(ctx, BriefingActivity::class.java)
            .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            // 🔴 BriefingActivity-তে এই extra দেখে ছোট "Seen it now / Remind
            // me later" প্রশ্ন দেখানো হয় (IN/OUT TIME-এর quick_mark-এর
            // হুবহু একই প্যাটার্ন)।
            .putExtra("quick_reminder", true)
        val flags = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M)
            android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE
        else android.app.PendingIntent.FLAG_UPDATE_CURRENT
        val pi = android.app.PendingIntent.getActivity(ctx, NOTIF_ID, open, flags)

        val text = if (count == 1) "You have 1 unread notice — tap to open"
            else "You have $count unread notices — tap to open"
        val n = NotificationCompat.Builder(ctx, channel)
            // 🎨 TK-APPROVED (2026-08-06): professional look — a clean white bell
            // notification icon + brand accent colour + expandable text (was the
            // plain launcher square with default styling).
            .setSmallIcon(R.drawable.ic_notif_bell)
            .setColor(android.graphics.Color.parseColor("#0B3B73"))
            .setContentTitle("🔔 Unread Briefing / Notice")
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pi)
            .build()
        nm.notify(NOTIF_ID, n)
    }

    companion object {
        const val CHANNEL_ID = "briefing_reminder"
        // ⛔ প্রজেক্টের বাকি নোটিফিকেশন ID-র সাথে সংঘর্ষ এড়াতে (4202/4203/
        // 4205/4206 আগে থেকেই ব্যবহৃত — AttendanceReminderWorker.kt-এর
        // কমেন্ট দেখুন) — নতুন, অব্যবহৃত ID।
        const val NOTIF_ID = 4207
    }
}
