package com.tkbiswas.pilesclinic.native

import android.content.Context
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import java.util.Calendar
import java.util.concurrent.TimeUnit

/**
 * Schedules the daily call-reminder at 11 AM, 2 PM and 4 PM. Uses a one-time
 * WorkManager job whose delay is the time until the next of those slots; the
 * worker re-schedules the following slot after it runs, so the chain keeps going
 * without needing exact-alarm permissions.
 *
 * 🔴🟢 খাতার সারি B429 (TK-নির্দেশ, 05.08.2026): সময় ১০টা/১২টা/২টা থেকে
 * বদলে ১১টা/২টা/৪টা করা হলো। ⛔ চেইন-লজিক/attempt-সংখ্যা/কিছুই বদলায়নি,
 * শুধু তিনটে ঘণ্টা।
 */
object CallReminderScheduler {

    private const val WORK_NAME = "piles_clinic_call_reminder"
    // Reminder hours (24h): 11:00, 14:00, 16:00
    private val SLOT_HOURS = intArrayOf(11, 14, 16)

    fun scheduleNext(context: Context) {
        val delay = millisUntilNextSlot()
        val request = OneTimeWorkRequestBuilder<CallReminderWorker>()
            .setInitialDelay(delay, TimeUnit.MILLISECONDS)
            .build()
        WorkManager.getInstance(context).enqueueUniqueWork(
            WORK_NAME,
            ExistingWorkPolicy.REPLACE,
            request
        )
    }

    private fun millisUntilNextSlot(): Long {
        val now = Calendar.getInstance()
        for (h in SLOT_HOURS) {
            val slot = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, h)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            if (slot.timeInMillis > now.timeInMillis) return slot.timeInMillis - now.timeInMillis
        }
        // All of today's slots have passed — first slot tomorrow.
        val tomorrow = Calendar.getInstance().apply {
            add(Calendar.DAY_OF_YEAR, 1)
            set(Calendar.HOUR_OF_DAY, SLOT_HOURS.first())
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        return tomorrow.timeInMillis - now.timeInMillis
    }
}
