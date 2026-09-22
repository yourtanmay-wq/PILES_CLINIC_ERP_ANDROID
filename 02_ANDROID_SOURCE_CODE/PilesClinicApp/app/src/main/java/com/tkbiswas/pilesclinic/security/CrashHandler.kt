package com.tkbiswas.pilesclinic.security

import android.content.Context
import android.util.Log
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Registered once from PilesClinicApplication. Does not try to "swallow" the
 * crash (that would risk running in a corrupted state) — it logs full
 * details to a local file first, then hands off to the previous/default
 * handler so Android still reports+recovers the process normally. The
 * Settings screen (Phase 9) can show the last saved crash log for
 * diagnostics/support.
 */
class CrashHandler(
    private val context: Context,
    private val defaultHandler: Thread.UncaughtExceptionHandler?
) : Thread.UncaughtExceptionHandler {

    override fun uncaughtException(thread: Thread, throwable: Throwable) {
        try {
            if (AppSettings.isCrashLoggingEnabled(context)) {
                val logDir = File(context.getExternalFilesDir(null) ?: context.filesDir, "CrashLogs")
                logDir.mkdirs()
                val stamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
                val logFile = File(logDir, "crash_$stamp.txt")
                logFile.writeText(
                    buildString {
                        appendLine("Time: ${Date()}")
                        appendLine("Thread: ${thread.name}")
                        appendLine("--- Stack trace ---")
                        appendLine(Log.getStackTraceString(throwable))
                    }
                )
                AppSettings.setLastCrashLogPath(context, logFile.absolutePath)
            }
        } catch (loggingFailure: Exception) {
            // Never let crash-logging itself throw and mask the original crash.
        }

        if (defaultHandler != null) {
            defaultHandler.uncaughtException(thread, throwable)
        } else {
            Runtime.getRuntime().exit(1)
        }
    }
}
