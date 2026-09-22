package com.tkbiswas.pilesclinic.security

import android.content.Context
import com.tkbiswas.pilesclinic.data.local.AppDatabase
import java.io.File

data class BackupResult(val success: Boolean, val file: File? = null, val message: String)

/**
 * Local file-based Backup & Restore for the offline-first Room database. This
 * is on top of (not instead of) Phase 5's Supabase sync — it protects against
 * "lost/corrupted device" even before a sync has ever run.
 */
object BackupManager {

    private const val DB_NAME = "piles_clinic.db"

    fun backupDir(context: Context): File =
        File(context.getExternalFilesDir(null) ?: context.filesDir, "Backups").apply { mkdirs() }

    fun backupNow(context: Context): BackupResult {
        return try {
            val db = AppDatabase.getInstance(context)
            // Flush the write-ahead log so the on-disk .db file is fully up to date.
            runCatching { db.openHelper.writableDatabase.query("PRAGMA wal_checkpoint(FULL)", emptyArray()).close() }

            val dbFile = context.getDatabasePath(DB_NAME)
            if (!dbFile.exists()) {
                return BackupResult(false, message = "No local database found yet — add some data first.")
            }
            val destination = File(backupDir(context), "piles_clinic_backup_${System.currentTimeMillis()}.db")
            dbFile.copyTo(destination, overwrite = true)
            BackupResult(true, destination, "Backup saved: ${destination.name}")
        } catch (e: Exception) {
            BackupResult(false, message = "Backup failed: ${e.message}")
        }
    }

    fun listBackups(context: Context): List<File> =
        backupDir(context).listFiles { f -> f.extension == "db" }
            ?.sortedByDescending { it.lastModified() }
            ?: emptyList()

    /** Restores from a backup file. The app must be restarted after this returns true,
     * since every existing Room connection/DAO instance becomes stale. */
    fun restore(context: Context, backupFile: File): BackupResult {
        return try {
            if (!backupFile.exists()) {
                return BackupResult(false, message = "Backup file not found.")
            }
            AppDatabase.closeInstance()
            val dbFile = context.getDatabasePath(DB_NAME)
            dbFile.parentFile?.mkdirs()
            backupFile.copyTo(dbFile, overwrite = true)
            // Also clear any stale WAL/SHM files from before the restore so
            // the restored file is read cleanly on next open.
            File(dbFile.path + "-wal").delete()
            File(dbFile.path + "-shm").delete()
            BackupResult(true, dbFile, "Restore complete. Please restart the app.")
        } catch (e: Exception) {
            BackupResult(false, message = "Restore failed: ${e.message}")
        }
    }
}
