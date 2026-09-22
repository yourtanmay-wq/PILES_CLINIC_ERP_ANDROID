package com.tkbiswas.pilesclinic.security

import android.os.Bundle
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.tkbiswas.pilesclinic.native.DateUtil
import com.tkbiswas.pilesclinic.R
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import com.tkbiswas.pilesclinic.native.UppercaseInputUtil

/**
 * Phase 9: standalone App Settings screen (same standalone-for-now pattern as
 * the Phase 4/5/6 hub screens — no native Dashboard exists yet to host it).
 *   adb shell am start -n com.tkbiswas.pilesclinic/.security.SettingsActivity
 */
class SettingsActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // TK-REPORTED (2026-07-27): this screen has no bottom bar, so until
        // now opening it never retried a save that was still stuck on this
        // phone. A staff member could sit here while a registration or a
        // payment stayed unsent. Same retry every other screen already does.
        try { com.tkbiswas.pilesclinic.native.BottomNav.retryStuckSaves(this) } catch (_: Throwable) { }
        setContentView(R.layout.activity_settings)
        UppercaseInputUtil.applyToAll(window.decorView.findViewById(android.R.id.content))  // TK-REQUESTED GLOBAL RULE (2026-07-24): English text auto-CAPITAL, Password fields excluded automatically

        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        toolbar.setNavigationOnClickListener { finish() }

        val canEdit = SecurityGuard.canChangeSettings()
        if (!canEdit) {
            findViewById<TextView>(R.id.tvReadOnlyNotice).visibility = android.view.View.VISIBLE
        }

        setupSessionSection(canEdit)
        setupAutoSyncSwitch(canEdit)
        setupCrashLoggingSection(canEdit)
        setupBackupRestoreSection(canEdit)
    }

    private fun setupSessionSection(canEdit: Boolean) {
        val et = findViewById<TextInputEditText>(R.id.etSessionTimeout)
        val btn = findViewById<MaterialButton>(R.id.btnSaveSessionTimeout)
        et.setText(AppSettings.getSessionTimeoutMinutes(this).toString())
        et.isEnabled = canEdit
        btn.isEnabled = canEdit
        btn.setOnClickListener {
            val minutes = et.text?.toString()?.toIntOrNull()
            if (minutes == null || minutes <= 0) {
                Toast.makeText(this, "Enter a valid number of minutes.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            AppSettings.setSessionTimeoutMinutes(this, minutes)
            Toast.makeText(this, "Session timeout set to $minutes minute(s).", Toast.LENGTH_SHORT).show()
        }
    }

    private fun setupAutoSyncSwitch(canEdit: Boolean) {
        val switch = findViewById<Switch>(R.id.switchAutoSync)
        switch.isChecked = AppSettings.isAutoSyncEnabled(this)
        switch.isEnabled = canEdit
        switch.setOnCheckedChangeListener { _, isChecked ->
            AppSettings.setAutoSyncEnabled(this, isChecked)
            Toast.makeText(
                this,
                if (isChecked) "Auto Sync enabled (takes effect on next app start)."
                else "Auto Sync disabled (takes effect on next app start).",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    private fun setupCrashLoggingSection(canEdit: Boolean) {
        val switch = findViewById<Switch>(R.id.switchCrashLogging)
        switch.isChecked = AppSettings.isCrashLoggingEnabled(this)
        switch.isEnabled = canEdit
        switch.setOnCheckedChangeListener { _, isChecked ->
            AppSettings.setCrashLoggingEnabled(this, isChecked)
        }

        val lastCrashPath = AppSettings.getLastCrashLogPath(this)
        findViewById<TextView>(R.id.tvLastCrash).text =
            if (lastCrashPath != null) "Last crash log: $lastCrashPath" else "No crash recorded."

        findViewById<MaterialButton>(R.id.btnViewCrashLog).setOnClickListener {
            if (lastCrashPath == null) {
                Toast.makeText(this, "No crash log available.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            val content = runCatching { java.io.File(lastCrashPath).readText() }
                .getOrDefault("Could not read the log file (it may have been deleted).")
            AlertDialog.Builder(this)
                .setCustomTitle(com.tkbiswas.pilesclinic.native.PremiumAlert.header(this, "Last Crash Log"))
                .setMessage(content)
                .setPositiveButton("Close", null)
                .show().also { com.tkbiswas.pilesclinic.native.PremiumAlert.paint(it) }
        }
    }

    private fun setupBackupRestoreSection(canEdit: Boolean) {
        val btnBackup = findViewById<MaterialButton>(R.id.btnBackupNow)
        val btnRestore = findViewById<MaterialButton>(R.id.btnRestoreLatest)
        btnBackup.isEnabled = canEdit
        btnRestore.isEnabled = canEdit
        refreshBackupStatus()

        btnBackup.setOnClickListener {
            /* 🔵🔒 V452 (19.08.2026, TK-approved — Free Plan safety):
             * Backup Now এখনও পূর্ণ Restore backup নেয়, তাই দরকার ছাড়া বারবার চাপা ঠিক নয়।
             * কিন্তু আগের 11 full reads আর নেই: Cloud-এর 7টা backup table একবার নামে,
             * JSON Restore file তৈরি হয়, তারপর ওই local JSON থেকেই 4টা CSV লেখা হয়।
             * তাই JSON + CSV দুটোই অক্ষত, কিন্তু duplicate Supabase download নেই।
             * Screen master-only; Restore logic, local DB backup ও weekly auto-backup অপরিবর্তিত।
             */
            AlertDialog.Builder(this)
                .setCustomTitle(com.tkbiswas.pilesclinic.native.PremiumAlert.header(this, "Backup Now"))
                .setMessage(
                    "ব্যাকআপ নিতে Cloud-এর 7টা Backup Table একবার নামবে " +
                    "(রোগীর ছবিসহ)। CSV কপি ওই একই Download থেকে তৈরি হবে।\n\n" +
                    "এতে মাসিক ডেটার হিসাব থেকে কিছুটা খরচ হয়। সত্যিই দরকার হলে " +
                    "তবেই নিন, বারবার নয়।\n\nএখন ব্যাকআপ নেব?"
                )
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Yes, back up") { _, _ -> doBackupNow() }
                .show().also { com.tkbiswas.pilesclinic.native.PremiumAlert.paint(it) }
        }

        /* 🔵 V405: Restore বোতামের কাজ হুবহু আগের মতোই — শুধু নিচের আলাদা
           ফাংশনে সরানো হলো, আর এখান থেকেই ডাকা হচ্ছে। */
        setupRestoreButton(btnRestore)
    }

    private fun doBackupNow() {
            val result = BackupManager.backupNow(this)
            Toast.makeText(this, result.message, Toast.LENGTH_LONG).show()
            refreshBackupStatus()
            /* 🔵🔒 V452 WORKING FIX (19.08.2026, TK-approved, Free Plan safety):
             * JSON Restore backup + Excel/Google-Sheets CSV backup both stay.
             * Previously they each fetched the same large cloud tables separately
             * (7 full reads for JSON + 4 repeated full reads for CSV = 11 reads).
             * Now the seven cloud tables are fetched ONCE by CloudBackup.export();
             * the four CSV files are generated from that already-downloaded JSON
             * on this device. Same outputs, no second Supabase download. */
            exportCloudBackupBundleSingleFetch()
    }

    private fun setupRestoreButton(btnRestore: MaterialButton) {

        btnRestore.setOnClickListener {
            AlertDialog.Builder(this)
                .setCustomTitle(com.tkbiswas.pilesclinic.native.PremiumAlert.header(this, "Restore"))
                .setItems(
                    arrayOf(
                        "Restore local database (device backup)",
                        "Restore cloud data from JSON  ⚠️ overwrites Supabase"
                    )
                ) { _, which ->
                    if (which == 0) restoreLocalDb() else confirmCloudJsonRestore()
                }
                .setNegativeButton("Cancel", null)
                .show().also { com.tkbiswas.pilesclinic.native.PremiumAlert.paint(it) }
        }
    }

    private fun restoreLocalDb() {
        val latest = BackupManager.listBackups(this).firstOrNull()
        if (latest == null) {
            Toast.makeText(this, "No backup found yet. Tap \"Backup Now\" first.", Toast.LENGTH_LONG).show()
            return
        }
        AlertDialog.Builder(this)
            .setCustomTitle(com.tkbiswas.pilesclinic.native.PremiumAlert.header(this, "Restore Backup"))
            .setMessage(
                "This will replace the current local database with the backup from " +
                    "${DateUtil.displayWithTime(Date(latest.lastModified()))}.\n\n" +
                    "Any local changes made after that backup will be lost unless already synced to Supabase."
            )
            .setPositiveButton("Restore") { _, _ ->
                val result = BackupManager.restore(this, latest)
                Toast.makeText(this, result.message, Toast.LENGTH_LONG).show()
            }
                .setNegativeButton("Cancel", null)
                .show().also { com.tkbiswas.pilesclinic.native.PremiumAlert.paint(it) }
    }

    /**
     * Egress-safe manual cloud backup bundle.
     * One cloud fetch produces the proven JSON restore file first; CSV copies are
     * then written only from that local JSON. If CSV creation fails, the JSON file
     * is still kept and reported as successful -- backup safety is never reduced.
     */
    private fun exportCloudBackupBundleSingleFetch() {
        lifecycleScope.launch {
            val out = withContext(Dispatchers.IO) {
                var jsonPath: String? = null
                var csvDir: String? = null
                try {
                    jsonPath = com.tkbiswas.pilesclinic.native.CloudBackup.export(this@SettingsActivity)
                    if (jsonPath != null) {
                        try {
                            val root = org.json.JSONObject(java.io.File(jsonPath).readText())
                            val csvTables = listOf("enquiries", "patients", "payments", "followups")
                            var last: java.io.File? = null
                            for (t in csvTables) {
                                val rows = root.optJSONArray(t) ?: org.json.JSONArray()
                                last = com.tkbiswas.pilesclinic.native.CsvExportHelper.writeCsv(
                                    this@SettingsActivity, t, rows
                                )
                            }
                            csvDir = last?.parentFile?.absolutePath
                        } catch (_: Exception) {
                            // JSON is the restore source; never mark it failed just because
                            // the optional human-readable CSV copy could not be written.
                        }
                    }
                } catch (_: Exception) { }
                Pair(jsonPath, csvDir)
            }
            val jsonPath = out.first
            val csvDir = out.second
            if (jsonPath != null) {
                Toast.makeText(
                    this@SettingsActivity,
                    "Cloud data (JSON) saved:\n$jsonPath",
                    Toast.LENGTH_LONG
                ).show().also { try { com.tkbiswas.pilesclinic.native.NoAutofill.scrubAnyDialog(it) } catch (_: Throwable) { } }   // 🤫 V774
            } else {
                Toast.makeText(
                    this@SettingsActivity,
                    "Cloud backup could not be completed — nothing was overwritten.",
                    Toast.LENGTH_LONG
                ).show().also { try { com.tkbiswas.pilesclinic.native.NoAutofill.scrubAnyDialog(it) } catch (_: Throwable) { } }   // 🤫 V774
            }
            if (csvDir != null) {
                Toast.makeText(
                    this@SettingsActivity,
                    "Excel-ready CSV backup saved:\n$csvDir",
                    Toast.LENGTH_LONG
                ).show().also { try { com.tkbiswas.pilesclinic.native.NoAutofill.scrubAnyDialog(it) } catch (_: Throwable) { } }   // 🤫 V774
            }
        }
    }

    private fun confirmCloudJsonRestore() {
        val dir = java.io.File(getExternalFilesDir(null), "backups")
        val file = dir.listFiles { f -> f.name.startsWith("cloud_backup_") && f.name.endsWith(".json") }
            ?.maxByOrNull { it.lastModified() }
        if (file == null) {
            Toast.makeText(this, "No JSON backup found. Tap \"Backup Now\" first.", Toast.LENGTH_LONG).show()
            return
        }
        val when0 = DateUtil.displayWithTime(Date(file.lastModified()))
        AlertDialog.Builder(this)
            .setCustomTitle(com.tkbiswas.pilesclinic.native.PremiumAlert.header(this, "⚠️ Restore cloud data?"))
            .setMessage(
                "This uploads the backup from $when0 back to Supabase and OVERWRITES current cloud " +
                    "records that share the same id. It affects ALL branches and cannot be undone."
            )
            .setPositiveButton("Continue") { _, _ ->
                AlertDialog.Builder(this)
                    .setCustomTitle(com.tkbiswas.pilesclinic.native.PremiumAlert.header(this, "Final confirmation"))
                    .setMessage("This will overwrite live shared data. Are you absolutely sure?")
                    .setPositiveButton("Restore now") { _, _ -> doCloudJsonRestore(file) }
                    .setNegativeButton("Cancel", null)
                    .show().also { com.tkbiswas.pilesclinic.native.PremiumAlert.paint(it) }
            }
            .setNegativeButton("Cancel", null)
            .show().also { com.tkbiswas.pilesclinic.native.PremiumAlert.paint(it) }
    }

    private fun doCloudJsonRestore(file: java.io.File) {
        Toast.makeText(this, "Restoring… please wait.", Toast.LENGTH_SHORT).show()
        lifecycleScope.launch {
            val result = withContext(Dispatchers.IO) {
                try {
                    val root = org.json.JSONObject(file.readText())
                    val tables = listOf("enquiries", "patients", "payments", "followups", "medical", "doctor_visits", "briefings")
                    var count = 0
                    var keptNewer = 0
                    var blocked = 0
                    var failedNet = 0   // 🔒 V223: লেখা network-fail (queue-এ retry) — নীরব নয়, গোনা হয়
                    for (t in tables) {
                        val arr = root.optJSONArray(t) ?: continue
                        // 🔒 V223 (§C1, 01.08.2026): পুরোনো backup যেন নতুন cloud data চাপা না
                        // দেয় — টেবিল-প্রতি **একবার** cloud-এর id,updatedAt নামানো হয়
                        // (`fetchListOrNull`: পড়া **ব্যর্থ = null**, খালি = সত্যিই ফাঁকা)।
                        //   · পড়া ব্যর্থ হলে ঐ টেবিলের **কোনো row লেখা হয় না** (নতুনত্ব নিশ্চিত
                        //     করা যায়নি — আন্দাজে overwrite নয়), সব "verify করা যায়নি"-তে গোনা হয়।
                        //   · backup row cloud-এর চেয়ে পুরোনো হলে বাদ (নতুন জেতে); stamp তুলনা
                        //     অসম্ভব হলেও বাদ (আন্দাজে নয়)। কোনো row নীরবে হারায় না — সব গোনা হয়।
                        val cloudRows = com.tkbiswas.pilesclinic.native.SupabaseClient
                            .fetchListOrNull(t, null, 100000, select = "id,updatedAt")
                        if (cloudRows == null) { blocked += arr.length(); continue }  // পড়া ব্যর্থ → আন্দাজে লেখা নয়
                        val cloudStamp = HashMap<String, Long>()
                        for (i in 0 until cloudRows.length()) {
                            val cr = cloudRows.optJSONObject(i) ?: continue
                            val cid = cr.optString("id"); if (cid.isBlank()) continue
                            cloudStamp[cid] = com.tkbiswas.pilesclinic.native.SupabaseClient.rowStampMs(cr)
                        }
                        for (i in 0 until arr.length()) {
                            val row = arr.optJSONObject(i) ?: continue
                            val rid = row.optString("id")
                            if (rid.isBlank()) { if (com.tkbiswas.pilesclinic.native.SupabaseClient.upsert(t, row)) count++ else failedNet++; continue }
                            if (!cloudStamp.containsKey(rid)) {   // cloud-এ ঐ id নেই → নিরাপদ insert
                                if (com.tkbiswas.pilesclinic.native.SupabaseClient.upsert(t, row)) count++ else failedNet++; continue
                            }
                            val inc = com.tkbiswas.pilesclinic.native.SupabaseClient.rowStampMs(row)
                            val cs = cloudStamp[rid] ?: 0L
                            if (inc <= 0L || cs <= 0L) { blocked++; continue }   // তুলনা অসম্ভব → আন্দাজে লেখা নয়
                            if (cs > inc) { keptNewer++; continue }              // cloud নবীন → বাদ
                            if (com.tkbiswas.pilesclinic.native.SupabaseClient.upsert(t, row)) count++ else failedNet++
                        }
                    }
                    val extra = buildString {
                        if (keptNewer > 0) append(" Kept $keptNewer newer cloud record(s).")
                        if (blocked > 0) append(" $blocked skipped (could not verify cloud — try again).")
                        if (failedNet > 0) append(" $failedNet queued for retry (network).")
                    }
                    "Restored $count records.$extra"
                } catch (e: Exception) {
                    "Restore failed — check the backup file and connection."
                }
            }
            Toast.makeText(this@SettingsActivity, result, Toast.LENGTH_LONG).show()
        }
    }

    private fun refreshBackupStatus() {
        val backups = BackupManager.listBackups(this)
        findViewById<TextView>(R.id.tvBackupStatus).text = if (backups.isEmpty()) {
            "No backups yet."
        } else {
            val latest = backups.first()
            "${backups.size} backup(s). Latest: ${DateUtil.displayWithTime(Date(latest.lastModified()))}"
        }
    }
}
