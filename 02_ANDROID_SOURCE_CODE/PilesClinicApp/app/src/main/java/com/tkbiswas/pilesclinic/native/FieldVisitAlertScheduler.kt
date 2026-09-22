package com.tkbiswas.pilesclinic.native

import android.content.Context
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import java.util.Calendar
import java.util.concurrent.TimeUnit

/**
 * 🛰️🔒 V1345 (১১.০৯.২০২৬, TK-নির্দেশ — "মাস্টার-অ্যালার্ট যোগ করুন") —
 * `FieldVisitAlertWorker`-এর বসানোর সময়সূচি।
 *
 * TK-র নিজের কথা: ব্যাটারি-সেভার/অনুমতির কারণে কোনো ফিল্ড-স্টাফের লোকেশন
 * কাজ না করলে তিনি এতদিন শুধু রাতে/পরে ধরতে পারতেন, ততক্ষণে ওই দিনটা চলে
 * যেত।
 *
 * 🏍️🔒 V1471 (১৪.০৯.২০২৬, TK: "RUPAM ৩০ কিমি ঘুরেছে, অ্যাপ ০.১ দেখাচ্ছে,
 * গভীরে যাচাই করুন") — আজকের ঘটনায় দেখা গেল RUPAM-এর সার্ভিস দুপুর ৩.২৬-এ
 * থেমে গিয়েছিল, কিন্তু **দিনে একবারই (দুপুর ৩টায়)** এই চেক চলত বলে TK
 * সেটা টেরই পাননি যতক্ষণ না নিজে সন্ধ্যায় পর্দাটা খুলে দেখেছেন — ততক্ষণে
 * বাকি দিনের চলাচল হারিয়ে গেছে। এখন **প্রতি ৪৫ মিনিটে** (কাজের সময়, সকাল
 * ৯টা–রাত ৯টা) আবার চেক হয়, তাই ফাঁক ধরা পড়লে সেই দিনেই (না রাতে/পরে)
 * TK ফোন করে সতর্ক করতে পারবেন। কাজের সময়ের বাইরে (রাত ৯টা–সকাল ৯টা)
 * বৃথা চেক বন্ধ, পরদিন সকাল ৯টায় আবার শুরু।
 * ⛔ `MasterOutTimeScheduler`/`MasterOutTimeWorker`-এ এতটুকুও হাত পড়েনি —
 *    এটা সম্পূর্ণ আলাদা, স্বাধীন চেইন।
 */
object FieldVisitAlertScheduler {

    private const val WORK_NAME = "field_visit_location_alert"
    private const val WORK_START_HOUR = 9    // সকাল ৯টা
    private const val WORK_END_HOUR = 21     // রাত ৯টা
    private const val REPEAT_MINUTES = 45L   // FieldVisitAlertWorker-এর নিজের ৪৫-মিনিট গ্যাপ-চেকের সাথে মেলানো

    fun scheduleNext(context: Context) {
        try {
            val now = Calendar.getInstance()
            val hour = now.get(Calendar.HOUR_OF_DAY)
            val at: Calendar
            if (hour in WORK_START_HOUR until WORK_END_HOUR) {
                // কাজের সময়ের ভিতরে — ৪৫ মিনিট পরেই আবার চেক।
                at = Calendar.getInstance().apply { add(Calendar.MINUTE, REPEAT_MINUTES.toInt()) }
            } else {
                // কাজের সময়ের বাইরে — পরের সকাল ৯টায় প্রথম চেক।
                at = Calendar.getInstance().apply {
                    set(Calendar.HOUR_OF_DAY, WORK_START_HOUR)
                    set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
                }
                if (!at.after(now)) at.add(Calendar.DAY_OF_YEAR, 1)
            }
            val delay = (at.timeInMillis - now.timeInMillis).coerceAtLeast(60_000L)
            val req = OneTimeWorkRequestBuilder<FieldVisitAlertWorker>()
                .setInitialDelay(delay, TimeUnit.MILLISECONDS)
                .build()
            WorkManager.getInstance(context)
                .enqueueUniqueWork(WORK_NAME, ExistingWorkPolicy.REPLACE, req)
        } catch (_: Throwable) { }
    }
}
