package com.tkbiswas.pilesclinic.native

import android.content.Context
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import java.util.Calendar
import java.util.concurrent.TimeUnit

/**
 * 🔴🔴🔒 V511 (২১.০৮.২০২৬, TK-নির্দেশ) — `MasterOutTimeWorker`-কে **রাত ৯টায়**
 * বসানোর সময়সূচি।
 *
 * ⛔ `AttendanceReminderScheduler`-এর হুবহু একই প্রমাণিত ধাঁচ — একবারের কাজ
 *    (OneTimeWork), চলার পরে নিজেই পরেরটা বসায়। কোনো নতুন অনুমতি লাগে না।
 * ⛔ `ExistingWorkPolicy.REPLACE` — অ্যাপ বারবার খুললেও একটাই কাজ থাকে,
 *    কখনো দুটো নোটিফিকেশন হয় না।
 * ⛔ এখন ৯টা পার হয়ে গেলে **আজকের জন্য আর নয়** — সোজা কালকের ৯টায়।
 */
object MasterOutTimeScheduler {

    private const val WORK_NAME = "master_out_time_missing"
    private const val HOUR = 21   // রাত ৯টা

    /** পরের ৯টা কখন, ততক্ষণের অপেক্ষা বসিয়ে দেয়। */
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
            val delay = at.timeInMillis - now.timeInMillis
            val req = OneTimeWorkRequestBuilder<MasterOutTimeWorker>()
                .setInitialDelay(delay, TimeUnit.MILLISECONDS)
                .build()
            WorkManager.getInstance(context)
                .enqueueUniqueWork(WORK_NAME, ExistingWorkPolicy.REPLACE, req)
        } catch (_: Throwable) { }
    }
}
