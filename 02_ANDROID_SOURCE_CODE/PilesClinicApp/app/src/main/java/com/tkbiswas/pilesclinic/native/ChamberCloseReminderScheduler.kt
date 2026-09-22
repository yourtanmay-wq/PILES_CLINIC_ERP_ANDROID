package com.tkbiswas.pilesclinic.native

import android.content.Context
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import java.util.Calendar
import java.util.concurrent.TimeUnit

/**
 * TK'S RULE (2026-07-27, locked): from 7 PM, every 10 minutes, until midnight,
 * a staff whose branch chamber is still not closed gets a reminder. After
 * midnight it stops for the day and the chain restarts at 7 PM the next day.
 *
 * Same one-time-chained WorkManager pattern the call reminder already uses, so
 * no exact-alarm permission is needed.
 */
object ChamberCloseReminderScheduler {

    private const val WORK_NAME = "piles_clinic_chamber_close_reminder"
    private const val START_HOUR = 19      // 7 PM
    private const val STOP_HOUR = 24       // midnight
    private const val STEP_MINUTES = 10

    fun scheduleNext(context: Context) {
        try {
            val request = OneTimeWorkRequestBuilder<ChamberCloseReminderWorker>()
                .setInitialDelay(millisUntilNextRun(), TimeUnit.MILLISECONDS)
                .build()
            WorkManager.getInstance(context).enqueueUniqueWork(
                WORK_NAME, ExistingWorkPolicy.REPLACE, request
            )
        } catch (_: Throwable) {
            // scheduling must never crash the app
        }
    }

    private fun millisUntilNextRun(): Long {
        val now = Calendar.getInstance()
        val hour = now.get(Calendar.HOUR_OF_DAY)
        if (hour < START_HOUR) {
            val start = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, START_HOUR)
                set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
            }
            return start.timeInMillis - now.timeInMillis
        }
        if (hour < STOP_HOUR) {
            val next = Calendar.getInstance().apply {
                add(Calendar.MINUTE, STEP_MINUTES)
                set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
            }
            return (next.timeInMillis - now.timeInMillis).coerceAtLeast(60_000L)
        }
        val tomorrow = Calendar.getInstance().apply {
            add(Calendar.DAY_OF_YEAR, 1)
            set(Calendar.HOUR_OF_DAY, START_HOUR)
            set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
        }
        return tomorrow.timeInMillis - now.timeInMillis
    }
}
