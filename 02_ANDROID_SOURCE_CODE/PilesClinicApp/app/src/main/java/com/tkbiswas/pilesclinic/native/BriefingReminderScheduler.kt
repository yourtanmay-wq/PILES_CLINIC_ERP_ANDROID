package com.tkbiswas.pilesclinic.native

import android.content.Context
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import java.util.Calendar
import java.util.concurrent.TimeUnit

/**
 * 🆕 B467 (05.08.2026, TK-নির্দেশ) — "ব্রিফিং যখন আসবে, নোটিফিকেশনের ঘন্টায়
 * স্টাফের ফোনে নোটিফিকেশন বাঁচতে হবে জোর করে... দশ মিনিট পর পর... আমি এটা
 * পরে করব বললে, সেই টাইমে আবার নোটিফিকেশন বাজবে।"
 *
 * `AttendanceReminderScheduler.kt`-এর হুবহু একই প্রমাণিত WorkManager-chain
 * প্যাটার্ন (one-time job নিজেই পরের ধাপের জন্য আবার শিডিউল করে, exact-alarm
 * অনুমতি লাগে না) — শুধু "IN/OUT TIME মার্ক হয়েছে কিনা" এর বদলে "অপঠিত
 * Briefing আছে কিনা" দেখে। ⛔ `AttendanceReminderScheduler`/Worker-এর
 * ভিতরে এক অক্ষরও হাত দেওয়া হয়নি — এটা সম্পূর্ণ আলাদা, স্বাধীন চেইন।
 */
object BriefingReminderScheduler {

    private const val REPEAT_GAP_MINUTES = 10L

    /** অ্যাপ চালু হওয়ার সময় (Application.onCreate) ডাকা হয় — অপঠিত থাকলে
     *  ১০ মিনিট পরে প্রথম চেক শুরু হবে (Worker নিজেই দেখে নেয় সত্যিই
     *  অপঠিত আছে কিনা, তাই এখানে আলাদা কোনো ক্লাউড-কল লাগে না)। */
    fun start(context: Context) {
        enqueue(context, TimeUnit.MINUTES.toMillis(REPEAT_GAP_MINUTES))
    }

    /** Worker নিজে আবার ডাকে — অপঠিত এখনো থাকলে চেইন চলতেই থাকে। */
    fun scheduleRepeat(context: Context) {
        enqueue(context, TimeUnit.MINUTES.toMillis(REPEAT_GAP_MINUTES))
    }

    /** "⏰ আমি এটা পরে করব" — স্টাফ নিজে ঘড়িতে যে সময় বাছেন, ঠিক সেই সময়েই
     *  পরের রিমাইন্ডার বসে (আজকের সময় পার হয়ে থাকলে কালকের একই সময়ে)। */
    fun scheduleExactTime(context: Context, hour: Int, minute: Int) {
        val now = Calendar.getInstance()
        val target = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour); set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
        }
        if (target.timeInMillis <= now.timeInMillis) target.add(Calendar.DAY_OF_YEAR, 1)
        enqueue(context, target.timeInMillis - now.timeInMillis)
    }

    private fun enqueue(context: Context, delayMs: Long) {
        val request = OneTimeWorkRequestBuilder<BriefingReminderWorker>()
            .setInitialDelay(delayMs, TimeUnit.MILLISECONDS)
            .build()
        WorkManager.getInstance(context).enqueueUniqueWork(
            "piles_clinic_briefing_reminder",
            ExistingWorkPolicy.REPLACE,
            request
        )
    }
}
