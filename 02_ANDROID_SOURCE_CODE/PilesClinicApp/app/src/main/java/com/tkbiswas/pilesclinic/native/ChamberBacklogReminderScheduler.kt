package com.tkbiswas.pilesclinic.native

import android.content.Context
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import java.util.Calendar
import java.util.concurrent.TimeUnit

/**
 * 🔔🔒 V1437 (১৩.০৯.২০২৬, TK-নির্দেশ · সমস্যা-তালিকা সারি ৫৬১) —
 * `ChamberBacklogReminderWorker`-কে **রোজ বেলা ১১টায়** বসানোর সময়সূচি।
 *
 * TK-এর কথা (ছবিসহ, জলপাইগুড়ি — "not closed yet — 12"):
 *   *"চেম্বার যে আজ ও বন্ধ করে নাই, staff-দের কি Notification আসে না"*
 *
 * কোডে মেপে পাওয়া ফাঁক: তাগাদা ছিল, কিন্তু **শুধু ওই দিনের সন্ধে ৭টা–রাত
 * ১২টা** (`ChamberCloseReminderScheduler`)। রাত ১২টা পেরোলেই ওই দিনটা চিরতরে
 * চুপ হয়ে যেত — **পুরনো বাকি দিন নিয়ে স্টাফের ফোনে কখনো কিছু আসত না**, তাই
 * ১২ দিন জমেছিল।
 *
 * ⛔ `MasterOutTimeScheduler`-এর হুবহু একই প্রমাণিত ধাঁচ — একবারের কাজ
 *    (OneTimeWork), চলার পরে নিজেই পরেরটা বসায়। নতুন কোনো অনুমতি লাগে না।
 * ⛔ `ExistingWorkPolicy.REPLACE` — অ্যাপ বারবার খুললেও একটাই কাজ থাকে,
 *    কখনো দুটো নোটিফিকেশন হয় না।
 * ⛔ সন্ধের পুরনো ৭টা–১২টার চেইন (আজকের দিনের জন্য) **একটুও ছোঁয়া হয়নি** —
 *    এটা সম্পূর্ণ আলাদা, স্বাধীন চেইন, আলাদা কাজের নাম ও আলাদা নোটিফিকেশন।
 */
object ChamberBacklogReminderScheduler {

    private const val WORK_NAME = "piles_clinic_chamber_backlog_reminder"

    /** বেলা ১১টা — স্টাফ তখন চেম্বারেই, পুরনো দিনটা তখনই বন্ধ করে ফেলতে পারেন। */
    private const val HOUR = 11

    /** পরের ১১টা কখন, ততক্ষণের অপেক্ষা বসিয়ে দেয়। */
    fun scheduleNext(context: Context) {
        try {
            val now = Calendar.getInstance()
            val at = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, HOUR)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            if (!at.after(now)) at.add(Calendar.DAY_OF_YEAR, 1)
            val req = OneTimeWorkRequestBuilder<ChamberBacklogReminderWorker>()
                .setInitialDelay(at.timeInMillis - now.timeInMillis, TimeUnit.MILLISECONDS)
                .build()
            WorkManager.getInstance(context)
                .enqueueUniqueWork(WORK_NAME, ExistingWorkPolicy.REPLACE, req)
        } catch (_: Throwable) {
            // সময়সূচি বসাতে ভুল হলেও অ্যাপ কখনো আটকাবে না
        }
    }
}
