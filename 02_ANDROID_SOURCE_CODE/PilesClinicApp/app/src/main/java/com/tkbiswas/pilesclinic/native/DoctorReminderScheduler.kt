package com.tkbiswas.pilesclinic.native

import android.content.Context
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

/**
 * 🟢🔒🔒 V656 (২৫.০৮.২০২৬) → 🔴🔒 V671 (২৫.০৮.২০২৬, TK-নির্দেশ) —
 * "Doctor Note & Reminder"।
 *
 * `BriefingReminderScheduler`-এর হুবহু একই প্রমাণিত WorkManager
 * one-time-chain প্যাটার্ন — এখন প্রতি ১৫ মিনিটে (আগে দিনে একবার, ৫টায়)।
 * এতে ডাক্তারের বাছা যেকোনো সময়ের কাছাকাছি (১৫ মিনিটের জানালায়)
 * নোটিফিকেশন বাজে — শুধু একটা ফিক্সড স্লট নয়।
 */
object DoctorReminderScheduler {

    private const val WORK_NAME = "piles_clinic_doctor_reminder"

    /** ⏰🔒 V1241 — অ্যালার্ম বাজলে **সঙ্গে সঙ্গে** একই কাজটা চালায় (অপেক্ষা নয়)।
     *  ⛔ কাজটার ভিতরের একটাও নিয়ম বদলায়নি — শুধু কখন চলবে সেটা বদলাল। */
    fun runNow(context: Context) {
        /* ⏰🔒 V1277 (০৯.০৯.২০২৬, TK-রিপোর্ট — তালিকা সারি ৪০০) — অ্যালার্ম
           ঠিক সময়ে বাজলেও কাজটা **সাধারণ সারিতে** যেত, আর ফোন গভীর ঘুমে
           (Doze) থাকলে WorkManager সেটা পিছিয়ে দিতে পারত ⇒ ঘণ্টা দেরিতে।
           ⇒ এখন **expedited** — Doze-এও সঙ্গে সঙ্গে চলে।
           ⛔ কোটা ফুরোলে নিজে থেকেই আগের সাধারণ নিয়মে নামে
              (`RUN_AS_NON_EXPEDITED_WORK_REQUEST`), তাই কখনো ব্যর্থ হয় না।
           ⛔ কাজটার ভিতরের একটাও নিয়ম বদলায়নি। */
        val request = OneTimeWorkRequestBuilder<DoctorReminderWorker>()
            .setExpedited(androidx.work.OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
            .build()
        WorkManager.getInstance(context).enqueueUniqueWork(
            WORK_NAME + "_now",
            ExistingWorkPolicy.REPLACE,
            request
        )
    }

    fun scheduleNext(context: Context) {
        val delay = TimeUnit.MINUTES.toMillis(DoctorReminderWorker.REPEAT_GAP_MINUTES.toLong())
        val request = OneTimeWorkRequestBuilder<DoctorReminderWorker>()
            .setInitialDelay(delay, TimeUnit.MILLISECONDS)
            .build()
        WorkManager.getInstance(context).enqueueUniqueWork(
            WORK_NAME,
            ExistingWorkPolicy.REPLACE,
            request
        )
    }
}
