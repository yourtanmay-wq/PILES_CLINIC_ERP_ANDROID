package com.tkbiswas.pilesclinic.native

import android.content.Context
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import java.util.Calendar
import java.util.concurrent.TimeUnit

/**
 * B325/B326 (03.08.2026, TK-নির্দেশ, ধাপে ধাপে স্পষ্ট করা হয়েছে):
 *   - IN TIME মার্ক না থাকলে সকাল ১০টা থেকে নোটিফিকেশন।
 *   - OUT TIME মার্ক না থাকলে সন্ধ্যা ৬টা থেকে নোটিফিকেশন।
 *   - দুটোতেই: প্রতি ১০ মিনিটে, মোট ৩ বার (TK নিজে বেছেছেন)।
 *
 * CallReminderScheduler.kt-এর প্রমাণিত WorkManager-chain প্যাটার্নে ভিত্তি করে —
 * একটা one-time job নিজেই পরের ধাপের জন্য আবার শিডিউল করে, তাই exact-alarm
 * অনুমতি লাগে না। "kind" ("in"/"out") ও "attempt" ছোট Data payload দিয়ে
 * worker-এর কাছে যায়।
 */
object AttendanceReminderScheduler {

    const val KIND_IN = "in"
    const val KIND_OUT = "out"
    const val MAX_ATTEMPTS = 3
    private const val REPEAT_GAP_MINUTES = 10L
    /* ⏰🔒 V1166 (০৭.০৯.২০২৬, TK-নির্দেশ) — OUT TIME মনে করানো **৬টা → ৭.৩০ PM**।
       TK: *"রাত ৯টা অবদি কেউ কাজ করে না; বিগত ২ বছরে সর্বোচ্চ ৭.৩০ PM"* ⇒ যখন
       সবাই সত্যিই বেরোনোর মুখে, তখনই মনে করানো — আগে বাজলে অকারণ বিরক্তি।
       ⛔ IN TIME-এর ১০টা এক অক্ষরও বদলায়নি। */
    private val START_HOUR = mapOf(KIND_IN to 10, KIND_OUT to 19)   // 10 AM, 7.30 PM
    private val START_MINUTE = mapOf(KIND_IN to 0, KIND_OUT to 30)

    // 🔴 B403 (04.08.2026, TK-রিপোর্ট, Laxmi/Kishanganj — "সঠিক সময়ে এসেও
    // নোটিফিকেশন আসেনি"): আসল কারণ ধরা পড়েছে — আগে `scheduleFreshDay()`
    // সবসময় `scheduleTomorrowFirstAttempt()` ডাকত, যেটা `millisUntilNextHour()`-এ
    // "আজকের স্লট পার হয়ে গেলে কালকে বসাও" নিয়ম মেনে চলত। এই ফাংশনটা App
    // চালু হওয়ার প্রতিবার (Application.onCreate) চলে — শুধু মাঝরাতে না।
    // তাই স্টাফ যদি সকাল ১০টার **পরে** কোনো এক মুহূর্তে অ্যাপ খোলেন (Android
    // প্রায়ই অ্যাপ মেরে আবার চালু করে) আর তখনও IN TIME মার্ক না করে থাকেন,
    // এই ফাংশন ভুল করে **কালকের** ১০টায় রিমাইন্ডার বসিয়ে দিত — আজকের
    // রিমাইন্ডারটাই কখনো আসত না। এখন — আজকের ফ্ল্যাগ (মার্ক হয়েছে কিনা)
    // দেখে, না-মার্ক-করা অবস্থায় স্লট পার হয়ে গেলে **শীঘ্রই** (১ মিনিটে)
    // রিমাইন্ডার বসে, কালকে পর্যন্ত অপেক্ষা করানো হয় না। ⛔ ইতিমধ্যে
    // মার্ক করা থাকলে বা এখনো স্লট না এলে — আচরণ আগের মতোই।
    fun scheduleFreshDay(context: Context) {
        scheduleForToday(context, KIND_IN)
        scheduleForToday(context, KIND_OUT)
    }

    private fun todayIso(): String {
        val f = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US)
        f.timeZone = java.util.TimeZone.getTimeZone("Asia/Kolkata")
        return f.format(java.util.Date())
    }

    private fun scheduleForToday(context: Context, kind: String) {
        val startHour = START_HOUR[kind] ?: return
        val prefKey = if (kind == KIND_IN) "checkin_or_leave_date" else "checkout_or_leave_date"
        val prefs = context.getSharedPreferences("wn_prefs", Context.MODE_PRIVATE)
        val alreadyMarkedToday = prefs.getString(prefKey, "") == todayIso()
        if (alreadyMarkedToday) {
            // আজ ইতিমধ্যেই মার্ক করা — আগের নিয়মেই কালকের স্লটে বসুক
            enqueue(context, kind, 1, millisUntilNextHour(startHour, START_MINUTE[kind] ?: 0))
            return
        }
        val now = Calendar.getInstance()
        val slot = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, startHour); set(Calendar.MINUTE, START_MINUTE[kind] ?: 0)
            set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
        }
        val delay = if (slot.timeInMillis > now.timeInMillis) slot.timeInMillis - now.timeInMillis
            else TimeUnit.MINUTES.toMillis(1) // স্লট আজ পার হয়ে গেছে, এখনো মার্ক হয়নি — শীঘ্রই মনে করানো হবে
        enqueue(context, kind, 1, delay)
    }

    fun scheduleRepeat(context: Context, kind: String, nextAttempt: Int) {
        enqueue(context, kind, nextAttempt, TimeUnit.MINUTES.toMillis(REPEAT_GAP_MINUTES))
    }

    fun scheduleTomorrowFirstAttempt(context: Context, kind: String) {
        val startHour = START_HOUR[kind] ?: return
        enqueue(context, kind, 1, millisUntilNextHour(startHour, START_MINUTE[kind] ?: 0))
    }

    // 🔴🆕🔒 B438 (05.08.2026, TK-নির্দেশ — "নির্ধারিত সময়ে মনে করানোর
    // ব্যবস্থা রাখুন, শুধু ২ ঘণ্টা বলে বসিয়ে রাখবেন না") — স্টাফ নিজে
    // ঘড়িতে যে সময় বেছেছেন (TimePickerDialog), ঠিক সেই সময়েই পরের
    // রিমাইন্ডার বসে। আজকের সময় পার হয়ে থাকলে (যেমন রাত ১১টায় সকাল ৯টা
    // বাছলে) কালকের একই সময়ে বসে। attempt আবার ১ থেকে শুরু হয় — এই বাছা
    // সময়ে না-মার্ক-করা থাকলে তারপর আগের ১০-মিনিট-চেইন যথারীতি চলবে।
    fun scheduleExactTime(context: Context, kind: String, hour: Int, minute: Int) {
        val now = Calendar.getInstance()
        val target = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour); set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
        }
        if (target.timeInMillis <= now.timeInMillis) target.add(Calendar.DAY_OF_YEAR, 1)
        enqueue(context, kind, 1, target.timeInMillis - now.timeInMillis)
    }

    private fun enqueue(context: Context, kind: String, attempt: Int, delayMs: Long) {
        val data = Data.Builder().putString("kind", kind).putInt("attempt", attempt).build()
        val request = OneTimeWorkRequestBuilder<AttendanceReminderWorker>()
            .setInitialDelay(delayMs, TimeUnit.MILLISECONDS)
            .setInputData(data)
            .build()
        WorkManager.getInstance(context).enqueueUniqueWork(
            "piles_clinic_attendance_reminder_$kind",
            ExistingWorkPolicy.REPLACE,
            request
        )
    }

    private fun millisUntilNextHour(hour: Int, minute: Int = 0): Long {
        val now = Calendar.getInstance()
        val slot = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        if (slot.timeInMillis <= now.timeInMillis) slot.add(Calendar.DAY_OF_YEAR, 1)
        return slot.timeInMillis - now.timeInMillis
    }
}
