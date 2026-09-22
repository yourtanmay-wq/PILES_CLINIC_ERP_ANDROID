package com.tkbiswas.pilesclinic.native

import android.content.Context
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import java.util.Calendar
import java.util.concurrent.TimeUnit

/**
 * NEW (2026-08-07, TK-approved "একদিন আগে আসার কথা" feature).
 *
 * Schedules ONE reminder per day, in the evening (5 PM), that tells the
 * branch staff who is expected at the chamber TOMORROW so they can phone and
 * remind them a day ahead. Uses the SAME proven WorkManager one-time-chain
 * pattern as CallReminderScheduler (no exact-alarm permission needed): the
 * worker re-schedules the next day's 5 PM slot after it runs.
 *
 * A single slot per day (not three) is deliberate — TK: "নোটিফিকেশনের ঘন্টা
 * যেন বারবার না আসে।" The worker also carries a date-flag so even if Android
 * happens to run it twice, it notifies only once.
 */
object ExpectedTomorrowReminderScheduler {

    private const val WORK_NAME = "piles_clinic_expected_tomorrow_reminder"
    // Single evening slot (24h). After the day's follow-up calls (11/14/16),
    // this is a quiet end-of-day "kal jara asbe" nudge.
    private const val SLOT_HOUR = 17

    fun scheduleNext(context: Context) {
        val delay = millisUntilNextSlot()
        val request = OneTimeWorkRequestBuilder<ExpectedTomorrowReminderWorker>()
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
        val slot = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, SLOT_HOUR)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        if (slot.timeInMillis > now.timeInMillis) return slot.timeInMillis - now.timeInMillis
        // Today's slot has passed — first slot tomorrow.
        val tomorrow = Calendar.getInstance().apply {
            add(Calendar.DAY_OF_YEAR, 1)
            set(Calendar.HOUR_OF_DAY, SLOT_HOUR)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        return tomorrow.timeInMillis - now.timeInMillis
    }
}
