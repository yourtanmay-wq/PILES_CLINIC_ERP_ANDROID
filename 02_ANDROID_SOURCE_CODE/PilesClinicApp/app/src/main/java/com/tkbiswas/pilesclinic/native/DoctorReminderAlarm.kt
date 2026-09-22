package com.tkbiswas.pilesclinic.native

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import java.util.Calendar

/**
 * ⏰🔒 V1241 (০৮.০৯.২০২৬, TK-নির্দেশ হুবহু — Dr. K.H MANDAL-এর তিন-চারবারের
 * অভিযোগের পরে): *"জোর করে আসতে হবে — যে টাইম সেট করবে সেই টাইমে সেই
 * ডাক্তারবাবুকে জোর করে নোটিফিকেশন দিতে হবে।"*
 *
 * **কেন দরকার ছিল:** এতদিন ঘণ্টা বাজাত শুধু WorkManager-এর প্রতি-১৫-মিনিটের
 * কাজ। ওটা ফোনের ইচ্ছেমতো পিছিয়ে যায় (Doze · ব্যাটারি সাশ্রয়), তাই বাছা
 * সময়ের **অনেক পরে** বাজত — বা ফোন গভীর ঘুমে থাকলে বাজতই না।
 *
 * **এখন:** বাছা সময়ের জন্য ফোনের নিজের **অ্যালার্ম ঘড়ি** (`setAlarmClock`)
 * বসানো হয় — এটাই সবচেয়ে জোরালো, Doze-ও একে আটকাতে পারে না (ফোনের অ্যালার্ম
 * যেভাবে বাজে, ঠিক সেভাবে)।
 *
 * ⛔ পুরনো ১৫-মিনিটের কাজটা **অটুট রইল** — জাল হিসেবে (অ্যালার্ম কোনো কারণে
 *    বসাতে না পারলে/মুছে গেলে ওটাই ধরে নেয়)। তাই আগের কোনো ভালো কাজ নষ্ট হয়নি।
 * ⛔ একই রিমাইন্ডার দুবার বাজে না — বাজানোর হিসাব আগের মতোই
 *    `DoctorReminderWorker`-এর "আজ কাদের বাজানো হয়েছে" তালিকাতেই থাকে।
 * ⛔ ক্লাউডে বাড়তি একটাও পড়া নেই — যে সারিগুলো এমনিতেই পর্দায় আনা হয়,
 *    সেগুলো থেকেই সময় নেওয়া হয় (নিয়ম ১৩, ফ্রি প্ল্যান)।
 */
object DoctorReminderAlarm {

    private const val ACTION = "com.tkbiswas.pilesclinic.DOCTOR_REMINDER_ALARM"
    private const val REQ = 42410

    private fun alarmManager(ctx: Context): AlarmManager? = try {
        ctx.getSystemService(Context.ALARM_SERVICE) as? AlarmManager
    } catch (_: Throwable) { null }

    /** ফোনটা কি সত্যিই কাঁটায়-কাঁটায় অ্যালার্ম বসাতে দেবে? */
    private fun canExact(ctx: Context): Boolean = try {
        if (android.os.Build.VERSION.SDK_INT < 31) true
        else alarmManager(ctx)?.canScheduleExactAlarms() == true
    } catch (_: Throwable) { false }

    private fun pending(ctx: Context): PendingIntent {
        val i = Intent(ctx, DoctorReminderAlarmReceiver::class.java).setAction(ACTION)
        val f = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        return PendingIntent.getBroadcast(ctx, REQ, i, f)
    }

    /**
     * এই মুহূর্তের পরে **সবচেয়ে কাছের** যে রিমাইন্ডারের সময় আছে, তার জন্য
     * অ্যালার্ম বসায়। একবারে একটাই অ্যালার্ম রাখা হয় — সেটা বেজে গেলে
     * পরেরটা বসে (রিসিভারে)।
     * @param atMillis কখন বাজবে। অতীত হলে কিছুই করা হয় না।
     */
    fun scheduleAt(ctx: Context, atMillis: Long) {
        if (atMillis <= System.currentTimeMillis()) return
        val am = alarmManager(ctx) ?: return
        try {
            val pi = pending(ctx)
            if (canExact(ctx)) {
                // ফোনের অ্যালার্ম ঘড়ির মতোই — Doze একে আটকায় না।
                am.setAlarmClock(AlarmManager.AlarmClockInfo(atMillis, pi), pi)
            } else {
                // অনুমতি না থাকলে যতটা কাছাকাছি সম্ভব — তবু পুরনো ১৫-মিনিটের জালটা আছেই।
                am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, atMillis, pi)
            }
        } catch (_: Throwable) { }
    }

    /** "yyyy-MM-dd" + "HH:mm" (ভারতীয় সময়) → কত মিলিসেকেন্ডে। না পারলে 0। */
    fun millisOf(dateIso: String, timeHm: String): Long = try {
        val d = dateIso.trim().split("-").map { it.toInt() }
        val t = timeHm.trim().split(":").map { it.toInt() }
        val c = Calendar.getInstance(java.util.TimeZone.getTimeZone("Asia/Kolkata"))
        c.set(d[0], d[1] - 1, d[2], t[0], t[1], 0)
        c.set(Calendar.MILLISECOND, 0)
        c.timeInMillis
    } catch (_: Throwable) { 0L }
}

/**
 * ⏰ V1241 — অ্যালার্ম বাজলে যা হয়: ঠিক সেই মুহূর্তেই রিমাইন্ডারের কাজটা
 * চালানো হয় (যে কাজটা এমনিতেও প্রতি ১৫ মিনিটে চলে), তাই বাজানোর নিয়ম ·
 * লেখা · শব্দ · "দিনে একবার" — সবই **হুবহু আগের মতোই**, শুধু সময়টা কাঁটায়।
 */
class DoctorReminderAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        val ctx = context?.applicationContext ?: return
        try { DoctorReminderScheduler.runNow(ctx) } catch (_: Throwable) { }
    }
}
